package .stats.client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

object StatsExtenstion extends ExtensionId[Stats] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Stats = new Stats()(system)
  override def lookup: ExtensionId[Stats] = StatsExtenstion
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

  private val cfg = system.settings.config
  private val enabled = cfg.getBoolean("stats.client.enabled")

  private val client: Option[ActorRef] =
    if (enabled) {
      val remote = (
        cfg.getString("stats.client.remote.host"),
        cfg.getInt("stats.client.remote.port"),
      )
      val local = (
        cfg.getString("akka.remote.netty.tcp.hostname"),
        cfg.getString("akka.remote.netty.tcp.port"),
      )
      Some(system.actorOf(Client.props(remote, local)))
    } else None

  private def send(m: Stat): Unit = {
    client map (_ ! m)
  }

  def measure[R](name: String)(block: => R): R = {
    val t0 = System.nanoTime
    val result = block
    val t1 = System.nanoTime
    send(MetricStat(name, (t1-t0).toString))
    result
  }

  def action(user: String, action: String): Unit = {
    send(ActionStat(user, action))
  }

  def error(className: String, message: String, stacktrace: String): Unit = {
    send(ErrorStat(className, message, stacktrace))
  }

  if (enabled) {
    val sigar = new Sigar

    val scheduler = system.scheduler
    // Uptime (seconds)
    scheduler.schedule(1 second, 5 seconds) {
      send(MetricStat("sys.uptime", system.uptime.toString))
    }
    // CPU load ([0,1])
    scheduler.schedule(1 second, 15 seconds) {
      send(MetricStat("cpu.load", sigar.getCpuPerc.getCombined.toString))
    }
    // Memory (bytes)
    scheduler.schedule(1 second, 1 minute) {
      send(MetricStat("mem.used", sigar.getMem.getActualUsed.toString))
      send(MetricStat("mem.free", sigar.getMem.getActualFree.toString))
      send(MetricStat("mem.total", sigar.getMem.getTotal.toString))
    }
    // FS (KB)
    scheduler.schedule(1 second, 1 hour) {
      import scala.util._
      Try(sigar.getFileSystemUsage("/")) match {
        case Success(usage) =>
          send(MetricStat("root./.used", usage.getUsed.toString))
          send(MetricStat("root./.free", usage.getFree.toString))
          send(MetricStat("root./.total", usage.getTotal.toString))
        case Failure(_) =>
          send(MetricStat("root./.used", "--"))
          send(MetricStat("root./.free", "--"))
          send(MetricStat("root./.total", "--"))
      }
    }
  }
}
