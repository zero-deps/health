package .stats
package actors

import akka.actor.{ Actor, ActorLogging }
import akka.actor.Props
import .kvs.Kvs
import .kvs.handle.`package`.En
import scala.util.{ Success, Failure, Try }
import scala.concurrent.duration.Duration
import TreeStorage._

object KvsActor {
  object REQ {
    object GetData {
      def unapply(data: GetData): Option[(Int, String, Option[TreeKey])] = Some((data.count, data.fid, data.key))
    }

    sealed trait GetData {
      val count: Int
      val fid: String
      val key: Option[TreeKey]
    }

    case class SaveData(data: Data)
    case class GetMessages(count: Int, key: Option[TreeKey] = None) extends GetData { val fid = History.FID }
    case class GetMetrcis(count: Int, key: Option[TreeKey] = None) extends GetData { val fid = Metric.FID }

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
  import TreeStorage._
  import .kvs.handle.Handler._

  def receive = {
    case REQ.SaveData(data: Data) =>
      kvs.treeAdd[String](data) match {
        case Right(en) => sender ! RES.DataSaved(data)
        case Left(error) => sender ! RES.Error(error.msg)
      }
    case req @ REQ.GetData(count, fid, treeKey) =>
      ////////////////////////////
      def _entryToData(entry: En[String]): Option[Data] = {
        (req match {
          case _: REQ.GetMessages => entryToHistory(entry)
          case _: REQ.GetMetrcis => entryToMetric(entry)
        }) match {
          case Success(data) => Some(data)
          case Failure(ex) =>
            log.warning(s"~~~~~~~~~~~${ex.getMessage}")
            println(kvs.remove(entry))
            None
        }
      }
      ///////////////////////////
      val dataList: List[Option[Data]] = treeKey match {
        case Some(treeKey) =>
          kvs.treeEntries[String](fid, treeKey, None, Some(count).filter(_ > 0)) map {
            case Right(entry) => _entryToData(entry)
            case Left(error) =>
              sender ! RES.Error(error.msg)
              None
          }

        case None =>
          kvs.entries[En[String]](fid, None, Some(count).filter(_ > 0)) match {
            case Left(error) =>
              sender ! RES.Error(error.msg)
              List.empty
            case Right(entries) =>
              entries map _entryToData
          }
      }
      sender ! RES.DataList(dataList.flatten)
  }

  def entryToMetric(entry: En[String]): Try[Metric] = Try {
    entry.data.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        Metric(name, node, param, Duration(time), value)
      case other => throw new IllegalArgumentException(entry.toString)
    }
  }

  def entryToHistory(entry: En[String]): Try[History] = Try {
    entry.data.split("::").toList match {
      case casino :: user :: time :: action :: Nil =>
        History(casino, user, Duration(time), action)
      case other => throw new IllegalArgumentException(entry.toString)
    }
  }

  def entryToData(fid_entry: (String, En[String])): Try[Data] = {
    val (fid, entry) = fid_entry
    fid match {
      case Metric.FID => entryToMetric(entry)
      case History.FID => entryToHistory(entry)
      case other => Failure(new IllegalArgumentException(fid_entry.toString))
    }
  }
}
