import sbt.Keys._
import sbt._

object Dependencies {

  object Versions {
    val buildAKKAVersion = "2.4-M1"
    val scalaCrossBuildVersion = List("2.11.6")
    val scalaVersion = scalaCrossBuildVersion.head
  }

  import Versions._

  object Compile {
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % buildAKKAVersion

    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % buildAKKAVersion

    val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % buildAKKAVersion

    val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % buildAKKAVersion

    val akkaPersistent = "com.typesafe.akka" %% "akka-persistence-experimental" % buildAKKAVersion

    val netty4 = "io.netty" % "netty-all" % "4.1.0.Beta5"

    val netty4Native = "io.netty" % "netty-transport-native-epoll" % "4.1.0.Beta5" classifier "linux-x86_64"

    val protobuf = "com.google.protobuf" % "protobuf-java" % "2.6.1"

    val asyncHttpClient = "com.ning" % "async-http-client" % "1.9.26"

    val jackson = {
      val jsonVersion = "2.5.4"
      Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % jsonVersion,
        "com.fasterxml.jackson.core" % "jackson-databind" % jsonVersion,
        "com.fasterxml.jackson.core" % "jackson-annotations" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jsonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jsonVersion exclude("com.google.guava", "guava"),
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.2" exclude("com.google.guava", "guava")
      )
    }

    val log4j2Core = "org.apache.logging.log4j" % "log4j-core" % "2.3"

    val log4j2Api = "org.apache.logging.log4j" % "log4j-api" % "2.3"

    val disruptor = "com.lmax" % "disruptor" % "3.3.2"

    val kryo = "com.esotericsoftware" % "kryo" % "3.0.1"

    val lz4 = "net.jpountz.lz4" % "lz4" % "1.3.0"

    val quartz = "org.quartz-scheduler" % "quartz" % "2.2.1" exclude("c3p0", "c3p0")

    val config = "com.typesafe" % "config" % "1.3.0"

    val reflections = "org.reflections" % "reflections" % "0.9.10"

    val commonCodec = "commons-codec" % "commons-codec" % "1.10"

    val scalaStm = "org.scala-stm" %% "scala-stm" % "0.7"

    val sigar = "org.fusesource" % "sigar" % "1.6.4"

    val sigarNative = "org.fusesource" % "sigar-native" % "1.6.4" from "http://repo1.maven.org/maven2/org/fusesource/sigar/1.6.4/sigar-1.6.4-native.jar"

    val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.3"

    val jol = "org.openjdk.jol" % "jol-core" % "0.3.2"

    val junit = "junit" % "junit" % "4.12"

    val guava = "com.google.guava" % "guava" % "18.0"

    val autoService = "com.google.auto.service" % "auto-service" % "1.0-rc2"

    val netty3 = "io.netty" % "netty" % "3.10.3.Final"

    val jedis = "redis.clients" % "jedis" % "2.7.2"

    val redisson = "org.redisson" % "redisson" % "1.2.1"

    val servlet = "javax.servlet" % "javax.servlet-api" % "3.1.0"

    val poi = "org.apache.poi" % "poi" % "3.12"

    val poiooxml = "org.apache.poi" % "poi-ooxml" % "3.12"

    val jprotobuf = "com.baidu" % "jprotobuf" % "1.5.8"


    val protoStuff = {
      val protostuffVersion = "1.3.5"
      Seq("io.protostuff" % "protostuff-core" % protostuffVersion,
        "io.protostuff" % "protostuff-runtime" % protostuffVersion,
        "io.protostuff" % "protostuff-runtime-registry" % protostuffVersion)
    }

    val hikariCP = "com.zaxxer" % "HikariCP" % "2.3.8"

    val slick = "com.typesafe.slick" %% "slick" % "2.1.0"

    val slickCodeGen = "com.typesafe.slick" %% "slick-codegen" % "2.1.0"

    val spray = {
      val sprayV = "1.3.3"
      Seq(
        "io.spray" %% "spray-can" % sprayV,
        "io.spray" %% "spray-routing" % sprayV,
        "io.spray" %% "spray-io" % sprayV,
        "io.spray" %% "spray-caching" % sprayV)
    }


    val mysqlConnector = "mysql" % "mysql-connector-java" % "5.1.35"

    val levelDb = "org.iq80.leveldb" % "leveldb" % "0.7"

    val zip4j = "net.lingala.zip4j" % "zip4j" % "1.3.2"

    val akkaPersistenceInmemory = "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.0.3"

    val jodaTime = "joda-time" % "joda-time" % "2.8"

    val fastjson = "com.alibaba" % "fastjson" % "1.2.6"

    val jooqVersion = "3.6.2"

    val jooq = "org.jooq" % "jooq" % jooqVersion

    val jooqCodeGen = "org.jooq" % "jooq-codegen" % jooqVersion

    val akkaMultiNodeTestKit ="com.typesafe.akka" %% "akka-multi-node-testkit" % buildAKKAVersion

    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"

    val joddTime = "org.jodd" % "jodd-core" % "3.5"

    val playSlick = "com.typesafe.play" %% "play-slick" % "1.0.0"

    val playSlickEvolutions = "com.typesafe.play" %% "play-slick-evolutions" % "1.0.0"
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  import Compile._

  val libs = libraryDependencies

  val akkaIONettyDeps = libs ++= Seq(akkaActor, netty4, netty4Native)

  val akkaKryoSerializerDeps = libs ++= Seq(akkaActor, kryo, lz4)

  val akkaLog4j2LoggerDeps = libs ++= Seq(akkaActor, log4j2Api, log4j2Core, disruptor)

  val akkaNetty4RemoteTransportDeps = libs ++= Seq(akkaRemote, netty4, netty4Native)

  val akkaQuartzSchedulerDeps = libs ++= Seq(akkaActor, quartz)

  val akkaIONettyRemoteTransportDeps = libs ++= Seq(akkaRemote, netty4, netty4Native)

  val easyConfigDeps = libs ++= Seq(config)

  val easyJsonDeps = libs ++= jackson ++ Seq(akkaActor)

  val easyReflectDeps = libs ++= Seq(reflections)

  val engineDeps = libs ++= Seq(akkaCluster,
    akkaClusterTools,
    commonCodec,
    scalaStm,
    sigar,
    sigarNative,
    scalaAsync,
    jol,
    junit,
    guava,
    protobuf,
    asyncHttpClient,
    autoService)

  val engineClusterTestDeps = libs ++= Seq(akkaMultiNodeTestKit,scalaTest)

  val utilsDeps = libs ++= Seq(akkaClusterTools, jooq, jedis, servlet, poi, poiooxml, jprotobuf,joddTime) ++ protoStuff

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  val pluginDataDeps = libs ++= Seq(hikariCP, slick, slickCodeGen)

  val pluginRedisDeps = libs ++= Seq(jedis,redisson)

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  val engineManagementDeps = libs ++= spray

  val engineControlDeps = libs ++= Seq(mysqlConnector, hikariCP)

  val gateDeps = libs ++= spray ++ Seq(mysqlConnector)

  val gateStandAloneDeps = libs ++= Seq(mysqlConnector,playSlick,playSlickEvolutions)

  val nettyServerProtocalDeps = libs ++= Seq(akkaActor)

  val payDeps = libs ++= spray ++ Seq(mysqlConnector, akkaPersistent, levelDb)

  val resourceManagerDeps = libs ++= spray ++ Seq(akkaPersistent, zip4j)

  val restServerdeps = libs ++= spray

  val statisticDeps = libs ++= Seq(akkaPersistenceInmemory, mysqlConnector, jodaTime)
  val statisticResolvers = Seq("dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven")

  val statisticClientDeps = libs ++= Seq(akkaPersistent)

  val commonProtocolDeps = libs ++= Seq(protobuf)

  val jooqCodeGenStrategyDeps = libs ++= Seq(jooqCodeGen)

  val dbDeps = libs ++= Seq(mysqlConnector, mysqlConnector % "jooq", fastjson)

  val gameFriendsDeps = libs ++= Seq(mysqlConnector, fastjson)

  val engineControlProtocalDeps = libs ++= Seq(akkaActor)

  val gameGateDeps = libs ++= Seq(mysqlConnector)

  val gmServerProtocalDeps = libs ++= jackson

  val logicMasterProtocalDeps = libs ++= Seq(akkaActor)

  val logicServerDeps = libs ++= Seq(poi, poiooxml,joddTime)

  val mailServiceDeps = libs ++= Seq(fastjson, mysqlConnector)

  val heroExchangeServiceDeps = libs ++= Seq(fastjson, mysqlConnector,poi)

}