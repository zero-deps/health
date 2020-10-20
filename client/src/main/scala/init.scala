package metrics.client

import akka.actor._
import java.lang.management.ManagementFactory
import java.io.File, java.nio.file.{FileSystems, Files}
import javax.management.{NotificationBroadcaster, NotificationListener, Notification}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import zero.ext._, int._

object Stats {
  def apply(leveldbDir: String)(as: ActorSystem): Stats = new Stats(leveldbDir)(as)
}

class Stats(leveldbDir: String)(implicit system: ActorSystem) {
  import system.dispatcher
  import system.log
  import Client._

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

  def error(msg: Option[String], cause: Throwable): Unit = {
    client ! toErrorStat(msg, cause)
  }

  val gc = ManagementFactory.getGarbageCollectorMXBeans
  val thr = ManagementFactory.getThreadMXBean

  val scheduler = system.scheduler
  // scheduler.schedule(1 second, 5 seconds) {
  //   error(Some("hello"), new Exception("ex"))
  // }
  object timeout {
    val thr = 5 minutes
    val cpu_mem = 1 minute
    val fd = 5 minutes
    val fs = 1 hour
    val kvs = 1 day
  }
  /* Threads */
  scheduler.schedule(1 second, timeout.thr) {
    val all = thr.getThreadCount
    val daemon = thr.getDaemonThreadCount
    val peak = thr.getPeakThreadCount
    val total = thr.getTotalStartedThreadCount
    metric("thr", s"${all}~${daemon}~${peak}~${total}")
  }
  /* Uptime (in seconds) */
  def scheduleUptime(): Unit = {
    val uptime = system.uptime
    val t =
      if (uptime < 60 + 5) 5 seconds
      else if (uptime < 3600) 1 minute
      else 1 hour;
    scheduler.scheduleOnce(t) {
      metric(id.uptime, uptime.toString)
      scheduleUptime()
    }
    ()
  }
  scheduleUptime()
  /* CPU and memory */
  scheduler.schedule(1 second, timeout.cpu_mem) {
    cpu_mem().foreach(metric("cpu_mem", _))
  }
  /* File descriptor count */
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
  }

  /* FS (Mbytes) */
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
    val size = getFolderSize(new File(leveldbDir))
    metric("kvs.size", size.toString)
  }

  def getFolderSize(f: File): Long = {
    if (f.isDirectory) {
      f.listFiles.foldLeft(0L)((acc, x) => 
        acc + getFolderSize(x)
      )
    } else {
      f.length
    }
  }

  log.info("Client is configured")
}
