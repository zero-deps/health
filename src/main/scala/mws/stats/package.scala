package 

import scala.concurrent.duration.Duration
import .kvs.handle.`package`.En
import scala.language.implicitConversions

package object stats {
  val STATS_FID = "stats"
  
  sealed trait Data {
    val serialize: String
    val fid: String
  }

  implicit def dataToEntry(data: Data) = En[String](data.fid, java.util.UUID.randomUUID.toString, data = data.serialize)

  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data {
    lazy val serialize = s"$name::$node::$param::${time}::$value"
    lazy val fid = Metric.FID
  }

  case class Message(casino: String, user: String, time: Duration, msg: String) extends Data {
    lazy val fid = Message.FID
    lazy val serialize = s"$casino::$user::${time}::$msg"
  }

  object Metric {
    val FID = s"$STATS_FID :: metric"
     
    def apply(metric: String): Metric = metric.split("::").toList match {
      case _ :: name :: node :: param :: time :: value :: Nil =>
        new Metric(name, node, param, Duration(time), value)
      case name :: node :: param :: time :: value :: Nil =>
        new Metric(name, node, param, Duration(time), value)
      case _ => throw new IllegalArgumentException(metric)
    }

    def apply(entity: En[String]): Metric = apply(entity.data)
  }

  object Message {
     val FID = s"$STATS_FID :: msg"
     
    def apply(msg: String): Message = msg.split("::").toList match {
      case _ :: casino :: user :: msg :: time :: Nil =>
        new Message(casino, user, Duration(time), msg)
      case casino :: user :: msg :: time :: Nil =>
        new Message(casino, user, Duration(time), msg)
      case _ => throw new IllegalArgumentException(msg)
    }

    def apply(msg: En[String]): Message = apply(msg.data)

  }

}
