package .stats

import akka.actor.{ActorRef, ActorSystem}
import kvs.LeveldbKvs
import org.iq80.leveldb.DB
import util.Try

object Boot extends App {
  val system = ActorSystem("stats")
  val config = system.settings.config

  var statsLeveldb: Option[DB] = None

  sys.addShutdownHook {
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

  system.actorOf(Listener.props(port = 12345, statsKvs), "listener")
}
