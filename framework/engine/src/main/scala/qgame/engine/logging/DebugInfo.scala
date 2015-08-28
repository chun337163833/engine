package qgame.engine.logging

/**
 * Created by kerr.
 */
trait DebugInfo {
  def debugInfo: String = {
    s"""
       +-----------------------------------------------
       |                 Debug Info
       +-----------------------------------------------
       |class :${this.getClass}
       |instance:${this.toString}
       +-----------------------------------------------
     """.stripMargin
  }
}
