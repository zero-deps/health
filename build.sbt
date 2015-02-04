name := "stats"
version := "1.0"
scalaVersion := "2.11.5"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

mainClass in Compile := Some(".stats.Boot")
