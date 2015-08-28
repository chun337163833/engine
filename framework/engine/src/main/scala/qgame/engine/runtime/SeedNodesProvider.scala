package qgame.engine.runtime

/**
 * Created by kerr.
 */
trait SeedNodesProvider {
  def get: Set[String]
}