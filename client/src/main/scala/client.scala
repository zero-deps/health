package .stats.client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString

object Client {
  def props(remote: (String, Int), localPort: String): Props = {
    val isa = { val (host, port) = remote; new InetSocketAddress(host, port) }
    Props(new Client(isa, localPort))
  }
}

class Client(remote: InetSocketAddress, localPort: String) extends Actor with ActorLogging {
  import context.system
  IO(Udp) ! Udp.SimpleSender

  def receive: Receive = {
    case Udp.SimpleSenderReady => context become ready(sender)
  }

  def send(udp: ActorRef)(data: String): Unit = {
    udp ! Udp.Send(ByteString(data), remote)
  }

  def ready(udp: ActorRef): Receive = {
    case m: Stat => m match {
      
      case MetricStat(name, value) =>
        send(udp)(s"metric::${name}::${value}::${localPort}")
      
      case ErrorStat(exception, stacktrace, toptrace) =>
        send(udp)(s"error::${exception}::${stacktrace}::${toptrace}::${localPort}")
      
      case ActionStat(action) =>
        send(udp)(s"action::${action}::${localPort}")
    }
  }
}
