package .stats.client

import akka.actor._
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}

class Logger extends Actor {
  private val remote: ActorRef = {
    val cfg = context.system.settings.config
    val remoteHost = cfg.getString("stats.client.remote.host")
    val remotePort = cfg.getInt("stats.client.remote.port")
    context.actorOf(Client.props(remoteHost, remotePort))
  }

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender() ! Logging.loggerInitialized()
    case event @ Error(cause: Throwable, logSource: String, logClass: Class[_], message: Any) =>
      val stacktrace = Some{
        val xs = cause.getStackTrace.map(_.toString)
        val k = xs.indexWhere(x => x.contains("") || x.contains("wpl"))+1 + 5
        if (k >= xs.length) xs else xs.take(k) :+ "..."
      }.filter(_.nonEmpty).getOrElse(Array("--"))
      remote ! Client.ErrorStat(
        exception = s"${message.toString}~${cause.toString}",
        stacktrace = stacktrace.mkString("~"),
      )
  }
}
