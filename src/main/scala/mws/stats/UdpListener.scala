package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress

object  UdpListener {
  def props(host: String, port: Int): Props = Props(new UdpListener(host, port))
}

class UdpListener(host: String, port: Int) extends Actor with ActorLogging {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(host, port))

  def receive: Receive = {
    case Udp.Bound(_) =>
      val socket = sender
      context become (ready(sender))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      data.decodeString("UTF-8").split('#') match {
        case Array(node, param, time, value) =>
          //todo publish to event bus
        case _ =>
      }
    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
  }
}
