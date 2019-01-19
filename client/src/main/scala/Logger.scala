package .stats.client

import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.{Error, InitializeLogger}
import argonaut._
import argonaut.Argonaut._

class Logger extends Actor {
  import Logger.StackTraceEncodeJson

  lazy val stats = StatsExtenstion(context.system)

  def receive: Receive = {
    case msg: InitializeLogger =>
      sender ! Logging.loggerInitialized()
    case event @ Error(cause, logSource, logClass, message) =>
      stats.error(
        className = cause.getClass.getName,
        message = cause.getMessage,
        stacktrace = cause.getStackTrace.toList.asJson.nospaces,
      )
  }
}

object Logger {
  implicit def StackTraceEncodeJson: EncodeJson[StackTraceElement] =
  EncodeJson{ (element: StackTraceElement) =>
    ("className" := element.getClassName) ->:
    ("method" := element.getMethodName) ->:
    ("fileName" := element.getFileName) ->:
    ("lineNumber" := element.getLineNumber) ->:
    jEmptyObject
  }
}
