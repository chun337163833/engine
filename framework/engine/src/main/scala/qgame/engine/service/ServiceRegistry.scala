package qgame.engine.service

import java.util.{ UUID, Optional }
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.ClusterEvent.{ CurrentClusterState, LeaderChanged }
import akka.cluster.ddata.ORMultiMapKey
import akka.cluster.{ Cluster, ClusterEvent }
import akka.serialization.Serialization
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import qgame.engine.core.Engine.LifeTime
import qgame.engine.libs.json.serializer.ActorRefJsonSerializer
import qgame.engine.libs.{ FutureBridge, Futures }
import qgame.engine.service.DefaultServiceRegistry.ServiceRegistryActor
import qgame.engine.service.DefaultServiceRegistry.ServiceRegistryActor._
import qgame.engine.service.ServiceRegistry.{ EmptyService, Service }
import qgame.engine.util.Version

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise }
import scala.language.postfixOps

/**
 * Created by kerr.
 */
trait ServiceRegistry {
  def registryLocally(service: Service): Future[Service]

  def registerAsync(service: Service, timeout: FiniteDuration): Future[Service]

  def registerAsync(service: Service, timeout: Long, timeUnit: TimeUnit): Future[Service] = {
    registerAsync(service, FiniteDuration(timeout, timeUnit))
  }

  def lookupLocally(name: String): Future[Set[Service]]

  def lookupAsync(name: String, timeout: FiniteDuration): Future[Set[Service]]

  def lookupAsync(name: String, timeout: Long, timeUnit: TimeUnit): Future[Set[Service]] = {
    lookupAsync(name, FiniteDuration(timeout, timeUnit))
  }

  def unregisterLocally(service: Service): Future[Service]

  def queryServicesLocally: Future[Map[String, Set[Service]]]

  def queryServicesAsync(timeout: FiniteDuration): Future[Map[String, Set[Service]]]
}

object ServiceRegistryHelper {
  import qgame.engine.executionContext
  def takeOne(services: Future[Set[Service]]): FutureBridge[Optional[Service]] = {
    Futures.wrap(services.map(_.headOption).collect {
      case Some(v) => Optional.of(v)
      case None => Optional.empty[Service]()
    })
  }

  def asJava(services: Future[Set[Service]]): FutureBridge[java.util.Set[Service]] = {
    import scala.collection.JavaConverters._
    Futures.wrap(services.map(_.asJava))
  }
}

private[service] class DefaultServiceRegistry(system: ActorSystem) extends ServiceRegistry with LifeTime {
  private var registryActor: ActorRef = _

  override def start(): Unit = {
    registryActor = system.actorOf(Props.create(classOf[ServiceRegistryActor]), "serviceRegistry")
  }

  override def stop(): Unit = {
    registryActor ! PoisonPill
  }

  override def registryLocally(service: Service): Future[Service] = {
    val promise = Promise[Service]()
    registryActor ! RegisterServiceLocally(service, promise)
    promise.future
  }

  override def registerAsync(service: Service, timeout: FiniteDuration): Future[Service] = {
    val promise = Promise[Service]()
    registryActor ! RegisterServiceAsync(service, promise, timeout)
    promise.future
  }

  override def lookupLocally(name: String): Future[Set[Service]] = {
    val promise = Promise[Set[Service]]()
    registryActor ! LookUpServiceLocally(name, promise)
    promise.future
  }

  override def lookupAsync(name: String, timeout: FiniteDuration): Future[Set[Service]] = {
    val promise = Promise[Set[Service]]()
    registryActor ! LookUpServiceAsync(name, promise, timeout)
    promise.future
  }

  override def unregisterLocally(service: Service): Future[Service] = {
    val promise = Promise[Service]()
    registryActor ! UnregisterServiceLocally(service, promise)
    promise.future
  }

  override def queryServicesLocally: Future[Map[String, Set[Service]]] = {
    val promise = Promise[Map[String, Set[Service]]]()
    registryActor ! QueryServiceLocally(promise)
    promise.future
  }

  override def queryServicesAsync(timeout: FiniteDuration): Future[Map[String, Set[Service]]] = {
    val promise = Promise[Map[String, Set[Service]]]()
    registryActor ! QueryServiceAsync(promise, timeout)
    promise.future
  }
}

private[service] object DefaultServiceRegistry {
  def apply(actorSystem: ActorSystem) = new DefaultServiceRegistry(actorSystem)

  private[service] class ServiceRegistryActor extends Actor with ActorLogging {

    import akka.cluster.ddata.Replicator._
    import akka.cluster.ddata.{ DistributedData, ORMultiMap }

    private implicit val cluster: Cluster = Cluster(context.system)
    private var registryLocalStore: Map[String, Set[Service]] = Map.empty.withDefault(key => Set.empty[Service])
    private val replicator = DistributedData(context.system).replicator

    private var registeringPromiseCallback: Map[String, (Promise[Service], Service)] = Map.empty.withDefault(hint =>
      (Promise.failed[Service](ServiceRegistryException(s"$hint not found for registering promise")), EmptyService))

    private var lookingUpPromiseCallback: Map[String, (Promise[Set[Service]], String)] = Map.empty.withDefault(hint =>
      (Promise.failed[Set[Service]](ServiceRegistryException(s"$hint not found for looking up promise")), "unknown"))

    private var queryingPromiseCallback: Map[String, Promise[Map[String, Set[Service]]]] = Map.empty.withDefault(hint =>
      Promise.failed[Map[String, Set[Service]]](ServiceRegistryException(s"$hint not found for query all service promise")))

    private var isLeader: Boolean = false
    private var serviceLookingBack: Map[ActorRef, Service] = Map.empty

    private def cacheRegisteringPromise(promise: Promise[Service], service: Service) = {
      val hint = UUID.randomUUID().toString
      registeringPromiseCallback = registeringPromiseCallback.updated(hint, (promise, service))
      hint
    }

    private def cacheLookingupPromise(promise: Promise[Set[Service]], name: String) = {
      val hint = UUID.randomUUID().toString
      lookingUpPromiseCallback = lookingUpPromiseCallback.updated(hint, (promise, name))
      hint
    }

    private def cacheQueryingPromise(promise: Promise[Map[String, Set[Service]]]) = {
      val hint = UUID.randomUUID().toString
      queryingPromiseCallback = queryingPromiseCallback.updated(hint, promise)
      hint
    }

    override def receive: Receive = {
      case RegisterServiceLocally(service, promise) =>
        log.debug("registering service locally :\n{}", service)
        replicator ! Update(registryKey, WriteLocal, Some(cacheRegisteringPromise(promise, service))) {
          case Some(v) => v.addBinding(service.name, service)
          case None => ORMultiMap.empty[Service]
        }
      case RegisterServiceAsync(service, promise, timeout) =>
        log.debug("registering service async :\n{}", service)
        replicator ! Update(registryKey, ORMultiMap.empty[Service], WriteAll(timeout), Some(cacheRegisteringPromise(promise, service)))(_.addBinding(service.name, service))

      case UpdateSuccess(`registryKey`, callbackHint: Option[String] @unchecked) =>
        callbackHint match {
          case Some(hint) =>
            registeringPromiseCallback.get(hint).foreach {
              case (promise, service) =>
                log.debug("update service success :\n{}", service)
                serviceLookingBack = serviceLookingBack.updated(service.endPoint, service)
                promise.trySuccess(service)
            }
            registeringPromiseCallback -= hint
          case None =>
        }
        replicator ! FlushChanges
      case UpdateTimeout(`registryKey`, callbackHint: Option[String] @unchecked) =>
        callbackHint match {
          case Some(hint) =>
            registeringPromiseCallback.get(hint).foreach {
              case (promise, service) =>
                log.error("update service timeout :\n{}", service)
                promise.tryFailure(ServiceRegistryException("time out when replicate to all the cluster node,the registry maybe ok :)"))
            }
            registeringPromiseCallback -= hint
          case None =>
        }
        replicator ! FlushChanges
      case ModifyFailure(`registryKey`, errorMessage, cause, callbackHint: Option[String] @unchecked) =>
        log.error(cause, "modify failed for key :[{}],error message :[{}] with hint :[{}]", `registryKey`, errorMessage, callbackHint)
        callbackHint match {
          case Some(hint) =>
            registeringPromiseCallback.get(hint).foreach {
              case (promise, service) =>
                log.debug("update service success :\n{}", service)
                serviceLookingBack = serviceLookingBack.updated(service.endPoint, service)
                promise.trySuccess(service)
            }
            registeringPromiseCallback -= hint
          case None =>
        }

      case GetFailure(`registryKey`, callbackHint: Option[String] @unchecked) => callbackHint match {
        case Some(hint) =>
          registeringPromiseCallback.get(hint).foreach {
            case (promise, service) =>
              log.debug("read service info failure :\n{}", service)
              promise.tryFailure(ServiceRegistryException("failure occur when reading the registry data,try later :("))
              registeringPromiseCallback -= hint
          }
          lookingUpPromiseCallback.get(hint).foreach {
            case (promise, name) =>
              log.debug("retrieving service info from cluster failed for name :{}", name)
              promise.tryFailure(ServiceRegistryException(s"looking up exception for the service :$name,please check it later or make sure the key is ok :)"))
          }
          lookingUpPromiseCallback -= hint
        case None =>
      }

      case LookUpServiceLocally(name, promise) =>
        registryLocalStore.get(name) match {
          case Some(values) =>
            log.debug("looking up service locally for name:{}", name)
            promise.trySuccess(values)
          case None =>
            log.error("looking up service locally error,not found for name:{}", name)
            promise.failure(ServiceRegistryException(s"no service find locally for the name $name,you may try the async way"))
        }

      case LookUpServiceAsync(name, promise, timeout) =>
        registryLocalStore.get(name) match {
          case Some(values) =>
            log.debug("looking up service async for name:{}", name)
            promise.trySuccess(values)
          case None =>
            log.debug("looking up service async ,not found locally,retrieving from cluster for name :{}", name)
            replicator ! Get(registryKey, ReadAll(timeout), Some(cacheLookingupPromise(promise, name)))
        }

      case QueryServiceLocally(promise) =>
        promise.trySuccess(registryLocalStore)

      case QueryServiceAsync(promise, timeout) =>
        log.debug("query all service async.")
        replicator ! Get(registryKey, ReadAll(timeout), Some(cacheQueryingPromise(promise)))

      case get @ GetSuccess(`registryKey`, callbackHint: Option[String] @unchecked) =>
        updateLocalCache(get.get(`registryKey`))
        callbackHint match {
          case Some(hint) =>
            //
            lookingUpPromiseCallback.get(hint).foreach {
              case (promise, name) =>
                log.debug("retrieving service info from cluster ok for name :{}", name)
                registryLocalStore.get(name) match {
                  case Some(values) =>
                    promise.trySuccess(values)
                  case None =>
                    promise.trySuccess(Set.empty[Service])
                }
                lookingUpPromiseCallback -= hint
            }
            //for the query all
            queryingPromiseCallback.get(hint).foreach {
              case promise =>
                log.debug("retrieving service info from cluster ok for query all service.")
                promise.trySuccess(registryLocalStore)
                queryingPromiseCallback -= hint
            }
          case None =>
        }

      case NotFound(key, callbackHint: Option[String] @unchecked) => callbackHint match {
        case Some(hint) =>
          lookingUpPromiseCallback.get(hint).foreach {
            case (promise, name) =>
              log.debug("retrieving service info from cluster failed for name :{}", name)
              promise.tryFailure(ServiceRegistryException(s"looking up exception [NotFound]for the service :$name,please check it later or make sure the key is ok :)"))
          }
          lookingUpPromiseCallback -= hint
        case None =>
      }

      case UnregisterServiceLocally(service, promise) =>
        registryLocalStore.get(service.name) match {
          case Some(values) =>
            log.debug("unregister service locally :\n{}", service)
            replicator ! Update(registryKey, ORMultiMap.empty[Service], WriteLocal) {
              data =>
                registryLocalStore = registryLocalStore.updated(service.name, values - service)
                promise.trySuccess(service)
                serviceLookingBack -= service.endPoint
                data.removeBinding(service.name, service)
            }
          case None =>
            log.debug("unregister service locally failed cause not found :\n{}", service)
            promise.tryFailure(ServiceRegistryException(s"could not found the service: $service in local cache to unregister"))
        }
      case c @ Changed(`registryKey`) =>
        val data = c.get(`registryKey`)
        //update local store
        log.debug("service changed :{}", data)
        updateLocalCache(data)
      case DataDeleted(key) =>
        if (key == registryKey) {
          registryLocalStore = Map.empty.withDefault(key => Set.empty[Service])
        }
      case CurrentClusterState(_, _, _, leader, _) =>
        leader.foreach {
          addr =>
            isLeader = cluster.selfAddress == addr
            log.debug("current cluster's leader is :{},isLeader :{}", addr, isLeader)
        }
      case LeaderChanged(leader) =>
        val wasLeader = isLeader
        leader.foreach {
          addr =>
            isLeader = addr == cluster.selfAddress
            log.debug("leader changed,wasLeader :{},isLeader:{}", wasLeader, isLeader)
            if (wasLeader && !isLeader) {
              serviceLookingBack.keys.foreach(context.unwatch)
            } else {
              serviceLookingBack.keys.foreach(context.watch)
            }
        }
      case Terminated(serviceEndPoint) =>
        serviceLookingBack.get(serviceEndPoint).foreach {
          service =>
            log.debug("service terminated :\n {}", service)
            registryLocalStore.get(service.name).foreach {
              services =>
                registryLocalStore = registryLocalStore.updated(service.name, services - service)
            }
            serviceLookingBack = serviceLookingBack - serviceEndPoint
            replicator ! Update(registryKey, ORMultiMap.empty[Service], WriteLocal)(_.removeBinding(service.name, service))
        }
    }

    private def updateLocalCache(data: ORMultiMap[Service]): Unit = {
      registryLocalStore = data.entries.foldLeft(registryLocalStore) {
        case (local, (dataKey, services)) =>
          local.updated(dataKey, local.getOrElse(dataKey, Set.empty[Service]) ++ services)
      }
      serviceLookingBack = registryLocalStore.values.flatten.foldLeft(serviceLookingBack) {
        case (map, service) => map.updated(service.endPoint, service)
      }
    }

    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      log.debug("service registry actor started")
      super.preStart()
      replicator ! Subscribe(registryKey, self)
      import scala.concurrent.duration._
      replicator ! Get(registryKey, ReadAll(10 seconds))
      cluster.subscribe(self, ClusterEvent.InitialStateAsSnapshot, classOf[ClusterEvent.LeaderChanged])
    }

    @throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      log.debug("service registry actor started")
      super.postStop()
      cluster.unsubscribe(self)
    }
  }

  private[service] object ServiceRegistryActor {
    private val registryKey = ORMultiMapKey[Service]("registry")

    import scala.concurrent.duration._

    case class ServiceRegistryException(message: String) extends RuntimeException(message)

    sealed trait ServiceRegisterActorCommand extends NoSerializationVerificationNeeded

    case class RegisterServiceLocally(service: Service, promise: Promise[Service]) extends ServiceRegisterActorCommand

    case class RegisterServiceAsync(service: Service, promise: Promise[Service], timeout: FiniteDuration) extends ServiceRegisterActorCommand

    case class LookUpServiceLocally(name: String, promise: Promise[Set[Service]]) extends ServiceRegisterActorCommand

    case class LookUpServiceAsync(name: String, promise: Promise[Set[Service]], timeout: FiniteDuration) extends ServiceRegisterActorCommand

    case class UnregisterServiceLocally(service: Service, promise: Promise[Service]) extends ServiceRegisterActorCommand

    case class QueryServiceLocally(promise: Promise[Map[String, Set[Service]]]) extends ServiceRegisterActorCommand

    case class QueryServiceAsync(promise: Promise[Map[String, Set[Service]]], timeout: FiniteDuration) extends ServiceRegisterActorCommand

  }
}

object ServiceRegistry {

  def apply(actorSystem: ActorSystem) = new DefaultServiceRegistry(actorSystem)

  sealed trait ServiceRegistryCommand

  sealed trait ServiceRegistryResponse

  sealed abstract class Service {
    def endPoint: ActorRef

    def name: String

    def version: Version

    def description: String
  }

  object Service {
    def create(
      endPoint: ActorRef,
      name: String,
      version: Version,
      description: String
    ): Service = DefaultService(endPoint, name, version, description)
    def create(
      endPoint: ActorRef,
      name: String,
      version: String,
      description: String
    ): Service = create(endPoint, name, Version(version), description)

    def empty: Service = EmptyService

  }

  case class DefaultService(
      @JsonSerialize(using = classOf[ActorRefJsonSerializer]) endPoint: ActorRef,
      name: String,
      version: Version,
      description: String
  ) extends Service {
    require(endPoint != null)
    require(name != null)
    require(version != null)
    require(description != null)

    def this(
      endPoint: ActorRef,
      name: String,
      version: Version
    ) = this(endPoint, name, version, "")

    override def toString: String = {
      s"""{
         |  name        : $name
         |  endPoint    : ${Serialization.serializedActorPath(endPoint)}
         |  version     : $version
         |  description : $description
         |}
       """.stripMargin
    }
  }

  case object EmptyService extends Service {
    override def description: String = {
      throw new IllegalStateException("this is an empty service")
    }

    override def name: String = {
      throw new IllegalStateException("this is an empty service")
    }

    override def endPoint: ActorRef = {
      throw new IllegalStateException("this is an empty service")
    }

    override def version: Version = {
      throw new IllegalStateException("this is an empty service")
    }
  }

  trait ServiceWatcherCallback {
    def serviceDown(service: Service)
  }

}
