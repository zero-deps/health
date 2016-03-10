package .stats

import akka.actor._

object MetricsListener {
  def init(implicit system:ActorSystem): Unit =
    if (system.settings.config.getBoolean("stats.enabled"))
      system.actorOf(Props(new MetricsListener))
}

class MetricsListener extends Actor with ActorLogging {
  if (!sys.props("java.library.path").contains(":native"))
    sys.props += ("java.library.path" -> (sys.props("java.library.path")+":native"))

  import context.system
  import akka.cluster.Cluster
  val selfAddress = Cluster(system).selfAddress

  import scala.language.postfixOps
  import scala.concurrent.duration._
  import context.dispatcher
  import org.hyperic.sigar._
  val sigar = new Sigar

  val scheduler = system.scheduler
  val schedules = List(
    // Uptime (seconds)
    scheduler.schedule(1 second, 5 seconds) {
      self ! ("sys.uptime"->system.uptime)
    },
    // CPU load ([0,1])
    scheduler.schedule(1 second, 15 seconds) {
      self ! ("cpu.load"->sigar.getCpuPerc.getCombined)
    },
    // Memory (bytes)
    scheduler.schedule(1 second, 1 minute) {
      self ! ("mem.used"->sigar.getMem.getActualUsed)
      self ! ("mem.free"->sigar.getMem.getActualFree)
      self ! ("mem.total"->sigar.getMem.getTotal)
    },
    // FS (KB)
    scheduler.schedule(1 second, 1 hour) {
      import scala.util._
      Try(sigar.getFileSystemUsage("/")) match {
        case Success(usage) =>
          self ! ("root./.used"->usage.getUsed)
          self ! ("root./.free"->usage.getFree)
          self ! ("root./.total"->usage.getTotal)
        case Failure(_) =>
          self ! ("root./.used"->"--")
          self ! ("root./.free"->"--")
          self ! ("root./.total"->"--")
      }
    }
  )

  override def postStop(): Unit = schedules.map(_.cancel())

  def receive: Receive = {
    case (param:String,value) =>
      system.eventStream.publish(StatsClient.Metric(selfAddress,param,value.toString))
  }
}
