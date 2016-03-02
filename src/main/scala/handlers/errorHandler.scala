package .stats
package handlers

import scala.concurrent.duration.Duration
import api._
import scala.xml._
import scala.xml.XML
import scala.util.{ Try, Success, Failure }
import TreeStorage._
import .stats.{ ErrorElement, Data, Error }

private[this] object errorHandler extends UdpHandler with SocketHandler with ByEnHandler[Error] with KvsHandlerTyped[Error] {
  private def elementToString(element: ErrorElement): String =
    s"${element.className}::${element.method}::${element.fileName}::${element.lineNumber}"

  private def stringToElement(str: String): ErrorElement = str.split("::").toList match {
    case className :: method :: fileName :: lineNumber :: Nil =>
      ErrorElement(className, method, fileName, lineNumber.toInt)
    case other => throw new IllegalArgumentException(str)
  }

  private def stackTracesToXML(traces: List[ErrorElement]): Node = <stackTraces>{
    traces map { element: ErrorElement =>
      <stackTrace>{ elementToString(element) }</stackTrace>
    }
  }</stackTraces>

  private def xmlToStackTraces(xml: Node): List[ErrorElement] =
    (xml \ "stackTrace") map { x => stringToElement(x.text) } toList

  private def errorToString(error: Error): String =
    s"${error.name}::${error.node}::${error.time}::${stackTracesToXML(error.stackTraces).toString}"

  private def strToError(str: String): Error = {
    str.split("::").toList match {
      case name :: node :: time :: stackTracesXML =>
        val xmlString = stackTracesXML.mkString("::")
        Error(name, node, Duration(time), if (xmlString == "") List.empty else xmlToStackTraces(XML.loadString(xmlString)))
      case other => throw new IllegalArgumentException(str)
    }
  }

  object UdpMessage {
    def unapply(str: String): Option[Error] =
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

  val TYPE_ALIAS = Error.alias

  def treeKey(err: Error) = err.name ~ err.node

  protected def kvsFilter(data: Data) = Some(data) filter { _.isInstanceOf[Error] } map { _.asInstanceOf[Error] }

  val socketMsg: PartialFunction[Data, Try[String]] = {
    case err: Error => Success(s"error::${errorToString(err)}")
  }

  val udpMessage: PartialFunction[String, Try[Data]] = {
    case UdpMessage(error) =>
      Success(error)
  }

  def serialize(err: Error) = errorToString(err).toString
  def deSerialize(str: String) = strToError(str)
}