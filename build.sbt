name := "stats"
version := "1.0"

scalaVersion := "2.11.5"
val akkaVersion = "2.3.9"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

mainClass in Compile := Some(".stats.Boot")
