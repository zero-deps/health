package .stats

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.io.{Tcp, IO}
import spray.can.Http

object HttpListener {
  def props(host: String, port: Int, stats: ActorRef): Props = Props(new HttpListener(host, port, stats))
}

class HttpListener(host: String, port: Int, stats: ActorRef) extends Actor with ActorLogging {
  import context.system
  IO(Http) ! Http.Bind(self, host, port)
  val router = context.actorOf(HttpRouter.props(stats))

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
