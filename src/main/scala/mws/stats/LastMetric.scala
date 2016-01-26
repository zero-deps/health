package .stats

import akka.actor.{Actor,ActorRef,ActorLogging,Props}
import .kvs.Kvs
import .kvs.stats.LastMetricKvs
import .kvs._
import akka.stream.actor.{ActorSubscriber,ActorSubscriberMessage,WatermarkRequestStrategy,ActorPublisher}
import akka.stream.actor.ActorSubscriber._
import akka.stream.actor.ActorSubscriberMessage._
import akka.stream.actor.ActorPublisherMessage._

import akka.routing.{ActorRefRoutee,RemoveRoutee,AddRoutee}

import scala.collection.mutable
import scala.annotation.tailrec
import scala.language.postfixOps

object Metric {
  def apply(str: String): Metric = {
    str.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        new Metric(name, node, param, time, value)
      case _ => throw new IllegalArgumentException(str)
    }
  }
}
case class Metric(name: String, node: String, param: String, time: String, value: String) extends Data {
  lazy val key       = s"$name::$node::$param"
  lazy val serialize = s"$name::$node::$param::$time::$value"
}

object LastMetric {
  case object Get
  case class Values(it: Iterator[String])
  case class Delete(key: String)
  case class QueueUpdated()

  def props(router:ActorRef): Props = Props(new LastMetric("lastmetric",router))
}

class LastMetric(container:String, router:ActorRef) extends ActorSubscriber
  with ActorPublisher[String]
  with ActorLogging {
  import context.system
  import LastMetric.QueueUpdated

  implicit val kvs:Kvs = new LastMetricKvs(container)

  val requestStrategy = WatermarkRequestStrategy(50)
  val MaxBufferSize = 50
  val queue = mutable.Queue[String]()
  var queueUpdated = false;

  override def preStart: Unit = {
    router ! AddRoutee(ActorRefRoutee(self))
    system.eventStream.subscribe(self, classOf[Metric])
    system.eventStream.subscribe(self, classOf[Message])
  }

  override def postStop: Unit = {
    router ! RemoveRoutee(ActorRefRoutee(self))
    system.eventStream.unsubscribe(self)
  }

  def receive: Receive = {
    case m: Metric  => {
      kvs.add(container,m)
      self ! "metric::" + m.serialize
    }
    case m: Message => self ! "msg::" + m.serialize
    case OnNext(msg)=> self ! LastMetric.Delete(msg.toString)
    case OnError(e) => {
      log.error(e.getMessage)
      context.stop(self)
    }
    case OnComplete => {
      log.info(s"subscriber complete $this")
      context.stop(self)
    }
    case LastMetric.Get =>
      sender ! LastMetric.Values(kvs.entries)
    case LastMetric.Delete(key) =>
      kvs.remove(container,key)
    case stats: String =>
      if (queue.size == MaxBufferSize) queue.dequeue()
      queue += stats
      if (!queueUpdated) {
        queueUpdated = true
        self ! QueueUpdated
      }
    case QueueUpdated => deliver()
    case Request(amount) => deliver()
    case Cancel => {
      log.info(s"publisher canceled $this") 
      context.stop(self)
    }
  }

  @tailrec final def deliver(): Unit = {
    if (totalDemand == 0) log.info(s"No more demand for: $this")
    if (queue.size == 0 && totalDemand != 0) {
      queueUpdated = false
    } else if (totalDemand > 0 && queue.size > 0) {
      onNext(queue.dequeue())
      deliver()
    }
  }
}
