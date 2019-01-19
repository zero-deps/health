package .stats

import akka.actor.{ ActorLogging, Actor, Props }
import akka.stream.actor.ActorPublisher
import scala.annotation.tailrec
import akka.stream.actor.ActorPublisherMessage._
import .kvs.Kvs

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))

  final case class BulkMsg(v: List[Msg])
}

class KvsPub(kvs: Kvs) extends ActorPublisher[Msg] with Actor with ActorLogging {
  import KvsPub.{BulkMsg}

  var buf = Vector.empty[Msg]

  self ! "init data"

  def receive: Receive = {
    case "init data" =>
      //todo: get first N actions/errors/metrics(distinct) from kvs
      val xs: List[Msg] = Nil
      self ! xs
    case BulkMsg(ms) =>
      if (buf.isEmpty && totalDemand > 0 && ms.nonEmpty) {
        buf ++= ms.tail
        onNext(ms.head)
      } else {
        buf ++= ms
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      log.debug(s"publisher canceled $this")
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}
