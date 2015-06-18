import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._
import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._
import sbt._


object BuildSettings {
  val buildVersion = sys.props.getOrElse("version", "1.0-SNAPSHOT")
  val buildScalaVersion = Dependencies.Versions.scalaVersion
  val buildHomepage = "http://www.q-game.cn"
  val buildResolver = "local" at "http://127.0.0.1:8080/artifactory/libs-release"

  def makeJavacOptions(version: String) = Seq("-source", version, "-target", version, "-encoding", "UTF-8")

  def commonSettings: Seq[Setting[_]] = Seq(
    organization := "cn.q-game",
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    homepage := Some.apply(url(buildHomepage)),
    javacOptions ++= makeJavacOptions("1.8"),
    fork in Test := true,
    testListeners in(Test, test) := Nil,
    resolvers += Resolver.jcenterRepo,
    publishArtifact in(Compile, packageDoc) := false
  ) ++ scalariformSettings ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  def runtimeProject(name: String, dir: String, base: String = "framework"): Project = {
    //project in file(dir)
    Project(name, file(base + "/" + dir))
      .settings(commonSettings: _*)
      .settings {
      sources in(Compile, doc) := Seq.empty
      publishArtifact in(Compile, packageDoc) := false
    }
  }



  def frameworkProject(name: String, dir: String) = {
    runtimeProject(name, dir, base = "framework")
  }

  def frameworkComponentProject(name: String, dir: String, sub: String) = {
    runtimeProject(name, dir + "/" + sub, base = "framework")
  }


  def enginePluginProject(name: String, dir: String) = {
    runtimeProject(name, dir, base = "plugins")
  }

  def stackComponentProject(name: String, dir: String, sub: String) = {
    runtimeProject(name, dir + "/" + sub, base = "stack")
  }

  def anyComponentProject(name: String, base: String, dir: String, sub: String) = {
    runtimeProject(name, dir + "/" + sub, base = base)
  }
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
    akkaIoNetty,
    akkaCRTD,
    kryoSerializer,
    log4j2Logger,
    netty4RemoteTransport,
    akkaQuartzJobScheduler,
    easyConfig,
    easyJson,
    easyReflect,
    engine,
    engineIO,
    engineSample)

  /** ************************************************************************************************************/
  /** ******************************    plugins     *******************************/
  /** ************************************************************************************************************/
  lazy val pluginRedis = enginePluginProject("engine-plugin-redis", "engine-plugin-redis")
    .settings(Dependencies.pluginRedisDeps)
    .dependsOn(engine)

  lazy val pluginData = enginePluginProject("engine-plugin-data", "engine-plugin-data")
    .settings(Dependencies.pluginDataDeps)
    .dependsOn(engine)
  /** ************************************************************************************************/
  /** ************************************************************************************************/

  lazy val engineCommon = frameworkComponentProject("engine-common", "engine", "common")

  lazy val engine = frameworkProject("engine", "engine")
    .settings(Dependencies.engineDeps)
    .copy(dependencies = Seq(easyConfig,
    easyReflect,
    easyJson,
    engineCommon,
    akkaRemoteTransportIONetty,
    log4j2Logger,
    kryoSerializer,
    akkaQuartzJobScheduler,
    akkaCRTD))

  lazy val engineSample = frameworkProject("engine-sample", "engine-sample")
    .dependsOn(engine)

  lazy val engineClusterTest = frameworkProject("engine-cluster-test","engine-cluster-test")
    .settings(Dependencies.engineClusterTestDeps)
    .settings(SbtMultiJvm.multiJvmSettings ++ Seq(
    // make sure that MultiJvm test are compiled by the default test compilation
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    // disable parallel tests
    parallelExecution in Test := false,
    // make sure that MultiJvm tests are executed by the default test target,
    // and combine the results from ordinary test and multi-jvm tests
    executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults)  =>
        val overall =
          if (testResults.overall.id < multiNodeResults.overall.id)
            multiNodeResults.overall
          else
            testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries)
    }
  ))
    .dependsOn(engine)
    .configs (MultiJvm)

  lazy val engineIO = frameworkProject("engine-io", "engine-io")
    .dependsOn(engine)

  lazy val netty4RemoteTransport = frameworkProject("akka-netty4-remote-transport", "akka-netty4-remote-transport")
    .settings(Dependencies.akkaNetty4RemoteTransportDeps)

  lazy val kryoSerializer = frameworkProject("akka-kryo-serializer", "akka-kryo-serializer")
    .settings(Dependencies.akkaKryoSerializerDeps)
    .dependsOn(easyConfig)
    .dependsOn(easyReflect)

  lazy val log4j2Logger = frameworkProject("akka-log4j2-logger", "akka-log4j2-logger")
    .settings(Dependencies.akkaLog4j2LoggerDeps)

  lazy val akkaQuartzJobScheduler = frameworkProject("akka-quartz-scheduler", "akka-quartz-scheduler")
    .settings(Dependencies.akkaQuartzSchedulerDeps)
    .dependsOn(easyConfig)

  lazy val akkaIoNetty = frameworkProject("akka-io-netty", "akka-io-netty")
    .settings(Dependencies.akkaIONettyDeps)
    .dependsOn(easyConfig)

  lazy val akkaRemoteTransportIONetty = frameworkProject("akka-remote-transport-io-netty", "akka-remote-transport-io-netty")
    .settings(Dependencies.akkaIONettyRemoteTransportDeps)
    .dependsOn(akkaIoNetty)

  lazy val akkaCRTD = frameworkProject("akka-crdt", "akka-crdt")

  lazy val easyConfig = frameworkProject("easyConfig", "easyConfig")
    .settings(Dependencies.easyConfigDeps)

  lazy val easyReflect = frameworkProject("easyReflect", "easyReflect")
    .settings(Dependencies.easyReflectDeps)

  lazy val easyJson = frameworkProject("easyJson", "easyJson")
    .settings(Dependencies.easyJsonDeps)

}
