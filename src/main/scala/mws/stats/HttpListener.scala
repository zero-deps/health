package .stats

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.io.{Tcp, IO}
import spray.can.Http

object HttpListener {
  def props(port: Int): Props = Props(new HttpListener(port))
}

class HttpListener(port: Int) extends Actor with ActorLogging {
  import context.system
  IO(Http) ! Http.Bind(self, "localhost", port)
  val router = context.actorOf(HttpRouter.props())

  def receive: Receive = {
    case Tcp.Bound(_) =>
      val socket = sender
      context become (ready(socket))
  }

  def ready(socket: ActorRef): Receive = {
    case _: Tcp.Connected =>
      sender ! Http.Register(router)
    case "close" =>
      socket ! Tcp.Unbind
    case Tcp.Unbound =>
      context.stop(self)
  }
}
