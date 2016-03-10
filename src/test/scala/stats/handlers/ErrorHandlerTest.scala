package .stats
package handlers

import org.scalatest.FreeSpecLike
import org.scalatest.Matchers
import scala.concurrent.duration.Duration
import collection.JavaConversions._
import scala.util.Success

class ErrorHandlerTest extends FreeSpecLike with Matchers {
  "errorHandler should " - {
    "serialize and deSerialize (error = deSerialize(serialize(error)))" in {
      val nullException = new NullPointerException
      val exception = new Exception(nullException)
      val stackTraces = exception.getStackTrace map { element => ErrorElement(element.getClassName, element.getMethodName, element.getFileName, element.getLineNumber) }
      val error = Error("TEST", "local", Duration("1456406636779 milliseconds"), "Test Exception", stackTraces.toList)
      val strError = errorHandler.serialize(error)

      println(strError)
      
      error shouldBe errorHandler.deSerialize(strError)
    }

    "hanlde messages without stack trace by UDP" in {
      val udpMessage = "error::TEST::local::Test Exception"
      val strError = errorHandler.udpMessage(udpMessage)
      strError match {
        case Success(Error("TEST", "local", _, "Test Exception", list)) =>
          list shouldBe List.empty
        case other => fail(s"$strError is incorrect message!")
      }
    }
  }
}