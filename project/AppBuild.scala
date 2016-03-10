package stats

import sbt.Keys._
import sbt._

object AppBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    organization := "com..",
    version := org.eclipse.jgit.api.Git.open(file(".")).describe().call(),
    scalaVersion := Deps.Versions.scala
  )

  lazy val stats = Project(
    id = "stats",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++
      publishSettings ++
      Seq(
        scalacOptions ++= Seq("-feature", "-deprecation"),
        fork := true,
        mainClass in (Compile, run) := Some(".stats.StatsApp"),
        cancelable in Global := true,
        resolvers ++= List(Resolver.mavenLocal, Repo),
        libraryDependencies ++=
          Deps.akka ++
          Deps.ftier ++
          Deps.logging ++
          Deps.json ++
          Deps.test
      )
  )

  lazy val publishSettings = List(
    publishTo := Some(Repo),
    credentials += Credentials("Sonatype Nexus Repository Manager", "ua--nexus01.ee..corp", "wpl-deployer", "aG1reeshie"),
    publishArtifact := true,
    publishArtifact in Compile := true,
    publishArtifact in Test := false,
    publishMavenStyle := true,
    pomIncludeRepository := (_ => false),
    publishLocal <<= publishM2,
    isSnapshot := true
  )

  lazy val Repo = " Releases" at "http://ua--nexus01.ee..corp/nexus/content/repositories/releases"
}
