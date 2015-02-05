package .stats

import akka.actor.{ActorRef, ActorSystem}
import kvs.LeveldbKvs
import org.iq80.leveldb.DB
import util.Try

object Boot extends App {
  val system = ActorSystem("stats")
  val config = system.settings.config

  var udpListener: Option[ActorRef] = None
  var httpListener: Option[ActorRef] = None
  var statsLeveldb: Option[DB] = None

  sys.addShutdownHook {
    udpListener foreach (_ ! "close")
    httpListener foreach (_ ! "close")
    statsLeveldb foreach (db => Try(db.close()))
    system.shutdown()
    println("Bye!")
  }

  val statsKvs: ActorRef = {
    val configPath = "stats-kvs"
    val leveldb = LeveldbKvs.open(config.getConfig(configPath))
    statsLeveldb = Some(leveldb)
    system.actorOf(StatsKvs.props(leveldb, configPath), "stats-kvs")
  }

  udpListener = Some(system.actorOf(UdpListener.props(port = 50123, statsKvs), "udp-listener"))
  httpListener = Some(system.actorOf(HttpListener.props(port = 9010), "http-listener"))
}
