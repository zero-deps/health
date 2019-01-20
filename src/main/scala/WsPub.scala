package .stats

import akka.actor.{ ActorLogging, Actor, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage._
import scala.annotation.tailrec

object WsPub {
  def props: Props = Props(new WsPub)
}

class WsPub extends ActorPublisher[Msg] with Actor with ActorLogging {
  import context.system

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[Msg])
  override def postStop: Unit = system.eventStream.unsubscribe(self)

  var buf = Vector.empty[Msg]

  def receive: Receive = {
    case msg: (Stat, StatMeta) =>
      if (buf.isEmpty && totalDemand > 0) {
        onNext(msg)
      } else {
        buf :+= msg
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
