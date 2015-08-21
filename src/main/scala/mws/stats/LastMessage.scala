package .stats

import akka.actor.{Actor, ActorLogging, Props}
import language.implicitConversions
import .stats.LastMessage.{DeleteOldData, LastMessageKvs}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Message {
  def apply(str: String): Message = {
    str.split("::").toList match {
      case casino :: user :: msg :: time :: Nil =>
        new Message(casino, user, msg, Duration(time + "ms"))
      case _ => throw new IllegalArgumentException(str)
    }
  }
}
case class Message(casino: String, user: String, msg: String, time: Duration)
    extends Kvs.Data {
  lazy val key       = s"$casino::$user::$msg::$time"
  lazy val serialize = s"$casino::$user::$msg::$time"
}

object LastMessage {
  case object Get
  case class Values(it: Iterator[String])
  private case object DeleteOldData

  class LastMessageKvs(kvs: Kvs, list: String) extends Kvs.Wrapper(kvs, list) with Kvs.Iterable

  def props(kvs: Kvs): Props = Props(new LastMessage(new LastMessageKvs(kvs, list = "lastmsg")))
}

class LastMessage(kvs: LastMessageKvs) extends Actor with ActorLogging {
  import context.system

  val cfg = system.settings.config
  val timeDiff = Duration(cfg.getString("kvs.message.time-diff"))

  {
    val duration = FiniteDuration(timeDiff.length, timeDiff.unit)
    system.scheduler.schedule(duration, duration, self, DeleteOldData)(executor = global)
  }

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[Message])

  override def postStop: Unit = system.eventStream.unsubscribe(self, classOf[Message])

  def receive: Receive = {
    case m: Message =>
      kvs.putToList(m)
    case LastMessage.Get =>
      sender ! LastMessage.Values(kvs.values)
    case DeleteOldData =>
      kvs.last.map(Message(_)).map { case last =>
        kvs.values.map(Message(_)).foreach { msg =>
          if (last.time - msg.time > timeDiff)
            kvs.deleteFromList(msg.key)
        }
      }
  }
}
