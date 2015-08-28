package qgame.engine.runtime

import akka.actor.SupervisorStrategy.{ Directive, Stop }
import akka.actor._
import qgame.engine.core.Engine.EngineInnerMessage
import qgame.engine.core.{ DefaultEngineContext, Engine, EngineContext }
import qgame.engine.libs.Logger
import qgame.engine.runtime.EngineRunner._

import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[runtime] class EngineRunner(runtime: EngineRuntime, name: String) extends Actor with ActorLogging {
  private var currentEngine: Engine = _

  override def receive: Receive = {
    case StartEngine(promise) =>
      val startFuture = startEngine()
      promise.tryCompleteWith(startFuture)
      startFuture.onComplete {
        case Success(info) =>
          self ! EngineStartSuccess(info)
        case Failure(e) =>
          self ! EngineStartFailed(e)
      }
      context.become(waitingEngineStarted)
  }

  private def startEngine(): Future[String] = {
    try {
      log.debug("config for engine:[{}] is :\n{}", name, runtime.engineConfig.underlying.root().render())
      val engineContext = DefaultEngineContext(context, name, context.system, runtime)
      EngineContext.stack.withValue(engineContext :: EngineContext.stack.value) {
        currentEngine = Engine.cacheEngine(self.path.name, runtime.engineProvider.get())
        Engine.updateEngineInstance(currentEngine)
        Logger.init(currentEngine)
        currentEngine.aroundPreStart()
        currentEngine.start()
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, "start engine error.")
        Future.failed(e)
    }
  }

  private def waitingEngineStarted: Receive = {
    case EngineStartSuccess(info) =>
      log.debug("engine :[{}],started success,info:[{}]", name, info)
      context.become(whenEngineStartedSuccessfully(info))

    case EngineStartFailed(e) =>
      log.error(e, "engine :[{}],start failed,will stop now.", name)
      context.become(whenEngineStartFailed(e))
  }

  private def whenEngineStartedSuccessfully(info: String): Receive = {
    case StopEngine(promise) =>
      val stopFuture = stopEngine()
      promise.tryCompleteWith(stopFuture)
      stopFuture.onComplete {
        case Success(stopInfo) =>
          self ! EngineStopSuccess(stopInfo)
        case Failure(stopError) =>
          self ! EngineStopFailure(stopError)
      }
      context.become(waitingEngineStopped)
  }

  private def whenEngineStartFailed(e: Throwable): Receive = {
    case StopEngine(promise) =>
      val stopFuture = stopEngine()
      promise.tryCompleteWith(stopFuture)
      stopFuture.onComplete {
        case Success(stopInfo) =>
          self ! EngineStopSuccess(stopInfo)
        case Failure(stopError) =>
          self ! EngineStopFailure(stopError)
      }
      context.become(waitingEngineStopped)
  }

  private def stopEngine(): Future[String] = {
    try {
      if (currentEngine ne null) {
        currentEngine.stop()
      } else {
        Future.failed(new IllegalStateException(s"engine :[$name] have not initialized."))
      }
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  private def waitingEngineStopped: Receive = {
    case EngineStopSuccess(info) =>
      log.debug("stop engine :[{}] success,info :[{}]", info)
      currentEngine.aroundPostStop()
      context.stop(self)
    case EngineStopFailure(e) =>
      log.error(e, "stop engine :{[]} error.", name)
      if (currentEngine ne null)
        currentEngine.aroundPostStop()
      context.stop(self)
  }

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy() {
      case _: ActorInitializationException ⇒ Stop
      case _: ActorKilledException ⇒ Stop
      case _: DeathPactException ⇒ Stop
      case exception: Exception => onChildException(exception)
    }
  }

  def onChildException(exception: Exception): Directive = {
    log.error(exception, "unhandled child exception of engine runner,resume")
    //TODO thinking more about it
    Stop
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("engine runner started for engine :[{}]", name)
    super.preStart()
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("engine runner stopped for engine :[{}]", name)
    super.postStop()
  }
}

private[runtime] object EngineRunner {

  sealed trait EngineRunnerCommand extends EngineInnerMessage

  case class StartEngine(promise: Promise[String]) extends EngineRunnerCommand
  //TODO change to EngineInfo
  private case class EngineStartSuccess(info: String)

  private case class EngineStartFailed(e: Throwable)

  case class StopEngine(promise: Promise[String]) extends EngineRunnerCommand

  private case class EngineStopSuccess(info: String)

  private case class EngineStopFailure(e: Throwable)
}
