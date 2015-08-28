package qgame.akka.serialization.kryo.serializer

import akka.actor.{ ActorRef, ExtendedActorSystem }
import akka.serialization.Serialization
import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class ActorRefSerializer(system: ExtendedActorSystem) extends Serializer[ActorRef] {
  override def write(kryo: Kryo, output: Output, actorRef: ActorRef): Unit = {
    output.writeString(Serialization.serializedActorPath(actorRef))
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[ActorRef]): ActorRef = {
    system.provider.resolveActorRef(input.readString())
  }
}
