package .stats

import akka.actor.Props
import kvs.LeveldbKvs
import .stats.StatsKvs._
import org.iq80.leveldb.DB
import scala.collection.immutable

object StatsKvs {
  case class Put(node: String, param: String, time: String, value: String)
  case object PutAck
  case object Get
  case class NodeInfo(node: String, param: String, value: String, time: String)
  case class GetAck(nodesInfo: immutable.Seq[NodeInfo])

  def props(db: DB, dbConfig: String): Props = Props(new StatsKvs(db, dbConfig))
}

class StatsKvs(val leveldb: DB, val leveldbConfigPath: String)
  extends LeveldbKvs { kvs: LeveldbKvs =>

  def receive: Receive = {
    case Put(node, param, time, value) =>
      kvs.put(s"$node#$param#$time", value)
      kvs.index(s"$node#$param", s"$node#$param#$time")
      sender ! PutAck
    case Get =>
      val nodesInfo = kvs.indexes().foldLeft(immutable.Seq.empty[NodeInfo]) {
        case (nodesInfo, (_, index)) =>
          kvs.get(index) match {
            case Some(value) =>
              val Array(node, param, time) = index.split('#')
              val nodeInfo = NodeInfo(node, param, value, time)
              nodeInfo +: nodesInfo
            case None => nodesInfo
          }
      }
      sender ! GetAck(nodesInfo)
  }
}
