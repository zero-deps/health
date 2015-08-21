package .stats

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import concurrent.Future
import .stats.Template.HomeContext
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers._
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import util.Success

object SockoWebServer {
  def props(lastData: ActorRef, lastMsg: ActorRef): Props =
    Props(new SockoWebServer(lastData, lastMsg))
}

class SockoWebServer(lastMetric: ActorRef, lastMsg: ActorRef) extends Actor with ActorLogging {
  import context.system

  val config = system.settings.config
  val hostname = config.getString("hostname")
  val httpPort = config.getInt("http.port")
  val serverConfig = WebServerConfig(hostname = hostname, port = httpPort)
  val wsUrl = "/webscoket"
  val exts = List("js", "css", "ico", "eot", "svg", "ttf", "woff", "woff2") map ('.' + _)

  var webServer: Option[WebServer] = None
  var staticHandler: Option[ActorRef] = None

  implicit val timeout: Timeout = 1 minute

  override def preStart: Unit = {
    val static = system.actorOf(Props(new StaticContentHandler(StaticContentHandlerConfig())), "static-handler")
    staticHandler = Some(static)
    val routes = Routes({
      case HttpRequest(request) => request match {
        case GET(Path("/")) =>
          val f1 = lastMetric ? LastMetric.Get
          val f2 = lastMsg ? LastMessage.Get
          Future.sequence(f1 :: f2 :: Nil) onComplete {
            case Success(List(LastMetric.Values(it1), LastMessage.Values(it2))) =>
              val ctx = HomeContext(hostname, httpPort, wsUrl, it1.toList, it2.toList)
              request.response.write(html.home(ctx).toString, "text/html; charset=UTF-8")
            case x =>
              request.response.write(s"Unexpected response: $x")
          }
        case GET(Path("/favicon.ico")) =>
          request.response.write(HttpResponseStatus.NOT_FOUND)
        case GET(Path(path)) if exts.find(path.endsWith).isDefined =>
          static ! new StaticResourceRequest(request, s"public$path")
      }
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path(wsUrl) => wsHandshake.authorize()
      }
      case WebSocketFrame(frame) =>
        val key = frame.readText
        lastMetric ! LastMetric.Delete(key);
    })
    val server = new WebServer(serverConfig, routes, system)
    webServer = Some(server)
    server.start()

    system.eventStream.subscribe(self, classOf[Metric])
    system.eventStream.subscribe(self, classOf[Message])
  }

  override def postStop(): Unit = {
    staticHandler foreach (_ ! PoisonPill)
    system.eventStream.unsubscribe(self)
  }

  def receive: Receive = {
    case m: Metric =>
      webServer foreach (_.webSocketConnections.writeText("metric::" + m.serialize))
    case m: Message =>
      webServer foreach (_.webSocketConnections.writeText("msg::" + m.serialize))
    case "stop" =>
      webServer foreach (_.webSocketConnections.closeAll())
      webServer foreach (_.stop())
  }
}
