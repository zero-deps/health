name := "stats"
version := "1.0"

scalaVersion := "2.11.5"
val akkaVersion = "2.3.9"
val sprayVersion = "1.3.2"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "io.spray" %% "spray-can"   % sprayVersion
libraryDependencies += "io.spray" %% "spray-httpx" % sprayVersion
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

mainClass in (Compile, run) := Some(".stats.Boot")
fork in (Compile, run) := true

enablePlugins(SbtTwirl)

sourceDirectories in (Compile, TwirlKeys.compileTemplates) += (sourceDirectory in Compile).value / "templates"
