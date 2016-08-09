package 

import scala.concurrent.duration.Duration

package object stats {
  sealed trait Data

  object Metric{
    val alias = "Metric"       
  }

  object History{
    val alias = "History"
  }

  object Error{
    val alias = "Error"
  }

  case class History(casino: String, user: String, time: Duration, action: String) extends Data
  case class Metric(name: String, node: String, param: String, time: String, value: String) extends Data
  case class ErrorElement(className: String, method: String, fileName: String, lineNumber: Int)
  case class Error(name: String, node: String, time: Duration, message: String, stackTraces: List[ErrorElement]) extends Data

}


