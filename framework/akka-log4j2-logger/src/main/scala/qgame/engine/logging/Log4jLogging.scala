package qgame.engine.logging

/**
 * Created by kerr.
 */

trait Log4jLogging {
  @transient
  lazy val log = Log4jLogger(this.getClass.getName)
}
