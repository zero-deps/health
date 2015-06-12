package .stats

import akka.actor.{ActorRef, ActorSystem, Props}
import .stats.Template.HomeContext
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.handlers._
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}

object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  val system = ActorSystem("Stats")
  val config = system.settings.config

  var udpListener: Option[ActorRef] = None
  var webServer: Option[WebServer] = None

  sys.addShutdownHook {
    webServer foreach (_.webSocketConnections.closeAll())
    webServer foreach (_.stop())
    udpListener foreach (_ ! "close")
    system.shutdown()
    system.awaitTermination()
    println("Bye!")
  }

  val hostname = config.getString("hostname")
  val udpPort = config.getInt("udp.port")
  val httpPort = config.getInt("http.port")

  udpListener = Some(system.actorOf(UdpListener.props(hostname, udpPort), "udp-listener"))

  system.actorOf(MetricsListener.props)

  {
    val config = WebServerConfig(hostname = hostname, port = httpPort)
    val wsUrl = "/webscoket"
    val staticHandler = system.actorOf(Props(new StaticContentHandler(StaticContentHandlerConfig())), "static-handler")
    val routes = Routes({
      case HttpRequest(request) => request match {
        case GET(Path("/")) =>
          val ctx = HomeContext(hostname, httpPort, wsUrl)
          request.response.write(html.home(ctx).toString, "text/html; charset=UTF-8")
        case Path("/favicon.ico") =>
          request.response.write(HttpResponseStatus.NOT_FOUND)
        case GET(Path("/bootstrap/bootstrap.min.css")) =>
          staticHandler ! new StaticResourceRequest(request, "public/bootstrap/bootstrap.min.css")
        case GET(Path("/home.css")) =>
          staticHandler ! new StaticResourceRequest(request, "public/home.css")
        case GET(Path("/react/react.min.js")) =>
          staticHandler ! new StaticResourceRequest(request, "public/react/react.min.js")
        case GET(Path("/react/JSXTransformer.js")) =>
          staticHandler ! new StaticResourceRequest(request, "public/react/JSXTransformer.js")
        case GET(Path("/util.js")) =>
          staticHandler ! new StaticResourceRequest(request, "public/util.js")
        case GET(Path("/home.js")) =>
          staticHandler ! new StaticResourceRequest(request, "public/home.js")
      }
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path(wsUrl) => wsHandshake.authorize()
      }
    })
    lazy val srv: WebServer = new WebServer(config, routes, system)
    webServer = Some(srv)
    srv.start()

    system.actorOf(WebSocketWriter.props(srv.webSocketConnections))
  }
}
