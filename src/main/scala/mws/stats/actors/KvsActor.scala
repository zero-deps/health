package .stats
package actors

import akka.actor.{ Actor, ActorLogging, Props }
import .kvs.Kvs
import TreeStorage.TreeKey
import scala.util.{ Failure, Success, Try }

object KvsActor {
  object REQ {
    object GetData {
      def unapply(data: GetData): Option[(Int, Option[TreeKey])] = Some((data.count, data.key))
    }

    sealed abstract class GetData(val TYPE_ALIAS: String) {
      val count: Int
      val key: Option[TreeKey]
    }

    case class GetHistory(count: Int, key: Option[TreeKey] = None) extends GetData(History.alias)
    case class GetMetrcis(count: Int, key: Option[TreeKey] = None) extends GetData(Metric.alias)

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
  import handlers._

  def receive = {
    case req @ REQ.GetData(count, treeKey) =>
      val dataList = handler.getFromKvs(kvs)(treeKey, Some(count).filter(_ > 0), req.TYPE_ALIAS) map {
        case Failure(error) =>
          error.printStackTrace()
          sender ! RES.Error(error.getMessage)
          None
        case Success(data) =>
          Some(data)
      }

      sender ! RES.DataList(dataList.flatten)
  }
}
