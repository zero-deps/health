package .stats
package handlers

import api._
import scala.util.Try
import .kvs.handle.`package`.En
import .kvs.Kvs
import scala.util.Failure

object handler extends UdpHandler with SocketHandler with KvsHandler {
  object handlers {
    val kvs = Seq[KvsHandler](historyHandler, metricHandler, errorHandler)
    val socket = Seq[SocketHandler](historyHandler, metricHandler, errorHandler)
    val udp = Seq[UdpHandler](historyHandler, metricHandler, errorHandler)
  }

  private type PF[P1, P2] = PartialFunction[P1, P2]

  private def failedPF[P1, P2] = {
    PartialFunction[P1, Try[P2]] {
      case other =>
        Failure(new Exception(s"No handler for $other"))
    }
  }

  private def orElse[P1, P2](functions: Seq[PF[P1, P2]]): PF[P1, P2] = functions.fold(PartialFunction.empty) {
    (func: PF[P1, P2], x: PF[P1, P2]) => x orElse func
  }
  private def orElse[H, P1, P2](handlers: Seq[H], func: (H) => PF[P1, P2]): PF[P1, P2] = orElse[P1, P2](handlers map func)

  override val udpMessage: PF[String, Try[Data]] = orElse[UdpHandler, String, Try[Data]](handlers.udp, _.udpMessage) orElse {
    case other =>
      Failure(new Exception(s"No handler for $other"))
  }

  override val socketMsg: PF[Data, Try[String]] = orElse[SocketHandler, Data, Try[String]](handlers.socket, _.socketMsg) orElse {
    case other =>
      Failure(new Exception(s"No handler for $other"))
  }

  override def saveToKvs(kvs: Kvs): PF[Data, Try[En[Data]]] = orElse[KvsHandler, Data, Try[En[Data]]](handlers.kvs, _.saveToKvs(kvs)) orElse {
    case other =>
      Failure(new Exception(s"No handler for $other"))
  }

  override def getFromKvs(kvs: Kvs): PF[(Option[Int], String), Try[List[Data]]] =
    orElse[KvsHandler, (Option[Int], String), Try[List[Data]]](handlers.kvs, _.getFromKvs(kvs)) orElse {
      case other =>
        Failure(new Exception(s"No handler for $other"))
    }
}