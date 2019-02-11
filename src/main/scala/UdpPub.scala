package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.{IO, Udp}
import java.net.InetSocketAddress
import scalaz.Scalaz._

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
      data.decodeString("UTF-8").split("::").toList match {
        case "metric" :: name :: value :: port :: Nil =>
          self ! Msg(MetricStat(name, value), StatMeta(now_ms(), addr=s"${host}:${port}"))
        case "error" :: exception :: stacktrace :: toptrace :: port :: Nil =>
          self ! Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(now_ms(), addr=s"${host}:${port}"))
        case "action" :: action :: port :: Nil =>
          self ! Msg(ActionStat(action), StatMeta(now_ms(), addr=s"${host}:${port}"))
        case unknown =>
          log.info(s"unknown message=${unknown}")
      }
    case Udp.Unbound =>
      context.stop(self)

    case a: ActorRef =>
      log.debug("got stage actor for udp")
      unstashAll()
      stageActor = a.some
    case msg: Msg =>
      stageActor match {
        case Some(a) => a ! msg
        case None => stash()
      }
  }

  override def postStop(): Unit = {
    socket map (_ ! Udp.Unbind)
  }
}
