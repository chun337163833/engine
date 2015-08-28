package qgame.akka.serialization.kryo.serializer

import java.util.UUID

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class UUIDSerializer extends Serializer[UUID] {
  override def write(kryo: Kryo, output: Output, uuid: UUID): Unit = {
    output.writeLong(uuid.getMostSignificantBits)
    output.writeLong(uuid.getLeastSignificantBits)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[UUID]): UUID = {
    new UUID(input.readLong(), input.readLong())
  }
}
