package .stats

import akka.actor.{ActorLogging, Actor}
import kvs.Kvs
import .stats.StatsKvsService._
import scala.collection.immutable

object StatsKvsService {
  case class Put(key: String, value: String)
  case object PutAck
  case class Get(key: String)
  case class Value(value: Option[String])
  case object All
  case class Values(values: immutable.Seq[(String, String)])
}

class StatsKvsService extends Actor with ActorLogging { kvs: Kvs =>
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
