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
  ).foreach{ fid =>
    kvs.all(fid).map_(_.collect{ case Right(a) => a.id -> extract(a) }.filter{ case (_, data) =>
      val old = data.time.toLong.toLocalDataTime().isBefore(year_ago())
      val blocklist = List("gitlab-ci-runner")
      blocklist.exists(data.host.contains) || old
    }.foreach{ case (id, _) => kvs.remove(fid, id) })
  }

  Flows.udp(system, kvs).run()

  ws.bindAndHandle

  // import zio._

  // val app: ZIO[ActorSystem, Any, Unit] = {
  //   (for {
  //     q    <- Queue.unbounded[NewEvent]
  //     _    <- workerLoop(q).forever.fork
  //     addr <- SocketAddress.inetSocketAddress(8001)
  //     kvs  <- ZIO.access[Kvs](_.get)
  //     bl   <- ZIO.access[Blocking](_.get)
  //     _    <- httpServer.bind(
  //               addr,
  //               IO.succeed(req => httpHandler(req).provideLayer(ZLayer.succeed(kvs))),
  //               IO.succeed(msg => wsHandler(q)(msg).provideSomeLayer[WsContext](ZLayer.succeed(kvs) ++ ZLayer.succeed(bl)))
  //             )
  //   } yield ()).provideLayer(Kvs.live ++ Blocking.live)
  // }

  // val runtime = Runtime.default
  // runtime.unsafeRun(app.provideLayer(system).fold(
  //   err => { println(err); 1 },
  //   _   => {               0 }
  // ))
}
