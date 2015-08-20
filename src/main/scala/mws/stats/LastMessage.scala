package .stats

import akka.actor.{Actor, ActorLogging, Props}
import language.implicitConversions
import .stats.LastMessage.LastMessageKvs

case class Message(casino: String, user: String, msg: String, time: String) 
    extends Kvs.Data {
  lazy val key = s"$casino#$user"
  lazy val serialize = s"$casino#$user#$msg#$time"
}

object LastMessage {
  case object Get
  case class Values(it: Iterator[String])

  class LastMessageKvs(kvs: Kvs, list: String) extends Kvs.Wrapper(kvs, list) with Kvs.Iterable

  def props(kvs: Kvs): Props = Props(new LastMessage(new LastMessageKvs(kvs, list = "lastmsg")))
}

class LastMessage(kvs: LastMessageKvs) extends Actor with ActorLogging {
  import context.system

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[Message])

  override def postStop: Unit = system.eventStream.unsubscribe(self, classOf[Message])

  def receive: Receive = {
    case m: Message =>
      kvs.putToList(m)
    case LastMessage.Get =>
      sender ! LastMetric.Values(kvs.iterator)
  }
}
