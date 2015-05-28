package stats

import com.typesafe.sbt.packager.Keys._

object Package {
  val scriptName = "start"

  lazy val settings = Seq(
    executableScriptName := scriptName
  )
}
