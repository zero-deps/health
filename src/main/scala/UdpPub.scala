package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress
import scalaz.Scalaz._
import zd.proto.api.{decode}
import .stats.client._
import scala.util.Try

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

  def receive: Receive = {
    case _: Udp.Bound =>
      socket = sender.some
    case Udp.Received(data, remote) =>
      val host = remote.getHostName.stripSuffix(".ee..corp").stripSuffix("..corp")
      Try(decode[ClientMsg](data.toArray)).foreach(
        _ match {
          case MetricMsg(name, value, port) => 
            self ! MetricStat(name, value, now_ms(), addr=s"${host}:${port}")
          case MeasureMsg(name, value, port) => 
            self ! MeasureStat(name, value, now_ms(), addr=s"${host}:${port}")
          case ErrorMsg(exception, stacktrace, toptrace, port) =>
            self ! ErrorStat(exception, stacktrace, toptrace, now_ms(), addr=s"${host}:${port}")
          case ActionMsg(action, port) =>
            self ! ActionStat(action, now_ms(), addr=s"${host}:${port}")
        }
      )
    case Udp.Unbound =>
      context.stop(self)

    case a: ActorRef =>
      log.debug("got stage actor for udp")
      unstashAll()
      stageActor = a.some
    case msg: StatMsg =>
      stageActor match {
        case Some(a) => a ! msg
        case None => stash()
      }
  }

  override def postStop(): Unit = {
    socket map (_ ! Udp.Unbind)
  }
}
