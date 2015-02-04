package .stats

import akka.actor.{Props, Actor, ActorLogging}
import akka.io.Udp

object Handler {
  def props(): Props = Props(new Handler())
}

class Handler extends Actor with ActorLogging {
  def receive: Receive = {
    case Udp.Received(data, _) =>
      log.info(data.decodeString("UTF-8"))
  }
}
