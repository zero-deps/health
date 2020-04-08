package .stats.client

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import java.net.{InetSocketAddress, InetAddress}
import zd.gs.z._
import zd.proto.api.encode

object Client {
  def props(remoteHost: String, remotePort: Int): Props = {
    Props(new Client(new InetSocketAddress(remoteHost, remotePort)))
  }

  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class MeasureStat(name: String, value: Long) extends Stat
  final case class ErrorStat(exception: String, stacktrace: String, toptrace: String) extends Stat
  final case class ActionStat(action: String) extends Stat
}

class Client(remote: InetSocketAddress) extends Actor with ActorLogging {
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
    case MetricStat(name, value) =>
      val ia = InetAddress.getLocalHost
      send(udp)(MetricMsg(name, value, ia.getHostName.just, ia.getHostAddress.just))
    
    case MeasureStat(name, value) =>
      val ia = InetAddress.getLocalHost
      send(udp)(MeasureMsg(name, value.toString, ia.getHostName.just, ia.getHostAddress.just))
    
    case ErrorStat(exception, stacktrace, toptrace) =>
      val ia = InetAddress.getLocalHost
      send(udp)(ErrorMsg(exception, stacktrace, toptrace, ia.getHostName.just, ia.getHostAddress.just))
    
    case ActionStat(action) =>
      val ia = InetAddress.getLocalHost
      send(udp)(ActionMsg(action, ia.getHostName.just, ia.getHostAddress.just))
  }
}
