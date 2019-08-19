ThisBuild / organization := "com.."
ThisBuild / scalaVersion := "2.13.0"
ThisBuild / version := zd.gs.git.GitOps.version
ThisBuild / fork := true
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-target:jvm-1.8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-Ywarn-unused:imports",
)
ThisBuild / publishTo := Some(" Releases" at "http://nexus.mobile..com/nexus3/repository/releases")
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / publishArtifact := true
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
ThisBuild / resolvers += " Releases" at "http://nexus.mobile..com/nexus3/repository/releases"
ThisBuild / resolvers += Resolver.jcenterRepo

ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "gs-plug" % "1.4.3")
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "gs-meta" % "1.4.3"
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "gs-ops" % "1.4.3"
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "gs-z" % "1.4.3"

ThisBuild / libraryDependencies += "io.github.zero-deps" %% "proto-macros" % "1.3.1" % Compile
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % "1.3.1" 

ThisBuild / turbo := true
ThisBuild / useCoursier := false
Global / onChangedBuildSource := ReloadOnSourceChanges

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH
lazy val stats = project.in(file(".")).settings(
  libraryDependencies += "com.." %% "ftier" % "2.0.2",
  libraryDependencies += "io.github.zero-deps" %% "kvs" % "4.6.2",
  mainClass in (Compile, run) := Some(".stats.StatsApp"),
  deployConfigs ++= Seq(
    ServerConfig(name="mon", host="ua--monitoring.ee..corp", user=Some("")),
  ),
  deployArtifacts ++= Seq(
    ArtifactSSH((packageBin in Universal).value, "stats")
  ),
  deploySshExecBefore ++= Seq(
    (ssh: SSH) => ssh.shell{ shell =>
      shell.execute("cd stats")
      shell.execute("touch pid")
      val pid = shell.execute("cat pid")
      if (pid != "") {
        shell.execute(s"kill ${pid}; sleep 5; kill -9 ${pid}")
      } else ()
      shell.execute("rm pid")
    }
  ),
  deploySshExecAfter ++= Seq(
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
  ),
).aggregate(client).dependsOn(client).enablePlugins(JavaAppPackaging, DeploySSH)

lazy val client = project.in(file("client")).settings(
  organization := organization.value + ".stats",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.23",
)

lazy val prjs = project.in(file("prjs")).settings(
  libraryDependencies += "io.github.zero-deps" %% "proto-purs" % "1.3.1",
).dependsOn(stats)
