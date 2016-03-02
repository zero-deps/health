package .stats
package handlers

import scala.util.{ Failure, Success, Try }
import .kvs.handle.`package`.En
import .kvs.Kvs
import TreeStorage._
import .kvs.handle.EnHandler

object api {
  val STATS_FID = "stats"

  trait UdpHandler {
    val udpMessage: PartialFunction[String, Try[Data]]
  }

  trait SocketHandler {
    val socketMsg: PartialFunction[Data, Try[String]]
  }

  trait KvsHandler {
    def saveToKvs(kvs: Kvs): PartialFunction[Data, Try[En[Data]]]
    def getFromKvs(kvs: Kvs): PartialFunction[(Option[TreeKey], Option[Int], String), List[Try[Data]]]
  }

  trait ByEnHandler[T <: Data] {
    def serialize(data: T): String
    def deSerialize(str: String): T

    lazy val handler: EnHandler[T] = EnHandler.by[T, String](serialize _)(deSerialize _)
  }

  trait KvsHandlerTyped[T <: Data] extends KvsHandler {
    val TYPE_ALIAS: String
    final lazy val FID = s"$STATS_FID :: $TYPE_ALIAS"

    val handler: EnHandler[T]

    case class PF(kvs: Kvs) extends PartialFunction[Data, Try[En[Data]]] {
      def isDefinedAt(data: Data): Boolean = kvsFilter(data).isDefined
      def apply(data: Data): Try[En[Data]] = {
        val value = kvsFilter(data).get
        val res = kvs.treeAdd[T](FID, treeKey(value), value)(handler)
        res fold (
          l => Failure(new Exception(l.msg)),
          r => Success(En[Data](r.fid, r.id, r.prev, r.next, r.data)))
      }
    }

    protected def kvsFilter(data: Data): Option[T]

    def treeKey(data: T): TreeKey

    override def saveToKvs(kvs: Kvs) = new PartialFunction[Data, Try[En[Data]]] {
      def isDefinedAt(data: Data): Boolean = kvsFilter(data).isDefined
      def apply(data: Data): Try[En[Data]] = {
        val value = kvsFilter(data).get
        val res = kvs.treeAdd[T](FID, treeKey(value), value)(handler)
        res fold (
          l => Failure(new Exception(l.msg)),
          r => Success(En[Data](r.fid, r.id, r.prev, r.next, r.data)))
      }
    }

    override def getFromKvs(kvs: Kvs) = {
      case (Some(treeKey), count, TYPE_ALIAS) =>
        kvs.treeEntries[T](FID, treeKey, None, count)(handler) map {
          _ fold (
            error => Failure(new Exception(error.msg)),
            entry => Success(entry.data))
        }
      case (None, count, TYPE_ALIAS) =>
        kvs.entries[En[T]](FID, None, count)(handler) fold (
          error => List(Failure(new Exception(error.msg))),
          entries => entries map { x => Success(x.data) })
    }
  }
}