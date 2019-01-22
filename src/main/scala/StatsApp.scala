package .stats

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import .ftier._
import .kvs._
import scala.concurrent.duration._

object StatsApp extends App {
  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = WsExtension(system)
  val kvs = Kvs(system)
  // val stats = client.StatsExtenstion(system)

  import system.dispatcher
  val scheduler = system.scheduler
  // Uptime (seconds)
  scheduler.schedule(1 second, 30 seconds) {
    system.log.error(new Exception("exc"), s"error occured at ${now_ms()}")
  }

  Flows.udp(system, kvs).run()

  ws.bindAndHandle
}
