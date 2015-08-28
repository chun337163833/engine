package qgame

/**
 * Created by kerr.
 */
package object engine {
  implicit val executionContext = qgame.engine.runtime.globalExecutionContext
}
