package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress
import zd.proto.api.{decode}
import .stats.client._
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
      decode[ClientMsg](data.toArray) match {
        case MetricMsg(name, value, hostname1, ipaddr1) =>
          val hostname = hostname1.orElse(Option(remote.getAddress).map(_.getHostName)).getOrElse("N/A")
          val ipaddr = ipaddr1.orElse(Option(remote.getAddress).map(_.getHostAddress)).getOrElse("N/A")
          self ! StatMsg(stat=Metric(name, value), meta=StatMeta(now_ms(), hostname, ipaddr))
        case MeasureMsg(name, value, hostname1, ipaddr1) =>
          val hostname = hostname1.orElse(Option(remote.getAddress).map(_.getHostName)).getOrElse("N/A")
          val ipaddr = ipaddr1.orElse(Option(remote.getAddress).map(_.getHostAddress)).getOrElse("N/A")
          self ! StatMsg(stat=Measure(name, value), meta=StatMeta(now_ms(), hostname, ipaddr))
        case ErrorMsg(exception, stacktrace, toptrace, hostname1, ipaddr1) =>
          val hostname = hostname1.orElse(Option(remote.getAddress).map(_.getHostName)).getOrElse("N/A")
          val ipaddr = ipaddr1.orElse(Option(remote.getAddress).map(_.getHostAddress)).getOrElse("N/A")
          self ! StatMsg(stat=Error(exception, stacktrace, toptrace), meta=StatMeta(now_ms(), hostname, ipaddr))
        case ActionMsg(action, hostname1, ipaddr1) =>
          val hostname = hostname1.orElse(Option(remote.getAddress).map(_.getHostName)).getOrElse("N/A")
          val ipaddr = ipaddr1.orElse(Option(remote.getAddress).map(_.getHostAddress)).getOrElse("N/A")
          self ! StatMsg(stat=Action(action), meta=StatMeta(now_ms(), hostname, ipaddr))
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
