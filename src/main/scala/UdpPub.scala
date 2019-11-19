package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress
import zd.proto.api.{decode}
import .stats.client._
import scala.util.Try
import zd.gs.z._
import zd.kvs.Kvs

object UdpPub {
  def props(kvs: Kvs): Props = Props(new UdpPub(kvs))
}

class UdpPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  import context.system

  val cfg = system.settings.config.getConfig("stats.server")
  val hostname = cfg.getString("host")
  val udpPort = cfg.getInt("port")

  log.info(s"Starting UDP listener on ${hostname}:${udpPort}...")

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(hostname, udpPort))
  var socket: Option[ActorRef] = None
  var stageActor: Option[ActorRef] = None

  def receive: Receive = {
    case _: Udp.Bound =>
      socket = sender.just

    case Udp.Received(data, remote) =>
      for {
        ip <- Try(remote.getAddress.toString.split("/")(1))
        hostname = remote.getHostName
        host = (if (hostname == ip) kvs.el.get[String](s"hostname_${ip}").toOption.flatten.getOrElse(ip) else hostname).stripSuffix(".ee..corp").stripSuffix("..corp")
        msg <- Try(decode[ClientMsg](data.toArray))
      } msg match {
        case MetricMsg(name, value, port) =>
          self ! StatMsg(stat=Metric(name, value), meta=StatMeta(now_ms(), s"${host}:${port}", ip))
        case MeasureMsg(name, value, port) =>
          self ! StatMsg(stat=Measure(name, value), meta=StatMeta(now_ms(), s"${host}:${port}", ip))
        case ErrorMsg(exception, stacktrace, toptrace, port) =>
          self ! StatMsg(stat=Error(exception, stacktrace, toptrace), meta=StatMeta(now_ms(), s"${host}:${port}", ip))
        case ActionMsg(action, port) =>
          self ! StatMsg(stat=Action(action), meta=StatMeta(now_ms(), s"${host}:${port}", ip))
      }

    case Udp.Unbound =>
      context.stop(self)

    case a: ActorRef =>
      log.debug("got stage actor for udp")
      unstashAll()
      stageActor = a.just
    case msg: Push =>
      stageActor match {
        case Some(a) => a ! msg
        case None => stash()
      }
  }

  override def postStop(): Unit = {
    socket map (_ ! Udp.Unbind)
  }
}
