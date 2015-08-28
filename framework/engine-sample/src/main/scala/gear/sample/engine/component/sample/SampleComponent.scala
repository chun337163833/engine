package gear.sample.engine.component.sample

import gear.sample.engine.component.sample.module.SampleModule
import qgame.akka.extension.quartz.JobIdentity
import qgame.engine.component.Component.{ ModuleBinder, ModuleBinding }
import qgame.engine.component.module.ModuleProps
import qgame.engine.component.{ AbstractComponent, ComponentConfigPath }
import qgame.engine.config.QGameConfig

/**
 * Created by kerr.
 */
@ComponentConfigPath(path = "sample")
class SampleComponent extends AbstractComponent {

  override protected def preStart(): Unit = {
    log.debug("pre start simple component ~~~")
    import qgame.engine.executionContext

    import scala.concurrent.duration._
    context.engine.jobScheduler.scheduleOnce(identity = JobIdentity("test", "sample", "sample"), 2.seconds) {
      println("#######################")
      println("#######################")
      println("#######################")
      println("test~~~~job scheduler")
      println("#######################")
      println("#######################")
      engine.serviceRegistry.lookupAsync("ping", 2.seconds).foreach {
        set =>
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println("~~~~~~~~~~~~~~~~~~~~~~~")
          println(set)
      }
    }

    super.preStart()
  }

  override protected def postStop(): Unit = {
    log.debug("post stop simple component ~~~")
    super.postStop()
  }

  override protected def loadModuleBindings(binder: ModuleBinder, config: QGameConfig): List[ModuleBinding] = {
    binder.bind("sample-module", ModuleProps.create(classOf[SampleModule]), isRequired = true)
      .build
  }
}
