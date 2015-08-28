package qgame.engine.component

import akka.actor._
import qgame.engine.component.Component.RunningComponentInfo
import qgame.engine.component.ComponentRunner._
import qgame.engine.component.ComponentRunnerManager._
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.core.Engine.EngineInnerMessage
import qgame.engine.logging.DebugInfo

import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
private[engine] class ComponentRunnerManager(val engine: Engine) extends Actor with ActorLogging with DebugInfo {
  private val allComponentsConfig = engine.config.getConfig("components").getOrElse(
    throw new IllegalArgumentException("must setup the components in engine.components section.")
  )
  //component name -> component runner
  //change to using linked list map
  private var components2RunnersMapping: Map[Class[_ <: Component], ComponentRichActorRef] = Map.empty

  private var runners2ComponentsMapping: Map[ActorRef, ComponentRichActorRef] = Map.empty

  private lazy val componentsClazzes: List[Class[_ <: Component]] = {
    val enabledComponentsClazzNames = allComponentsConfig.getStringSeq("enabled").getOrElse(
      throw new IllegalArgumentException("must have an enabled section in components")
    )
    enabledComponentsClazzNames.toList.map { componentClazzName =>
      val componentClazz = {
        val clazz = Class.forName(componentClazzName, true, engine.classLoader)
        if (classOf[Component].isAssignableFrom(clazz))
          clazz.asInstanceOf[Class[_ <: Component]]
        else
          throw new IllegalArgumentException(s"$clazz is not an instance of ${classOf[Component]}")
      }
      componentClazz
    }
  }

  override def debugInfo: String = {
    s"""
       |+--------------------------------------------
       ||              Component Manager
       |+--------------------------------------------
       ||                Components
       |+--------------------------------------------
       ||${componentsClazzes.zipWithIndex.map(clazzes => s"${clazzes._2})->${clazzes._1}").mkString("\n|")}
        |+--------------------------------------------
        ||             Component Runners
        |+--------------------------------------------
        ||${components2RunnersMapping.map(runners => s"${runners._1.getSimpleName})->${runners._2}").mkString("\n|")}
        |+--------------------------------------------
     """.stripMargin
  }

  def receive: Actor.Receive = {
    case StartComponents(promise) =>
      if (componentsClazzes.isEmpty) {
        log.warning("engine :[{}] has no component enabled,will start without any component.", engine.name)
      }
      context.become(startingComponent(componentsClazzes, promise))
      self ! TriggerStartingComponents
      log.debug("trigger starting components,debug info :\n{}", debugInfo)
  }

  private def startingComponent(components: List[Class[_ <: Component]], promise: Promise[String]): Receive = {
    case TriggerStartingComponents =>
      try {
        components match {
          case head :: tail =>
            val componentConfig = readComponentConfig(head)
            val runner = {
              val simpleName = head.getSimpleName
              context.child(simpleName) match {
                case Some(actor) =>
                  context.actorOf(Props.create(classOf[ComponentRunner], engine, componentConfig, head), head.getName)
                case None =>
                  context.actorOf(Props.create(classOf[ComponentRunner], engine, componentConfig, head), head.getSimpleName)
              }
            }
            runner ! StartComponent
            context.become(waitingComponentStarted(head, tail, promise))
          case Nil =>
            context.become(componentsStartedSuccessfully)
            promise.trySuccess("all components start completed")
        }
      } catch {
        case NonFatal(e) =>
          log.error(e, "error when starting component")
          promise.tryFailure(e)
      }
  }

  private def readComponentConfig(clazz: Class[_ <: Component]): QGameConfig = {
    if (!clazz.isAnnotationPresent(classOf[ComponentConfigPath])) {
      //      throw new IllegalStateException(s"a component implement: [$clazz] must annotated with ${classOf[ComponentConfigPath]}.")
      //TODO make sure this is a right decision
      log.warning("component :[{}] without annotation of ComponentConfigPath should not have any module bindings,and will receive an empty config.")
    }
    val config = clazz.getAnnotationsByType(classOf[ComponentConfigPath]).headOption.map(_.path()) match {
      case Some(v) =>
        if (v == "") {
          log.warning("config path for component :[{}] is empty,so default empty config is used.", clazz)
          QGameConfig.empty
        } else {
          allComponentsConfig.getConfig(v).getOrElse(
            throw new IllegalArgumentException(s"there is no config section for component :[${this.getClass}] in [engine.components] section with value [$v].")
          )
        }
      case None =>
        //TODO make sure about this ,should here be an exception?
        log.warning("component :[{}] are not annotated with ComponentConfigPath,and will receive an empty config,and should not have any module bindings.", clazz)
        QGameConfig.empty
    }
    config
  }

  private def waitingComponentStarted(current: Class[_ <: Component], leftComponentClazzes: List[Class[_ <: Component]], promise: Promise[String]): Receive = {
    case ComponentStartSuccess(info) =>

      components2RunnersMapping += (current -> ComponentRichActorRef(sender(), current))
      runners2ComponentsMapping += (sender() -> ComponentRichActorRef(sender(), current))

      log.debug("component :[{}] start successfully,info :[{}]", current, info)
      if (leftComponentClazzes.isEmpty) {
        //all started up
        log.debug("all components started up.debug info :\n{}", debugInfo)
        promise.trySuccess("all components started up")
        context.become(componentsStartedSuccessfully)
      } else {
        log.debug("left [{}] components need start,continues.", leftComponentClazzes.size)
        context.become(startingComponent(leftComponentClazzes, promise))
        self ! TriggerStartingComponents
      }
    case ComponentStartFailed(e) =>
      //TODO think more about this,maybe this should be come from a strategy
      log.error(e, "component :[{}] start failed", current)
      sender() ! StopComponent
      components2RunnersMapping -= current
      runners2ComponentsMapping -= sender()
      promise.tryFailure(e)
      context.become(componentsStartFailed(e))
  }

  private def componentsStartedSuccessfully: Receive = {
    case StopComponents(promise) =>
      log.debug("components was started successfully,request to stop all components.")
      val components = componentsClazzes.reverse.flatMap(components2RunnersMapping.get)
      context.become(stoppingComponents(components, promise))
      self ! TriggerStoppingComponents
    case QueryCurrentComponentsInfo(promise) =>
      val componentsInfos = Future.sequence {
        runners2ComponentsMapping.keys.map { ref =>
          val p = Promise[RunningComponentInfo]()
          ref ! QueryComponentInfo(p)
          p.future
        }.toList
      }
      promise.tryCompleteWith(componentsInfos)
  }

  private def componentsStartFailed(e: Throwable): Receive = {
    case StopComponents(promise) =>
      //here stop all components
      log.debug("components was started failed cause by :[{}],request to stop all components.", e.getMessage)
      val components = componentsClazzes.reverse.flatMap(components2RunnersMapping.get)
      context.become(stoppingComponents(components, promise))
      self ! TriggerStoppingComponents
  }

  private def stoppingComponents(componentRunners: List[ComponentRichActorRef], promise: Promise[String]): Receive = {
    case TriggerStartingComponents =>
      try {
        componentRunners match {
          case (head @ ComponentRichActorRef(actorRef, clazz)) :: tail =>
            log.debug("stopping component :[{}] at [{}]", clazz, actorRef)
            actorRef ! StopComponent
            context.become(waitingComponentStopped(head, tail, promise))
          case Nil =>
            log.debug("no more components need to stop.stop components successfully.")
            promise.trySuccess("no more components need to stop.stop components successfully.")
            context.become(componentsStoppedSuccessfully)
        }
      } catch {
        case NonFatal(e) =>
          log.error(e, "error when stopping components.")
          promise.tryFailure(e)
      }
  }

  private def waitingComponentStopped(
    current: ComponentRichActorRef,
    leftComponents: List[ComponentRichActorRef],
    promise: Promise[String]
  ): Receive = {
    case ComponentStopSuccess(info) =>
      log.debug("component :[{}] stopped successfully,info :[{}]", current, info)
      if (leftComponents.isEmpty) {
        log.debug("all components stopped successfully.")
        promise.trySuccess("all components stopped successfully.")
        context.become(componentsStartedSuccessfully)
      } else {
        log.debug("left [{}] components need to stop.", leftComponents.size)
        context.become(stoppingComponents(leftComponents, promise))
        self ! TriggerStoppingComponents
      }
    case ComponentStopFailed(e) =>
      log.error(e, "component :[{}] stop failed.", current)
      promise.tryFailure(e)
      context.become(componentsStopFailed(e))
  }

  //TODO handle the control command
  private def componentsStoppedSuccessfully: Receive = {
    case msg =>
  }

  private def componentsStopFailed(e: Throwable): Receive = {
    case msg =>
  }

}

private[engine] object ComponentRunnerManager {

  private[component] case class ComponentRichActorRef(ref: ActorRef, clazz: Class[_ <: Component])

  sealed trait ComponentManagerCommand extends EngineInnerMessage

  case class StartComponents(promise: Promise[String]) extends ComponentManagerCommand

  private case object TriggerStartingComponents extends ComponentManagerCommand

  case class StopComponents(promise: Promise[String]) extends ComponentManagerCommand

  private case object TriggerStoppingComponents extends ComponentManagerCommand

  ////////////////////////////////////////////////

  case class QueryCurrentComponentsInfo(promise: Promise[List[RunningComponentInfo]]) extends ComponentManagerCommand

}
