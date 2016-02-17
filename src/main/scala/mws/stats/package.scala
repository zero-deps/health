package 

import scala.concurrent.duration.Duration
import .kvs.handle.`package`.En
import scala.language.implicitConversions
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import .kvs.handle.EnHandler

package object stats {
  sealed trait Data

  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data
  case class History(casino: String, user: String, time: Duration, action: String) extends Data
}
