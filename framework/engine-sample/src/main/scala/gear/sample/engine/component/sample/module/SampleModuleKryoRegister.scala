package gear.sample.engine.component.sample.module

import com.esotericsoftware.kryo.Kryo
import gear.sample.engine.component.sample.module.SampleModuleActor.Tick
import qgame.akka.serialization.kryo.KryoClassRegister
import qgame.akka.serialization.kryo.serializer.ScalaObjectSerializer

/**
 * Created by kerr.
 */
class SampleModuleKryoRegister extends KryoClassRegister {
  override def register(kryo: Kryo): Unit = {
    kryo.register(Tick.getClass, new ScalaObjectSerializer[Tick.type](Tick))
  }
}