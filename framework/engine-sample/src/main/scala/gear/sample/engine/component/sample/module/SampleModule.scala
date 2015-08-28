package gear.sample.engine.component.sample.module

import akka.actor._
import akka.routing.RandomRoutingLogic
import gear.sample.engine.plugin.SamplePlugin
import qgame.engine.component.Component
import qgame.engine.component.module.{ AbstractModule, Module, ModuleConfigPath }
import qgame.engine.core.Engine
import qgame.engine.core.serialization.EngineMessage

import scala.concurrent.Promise
import scala.language.postfixOps
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
@ModuleConfigPath(path = "sample-module") //@ServiceDependency(name = "ping")
class SampleModule extends AbstractModule {
  private var moduleActor: ActorRef = _

  override def doStart(completePromise: Promise[String]): Unit = {
    log.debug("DefaultModule on start")
    log.warning(context.config.getString("name").toString)
    moduleActor = context.actorOf(Props.create(classOf[SampleModuleActor]), "defaultModuleActor")
    //    moduleActor = context.actorOf(KafkaLoggingEventConsumer.props(
    //      brokerList = "127.0.0.1:9092",
    //      zooKeeperHost = "127.0.0.1:2181",
    //      topic = "test",
    //      groupId = "loggingEventConsumer",
    //      props = Props(new AbstractLoggingEventPumper {
    //        override def handleLoggingEvent(event: LoggingEvent): Unit = println(event)
    //      })
    //    ), "consumer")
    //    log.info("get value from plugin :" + Engine.current.plugin(classOf[SamplePlugin]).name)
    completePromise.trySuccess("wow")
  }

  override def doStop(promise: Promise[String]): Unit = {
    log.debug("DefaultModule on stop")
  }
}

class SampleModuleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case msg =>
      log.info("my path:{}", self.path)
      log.info("my path elements:{}", self.path.elements.mkString(","))
      log.info("my path without address :{}", self.path.toStringWithoutAddress)
      log.info("receive message :[{}] from :[{}]", msg, sender())
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("DefaultModuleActor pre start")
    Engine.current.clusterEventStream.subscribe(self, "topic")
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    context.system.scheduler.scheduleOnce(2.seconds) {
      Engine.current.clusterEventStream.publish("topic", "message :i am from event stream")
    }
    Engine.current.serviceRegistry.lookupAsync("ping", 2.seconds).map(_.head).foreach(
      service =>
        service.endPoint.!("ping")(self)
    )
    log.info("get value from plugin :[{}] call (name) :[{}]", classOf[SamplePlugin], Engine.current.plugin(classOf[SamplePlugin]).name)
    val futureAgent = Engine.current.serviceBroker.forService("ping", "0.0.1", RandomRoutingLogic())
    futureAgent.onComplete {
      case Success(agent) =>
        println("###############################")
        println("service available")
        println("###############################")
        agent.ask("ping", 10.second).onComplete {
          case Success(any) =>
            log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~get message from agent :[{}]", any)
          case Failure(e) =>
            log.error(e, "error when get message from agent.")
        }
      case Failure(e) =>
        log.error(e, "error for service agent:[ping]")
    }
    log.debug(
      s"""
         |----------------------------------------
         |vai of
         |engine of :${Engine.of}
         |component of :${Component.of}
         |module of :${Module.of}
         |----------------------------------------
      """.stripMargin
    )
    super.preStart()
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("DefaultModuleActor post stop")
    super.postStop()
  }
}

object SampleModuleActor {

  case class TickInfo(info: String) extends EngineMessage

  case object Tick extends EngineMessage

}
