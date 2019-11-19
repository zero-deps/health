ThisBuild / resolvers += Resolver.bintrayRepo("zero-deps", "maven")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")

addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.4")

libraryDependencies += "io.github.zero-deps" %% "gs-git" % "1.6.2"
