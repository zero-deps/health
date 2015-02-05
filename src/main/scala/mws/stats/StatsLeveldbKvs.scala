package .stats

import akka.actor.Props
import kvs.LeveldbKvs
import org.iq80.leveldb.DB

object StatsLeveldbKvs {
  def props(db: DB, dbConfig: String): Props = Props(new StatsLeveldbKvs(db, dbConfig))
}

class StatsLeveldbKvs(val leveldb: DB, val leveldbConfigPath: String)
  extends StatsKvsService
  with LeveldbKvs
