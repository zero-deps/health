package .stats

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import .ftier._
import zd.kvs._
import zero.ext._, either._

object StatsApp extends App {
  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = WsExtension(system)
  val kvs = Kvs(system)

  // val stats = client.StatsExtension(system)
  // import scala.concurrent.duration._
  // import system.dispatcher
  // val scheduler = system.scheduler
  // scheduler.schedule(1 second, 13 seconds) {
  //   system.log.error(new Exception("exc"), s"error occured at ${now_ms()}")
  // }
  // scheduler.schedule(1 second, 7 seconds) {
  //   stats.action(s"event ${now_ms()}")
  // }

  import keys._
  LazyList(
    `cpu_mem.live`, `cpu.hour`,
    `search.ts.latest`, `search.wc.latest`, `search.fs.latest`,
    `static.create.latest`, `static.gen.latest`,
    `reindex.ts.latest`, `reindex.wc.latest`, `reindex.files.latest`, `reindex.all.latest`,
    `static.create.year`, `static.gen.year`,
    `kvs.size.year`, `action.live`, `metrics`, `errors`,
    `feature`
  ).foreach(kvs.all[StatEn](_).map_(_.collect{ case Right(a) => a }.filter{ a =>
    val old = a.time.toLong.toLocalDataTime.isBefore(year_ago())
    val blocklist = List(":4450", ":4011", ":4012", ":4013", "gitlab-ci-runner", "-depl-")
    blocklist.exists(a.host.contains) || old
  }.foreach(en => kvs.remove[StatEn](en.fid, en.id))))

  Flows.udp(system, kvs).run()

  ws.bindAndHandle
}
