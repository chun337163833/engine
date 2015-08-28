package gear.sample.engine.component.ping.module

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import gear.sample.engine.component.ping.module.PingServiceModule.PongServiceActor
import qgame.engine.component.module.AbstractModule
import qgame.engine.core.Engine
import qgame.engine.libs.Json
import qgame.engine.service.ServiceRegistry.Service

import scala.concurrent.Promise
import scala.language.postfixOps

/**
 * Created by kerr.
 */
class PingServiceModule extends AbstractModule {
  private var pongServiceActor: ActorRef = _
  override protected def doStart(promise: Promise[String]): Unit = {
    pongServiceActor = context.actorOf(Props.create(classOf[PongServiceActor]), "ping")
    val pongService = Service.create(pongServiceActor, "ping", "1.0.0", "ping pong")
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    promise.tryCompleteWith(context.engine.serviceRegistry.registerAsync(pongService, 3.seconds).map(_.description))
  }

  override protected def doStop(promise: Promise[String]): Unit = {

  }
}

object PingServiceModule {
  class PongServiceActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case "ping" =>
        log.debug("get ping from :[{}]", sender())
        sender() ! "pong~~"
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.duration._
        context.system.scheduler.schedule(1 seconds, 5 seconds) {
          Engine.current.queryEngineInfo.foreach {
            info =>
              log.info(Json.toJsonString(info))
              println(Json.toJsonString(info))
          }
          //
          //          Engine.current.queryEngineInfo.onComplete {
          //            case Success(info) =>
          //              try {
          //                println(Json.pretty(Json.toJsonNode(info)))
          //              } catch {
          //                case NonFatal(e) =>
          //                  e.printStackTrace()
          //              }
          //            case Failure(e) =>
          //              e.printStackTrace()
          //          }
        }

    }
  }
}
