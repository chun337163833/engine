package gear.sample.engine.plugin

import gear.sample.engine.plugin.SamplePlugin.SamplePluginAPI
import qgame.engine.plugin.AbstractPlugin

class SamplePlugin extends AbstractPlugin[SamplePluginAPI] {

  override protected def preStart(): Unit = {
    log.debug("loading sample plugin :[{}]", this.getClass)
    super.preStart()
  }

  override protected def postStop(): Unit = super.postStop()

  override def instance: SamplePluginAPI = new SamplePluginAPI {
    override def name: String = "haha"
  }
}

object SamplePlugin {
  trait SamplePluginAPI {
    def name: String
  }
}
