package qgame.engine.libs.json.serializer

import akka.actor.ActorRef
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }

/**
 * Created by kerr.
 */
class ActorRefJsonSerializer extends JsonSerializer[ActorRef] {
  override def serialize(value: ActorRef, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(value.path.toSerializationFormat)
  }
}
