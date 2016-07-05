package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.11.8"
    val akka = "2.3.15"
    val logback = "1.1.6"
    val ftier = "0-210-g6357c5a"
    val scalaTest = "2.2.4"
    val argonaut = "6.2-M2"
    val sigarLoader = "1.6.6-rev002"
    val slf4j = "1.7.18"
  }

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier,
    "io.argonaut" %% "argonaut" % Versions.argonaut,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    ("com.typesafe.akka" %% "akka-slf4j" % Versions.akka).
      exclude("org.slf4j", "slf4j-api"),
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api"),
    "org.slf4j" % "jul-to-slf4j" % Versions.slf4j // for sigar loader
  ) ++ test

  val stats_client = Seq("io.argonaut" %% "argonaut" % Versions.argonaut,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    ("com.typesafe.akka" %% "akka-slf4j" % Versions.akka).
      exclude("org.slf4j", "slf4j-api"),
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api"),
    "com.typesafe.akka" %% "akka-cluster" % Versions.akka,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "org.slf4j" % "jul-to-slf4j" % Versions.slf4j // for sigar loader
  )


  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka
  ) map (_ % Test)
}
