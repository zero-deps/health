package .stats

import akka.actor._

object StatsClient {
  case class Data(address: Address, param: String, value: String)

  def props(): Props = Props(new StatsClient())
}

class StatsClient() extends Actor with ActorLogging {
  import context.system

  def receive: Receive = {
    case StatsClient.Data(address, param, value) =>
      val name = system.name
      val node = s"${address.host.getOrElse("")}:${address.port.getOrElse("")}"
      val time = System.currentTimeMillis()
      val data = s"$name#$node#$param#$time#$value"
      StatsApp.webServer foreach (_.webSocketConnections.writeText(data))
  }
}
