package .stats

import  akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress

object UdpListener {
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
      val decoded = data.decodeString("UTF-8")
      system.eventStream.publish(Metric(decoded))
    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
  }
}
