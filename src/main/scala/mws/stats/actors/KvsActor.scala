package .stats
package actors

import akka.actor.{ Actor, ActorLogging }
import akka.actor.Props
import .kvs.Kvs
import .kvs.handle.`package`.En

object KvsActor {
  object REQ {
    sealed trait GetData{
    	val count: Int
    	val fid: String
    }
    
    case class SaveData(data: Data)
    case class GetMessages(count: Int) extends GetData{val fid = Message.FID}
    case class GetMetrcis(count: Int) extends GetData{val fid = Metric.FID}
    
  }
  object RES {
    case class DataSaved(data: Data)
    case class DataList(datas: List[Data])
    case class Error(msg: String)
  }

  def props(kvs: Kvs) = Props(classOf[KvsActor], kvs)
}

class KvsActor(kvs: Kvs) extends Actor with ActorLogging {
  import KvsActor._

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
           val datas: List[Data] = req match{
             case _: REQ.GetMessages => entries map {x => Message(x)}
             case _: REQ.GetMetrcis => entries map {x => Metric(x)}
           }
          sender ! RES.DataList(datas)
        case Left(error) =>
          sender ! RES.Error(error.msg)
      }
  }
}
