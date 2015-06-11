package stats

import sbt.Keys._
import sbt._

object Resolvers {
  val settings = Seq(
    resolvers ++= Seq(
      " nexus releases" at "http://nexus-mobile.ee..corp/nexus/content/repositories/-releases/"
    )
  )
}
