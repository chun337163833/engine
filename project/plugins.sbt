resolvers += Classpaths.typesafeReleases

resolvers += Classpaths.typesafeSnapshots

logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.4")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.0")

addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.3.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.1")

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.8")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.35"

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.5.9")