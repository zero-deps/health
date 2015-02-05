package .stats

import akka.actor.Props
import collection.immutable
import kvs.LeveldbKvs
import .stats.StatsKvs._
import org.iq80.leveldb.DB

object StatsKvs {
  case class Put(key: String, value: String)
  case object PutAck
  case class Get(key: String)
  case class Value(value: Option[String])
  case object All
  case class Values(values: immutable.Seq[(String, String)])

  def props(db: DB, dbConfig: String): Props = Props(new StatsKvs(db, dbConfig))
}

class StatsKvs(val leveldb: DB, val leveldbConfigPath: String)
  extends LeveldbKvs { kvs: LeveldbKvs =>

  def receive: Receive = {
    case Put(key, value) =>
      kvs.put(key, value)
      sender ! PutAck
    case Get(key) =>
      val value = kvs.get(key)
      sender ! Value(value)
    case All =>
      val values = kvs.get()
      sender ! Values(values)
  }
}
