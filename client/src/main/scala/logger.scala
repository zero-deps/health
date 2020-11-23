package metrics.client

import zero.ext._, option._
import akka.actor._
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}

class Logger extends Actor {
  private val remote: ActorRef = {
    val cfg = context.system.settings.config
    val remoteHost = cfg.getString("stats.client.remote.host")
    val remotePort = cfg.getInt   ("stats.client.remote.port")
    context.actorOf(Client.props(remoteHost, remotePort))
  }

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender() ! Logging.loggerInitialized()
    case event @ Error(cause: Throwable, logSource: String, logClass: Class[_], message: Any) =>
      remote ! toErrorStat(msg=fromNullable(message).map(_.toString).filter(_.nonEmpty), cause)
  }
}
