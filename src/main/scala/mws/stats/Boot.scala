package .stats

import akka.actor.{ActorRef, ActorSystem}
import kvs.LeveldbKvs
import org.iq80.leveldb.DB
import util.Try

object Boot extends App {
  val system = ActorSystem("stats")
  val config = system.settings.config

  var listener: Option[ActorRef] = None
  var statsLeveldb: Option[DB] = None

  sys.addShutdownHook {
    listener foreach (_ ! "close")
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

  listener = Some(system.actorOf(Listener.props(port = 50123, statsKvs), "listener"))
}
