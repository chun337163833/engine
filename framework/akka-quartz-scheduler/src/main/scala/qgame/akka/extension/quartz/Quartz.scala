package qgame.akka.extension.quartz

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionKey }
import qgame.engine.config.QGameConfig

/**
 * Created by kerr.
 */
object Quartz extends ExtensionKey[QuartzExt]

trait QuartzJobScheduler {
  def scheduler: JobScheduler
}
class QuartzExt(system: ExtendedActorSystem) extends Extension with QuartzJobScheduler {
  private val config = QGameConfig(system.settings.config).getConfig("akka.extension.quartz-scheduler").getOrElse(
    throw new IllegalArgumentException("could not instantiate QuartzExt,should have an akka.extension.quartz-scheduler section in your config.")
  )
  val scheduler: JobScheduler = QuartzScheduler(config, system)
}
