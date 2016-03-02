package .stats

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.metrics.StandardMetrics.{Cpu, HeapMemory}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics}
import scala.concurrent.duration.Duration
import actors.DataSource

object MetricsListener {
  def props: Props = Props(new MetricsListener)
}

class MetricsListener extends Actor with ActorLogging {
  import context.system
  import DataSource._

  val selfAddress = Cluster(system).selfAddress
  val metrics = ClusterMetricsExtension(system)

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
      publish(address, "Heap", heap)
    case _ =>
  }

  def cpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, _, Some(systemLoadAverage), _, _, _) =>
      val cpu = "%.2f".format(systemLoadAverage)
      publish(address, "CPU", cpu)
    case _ =>
  }

  def publish(address: Address, param: String, value: String): Unit = {
    val name = system.name
    val node = s"${address.host.getOrElse("")}:${address.port.getOrElse("")}"
    val time = Duration(s"${System.currentTimeMillis.toString} ms")
    system.eventStream.publish(SourceMsg(Metric(name, node, param, time, value)))
  }
}
