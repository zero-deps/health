package .stats.client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import java.lang.management.ManagementFactory
import java.nio.file.{FileSystems, Files}
import javax.management.{NotificationBroadcaster, NotificationListener, Notification}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import zd.gs.meta.Literals

object StatsExtenstion extends ExtensionId[Stats] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Stats = new Stats()(system)
  override def lookup: ExtensionId[Stats] = StatsExtenstion
}

class Stats(implicit system: ActorSystem) extends Extension {
  import system.dispatcher
  import system.log

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
    val os = ManagementFactory.getOperatingSystemMXBean
    val gc = ManagementFactory.getGarbageCollectorMXBeans
    val thr = ManagementFactory.getThreadMXBean

    val scheduler = system.scheduler
    object timeout {
      val thr = 5 minutes
      val cpu_mem = 30 seconds
      val fd = 5 minutes
      val fs = 1 hour
      // val thr = 5 seconds
      // val cpu_mem = 5 seconds
      // val fd = 5 seconds
      // val fs = 5 seconds
    }
    // Threads
    scheduler.schedule(1 second, timeout.thr) {
      val all = thr.getThreadCount
      val daemon = thr.getDaemonThreadCount
      val peak = thr.getPeakThreadCount
      val total = thr.getTotalStartedThreadCount
      send(MetricStat("thr", s"${all}~${daemon}~${peak}~${total}"))
    }
    // Uptime (seconds)
    def scheduleUptime(): Unit = {
      val uptime = system.uptime
      val t =
        if (uptime < 60 + 5) 5 seconds
        else if (uptime < 3600) 1 minute
        else 1 hour;
      scheduler.scheduleOnce(t) {
        send(MetricStat("uptime", uptime.toString))
        scheduleUptime()
      }
    }
    scheduleUptime()
    os match {
      case os: com.sun.management.OperatingSystemMXBean =>
        scheduler.schedule(1 second, timeout.cpu_mem) {
          // CPU load (percentage)
          val cpu = os.getSystemCpuLoad match {
            case x if x < 0 => "" // not available
            case x => (100*x).toInt.toString
          }
          // Memory (Mbytes)
          val free = os.getFreePhysicalMemorySize
          val total = os.getTotalPhysicalMemorySize
          send(MetricStat("cpu_mem", s"${cpu}~${free/i"1'000'000"}~${total/i"1'000'000"}"))
        }
        os match {
          case os: com.sun.management.UnixOperatingSystemMXBean =>
            // File descriptor count
            scheduler.schedule(1 second, timeout.fd) {
              val open = os.getOpenFileDescriptorCount
              val max = os.getMaxFileDescriptorCount
              send(MetricStat("fd", s"${open}~${max}"))
            }
          case _ => log.info("unix descriptors are unavailable")
        }
      case _ => log.error("bad os implementation (impossible)")
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
        case _ => log.error(s"gc=${gc.getClass.getName} is not a NotificationBroadcaster")
      }
    }
    // FS (Mbytes)
    FileSystems.getDefault.getRootDirectories.asScala.toList.headOption match {
      case Some(root) =>
        Try(Files.getFileStore(root)) match {
          case Success(store) =>
            def usable = Try(store.getUsableSpace)
            def total = Try(store.getTotalSpace)
            scheduler.schedule(1 second, timeout.fs) {
              (usable, total) match {
                case (Success(usable), Success(total)) =>
                  send(MetricStat("fs./", s"${usable/i"1'000'000"}~${total/i"1'000'000"}"))
                case _ => log.error("can't get disk usage")
              }
            }
          case Failure(t) => log.error("can't get file store", t)
        }
      case None => log.error("no root directory (impossible)")
    }
  }
}
