package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Udp

object Handler {
  def props(kvs: ActorRef): Props = Props(new Handler(kvs))
}

class Handler(kvs: ActorRef) extends Actor with ActorLogging {
  def receive: Receive = {
    case Udp.Received(data, _) =>
      data.decodeString("UTF-8").split('#') match {
        case Array(node, stats, _*) =>
          kvs ! StatsKvsService.Put(node, stats)
        case _ =>
      }
  }
}
