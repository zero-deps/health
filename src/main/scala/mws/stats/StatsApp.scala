package .stats

import akka.actor.{ActorRef, ActorSystem}
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}

object StatsApp extends App {
  val system = ActorSystem("stats")
  val config = system.settings.config

  var udpListener: Option[ActorRef] = None
  var webServer: Option[WebServer] = None

  sys.addShutdownHook {
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

  {
    val config = WebServerConfig(hostname = hostname, port = httpPort)
    val routes = Routes({
      case HttpRequest(httpRequest) => httpRequest match {
        case GET(Path("/")) =>
          httpRequest.response.write(html.home().toString, "text/html; charset=UTF-8")
        case Path("/favicon.ico") =>
          httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
      case WebSocketHandshake(wsHandshake) => wsHandshake match {
        case Path("/websocket") => wsHandshake.authorize()
      }
    })
    lazy val srv: WebServer = new WebServer(config, routes, system)
    webServer = Some(srv)
    srv.start()
  }
}
