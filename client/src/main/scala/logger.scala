package .stats.client

import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}

class Logger extends Actor {
  lazy val stats = StatsExtenstion(context.system)

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender ! Logging.loggerInitialized()
    case event @ Error(cause: Throwable, logSource: String, logClass: Class[_], message: Any) =>
      val ys = {
        val xs = cause.getStackTrace.map(_.toString)
        val k = xs.indexWhere(x => x.contains("") || x.contains("wpl"))+1 + 5
        if (k >= xs.length) xs else xs.take(k) :+ "..."
      }
      stats.error(
        exception = s"${message.toString}~${cause.getClass.getSimpleName}: ${cause.getMessage}",
        stacktrace = ys.mkString("~"),
        toptrace = ys.headOption.getOrElse("")
      )
  }
}
