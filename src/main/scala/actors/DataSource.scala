package .stats
package actors

import scalaz._
import akka.actor.{ActorLogging, Actor, Props}
import akka.stream.actor.ActorPublisher
import scala.collection.mutable
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
  val MaxBufferSize = 50
  val queue = mutable.Queue[Data]()
  var queueUpdated = false;

  kvsActor ! KvsActor.REQ.GetHistory(count=1000)
  kvsActor ! KvsActor.REQ.GetErrors(count=1000)
  kvsActor ! KvsActor.REQ.GetMetrcis(count=0)

  def receive: Receive = {
    case KvsActor.RES.DataList(list) => list.reverse map { x => self ! SourceMsg(x) }
    case _:KvsActor.RES.Error => log.debug("No data")
    case SourceMsg(data) => publishData(data)
    case QueueUpdated => deliver()
    case Request(amount) => deliver()
    case Cancel =>
      log.debug(s"publisher canceled $this")
      context.stop(self)
  }

  private def publishData(data: Data) = {
    if (queue.size == MaxBufferSize) queue.dequeue()
    queue += data
    if (!queueUpdated) {
      queueUpdated = true
      self ! QueueUpdated
    }
  }
  @tailrec final def deliver(): Unit = {
    if (totalDemand == 0) log.debug(s"No more demand for: $this")
    else if (queue.isEmpty && totalDemand != 0) queueUpdated = false
    else if (totalDemand > 0 && queue.nonEmpty) {
      onNext(queue.dequeue())
      deliver()
    }
  }
}