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
      stats.error(
        exception = s"${message.toString}~${cause.getClass.getSimpleName}: ${cause.getMessage}",
        stacktrace = cause.getStackTrace.toList.map(e => s"${e.getClassName}.${e.getMethodName}~${e.getFileName}:${e.getLineNumber}").mkString("~~"),
      )
  }
}
