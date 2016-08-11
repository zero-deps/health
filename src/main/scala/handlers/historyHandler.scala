package .stats
package handlers

import scala.concurrent.duration.Duration
import api._
import scala.util.Try

private[this] object historyHandler extends UdpHandler with SocketHandler with ByEnHandler[History] with KvsHandlerTyped[History] {
  object UdpMessage {
    def unapply(str: String): Option[History] =
      str.split("::").toList match {
        case "history" :: casino :: user :: action :: Nil =>
          Some(History(casino, user, Duration(s"${System.currentTimeMillis.toString} ms"), action))
        case other => None
      }
  }

  lazy val TYPE_ALIAS = History.alias
    
  protected def kvsFilter(data: Data) = Some(data) filter { _.isInstanceOf[History] } map { _.asInstanceOf[History] }

  override val socketMsg: PartialFunction[Data, Try[String]] = {
    case History(casino, user, time, action) => Try { s"history::${casino}::${user}::${time.toMillis}::${action}" }
  }

  override val udpMessage: PartialFunction[String, Try[Data]] = {
    case UdpMessage(history) => Try { history }
  }

  def serialize(history: History) = s"${history.casino}::${history.user}::${history.time}::${history.action}"
  def deSerialize(str: String) = str.split("::").toList match {
    case casino :: user :: time :: action :: Nil =>
      History(casino, user, Duration(time), action)
    case other => throw new IllegalArgumentException(str)
  }
}