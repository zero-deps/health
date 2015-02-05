package .stats

import akka.actor.Props
import kvs.LeveldbKvs
import .stats.StatsKvs._
import org.iq80.leveldb.DB

object StatsKvs {
  case class Put(key: String, value: String)
  case object PutAck
  case object Get
  type ParamKey = String
  type ParamValue = Map[String, String]
  type Params = Map[ParamKey, ParamValue]
  type Node = String
  type Nodes = Map[Node, Params]
  case class GetAck(nodes: Nodes)

  def props(db: DB, dbConfig: String): Props = Props(new StatsKvs(db, dbConfig))
}

class StatsKvs(val leveldb: DB, val leveldbConfigPath: String)
  extends LeveldbKvs { kvs: LeveldbKvs =>

  def receive: Receive = {
    case Put(key, value) =>
      kvs.put(key, value)
      sender ! PutAck
    case Get =>
      val nodes = kvs.get()
      val newNodes = nodes.foldLeft(Map.empty[Node, Params]) {
        case (nodes: Nodes, (k, value)) =>
          val Array(node, param, time) = k.split('#')
          val params = nodes.getOrElse(node, Map.empty)
          if (time > params.getOrElse(param, Map.empty).getOrElse("time", "0")) {
            val newParamValue = Map("time" -> time, "value" -> value)
            val newParams = params.filterNot(_._1 == param) + (param -> newParamValue)
            nodes.filterNot(_._1 == node) + (node -> newParams)
          } else nodes
      }
      sender ! GetAck(newNodes)
  }
}
