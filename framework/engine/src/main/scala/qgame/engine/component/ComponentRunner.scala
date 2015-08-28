package qgame.engine.component

import akka.actor.{ Actor, ActorLogging }
import qgame.engine.component.Component.RunningComponentInfo
import qgame.engine.component.ComponentContext.DefaultComponentContext
import qgame.engine.component.ComponentRunner._
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.libs.Reflect

import scala.concurrent.{ Promise, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[component] class ComponentRunner(
    engine: Engine,
    config: QGameConfig,
    clazz: Class[_ <: Component]
) extends Actor with ActorLogging {
  private var currentComponent: Component = _
  //should show a warning when the config is empty
  def receive: Receive = {
    case StartComponent =>
      log.info("starting component :[{}].", clazz)
      val startFuture = startComponent()
      startFuture.onComplete {
        case Success(info) =>
          self ! ComponentStartSuccess(info)
        case Failure(e) =>
          self ! ComponentStartFailed(e)
      }
      context.become(waitingComponentStarted)
  }

  private def startComponent(): Future[String] = {
    try {
      log.debug("config for component :[{}] configuration is:\n{}", clazz, config.underlying.root().render())
      val componentContext = new DefaultComponentContext(context, engine, config)
      ComponentContext.stack.withValue(componentContext :: ComponentContext.stack.value) {
        currentComponent = Component.cacheComponent(self.path.name, Reflect.instantiate(clazz))
        currentComponent.aroundPreStart()
        currentComponent.start()
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, "error when start component :[{}]", clazz)
        Future.failed(e)
    }
  }

  private def waitingComponentStarted: Receive = {
    case m @ ComponentStartSuccess(info) =>
      log.debug("component :[{}] start successful,info :[{}]", clazz, info)
      context.parent ! m
      context.become(whenComponentStartedSuccessfully(info))
    case m @ ComponentStartFailed(e) =>
      log.error(e, "component :[{}] start failed", clazz)
      context.parent ! m
      context.become(whenComponentStartFailed(e))
  }

  private def whenComponentStartedSuccessfully(info: String): Receive = {
    case StopComponent =>
      log.debug("component :[{}] was started successfully,info :[{}],request to stop", clazz, info)
      val stopFuture = stopComponent()
      stopFuture.onComplete {
        case Success(stopInfo) =>
          self ! ComponentStopSuccess(stopInfo)
        case Failure(stopError) =>
          self ! ComponentStopFailed(stopError)
      }
      context.become(waitingComponentStopped)
    case QueryComponentInfo(promise) =>
      if (currentComponent ne null) {
        promise.tryCompleteWith(currentComponent.queryComponentInfo())
      } else {
        promise.failure(new IllegalStateException("there is no component running."))
      }
  }

  private def whenComponentStartFailed(e: Throwable): Receive = {
    case StopComponent =>
      log.debug("component :[{}] start failed,cause :[{}],request to stop.", clazz, e.getMessage)
      val stopFuture = stopComponent()
      stopFuture.onComplete {
        case Success(info) =>
          self ! ComponentStopSuccess(info)
        case Failure(stopError) =>
          self ! ComponentStopFailed(stopError)
      }
      context.become(waitingComponentStopped)
  }

  private def stopComponent(): Future[String] = {
    try {
      if (currentComponent ne null) {
        currentComponent.stop()
      } else {
        Future.failed(new IllegalStateException(s"component :[$clazz] have not initialized."))
      }
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  private def waitingComponentStopped: Receive = {
    case m @ ComponentStopSuccess(info) =>
      log.debug("stop component :[{}] successful,info :[{}]", clazz, info)
      currentComponent.aroundPostStop()
      context.parent ! m
      context.become(componentStopSuccessfully)
    case m @ ComponentStopFailed(e) =>
      log.error(e, "stop component :[{}] failed,info :[{}]", clazz)
      if (currentComponent ne null)
        currentComponent.aroundPostStop()
      context.parent ! m
      context.become(componentStopFailed(e))
  }

  private def componentStopSuccessfully: Receive = {
    case msg =>
  }

  private def componentStopFailed(e: Throwable): Receive = {
    case msg =>
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    if (config.isEmpty) {
      log.warning("component runner pre start ,config for component :[{}] is empty.", clazz)
    }
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = super.postStop()
}

private[component] object ComponentRunner {

  sealed trait ComponentRunnerCommand

  object StartComponent extends ComponentRunnerCommand
  //TODO  change all success to successful
  case class ComponentStartSuccess(info: String)

  case class ComponentStartFailed(e: Throwable)

  object StopComponent extends ComponentRunnerCommand

  case class ComponentStopSuccess(info: String)

  case class ComponentStopFailed(e: Throwable)

  ///////////////////////////////////////////////////
  case class QueryComponentInfo(promise: Promise[RunningComponentInfo]) extends ComponentRunnerCommand
}
