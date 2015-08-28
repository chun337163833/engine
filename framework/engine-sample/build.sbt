import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

version := "1.0"

enablePlugins(JavaAppPackaging)
//(mainClass in Compile) := Some("qgame.engine.DefaultStarter")