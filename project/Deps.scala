package stats

import sbt._

object Deps {
  object Versions {
    val scala = "2.11.7"
    val akka = "2.3.14"
    val logback = "1.1.3"
    val ftier = "0.1-190-g1fe6934"
    val scalaTest = "2.2.4"
  }

  val akka = Seq(
    "com.." %% "akka-cluster-metrics" % "1.0")

  val ftier = Seq("com.." %% "ftier-ws" % Versions.ftier)

  val logging = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
    "ch.qos.logback" % "logback-classic" % Versions.logback)

  val test = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka
    ) map (_ % Test)
}
