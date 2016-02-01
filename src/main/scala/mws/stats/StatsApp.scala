package .stats

import .kvs.Kvs
import ftier.ws._
import akka.actor.{ActorRef,ActorRefFactory,ActorSystem,Actor,Props}
import akka.routing.{ActorRefRoutee,RoundRobinRoutingLogic,Router,Routee,RemoveRoutee,AddRoutee}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{FlowShape,ClosedShape}
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.MediaTypes.`text/html`

object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  val ws = Ws(system)

  implicit val kvs:Kvs = ws.kvs

  def init()(implicit fa:ActorSystem,kvs:Kvs) = {
    fa.actorOf(LastMessage.props(kvs), name = "last-msg")
    fa.actorOf(MetricsListener.props)
  }

  def handleStats(req:HttpRequest)(implicit fa:ActorSystem,kvs:Kvs) = req.header[UpgradeToWebsocket] match {
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

  def index()(implicit fa:ActorSystem): HttpResponse = {
    val cfg = ConfigFactory.load
    val p = cfg.getInt("http.port")

    HttpResponse(entity=HttpEntity(akka.http.scaladsl.model.ContentTypes.`text/html(UTF-8)`,
      html.home(HomeContext(p, "/websocket", List.empty, List.empty)).toString)
    )
  }

  init()

  val udpListener:Option[ActorRef] = Some(system.actorOf(UdpListener.props, "udp-listener"))

  val bf = ws.bindAndHandle

  sys.addShutdownHook {
    udpListener foreach (_ ! "close")
    import system.dispatcher
    ws.unbind(bf).onComplete(_ => system.shutdown())
    system.awaitTermination()
    println("Bye!")
  }

  println("ENTER to stop...")
  scala.io.StdIn.readLine()
  sys.exit
}
