package .stats

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import .ftier._
import .kvs._

object StatsApp extends App {
  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = WsExtension(system)
  val kvs = Kvs(system)

  Flows.udp(system, kvs).run()

  ws.bindAndHandle
}
