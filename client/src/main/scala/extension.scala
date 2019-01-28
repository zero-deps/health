package .stats.client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import .stats.macros.Literals

object StatsExtenstion extends ExtensionId[Stats] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Stats = new Stats()(system)
  override def lookup: ExtensionId[Stats] = StatsExtenstion
}

class Stats(implicit system: ActorSystem) extends Extension {
  import system.dispatcher
  import system.log
  import scala.concurrent.duration._

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
    import java.lang.management.ManagementFactory
    import scala.util.{Try, Success, Failure}
    import java.nio.file.{FileSystems, Files}
    import scala.collection.JavaConverters._
    import javax.management.{NotificationBroadcaster, NotificationListener, Notification}

    val os = ManagementFactory.getOperatingSystemMXBean
    val gc = ManagementFactory.getGarbageCollectorMXBeans

    val scheduler = system.scheduler
    // Uptime (seconds)
    scheduler.schedule(1 second, 5 seconds) {
      send(MetricStat("sys.uptime", system.uptime.toString))
    }
    os match {
      case os: com.sun.management.OperatingSystemMXBean =>
        // CPU load (percentage)
        scheduler.schedule(1 second, 5 seconds) {
          os.getSystemCpuLoad match {
            case x if x < 0 => // not available
            case x => send(MetricStat("cpu.load", (100*x).toInt.toString))
          }
        }
        // Memory (Mbytes)
        scheduler.schedule(1 second, 5 seconds) {
          val free = os.getFreePhysicalMemorySize
          val total = os.getTotalPhysicalMemorySize
          val used = total - free
          send(MetricStat("mem.used", (used/i"1'000'000").toString))
          send(MetricStat("mem.free", (free/i"1'000'000").toString))
          send(MetricStat("mem.total", (total/i"1'000'000").toString))
        }
      case _ =>
        log.error("bad os implementation (impossible)")
    }
    gc.asScala.toList.foreach{ gc =>
      gc match {
        case gc: NotificationBroadcaster =>
          gc.addNotificationListener(new NotificationListener {
            def handleNotification(notification: Notification, handback: Any): Unit = {
              import com.sun.management.{GarbageCollectionNotificationInfo => Info}
              import javax.management.openmbean.CompositeData
              if (notification.getType == Info.GARBAGE_COLLECTION_NOTIFICATION) {
                notification.getUserData match {
                  case data: CompositeData => 
                    val info = Info.from(data)
                    send(ActionStat(s"${info.getGcAction} in ${info.getGcInfo.getDuration} ms"))
                  case _ =>
                }
              } else ()
            }
          }, null, null)
        case _ =>
          log.error(s"gc=${gc.getClass.getName} is not a NotificationBroadcaster")
      }
    }
    // FS (Mbytes)
    FileSystems.getDefault.getRootDirectories.asScala.toList.headOption match {
      case Some(root) =>
        Try(Files.getFileStore(root)) match {
          case Success(store) =>
            def usable = Try(store.getUsableSpace)
            def total = Try(store.getTotalSpace)
            scheduler.schedule(1 second, 5 seconds) {
              (usable, total) match {
                case (Success(usable), Success(total)) =>
                  val used = total - usable
                  send(MetricStat("fs./.used", (used/i"1'000'000").toString))
                  send(MetricStat("fs./.free", (usable/i"1'000'000").toString))
                  send(MetricStat("fs./.total", (total/i"1'000'000").toString))
                case _ =>
                  log.error("can't get disk usage")
              }
            }
          case Failure(t) =>
            log.error("can't get file store", t)
        }
      case None =>
        log.error("no root directory (impossible)")
    }
  }
}
