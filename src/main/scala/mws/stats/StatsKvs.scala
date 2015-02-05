package .stats

import akka.actor.Props
import kvs.LeveldbKvs
import .stats.StatsKvs._
import org.iq80.leveldb.DB

object StatsKvs {
  case class Put(key: String, value: String)
  case object PutAck
  case object All
  type Time = String
  type ParamValue = String
  type TimedParamValues = Map[Time, ParamValue]
  type ParamKey = String
  type Params = Map[ParamKey, TimedParamValues]
  type Node = String
  type Nodes = Map[Node, Params]
  case class AllAck(nodes: Nodes)

  def props(db: DB, dbConfig: String): Props = Props(new StatsKvs(db, dbConfig))
}

class StatsKvs(val leveldb: DB, val leveldbConfigPath: String)
  extends LeveldbKvs { kvs: LeveldbKvs =>

  def receive: Receive = {
    case Put(key, value) =>
      kvs.put(key, value)
      sender ! PutAck
    case All =>
      val nodes = kvs.get()
      val newNodes = nodes.foldLeft(Map.empty[Node, Params]) {
        case (nodes: Nodes, (k, value)) =>
          val Array(node, param, time) = k.split('#')
          nodes.get(node) match {
            case Some(params: Params) =>
              val timedParamValues = params.getOrElse(param, Map.empty)
              val newTimedParamValues = timedParamValues + (time -> value)
              val newParams = params.filterNot(_._1 == param) + (param -> newTimedParamValues)
              val newNodes = nodes.filterNot(_._1 == node) + (node -> newParams)
              newNodes
            case None =>
              val newTimedParamValues = Map(time -> value)
              val newParams = Map(param -> newTimedParamValues)
              val newNodes = nodes + (node -> newParams)
              newNodes
          }
      }
      sender ! AllAck(newNodes)
  }
}
