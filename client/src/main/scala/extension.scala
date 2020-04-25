package .stats.client

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import java.lang.management.ManagementFactory
import java.nio.file.{FileSystems, Files}
import javax.management.{NotificationBroadcaster, NotificationListener, Notification}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import zero.ext._, int._

object StatsExtension extends ExtensionId[Stats] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): Stats = new Stats()(system)
  override def lookup: ExtensionId[Stats] = StatsExtension
}

class Stats(implicit system: ActorSystem) extends Extension {
  import system.dispatcher
  import system.log
  import .stats.client.Client._

  private val cfg = system.settings.config

  private val client: ActorRef = {
    val remoteHost = cfg.getString("stats.client.remote.host")
    val remotePort = cfg.getInt("stats.client.remote.port")
    system.actorOf(Client.props(remoteHost, remotePort))
  }

  def metric(name: String, value: String): Unit = {
    client ! MetricStat(name, value)
  }

  def measure[R](name: String)(block: => R): R = {
    val t0 = System.nanoTime
    val result = block
    val t1 = System.nanoTime
    measure(name, ns=t1-t0)
    result
  }

  def measure(name: String, ns: Long): Unit = {
    client ! MeasureStat(name, Math.max(1, ns/i"1'000'000"))
  }

  def action(action: String): Unit = {
    client ! ActionStat(action)
    cpu_mem().foreach(metric("cpu_mem", _))
  }

  def error(exception: String, stacktrace: String, toptrace: String): Unit = {
    client ! ErrorStat(exception, stacktrace, toptrace)
  }

  val gc = ManagementFactory.getGarbageCollectorMXBeans
  val thr = ManagementFactory.getThreadMXBean

  val scheduler = system.scheduler
  object timeout {
    val thr = 5 minutes
    val cpu_mem = 30 seconds
    val fd = 5 minutes
    val fs = 1 hour
    val kvs = 1 day
  }
  // Threads
  scheduler.schedule(1 second, timeout.thr) {
    val all = thr.getThreadCount
    val daemon = thr.getDaemonThreadCount
    val peak = thr.getPeakThreadCount
    val total = thr.getTotalStartedThreadCount
    metric("thr", s"${all}~${daemon}~${peak}~${total}")
  }
  // Uptime (seconds)
  def scheduleUptime(): Unit = {
    val uptime = system.uptime
    val t =
      if (uptime < 60 + 5) 5 seconds
      else if (uptime < 3600) 1 minute
      else 1 hour;
    scheduler.scheduleOnce(t) {
      metric("uptime", uptime.toString)
      scheduleUptime()
    }
  }
  scheduleUptime()
  // CPU and memory
  scheduler.schedule(1 second, timeout.cpu_mem) {
    cpu_mem().foreach(metric("cpu_mem", _))
  }
  // File descriptor count
  scheduler.schedule(1 second, timeout.fd) {
    fd().foreach(metric("fd", _))
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
                  if (info.getGcAction == "end of minor GC") ()
                  else client ! ActionStat(s"${info.getGcAction} in ${info.getGcInfo.getDuration} ms")
                case _ =>
              }
            } else ()
          }
        }, null, null)
      case _ => log.error(s"gc=${gc.getClass.getName} is not a NotificationBroadcaster")
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
                  metric("fs./", s"${usable/i"1'000'000"}~${total/i"1'000'000"}")
                case _ => log.error("can't get disk usage")
              }
            }
          case Failure(t) => log.error("can't get file store", t)
        }
      case None => log.error("no root directory (impossible)")
    }

    scheduler.schedule(1 second, timeout.kvs) {
      val size = getFolderSize(new java.io.File(cfg.getString("ring.leveldb.dir")))
      metric("kvs.size", size.toString)
    }
  }

  def getFolderSize(f: java.io.File): Long = {
    if (f.isDirectory) {
      f.listFiles.foldLeft(0L)((acc, x) => 
        acc + getFolderSize(x)
      )
    } else {
      f.length
    }
  }
}
