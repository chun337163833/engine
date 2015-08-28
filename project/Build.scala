import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._
import sbt._


object BuildSettings {
  val buildVersion = sys.props.getOrElse("version", "1.0-SNAPSHOT")
  val buildScalaVersion = Dependencies.Versions.scalaVersion
  val buildHomepage = "http://www.q-game.cn"

  def frameworkProject(name: String, dir: String) = {
    runtimeProject(name, dir, base = "framework")
  }

  def frameworkComponentProject(name: String, dir: String, sub: String) = {
    runtimeProject(name, dir + "/" + sub, base = "framework")
  }

  def anyComponentProject(name: String, base: String, dir: String, sub: String) = {
    runtimeProject(name, dir + "/" + sub, base = base)
  }

  def runtimeProject(name: String, dir: String, base: String = "framework"): Project = {
    //project in file(dir)
    Project(name, file(base + "/" + dir))
      .settings(commonSettings: _*)
      .settings {
        sources in(Compile, doc) := Seq.empty
        publishArtifact in(Compile, packageDoc) := false
      }
  }

  val publishSettings = Seq(
    homepage := Some.apply(url("https://github.com/qgame/engine")),
    licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := {
      x => false
    },
    pomExtra := <scm>
      <url>https://github.com/qgame/engine</url>
      <connection>scm:git:git@github.com:qgame/engine.git</connection>
    </scm>
      <developers>
        <developer>
          <id>hepin1989</id>
          <name>He Pin</name>
          <url>https://github.com/hepin1989</url>
        </developer>
      </developers>)

  def commonSettings: Seq[Setting[_]] = Seq(
    organization := "cn.q-game",
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    javacOptions ++= makeJavacOptions("1.8"),
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    fork in Test := true,
    testListeners in(Test, test) := Nil,
    resolvers += ("github" at "https://github.com/qgame/release-repo/raw/master"),
    resolvers += Resolver.mavenLocal,
    sources in(Compile, doc) := Seq.empty,
    publishArtifact in(Compile, packageDoc) := false
  ) ++ scalariformSettings ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  def makeJavacOptions(version: String) = Seq("-source", version, "-target", version, "-encoding", "UTF-8")
}

object ServerBuild extends Build {

  import BuildSettings._

  lazy val root = Project(
    "root",
    file(".")
  ).settings(commonSettings: _*)
    //    .settings(releaseSettings:_*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .aggregate(frameWorkAndPluginsProjects: _*)

  lazy val frameWorkAndPluginsProjects = Seq[ProjectReference](
    engine,
    engineSample
  )

  /** ************************************************************************************************/
  /** ************************************************************************************************/

  lazy val engineCommon = frameworkComponentProject("engine-common", "engine", "common")
    .settings(publishSettings)

  lazy val engine = frameworkProject("engine", "engine")
    .settings(Dependencies.engineDeps)
    .copy(dependencies = Seq(
      engineCommon,
      akkaIoNetty,
      kafkaLogger,
      kryoSerializer,
      log4j2Logger,
      akkaQuartzJobScheduler,
      akkaRemoteTransportIONetty,
      easyConfig,
      easyCrypto,
      easyFiles,
      easyFunction,
      easyJson,
      easyReflect))

  lazy val engineSample = frameworkProject("engine-sample", "engine-sample")
    .dependsOn(engine)
    .dependsOn(kafkaLogger)

  lazy val kryoSerializer = frameworkProject("akka-kryo-serializer", "akka-kryo-serializer")
    .settings(Dependencies.akkaKryoSerializerDeps)
    .settings(publishSettings)
    .dependsOn(easyReflect)
    .dependsOn(easyConfig)

  lazy val log4j2Logger = frameworkProject("akka-log4j2-logger", "akka-log4j2-logger")
    .settings(Dependencies.akkaLog4j2LoggerDeps)
    .settings(publishSettings)

  lazy val kafkaLogger = frameworkProject("akka-kafka-logger", "akka-kafka-logger")
    .settings(Dependencies.akkaKafkaLoggerDeps)
    .settings(publishSettings)
    .dependsOn(easyConfig)

  lazy val akkaQuartzJobScheduler = frameworkProject("akka-quartz-scheduler", "akka-quartz-scheduler")
    .settings(Dependencies.akkaQuartzSchedulerDeps)
    .settings(publishSettings)
    .dependsOn(easyConfig)

  lazy val akkaIoNetty = frameworkProject("akka-io-netty", "akka-io-netty")
    .settings(Dependencies.akkaIONettyDeps)
    .dependsOn(easyConfig)

  lazy val akkaRemoteTransportIONetty = frameworkProject("akka-remote-transport-io-netty", "akka-remote-transport-io-netty")
    .settings(Dependencies.akkaIONettyRemoteTransportDeps)
    .settings(publishSettings)
    .dependsOn(akkaIoNetty)

  lazy val easyReflect = frameworkProject("easyReflect", "easyReflect")
    .settings(Dependencies.easyReflectDeps)
    .settings(publishSettings)

  lazy val easyJson = frameworkProject("easyJson", "easyJson")
    .settings(Dependencies.easyJsonDeps)
    .settings(publishSettings)

  lazy val easyFunction = frameworkProject("easyFunction", "easyFunction")
    .settings(publishSettings)

  lazy val easyFiles = frameworkProject("easyFiles", "easyFiles")
    .dependsOn(easyFunction)
    .settings(publishSettings)

  lazy val easyCrypto = frameworkProject("easyCrypto", "easyCrypto")
    .settings(Dependencies.easyCryptoDeps)
    .settings(publishSettings)

  lazy val easyConfig = frameworkProject("easyConfig", "easyConfig")
    .settings(Dependencies.easyConfigDeps)
    .settings(publishSettings)
}