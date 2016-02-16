package 

import scala.concurrent.duration.Duration
import .kvs.handle.`package`.En
import scala.language.implicitConversions
import scala.util.Try
import scala.util.Success
import scala.util.Failure

package object stats {
  import stats.TreeStorage._
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

  implicit def dataToTreeEntry(data: Data): TreeEn[String] = {
    val (fid, treeKey, serialized) = data match {
      case History(casino, user, time, msg) =>
        (History.FID, casino ~ user, s"$casino::$user::${time}::$msg")
      case Metric(name, node, param, time, value) =>
        (Metric.FID, name ~ node ~ param, s"$name::$node::$param::${time}::$value")
    }
    TreeEn[String](fid, treeKey, java.util.UUID.randomUUID.toString, data = serialized)
  }
}
