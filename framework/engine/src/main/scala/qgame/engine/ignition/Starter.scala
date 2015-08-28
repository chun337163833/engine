package qgame.engine.ignition

import java.io.{ File, FileOutputStream }
import java.nio.file.Paths

import qgame.engine.core.Engine.EngineStartException
import qgame.engine.libs.{ FileSystem, Logger, Platform }
import qgame.engine.runtime.{ EngineRuntime, DefaultEngineRuntime }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future, Promise }
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

/**
 * Created by kerr.
 */
trait Starter {
  private lazy val log = Logger(classOf[Starter])

  def main(args: Array[String]) {
    println(Starter.GEARINGLOGO)
    sys.props += "java.net.preferIPv4Stack" -> "true"
    val engineProcess = new RealEngineJVMProcess(args)
    log.debug("engine process started :\n" + engineProcess.debugInfo)
    val configuration = readConfiguration(engineProcess)
    handleCommand(args, configuration)
    if (Platform.isUnix) {
      log.debug("loading sigar native")
      loadSigar(configuration)
    }
    //TODO current only start one engine
    //TODO update this after akka 2.4 release
    val engineInfo = Await.ready(start(engineProcess, configuration), Duration.Inf)
    engineInfo.onComplete {
      case Success(info) =>
        log.debug(s"gear started ,info :$info")
      case Failure(error) => error match {
        case EngineStartException(message, cause) =>
          log.error(error, s"gear :EngineStartException,could not start,info :[$message],will stop now")
          engineProcess.exit(message, cause)
        case NonFatal(e) =>
          log.error(e, s"gear: Exception :[${e.getMessage}],could not start,will stop now.")
          engineProcess.exit("Cannot start Engine", Some(e))
      }

    }
  }

  private def handleCommand(args: Array[String], configuration: EngineRuntimeConfiguration): Unit = {
    if (args.length == 1) {
      val command = args(0)
      command match {
        case "help" =>
          log.debug("help:")
          log.debug("stop stop the running server")
        case "stop" =>
          log.debug("stop command")
          val engineConfig = configuration.engineConfig
          engineConfig.getBoolean("prod").foreach {
            mode =>
              if (!mode) {
                log.debug("stop is only available on prod mode")
              } else {
                //cat the running pid
                val tryPidFile = configuration.engineConfig.getString("pidFile.path").map(new File(_))

                val tryPidFileName = configuration.engineConfig.getString("pidFile.name").getOrElse(throw EngineStartException(s"Invalid Engine pidFile.name"))

                val defaultPidFile = new File(configuration.rootDir, tryPidFileName)

                val pidFile = (tryPidFile getOrElse defaultPidFile).getAbsoluteFile

                if (pidFile.getAbsolutePath != "/dev/null") {
                  if (!pidFile.exists()) {
                    throw EngineStartException(s"This Engine is not running,cause no file exists on (${pidFile.getPath}).\n nothing to stop")
                  } else {
                    //read the pid and stop it
                    Source.fromFile(pidFile).getLines().take(1).foreach {
                      pidStr =>
                        log.debug(s"previous running engine at pid: $pidStr")
                        Try {
                          log.debug(s"killing... :$pidStr")
                          sys.runtime.exec(Array("kill", pidStr))
                        } match {
                          case Failure(e) =>
                            log.debug("failed to stop previous running engine")
                          case Success(p) =>
                            log.debug(s"stop previous running engine ok at process :$p")
                        }

                    }
                  }
                }
              }
          }
        case _ => //NOOP
      }
    }
  }

  def start(process: EngineProcess, configuration: EngineRuntimeConfiguration): Future[(EngineRuntime, String)] = {
    try {
      val runtime = new DefaultEngineRuntime(process, configuration)
      log.debug("gong starting runtime")
      val promise = Promise[(EngineRuntime, String)]()
      runtime.start().onComplete {
        case Success(runtimeInfo) =>
          log.debug("runtime started,debug info :\n" + runtimeInfo)
          runtime.startEngine().onComplete {
            case Success(engineInfo) =>
              log.debug("engine started,debug info :\n" + engineInfo)
              promise.trySuccess((runtime, engineInfo))
            case Failure(engineStartError) =>
              log.error(engineStartError, "engine starting error.")
              promise.tryFailure(engineStartError)
          }
        case Failure(runtimeStartError) =>
          log.error(runtimeStartError, "runtime starting error.")
          promise.tryFailure(runtimeStartError)
      }
      configuration.engineConfig.getBoolean("prod").foreach {
        mode =>
          if (mode) {
            val pidFile = createPidFile(process, configuration)
            process.addShutdownHook {
              val stopRuntimeInfo = Await.result(runtime.stop(), Duration.Inf)
              println(stopRuntimeInfo)
              pidFile.foreach {
                file =>
                  println("deleted pid file :" + file)
                  file.delete()
              }
              assert(!pidFile.exists(_.exists), "PID file should not exist!")
            }
          }
      }
      promise.future
    } catch {
      case EngineStartException(message, cause) =>
        process.exit(message, cause)
      case NonFatal(e) =>
        process.exit("Cannot start Engine", Some(e))
    }

  }

  private def createPidFile(process: EngineProcess, configuration: EngineRuntimeConfiguration): Option[File] = {
    val pid = process.pid getOrElse (throw EngineStartException("could not detect current jvm's process's pid"))

    val tryPidFile = configuration.engineConfig.getString("pidFile.path").map(new File(_))

    val tryPidFileName = configuration.engineConfig.getString("pidFile.name").getOrElse(throw EngineStartException(s"Invalid Engine pidFile.name"))

    val defaultPidFile = new File(configuration.rootDir, tryPidFileName)

    val pidFile = (tryPidFile getOrElse defaultPidFile).getAbsoluteFile

    if (pidFile.getAbsolutePath != "/dev/null") {
      if (pidFile.exists()) {
        throw EngineStartException(s"This Engine is already running (Or delete ${pidFile.getPath} file).")
      }
      val fileOut = new FileOutputStream(pidFile)
      try {
        fileOut.write(pid.getBytes)
      } finally {
        fileOut.close()
      }
      Some(pidFile)
    } else {
      None
    }
  }

  /**
   * override this to provide how to read the EngineConfig,
   * the return config will be used to create the engine via engine provider
   */
  private def readConfiguration(process: EngineProcess): EngineRuntimeConfiguration = {
    val engineConfig = process.config.getConfig("engine").orNull
    val rootDir: File = {
      val path = process.config.getString("user.dir").orNull
      val file = new File(path)
      if (!(file.exists() && file.isDirectory)) {
        throw EngineStartException(s"Bad root server path: $path")
      }
      file
    }
    DefaultEngineRuntimeConfiguration(rootDir, process.config, engineConfig, process)
  }

  private def loadSigar(configuration: EngineRuntimeConfiguration): Unit = {
    //    val field = classOf[ClassLoader].getDeclaredField("usr_paths")
    //    field.setAccessible(true)
    //    val currentLibs = field.get(null).asInstanceOf[Array[String]].toSet
    //    log.debug("current libs :{}",currentLibs)
    //FIXME read from configuration
    val sigarNativeHome = configuration.rootConfig.getString("user.dir").map(_ + File.separator + "sigar-native").getOrElse(
      throw new IllegalArgumentException("could not load sigar without user.dir")
    )
    log.debug("sigar native home:{}", sigarNativeHome)
    if (!FileSystem.exists(sigarNativeHome)) {
      log.debug("sigar native home not exist,creating")
      FileSystem.mkdir(sigarNativeHome)
    }
    val classLoader = configuration.process.classLoader
    //check if the file is exist,other wise copy to file path
    if (Platform.isLinux) {
      if (Platform.is32Bit) {
        if (!FileSystem.exists(sigarNativeHome, SigarNativeLib.linuxX86)) {
          log.debug("sigar native lib :{} not exists,copy now", SigarNativeLib.linuxX86)
          val libSigarLinuxX86Stream = classLoader.getResourceAsStream(SigarNativeLib.linuxX86)
          val target = Paths.get(sigarNativeHome, SigarNativeLib.linuxX86).toString
          FileSystem.create(target)
          FileSystem.copy(libSigarLinuxX86Stream, target)
        }
      } else if (Platform.is64Bit) {
        if (!FileSystem.exists(sigarNativeHome, SigarNativeLib.linuxX64)) {
          log.debug("sigar native lib :{} not exists,copy now", SigarNativeLib.linuxX64)
          val libSigarLinuxX64Stream = classLoader.getResourceAsStream(SigarNativeLib.linuxX64)
          val target = Paths.get(sigarNativeHome, SigarNativeLib.linuxX64).toString
          FileSystem.create(target)
          FileSystem.copy(libSigarLinuxX64Stream, target)
        }
      } else {
        log.error("unknown black tech")
      }
    } else if (Platform.isMacOSX) {
      if (!FileSystem.exists(sigarNativeHome, SigarNativeLib.macosxX64)) {
        log.debug("sigar native lib :{} not exists,copy now", SigarNativeLib.macosxX64)
        val libSigarMacosxX64Stream = classLoader.getResourceAsStream(SigarNativeLib.macosxX64)
        val target = Paths.get(sigarNativeHome, SigarNativeLib.macosxX64).toString
        FileSystem.create(target)
        FileSystem.copy(libSigarMacosxX64Stream, target)
      }
    } else if (Platform.isSolaris) {
      if (!FileSystem.exists(sigarNativeHome, SigarNativeLib.solaris)) {
        log.debug("sigar native lib :{} not exists,copy now", SigarNativeLib.solaris)
        val libSigarSolarisStream = classLoader.getResourceAsStream(SigarNativeLib.solaris)
        val target = Paths.get(sigarNativeHome, SigarNativeLib.solaris).toString
        FileSystem.create(target)
        FileSystem.copy(libSigarSolarisStream, target)
      }
    }
    //val currentLibsWithSigarNativeHome = (currentLibs + sigarNativeHome).toArray
    //field.set(null,currentLibsWithSigarNativeHome)
    sys.props.get("java.library.path").foreach {
      javaLibPath =>
        sys.props.update("java.library.path", javaLibPath + File.pathSeparator + sigarNativeHome)
    }
  }

  object SigarNativeLib {
    val linuxX64 = "libsigar-amd64-linux-1.6.4.so"
    val linuxX86 = "libsigar-x86-linux-1.6.4.so"
    val macosxX64 = "libsigar-universal64-macosx-1.6.4.dylib"
    val solaris = "libsigar-amd64-solaris-1.6.4.so"
  }

}

private[ignition] object Starter {
  private final val GEARINGLOGO =
    """
      |-----------------------------------------------------------------------------------------
      |
      | ██████╗ ███████╗ █████╗ ██████╗         ███████╗███╗   ██╗ ██████╗ ██╗███╗   ██╗███████╗
      |██╔════╝ ██╔════╝██╔══██╗██╔══██╗        ██╔════╝████╗  ██║██╔════╝ ██║████╗  ██║██╔════╝
      |██║  ███╗█████╗  ███████║██████╔╝        █████╗  ██╔██╗ ██║██║  ███╗██║██╔██╗ ██║█████╗
      |██║   ██║██╔══╝  ██╔══██║██╔══██╗        ██╔══╝  ██║╚██╗██║██║   ██║██║██║╚██╗██║██╔══╝
      |╚██████╔╝███████╗██║  ██║██║  ██║        ███████╗██║ ╚████║╚██████╔╝██║██║ ╚████║███████╗
      | ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝        ╚══════╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═══╝╚══════╝
      |
      |version : 2.0.0-alpha
      |-----------------------------------------------------------------------------------------
    """.stripMargin
}

