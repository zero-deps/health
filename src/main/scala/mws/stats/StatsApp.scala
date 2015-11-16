package .stats

import .kvs.{StKvs,LeveldbKvs}
import akka.actor.{ActorRef,ActorRefFactory,ActorSystem,Actor,Props}
import akka.routing.{ActorRefRoutee,RoundRobinRoutingLogic,Router,Routee,RemoveRoutee,AddRoutee}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source, FlowGraph}
import akka.stream.{FlowShape,ClosedShape}
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.MediaTypes.`text/html`

object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()
  val c = system.settings.config
  val hp = c.getInt("http.port")
  val ex = Rt(system)

  val routes = ex.route
  implicit val kvs:StKvs = ex.kvs

  println(s"routes collected $routes")

  def init()(implicit fa:ActorRefFactory,kvs:StKvs) = {
    fa.actorOf(LastMessage.props(kvs), name = "last-msg")
    fa.actorOf(MetricsListener.props)
  }

  def handleStats(req:HttpRequest)(implicit fa:ActorRefFactory,kvs:StKvs) = req.header[UpgradeToWebsocket] match {
    case Some(upg) => {
      val router: ActorRef = fa.actorOf(Props(new Actor{
        var routees = Set[Routee]()
        def receive = {
          case ar: AddRoutee => routees = routees + ar.routee
          case rr: RemoveRoutee => routees = routees - rr.routee
          case msg => routees.foreach(_.send(msg, sender)) }}))

      upg.handleMessages(Flows.stats(router,kvs))
    }
    case None => HttpResponse(BadRequest)
  }

  import .stats.Template._

  def index()(implicit fa:ActorRefFactory): HttpResponse = {
    val cfg = ConfigFactory.load
    val p = cfg.getInt("http.port")

    HttpResponse(entity=HttpEntity(`text/html`,
      html.home(HomeContext(p, "/websocket", List.empty, List.empty)).toString)
    )
  }

  init()

  val udpListener:Option[ActorRef] = Some(system.actorOf(UdpListener.props, "udp-listener"))

  val f = Flow.fromGraph(
    FlowGraph.create(){ implicit b =>
      import FlowGraph.Implicits._
      val cl = b.add(Flow[HttpRequest].collect[HttpResponse](routes))
      val log = b.add(Flow[HttpResponse].map[HttpResponse]{x=>println(x);x})
      cl ~> log
      FlowShape(cl.inlet,log.outlet)
  })

  val bf = Http().bindAndHandle(f, interface="0.0.0.0", port=hp)

  sys.addShutdownHook {
    kvs.close()
    udpListener foreach (_ ! "close")
    import system.dispatcher
    bf.flatMap(_.unbind()).onComplete(_ => system.shutdown())
    system.awaitTermination()
    println("Bye!")
  }
}
