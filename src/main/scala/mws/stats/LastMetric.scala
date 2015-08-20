package .stats

import akka.actor.{Actor, ActorLogging, Props}
import .stats.LastMetric.LastMetricKvs

case class Metric(name: String, node: String, param: String, time: String, value: String) 
    extends Kvs.Data {
  lazy val key = s"$name#$node#$param"
  lazy val serialize = s"$name#$node#$param#$time#$value"
}

object LastMetric {
  case object Get
  case class Values(it: Iterator[String])
  case class Delete(key: String)

  class LastMetricKvs(kvs: Kvs, list: String) extends Kvs.Wrapper(kvs, list) with Kvs.Iterable

  def props(kvs: Kvs): Props = Props(new LastMetric(new LastMetricKvs(kvs, list = "lastmetric")))
}

class LastMetric(kvs: LastMetricKvs) extends Actor with ActorLogging {
  import context.system

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[Metric])

  override def postStop: Unit = system.eventStream.unsubscribe(self, classOf[Metric])

  def receive: Receive = {
    case m: Metric =>
      kvs.putToList(m)
    case LastMetric.Get =>
      sender ! LastMetric.Values(kvs.iterator)
    case LastMetric.Delete(key) =>
      kvs.deleteFromList(key)
  }
}
