package .stats
package actors

import akka.actor.{ Actor, ActorLogging, Props }
import .kvs.Kvs
import TreeStorage.TreeKey

object KvsActor {
  object REQ {
    object GetData {
      def unapply(data: GetData): Option[(Int, Option[TreeKey])] = Some((data.count, data.key))
    }

    sealed trait GetData {
      val count: Int
      val key: Option[TreeKey]
    }
    
    case class GetHistory(count: Int, key: Option[TreeKey] = None) extends GetData
    case class GetMetrcis(count: Int, key: Option[TreeKey] = None) extends GetData

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
      val dataHandler = req match{
        case _: REQ.GetHistory => historyHandler
        case _: REQ.GetMetrcis => metricHandler
      }
      
      val dataList =  dataHandler.getFromKvs(treeKey, Some(count).filter(_ > 0))(kvs) map {
        case Left(error) => 
          sender ! RES.Error(error.name)
          None
        case Right(data) => Some(data)
      }

      sender ! RES.DataList(dataList.flatten)
  }
}
