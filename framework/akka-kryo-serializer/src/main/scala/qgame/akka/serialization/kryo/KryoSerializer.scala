package qgame.akka.serialization.kryo

import java.net.{ InetAddress, InetSocketAddress, URI }
import java.util
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

import akka.actor.{ ActorRef, ExtendedActorSystem }
import akka.serialization.Serializer
import com.esotericsoftware.kryo.io.{ Input, Output, UnsafeInput, UnsafeOutput }
import com.esotericsoftware.kryo.{ Kryo, KryoException }
import net.jpountz.lz4.LZ4Factory
import org.objenesis.strategy.StdInstantiatorStrategy
import qgame.akka.serialization.kryo.serializer._
import qgame.engine.config.QGameConfig
import qgame.engine.libs.Reflect

import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
class KryoSerializer(system: ExtendedActorSystem) extends Serializer {
  //we need to get the configuration here
  private val log = akka.event.Logging(system, classOf[KryoSerializer])
  private val underlyingKryoConfig = QGameConfig(system.settings.config).getConfig("akka.actor.kryo").getOrElse(
    throw new IllegalArgumentException("must setup akka.actor.kryo to using this serializer")
  )

  private val KryoConfig = KryoSerializerConfiguration(underlyingKryoConfig)

  private val serializers = KryoConfig.serializer.foldLeft(Map.empty[String, Class[_ <: AnyRef]]) {
    case (map, (serializerName, serializerClazzName)) =>
      val serializerClazz = system.dynamicAccess.getClassFor[AnyRef](serializerClazzName) match {
        case Success(clazz) =>
          clazz
        case Failure(e) =>
          log.error(e, "could not load kryo serializer for name :{}", serializerClazzName)
          throw e
      }
      map.updated(serializerName, serializerClazz)
  }

  private val registryClassesWithBinding = KryoConfig.registryClassesWithBinding.foldLeft(Map.empty[Class[_ <: AnyRef], String]) {
    case (map, (registryClazzName, serializerName)) =>
      val registryClazz = system.dynamicAccess.getClassFor[AnyRef](registryClazzName) match {
        case Success(clazz) =>
          clazz
        case Failure(e) =>
          log.error(e, "could not load kryo registry class for name :{}", registryClazzName)
          throw e
      }
      map.updated(registryClazz, serializerName)
  }

  //check if mismatch
  val misMatchSet = registryClassesWithBinding.values.toSet diff serializers.keySet
  if (misMatchSet.nonEmpty) {
    throw new IllegalArgumentException(s"registry class and serializer class mismatch,don't have serializer for \n$misMatchSet")
  }

  val registryClassesBindingWithSerializers = for {
    (registryClazz, serializerName) <- registryClassesWithBinding
    serializerClazz <- serializers.get(serializerName)
  } yield (registryClazz, serializerClazz)

  val sortedRegistryClassesBindingWithSerializerClasses = registryClassesBindingWithSerializers.toSeq.sortBy(_._1.getName)

  log.debug("sorted registry clazz with serializer clazz by registry clazz name :\n{}", sortedRegistryClassesBindingWithSerializerClasses.mkString("\n"))

  val sortedRegistryClassesBindingWithSerializers = sortedRegistryClassesBindingWithSerializerClasses.map {
    case (clazz, serializerClazz) =>
      (clazz, Reflect.instantiate(serializerClazz).asInstanceOf[com.esotericsoftware.kryo.Serializer[_]])
  }
  //then check for no serializer binding classes
  val sortedRegistryClassesWithoutSerializers = KryoConfig.registryClassesWithoutBindingClasses.map {
    clazzName =>
      system.dynamicAccess.getClassFor[AnyRef](clazzName) match {
        case Success(clazz) => clazz
        case Failure(e) =>
          log.error(e, "cloud not load kryo registry class for name :{}", clazzName)
          throw e
      }
  }.sortBy(_.getName)
  log.debug("sorted registry class without serializer clazz by registry clazz name :\n{}", sortedRegistryClassesWithoutSerializers.mkString("\n"))

  val sortedRegistryClassesWithoutSerializersWithSubClassesSorted = sortedRegistryClassesWithoutSerializers.flatMap {
    clazz =>
      val subClazz = Reflect.subTypeOf(clazz, system.dynamicAccess.classLoader, KryoConfig.registryClassesWithoutBindingFilter).toSeq.sortBy(_.getName)
      log.debug("sorted registry class without serializer with subClazz :\n super: {} \nsubClazz :\n-{}", clazz, subClazz.mkString("\n-"))
      subClazz
  }

  val defaultRegistryClassesWithSerializers = Seq(
    (classOf[ActorRef], new ActorRefSerializer(system)),
    (classOf[InetAddress], new InetAddressSerializer),
    (classOf[InetSocketAddress], new InetSocketAddressSerializer),
    //    (classOf[JsonNode], new JsonNodeSerializer),
    //    (classOf[Message], new ProtobufSerializer(system)),
    (classOf[Pattern], new RegexSerializer),
    (classOf[URI], new URISerializer),
    (classOf[UUID], new UUIDSerializer)
  )

  val sortedDefaultRegistryClassesWithSerializers = defaultRegistryClassesWithSerializers.sortBy {
    case (clazz, _) => clazz.getName
  }

  log.debug("sorted default registry classes with serializers \n{}", sortedDefaultRegistryClassesWithSerializers.mkString("\n"))

  private val kryoCount = new AtomicLong(0)
  private val kryoCreator: () => Kryo = () => {
    val kryo = new Kryo()
    // Support deserialization of classes without no-arg constructors
    val instStrategy = kryo.getInstantiatorStrategy.asInstanceOf[Kryo.DefaultInstantiatorStrategy]
    instStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.setInstantiatorStrategy(instStrategy)
    //need registry?
    if (KryoConfig.required) {
      kryo.setRegistrationRequired(true)
    } else {
      kryo.setRegistrationRequired(false)
    }
    //register
    log.debug("kryo creating :{},current kryo instances count :{}", kryo, kryoCount.incrementAndGet())

    defaultRegistryClassesWithSerializers.foreach {
      case (defaultClazz, serializer) =>
        log.debug("registering default clazz :{} --> serializer :{}@{}", defaultClazz, serializer.getClass, serializer)
        kryo.addDefaultSerializer(defaultClazz, serializer)
    }

    sortedRegistryClassesBindingWithSerializers.foreach {
      case (clazz, serializer) =>
        log.debug("registering clazz :{} --> serializer :{}@{}", clazz, serializer.getClass, serializer)
        kryo.register(clazz, serializer)
    }
    sortedRegistryClassesWithoutSerializersWithSubClassesSorted.foreach {
      case clazz =>
        log.debug("registering clazz :{}", clazz)
        kryo.register(clazz.asInstanceOf[Class[_]])
    }

    KryoConfig.registers.map {
      clazz =>
        system.dynamicAccess.getClassFor[KryoClassRegister](clazz)
    }.collect {
      case Success(clazz) =>
        clazz
      case Failure(e) =>
        log.error(e, "clazz is not an instance of KryoClassRegister")
        throw e
    }.map(_.newInstance()).foreach {
      register =>
        log.debug("registering kryo via :{}", register.getClass.getName)
        register.register(kryo)
    }

    log.debug("set asm enabled")
    kryo.setAsmEnabled(true)
    log.debug("set kryo classLoader to system's classLoader")
    kryo.setClassLoader(system.dynamicAccess.classLoader)
    kryo
  }

  private val pool = new DefaultKryoSerializerPool(kryoCreator, KryoConfig.initialOutputBufferSize, KryoConfig.maxOutputBufferSize)

  override def identifier: Int = 18

  override def includeManifest: Boolean = false

  private val factory = LZ4Factory.fastestInstance()
  private val compressor = factory.fastCompressor()
  private val decompressor = factory.fastDecompressor()

  private val lz4Enable = KryoConfig.lz4
  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    pool.withKryoSerializerComponent {
      case KryoSerializerComponent(kryo, input, _) =>
        input.setBuffer {
          if (lz4Enable) {
            val sourceLength = {
              (bytes(0) & 0xff) << 24 |
                (bytes(1) & 0xff) << 16 |
                (bytes(2) & 0xff) << 8 |
                bytes(3) & 0xff
            }
            println("lz4 source :" + bytes.length)
            println("source :" + sourceLength)
            decompressor.decompress(bytes, 4, sourceLength)
          } else {
            println("source :" + bytes.length)
            bytes
          }
        }
        val anyRef = kryo.readClassAndObject(input)
        println(anyRef)
        anyRef
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    println("serializing :" + o.getClass)
    val bytes = pool.withKryoSerializerComponent {
      case KryoSerializerComponent(kryo, _, output) =>
        output.clear()
        try {
          kryo.writeClassAndObject(output, o)
          output.getBuffer
        } catch {
          case e: KryoException if e.getMessage.startsWith("Buffer overflow") =>
            log.error(e, "Kryo serialization failed: {}. To avoid this increase spark.kryoserializer.buffer.max.mb value.", e.getMessage)
            throw new KryoSerializerException(e)
        }
    }
    if (lz4Enable) {
      val sourceLength = bytes.length
      val compressedBytes = compressor.compress(bytes)
      val compressedBytesLength = compressedBytes.length
      val compressedBytesWithLength = new Array[Byte](compressedBytesLength + 4)

      System.arraycopy(compressedBytes, 0, compressedBytesWithLength, 4, compressedBytesLength)

      compressedBytesWithLength(0) = (sourceLength >>> 24).asInstanceOf[Byte]
      compressedBytesWithLength(1) = (sourceLength >>> 16).asInstanceOf[Byte]
      compressedBytesWithLength(2) = (sourceLength >>> 8).asInstanceOf[Byte]
      compressedBytesWithLength(3) = sourceLength.asInstanceOf[Byte]
      println("dest lz4 :" + compressedBytesLength + "(+4)")
      println("dest :" + sourceLength)
      compressedBytesWithLength
    } else {
      println("dest :" + bytes.length)
      bytes
    }
  }
}

trait KryoClassRegister {
  def register(kryo: Kryo): Unit
}

class DefaultKryoClassRegister extends KryoClassRegister {
  override def register(kryo: Kryo): Unit = {
    kryo.register(().getClass, new ScalaObjectSerializer[Unit](()))
  }
}

case class KryoSerializerException(cause: Throwable) extends RuntimeException(cause)

case class KryoSerializerConfiguration(config: QGameConfig) {
  val serializer = config.getMap("serializer").getOrElse(
    throw new IllegalArgumentException("must setup serializer in akka.actor.kryo.serializer,even it's empty")
  )

  val required = config.getBoolean("registry.required").getOrElse(true)

  val registryClassesWithBinding = config.getMap("registry.classes-with-binding").getOrElse(
    throw new IllegalArgumentException("must setup registry.classes-with-binding,even it's empty")
  )

  val registryClassesWithoutBindingClasses = config.getStringSeq("registry.classes-without-binding.classes").getOrElse(
    throw new IllegalArgumentException("must setup registry.classes-without-binding.classes,even it's empty")
  )

  val registryClassesWithoutBindingFilter = config.getString("registry.classes-without-binding.filter").getOrElse(
    throw new IllegalArgumentException("must setup registry.classes-without-binding.filter,even it's empty")
  )

  val registers = config.getStringSeq("registry.registers").getOrElse(
    throw new IllegalArgumentException("must setup registry.registers")
  )

  val lz4 = config.getBoolean("lz4").getOrElse(
    throw new IllegalArgumentException("must setup up lz4")
  )

  val initialOutputBufferSize = config.getBytes("initial-output-buffer-size").getOrElse(1024l).toInt

  val maxOutputBufferSize = config.getBytes("max-output-buffer-size").getOrElse(1024 * 1024l).toInt
}

case class KryoSerializerComponent(kryo: Kryo, input: Input, output: Output)

trait KryoSerializerPool {
  def acquire(): KryoSerializerComponent

  def release(component: KryoSerializerComponent): Unit

  def withKryoSerializerComponent[T](f: (KryoSerializerComponent) => T): T
}

abstract class AbstractKryoSerializerPool extends KryoSerializerPool {
  protected val queue: util.Queue[KryoSerializerComponent]

  protected def componentCreator: KryoSerializerComponent

  override def acquire(): KryoSerializerComponent = {
    val component = queue.poll()
    if (component ne null) component else componentCreator
  }

  override def withKryoSerializerComponent[T](f: (KryoSerializerComponent) => T): T = {
    val component = acquire()
    try {
      f(component)
    } finally {
      release(component)
    }
  }

  override def release(component: KryoSerializerComponent): Unit = queue.offer(component)
}

class DefaultKryoSerializerPool(kryoCreator: () => Kryo, initialOutputBufferSize: Int, maxOutPutBufferSize: Int) extends AbstractKryoSerializerPool {
  override protected val queue: util.Queue[KryoSerializerComponent] = new ConcurrentLinkedQueue[KryoSerializerComponent]()

  override protected def componentCreator: KryoSerializerComponent = {
    KryoSerializerComponent(
      kryoCreator(),
      new UnsafeInput(),
      new UnsafeOutput(initialOutputBufferSize, maxOutPutBufferSize)
    )
  }
}
