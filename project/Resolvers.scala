package stats

import sbt.Keys._
import sbt._

object Resolvers {
  val settings = Seq(
    resolvers ++= Resolver.mavenLocal +: Seq(
      " nexus releases" at "http://ua--nexus01.ee..corp/nexus/content/repositories/releases"
    )
  )
}
