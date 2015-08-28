package qgame.engine.util

import akka.actor.{ NoScopeGiven, Scope, Props, UntypedActorWithStash }

/**
 * Created by kerr.
 */
object Actors {
  abstract class UntypedFlowControlActor extends UntypedActorWithStash with FlowControl

  implicit class PropsHelper(props: Props) {
    def withScope(s: Scope): Props = s match {
      case NoScopeGiven => props.copy(deploy = props.deploy.copy(scope = s))
      case x => if (x == props.deploy.scope) props else props.copy(deploy = props.deploy.copy(scope = x))
    }
  }
}
