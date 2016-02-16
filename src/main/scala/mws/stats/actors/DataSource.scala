package .stats
package actors

import scalaz._
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import scala.collection.mutable
import scala.annotation.tailrec
import akka.stream.actor.ActorPublisherMessage._
import akka.actor.actorRef2Scala
import .kvs.Kvs
import TreeStorage._

object DataSource {
  case class SourceMsg(data: Data)
  case object QueueUpdated
  def props(kvs: Kvs): Props = Props(classOf[DataSource], kvs)
}

class DataSource(kvs: Kvs) extends ActorPublisher[Data] with Actor with ActorLogging {
  import context.system
  import DataSource._

  override def preStart: Unit = system.eventStream.subscribe(self, classOf[SourceMsg])
  override def postStop: Unit = system.eventStream.unsubscribe(self, classOf[SourceMsg])

  val kvsActor = context.actorOf(KvsActor.props(kvs))
  val MaxBufferSize = 50
  val queue = mutable.Queue[Data]()
  var queueUpdated = false;

  kvsActor ! KvsActor.REQ.GetMetrcis(10) //Get LAST 10 metrics from KVS
  kvsActor ! KvsActor.REQ.GetMessages(10) //Get LAST 10 messages from KVS

  def receive: Receive = {
    case KvsActor.RES.DataList(list) => list.reverse map { x => self ! SourceMsg(x) }
    case KvsActor.RES.Error(msg: String) => log.error(msg)
    case SourceMsg(data) => publishData(data)
    case QueueUpdated => deliver()
    case Request(amount) => deliver()
    case Cancel =>
      log.info(s"publisher canceled $this")
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
    if (totalDemand == 0) log.info(s"No more demand for: $this")
    if (queue.size == 0 && totalDemand != 0) {
      queueUpdated = false
    } else if (totalDemand > 0 && queue.size > 0) {
      onNext(queue.dequeue())
      deliver()
    }
  }
}