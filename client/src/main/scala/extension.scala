package .stats.client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import .stats.macros.Literals

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
      val localPort = cfg.getString("akka.remote.netty.tcp.port")
      Some(system.actorOf(Client.props(remote, localPort)))
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

  def action(action: String): Unit = {
    send(ActionStat(action))
  }

  def error(exception: String, stacktrace: String, toptrace: String): Unit = {
    send(ErrorStat(exception, stacktrace, toptrace))
  }

  if (enabled) {
    val sigar = new Sigar

    val scheduler = system.scheduler
    // Uptime (seconds)
    scheduler.schedule(1 second, 5 seconds) {
      send(MetricStat("sys.uptime", system.uptime.toString))
    }
    // CPU load ([0,100])
    scheduler.schedule(1 second, 5 seconds) {
      send(MetricStat("cpu.load", (100*sigar.getCpuPerc.getCombined).toInt.toString))
    }
    // Memory (Mbytes)
    scheduler.schedule(1 second, 5 seconds) {
      send(MetricStat("mem.used", (sigar.getMem.getActualUsed/i"1'000'000").toString))
      send(MetricStat("mem.free", (sigar.getMem.getActualFree/i"1'000'000").toString))
      send(MetricStat("mem.total", (sigar.getMem.getTotal/i"1'000'000").toString))
    }
    // FS (Mbytes)
    scheduler.schedule(1 second, 5 seconds) {
      import scala.util._
      Try(sigar.getFileSystemUsage("/")) match {
        case Success(usage) =>
          send(MetricStat("fs./.used", (usage.getUsed/i"1'000").toString))
          send(MetricStat("fs./.free", (usage.getFree/i"1'000").toString))
          send(MetricStat("fs./.total", (usage.getTotal/i"1'000").toString))
        case Failure(_) =>
      }
    }
  }
}
