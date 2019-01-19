package .stats.client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString

object Client {
  def props(remote: (String, Int), local: (String, String)): Props = {
    val isa = { val (host, port) = remote; new InetSocketAddress(host, port) }
    Props(new Client(isa, local))
  }
}

class Client(remote: InetSocketAddress, local: (String,String)) extends Actor with ActorLogging {
  import context.system
  val (host, port) = local

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
        send(udp)(s"metric::${system.name}::${host}:${port}::${name}::${value}")
      
      case ErrorStat(className, message, stacktrace) =>
        send(udp)(s"error::${system.name}::${host}:${port}::${className}::${message}::${stacktrace}")
      
      case ActionStat(user, action) =>
        send(udp)(s"action::${system.name}::${host}:${port}::${user}::${action}")
    }
  }
}
