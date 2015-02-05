package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress

object UdpListener {
  def props(port: Int, kvs: ActorRef): Props = Props(new UdpListener(port, kvs))
}

class UdpListener(port: Int, kvs: ActorRef) extends Actor with ActorLogging {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", port))

  def receive: Receive = {
    case Udp.Bound(_) =>
      val socket = sender
      context become (ready(sender))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      data.decodeString("UTF-8").split('^') match {
        case Array(key, value) =>
          kvs ! StatsKvs.Put(key, value)
        case _ =>
      }
    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
  }
}
