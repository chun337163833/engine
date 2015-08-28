package qgame.engine.util

import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
case class Version(major: Int, minor: Int, patch: Int) {
  require(major != 0 || minor != 0 || patch != 0)
  require(major >= 0 || minor >= 0 || patch >= 0)
  override def toString: String = s"$major.$minor.$patch"

  def >(that: Version): Boolean = {
    if (major > that.major) true
    else if (major == that.major) {
      if (minor > that.minor) true
      else if (minor == that.minor) {
        if (patch > that.patch) true
        else false
      } else false
    } else false
  }

  def <(that: Version): Boolean = {
    that > this
  }

  def >=(that: Version): Boolean = {
    this > that || this == that
  }

  def <=(that: Version): Boolean = {
    that >= this
  }
}

object Version {
  def apply(version: String): Version = {
    require(version ne null, "version should not be null")
    try {
      val Array(major, minor, patch) = version.split('.')
      Version(major.toInt, minor.toInt, patch.toInt)
    } catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(s"version is not valid,please check,it should be major.minor.patch,but you provide $version")
    }
  }
}
