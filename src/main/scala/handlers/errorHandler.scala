package .stats
package handlers

import scala.concurrent.duration.Duration
import api._
import scala.util.{ Try, Success, Failure }
import argonaut._, Argonaut._

private[this] object errorHandler extends UdpHandler with SocketHandler with ByEnHandler[Error] with KvsHandlerTyped[Error] {
  implicit def ErrorElementJson: CodecJson[ErrorElement] =
    casecodec4(ErrorElement.apply, ErrorElement.unapply)("className", "method", "fileName", "lineNumber")

  private def elementToString(element: ErrorElement): String =
    s"${element.className}::${element.method}::${element.fileName}::${element.lineNumber}"

  private def stringToElement(str: String): ErrorElement = str.split("::").toList match {
    case className :: method :: fileName :: lineNumber :: Nil =>
      ErrorElement(className, method, fileName, lineNumber.toInt)
    case other => throw new IllegalArgumentException(str)
  }

  private def stackTracesToJson(traces: List[ErrorElement]): Option[String] = Some(traces) filter (!_.isEmpty) map { _.asJson.nospaces  }

  private def jsonToStackTraces(json: String): List[ErrorElement] = json.decodeOption[List[ErrorElement]].getOrElse(Nil)

  private def errorToString(error: Error): String =
    s"${error.name}::${error.node}::${error.time.toMillis}::${error.message}" + { stackTracesToJson(error.stackTraces) map { _.toString } getOrElse "" }

  private def strToError(str: String): Error = {
    str.split("::").toList match {
      case name :: node :: time :: message :: stackTracesXML =>
        val jsonString = stackTracesXML.mkString("::")
        Error(name, node, if (time.trim.contains(" ")) Duration(time) else Duration(time + " ms"), message, if (jsonString eq "") List.empty else jsonToStackTraces(jsonString))
      case other => throw new IllegalArgumentException(str)
    }
  }

  object UdpMessage {
    def unapply(str: String): Option[Error] = {
      str.split("::").toList match {
        case msgType :: name :: node :: tail if msgType == "error" =>
          val error = (name :: node :: s"${System.currentTimeMillis} ms" :: tail).mkString("::")
          Try { strToError(error) } match {
            case Success(error) => Some(error)
            case Failure(err) => None
          }
        case other => None
      }
    }
  }

  lazy val TYPE_ALIAS = Error.alias

  protected def kvsFilter(data: Data) = Some(data) filter { _.isInstanceOf[Error] } map { _.asInstanceOf[Error] }

  val socketMsg: PartialFunction[Data, Try[String]] = {
    case err: Error => 
      println(errorToString(err))
      Try{s"error::${errorToString(err)}"}
  }

  val udpMessage: PartialFunction[String, Try[Data]] = {
    case UdpMessage(error) =>
      Success(error)
  }

  def serialize(err: Error) = errorToString(err).toString
  def deSerialize(str: String) = strToError(str)
}