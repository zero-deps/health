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
import scala.util.Failure

object UdpListener {
  def props: Props = Props(classOf[UdpListener])
}

class UdpListener extends ActorPublisher[String] with Actor with ActorLogging {
  import context.system
  import UdpListener._

  var buf = Vector.empty[String]
  val MaxBufferSize = 10000

  val config = system.settings.config
  val hostname = config.getString("hostname")
  val udpPort = config.getInt("udp.port")

  log.info(s"Starting UDP listener on $hostname:$udpPort...")

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(hostname, udpPort))

  def receive: Receive = {
    case msg @ Udp.Bound(_) =>
      log.info(s"Received Udp.Bound: $msg...")
      val socket = sender
      context become ready(sender)
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      val decoded = data.decodeString("UTF-8")
      log.debug(s"Received: $decoded")

      if (buf.isEmpty && totalDemand > 0)
        onNext(decoded)
      else {
        buf :+= decoded
        deliverBuf()
      }

    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
    case Request(_) => deliverBuf()
    case Cancel => {
      log.info(s"publisher canceled $this")
      context.stop(self)
    }
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
