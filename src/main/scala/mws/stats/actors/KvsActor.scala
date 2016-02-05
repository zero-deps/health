package .stats
package actors

import akka.actor.{ Actor, ActorLogging }
import akka.actor.Props
import .kvs.Kvs
import .kvs.handle.`package`.En
import scala.util.{ Success, Failure, Try }
import scala.concurrent.duration.Duration

object KvsActor {
  object REQ {
    sealed trait GetData {
      val count: Int
      val fid: String
    }

    case class SaveData(data: Data)
    case class GetMessages(count: Int) extends GetData { val fid = Message.FID }
    case class GetMetrcis(count: Int) extends GetData { val fid = Metric.FID }

  }
  object RES {
    case class DataSaved(data: Data)
    case class DataList(datas: Seq[Data])
    case class Error(msg: String)
  }

  def props(kvs: Kvs) = Props(classOf[KvsActor], kvs)
}

class KvsActor(kvs: Kvs) extends Actor with ActorLogging {
  import KvsActor._
  import .stats._

  def receive = {
    case REQ.SaveData(data: Data) =>
      kvs.add[En[String]](data) match {
        case Right(en) =>
          println(s"added $en")
          sender ! RES.DataSaved(data)
        case Left(error) =>
          sender ! RES.Error(error.msg)
      }
    case req: REQ.GetData =>
      println(s"Getting fid ${req.fid}...")
      val entries = kvs.entries[En[String]](req.fid, None, Some(req.count).filter(_ > 0))
      println(s"$entries!!!!")
      entries match {
        case Right(entries) =>
          val datas = req match {
            case _: REQ.GetMessages => entries map entryToMessage
            case _: REQ.GetMetrcis => entries map entryToMetric
          }

          sender ! RES.DataList((datas collect {
            case Success(data) => Some(data)
            case Failure(err) => log.error(err, err.getMessage); None
          }).flatten)
        case Left(error) =>
          sender ! RES.Error(error.msg)
      }
  }

  def entryToMetric(entry: En[String]): Try[Metric] = Try {
    entry.data.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        Metric(name, node, param, Duration(time), value)
      case other => throw new IllegalArgumentException(entry.toString)
    }
  }

  def entryToMessage(entry: En[String]): Try[Message] = Try {
    entry.data.split("::").toList match {
      case casino :: user :: msg :: time :: Nil =>
        Message(casino, user, Duration(time), msg)
      case other => throw new IllegalArgumentException(entry.toString)
    }
  }

  def entryToData(fid_entry: (String, En[String])): Try[Data] = {
    val (fid, entry) = fid_entry
    fid match {
      case Metric.FID => entryToMetric(entry)
      case Message.FID => entryToMessage(entry)
      case other => Failure(new IllegalArgumentException(fid))
    }
  }
}
