package 
package stats

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.io.{ IO, Udp }
import java.net.InetSocketAddress
import akka.stream.actor.ActorPublisher
import scala.annotation.tailrec
import akka.stream.actor.ActorPublisherMessage._
import scala.collection.mutable
import scala.concurrent.duration.Duration
import handlers._

object UdpListener {
  case object QueueUpdated

  def props: Props = Props(classOf[UdpListener])
}

class UdpListener extends ActorPublisher[Data] with Actor with ActorLogging {
  import context.system
  import UdpListener._

  val MaxBufferSize = 50
  val queue = mutable.Queue[Data]()
  var queueUpdated = false;

  val config = system.settings.config
  val hostname = config.getString("hostname")
  val udpPort = config.getInt("udp.port")

  log.info(s"Starting UDP listener on $hostname:$udpPort...")

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(hostname, udpPort))

  def receive: Receive = {
    case msg @ Udp.Bound(_) =>
      log.info(s"Received Udp.Bound: $msg...")
      val socket = sender
      context become (ready(sender))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      val decoded = data.decodeString("UTF-8")
      log.debug(s"Received: $decoded")
      handler.udpMessage(decoded) map { message =>
        if (queue.size == MaxBufferSize) queue.dequeue()
        queue += message
        if (!queueUpdated) {
          queueUpdated = true
          self ! QueueUpdated
        }
      }
    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
    case QueueUpdated => deliver()
    case Request(amount) => deliver()
    case Cancel => {
      log.info(s"publisher canceled $this")
      context.stop(self)
    }
  }

  @tailrec final def deliver(): Unit = {
    if (totalDemand == 0) log.debug(s"No more demand for: $this")
    if (queue.size == 0 && totalDemand != 0) {
      queueUpdated = false
    } else if (totalDemand > 0 && queue.size > 0) {
      onNext(queue.dequeue())
      deliver()
    }
  }
}
