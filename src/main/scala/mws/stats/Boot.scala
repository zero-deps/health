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
    listener foreach (_ ! Listener.Disconnect)
    statsLeveldb foreach (db => Try(db.close()))
    system.shutdown()
    println("Bye!")
  }

  statsLeveldb = Some(LeveldbKvs.open(config.getConfig("stats-kvs")))
  val statsKvs = system.actorOf(StatsLeveldbKvs.props(statsLeveldb.get, "stats-kvs"), "stats-kvs")
  val handler = system.actorOf(Handler.props(statsKvs), "handler")
  listener = Some(system.actorOf(Listener.props(port = 12345, handler), "listener"))

  listener foreach (_ ! Listener.Connect)
}
