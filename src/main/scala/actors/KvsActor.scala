package .stats
package actors

import akka.actor.{ Actor, ActorLogging, Props }
import .kvs.Kvs
import scala.util.{ Failure, Success, Try }

object KvsActor {
  object REQ {
    object GetData {
      def unapply(data: GetData): Option[Int] = Some(data.count)
    }

    sealed abstract class GetData(val TYPE_ALIAS: String) {
      val count: Int
    }

    case class GetHistory(count: Int) extends GetData(History.alias)
    case class GetMetrcis(count: Int) extends GetData(Metric.alias)

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
  import .kvs.handle.Handler._
  import handlers._

  def receive = {
    case req @ REQ.GetData(count) =>
      val dataList = handler.getFromKvs(kvs)(Some(count).filter(_ > 0), req.TYPE_ALIAS) match {
        case Failure(error) =>
          sender ! RES.Error(error.getMessage)
          None
        case Success(dataList) =>
          Some(dataList)
      }

      dataList map { list => sender ! RES.DataList(list) }
  }
}
