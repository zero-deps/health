package .stats

import scala.concurrent.duration.Duration
import .kvs._,handle._

package object handlers {
  import TreeStorage._
  val STATS_FID = "stats"

  trait DataHandler[T <: Data] {
    val FID: String

    def treeKey(data: T): TreeKey
    def socketMsg(data: T): String
    def serialize(data: T): String
    def deSerialize(str: String): T

    final lazy val handler = EnHandler.by[T, String](serialize _)(deSerialize _)

    final def saveToKvs(data: T)(kvs: Kvs): Either[Dbe, En[T]] = kvs.treeAdd[T](FID, treeKey(data), data)(handler)
    final def getFromKvs(treeKey: Option[TreeKey], count: Option[Int])(kvs: Kvs): List[Either[Dbe, Data]] = treeKey match {
      case Some(treeKey) =>
        kvs.treeEntries[T](FID, treeKey, None, count)(handler) map { _.right map { _.data } }
      case None =>
        kvs.entries[En[T]](FID, None, count)(handler) fold (
          error => List(Left(error)),
          entries => entries map { x => Right(x.data) })
    }
  }

  object metricHandler extends DataHandler[Metric] {
    val FID = s"$STATS_FID :: metric"

    def treeKey(metric: Metric) = metric.name ~ metric.node ~ metric.param
    def socketMsg(metric: Metric) = s"metric::${metric.name}::${metric.node}::${metric.param}::${metric.time.toMillis}::${metric.value}"
    def serialize(metric: Metric) = s"${metric.name}::${metric.node}::${metric.param}::${metric.time}::${metric.value}"
    def deSerialize(str: String) = str.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        Metric(name, node, param, Duration(time), value)
      case _ => throw new IllegalArgumentException(str)
    }
  }

  object historyHandler extends DataHandler[History] {
    val FID = s"$STATS_FID :: history"

    def treeKey(history: History) = history.casino ~ history.user
    def socketMsg(history: History) = s"history::${history.casino}::${history.user}::${history.time.toMillis}::${history.action}"
    def serialize(history: History) = s"${history.casino}::${history.user}::${history.time}::${history.action}"
    def deSerialize(str: String) = str.split("::").toList match {
      case casino :: user :: time :: action :: Nil =>
        History(casino, user, Duration(time), action)
      case other => throw new IllegalArgumentException(str)
    }
  }
}