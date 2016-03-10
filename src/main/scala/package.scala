package 

import scala.concurrent.duration.Duration

package object stats {
  sealed trait Data

  object Metric{
    val alias = "Metric"       
  }
  case class Metric(name: String, node: String, param: String, time: Duration, value: String) extends Data
  
  object History{
    val alias = "History"
  }
  case class History(casino: String, user: String, time: Duration, action: String) extends Data

  case class ErrorElement(className: String, method: String, fileName: String, lineNumber: Int)
  
  object Error{
    val alias = "Error"
  }
  case class Error(name: String, node: String, time: Duration, message: String, stackTraces: List[ErrorElement]) extends Data
}


