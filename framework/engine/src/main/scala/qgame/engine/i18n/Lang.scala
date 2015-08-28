package qgame.engine.i18n

import java.util.Optional
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.ConfigFactory
import qgame.engine.config.QGameConfig

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
class Lang private[engine] (val key: String, val config: QGameConfig) {
  def get(key: String): Option[String] = config.getString(key)

  def getOrDefault(key: String, default: String) = config.getString(key, default)

  def getOptional(key: String): Optional[String] = config.getStringOptional(key)

  def fallbackWith(lang: Lang): Lang = new Lang(key, QGameConfig(config.underlying.withFallback(lang.config.underlying)))
}

object Lang {
  private val cacheRef: AtomicReference[Map[String, Lang]] = new AtomicReference[Map[String, Lang]](Map.empty)

  private def loadLang(key: String): Lang = {
    try {
      new Lang(key, QGameConfig(ConfigFactory.load(s"i18n/lang-$key")))
    } catch {
      case NonFatal(e) =>
        throw new LangLoadingException(s"please put the lang-$key.<conf,json> in the folder of resource/i18n,could not find one", e)
    }
  }
  class LangLoadingException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

  def select(key: String): Lang = {
    @tailrec
    def cacheLang(lang: Lang = null): Lang = {
      val cache = cacheRef.get()
      cache.get(key) match {
        case Some(cachedLang) =>
          cachedLang
        case None =>
          val needCacheLang = if (lang eq null) loadLang(key) else lang
          if (cacheRef.compareAndSet(cache, cache.updated(key, needCacheLang))) {
            needCacheLang
          } else {
            cacheLang(needCacheLang)
          }
      }

    }
    cacheLang()
  }

}

