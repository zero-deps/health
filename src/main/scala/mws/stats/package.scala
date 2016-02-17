package 

import scala.concurrent.duration.Duration
import .kvs.handle.`package`.En
import scala.language.implicitConversions
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import .kvs.handle.EnHandler

package object stats {
  import TreeStorage._
  val STATS_FID = "stats"

  sealed trait Data

  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data
  case class History(casino: String, user: String, time: Duration, action: String) extends Data

  object Metric {
    val FID = s"$STATS_FID :: metric"
  }

  object History {
    val FID = s"$STATS_FID :: history"
  }

  val getTreeKeyAndFid: PartialFunction[Data, (String, TreeKey)] = {
    case Metric(name, node, param, time, value) => (Metric.FID, name ~ node ~ param)
    case History(casino, user, time, action) => (History.FID, casino ~ user)
  }

  private def metricToString(metric: Metric): String = s"${metric.name}::${metric.node}::${metric.param}::${metric.time}::${metric.value}"
  
  private def historyToString(history: History): String = s"${history.casino}::${history.user}::${history.time}::${history.action}"

  private def stringToMetric(str: String): Metric =
    str.split("::").toList match {
      case name :: node :: param :: time :: value :: Nil =>
        Metric(name, node, param, Duration(time), value)
    }

  private def stringToHistory(str: String): History =
    str.split("::").toList match {
      case casino :: user :: time :: action :: Nil =>
        History(casino, user, Duration(time), action)
      case other => throw new IllegalArgumentException(str)
    }

  implicit val metricHandler = EnHandler.by[Metric, String](metricToString _)(stringToMetric _)
  implicit val historyHandler = EnHandler.by[History, String](historyToString _)(stringToHistory _)
}
