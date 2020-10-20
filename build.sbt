val akka = "2.5.31"
val ext = "2.2.0.7.g8f0877e"
val proto = "1.8"
val protopurs = "2.2"

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := zero.ext.git.version
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-deprecation"
, "-encoding", "UTF-8"
, "-explaintypes"
, "-feature"
, "-language:_"
, "-unchecked"
, "-Xfatal-warnings"
, "-Xlint:adapted-args"
, "-Xlint:constant"
, "-Xlint:delayedinit-select"
, "-Xlint:inaccessible"
, "-Xlint:infer-any"
, "-Xlint:missing-interpolator"
, "-Xlint:nullary-unit"
, "-Xlint:option-implicit"
, "-Xlint:package-object-classes"
, "-Xlint:poly-implicit-overload"
, "-Xlint:private-shadow"
, "-Xlint:stars-align"
, "-Xlint:type-parameter-shadow"
, "-Ywarn-extra-implicit"
, "-Ywarn-numeric-widen"
, "-Ywarn-unused:implicits"
, "-Ywarn-unused:imports"
, "-Ywarn-unused:params"
, "-Ywarn-value-discard"
, "-Xmaxerrs", "1"
, "-Xmaxwarns", "1"
, "-Wconf:cat=deprecation&msg=Auto-application:silent"
)

ThisBuild / resolvers += Resolver.jcenterRepo
ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "ext-plug" % ext)

ThisBuild / turbo := true
ThisBuild / useCoursier := true
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val stats = project.in(file(".")).settings(
  fork := true
).dependsOn(client, frontier, kvs_seq, api)

lazy val client = project.in(file("client")).settings(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akka % Provided
, libraryDependencies += "io.github.zero-deps" %% "ext" % ext % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Provided
)

lazy val api = project.in(file("api")).settings(
  libraryDependencies += "io.github.zero-deps" %% "proto-purs" % protopurs
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Compile
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto
)

lazy val frontier = project.in(file("deps/frontier"))

lazy val kvs_core = project.in(file("deps/kvs/core"))

lazy val kvs_seq = project.in(file("deps/kvs/seq"))
