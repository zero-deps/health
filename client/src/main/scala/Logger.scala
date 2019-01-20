package .stats.client

import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}

class Logger extends Actor {
  lazy val stats = StatsExtenstion(context.system)

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender ! Logging.loggerInitialized()
    case event @ Error(cause, logSource, logClass, message) =>
      stats.error(
        className = cause.getClass.getName,
        message = cause.getMessage,
        stacktrace = cause.getStackTrace.toList.map(e => s"${e.getClassName}~${e.getMethodName}~${e.getFileName}~${e.getLineNumber}").mkString("~~"),
      )
  }
}
