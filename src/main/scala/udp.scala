package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress
import zd.proto.api.{decode}
import .stats.client._
import zero.ext._, option._

object UdpPub {
  def props: Props = Props(new UdpPub)
}

class UdpPub extends Actor with Stash with ActorLogging {
  import context.system

  val cfg = system.settings.config.getConfig("stats.server")
  val hostname = cfg.getString("host")
  val udpPort = cfg.getInt("port")

  log.info(s"Starting UDP listener on ${hostname}:${udpPort}...")

  IO(Udp) ! Udp.Bind(self, new InetSocketAddress(hostname, udpPort))
  var socket: Option[ActorRef] = None
  var stageActor: Option[ActorRef] = None

  def merge_host(x: Option[String], remote: InetSocketAddress): String = x.orElse(Option(remote.getAddress).map(_.getHostName)).getOrElse("N/A").split("-depl-").head
  def merge_ipaddr(x: Option[String], remote: InetSocketAddress): String = x.orElse(Option(remote.getAddress).map(_.getHostAddress)).getOrElse("N/A").split("-depl-").head

  def receive: Receive = {
    case _: Udp.Bound =>
      socket = sender().some

    case Udp.Received(data, remote) =>
      val _ = util.Try(decode[ClientMsg](data.toArray)).collect{
        case MetricMsg(name, value, hostname1, ipaddr1) =>
          val host = merge_host(hostname1, remote)
          val ipaddr = merge_ipaddr(hostname1, remote)
          val time = System.currentTimeMillis
          self ! HostMsg(host=host, ipaddr=ipaddr, time=time)
          self ! StatMsg(stat=Metric(name, value), time=time, host=host)
        case MeasureMsg(name, value, hostname1, ipaddr1) =>
          val host = merge_host(hostname1, remote)
          val ipaddr = merge_ipaddr(hostname1, remote)
          val time = System.currentTimeMillis
          self ! HostMsg(host=host, ipaddr=ipaddr, time=time)
          self ! StatMsg(stat=Measure(name, value), time=time, host=host)
        case ErrorMsg(exception, stacktrace, toptrace, hostname1, ipaddr1) =>
          val host = merge_host(hostname1, remote)
          val ipaddr = merge_ipaddr(hostname1, remote)
          val time = System.currentTimeMillis
          self ! HostMsg(host=host, ipaddr=ipaddr, time=time)
          self ! StatMsg(stat=Error(exception, stacktrace, toptrace), time=time, host=host)
        case ActionMsg(action, hostname1, ipaddr1) =>
          val host = merge_host(hostname1, remote)
          val ipaddr = merge_ipaddr(hostname1, remote)
          val time = System.currentTimeMillis
          self ! HostMsg(host=host, ipaddr=ipaddr, time=time)
          self ! StatMsg(stat=Action(action), time=time, host=host)
      }

    case Udp.Unbound =>
      context.stop(self)

    case a: ActorRef =>
      log.debug("got stage actor for udp")
      unstashAll()
      stageActor = a.some

    case msg: Push =>
      stageActor match {
        case Some(a) => a ! msg
        case None => stash()
      }
  }

  override def postStop(): Unit = {
    socket map_ (_ ! Udp.Unbind)
  }
}
