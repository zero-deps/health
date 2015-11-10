package .stats

import akka.actor.{ActorRef,ActorRefFactory,ActorSystem,Actor,Props}
import akka.routing.{ActorRefRoutee,RoundRobinRoutingLogic,Router,Routee,RemoveRoutee,AddRoutee}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.MediaTypes.`text/html`


object StatsApp extends App {
  println("START STATS")
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()
  val c = system.settings.config

  val kvs: Kvs = LeveldbKvs(c.getConfig("leveldb"))
  val udpListener: Option[ActorRef] = Some(system.actorOf(UdpListener.props, "udp-listener"))
  val lastMsg = system.actorOf(LastMessage.props(kvs.get), name = "last-msg")


  def handleStats(req:HttpRequest)(implicit fa:ActorRefFactory) = req.header[UpgradeToWebsocket] match {
    case Some(upg) => {
      println(s"upgrade ws with $kvs")
      val router: ActorRef = fa.actorOf(Props(new Actor{
        var routees = Set[Routee]()
        def receive = {
          case ar: AddRoutee => routees = routees + ar.routee
          case rr: RemoveRoutee => routees = routees - rr.routee
          case msg => routees.foreach(_.send(msg, sender)) }}))

      upg.handleMessages(Flows.stats(router, kvs.get))
    }
    case None => HttpResponse(BadRequest)
  }

  import .stats.Template._

  def index()(implicit fa:ActorRefFactory):HttpResponse = {
    val cfg = ConfigFactory.load
    val p = cfg.getInt("http.port")
    println(s"actor factory $fa")

    HttpResponse(entity=HttpEntity(`text/html`,
      html.home(HomeContext(p, "/websocket", List.empty, List.empty)).toString)
    )
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
