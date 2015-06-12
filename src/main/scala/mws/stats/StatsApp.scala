package .stats

import akka.actor.{ActorRef, ActorSystem}

object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  val system = ActorSystem("Stats")

  var udpListener: Option[ActorRef] = None
  var webServer: Option[ActorRef] = None

  sys.addShutdownHook {
    udpListener foreach (_ ! "close")
    webServer foreach (_ ! "stop")
    system.shutdown()
    system.awaitTermination()
    println("Bye!")
  }

  udpListener = Some(system.actorOf(UdpListener.props, "udp-listener"))
  system.actorOf(MetricsListener.props)
  webServer = Some(system.actorOf(SockoWebServer.props, "web-server"))
}
