package .stats

import akka.actor.{ActorRef,ActorSystem,Actor,Props}
import akka.routing.{ActorRefRoutee,RoundRobinRoutingLogic,Router,Routee,RemoveRoutee,AddRoutee}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest,HttpResponse}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory


object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()
  val c = system.settings.config

  val kvs: Option[Kvs] = Some(LeveldbKvs(c.getConfig("leveldb")))
  val udpListener: Option[ActorRef] = Some(system.actorOf(UdpListener.props, "udp-listener"))
  val lastMsg = system.actorOf(LastMessage.props(kvs.get), name = "last-msg")

  val router: ActorRef = system.actorOf(Props(new Actor{
    var routees = Set[Routee]()
    def receive = {
      case ar: AddRoutee => routees = routees + ar.routee
      case rr: RemoveRoutee => routees = routees - rr.routee
      case msg => routees.foreach(_.send(msg, sender))
    }
  }))

  def handleStats(req:HttpRequest) = req.header[UpgradeToWebsocket] match {
    case Some(upg) => upg.handleMessages(Flows.stats(router, kvs.get))
    case None => HttpResponse(BadRequest)
  }

  system.actorOf(MetricsListener.props)

  val bf = Http().bindAndHandle(
    Flow[HttpRequest].collect(Rt(system).route),
    interface="0.0.0.0",
    port=c.getInt("http.port"))

  sys.addShutdownHook {
    kvs foreach (_.close())
    udpListener foreach (_ ! "close")
    import system.dispatcher
    bf.flatMap(_.unbind()).onComplete(_ => system.shutdown())
    system.awaitTermination()
    println("Bye!")
  }
}
