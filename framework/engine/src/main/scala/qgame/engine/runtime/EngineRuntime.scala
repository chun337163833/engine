package qgame.engine.runtime

import java.io.File
import java.util.Date

import akka.actor._
import akka.cluster.Cluster
import com.typesafe.config.{ Config, ConfigFactory }
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine.EngineStartException
import qgame.engine.ignition.{ EngineProcess, EngineProvider, EngineRuntimeConfiguration }
import qgame.engine.libs.{ Cell, LoggingAble, Reflect }
import qgame.engine.logging.DebugInfo
import qgame.engine.runtime.EngineRunner.{ StartEngine, StopEngine }

import scala.concurrent.{ Future, Promise }
import scala.util.Success
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
abstract class EngineRuntime {
  def rootPath: File

  def rootConfig: QGameConfig

  def engineConfig: QGameConfig

  def classLoader: ClassLoader

  def engineProvider: EngineProvider

  def process: EngineProcess

  private[engine] def start(): Future[String]

  private[engine] def startEngine(): Future[String]

  private[engine] def stop(): Future[String]

  private[engine] def stopEngine(): Future[String]
}

object EngineRuntime {
  private val instance = Cell[EngineRuntime](null)

  private[engine] def update(runtime: EngineRuntime) = instance.lazySet(runtime)

  def current: EngineRuntime = instance.value
}

private[engine] class DefaultEngineRuntime(val process: EngineProcess, configuration: EngineRuntimeConfiguration) extends EngineRuntime with LoggingAble with DebugInfo {
  override def rootPath: File = configuration.rootDir

  override def engineConfig: QGameConfig = configuration.engineConfig

  override def classLoader: ClassLoader = process.classLoader

  override def rootConfig: QGameConfig = configuration.rootConfig

  lazy val engineProvider = loadEngineProvider(process, configuration)

  private def loadEngineProvider(process: EngineProcess, engineConfig: EngineRuntimeConfiguration): EngineProvider = {
    def readEngineProvider() = {
      engineConfig.engineConfig.getString("provider").map {
        tryProviderClassName =>
          //TODO update this when multiply engine supported
          val tryProviderClazz = try process.classLoader.loadClass(tryProviderClassName) catch {
            case e: ClassNotFoundException =>
              throw new EngineStartException(s"Couldn't find Default Engine provider class :[$tryProviderClassName].", Some(e))
          }
          if (!classOf[EngineProvider].isAssignableFrom(tryProviderClazz)) {
            throw new EngineStartException(s"Class ${tryProviderClazz.getName} must implement EngineProvider interface")
          }
          //if engineIOConfig not ok,using EngineConfig here
          val tryProviderConstructor = try tryProviderClazz.getConstructor() catch {
            case e: NoSuchMethodException =>
              throw new EngineStartException(s"Class ${tryProviderClazz.getName} must have a public default constructor")
          }
          tryProviderConstructor.newInstance().asInstanceOf[EngineProvider]
      }
    }
    readEngineProvider().get
  }

  private val systemName = configuration.engineConfig.getString("system-name").getOrElse("root")

  private lazy val clusterSeedNodes = {
    def addressToAkkaAddress(address: String) = {
      //      val Array(host, port) = address.split(":")
      //      //TODO make this configuable
      //      Address("akka.tcp", systemName, host, port.toInt)
      AddressFromURIString.parse(address)
    }
    val seedNodesFromProvider = configuration.engineConfig.getString("seed-nodes-provider").collect {
      case "" => Set.empty[String]
      case providerClazzName =>
        val providerClazz = Class.forName(providerClazzName, true, process.classLoader)
        if (!classOf[SeedNodesProvider].isAssignableFrom(providerClazz)) {
          throw new EngineStartException(s"class $providerClazzName is not an implement of ${classOf[SeedNodesProvider]}")
        } else {
          Reflect.instantiate(providerClazz).asInstanceOf[SeedNodesProvider].get
        }
    }.map(_.map(addressToAkkaAddress)).getOrElse(Set.empty)

    val seedNodesFromConfiguration = configuration.engineConfig.getStringSeq("seed-nodes").map(_.toSet.map(addressToAkkaAddress)).getOrElse(Set.empty)
    seedNodesFromProvider ++ seedNodesFromConfiguration
  }

  private lazy val clusterSeedNodesConfig: Config = {
    val stringBuilder = new StringBuilder()
    //stringBuilder.append(roles.mkString("akka.cluster.roles=[", ",", "]")).append("\n")
    stringBuilder.append(clusterSeedNodes.map(a => "\"" + a + "\"").mkString("akka.cluster.seed-nodes = [", ",", "]")).append("\n")
    ConfigFactory.parseString(stringBuilder.toString())
  }
  private val parallelism = configuration.engineConfig.getInt("parallelism").getOrElse(64)
  private val asyncMode = configuration.engineConfig.getString("task-peeking-mode").getOrElse("FIFO") match {
    case "FIFO" => true
    case "LIFO" => false
    case _ => throw new IllegalArgumentException("task-peeking-mode should be FIFO or LIFO.")
  }
  private lazy val actorSystem = {
    //TODO provide a way to setup the dispatcher for the system
    ActorSystem(
      systemName,
      Some(configuration.engineConfig.underlying.withFallback(clusterSeedNodesConfig).withFallback(ConfigFactory.load())),
      Some(process.classLoader),
      Some(new EngineExecutionContext(parallelism, asyncMode))
    )
  }

  private var engineRunner: ActorRef = _

  private lazy val cluster = Cluster(actorSystem)

  override private[engine] def start(): Future[String] = {
    val promise = Promise[String]()
    try {
      log.debug("seed nodes:[{}]", cluster.settings.SeedNodes.mkString(","))
      if (cluster.settings.SeedNodes.isEmpty) {
        log.warning("cluster's seed nodes is empty,will try to join self,and this node will be your first cluster node and it could be used as seednode later.")
        cluster.join(cluster.selfAddress)
      }
      cluster.registerOnMemberUp {
        log.info("engine :[{}] with address have been joined the cluster,leader at :[{}]", engineName, cluster.state.getLeader)
        promise.trySuccess(debugInfo)
      }
      promise.future
    } catch {
      case NonFatal(e) =>
        log.error(e, "error when start the engine runtime.")
        Future.failed(e)
    }
  }

  private lazy val engineName = engineConfig.getString("name").getOrElse("engine")

  override def debugInfo: String = {

    s"""
       |+------------------------------------------------------------
       ||                        Engine Runtime
       |+------------------------------------------------------------
       ||self address:[${cluster.selfAddress}]
       ||actor system name :[${actorSystem.name}]
       |+------------------------------------------------------------
       ||                        Cluster Info
       |+------------------------------------------------------------
       ||leader :[${cluster.state.leader}]
       |+------------------------------------------------------------
       ||                          Members
       |+------------------------------------------------------------
       ||${cluster.state.members.zipWithIndex.map({ case (member, index) => s"$index),${member.address},${member.status}" }).mkString("\n|")}
       |+------------------------------------------------------------
       |
     """.stripMargin
  }
  //TODO MAKE Runtime plugin
  //TODO Make multi engine support???
  override private[engine] def startEngine(): Future[String] = {
    log.debug("engine runtime system start up time :", new Date(actorSystem.startTime))
    log.debug("starting engine runner for engine :[{}]", engineName)
    engineRunner = actorSystem.actorOf(Props.create(classOf[EngineRunner], this, engineName), engineName)
    val promise = Promise[String]()
    engineRunner ! StartEngine(promise)
    promise.future
  }

  //TODO make this to shutdown callback hook
  override private[engine] def stopEngine(): Future[String] = {
    val promise = Promise[String]()
    engineRunner ! StopEngine(promise)
    promise.future
  }

  override private[engine] def stop(): Future[String] = {
    //TODO update to terminate for akka 2.4
    //it should stop engine first
    stopEngine().andThen {
      case Success(v) =>
        cluster.leave(cluster.selfAddress)
    }
  }
}