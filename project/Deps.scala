package stats

import sbt._
import sbt.Keys._

object Deps {
  object Versions {
    val scala = "2.12.0"
    val ftier = "0-245-gbc9e8ba"
    val sigarLoader = "1.6.6-rev002"
    val jul2slf4j = "1.7.21"
  }

  val stats_client = Seq(
    "com.." %% "ftier-ws" % Versions.ftier % Provided,
    "io.kamon" % "sigar-loader" % Versions.sigarLoader,
    ("org.slf4j" % "jul-to-slf4j" % Versions.jul2slf4j).exclude("org.slf4j", "slf4j-api") // for sigar loader
  )

  val stats = libraryDependencies ++= Seq(
    "com.." %% "ftier-ws" % Versions.ftier
  )
}
