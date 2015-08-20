package .stats

import akka.actor.{ActorRef, ActorSystem}

object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  val config = system.settings.config

  var kvs: Option[Kvs] = None
  var udpListener: Option[ActorRef] = None
  var webServer: Option[ActorRef] = None

  sys.addShutdownHook {
    udpListener foreach (_ ! "close")
    webServer foreach (_ ! "stop")
    system.shutdown()
    system.awaitTermination()
    kvs foreach (_.close())
    println("Bye!")
  }

  kvs = Some(LeveldbKvs(config.getConfig("leveldb")))
  val lastData = system.actorOf(LastMetric.props(kvs.get), name = "last-metric")

  webServer = Some(system.actorOf(SockoWebServer.props(lastData), "web-server"))
  udpListener = Some(system.actorOf(UdpListener.props, "udp-listener"))

  system.actorOf(MetricsListener.props)
}
