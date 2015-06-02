package stats

import play.twirl.sbt.Import.TwirlKeys
import TwirlKeys._
import sbt.Keys._
import sbt._

object Templates {
  lazy val settings = Seq(
    templateImports ++= Seq(
      ".stats.Template._"
    ),
    sourceDirectories in (Compile, compileTemplates) +=
      (sourceDirectory in Compile).value / "templates"
  )
}