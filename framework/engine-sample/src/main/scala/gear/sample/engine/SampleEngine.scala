package gear.sample.engine

import akka.actor.{ Actor, Stash }
import qgame.engine.core.AbstractEngine
import qgame.engine.libs.WS
import qgame.engine.util.FlowControl

import scala.concurrent.Future

/**
 * Created by kerr.
 */
class SampleEngine extends AbstractEngine {

  override protected def preStart(): Unit = {
    super.preStart()
    log.debug("sample engine preStart")
    //    log.debug("DefaultEngine do start")
    //    val actor = context.actorOf(Props.create(classOf[DummyActor], this))
    //    import scala.concurrent.ExecutionContext.Implicits.global
    //    import scala.concurrent.duration._
    //    val future = serviceRegistry.registerAsync(DefaultService(actor, "default", Version(1, 1, 1), "this is a default service"), 2.seconds)
    //    future.foreach(
    //      service =>
    //        log.error("wow,service registered :{}", service)
    //    )
  }

  class DummyActor extends Actor with Stash with FlowControl {
    override def receive: Actor.Receive = {
      case "again" =>
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
      case "wahaha" =>
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        println("wahaha")
        flowExecution("again").mapAsync(Future.successful).foreach {
          data =>
            println("again once:" + data)
            flowExecution(data).mapAsync(Future.successful).foreach {
              innerData =>
                println("inner data :" + innerData)
                self ! innerData
            }
        }
      case msg: String =>
        import scala.concurrent.ExecutionContext.Implicits.global
        flowExecution(msg)
          .map(_.split(","))
          .map(_.toSeq)
          .mapAsync(addresses => Future.sequence(addresses.map(addr => Future.successful(s"http://$addr"))))
          .mapParallelism[Int, Int, String](
            address => WS.prepareGet(address.head).execute().map(_.getResponseBody.length),
            address => WS.prepareGet(address(1)).execute().map(_.getResponseBody.length)
          )((a, b) => s"a: $a ,b: $b").foreach { info =>
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              println(info)
              self ! "wahaha"
            }
    }

    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      self ! "baidu.com,qq.com"
      super.preStart()
    }
  }

}

