package .stats

import akka.actor.{ActorRef, ActorSystem}
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}

object Boot extends App {
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
    val ws = new WebServer(config, Http.routes, system)
    webServer = Some(ws)
    ws.start()
  }
}
