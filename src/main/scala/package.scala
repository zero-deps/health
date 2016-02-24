package 

import scala.concurrent.duration.Duration

package object stats {
  sealed trait Data

  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data
  case class History(casino: String, user: String, time: Duration, action: String, cid: String) extends Data
}
