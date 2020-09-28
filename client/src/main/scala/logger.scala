package .stats.client

import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}

class Logger extends Actor {
  lazy val stats = StatsExtension(context.system)

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender() ! Logging.loggerInitialized()
    case event @ Error(cause: Throwable, logSource: String, logClass: Class[_], message: Any) =>
      val stacktrace = Some{
        val xs = cause.getStackTrace.map(_.toString)
        val k = xs.indexWhere(x => x.contains("") || x.contains("wpl"))+1 + 5
        if (k >= xs.length) xs else xs.take(k) :+ "..."
      }.filter(_.nonEmpty).getOrElse(Array("--"))
      stats.error(
        exception = s"${message.toString}~${cause.toString}",
        stacktrace = stacktrace.mkString("~"),
      )
  }
}
