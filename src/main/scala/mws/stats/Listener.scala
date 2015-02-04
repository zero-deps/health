package .stats

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import java.net.InetSocketAddress
import akka.io.IO
import akka.io.Udp

object Listener {
  case object Connect
  case object Disconnect

  def props(port: Int, handler: ActorRef): Props = Props(new Listener(port, handler))
}

class Listener(port: Int, handler: ActorRef) extends Actor with ActorLogging {
  implicit val system = context.system

  def receive: Receive = {
    case Listener.Connect =>
      IO(Udp) ! Udp.Bind(handler, new InetSocketAddress(port))

    case Udp.Bound(_) =>
      val worker = sender
      context.become {
        case Listener.Disconnect =>
          worker ! Udp.Unbind
          context.become(receive)
      }
  }
}
