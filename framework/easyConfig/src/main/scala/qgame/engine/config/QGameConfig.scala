package qgame.engine.config

import java.util.Optional
import java.util.concurrent.TimeUnit

import com.typesafe.config._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by kerr.
 */
case class QGameConfig(underlying: Config) {
  def readValue[T](key: String, v: => T): Option[T] = {
    if (underlying.hasPath(key)) {
      Option(v)
    } else {
      None
    }
  }

  def isEmpty: Boolean = underlying.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def config(): Config = underlying

  def getString(key: String): Option[String] = readValue(key, underlying.getString(key))

  def getString(key: String, default: String): String = getString(key).getOrElse(default)

  def getStringOrNull(key: String): String = getString(key).orNull

  def getStringOptional(key: String): Optional[String] = Optional.ofNullable(getStringOrNull(key))

  def getInt(key: String): Option[Int] = readValue(key, underlying.getInt(key))

  def getInt(key: String, default: Int): Int = getInt(key).getOrElse(default)

  def getIntOrNull(key: String): Integer = getInt(key).map(int2Integer).orNull

  def getIntOptional(key: String): Optional[Integer] = Optional.ofNullable(getIntOrNull(key))

  def getBoolean(key: String): Option[Boolean] = readValue(key, underlying.getBoolean(key))

  def getBoolean(key: String, default: Boolean): Boolean = getBoolean(key).getOrElse(default)

  def getBooleanOrNull(key: String): java.lang.Boolean = getBoolean(key).map(boolean2Boolean).orNull

  def getBooleanOptional(key: String): Optional[java.lang.Boolean] = Optional.ofNullable(getBooleanOrNull(key))

  def getMilliseconds(key: String): Option[Long] = readValue(key, underlying.getDuration(key, TimeUnit.MILLISECONDS))

  def getMilliseconds(key: String, default: Long): Long = getMilliseconds(key).getOrElse(default)

  def getMillisecondsOrNull(key: String): java.lang.Long = getMilliseconds(key).map(long2Long).orNull

  def getMillisecondsOptional(key: String): Optional[java.lang.Long] = Optional.ofNullable(getMillisecondsOrNull(key))

  def getNanoseconds(key: String): Option[Long] = readValue(key, underlying.getDuration(key, TimeUnit.NANOSECONDS))

  def getNanoseconds(key: String, default: Long): Long = getNanoseconds(key).getOrElse(default)

  def getNanosecondsOrNull(key: String): java.lang.Long = getNanoseconds(key).map(long2Long).orNull

  def getNanosecondsOptional(key: String): Optional[java.lang.Long] = Optional.ofNullable(getNanosecondsOrNull(key))

  def getDuration(key: String, timeUnit: TimeUnit): Option[Long] = readValue(key, underlying.getDuration(key, timeUnit))

  def getDuration(key: String, default: Long, timeUnit: TimeUnit): Long = getDuration(key, timeUnit).getOrElse(default)

  def getDurationOrNull(key: String, timeUnit: TimeUnit): java.lang.Long = getDuration(key, timeUnit).map(long2Long).orNull

  def getDurationOptional(key: String, timeUnit: TimeUnit): Optional[java.lang.Long] = Optional.ofNullable(getDurationOrNull(key, timeUnit))

  def getBytes(key: String): Option[Long] = readValue(key, underlying.getBytes(key))

  def getBytes(key: String, default: Long): Long = getBytes(key).getOrElse(default)

  def getBytesOrNull(key: String): java.lang.Long = getBytes(key).map(long2Long).orNull

  def getBytesOptional(key: String): Optional[java.lang.Long] = Optional.ofNullable(getBytesOrNull(key))

  def getConfig(key: String): Option[QGameConfig] = readValue(key, underlying.getConfig(key)).map(QGameConfig(_))

  def getConfigOrNull(key: String): QGameConfig = getConfig(key).orNull

  def getConfigOptional(key: String): Optional[QGameConfig] = Optional.ofNullable(getConfigOrNull(key))

  def getDouble(key: String): Option[Double] = readValue(key, underlying.getDouble(key))

  def getDouble(key: String, default: Double): Double = getDouble(key).getOrElse(default)

  def getDoubleOrNull(key: String): java.lang.Double = getDouble(key).map(double2Double).orNull

  def getDoubleOptional(key: String): Optional[java.lang.Double] = Optional.ofNullable(getDoubleOrNull(key))

  def getLong(key: String): Option[Long] = readValue(key, underlying.getLong(key))

  def getLong(key: String, default: Long): Long = getLong(key).getOrElse(default)

  def getLongOrNull(key: String): java.lang.Long = getLong(key).map(long2Long).orNull

  def getLongOptional(key: String): Optional[java.lang.Long] = Optional.ofNullable(getLongOrNull(key))

  def getNumber(key: String): Option[Number] = readValue(key, underlying.getNumber(key))

  def getNumber(key: String, default: Number): Number = getNumber(key).getOrElse(default)

  def getNumberOrNull(key: String): Number = getNumber(key).orNull

  def getNumberOptional(key: String): Optional[Number] = Optional.ofNullable(getNumberOrNull(key))

  def getBooleanList(key: String): Option[java.util.List[java.lang.Boolean]] = readValue(key, underlying.getBooleanList(key))

  def getBooleanListOrNull(key: String): java.util.List[java.lang.Boolean] = getBooleanList(key).orNull

  def getBooleanListOptional(key: String): Optional[java.util.List[java.lang.Boolean]] = Optional.ofNullable(getBooleanListOrNull(key))

  def getBooleanSeq(key: String): Option[Seq[java.lang.Boolean]] = getBooleanList(key).map(asScalaList)

  def getBytesList(key: String): Option[java.util.List[java.lang.Long]] = readValue(key, underlying.getBytesList(key))

  def getBytesListOrNull(key: String): java.util.List[java.lang.Long] = getBytesList(key).orNull

  def getBytesListOptional(key: String): Optional[java.util.List[java.lang.Long]] = Optional.ofNullable(getBytesListOrNull(key))

  def getBytesSeq(key: String): Option[Seq[java.lang.Long]] = readValue(key, underlying.getBytesList(key))

  def getConfigList(key: String): Option[java.util.List[QGameConfig]] = readValue(key, underlying.getConfigList(key)).map(configs => configs.map(QGameConfig(_)))

  def getConfigListOrNull(key: String): java.util.List[QGameConfig] = getConfigList(key).orNull

  def getConfigListOptional(key: String): Optional[java.util.List[QGameConfig]] = Optional.ofNullable(getConfigListOrNull(key))

  def getConfigSeq(key: String): Option[Seq[QGameConfig]] = readValue(key, underlying.getConfigList(key)).map(_.toList).map(configs => configs.map(QGameConfig(_)))

  def getDoubleList(key: String): Option[java.util.List[java.lang.Double]] = readValue(key, underlying.getDoubleList(key))

  def getDoubleListOrNull(key: String): java.util.List[java.lang.Double] = getDoubleList(key).orNull

  def getDoubleListOptional(key: String): Optional[java.util.List[java.lang.Double]] = Optional.ofNullable(getDoubleListOrNull(key))

  def getDoubleSeq(key: String): Option[Seq[java.lang.Double]] = getDoubleList(key).map(asScalaList)

  def getIntList(key: String): Option[java.util.List[java.lang.Integer]] = readValue(key, underlying.getIntList(key))

  def getIntListOrNull(key: String): java.util.List[java.lang.Integer] = getIntList(key).orNull

  def getIntListOptional(key: String): Optional[java.util.List[java.lang.Integer]] = Optional.ofNullable(getIntListOrNull(key))

  def getIntSeq(key: String): Option[Seq[java.lang.Integer]] = getIntList(key).map(asScalaList)

  def getList(key: String): Option[ConfigList] = readValue(key, underlying.getList(key))

  def getListOrNull(key: String): ConfigList = getList(key).orNull

  def getListOptional(key: String): Optional[ConfigList] = Optional.ofNullable(getListOrNull(key))

  def getLongList(key: String): Option[java.util.List[java.lang.Long]] = readValue(key, underlying.getLongList(key))

  def getLongListOrNull(key: String): java.util.List[java.lang.Long] = getLongList(key).orNull

  def getLongListOptional(key: String): Optional[java.util.List[java.lang.Long]] = Optional.ofNullable(getLongList(key).orNull)

  def getLongSeq(key: String): Option[Seq[java.lang.Long]] = getLongList(key).map(asScalaList)

  def getMillisecondsList(key: String): Option[java.util.List[java.lang.Long]] = readValue(key, underlying.getDurationList(key, TimeUnit.MILLISECONDS))

  def getMillisecondsListOrNull(key: String): java.util.List[java.lang.Long] = getMillisecondsList(key).orNull

  def getMillisecondsListOptional(key: String): Optional[java.util.List[java.lang.Long]] = Optional.ofNullable(getMillisecondsList(key).orNull)

  def getMillisecondsSeq(key: String): Option[Seq[java.lang.Long]] = getMillisecondsList(key).map(asScalaList)

  def getNanosecondsList(key: String): Option[java.util.List[java.lang.Long]] = readValue(key, underlying.getDurationList(key, TimeUnit.NANOSECONDS))

  def getNanosecondsListOrNull(key: String): java.util.List[java.lang.Long] = getNanosecondsList(key).orNull

  def getNanosecondsListOptional(key: String): Optional[java.util.List[java.lang.Long]] = Optional.ofNullable(getNanosecondsList(key).orNull)

  def getNanosecondsSeq(key: String): Option[Seq[java.lang.Long]] = getNanosecondsList(key).map(asScalaList)

  def getNumberList(key: String): Option[java.util.List[java.lang.Number]] = readValue(key, underlying.getNumberList(key))

  def getNumberListOrNull(key: String): java.util.List[java.lang.Number] = getNumberList(key).orNull

  def getNumberListOptional(key: String): Optional[java.util.List[java.lang.Number]] = Optional.ofNullable(getNumberList(key).orNull)

  def getNumberSeq(key: String): Option[Seq[java.lang.Number]] = getNumberList(key).map(asScalaList)

  def getObjectList(key: String): Option[java.util.List[_ <: ConfigObject]] = readValue(key, underlying.getObjectList(key))

  def getObjectListOrNull(key: String): java.util.List[_ <: ConfigObject] = getObjectList(key).orNull

  def getObjectListOptional(key: String): Optional[java.util.List[_ <: ConfigObject]] = Optional.ofNullable(getObjectList(key).orNull)

  def getObjectSeq(key: String): Option[Seq[_ <: ConfigObject]] = readValue(key, underlying.getObjectList(key)).map(_.toList)

  def getStringList(key: String): Option[java.util.List[java.lang.String]] = readValue(key, underlying.getStringList(key))

  def getStringListOrNull(key: String): java.util.List[java.lang.String] = getStringList(key).orNull

  def getStringListOptional(key: String): Optional[java.util.List[java.lang.String]] = Optional.ofNullable(getStringList(key).orNull)

  def getStringSeq(key: String): Option[Seq[java.lang.String]] = getStringList(key).map(asScalaList)

  def getObject(key: String): Option[ConfigObject] = readValue(key, underlying.getObject(key))

  def getObjectOrNull(key: String): ConfigObject = getObject(key).orNull

  def getObjectOptional(key: String): Optional[ConfigObject] = Optional.ofNullable(getObject(key).orNull)

  def keys: Set[String] = underlying.root().entrySet.asScala.map(_.getKey).toSet

  def entrySet: Set[(String, ConfigValue)] = underlying.entrySet().asScala.map(e => e.getKey -> e.getValue).toSet

  def getMap(key: String): Option[Map[String, String]] = {
    readValue(key, underlying.getConfig(key).root().unwrapped().foldLeft(Map.empty[String, String]) {
      case (map, (configKey, configValue)) =>
        map.updated(configKey, configValue.toString)
    })
  }

  def getMapOrNull(key: String): java.util.Map[String, String] = getMap(key).map(mapAsJavaMap(_)).orNull

  def getMapOptional(key: String): Optional[java.util.Map[String, String]] = Optional.ofNullable(getMap(key).map(mapAsJavaMap(_)).orNull)

  private[QGameConfig] def asScalaList[A](l: java.util.List[A]): Seq[A] = asScalaBufferConverter(l).asScala.toList

}

object QGameConfig {
  val empty = QGameConfig(ConfigFactory.empty())
}
