resolvers += Classpaths.typesafeReleases

resolvers += Classpaths.typesafeSnapshots

logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.0")

addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.3.3")

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.4.0")

resolvers += "hepin1989" at "https://github.com/hepin1989/release-repo/tree/master"

addSbtPlugin("sean8223" %% "jooq-sbt-plugin" % "1.6.1") // see above

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.8")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")