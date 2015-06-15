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

  var webServer: Option[WebServer] = None
  var staticHandler: Option[ActorRef] = None

  implicit val timeout: Timeout = 1 minute

  override def preStart: Unit = {
    val static = system.actorOf(Props(new StaticContentHandler(StaticContentHandlerConfig())), "static-handler")
    staticHandler = Some(static)
    val routes = Routes({
      case HttpRequest(request) => request match {
        case GET(Path("/")) =>
          val data = lastData ? LastData.Get onComplete {
            case util.Success(LastData.Values(it)) =>
              val ctx = HomeContext(hostname, httpPort, wsUrl, it.toList)
              request.response.write(html.home(ctx).toString, "text/html; charset=UTF-8")
            case util.Failure(e) =>
              log.error(e.getMessage, e)
              request.response.write("It doesn't work")
            case _ =>
          }
        case Path("/favicon.ico") =>
          request.response.write(HttpResponseStatus.NOT_FOUND)
        case GET(Path("/bootstrap/bootstrap.min.css")) =>
          static ! new StaticResourceRequest(request, "public/bootstrap/bootstrap.min.css")
        case GET(Path("/home.css")) =>
          static ! new StaticResourceRequest(request, "public/home.css")
        case GET(Path("/react/react.min.js")) =>
          static ! new StaticResourceRequest(request, "public/react/react.min.js")
        case GET(Path("/react/JSXTransformer.js")) =>
          static ! new StaticResourceRequest(request, "public/react/JSXTransformer.js")
        case GET(Path("/util.js")) =>
          static ! new StaticResourceRequest(request, "public/util.js")
        case GET(Path("/home.js")) =>
          static ! new StaticResourceRequest(request, "public/home.js")
      }
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path(wsUrl) => wsHandshake.authorize()
      }
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
    case Metric(data) =>
      webServer foreach (_.webSocketConnections.writeText(data))
    case "stop" =>
      webServer foreach (_.webSocketConnections.closeAll())
      webServer foreach (_.stop())
  }
}
