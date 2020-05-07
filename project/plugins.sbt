libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"
libraryDependencies += "io.github.zero-deps" %% "ext-git" % "2.1.1"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")
addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.4")

resolvers += Resolver.bintrayRepo("zero-deps", "maven")
