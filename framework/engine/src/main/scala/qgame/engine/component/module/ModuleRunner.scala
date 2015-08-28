package qgame.engine.component.module

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import qgame.engine.component.module.Module.ModuleInitializingException
import qgame.engine.component.module.ModuleContext.DefaultModuleContext
import qgame.engine.component.module.ModuleRunner._
import qgame.engine.component.module.ServiceDependency.RequiredPolicy
import qgame.engine.component.{ Component, ComponentConfigPath }
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine
import qgame.engine.core.Engine.EngineInnerMessage
import qgame.engine.logging.DebugInfo
import qgame.engine.util.Version

import scala.concurrent.{ Await, Future, Promise }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[module] class ModuleRunner(
    engine: Engine,
    component: Component,
    name: String,
    moduleProps: ModuleProps
) extends Actor with ActorLogging with DebugInfo {
  private var currentModule: Module = _

  private val serviceDependencies = moduleProps.moduleClass().getAnnotationsByType(classOf[ServiceDependency])

  override lazy val debugInfo: String = {
    s"""
       |+--------------------------------------------
       ||                Module Runner
       |+--------------------------------------------
       ||                  Component
       |+--------------------------------------------
       ||clazz:[${component.getClass}]
                                       |+--------------------------------------------
                                       ||                   Module
                                       |+--------------------------------------------
                                       ||name:[$name]
                                                      ||clazz:[${moduleProps.moduleClass()}]
                                                                                             ||serviceDependencies:${if (serviceDependencies.isEmpty) "[]" else s"[\n${serviceDependencies.mkString("  -", "\n", ",")}]"}
        |+--------------------------------------------
     """.stripMargin
  }

  override def receive: Receive = {
    case StartModule =>
      val promise = Promise[String]()
      val resolvingFuture = resolveServiceDependency()
      context.become(waitingServiceDependencyResolved(promise))
      resolvingFuture.onComplete {
        case Success(info) =>
          self ! ServiceDependencyResolveSuccess(info)
        case Failure(e) =>
          self ! ServiceDependencyResolveFailed(e)
      }

      promise.future.onComplete {
        case Success(info) =>
          self ! ModuleStartSuccess(info)
        case Failure(e) =>
          self ! ModuleStartFailed(e)
      }
  }

  private def resolveServiceDependency(): Future[String] = {
    //check the annotation for service dependency
    val promise = Promise[String]()
    try {
      if (!moduleProps.moduleClass().isAnnotationPresent(classOf[ServiceDependency])) {
        log.warning("there is no service dependency annotation for module :{},will not retrieve any dependency.", moduleProps.moduleClass())
        promise.trySuccess("no service dependency need to resolve")
      } else {
        log.debug(
          """
            |module :{}
            |service dependencies:
            |{}
          """.stripMargin, moduleProps.moduleClass(), serviceDependencies.mkString("\n")
        )
        val requiredServices = serviceDependencies.filter(_.scope() == ServiceDependency.Scope.REQUIRED)
        log.debug("checking service dependency")
        import scala.concurrent.duration._
        def checkService(retryCount: Int = -1, serviceDependency: ServiceDependency): Unit = {
          log.debug("checking :{},retryCount :{}", serviceDependency, retryCount)
          try {
            val future = engine.serviceRegistry.lookupAsync(serviceDependency.name(), serviceDependency.maxWaitInSeconds().seconds)
            //TODO kick out this blocking code
            val services = Await.result(future, serviceDependency.maxWaitInSeconds().seconds)
            log.debug("found service for dependency :{}\n :{}", serviceDependencies, services.mkString("\n"))
            if (services.exists(_.version >= Version(serviceDependency.version()))) {
              log.debug("version check pass")
            } else {
              throw new ServiceDependencyException(s"could not found one service for name :${serviceDependency.name()},version >=:${serviceDependency.version()}")
            }
          } catch {
            case NonFatal(e) =>
              log.error(e, "error when checking service :{} \nfor module :{}", serviceDependency, moduleProps.moduleClass())
              serviceDependency.requirePolicy() match {
                case RequiredPolicy.RETRY =>
                  if (retryCount == -1) {
                    val maxRetryCount = serviceDependency.maxRetry()
                    if (maxRetryCount < 0) {
                      throw new IllegalArgumentException(s"maxRetryCount value $maxRetryCount in annotation ServiceDependency for module :${moduleProps.moduleClass()}" +
                        " is not right,could not < 0")
                    } else if (maxRetryCount == 0) {
                      throw new ModuleInitializingException(s"maxRetryCount ${serviceDependency.maxRetry()} is set to 0 for service ${serviceDependency.name()}" +
                        s",in module :${moduleProps.moduleClass()}")
                    }
                    checkService(maxRetryCount - 1, serviceDependency)
                  } else if (retryCount == 0) {
                    throw new ModuleInitializingException(s"maxRetryCount ${serviceDependency.maxRetry()} is acced to 0 for service ${serviceDependency.name()}" +
                      s",in module :${moduleProps.moduleClass()}")
                  } else {
                    checkService(retryCount - 1, serviceDependency)
                  }
                case RequiredPolicy.FAIL =>
                  throw new ModuleInitializingException(s"retrieving required service :${serviceDependency.name()} failed for module :${moduleProps.moduleClass()},using policy fail")
                case RequiredPolicy.IGNORE =>
                  log.debug("retrieving required service :{} failed for module :{},using policy ignore", serviceDependency.name(), moduleProps.moduleClass())
              }
          }
        }
        requiredServices.foreach {
          requiredService =>
            //TODO make the resolving parallelism
            checkService(serviceDependency = requiredService)
        }
        log.debug(s"resolving service dependencies :[{}] for module :[{}] of component :[{}] successfully.", requiredServices.mkString(","), name, component.getClass)
        promise.trySuccess("resolving services successfully.")
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, "resolving service dependency for module :[{}] of component :[{}] failed.", name, component.getClass)
        promise.tryFailure(e)
    }
    promise.future
  }

  private def waitingServiceDependencyResolved(promise: Promise[String]): Receive = {
    case ServiceDependencyResolveSuccess(info) =>
      context.become(startingModule(promise))
      self ! TriggerStartingModule
    case ServiceDependencyResolveFailed(e) =>
      promise.tryFailure(e)
  }

  private def startingModule(promise: Promise[String]): Receive = {
    case TriggerStartingModule =>
      val startFuture = startModule()
      startFuture.onComplete {
        case Success(info) =>
          self ! ModuleStartSuccess(info)
        case Failure(e) =>
          self ! ModuleStartFailed(e)
      }
      context.become(waitingModuleStarted)
  }

  private def startModule(): Future[String] = {
    try {
      val config = readModuleConfig
      log.debug("config for module :[{}] of component :[{}] is :\n{}", name, component, config.underlying.root().render())
      val moduleContext = DefaultModuleContext(moduleProps.moduleClass(), context, engine, component, config, name)
      //in the stack
      ModuleContext.stack.withValue(moduleContext :: ModuleContext.stack.value) {
        currentModule = Module.cacheModule(self.path.name, moduleProps.newModule())
        //here module maybe create the child actor
        currentModule.aroundPreStart()
        currentModule.start()
      }
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  private def readModuleConfig: QGameConfig = {
    //change to the same as component,which will not force to annotation with ModuleConfigPath

    val clazz = moduleProps.moduleClass()
    if (!clazz.isAnnotationPresent(classOf[ModuleConfigPath])) {
      //      throw new IllegalArgumentException(s"a module implement ${moduleProps.moduleClass().getName} must annotation with a ModuleConfigPath annotation")
      log.warning("module: [{}] without annotation of ModuleConfigPath will receive an empty config. ")
    }
    val config = clazz.getAnnotationsByType(classOf[ModuleConfigPath]).headOption.map(_.path()) match {
      case Some(v) =>
        if (v == "") {
          log.warning("config path for module :[{}] is empty,so default empty config is used.", clazz)
          QGameConfig.empty
        } else {
          val componentConfigPath = component.getClass.getAnnotationsByType(classOf[ComponentConfigPath]).headOption.map(_.path()).orNull
          component.config.getConfig(s"modules.$v").getOrElse(
            throw new IllegalArgumentException(s"there is no config section for module :[$clazz}] in [engine.components.$componentConfigPath.modules}] section with value [$v].")
          )
        }
      case None =>
        log.warning("module :[{}] are not annotated with ModuleConfigPath,and will receive an empty config.", clazz)
        QGameConfig.empty
    }
    config
  }

  private def waitingModuleStarted: Receive = {
    case msg @ ModuleStartSuccess(info) =>
      log.debug("module :[{}] for component :[{}] started successfully.", name, component.getClass)
      context.parent ! msg
      context.become(whenModuleStartedSuccessfully)
    case msg @ ModuleStartFailed(e) =>
      log.error(e, "module :[{}] for component :[{}] start failed.", name, component.getClass)
      context.parent ! msg
      context.become(whenModuleStartFailed(e))
  }

  private def whenModuleStartedSuccessfully: Receive = {
    case StopModule =>
      log.debug("module :[{}] clazz :[{}] of component :[{}] was started successfully,request to stop", name, moduleProps.moduleClass(), component.getClass)
      val stopFuture = stopModule()
      stopFuture.onComplete {
        case Success(info) =>
          self ! ModuleStopSuccess(info)
        case Failure(stopError) =>
          self ! ModuleStopFailed(stopError)
      }
      context.become(waitingModuleStopped)
  }

  private def whenModuleStartFailed(e: Throwable): Receive = {
    case StopModule =>
      log.debug("module :[{}] clazz :[{}] of component :[{}] was start failed cause :[{}],request to stop", name, moduleProps.moduleClass(), component.getClass, e.getMessage)
      val stopFuture = stopModule()
      stopFuture.onComplete {
        case Success(info) =>
          self ! ModuleStopSuccess(info)
        case Failure(stopError) =>
          self ! ModuleStopFailed(stopError)
      }
      context.become(waitingModuleStopped)
  }

  private def stopModule(): Future[String] = {
    try {
      if (currentModule ne null) {
        currentModule.stop()
      } else {
        Future.failed(new IllegalStateException(s"module :[$name] for component :[${component.getClass}] have not initialized."))
      }
    } catch {
      case NonFatal(e) =>
        Future.failed(e)
    }
  }

  private def waitingModuleStopped: Receive = {
    case ModuleStopSuccess(info) =>
      log.debug("module :[{}] for component :[{}] stopped successfully,info :[{}]", name, component.getClass, info)
      //TODO try catch,2,notify the parent
      currentModule.aroundPostStop()
      context.become(moduleStopSuccessfully)
    case ModuleStopFailed(e) =>
      log.error(e, "module :[{}] for component :[{}] stopped failed.", name, component.getClass)
      if (currentModule ne null) {
        currentModule.aroundPostStop()
      }
      context.become(moduleStopFailed(e))
  }

  private def moduleStopSuccessfully: Receive = {
    case msg =>
  }

  private def moduleStopFailed(e: Throwable): Receive = {
    case msg =>
  }

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy() {
      case e: ActorInitializationException ⇒
        log.error(e, "module actor initialization failed.")
        Stop
      case e: ActorKilledException ⇒
        log.error(e, "module actor killed.")
        Stop
      case e: DeathPactException ⇒ Stop
      case e: Exception ⇒
        log.error(e, "module actor unhandled exception.")
        Stop
    }
  }
}

private[module] object ModuleRunner {

  sealed trait ModuleRunnerCommand extends EngineInnerMessage

  case object StartModule extends ModuleRunnerCommand

  class ServiceDependencyException(message: String) extends RuntimeException(message)

  private case class ServiceDependencyResolveSuccess(info: String)

  private case class ServiceDependencyResolveFailed(e: Throwable)

  private case object TriggerStartingModule extends ModuleRunnerCommand

  case class ModuleStartSuccess(info: String)

  case class ModuleStartFailed(e: Throwable)

  case object StopModule extends ModuleRunnerCommand

  case class ModuleStopSuccess(info: String)

  case class ModuleStopFailed(e: Throwable)

}
