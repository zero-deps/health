package .stats
package actors

import akka.actor.{ ActorLogging, Actor, Props }
import akka.stream.actor.ActorPublisher
import scala.annotation.tailrec
import akka.stream.actor.ActorPublisherMessage._
import .kvs.Kvs

object DataSource {
  case class SourceMsg(data: Data)
  case object QueueUpdated
  def props(kvs: Kvs): Props = Props(classOf[DataSource], kvs)
}

class DataSource(kvs: Kvs) extends ActorPublisher[Data] with Actor with ActorLogging {
  import context.system
  import DataSource._

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[SourceMsg])
  override def postStop: Unit = system.eventStream.unsubscribe(self)

  val kvsActor = context.actorOf(KvsActor.props(kvs))
  val MaxBufferSize = 10000
  var buf = Vector.empty[Data]

  kvsActor ! KvsActor.REQ.GetHistory(count = 1000)
  kvsActor ! KvsActor.REQ.GetErrors(count = 1000)
  kvsActor ! KvsActor.REQ.GetMetrcis(count = 1000)

  def receive: Receive = {
    case KvsActor.RES.DataList(list) =>
      list.reverse map {
        x => self ! SourceMsg(x)
      }
    case _: KvsActor.RES.Error => log.debug("No data")
    case SourceMsg(data) =>
      if (buf.isEmpty && totalDemand > 0)
        onNext(data)
      else {
        buf :+= data
        deliverBuf()
      }
    case Request(_) => deliverBuf()
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
