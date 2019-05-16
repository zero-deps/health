package .stats.client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import zd.proto.api.{encode}

object Client {
  def props(remote: (String, Int), localPort: String): Props = {
    val isa = { val (host, port) = remote; new InetSocketAddress(host, port) }
    Props(new Client(isa, localPort))
  }

  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class MeasureStat(name: String, value: Long) extends Stat
  final case class ErrorStat(exception: String, stacktrace: String, toptrace: String) extends Stat
  final case class ActionStat(action: String) extends Stat
}

class Client(remote: InetSocketAddress, localPort: String) extends Actor with ActorLogging {
  import context.system
  import .stats.client.Client._
  IO(Udp) ! Udp.SimpleSender

  def receive: Receive = {
    case Udp.SimpleSenderReady => context become ready(sender)
  }

  def send(udp: ActorRef)(msg: ClientMsg): Unit = {
    udp ! Udp.Send(ByteString(encode[ClientMsg](msg)), remote)
  }

  def ready(udp: ActorRef): Receive = {
    case m: Stat => m match {
      
      case MetricStat(name, value) =>
        send(udp)(MetricMsg(name, value, localPort))
      
      case MeasureStat(name, value) =>
        send(udp)(MeasureMsg(name, value.toString, localPort))
      
      case ErrorStat(exception, stacktrace, toptrace) =>
        send(udp)(ErrorMsg(exception, stacktrace, toptrace, localPort))
      
      case ActionStat(action) =>
        send(udp)(ActionMsg(action, localPort))
    }
  }
}
