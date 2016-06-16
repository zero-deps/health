package .stats

import akka.actor._
import akka.io.{IO,Udp}
import akka.util.ByteString
import java.net.InetSocketAddress

object StatsClient {
  case class Metric(address:Address,param:String,value:String)
  case class History(casino:String,user:String,message:String)

  def init(implicit system:ActorSystem):Unit = {
    val cfg = system.settings.config.getConfig("stats")
    if (cfg.getBoolean("enabled")) {
      val c = cfg.getConfig("server")
      val (host,port) = (c.getString("host"),c.getInt("port"))
      system.actorOf(Props(new StatsClient(new InetSocketAddress(host,port))))
    }
  }

}

class StatsClient(remote:InetSocketAddress) extends Actor with ActorLogging {
  import StatsClient._
  import context.system
  val eventStream = system.eventStream

  IO(Udp) ! Udp.SimpleSender

  override def preStart():Unit = {
    eventStream.subscribe(self,classOf[Metric])
    eventStream.subscribe(self,classOf[History])
  }
  override def postStop():Unit = eventStream.unsubscribe(self)

  def receive:Receive = {
    case Udp.SimpleSenderReady => context become ready(sender)
  }

  def ready(send:ActorRef):Receive = {
    case Metric(address,param,value) =>
      val name = system.name
      val node = s"${address.host.getOrElse("")}:${address.port.getOrElse("")}"
      send ! prepare("metric",name,node,param,value)

    case History(casino,user,message) =>
      send ! prepare("history",casino,user,message)
  }

  def prepare(data:Any*):Udp.Send = Udp.Send(ByteString(data mkString "::"),remote)
}
