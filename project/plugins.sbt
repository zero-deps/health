libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.2.1.201812262042-r"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"
addSbtPlugin("com.github.shmishleniy" % "sbt-deploy-ssh" % "0.1.4")

libraryDependencies += "io.github.zero-deps" %% "gs-git" % "latest.integration"
