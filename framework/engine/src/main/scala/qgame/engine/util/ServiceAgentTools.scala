package qgame.engine.util

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.routing.RoundRobinRoutingLogic
import qgame.engine.core.Engine
import qgame.engine.service.ServiceBroker.ServiceAgent

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.FiniteDuration

/**
 * Tools for ServiceAgentTools
 * Created by DongLei on 2015/5/25.
 */
object ServiceAgentTools {

  var serviceAgentMap: Map[String, ServiceAgent] = Map.empty[String, ServiceAgent]

  def getServiceAgent(topic: String): ServiceAgent = {
    val optionAgent: Option[ServiceAgent] = serviceAgentMap.get(topic)
    if (optionAgent.isDefined) {
      val agent = optionAgent.get
      if (agent.isAvailable) {
        agent
      } else {
        try {
          val newAgent = Await.result(Engine.current.serviceBroker.forService(topic, "0.0.1", new RoundRobinRoutingLogic()), new FiniteDuration(10, TimeUnit.SECONDS))
          serviceAgentMap += (topic -> newAgent)
          return newAgent
        } catch {
          case ex: Exception =>
            ex.printStackTrace()
        }
        null
      }
    } else {
      try {
        val newAgent = Await.result(Engine.current.serviceBroker.forService(topic, "0.0.1", new RoundRobinRoutingLogic()), new FiniteDuration(10, TimeUnit.SECONDS))
        serviceAgentMap += (topic -> newAgent)
        return newAgent
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
      }
      null
    }
  }

  def sendToService(topic: String, message: Any, sender: ActorRef): Unit = {
    val agent: ServiceAgent = getServiceAgent(topic)
    agent.send(message, sender)
  }

  def askToService(topic: String, message: Any, timeout: FiniteDuration): Unit = {
    val agent: ServiceAgent = getServiceAgent(topic)
    Await.result(agent.ask(message, timeout), timeout)
  }

  def askToServiceFuture[R](topic: String, message: Any, timeOut: FiniteDuration): Future[R] = {
    val agent: ServiceAgent = getServiceAgent(topic)
    agent.ask(message, timeOut).asInstanceOf[Future[R]]
  }

}
