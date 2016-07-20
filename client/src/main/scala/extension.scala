package stats
package client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionKey}


object Stats extends ExtensionKey[Stats] {
  override def lookup = Stats
  override def createExtension(system: ExtendedActorSystem): Stats = new Stats()(system)
}

class Stats(implicit system: ActorSystem) extends Extension {
  import system.dispatcher
  import scala.concurrent.duration._
  import org.hyperic.sigar._
  import scala.language.postfixOps

  { // Sigar loader
    import org.slf4j.bridge.SLF4JBridgeHandler
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    kamon.sigar.SigarProvisioner.provision()
  }

  private val config = system.settings.config
  private val enabled = config.hasPath("stats.client.enabled") && config.getBoolean("stats.client.enabled")

  private val client: Option[ActorRef] = if (enabled) {
                 val remote = config.getString("stats.client.remote.host")
                 val port = config.getInt("stats.client.remote.port")
                 Some(Client.create(remote, port))
               } else None


  def send (m : Any ) = client match {
    case Some(c) => c ! m
    case None =>
  }


  if(enabled){
    val sigar = new Sigar

    val scheduler = system.scheduler
    // Uptime (seconds)
    scheduler.schedule(1 second, 5 seconds) {
      send ("sys.uptime", system.uptime)
    }
    // CPU load ([0,1])
    scheduler.schedule(1 second, 15 seconds) {
      send ("cpu.load", sigar.getCpuPerc.getCombined)
    }
    // Memory (bytes)
    scheduler.schedule(1 second, 1 minute) {
      send ("mem.used", sigar.getMem.getActualUsed)
      send ("mem.free", sigar.getMem.getActualFree)
      send ("mem.total", sigar.getMem.getTotal)
    }
    // FS (KB)
    scheduler.schedule(1 second, 1 hour) {
      import scala.util._
      Try(sigar.getFileSystemUsage("/")) match {
        case Success(usage) =>
          send ("root./.used", usage.getUsed)
          send ("root./.free", usage.getFree)
          send ("root./.total", usage.getTotal)
        case Failure(_) =>
          send ("root./.used", "--")
          send ("root./.free", "--")
          send ("root./.total", "--")
      }
    }
  }

}