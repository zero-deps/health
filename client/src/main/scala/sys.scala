package metrics

import com.sun.management.{OperatingSystemMXBean, UnixOperatingSystemMXBean}
import javax.management.{NotificationBroadcaster, NotificationListener, Notification}
import java.lang.management.ManagementFactory
import java.io.File, java.nio.file.{FileSystems, Files}
import scala.jdk.CollectionConverters._
import zero.ext._, int._, option._
import zio._

object sys {
  def uptime_sec(): UIO[Long] = {
    for {
      ms <- IO.effectTotal(ManagementFactory.getRuntimeMXBean.getUptime)
    } yield ms / i"1'000"
  }

  def cpu_mem(): UIO[Option[api.MetricMsg]] = {
    IO.effectTotal(ManagementFactory.getOperatingSystemMXBean match {
      case os: OperatingSystemMXBean =>
        /* CPU load (percentage) */
        val cpu = os.getCpuLoad match {
          case x if x < 0 => "" /* not available */
          case x => (100*x).toInt.toString
        }
        /* Memory (Mbytes) */
        val free  = os.getFreeMemorySize
        val total = os.getTotalMemorySize
        val mem = ManagementFactory.getMemoryMXBean
        val heapMem = mem.getHeapMemoryUsage.getUsed
        api.MetricMsg.cpu_mem(cpu=cpu, free=free/i"1'000'000", total=total/i"1'000'000", heap=heapMem/i"1'000'000").some
      case _ => none
    })
  }

  def fileD(): UIO[Option[api.MetricMsg]] = {
    IO.effectTotal(ManagementFactory.getOperatingSystemMXBean match {
      case os: UnixOperatingSystemMXBean =>
        /* file descriptor count */
        val open = os.getOpenFileDescriptorCount
        val max  = os.getMaxFileDescriptorCount
        api.MetricMsg.fileD(open=open, max=max).some
      case _ => none
    })
  }

  def threads(): UIO[api.MetricMsg] = {
    import ManagementFactory.{getThreadMXBean=>thr}
    for {
      all    <- IO.effectTotal(thr.getThreadCount)
      daemon <- IO.effectTotal(thr.getDaemonThreadCount)
      peak   <- IO.effectTotal(thr.getPeakThreadCount)
      total  <- IO.effectTotal(thr.getTotalStartedThreadCount)
    } yield api.MetricMsg.threads(all=all, daemon=daemon, peak=peak, total=total)
  }

  def dirSize(f: File): Long = {
    if (f.isDirectory) f.listFiles.foldLeft(0L)((acc, x) => acc + dirSize(x))
    else f.length
  }

  def fs(): UIO[Option[api.MetricMsg]] =
    IO.effectTotal(
      FileSystems.getDefault.getRootDirectories.asScala.toList.headOption.flatMap{ root =>
        util.Try(Files.getFileStore(root)).toOption.flatMap{ store =>
          def usable = util.Try(store.getUsableSpace).toOption
          def total  = util.Try(store.getTotalSpace ).toOption
          (usable, total) match {
            case (Some(usable), Some(total)) =>
              api.MetricMsg.fs(usable=usable/i"1'000'000", total=total/i"1'000'000").some
            case _ => none
          }
        }
      }
    )

  def gcEvent(): UIO[api.ActionMsg] = {
    IO.effectAsync { callback =>
      gcEvent {
        case msg => callback(IO.succeed(msg))
      }
    }
  }

  def gcEvent(cb: api.ActionMsg => Unit): Unit = {
    ManagementFactory.getGarbageCollectorMXBeans.asScala.toList.foreach{
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
                  else cb(api.ActionMsg.gc(name=info.getGcAction, t=info.getGcInfo.getDuration))
                case _ =>
              }
            } else ()
          }
        }, null, null)
      case _ =>
    }
  }
}
