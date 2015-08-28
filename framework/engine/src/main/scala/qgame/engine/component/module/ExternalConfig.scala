package qgame.engine.component.module

import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.{ ConfigFactory, ConfigResolveOptions }
import qgame.engine.config.QGameConfig

import scala.annotation.tailrec

/**
 * Created by kerr.
 */

object ExternalConfig {
  //double cache
  private val cacheRef: AtomicReference[Map[Class[_ <: Module], (Map[String, String], Map[String, QGameConfig])]] = new AtomicReference[Map[Class[_ <: Module], (Map[String, String], Map[String, QGameConfig])]](
    Map.empty
  )

  def apply(clazz: Class[_ <: Module], key: String, classLoader: ClassLoader = null): QGameConfig = {
    //read from the path

    @tailrec
    def loadConfigFromResource(externalConfigPaths: Map[String, String], qGameConfig: QGameConfig = null): QGameConfig = {
      val finalConfig = if (qGameConfig eq null) {
        if (externalConfigPaths.isEmpty || !externalConfigPaths.contains(key)) {
          throw new NoSuchElementException(s"no such a external config element for key :$key,please note:\n to using externalConfig must annotation with ExternalModuleConfigPath")
        }
        //begin
        val target = externalConfigPaths(key)
        val realClassLoader = if (classLoader eq null) this.getClass.getClassLoader else classLoader
        val resolveOption = ConfigResolveOptions.defaults().setAllowUnresolved(false).setUseSystemEnvironment(true)
        val config = ConfigFactory.parseResourcesAnySyntax(realClassLoader, target)
        val realConfig = config.resolve(resolveOption)
        //end
        QGameConfig(realConfig)
      } else {
        qGameConfig
      }
      val cache = cacheRef.get()
      val updatedCache = cache.updated(clazz, (externalConfigPaths, cache(clazz)._2.updated(key, finalConfig)))
      if (cacheRef.compareAndSet(cache, updatedCache)) {
        finalConfig
      } else {
        loadConfigFromResource(externalConfigPaths, finalConfig)
      }
    }

    @tailrec
    def loadExternalConfigPaths(externalConfigPaths: Map[String, String] = null): Map[String, String] = {
      val cache = cacheRef.get()
      cache.get(clazz) match {
        case Some(cached) =>
          cached._1
        case None =>
          val externalConfigPathsFromAnnotation = if (externalConfigPaths eq null) {
            clazz.getAnnotationsByType(classOf[ExternalModuleConfigPath]).map(value => (value.key(), value.target())).toMap
          } else {
            externalConfigPaths
          }
          val updatedCache = cache.updated(clazz, (externalConfigPathsFromAnnotation, Map.empty[String, QGameConfig]))
          if (cacheRef.compareAndSet(cache, updatedCache)) {
            externalConfigPathsFromAnnotation
          } else {
            loadExternalConfigPaths(externalConfigPathsFromAnnotation)
          }
      }
    }
    require(clazz.isAnnotationPresent(classOf[ExternalModuleConfigPath]), "must annotation with ExternalModuleConfigPath to use this feature")
    cacheRef.get().get(clazz) match {
      case Some(cacheLevel2) =>
        cacheLevel2._2.get(key) match {
          case Some(cachedConfig) =>
            cachedConfig
          case None =>
            loadConfigFromResource(cacheLevel2._1)
        }
      case None =>
        loadConfigFromResource(loadExternalConfigPaths())
    }

  }
}
