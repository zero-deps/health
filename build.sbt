ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := zero.ext.git.version
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-deprecation"
, "-encoding", "UTF-8"
, "-explaintypes"
, "-feature"
, "-language:_"
, "-unchecked"
// , "-Xfatal-warnings"
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
ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "ext-plug" % "2.4.1.g7c28a4a")

ThisBuild / turbo := true
ThisBuild / useCoursier := true
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / githubOwner := "zero-deps"
ThisBuild / githubRepository := "ext"

lazy val stats = project.in(file(".")).settings(
  fork := true
).dependsOn(client, frontier, kvs_seq, api)

lazy val client = project.in(file("client")).settings(
  libraryDependencies += "io.github.zero-deps" %% "ext" % "2.4.1.g7c28a4a" % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % "1.8"  % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-macros"  % "1.8"  % Provided
).dependsOn(frontier)

lazy val api = project.in(file("api")).settings(
  libraryDependencies += "io.github.zero-deps" %% "proto-purs" % "2.2"
, libraryDependencies += "io.github.zero-deps" %% "proto-macros"  % "1.8" % Compile
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % "1.8"
)

lazy val frontier = project.in(file("deps/frontier"))

lazy val kvs_core = project.in(file("deps/kvs/core"))

lazy val kvs_seq = project.in(file("deps/kvs/seq"))
