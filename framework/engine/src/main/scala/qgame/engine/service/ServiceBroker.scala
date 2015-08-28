package qgame.engine.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor._
import akka.routing.{ ActorRefRoutee, Router, RoutingLogic }
import qgame.engine.core.Engine.{ EngineInnerMessage, LifeTime }
import qgame.engine.core.{ Engine, EngineContext }
import qgame.engine.libs.LoggingAble
import qgame.engine.service.ServiceBroker.Agent.{ Ask, Send }
import qgame.engine.service.ServiceBroker.ServiceAgent.ServiceAgentException
import qgame.engine.service.ServiceRegistry.Service
import qgame.engine.util.Version

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
class ServiceBroker(engine: Engine, context: EngineContext) extends LifeTime with LoggingAble {

  import ServiceBroker.Broker._
  import ServiceBroker._

  private final var broker: ActorRef = _

  override private[engine] def start(): Unit = broker = context.actorOf(Props.create(classOf[Broker]), "service-broker")

  override private[engine] def stop(): Unit = if (broker ne null) broker ! PoisonPill

  //TODO provide ha and autoRetrieve

  def forService(name: String, version: String, logic: RoutingLogic): Future[ServiceAgent] =
    forService(name, version, logic, FiniteDuration(10, TimeUnit.SECONDS))

  def forService(name: String, version: String, logic: RoutingLogic, timeOut: FiniteDuration): Future[ServiceAgent] =
    forService(name, Version(version), logic)(timeOut)

  def forService(name: String, version: Version, logic: RoutingLogic)(implicit timeout: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS)): Future[ServiceAgent] = {
    require(name ne null)
    require(logic ne null)
    val promise = Promise[ServiceAgent]()
    broker ! RetrieveServiceAgent(name, version, engine, logic, promise, timeout)
    promise.future
  }
}

object ServiceBroker {

  private[service] class Broker extends Actor with ActorLogging {

    import Broker._

    override def receive: Receive = {
      case RetrieveServiceAgent(name, version, engine, logic, promise, timeOut) =>
        log.debug("retrieve service agent for name:[{}] version :[{}]", name, version)
        context.actorOf(Agent.props(name, version, engine, logic, promise, timeOut))
    }

  }

  private[service] object Broker {

    sealed trait BrokerCommand extends EngineInnerMessage

    case class RetrieveServiceAgent(
      name: String,
      version: Version,
      engine: Engine,
      logic: RoutingLogic,
      promise: Promise[ServiceAgent],
      timeout: FiniteDuration
    ) extends BrokerCommand

  }

  abstract class ServiceAgent {
    def isAvailable: Boolean

    def send(msg: Any, sender: ActorRef) = this.!(msg)(sender)

    def !(msg: Any)(implicit sender: ActorRef = ActorRef.noSender): Unit

    def ask(msg: Any, timeout: FiniteDuration): Future[Any]

    def ask(msg: Any, timeout: Int, timeUnit: TimeUnit): Future[Any] = ask(msg, FiniteDuration(timeout, timeUnit))
  }

  object ServiceAgent {

    class ServiceAgentException(message: String) extends RuntimeException(message)

  }

  private class DefaultServiceAgent(name: String, version: Version, available: AtomicBoolean, relay: ActorRef) extends ServiceAgent {

    override def isAvailable: Boolean = available.get()

    @throws(classOf[ServiceAgentException])
    override def !(msg: Any)(implicit sender: ActorRef): Unit = {
      if (!available.get()) {
        Future.failed(new ServiceAgentException(s"no service available for name :[$name],version :[$version]"))
      } else {
        relay ! Send(msg, sender)
      }
    }

    override def ask(msg: Any, timeout: FiniteDuration): Future[Any] = {
      if (!available.get()) {
        Future.failed(new ServiceAgentException(s"no service available for name :[$name],version :[$version]"))
      } else {
        val promise = Promise[Any]()
        relay ! Ask(msg, timeout, promise)
        promise.future
      }
    }
  }

  private class Agent(
      name: String,
      version: Version,
      engine: Engine,
      logic: RoutingLogic,
      promise: Promise[ServiceAgent],
      timeout: FiniteDuration
  ) extends Actor with ActorLogging {

    import Agent._

    private var services: Map[ActorRef, Service] = Map.empty

    private var router: Router = _
    private final val available = new AtomicBoolean(false)

    override def receive: Actor.Receive = {
      case Ask(msg, askTimeOut, askPromise) =>
        if (router.routees.isEmpty) {
          askPromise.tryFailure(new ServiceAgentException(s"no service available for name :[$name],version :[$version]"))
        } else {
          val ref = router.logic.select(msg, router.routees).asInstanceOf[ActorRefRoutee].ref
          //todo upgrade to promise actorRef
          askPromise.tryCompleteWith(akka.pattern.ask(ref, msg)(askTimeOut))
        }
      case Send(msg, sender) =>
        router.route(msg, sender)
      case ServiceRetrieved(retrieved) =>
        log.info("services retrieved for engine :[{}] service :[{}] ,services :\n{}", engine.name, name, retrieved.mkString("\n"))
        val validServices = retrieved.filter(_.version >= version)
        validServices.foreach(service => context.watch(service.endPoint))

        services ++= validServices.map(service => (service.endPoint, service))
        //via router
        router = Router(logic, services.keys.toIndexedSeq.map(ActorRefRoutee.apply))
        if (services.isEmpty) {
          available.set(false)
          promise.tryFailure(new ServiceAgentException(s"services retrieved success,but could not found valid service for name:[$name] version :[$version]"))
        } else {
          available.set(true)
          promise.trySuccess(new DefaultServiceAgent(name, version, available, self))
        }
      case ServiceRetrieveFailed(e) =>
        log.error(e, "service retire failed for engine :[{}] service :[{}]", engine.name, name)
        available.set(false)
        promise.tryFailure(new ServiceAgentException(s"services retrieve failed for name:[$name] version :[$version],cause :[${e.getMessage}}]"))
        context.stop(self)
      case Terminated(actorRef) =>
        services.get(actorRef).foreach {
          service =>
            log.debug("service terminated :[{}]", service)
            services -= actorRef
            router.removeRoutee(actorRef)
        }
        context.unwatch(actorRef)
        if (services.isEmpty) {
          available.set(false)
          log.warning("there is no service available for service name :[{}] version :[{}]", name, version)
          context.stop(self)
        }
    }
    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      log.debug("service reply for engine :[{}] service :[{}] version :[{}] started.", engine.name, name, version)
      import qgame.engine.executionContext
      engine.serviceRegistry.lookupAsync(name, timeout).onComplete {
        case Success(retrieved) =>
          self ! ServiceRetrieved(retrieved)
        case Failure(e) =>
          self ! ServiceRetrieveFailed(e)
      }
      super.preStart()
    }

    @throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      log.warning("service reply for engine :[{}] service :[{}] version :[{}] stopped.", engine.name, name, version)
      super.postStop()
    }

  }

  object Agent {

    def props(
      name: String,
      version: Version,
      engine: Engine,
      logic: RoutingLogic,
      promise: Promise[ServiceAgent], timeout: FiniteDuration
    ) = Props(new Agent(name, version, engine, logic, promise, timeout))

    sealed trait AgentCommand extends EngineInnerMessage

    case class Ask(msg: Any, askTimeOut: FiniteDuration, promise: Promise[Any]) extends AgentCommand

    case class Send(msg: Any, sender: ActorRef) extends AgentCommand

    private case class ServiceRetrieved(services: Set[Service])

    private case class ServiceRetrieveFailed(e: Throwable)

  }

}