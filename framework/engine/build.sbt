import com.typesafe.sbt.packager.MappingsHelper._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

enablePlugins(JavaAppPackaging)

testFrameworks += new TestFramework(
  "org.scalameter.ScalaMeterFramework")

logBuffered := false

parallelExecution in Test := false

publishArtifact in(Compile, packageDoc) := false

mappings in Universal ++= contentOf((resourceDirectory in Compile).value).map{
  case (file,path)=>
    file -> ("conf/" + path)
}

scriptClasspath := "../conf/" +: scriptClasspath.value

net.virtualvoid.sbt.graph.Plugin.graphSettings

scalariformSettings