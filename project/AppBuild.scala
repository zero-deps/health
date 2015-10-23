package stats

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import deployssh.DeploySSH
import deployssh.DeploySSH.Keys._
import play.twirl.sbt.SbtTwirl
import sbt.Keys._
import sbt._

object AppBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    organization := "com..",
    version := "0.1-SNAPSHOT",
    scalaVersion := Deps.Versions.scala
  )

  lazy val defaultSettings = Defaults.coreDefaultSettings ++
    Seq(
      scalacOptions ++= Seq("-feature", "-deprecation"),
      fork := true,
      publishMavenStyle := false,
      publishArtifact in (Compile, packageSrc) := false,
      publishArtifact in (Compile, packageDoc) := false
    )

  lazy val stats = Project(
    id = "stats",
    base = file("."),
    settings = defaultSettings ++
      Templates.settings ++
      Package.settings ++
      Deploy.settings ++
      Resolvers.settings ++
      Seq(
        mainClass in (Compile, run) := Some(".stats.StatsApp"),
        libraryDependencies ++=
          Deps.akka ++
          Deps.socko ++
          Deps.json ++
          Deps.kvs ++
          Deps.sql ++
          Deps.logging,
        Deploy.deploy <<= deploySshTask
      )
  ).enablePlugins(SbtTwirl, JavaAppPackaging, DeploySSH)
}
