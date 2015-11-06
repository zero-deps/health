package .stats

import akka.actor.{Actor, ActorLogging, Props}
import .stats.LastMetric.LastMetricKvs
import akka.stream.actor.{ActorSubscriber,ActorSubscriberMessage,WatermarkRequestStrategy}
import akka.stream.actor.ActorSubscriber._
import akka.stream.actor.ActorSubscriberMessage._


object Metric {
  def apply(str: String): Metric = {
    str.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        new Metric(name, node, param, time, value)
      case _ => throw new IllegalArgumentException(str)
    }
  }
}
case class Metric(name: String, node: String, param: String, time: String, value: String) 
    extends Kvs.Data {
  lazy val key       = s"$name::$node::$param"
  lazy val serialize = s"$name::$node::$param::$time::$value"
}

object LastMetric {
  case object Get
  case class Values(it: Iterator[String])
  case class Delete(key: String)

  class LastMetricKvs(kvs: Kvs, list: String) extends Kvs.Wrapper(kvs, list) with Kvs.Iterable

  def props(kvs: Kvs): Props = Props(new LastMetric(new LastMetricKvs(kvs, list = "lastmetric")))
}

class LastMetric(kvs: LastMetricKvs) extends ActorSubscriber with ActorLogging {
  import context.system

  val requestStrategy = WatermarkRequestStrategy(50)

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[Metric])

  override def postStop: Unit = system.eventStream.unsubscribe(self, classOf[Metric])

  def receive: Receive = {
    case OnNext(msg)=> self ! LastMetric.Delete(msg.toString)
    case OnError(e) => context.stop(self)
    case OnComplete => context.stop(self)
    case m: Metric =>
      kvs.putToList(m)
    case LastMetric.Get =>
      sender ! LastMetric.Values(kvs.values)
    case LastMetric.Delete(key) =>
      kvs.deleteFromList(key)
  }
}
