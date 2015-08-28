package qgame.engine.libs

import java.io.IOException
import java.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.{ ObjectNode, ArrayNode }
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.mutable.ArrayBuffer

/**
 * Created by kerr.
 */
/**
 *  https://github.com/FasterXML/jackson-annotations
 *  https://github.com/FasterXML/jackson-annotations#handling-polymorphic-types
 */
object Json {
  implicit class JsonAble(`object`: AnyRef) {
    def toJsonNode = Json.toJsonNode(`object`)
    def toJsonString = Json.toJsonString(`object`)
    def toJsonString(typeRef: TypeReference[_]) = Json.toJsonString(`object`, typeRef)
    def toPrettyJsonString = Json.pretty(Json.toJsonNode(`object`))
    def toJsonBytes = Json.toJsonBytes(`object`)
    def toJsonBytes(typeRef: TypeReference[_]) = Json.toJsonBytes(`object`, typeRef)
  }

  trait JsonLike[V] {
    def toJsonNode: JsonNode
    def toBean[T](clazz: Class[T]): T
    def toBean[T](typeRef: TypeReference[T]): T
  }

  implicit class StringJsonLike(value: String) extends JsonLike[String] {
    override def toBean[T](clazz: Class[T]): T = Json.toBean(value, clazz)

    override def toBean[T](typeRef: TypeReference[T]): T = Json.toBean(value, typeRef)

    override def toJsonNode: JsonNode = Json.toJsonNode(value)
  }

  implicit class BytesJsonLike(value: Array[Byte]) extends JsonLike[Array[Byte]] {
    override def toBean[T](clazz: Class[T]): T = Json.toBean(value, clazz)

    override def toBean[T](typeRef: TypeReference[T]): T = Json.toBean(value, typeRef)

    override def toJsonNode: JsonNode = Json.toJsonNode(value)
  }

  private final val mapper: ObjectMapper = {
    val objectMapper = new ObjectMapper
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    objectMapper.registerModule(DefaultScalaModule)
    objectMapper.registerModule(new Jdk8Module)
    objectMapper.registerModule(new JodaModule)
    objectMapper.registerModule(new JSR310Module)
    objectMapper.registerModule(new GuavaModule)
  }

  def pretty(jsonNode: JsonNode): String = {
    try {
      mapper.writerWithDefaultPrettyPrinter().asInstanceOf[ObjectWriter].writeValueAsString(jsonNode)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toBean[T](jsonNode: JsonNode, clazz: Class[T]): T = {
    try {
      mapper.treeToValue(jsonNode, clazz)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toBean[T](jsonStr: String, clazz: Class[T]): T = {
    try {
      mapper.readValue(jsonStr, clazz)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new JsonException(e)
    }
  }

  def toBean[T](jsonStr: String, typeRef: TypeReference[T]): T = {
    try {
      mapper.readValue(jsonStr, typeRef)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toBean[T](bytes: Array[Byte], clazz: Class[T]): T = {
    try {
      mapper.readValue(bytes, clazz)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toBean[T](bytes: Array[Byte], typeRef: TypeReference[T]): T = {
    try {
      mapper.readValue(bytes, typeRef)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toJsonNode(jsonStr: String): JsonNode = {
    try {
      mapper.readValue(jsonStr, classOf[JsonNode])
    } catch {
      case e: IOException =>
        throw new JsonException(e)
    }
  }

  def toJsonNode(bytes: Array[Byte]): JsonNode = {
    try {
      mapper.readValue(bytes, classOf[JsonNode])
    } catch {
      case e: IOException =>
        throw new JsonException(e)
    }
  }

  def toJsonNode(`object`: AnyRef): JsonNode = {
    try {
      mapper.valueToTree(`object`)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toJsonString(`object`: AnyRef): String = {
    try {
      mapper.writeValueAsString(`object`)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toJsonString(`object`: AnyRef, typeRef: TypeReference[_]): String = {
    try {
      mapper.writerFor(typeRef).asInstanceOf[ObjectWriter].writeValueAsString(`object`)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toJsonBytes(`object`: AnyRef): Array[Byte] = {
    try {
      mapper.writeValueAsBytes(`object`)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def toJsonBytes(`object`: AnyRef, typeRef: TypeReference[_]): Array[Byte] = {
    try {
      mapper.writerFor(typeRef).asInstanceOf[ObjectWriter].writeValueAsBytes(`object`)
    } catch {
      case e: Exception =>
        throw new JsonException(e)
    }
  }

  def objectNode: ObjectNode = {
    mapper.createObjectNode
  }

  def arrayNode: ArrayNode = {
    mapper.createArrayNode
  }

  def jsonNodeToMap(jsonNode: JsonNode): Map[String, Array[String]] = {
    var map: Map[String, Array[String]] = Map.empty[String, Array[String]]

    val iteratorKey: util.Iterator[String] = jsonNode.fieldNames()

    while (iteratorKey.hasNext) {
      val key: String = iteratorKey.next()
      var arrayBuffer: ArrayBuffer[String] = ArrayBuffer.empty[String]
      val iteratorValue: util.Iterator[JsonNode] = jsonNode.get(key).elements()
      while (iteratorValue.hasNext) {
        arrayBuffer += iteratorValue.next().asText()
      }
      map += (key -> arrayBuffer.toArray)
    }
    map
  }

  def jsonNodeToMapForJava(jsonNode: JsonNode): util.Map[String, Array[String]] = {
    import scala.collection.JavaConversions._
    jsonNodeToMap(jsonNode)
  }

  class JsonException(e: Exception) extends Exception(e)

}