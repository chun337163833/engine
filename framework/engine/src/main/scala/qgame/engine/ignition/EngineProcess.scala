package qgame.engine.ignition

import java.lang.management.ManagementFactory
import java.net.URL
import java.nio.file.{ Files, Path, Paths }

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions, ConfigResolveOptions }
import qgame.engine.config.QGameConfig
import qgame.engine.libs.Logger
import qgame.engine.logging.DebugInfo

/**
 * Created by kerr.
 */
/**
 * inner usage
 */
private[qgame] trait EngineProcess {
  def classLoader: ClassLoader

  def args: Array[String]

  def arguments: Array[String]

  def config: QGameConfig

  def pid: Option[String]

  def addShutdownHook(hook: => Unit): Unit

  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing

  def provider: String
}

private[engine] class RealEngineJVMProcess(val args: Array[String]) extends EngineProcess with DebugInfo {
  private val logger = Logger(this)
  private val innerConfig = load()

  override def classLoader: ClassLoader = {
    Thread.currentThread().getContextClassLoader
  }

  override def pid: Option[String] = {
    ManagementFactory.getRuntimeMXBean.getName.split('@').headOption
  }

  override def arguments: Array[String] = {
    val arguments = ManagementFactory.getRuntimeMXBean.getInputArguments
    val argumentsArray = new Array[String](arguments.size())
    arguments.toArray(argumentsArray)
    argumentsArray
  }

  override def config: QGameConfig = {
    QGameConfig(innerConfig)
  }

  private def load(): Config = {
    val userDir = Paths.get(System.getProperty("user.dir"))
    logger.debug("user.dir :" + userDir)

    def resourceToConfig(resource: String): Config = {
      logger.debug("loading config from resource: " + resource)
      if (classLoader.getResourceAsStream(resource).available() <= 0) {
        throw new IllegalArgumentException(s"could not load the resources: $resource,please check!!!")
      }
      ConfigFactory.parseResourcesAnySyntax(classLoader, resource)
    }

    def reduceFinalConfig(configs: String, op: String => Config): Config = {
      configs.split(",").reverseMap(op).reduceLeft(_.withFallback(_))
    }

    val resourceConfig = Option(System.getProperty("config.resources")).map(reduceFinalConfig(_, resourceToConfig))

    def fileToConfig(path: Path) = {
      logger.info("path :{}", path)
      if (Files.notExists(path)) {
        throw new IllegalArgumentException(s"the file you provide is not exists,please check!!!\n you provide is :$path")
      }
      logger.debug("loading config from file: " + path)
      ConfigFactory.parseFileAnySyntax(path.toFile)
    }

    val fileConfig = Option(System.getProperty("config.files")).map(reduceFinalConfig(_, path => fileToConfig(Paths.get(path))))

    val relativeFileConfig = Option(System.getProperty("config.relativeFiles")).map(reduceFinalConfig(_, relativeFile => fileToConfig(userDir.resolve(relativeFile))))

    def urlToConfig(url: String) = {
      val realURL = new URL(url)
      logger.debug("loading config from url: " + realURL)
      ConfigFactory.parseURL(realURL)
    }

    val urlConfig = Option(System.getProperty("config.urls")).map(reduceFinalConfig(_, urlToConfig))

    val resolveOption = ConfigResolveOptions.defaults().setAllowUnresolved(false).setUseSystemEnvironment(true)
    val defaultConfig = ConfigFactory.load().resolve(resolveOption)
    val renderOptions = ConfigRenderOptions.concise().setFormatted(true).setJson(false)
    resourceConfig.orElse(fileConfig).orElse(relativeFileConfig).orElse(urlConfig).map { config =>
      val realConfig = config.resolve(resolveOption)
      logger.debug(
        s"""
          |------------------------------------------------------------------------------------
          |                                 you provided
          |------------------------------------------------------------------------------------
          |${realConfig.root().render(renderOptions)}
          |------------------------------------------------------------------------------------
          |                             default configuration
          |------------------------------------------------------------------------------------
          |${defaultConfig.root().render(renderOptions)}
          |------------------------------------------------------------------------------------
          |will use you provided with fallback to default
          |------------------------------------------------------------------------------------
        """.stripMargin
      )
      realConfig.withFallback(defaultConfig)
    }.getOrElse {
      logger.debug(
        s"""
          |------------------------------------------------------------------------------------
          |                                 you provided
          |------------------------------------------------------------------------------------
          |None
          |------------------------------------------------------------------------------------
          |                             default configuration
          |------------------------------------------------------------------------------------
          |${defaultConfig.root().render(renderOptions)}
          |------------------------------------------------------------------------------------
          |will use default config
          |------------------------------------------------------------------------------------
        """.stripMargin
      )
      defaultConfig
    }
  }

  override def addShutdownHook(hook: => Unit): Unit = {
    sys.runtime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = hook
    }))
  }

  override def exit(message: String, cause: Option[Throwable], returnCode: Int = -1): Nothing = {
    logger.error(message)
    cause.foreach(_.printStackTrace())
    sys.exit()
  }

  override def provider: String = config.getString("engine.provider").getOrElse(
    throw new IllegalArgumentException("must setup an engine provider to run this application")
  )

  override def debugInfo: String = {
    s"""
        |+------------------------------------------------------------------------
        ||                            Process Info
        |+------------------------------------------------------------------------
        ||pid : ${pid.getOrElse("no pid")}
        ||args: ${args.mkString(" ")}
        ||arguments: ${arguments.mkString(" ")}
        ||classLoader: ${classLoader.getClass}
        ||classLoader's parent : ${classLoader.getParent.getClass}
        ||provider : $provider
        |+------------------------------------------------------------------------
     """.stripMargin
  }
}
