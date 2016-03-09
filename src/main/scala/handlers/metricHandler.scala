package .stats
package handlers

import scala.concurrent.duration.Duration
import api._
import .stats.{ Metric, Data }
import scala.util.Try
import .kvs.Kvs
import scala.util.Failure
import .kvs.handle.`package`.En
import scala.util.Success

private[this] object metricHandler extends UdpHandler with SocketHandler with ByEnHandler[Metric] with KvsHandlerTyped[Metric] {
  object UdpMessage {
    def unapply(str: String): Option[Metric] =
      str.split("::").toList match {
        case "metric" :: name :: node :: param :: value :: Nil => Some(Metric(name, node, param, Duration(s"${System.currentTimeMillis} ms"), value))
        case other => None
      }
  }

  lazy val TYPE_ALIAS = Metric.alias

  protected def kvsFilter(data: Data) = Some(data) filter { _.isInstanceOf[Metric] } map { _.asInstanceOf[Metric] }

  override val socketMsg: PartialFunction[Data, Try[String]] = {
    case Metric(name, node, param, time, value) => Try { s"metric::${name}::${node}::${param}::${time.toMillis}::${value}" }
  }

  override val udpMessage: PartialFunction[String, Try[Data]] = {
    case UdpMessage(metric) => Try { metric }
  }

  def serialize(metric: Metric) = s"${metric.name}::${metric.node}::${metric.param}::${metric.time}::${metric.value}"
  def deSerialize(str: String) = str.split("::").toList match {
    case name :: node :: param :: time :: value :: Nil =>
      Metric(name, node, param, Duration(time), value)
    case _ => throw new IllegalArgumentException(str)
  }

  protected override def entry(m:Metric):En[Metric] = En[Metric](fid=FID,id=s"${m.name}::${m.node}::${m.param}",data=m)
}
