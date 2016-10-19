package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.12.0-RC2"
    val akka = "2.4.11-19-g8520080"
    val logback = "1.1.7"
    val ftier = "0-236-g8a3356f"
    val argonaut = "6.2-M3-14-gbf4b286"
    val sigarLoader = "1.6.6-rev002"
    val jul2slf4j = "1.7.21"
    val scalatest = "3.0.0"
  }

  val stats_client = Seq(
    "com.." %% "argonaut" % Versions.argonaut % Provided,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    "org.slf4j" % "jul-to-slf4j" % Versions.jul2slf4j, // for sigar loader
    "com...akka" %% "akka-cluster" % Versions.akka % Provided,
    "com...akka" %% "akka-slf4j" % Versions.akka % Provided,
    ("ch.qos.logback" % "logback-classic" % Versions.logback % Provided).exclude("org.slf4j", "slf4j-api")
  )

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier,
    "com.." %% "argonaut" % Versions.argonaut
  ) ++ test

  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest,
    "com...akka" %% "akka-testkit" % Versions.akka
  ) map (_ % Test)
}
