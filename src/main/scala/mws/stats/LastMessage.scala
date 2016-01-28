package .stats

import akka.actor.{ Actor, ActorLogging, Props }
import language.implicitConversions
import .stats.LastMessage.DeleteOldData
import .kvs._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import .kvs.handle.`package`.En

object Message {
  val FID = "stats::message"
  type MessageEntry = En[String]

  def apply(msg: String): Message = {
    msg.split("::").toList match {
      case casino :: user :: msg :: time :: Nil =>
        new Message(casino, user, Duration(s"$time nanos"), msg)
      case _ => throw new IllegalArgumentException(msg)
    }
  }

  def apply(msg: MessageEntry): Message = apply(msg.data)

  implicit def messageToEntry(msg: Message): MessageEntry = {
    val fid = FID
    val id = s"${msg.casino}::${msg.user}::${msg.time.toNanos} :: ${msg.msg}"
    En[String](fid, id, data = msg.serialize)
  }
}
case class Message(casino: String, user: String, time: Duration, msg: String) {
  lazy val serialize = s"$casino::$user::$msg::${time.toMillis}"
}

object LastMessage {
  case object Get
  case class Values(it: Iterator[String])
  private case object DeleteOldData

  def props(kvs: Kvs): Props = Props(classOf[LastMessage], kvs)
}

class LastMessage(kvs: Kvs) extends Actor with ActorLogging {
  import context.system
  import Message._
  import .kvs.handle.Handler._

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
      kvs.add[MessageEntry](m)
    case LastMessage.Get =>
      sender ! {
        kvs.entries[MessageEntry](FID, None, Some(1)) match {
          case Left(err) => err
          case Right(entries) => entries.headOption
        }
      }
    case DeleteOldData =>
    //kvs.last.map(Message(_)).map { case last =>
    //  kvs.entries.map(Message(_)).foreach { msg =>
    //    if (last.time - msg.time > timeDiff)
    //      kvs.remove(container,msg)
    //  }
    //}
  }
}
