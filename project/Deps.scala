package stats

import sbt._

object Deps {
  object Versions {
    val scala = "2.11.6"
    val akka = "2.3.11"
    val leveldbPort = "0.7"
    val leveldbNative = "1.8"
    val sqlite = "3.8.6"
    val jackson = "2.5.2"
    val logback = "1.1.3"
    val ftier= "0.1.53.gf8ab8ab"
  }

  val akka = Seq(
    "com.." %% "akka-cluster-metrics" % "1.0"
  )

  val json = Seq(
    "com.fasterxml.jackson.core"    % "jackson-databind"     % Versions.jackson,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % Versions.jackson
  )

  val ftier = Seq("com...ftier" %% "ftier-ws" % Versions.ftier)

  val sql = Seq(
    "org.xerial" % "sqlite-jdbc" % Versions.sqlite
  )

  val logging = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
    "ch.qos.logback" % "logback-classic" % Versions.logback
  )
}
