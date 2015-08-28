package qgame.engine.logging

import org.apache.logging.log4j.core.util.Clock
import qgame.engine.core.TimeService

/**
 * Created by kerr.
 */
class Log4j2Clock extends Clock {
  override def currentTimeMillis(): Long = TimeService.timeTickInSeconds()
}
