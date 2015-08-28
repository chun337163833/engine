package qgame.engine.component.module

import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorRef
import qgame.engine.component.Component
import qgame.engine.component.module.Module.ModuleInitializingException
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.libs.Logger

import scala.annotation.tailrec
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */

trait Module {
  //TODO make this protected
  protected def context: ModuleContext

  def component: Component

  def engine: Engine

  lazy val scope: ModuleScope = ModuleScope(this)

  def config: QGameConfig

  private[module] def aroundPreStart() = preStart()

  protected def preStart(): Unit = ()

  private[module] def start(): Future[String] = {
    try {
      val promise = Promise[String]()
      doStart(promise)
      promise.future
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  protected def doStart(promise: Promise[String]): Unit

  private[module] def stop(): Future[String] = {
    try {
      val promise = Promise[String]()
      doStart(promise)
      promise.future
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  protected def doStop(promise: Promise[String]): Unit

  private[module] def aroundPostStop() = postStop()

  protected def postStop(): Unit = ()
}

abstract class AbstractModule extends Module {
  protected lazy val log = Logger.apply(this)

  protected implicit val context: ModuleContext = {
    val currentStack = ModuleContext.stack.value
    if (currentStack.isEmpty || (currentStack.head eq null)) {
      throw new ModuleInitializingException(s"could not found module context for module :${this.getClass.getName}")
    } else {
      currentStack.head
    }
  }

  override def component: Component = context.component

  override def config: QGameConfig = context.config

  override def engine: Engine = context.engine
}

object Module {
  class ModuleInitializingException(message: String) extends RuntimeException(message)

  private val moduleCacheRef: AtomicReference[Map[String, Module]] = new AtomicReference[Map[String, Module]](Map.empty)

  @tailrec
  private[module] def cacheModule(name: String, module: Module): Module = {
    val cache = moduleCacheRef.get()
    val updated = cache.updated(name, module)
    if (moduleCacheRef.compareAndSet(cache, updated)) {
      module
    } else {
      cacheModule(name, module)
    }
  }

  def of(implicit self: ActorRef): Option[Module] = {
    self.path.elements.slice(5, 6).headOption.flatMap(moduleCacheRef.get().get)
  }
}

