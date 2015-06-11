package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.metrics.StandardMetrics.{Cpu, HeapMemory}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics}

object MetricsListener {
  def props(stats: ActorRef): Props = Props(new MetricsListener(stats))
}

class MetricsListener(stats: ActorRef) extends Actor with ActorLogging {
  val selfAddress = Cluster(context.system).selfAddress
  val metrics = ClusterMetricsExtension(context.system)

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
      stats ! StatsClient.Data(address, "Heap", heap)
    case _ =>
  }

  def cpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, _, Some(systemLoadAverage), _, _, _) =>
      val cpu = "%.2f".format(systemLoadAverage)
      stats ! StatsClient.Data(address, "CPU", cpu)
    case _ =>
  }
}
