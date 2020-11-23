package metrics.client

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import java.net.{InetSocketAddress, InetAddress}
import zero.ext._, option._
import zd.proto.api.encode

object Client {
  def props(remoteHost: String, remotePort: Int): Props = {
    Props(new Client(new InetSocketAddress(remoteHost, remotePort)))
  }

  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class MeasureStat(name: String, value: Long) extends Stat
  final case class ErrorStat(msg: Option[String], cause: Option[String], st: Seq[String]) extends Stat
  final case class ActionStat(action: String) extends Stat
}

class Client(remote: InetSocketAddress) extends Actor with ActorLogging {
  import context.system
  import Client._
  IO(Udp) ! Udp.SimpleSender

  def receive: Receive = {
    case Udp.SimpleSenderReady => context become ready(sender())
  }

  def send(udp: ActorRef)(msg: ClientMsg): Unit = {
    val encoded = encode[ClientMsg](msg)
    val len = encoded.length
    if (len > conf.msgSize)
      println(s"message has length $len")
    else
      udp ! Udp.Send(ByteString(encoded), remote)
  }

  def ready(udp: ActorRef): Receive = {
    case MetricStat(name, value) =>
      val ia = InetAddress.getLocalHost
      send(udp)(MetricMsg(name, value, ia.getHostName.some, ia.getHostAddress.some))
    
    case MeasureStat(name, value) =>
      val ia = InetAddress.getLocalHost
      send(udp)(MeasureMsg(name, value.toString, ia.getHostName.some, ia.getHostAddress.some))
    
    case x: ErrorStat =>
      import x._
      val ia = InetAddress.getLocalHost
      send(udp)(ErrorMsg(msg=msg, cause=cause, st=st, host=ia.getHostName, ipaddr=ia.getHostAddress))
    
    case ActionStat(action) =>
      val ia = InetAddress.getLocalHost
      send(udp)(ActionMsg(action, ia.getHostName.some, ia.getHostAddress.some))
  }
}
