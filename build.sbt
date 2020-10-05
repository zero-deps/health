val akka = "2.5.31"
val ext = "2.2.0.7.g8f0877e"
val frontier = "2.0.2-1-gb7b0ec7"
val leveldb = "1.0.4"
// val logback = "1.2.3"
val proto = "1.8"
val protopurs = "2.2"
val scalatest = "3.1.1"
val scalaV = "2.13.3"
val zionio = "1.0.0-RC9"

ThisBuild / organization := "com.."
ThisBuild / scalaVersion := scalaV
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
// , "-Ywarn-dead-code"
// , "-Ywarn-extra-implicit"
// , "-Ywarn-numeric-widen"
// , "-Ywarn-unused:implicits"
// , "-Ywarn-unused:imports"
// , "-Ywarn-unused:params"
// , "-Ywarn-value-discard"
, "-Xmaxerrs", "1"
// , "-Xmaxwarns", "2"
, "-Wconf:cat=deprecation&msg=Auto-application:silent"
)
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "", "")
ThisBuild / resolvers += " Releases" at "https://nexus.mobile..com/nexus3/repository/releases"
ThisBuild / resolvers += "zero" at "https://nexus.mobile..com/nexus3/repository/zd"

ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "ext-plug" % ext)

ThisBuild / turbo := true
ThisBuild / useCoursier := true
Global / onChangedBuildSource := ReloadOnSourceChanges

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH
lazy val stats = project.in(file(".")).settings(
  libraryDependencies += "com.." %% "ftier" % frontier
, libraryDependencies += "io.github.zero-deps" %% "ext" % ext
, fork := true
, deployConfigs ++= Seq(
    ServerConfig(name="mon", host="ua--monitoring.ee..corp", user=Some("")),
  )
, deployArtifacts ++= Seq(
    ArtifactSSH((packageBin in Universal).value, "stats")
  )
, deploySshExecBefore ++= Seq(
    (ssh: SSH) => ssh.shell{ shell =>
      shell.execute("cd stats")
      shell.execute("touch pid")
      val pid = shell.execute("cat pid")
      if (pid != "") {
        shell.execute(s"kill ${pid}; sleep 5; kill -9 ${pid}")
      } else ()
      shell.execute("rm pid")
    }
  )
, deploySshExecAfter ++= Seq(
    (ssh: SSH) => {
      ssh.scp { scp =>
        scp.send(file(s"./deploy/${ssh.options.name.get}.conf"), "stats/app.conf")
      }
      ssh.shell{ shell =>
        val name = (packageName in Universal).value
        val script = (executableScriptName in Universal).value
        shell.execute("cd stats")
        shell.execute(s"unzip -q -o ${name}.zip")
        shell.execute(s"rm ${name}.zip")
        shell.execute(s"nohup ./${name}/bin/${script} -Dconfig.file=$$(pwd)/app.conf &")
        shell.execute("echo $! > pid")
        shell.execute("touch pid")
        val pid = shell.execute("cat pid")
        val (_, status) = shell.executeWithStatus("echo $?")
        if (status != 0 || pid == "") {
          throw new RuntimeException(s"status=${status}, pid=${pid}. please check package")
        }
      }
    }
  )
).aggregate(client, api, app, frontier2).dependsOn(client, api, kvszio).enablePlugins(JavaAppPackaging, DeploySSH)

lazy val client = project.in(file("client")).settings(
  organization := organization.value + ".stats"
, libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akka % Provided
, libraryDependencies += "io.github.zero-deps" %% "ext" % ext % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Provided
, publishTo := Some(" Releases" at "https://nexus.mobile..com/nexus3/repository/releases")
, credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
, publishArtifact := true
, publishMavenStyle := true
, isSnapshot := true
)

lazy val api = project.in(file("api")).settings(
  libraryDependencies += "io.github.zero-deps" %% "proto-purs" % protopurs
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Compile
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto
)

lazy val app = project.in(file("app")).dependsOn(client, frontier2, kvszio, api)

lazy val frontier2 = project.in(file("frontier")).settings(
  libraryDependencies += "dev.zio" %% "zio-nio" % zionio
)

lazy val kvs = project.in(file("kvs/core")).settings(
  libraryDependencies ++= Seq(
    // "ch.qos.logback" % "logback-classic" % logback,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akka,
    "com.typesafe.akka" %% "akka-slf4j"            % akka,
    "io.github.zero-deps" %% "proto-macros"  % proto % Compile,
    "io.github.zero-deps" %% "proto-runtime" % proto,
    "io.github.zero-deps" %% "ext" % ext,
    "io.github.zero-deps" %% "leveldb-jnr" % leveldb,

    "com.typesafe.akka" %% "akka-testkit" % akka % Test,
    "org.scalatest" %% "scalatest" % scalatest % Test,
  )
)

lazy val kvszio = project.in(file("kvs/core-zio")).settings(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio-nio" % zionio
  , "dev.zio" %% "zio-akka-cluster" % "0.2.0" excludeAll(ExclusionRule(organization = "dev.zio"))
  )
).dependsOn(kvs)

maintainer := ".core.be@.com"