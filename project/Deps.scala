package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.11.8"
    val akka = "2.4.9-10-g5573d26"
    val logback = "1.1.7"
    val ftier = "0-223-g0c0991d"
    val scalaTest = "2.2.4"
    val argonaut = "6.2-M3"
    val sigarLoader = "1.6.6-rev002"
    val slf4j = "1.7.18"
  }

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier,
    "io.argonaut" %% "argonaut" % Versions.argonaut,
    ("com...akka" %% "akka-slf4j" % Versions.akka).
      exclude("org.slf4j", "slf4j-api"),
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api")
  ) ++ test

  val stats_client = Seq("io.argonaut" %% "argonaut" % Versions.argonaut % Provided,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    "com...akka" %% "akka-slf4j" % Versions.akka % Provided,
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api"),
    "com...akka" %% "akka-cluster" % Versions.akka % Provided,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "org.slf4j" % "jul-to-slf4j" % Versions.slf4j // for sigar loader
  )


  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com...akka" %% "akka-testkit" % Versions.akka
  ) map (_ % Test)
}
