package qgame.engine.libs

import java.io.File

/**
 * Created by kerr.
 */
object Platform {
  private val osName = sys.props("os.name")
  private val osArch = sys.props("os.arch")
  private val dataModel = sys.props.getOrElse("sun.arch.data.model", {
    sys.props.get("com.ibm.vm.bitmode")
  })

  val is64Bit = dataModel == "64"

  val is32Bit = dataModel == "32"

  val machine = if (is64Bit) "x86_64" else "i386"

  def execArgs(command: Array[String], dir: File) = {
    try {
      sys.runtime.exec(command, null, dir)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        throw new NoSuchMethodException(ex.toString)
    }
  }

  def exec(command: String) = {
    try {
      sys.runtime.exec(command)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        throw new NoSuchMethodException(ex.toString)
    }
  }

  def hasUnixCommand(command: String): Boolean = {
    sys.runtime.exec(Array("which", command)).waitFor() == 0
  }

  def isAndroid: Boolean = "dalvik".equalsIgnoreCase(sys.props("java.vm.name")) && isLinux

  def isArm: Boolean = "arm".equals(osArch)

  def isUnix: Boolean = File.separatorChar == '/'

  def isWindows: Boolean = File.separatorChar == '\\'

  def isLinux: Boolean = isUnix && osName.toLowerCase.contains("linux")

  def isMacOSX: Boolean = isUnix && osName.startsWith("Mac") || osName.startsWith("Darwin")

  def isBSD: Boolean = isUnix && osName.startsWith("BSD")

  def isSolaris: Boolean = isUnix && osName.startsWith("SunOS") || osName.startsWith("Solaris")

  def isWindows7: Boolean = osName.contains("Windows 7")

  def isWindows8: Boolean = osName.contains("Windows 8")

  def isWindows9: Boolean = osName.contains("Windows 9")

  def isWindows10: Boolean = osName.contains("Windows 10")
}
