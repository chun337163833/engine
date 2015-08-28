package qgame.engine.libs.flows

/**
 * Created by kerr.
 */
trait FlowLike {
  type IN
  type OUT
  def source: IN
  def output: OUT
  //onComplete onError onNext
}
