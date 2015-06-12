package .stats

import akka.actor.{Props, Actor, ActorLogging}
import org.mashupbots.socko.webserver.WebSocketConnections

object WebSocketWriter {
  def props(conns: WebSocketConnections): Props = Props(new WebSocketWriter(conns))
}

class WebSocketWriter(conns: WebSocketConnections) extends Actor with ActorLogging {
  import context.system

  override def preStart(): Unit =
    system.eventStream.subscribe(self, classOf[Metric])

  override def postStop(): Unit =
    system.eventStream.unsubscribe(self, classOf[Metric])

  def receive: Receive = {
    case Metric(data) =>
      conns.writeText(data)
  }
}
