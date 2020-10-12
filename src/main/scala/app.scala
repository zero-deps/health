package .stats

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import .ftier._
import kvs._

object StatsApp extends App {
  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = WsExtension(system)
  val kvs = Kvs.rng(system)

  /*
  val stats = client.StatsExtension(system)
  import scala.concurrent.duration._
  import system.dispatcher
  val scheduler = system.scheduler
  scheduler.schedule(1 second, 13 seconds) {
    system.log.error(new Exception("exc"), s"error occured at ${now_ms()}")
  }
  scheduler.schedule(1 second, 7 seconds) {
    stats.action(s"event ${now_ms()}")
  }
  */

  //todo: mark old data to deletion (e.g. common errors)
  //todo: cleanup feeds
  // import keys._
  // LazyList(
  //   `cpu_mem.live`, `cpu.hour`,
  //   `search.ts.latest`, `search.wc.latest`, `search.fs.latest`,
  //   `static.gen.latest`,
  //   `reindex.all.latest`,
  //   `static.gen.year`,
  //   `kvs.size.year`, `action.live`, `metrics`, `errors`,
  //   `feature`
  // ).foreach{ fid =>
  //   kvs.all(fid).map_(_.collect{ case Right(a) => a.id -> extract(a) }.filter{ case (_, data) =>
  //     val old = data.time.toLong.toLocalDataTime().isBefore(year_ago())
  //     val blocklist = List("gitlab-ci-runner")
  //     blocklist.exists(data.host.contains) || old
  //   }.foreach{ case (id, _) => kvs.remove(fid, id) })
  // }

  Flows.udp(system, kvs).run()

  ws.bindAndHandle
}
