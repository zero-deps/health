package .stats

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import .stats.Template.HomeContext
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers._
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object SockoWebServer {
  def props(lastData: ActorRef): Props = Props(new SockoWebServer(lastData))
}

class SockoWebServer(lastData: ActorRef) extends Actor with ActorLogging {
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
          val data = lastData ? LastMetric.Get onComplete {
            case util.Success(LastMetric.Values(it)) =>
              val ctx = HomeContext(hostname, httpPort, wsUrl, it.toList)
              request.response.write(html.home(ctx).toString, "text/html; charset=UTF-8")
            case util.Failure(e) =>
              log.error(e.getMessage, e)
              request.response.write("It doesn't work")
            case _ =>
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
        lastData ! LastMetric.Delete(key);
    })
    val server = new WebServer(serverConfig, routes, system)
    webServer = Some(server)
    server.start()

    system.eventStream.subscribe(self, classOf[Metric])
  }

  override def postStop(): Unit = {
    staticHandler foreach (_ ! PoisonPill)
    system.eventStream.unsubscribe(self, classOf[Metric])
  }

  def receive: Receive = {
    case m: Metric =>
      webServer foreach (_.webSocketConnections.writeText(m.serialize))
    case "stop" =>
      webServer foreach (_.webSocketConnections.closeAll())
      webServer foreach (_.stop())
  }
}
