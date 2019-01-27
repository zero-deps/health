ThisBuild / organization := "com.."
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := org.eclipse.jgit.api.Git.open(file(".")).describe().call()
ThisBuild / fork := true
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-target:jvm-1.8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
)
ThisBuild / publishTo := Some(" Releases" at "http://nexus.mobile..com/nexus3/repository/releases")
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "", "")
ThisBuild / publishArtifact := true
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
ThisBuild / resolvers += " Releases" at "http://nexus.mobile..com/nexus3/repository/releases"

lazy val stats = project.in(file(".")).settings(
  libraryDependencies += "com.." %% "ftier" % "0.0.13-12-g5b9a4ad",
  libraryDependencies += "com.." %% "kvs" % "4.1.2",
  mainClass in (Compile, run) := Some(".stats.StatsApp"),
).aggregate(client, macros).dependsOn(client)

lazy val client = project.in(file("client")).withId("stats_client").settings(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.19",
).dependsOn(macros)

lazy val macros = project.in(file("macros")).settings(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Compile, 
)
