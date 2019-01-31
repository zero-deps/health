ThisBuild / organization := "com.."
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := org.eclipse.jgit.api.Git.open(file(".")).describe().call()
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
  "-Ywarn-unused-import",
)
ThisBuild / publishTo := Some(" Releases" at "http://nexus.mobile..com/nexus3/repository/releases")
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.mobile..com", "", "")
ThisBuild / publishArtifact := true
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
ThisBuild / resolvers += " Releases" at "http://nexus.mobile..com/nexus3/repository/releases"

import deployssh.DeploySSH.{ServerConfig, ArtifactSSH}
import fr.janalyse.ssh.SSH
lazy val stats = project.in(file(".")).settings(
  libraryDependencies += "com.." %% "ftier" % "2.0.1",
  libraryDependencies += "com.." %% "kvs" % "4.1.2",
  mainClass in (Compile, run) := Some(".stats.StatsApp"),
  deployConfigs ++= Seq(
    ServerConfig(name="cms2", host="ua--newcms2", user=Some("anle")),
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
      val name = (packageName in Universal).value
      shell.execute(s"rm -v -rf *")
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
        shell.execute(s"rm -v -rf ${name}")
        shell.execute(s"unzip -q -o ${name}.zip")
        shell.execute(s"nohup ./${name}/bin/${script} -Dconfig.file=/home/anle/stats/app.conf &")
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
).aggregate(client, macros).dependsOn(client).enablePlugins(JavaAppPackaging, DeploySSH)

lazy val client = project.in(file("client")).settings(
  organization := organization.value + ".stats",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.19",
).dependsOn(macros).aggregate(macros)

lazy val macros = project.in(file("macros")).settings(
  organization := organization.value + ".stats",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Compile, 
)
