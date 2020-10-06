package zero.ws

sealed trait Err

case class ParseErr(o: String) extends Err
case class TcpErr(e: Throwable) extends RuntimeException(e) with Err
case class ThrowableErr(e: Throwable) extends Err

sealed trait HttpErr extends Err
object HttpErr {
    case object NotValidFirstLine extends HttpErr
    case object WrongContentLength extends HttpErr
    case object NotHandled extends HttpErr
    case class WrongUri(e: Throwable) extends RuntimeException(e) with HttpErr
}
sealed trait WsErr extends Err
object WsErr {
    case class WriteMessageErr(e: Throwable) extends RuntimeException(e) with HttpErr
    case class CloseErr(e: Throwable) extends RuntimeException(e) with HttpErr
}

case class ForeignErr[A](e: A) extends Err

case class BindFailed(addr: zio.nio.core.SocketAddress, e: Throwable) extends Err