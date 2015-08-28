package qgame.engine.component

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ ActorRef, Props }
import akka.event.EventStream
import qgame.engine.component.Component._
import qgame.engine.component.module.Module.ModuleInitializingException
import qgame.engine.component.module.ModuleRunnerManager.{ QueryModuleInfos, RunningModuleInfo, StartModules, StopModules }
import qgame.engine.component.module.{ ModuleProps, ModuleRunnerManager }
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.libs.LoggingAble
import qgame.engine.logging.DebugInfo

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
trait Component {
  protected def context: ComponentContext

  def eventStream: EventStream

  def engine: Engine

  lazy val scope: ComponentScope = ComponentScope(this)

  def config: QGameConfig

  protected def loadModuleBindings(binder: ModuleBinder, config: QGameConfig): List[ModuleBinding]

  private[component] def aroundPreStart() = preStart()

  protected def preStart(): Unit = ()

  private[component] def start(): Future[String]

  private[component] def stop(): Future[String]

  private[component] def aroundPostStop() = postStop()

  protected def postStop(): Unit = ()

  //support method
  private[component] def queryModuleInfos(): Future[Map[String, RunningModuleInfo]]

  private[component] def queryComponentInfo(): Future[RunningComponentInfo]
}

object Component {

  private val componentCacheRef: AtomicReference[Map[String, Component]] = new AtomicReference[Map[String, Component]](Map.empty)

  @tailrec
  private[component] def cacheComponent(name: String, component: Component): Component = {
    val cache = componentCacheRef.get()
    val updated = cache.updated(name, component)

    if (componentCacheRef.compareAndSet(cache, updated)) {
      component
    } else {
      cacheComponent(name, component)
    }
  }

  //TODO think more about this,cause current I could not limit the attributes fetch permit
  def of(implicit self: ActorRef): Option[Component] = {
    self.path.elements.slice(3, 4).headOption.flatMap(componentCacheRef.get().get)
  }

  case class ModuleBinding(name: String, moduleProps: ModuleProps, isRequired: Boolean)

  trait ModuleBinder {
    def bind(name: String, moduleProps: ModuleProps): ModuleBinder
    def bind(name: String, moduleProps: ModuleProps, isRequired: Boolean): ModuleBinder

    def build: List[ModuleBinding]
  }

  private[component] class ModuleBinderImpl extends ModuleBinder {
    private val bindingBuffer: ListBuffer[ModuleBinding] = new ListBuffer[ModuleBinding]

    override def bind(name: String, moduleProps: ModuleProps, isRequired: Boolean): ModuleBinder = {
      bindingBuffer += ModuleBinding(name, moduleProps, isRequired = true)
      this
    }

    override def bind(name: String, moduleProps: ModuleProps): ModuleBinder = bind(name, moduleProps, isRequired = true)

    override def build: List[ModuleBinding] = bindingBuffer.toList
  }

  class ComponentInitializeException(message: String) extends Exception(message)

  case class RunningComponentInfo(
    clazz: Class[_ <: Component],
    moduleBindings: List[ModuleBinding],
    moduleInfos: Map[String, RunningModuleInfo]
  )

}

abstract class AbstractComponent extends Component with LoggingAble with DebugInfo {
  protected implicit val context: ComponentContext = {
    val currentStack = ComponentContext.stack.value
    if (currentStack.isEmpty || (currentStack.head eq null)) {
      throw new ModuleInitializingException(s"could not found module context for module :${this.getClass.getName}")
    } else {
      currentStack.head
    }
  }

  lazy val eventStream = new EventStream(context.engine.actorSystem)

  def config: QGameConfig = context.config

  def engine: Engine = context.engine

  private var moduleManager: ActorRef = _

  private var moduleBindings: List[ModuleBinding] = _

  override private[component] def aroundPreStart(): Unit = {
    try {
      moduleBindings = loadModuleBindings(new ModuleBinderImpl, config)
      if (moduleBindings.isEmpty) {
        log.warning(s"component: [{}] has no moduleBindings.will not start module manager.", this.getClass)
      } else {
        log.debug("component :[{}] have module bindings :\n[{}]", this.getClass, moduleBindings.map(binding => s"name :${binding.name}, " +
          s"clazz :${binding.moduleProps.moduleClass()}, isRequired :${binding.isRequired}").zipWithIndex.map(zip => s"${zip._2})-> ${zip._1}"))
        val props = Props.create(classOf[ModuleRunnerManager], engine, this, moduleBindings)
        moduleManager = context.actorOf(props, "mrm")
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, "error when around pre start the component :[{}]", this.getClass)
        throw e
    }
    super.aroundPreStart()
  }

  override private[component] def start(): Future[String] = {
    val promise = Promise[String]()
    try {
      if (moduleBindings.isEmpty) {
        log.debug("component :[{}] have no module bindings, will start with no module manager.", this.getClass)
        promise.trySuccess(s"component :[${this.getClass}}] started with no module manager.")
      } else {
        //we need to check the component's config
        if (config.isEmpty) {
          log.warning("component :[{}] have no config,so don't have a modules.enabled section too.will start without any modules.", this.getClass)
          promise.trySuccess(s"component :[${this.getClass}] have no config,started with no pre load modules.")
        } else {
          //then we need to check the preload modules
          config.getStringSeq("modules.enabled") match {
            case Some(needPreLoadModuleNames) =>
              log.debug(
                "component  debug info :\n{}",
                s"""|+-----------------------------------------------------------
                   ||                      Component
                   ||[${this.getClass}]
                                        ||-----------------------------------------------------------
                                        ||                       Modules
                                        ||-----------------------------------------------------------
                                        ||                    module bindings
                                        ||${moduleBindings.zipWithIndex.map({ case (binding, index) => s"$index)-> name :[${binding.name}] clazz :[${binding.moduleProps.clazz}]" }).mkString("\n")}
                    ||-----------------------------------------------------------
                    ||                     preLoad modules
                    ||${needPreLoadModuleNames.mkString("[", ",", "]")}
                    |+-----------------------------------------------------------
        """.stripMargin
              )

              val diff = needPreLoadModuleNames.diff(moduleBindings.map(_.name))
              if (diff.nonEmpty) {
                throw new ComponentInitializeException(s"there is no module definition for [${diff.mkString(",")}] in component [${this.getClass}],please check.")
              }
              if (needPreLoadModuleNames.isEmpty) {
                log.warning(s"component:[{}] has no preload modules.will start without any module preStarted", this.getClass)
                promise.trySuccess(s"component:[${this.getClass}] has no preload modules.will start without any module preStarted")
              } else {
                log.debug("component :[{}] starting modules.", this.getClass)
                //TODO make use of modules.enabled but not modules.enabled
                moduleManager ! StartModules(promise, needPreLoadModuleNames.toList)
              }

            case None =>
              log.warning("component :[{}] have config but don't have a modules.enabled section,will start without any modules.", this.getClass)
              promise.trySuccess(s"component :[${this.getClass}}] have config but without a modules.enabled section,started without no pre load modules.")
          }
        }
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, "error when start component :[{}].", this.getClass)
        promise.tryFailure(e)
    }
    promise.future
  }

  override private[component] def stop(): Future[String] = {
    if (moduleManager ne null) {
      val promise = Promise[String]()
      moduleManager ! StopModules(promise)
      promise.future
    } else {
      Future.successful(s"component :[${this.getClass}] stopped successfully.")
    }
  }

  //support method
  override private[component] def queryModuleInfos(): Future[Map[String, RunningModuleInfo]] = {
    if (moduleManager ne null) {
      val promise = Promise[Map[String, RunningModuleInfo]]()
      moduleManager ! QueryModuleInfos(promise)
      promise.future
    } else {
      Future.failed(new IllegalStateException("module manager is not started"))
    }
  }

  override private[component] def queryComponentInfo(): Future[RunningComponentInfo] = {
    queryModuleInfos().map {
      infos =>
        RunningComponentInfo(this.getClass, moduleBindings, infos)
    }
  }
}