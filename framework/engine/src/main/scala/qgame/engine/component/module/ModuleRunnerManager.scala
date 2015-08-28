package qgame.engine.component.module

import akka.actor.SupervisorStrategy.{ Decider, Stop }
import akka.actor._
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import qgame.engine.component.Component
import qgame.engine.component.Component.ModuleBinding
import qgame.engine.component.module.ModuleRunner._
import qgame.engine.component.module.ModuleRunnerManager._
import qgame.engine.core.Engine
import qgame.engine.core.Engine.EngineInnerMessage
import qgame.engine.libs.json.serializer.ActorRefJsonSerializer
import qgame.engine.logging.DebugInfo

import scala.concurrent.Promise
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
private[component] class ModuleRunnerManager(
    engine: Engine,
    component: Component,
    moduleBindings: List[ModuleBinding]
) extends Actor with ActorLogging with DebugInfo {
  //moduleName => Running module info
  private var runningModuleInfos: Map[String, RunningModuleInfo] = Map.empty
  //module runner => module name
  private var runningModuleRunners: Map[ActorRef, RunningModuleInfo] = Map.empty

  override def debugInfo: String = {
    s"""
       |+------------------------------------------
       ||                Component
       |+------------------------------------------
       ||clazz:[${component.getClass}]
       |+------------------------------------------
       ||              Module Infos
       |+------------------------------------------
       ||${runningModuleInfos.map({ case (k, v) => s"name :$k ,clazz :${v.moduleProps.moduleClass()}" }).mkString("", "\n|", "")}
       |+------------------------------------------
     """.stripMargin
  }

  override def receive: Receive = {
    case StartModules(promise, modules) =>
      val moduleBindingsWithName = moduleBindings.map(binding => (binding.name, binding)).toMap
      val moduleBindingsForStart = for {
        name <- modules
        binding <- moduleBindingsWithName.get(name)
      } yield binding
      context.become(startingModules(moduleBindingsForStart, promise))
      self ! TriggerStartingModules
  }

  private def startingModules(moduleBindings: List[ModuleBinding], promise: Promise[String]): Receive = {
    case TriggerStartingModules =>
      moduleBindings match {
        case (current @ ModuleBinding(name, moduleProps, isRequired)) :: tail =>
          //start the moduleProps actor
          val moduleClazz = moduleProps.moduleClass()
          log.debug("starting module: [{}] ,clazz : [{}],isRequired :[{}]", name, moduleClazz, isRequired)
          val moduleRunner = context.child(moduleClazz.getSimpleName) match {
            case Some(v) =>
              //TODO make sure how many module started per modules
              context.actorOf(Props.create(classOf[ModuleRunner], engine, component, name, moduleProps), moduleClazz.getName)
            case None =>
              context.actorOf(Props.create(classOf[ModuleRunner], engine, component, name, moduleProps), moduleClazz.getSimpleName)
          }
          val runningModuleInfo = RunningModuleInfo(moduleRunner, name, moduleProps, isRequired)
          runningModuleRunners += (moduleRunner -> runningModuleInfo)
          runningModuleInfos += (name -> runningModuleInfo)
          moduleRunner ! StartModule
          context.become(waitingModuleStarted(current, tail, promise))
        case Nil =>
          log.debug("no more module binding need to start.")
          context.become(modulesStartedSuccessfully)
      }
  }

  private def waitingModuleStarted(current: ModuleBinding, leftModuleBindings: List[ModuleBinding], promise: Promise[String]): Receive = {
    case ModuleStartSuccess(info) =>
      log.debug("component:[{}],module:[{}],started success,info:[{}]", component.getClass, current.name, info)
      if (leftModuleBindings.isEmpty) {
        log.debug("all modules of component :[{}] engine :[{}] started successfully.debug info \n{}", component.getClass, engine.name, debugInfo)
        promise.trySuccess(s"all modules of component :[${component.getClass}] engine :[${engine.name}] started successfully.")
        context.become(modulesStartedSuccessfully)
      } else {
        log.debug("left :[{}] modules need to start.", leftModuleBindings.size)
        context.become(startingModules(leftModuleBindings, promise))
        self ! TriggerStartingModules //trigger next
      }
    case ModuleStartFailed(e) =>
      log.error(e, "start module name:[{}],clazz:[{}] isRequired :[{}], failed.", current.name, current.moduleProps.moduleClass(), current.isRequired)
      if (current.isRequired) {
        log.warning("start module module :[{}] failed,but it's required,stop all modules now.")
        promise.tryFailure(e)
        //TODO going to stop
        context.become(modulesStartFailed(e))
      } else {
        log.warning("start module module :[{}] failed,but it not required,continues starting modules now.")
        context.become(startingModules(leftModuleBindings, promise))
        self ! TriggerStartingModules
      }
  }

  private def modulesStartedSuccessfully: Receive = {
    case StopModules(promise) =>
      log.debug("all modules was started up successfully,request to stop.")
      //TODO add close
      val modules = moduleBindings.reverse.flatMap(binding => runningModuleInfos.get(binding.name))
    case QueryModuleInfos(promise) =>
      promise.trySuccess(runningModuleInfos)
  }

  private def modulesStartFailed(e: Throwable): Receive = {
    case StopModules(promise) =>
    //here need to stop all the modules
  }

  private def stoppingModules(modules: List[RunningModuleInfo], promise: Promise[String]): Receive = {
    case TriggerStoppingModules =>
      try {
        modules match {
          case (current @ RunningModuleInfo(actorRef, name, moduleProps, isRequired)) :: tail =>
            log.debug("stopping module :[{}] clazz :[{}] isRequired :[{}] at :[{}]", name, moduleProps.moduleClass(), isRequired, actorRef)
            actorRef ! StopModule
            context.become(waitingModuleStopped(current, tail, promise))
          case Nil =>
            log.debug("no more modules need to stop.stop modules successfully.")
            promise.trySuccess("no more modules need to stop.stop modules successfully.")
            context.become(modulesStoppedSuccessfully)
        }
      } catch {
        case NonFatal(e) =>
          log.error(e, "stopping modules failed")
          promise.tryFailure(e)
      }
  }

  private def waitingModuleStopped(current: RunningModuleInfo, leftModules: List[RunningModuleInfo], promise: Promise[String]): Receive = {
    case ModuleStopSuccess(info) =>
      log.debug("module :[{}] stopped successfully,info :[{}]", current, info)
      if (leftModules.isEmpty) {
        log.debug("all modules stopped successfully.")
        promise.trySuccess("all modules stopped successfully.")
        context.become(modulesStoppedSuccessfully)
      } else {
        log.debug("left :[{}] modules need to stop,continues.", leftModules.size)
        context.become(stoppingModules(leftModules, promise))
        self ! TriggerStoppingModules
      }
    case ModuleStopFailed(e) =>
      log.error(e, "module :[{}] stop failed.", current)
      promise.tryFailure(e)
      context.become(modulesStopFailed(e))
  }

  private def modulesStoppedSuccessfully: Receive = {
    case msg =>
  }

  private def modulesStopFailed(e: Throwable): Receive = {
    case msg =>
  }

  //TODO using the defined strategy
  override def supervisorStrategy: SupervisorStrategy = {
    val f = (actorRef: ActorRef) => runningModuleRunners.get(actorRef) match {
      case Some(info) => info.isRequired
      case None => false
    }
    new ModuleRunnerSupervisorStrategy(f)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("module runner manager for component :[{}] started.", component.getClass)
    super.preStart()
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = super.postStop()
}

private[component] object ModuleRunnerManager {
  //is high avilible
  private class ModuleRunnerSupervisorStrategy(f: ActorRef => Boolean) extends SupervisorStrategy {
    override protected val loggingEnabled: Boolean = true

    override def decider: Decider = {
      case _: ActorInitializationException ⇒ Stop
      case _: ActorKilledException ⇒ Stop
      case _: DeathPactException ⇒ Stop
      case exception: Exception =>
        //TODO Think more about this
        Stop
    }

    override def handleChildTerminated(context: ActorContext, child: ActorRef, children: Iterable[ActorRef]): Unit = ()

    override def processFailure(context: ActorContext, restart: Boolean,
      child: ActorRef,
      cause: Throwable,
      stats: ChildRestartStats,
      children: Iterable[ChildRestartStats]): Unit = {
      if (restart) {
        restartChild(child, cause, suspendFirst = false)
        child ! StartModule
      } else {
        //then should be stop
        if (f(child)) {
          restartChild(child, cause, suspendFirst = false)
          child ! StartModule
        } else {
          //resume first,then stop it.
          resumeChild(child, cause)
          child ! StopModule
        }
      }
    }
  }

  case class RunningModuleInfo(@JsonSerialize(using = classOf[ActorRefJsonSerializer]) actorRef: ActorRef, name: String, moduleProps: ModuleProps, isRequired: Boolean)

  sealed trait ModuleRunnerManagerCommand extends EngineInnerMessage

  case class StartModules(promise: Promise[String], modules: List[String]) extends ModuleRunnerManagerCommand

  private object TriggerStartingModules extends ModuleRunnerManagerCommand

  case class StopModules(promise: Promise[String]) extends ModuleRunnerManagerCommand

  private object TriggerStoppingModules extends ModuleRunnerManagerCommand

  /////
  case class QueryModuleInfos(promise: Promise[Map[String, RunningModuleInfo]]) extends ModuleRunnerManagerCommand

}

