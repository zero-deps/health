package .stats

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
      val heap = "%.2f".format(used.doubleValue / 1024 / 1024)
      eventStream.publish(StatsClient.Metric(address,"Heap",heap))
    case _ =>
  }

  def cpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, _, Some(systemLoadAverage), _, _, _) =>
      val cpu = "%.2f".format(systemLoadAverage)
      eventStream.publish(StatsClient.Metric(address,"CPU",cpu))
    case _ =>
  }
}
