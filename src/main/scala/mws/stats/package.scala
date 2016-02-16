package 

import scala.concurrent.duration.Duration
import .kvs.handle.`package`.En
import scala.language.implicitConversions
import scala.util.Try
import scala.util.Success
import scala.util.Failure

package object stats {
  val STATS_FID = "stats"

  sealed trait Data

  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data

  case class Message(casino: String, user: String, time: Duration, msg: String) extends Data

  object Metric {
    val FID = s"$STATS_FID :: metric"
  }

  object Message {
    val FID = s"$STATS_FID :: msg"
  }
  
  implicit def dataToEntry(data: Data) = {
    val (fid, serialized) = data match {
      case Message(casino, user, time, msg) =>
        (Message.FID, s"$casino::$user::${time}::$msg")
      case Metric(name, node, param, time, value) =>
        (Metric.FID, s"$name::$node::$param::${time}::$value")
    }
    En[String](fid, java.util.UUID.randomUUID.toString, data = serialized)
  }

}
