package stats
package client

import akka.event.Logging.Error
import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.InitializeLogger

class Logger extends Actor {
  def receive = {
    case msg: InitializeLogger =>
      sender ! Logging.loggerInitialized()
    case event @ Error(cause, logSource, logClass, message) =>
      Stats(context.system).send(cause)
  }
}