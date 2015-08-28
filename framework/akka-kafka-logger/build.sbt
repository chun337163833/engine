import sbtprotobuf.{ProtobufPlugin => PB}

version := "1.0"

Seq(PB.protobufSettings: _*)

javaSource in PB.protobufConfig <<= (javaSource in Compile)

version in PB.protobufConfig := "2.6.1"

cleanFiles := Nil

compileOrder in Compile := CompileOrder.JavaThenScala