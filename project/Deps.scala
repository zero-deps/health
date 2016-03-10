package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.11.7"
    val akka = "2.3.14"
    val logback = "1.1.3"
    val ftier = "0.1-190-g1fe6934"
    val scalaTest = "2.2.4"
    val argonaut = "6.2-M1"
    val sigarLoader = "1.6.6-rev002"
    val log4j = "1.2.17"
  }

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier,
    "io.argonaut" %% "argonaut" % Versions.argonaut,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "log4j" % "log4j" % Versions.log4j // for sigar
  ) ++ test

  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka
  ) map (_ % Test)
}
