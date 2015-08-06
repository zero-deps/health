package .stats

import  akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress

object UdpListener {
  def props: Props = Props(new UdpListener)
}

class UdpListener extends Actor with ActorLogging {
  import context.system

  val config = system.settings.config
  val hostname = config.getString("hostname")
  val udpPort = config.getInt("udp.port")

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(hostname, udpPort))

  def receive: Receive = {
    case Udp.Bound(_) =>
      val socket = sender
      context become (ready(sender))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      val decoded = data.decodeString("UTF-8")
      log.debug(s"Received: $decoded")
      decoded.split("::").toList match {
        case "metric" :: name :: node :: param :: time :: value :: Nil =>
          system.eventStream.publish(Metric(name, node, param, time, value))
        case "message" :: casino :: user :: msg :: time :: Nil =>
          system.eventStream.publish(Message(casino, user, msg, time))
        case _ =>
      }
    case "close" =>
      socket ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
  }
}
