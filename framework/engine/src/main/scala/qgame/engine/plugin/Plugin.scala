package qgame.engine.plugin

import java.util.concurrent.atomic.AtomicReference

import qgame.engine.component.module.Module.ModuleInitializingException
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.core.Engine.LifeTime
import qgame.engine.libs.{ Logger, LoggingAble, Reflect }
import qgame.engine.plugin.PluginContext.DefaultPluginContext

import scala.annotation.tailrec
import scala.util.Try

/**
 * Created by kerr.
 */
trait Plugin[API >: Null <: AnyRef] {
  protected def context: PluginContext

  protected def engine: Engine

  protected def config: QGameConfig

  private[plugin] def aroundPreStart() = preStart()

  protected def preStart(): Unit = ()

  def instance: API

  private[plugin] def aroundPostStop() = postStop()

  protected def postStop(): Unit = ()
}

abstract class AbstractPlugin[API >: Null <: AnyRef] extends Plugin[API] {
  protected lazy val log = Logger.apply(this)

  override protected implicit val context: PluginContext = {
    val currentStack = PluginContext.stack.value
    if (currentStack.isEmpty || (currentStack.head eq null)) {
      throw new ModuleInitializingException(s"could not found plugin context for plugin :${this.getClass.getName}")
    } else {
      currentStack.head
    }
  }

  override protected def engine: Engine = context.engine

  override protected def config: QGameConfig = context.config
}

private[engine] trait PluginManager {
  def plugin[T >: Null <: AnyRef](clazz: Class[_ <: Plugin[T]]): T

  //  def apply[T >: Null <: AnyRef, P <: Plugin[T]: ClassTag]: T
}

private[engine] object PluginManager {

  class PluginException(message: String) extends RuntimeException(message)

  def apply(engine: Engine) = new DefaultPluginManager(engine)
}

private[plugin] object DefaultPluginManager {
  def apply(engine: Engine) = new DefaultPluginManager(engine)
}

private[plugin] class DefaultPluginManager(engine: Engine) extends PluginManager with LoggingAble with LifeTime {
  private val apiCachedRef: AtomicReference[Map[Class[_ <: Plugin[_]], _ >: Null <: AnyRef]] = new AtomicReference[Map[Class[_ <: Plugin[_]], _ >: Null <: AnyRef]](Map.empty)
  private val pluginCachedRef: AtomicReference[Map[Class[_ <: Plugin[_]], _ >: Null <: Plugin[_]]] = new AtomicReference[Map[Class[_ <: Plugin[_]], _ >: Null <: Plugin[_]]](Map.empty)

  private val pluginsConfig: QGameConfig = engine.config.getConfig("plugins").getOrElse(QGameConfig.empty)
  if (pluginsConfig.isEmpty) {
    log.warning("engine :[{}] don't have plugins config,so there will have no plugin started." +
      "if you want to use plugin,please make sure have an engine.plugins.enabled = [FQCN of plugins] section.", engine.name)
  }
  private val pluginClazzNames: Seq[String] = pluginsConfig.getStringSeq("enabled").getOrElse(Seq.empty)
  if (pluginClazzNames.isEmpty) {
    log.warning("engine :[{}] don't have any plugins enabled,so will not start any plugins.", engine.name)
  }
  private val hasPlugin = pluginsConfig.nonEmpty && pluginClazzNames.nonEmpty

  private var enabledPluginClasses: Set[Class[_ <: Plugin[_]]] = Set.empty

  //  override def apply[T >: Null <: AnyRef, P <: Plugin[T]: ClassTag]: T = plugin(implicitly[ClassTag[P]].runtimeClass.asInstanceOf[Class[_ <: Plugin[T]]])

  override def start(): Unit = {
    if (hasPlugin) {
      def loadClazz(clazzName: String): Class[_ <: Plugin[_ >: Null <: AnyRef]] = {
        val clazz = Try[Class[_]](Class.forName(clazzName, true, engine.classLoader)).recoverWith {
          case e: Throwable =>
            log.error(e, "could not loading class via the engine classLoader,fallbacking")
            Try[Class[_]](Class.forName(clazzName))
        }.recoverWith {
          case e: Throwable =>
            log.error(e, "could not loading class via the engine classLoader,fallbacking 2")
            Try[Class[_]](this.getClass.getClassLoader.loadClass(clazzName))
        }.recoverWith {
          case e: Throwable =>
            log.error(e, "could not loading class via the engine classLoader,fallbacking 3")
            Try[Class[_]](Thread.currentThread().getContextClassLoader.loadClass(clazzName))
        }.get
        if (!classOf[Plugin[_ >: Null <: AnyRef]].isAssignableFrom(clazz)) {
          throw new IllegalArgumentException(s"plugin [$clazz} is not an implement of [${classOf[Plugin[_]]}],please check.")
        }
        clazz.asInstanceOf[Class[_ <: Plugin[AnyRef]]]
      }
      pluginClazzNames.foreach { clazzName =>
        log.debug("engine :[{}] is loading plugin :[{}]", engine.name, clazzName)
        val clazz = loadClazz(clazzName)
        enabledPluginClasses += clazz
        clazz.getAnnotationsByType(classOf[Lazy]).headOption match {
          case Some(v) =>
            log.debug("plugin :[{}] is annotated with [{}],will load lazily.", clazz, classOf[Lazy])
          case None =>
            log.debug("plugin :[{}] is not annotated with [{}],will load it now.", clazz, classOf[Lazy])
            //dummy retrieve
            //TODO check this later
            plugin(clazz.asInstanceOf[Class[_ <: Plugin[AnyRef]]])
        }
      }
    } else {
      log.warning("engine :[{}] don't have any plugins,so will not start any plugins.", engine.name)
    }
  }

  override def stop(): Unit = {
    pluginCachedRef.get().values.foreach(_.aroundPostStop())
  }

  override def plugin[T >: Null <: AnyRef](clazz: Class[_ <: Plugin[T]]): T = {
    if (!hasPlugin) {
      throw new IllegalArgumentException(s"engine :[${engine.name}] don't have any plugin.")
    }
    if (!enabledPluginClasses(clazz)) {
      throw new IllegalArgumentException(s"plugin :[${clazz.getName}] is not enabled in the engine.plugins.enabled section.")
    }

    @tailrec
    def cachePluginApi(pluginApi: T = null): T = {
      val cache = apiCachedRef.get()
      val newPluginApi = if (pluginApi eq null) initPlugin(clazz).instance else pluginApi
      val newCache = cache.updated(clazz, newPluginApi)
      if (apiCachedRef.compareAndSet(cache, newCache)) {
        newPluginApi
      } else {
        cachePluginApi(newPluginApi)
      }
    }
    apiCachedRef.get.get(clazz) match {
      case Some(cachedPluginApi) =>
        cachedPluginApi.asInstanceOf[T]
      case None =>
        clazz.synchronized {
          cachePluginApi()
        }
    }
  }

  private def initPlugin[T >: Null <: AnyRef](clazz: Class[_ <: Plugin[T]]): Plugin[T] = {
    @tailrec
    def cachePlugin(plugin: Plugin[T] = null): Plugin[T] = {
      val cache = pluginCachedRef.get()
      val newPlugin = if (plugin eq null) {
        val config = clazz.getAnnotationsByType(classOf[PluginConfigPath]).headOption.map(_.path()) match {
          case Some(v) =>
            if (v == "") {
              log.warning("config path for plugin :[{}] is empty,so default empty config is used.", clazz)
              QGameConfig.empty
            } else {
              pluginsConfig.getConfig(v).getOrElse(
                throw new IllegalArgumentException(s"there is no config section for plugin :[$clazz}] in [engine.plugins}] section with value [$v].")
              )
            }
          case None =>
            log.warning("plugin :[{}] are not annotated with PluginConfigPath,and will receive an empty config.", clazz)
            QGameConfig.empty
        }
        //TODO change the plugin ,with pluginContext.and remove the doInit method call
        val context = DefaultPluginContext(engine, config)
        PluginContext.stack.withValue(context :: PluginContext.stack.value) {
          val newPlugin = Reflect.instantiate(clazz)
          newPlugin.aroundPreStart()
          newPlugin
        }
      } else plugin
      val newCache = cache.updated(clazz, newPlugin)
      if (pluginCachedRef.compareAndSet(cache, newCache)) {
        newPlugin
      } else {
        cachePlugin(newPlugin)
      }
    }

    pluginCachedRef.get().get(clazz) match {
      case Some(cachedPlugin) => cachedPlugin.asInstanceOf[Plugin[T]]
      case None => cachePlugin()
    }
  }
}