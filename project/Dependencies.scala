import sbt.Keys._
import sbt._

object Dependencies {

  object Versions {
    val buildAKKAVersion = "2.4.0-RC1"
    val scalaCrossBuildVersion = List("2.11.7")
    val scalaVersion = scalaCrossBuildVersion.head
  }

  import Versions._

  object Compile {
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % buildAKKAVersion

    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % buildAKKAVersion

    val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % buildAKKAVersion

    val akkaDD = "com.typesafe.akka" %% "akka-distributed-data-experimental" % buildAKKAVersion

    val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % buildAKKAVersion

    val akkaPersistent = "com.typesafe.akka" %% "akka-persistence" % buildAKKAVersion

    val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"

    val netty4 = "io.netty" % "netty-all" % "4.1.0.Beta5"

    val netty41Native = "io.netty" % "netty-transport-native-epoll" % "4.1.0.Beta5" classifier "linux-x86_64"

    val netty40Native = "io.netty" % "netty-transport-native-epoll" % "4.0.29.Final" classifier "linux-x86_64"

    val protobuf = "com.google.protobuf" % "protobuf-java" % "2.6.1"

    val asyncHttpClient = "com.ning" % "async-http-client" % "1.9.30"

    val jackson = {
      val jsonVersion = "2.6.1"
      Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % jsonVersion,
        "com.fasterxml.jackson.core" % "jackson-databind" % jsonVersion,
        "com.fasterxml.jackson.core" % "jackson-annotations" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jsonVersion exclude("com.google.guava", "guava"),
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jsonVersion exclude("com.google.guava", "guava")
      )
    }

    val log4j2Core = "org.apache.logging.log4j" % "log4j-core" % "2.3"

    val log4j2Api = "org.apache.logging.log4j" % "log4j-api" % "2.3"

    val disruptor = "com.lmax" % "disruptor" % "3.3.2"

    val kryo = "com.esotericsoftware" % "kryo" % "3.0.3"

    val lz4 = "net.jpountz.lz4" % "lz4" % "1.3.0"

    val quartz = "org.quartz-scheduler" % "quartz" % "2.2.1" exclude("c3p0", "c3p0")

    val config = "com.typesafe" % "config" % "1.3.0"

    val reflections = "org.reflections" % "reflections" % "0.9.10"

    val commonCodec = "commons-codec" % "commons-codec" % "1.10"

    val scalaStm = "org.scala-stm" %% "scala-stm" % "0.7"

    val sigar = "org.fusesource" % "sigar" % "1.6.4"

    val sigarNative = "org.fusesource" % "sigar-native" % "1.6.4" from "http://repo1.maven.org/maven2/org/fusesource/sigar/1.6.4/sigar-1.6.4-native.jar"

    val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.5"

    val jol = "org.openjdk.jol" % "jol-core" % "0.3.2"

    val junit = "junit" % "junit" % "4.12"

    val guava = "com.google.guava" % "guava" % "18.0"

    ///////////////

    val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0"

    val reactiveKafka = "com.softwaremill" %% "reactive-kafka" % "0.7.0"
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  import Compile._

  val libs = libraryDependencies

  val akkaIONettyDeps = libs ++= Seq(akkaActor, netty4, netty41Native, reactiveStreams)

  val akkaKryoSerializerDeps = libs ++= Seq(akkaActor, kryo, lz4)

  val akkaLog4j2LoggerDeps = libs ++= Seq(akkaActor, log4j2Api, log4j2Core, disruptor)

  val akkaKafkaLoggerDeps = libs ++= Seq(akkaActor,reactiveKafka)

  val akkaQuartzSchedulerDeps = libs ++= Seq(akkaActor, quartz)

  val akkaIONettyRemoteTransportDeps = libs ++= Seq(akkaRemote, netty4, netty41Native)

  val easyConfigDeps = libs ++= Seq(config)

  val easyJsonDeps = libs ++= jackson ++ Seq(akkaActor)

  val easyReflectDeps = libs ++= Seq(reflections)

  val easyCryptoDeps = libs ++= Seq(commonCodec)

  val engineDeps = libs ++= Seq(akkaCluster,
    akkaClusterTools,
    akkaDD,
    commonCodec,
    scalaStm,
    sigar,
    sigarNative,
    scalaAsync,
    jol,
    junit,
    guava,
    protobuf,
    asyncHttpClient)
}