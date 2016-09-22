package stats

import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  override lazy val settings = super.settings ++ Seq(
    organization := "com..",
    version := org.eclipse.jgit.api.Git.open(file(".")).describe().call(),
    scalaVersion := Deps.Versions.scala
  )

  lazy val stats = Project(
    id = "stats",
    base = file("."),
    settings = defaultSettings ++
      publishSettings ++
      Deps.stats ++
      Seq(
        scalacOptions ++= Seq("-feature", "-deprecation"),
        fork := true,
        mainClass in (Compile, run) := Some(".stats.StatsApp"),
        cancelable in Global := true,
        resolvers ++= List(Resolver.mavenLocal, Repo)
      ),
    aggregate = Seq(client)
  ).dependsOn(client)

  lazy val client = Project(
    id = "stats_client",
    base = file("client"),
    settings = defaultSettings ++
      publishSettings ++
      Seq(
        libraryDependencies ++= Deps.stats_client ++ Deps.test
      )
  )

  lazy val defaultSettings = Defaults.coreDefaultSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-feature","-deprecation"),
    resolvers ++= List(Resolver.mavenLocal, Repo)
  )

  lazy val publishSettings = List(
    publishTo := Some(Repo),
    credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "wpl-deployer", "aG1reeshie"),
    publishArtifact := true,
    publishArtifact in Compile := true,
    publishArtifact in Test := false,
    publishMavenStyle := true,
    pomIncludeRepository := (_ => false),
    publishLocal <<= publishM2,
    isSnapshot := true
  )

  lazy val Repo = " Releases" at "http://nexus.mobile..com/nexus/content/repositories/releases"
}
