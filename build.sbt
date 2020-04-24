val akkaVersion = "2.5.26"
val frontierVersion = "2.0.2"
val gsVersion = "1.6.2"
val kvsVersion = "4.9.1"
val protoVersion = "1.7.0"

ThisBuild / organization := "com.."
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / version := zd.gs.git.GitOps.version
ThisBuild / fork := true
ThisBuild / cancelable in Global := true
ThisBuild / scalacOptions in Compile ++= Vector(
  "-target:jvm-12",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-Ywarn-unused:imports",
)
ThisBuild / publishTo := Some(" Releases" at "https://nexus.mobile..com/nexus3/repository/releases")
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "", "")
ThisBuild / publishArtifact := true
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
ThisBuild / resolvers += " Releases" at "https://nexus.mobile..com/nexus3/repository/releases"
ThisBuild / resolvers += "zd" at "https://nexus.mobile..com/nexus3/repository/zd"

ThisBuild / libraryDependencies += compilerPlugin("io.github.zero-deps" %% "gs-plug" % gsVersion)
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "gs-meta" % gsVersion
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "gs-z" % gsVersion

ThisBuild / libraryDependencies += "io.github.zero-deps" %% "proto-macros" % protoVersion % Compile
ThisBuild / libraryDependencies += "io.github.zero-deps" %% "proto-runtime" % protoVersion

ThisBuild / turbo := true
ThisBuild / useCoursier := true
Global / onChangedBuildSource := ReloadOnSourceChanges

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH
lazy val stats = project.in(file(".")).settings(
  libraryDependencies += "com.." %% "ftier" % frontierVersion,
  libraryDependencies += "io.github.zero-deps" %% "kvs" % kvsVersion,
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
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
)

lazy val gen = project.in(file("gen")).settings(
  libraryDependencies += "io.github.zero-deps" %% "proto-purs" % protoVersion,
).dependsOn(stats)
