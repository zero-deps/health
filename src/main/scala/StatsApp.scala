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
  val stats = client.StatsExtension(system)

  // import scala.concurrent.duration._
  // import system.dispatcher
  // val scheduler = system.scheduler
  // scheduler.schedule(1 second, 13 seconds) {
  //   system.log.error(new Exception("exc"), s"error occured at ${now_ms()}")
  // }
  // scheduler.schedule(1 second, 7 seconds) {
  //   stats.action(s"event ${now_ms()}")
  // }

  Flows.udp(system, kvs).run()

  ws.bindAndHandle
}
