val akka = "2.5.31"
val ext = "2.1.1-2-gd0abdf9"
val frontier = "2.0.2-1-gb7b0ec7"
val kvs = "5.1.0-21-g20bbd4f"
val proto = "1.8"
val protopurs = "2.2"
val scalaV = "2.13.3"

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
, "-Ywarn-dead-code"
, "-Ywarn-extra-implicit"
, "-Ywarn-numeric-widen"
, "-Ywarn-unused:implicits"
, "-Ywarn-unused:imports"
, "-Ywarn-unused:params"
, "-Ywarn-value-discard"
, "-Xmaxerrs", "1"
, "-Xmaxwarns", "1"
)
ThisBuild / publishTo := Some(" Releases" at "https://nexus.mobile..com/nexus3/repository/releases")
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "", "")
ThisBuild / publishArtifact := true
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
ThisBuild / resolvers += " Releases" at "https://nexus.mobile..com/nexus3/repository/releases"
ThisBuild / resolvers += "zero" at "https://nexus.mobile..com/nexus3/repository/zd"

ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "ext-plug" % ext)

ThisBuild / turbo := true
ThisBuild / useCoursier := true
Global / onChangedBuildSource := ReloadOnSourceChanges

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH
lazy val stats = project.in(file(".")).settings(
  skip in publish := true
, libraryDependencies += "com.." %% "ftier" % frontier
, libraryDependencies += "io.github.zero-deps" %% "kvs-core" % kvs
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
).aggregate(client, api, app, frontier2).dependsOn(client, api).enablePlugins(JavaAppPackaging, DeploySSH)

lazy val client = project.in(file("client")).settings(
  organization := organization.value + ".stats"
, libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akka % Provided
, libraryDependencies += "io.github.zero-deps" %% "ext" % ext % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto % Provided
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Provided
)

lazy val api = project.in(file("api")).settings(
  skip in publish := true
, libraryDependencies += "io.github.zero-deps" %% "proto-purs" % protopurs
, libraryDependencies += "io.github.zero-deps" %% "proto-macros" % proto % Compile
, libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % proto
)

lazy val app = project.in(file("app")).settings(
  skip in publish := true
).dependsOn(frontier2)

lazy val frontier2 = project.in(file("frontier")).settings(
  skip in publish := true
, libraryDependencies += "dev.zio" %% "zio" % "1.0.1"
)

maintainer := ".core.be@.com"