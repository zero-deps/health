package .stats

import scala.language.postfixOps
import akka.actor.{ActorRef,Actor,ActorLogging,Props,ActorSystem}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics}
import akka.cluster.metrics.StandardMetrics.{Cpu, HeapMemory}

object MetricsListener {
  def init(implicit system:ActorSystem): Unit =
    if (system.settings.config.getBoolean("stats.enabled"))
      system.actorOf(Props(new MetricsListener))
}

class MetricsListener extends Actor with ActorLogging {
  if (!sys.props("java.library.path").contains(":native"))
    sys.props += ("java.library.path" -> (sys.props("java.library.path")+":native"))

  import context.system
  val selfAddress = Cluster(system).selfAddress
  val metrics = ClusterMetricsExtension(system)
  val eventStream = system.eventStream

  import scala.concurrent.duration._
  import context.dispatcher
  system.scheduler.schedule(5 seconds, 10 seconds) {
    val rt = sys.runtime
    eventStream.publish(StatsClient.Metric(selfAddress,"cpu.count",""+rt.availableProcessors))
    eventStream.publish(StatsClient.Metric(selfAddress,"mem.free",""+rt.freeMemory))
    eventStream.publish(StatsClient.Metric(selfAddress,"mem.max",""+rt.maxMemory))
    eventStream.publish(StatsClient.Metric(selfAddress,"mem.total",""+rt.totalMemory))
    eventStream.publish(StatsClient.Metric(selfAddress,"sys.uptime",""+system.uptime))
    java.io.File.listRoots.map{ root =>
      val path = root.getAbsolutePath
      eventStream.publish(StatsClient.Metric(selfAddress,s"root.${path}.total",""+root.getTotalSpace))
      eventStream.publish(StatsClient.Metric(selfAddress,s"root.${path}.free",""+root.getFreeSpace))
      eventStream.publish(StatsClient.Metric(selfAddress,s"root.${path}.usable",""+root.getUsableSpace))
    }
  }

  override def preStart(): Unit = metrics.subscribe(self)
  override def postStop(): Unit = metrics.unsubscribe(self)

  def receive: Receive = {
    case ClusterMetricsChanged(clusterMetrics) =>
      clusterMetrics.filter(_.address == selfAddress) foreach { nodeMetrics =>
        heap(nodeMetrics)
        cpu(nodeMetrics)
      }
    case state: CurrentClusterState =>
  }

  def heap(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case HeapMemory(address, _, used, _, _) =>
      eventStream.publish(StatsClient.Metric(address,"mem.heap",""+used))
    case _ =>
  }

  def cpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, _, Some(systemLoadAverage), _, _, _) =>
      eventStream.publish(StatsClient.Metric(address,"cpu.load",""+systemLoadAverage))
    case _ =>
  }
}
