package .stats

import akka.actor.ActorSystem

object Boot extends App {
  val system = ActorSystem("stats")

  sys.addShutdownHook {
    listener ! Listener.Disconnect
    system.shutdown()
    println("Bye!")
  }

  lazy val handler = system.actorOf(Handler.props(), "handler")
  lazy val listener = system.actorOf(Listener.props(port = 12345, handler), "listener")

  listener ! Listener.Connect
}
