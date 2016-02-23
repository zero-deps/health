package .stats

import ftier.ws._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object StatsApp extends App {
  sys.props += (("java.library.path", sys.props("java.library.path")+":native"))

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = Ws(system)
  import ws.kvs

  system.actorOf(MetricsListener.props)
  Flows.saveDataFromUdp.run()
  
  val bf = ws.bindAndHandle
}
