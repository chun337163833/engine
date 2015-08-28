package qgame.engine.ignition

import java.io.File

import qgame.engine.config.QGameConfig
import qgame.engine.core.{ DefaultEngine, Engine }

/**
 * Author: kerr
 */
trait EngineProvider {
  /**
   * just create the engine
   * with the engine bootstrap ,using engine.start to start up the engine,and with engine.stop to stop it
   */
  def get(): Engine
}

class DefaultEngineProvider extends EngineProvider {

  override def get(): Engine = new DefaultEngine
}

trait EngineRuntimeConfiguration {
  def rootDir: File
  def rootConfig: QGameConfig //all the configuration
  def engineConfig: QGameConfig
  def process: EngineProcess
}

final private[engine] case class DefaultEngineRuntimeConfiguration(rootDir: File, rootConfig: QGameConfig, engineConfig: QGameConfig, process: EngineProcess) extends EngineRuntimeConfiguration
