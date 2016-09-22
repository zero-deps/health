package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.11.8"
    val akka = "2.4.10-12-g84649db"
    val logback = "1.1.7"
    val ftier = "0-231-g1c4cdb5"
    val argonaut = "6.2-M3"
    val sigarLoader = "1.6.6-rev002"
    val jul2slf4j = "1.7.21"
    val scalatest = "3.0.0"
  }

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier,
    "io.argonaut" %% "argonaut" % Versions.argonaut,
    ("com...akka" %% "akka-slf4j" % Versions.akka).
      exclude("org.slf4j", "slf4j-api"),
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api")
  ) ++ test

  val stats_client = Seq("io.argonaut" %% "argonaut" % Versions.argonaut,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    "com...akka" %% "akka-slf4j" % Versions.akka,
    ("ch.qos.logback" % "logback-classic" % Versions.logback).
      exclude("org.slf4j", "slf4j-api"),
    "com...akka" %% "akka-cluster" % Versions.akka,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "org.slf4j" % "jul-to-slf4j" % Versions.jul2slf4j // for sigar loader
  )


  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest,
    "com...akka" %% "akka-testkit" % Versions.akka
  ) map (_ % Test)
}
