package stats

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import deployssh.DeploySSH.Keys._
import deployssh.DeploySSH.{ArtifactSSH, ServerConfig}
import fr.janalyse.ssh.{SSHBatch, SSH}
import sbt._

object Deploy {
  lazy val deploy = TaskKey[Unit]("deploy", "Build, package, deploy and restart app")

  val remoteDir = "/home//apps/stats"
  val pidPath = s"$remoteDir/pid"
  val scriptRelativePath = s"bin/${Package.scriptName}"

  lazy val settings = Seq(
    deployConfigs += ServerConfig(
      name = "devcore1",
      host = "172.29.49.43",
      user = Some("")
    ),
    deploySshServersNames += "devcore1",
    deployArtifacts ++= Seq(
      ArtifactSSH(path = (stage in Universal).value, remoteDir)
    ),
    deploySshExecBefore += ((ssh: SSH) => {
      ssh.execute(s"kill $$(cat $pidPath) || true")
    }),
    deploySshExecAfter += ((ssh: SSH) => {
      ssh.executeAll(SSHBatch(List(
        s"cd $remoteDir",
        s"chmod +x $scriptRelativePath",
        s"nohup $scriptRelativePath -J-Xmx128m >/dev/null 2>&1 & echo $$! > $pidPath"
      ))) filter (_.nonEmpty) foreach println
    })
  )
}
