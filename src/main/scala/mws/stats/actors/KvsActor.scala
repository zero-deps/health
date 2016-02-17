package .stats
package actors

import akka.actor.{ Actor, ActorLogging }
import akka.actor.Props
import .kvs.Kvs
import .kvs.handle.`package`.En
import scala.util.{ Success, Failure, Try }
import scala.concurrent.duration.Duration
import TreeStorage._
import .kvs.`package`.Dbe

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
      val (fid, treeKey) = getTreeKeyAndFid(data)

      (data match {
        case data: Metric => kvs.treeAdd[Metric](fid, treeKey, data)
        case data: History => kvs.treeAdd[History](fid, treeKey, data)
      }) match {
        case Right(en) => sender ! RES.DataSaved(data)
        case Left(error) => sender ! RES.Error(error.msg)
      }
    case req @ REQ.GetData(count, fid, treeKey) =>
      val dataList = treeKey map { getEntries(fid, _, Some(count).filter(_ > 0)) } getOrElse getEntries(fid, Some(count).filter(_ > 0)) map {
        case Left(error) =>
          sender ! RES.Error(error.msg)
          None
        case Right(data) => Some(data)
      }

      sender ! RES.DataList(dataList.flatten)
  }

  def getEntries(fid: String, treeKey: TreeKey, count: Option[Int]): List[Either[Dbe, Data]] = {
    (fid match {
      case History.FID => kvs.treeEntries[History](fid, treeKey, None, count)
      case Metric.FID => kvs.treeEntries[Metric](fid, treeKey, None, count)
    }) map { _.right map { _.data } }
  }

  def getEntries(fid: String, count: Option[Int]): List[Either[Dbe, Data]] = {
    (fid match {
      case History.FID => kvs.entries[En[History]](fid, None, count)
      case Metric.FID => kvs.entries[En[Metric]](fid, None, count)
    }) fold (
      error => List(Left(error)),
      entries => entries map { x => Right(x.data) })
  }
}
