package .stats

import zio._, nio._, core._, core.channels._, clock.Clock
import zd.kvs.{Kvs=>_,Err=>_,_}
import zero.kvs.sec._
import zero.ftier._, ws._
import encoding._
import com.typesafe.config.ConfigFactory

sealed trait NewEvent
case class UdpMsg() extends NewEvent
// case class WsMsg() extends NewEvent

case class KvsErr(e: zd.kvs.Err) extends ForeignErr

object StatsApp {
  implicit object EnDataCodec extends DataCodec[EnData] {
    import zd.proto._, macrosapi._
    implicit val c = caseCodecAuto[EnData]
    def extract(xs: Bytes): EnData = api.decode[EnData](xs)
    def insert(x: EnData): Bytes = api.encodeToBytes(x)
  }
  val httpHandler: http.Request => ZIO[Kvs, Err, http.Response] = {
    case UpgradeRequest(r) => upgrade(r)
  }
  def msgHandler(queue: Queue[NewEvent]): Pull => ZIO[Kvs with WsContext, Err, Unit] = {
    case ask: HealthAsk => ZIO.unit
  }
  def wsHandler(queue: Queue[NewEvent]): Msg => ZIO[WsContext with Kvs, Err, Unit] = {
    case msg: Binary =>
      for {
          message  <- decode[Pull](msg.v.toArray)
          _        <- msgHandler(queue)(message).catchAllCause(cause =>
                          IO.effect(println(s"msg ${cause.failureOption} err ${cause.prettyPrint}"))
                      ).fork.unit
      } yield ()
    case Open => ZIO.unit
    case Close => ZIO.unit
    case msg => Ws.close
  }
  def workerLoop(q: Queue[NewEvent]): ZIO[Kvs, Err, Unit] = q.take.flatMap{
    case UdpMsg() =>
      val host = "todo"
      val ipaddr = "todo"
      val time = 0 //todo
      for {
        _ <- Kvs.put(fid(fid.Nodes()), en_id.str(host), EnData(value=ipaddr, time=time, host=host)).mapError(KvsErr)
      } yield ()
  }
  val app: ZIO[ActorSystem, Any, Unit] = {
    (for {
      ucfg <- IO.effect(ConfigFactory.load().getConfig("stats.server"))
      uhos <- IO.effect(ucfg.getString("host"))
      upor <- IO.effect(ucfg.getInt("port"))
      udpa <- SocketAddress.inetSocketAddress(uhos, upor)
      _    <- udp.bind(udpa)(channel =>
                (for {
                  data <- channel.read
                  _ <- ZIO.unit.map(_ => println(new String(data.toArray)))
                } yield ()).catchAll(ex => ZIO.unit.map(_ => println(ex)))
              ).use(ch => ZIO.never.ensuring(ch.close.ignore)).fork
      q    <- Queue.unbounded[NewEvent]
      _    <- workerLoop(q).forever.fork
      addr <- SocketAddress.inetSocketAddress(8001)
      kvs  <- ZIO.access[Kvs](_.get)
      _    <- httpServer.bind(
                addr,
                IO.succeed(req => httpHandler(req).provideLayer(ZLayer.succeed(kvs))),
                IO.succeed(msg => wsHandler(q)(msg).provideSomeLayer[WsContext](ZLayer.succeed(kvs)))
              )
    } yield ()).provideLayer(Kvs.live ++ (Clock.live >>> udp.live(128))) //todo: get size from client conf + warn about size exceed
  }
  def main(args: Array[String]): Unit = {
    val actorSystemName = "Stats"
    val cfg = s"""
      |akka.remote.netty.tcp.hostname = 127.0.0.1
      |akka.remote.netty.tcp.port = 4343
      |akka.actor.provider = cluster
      |akka.cluster.seed-nodes = [ "akka.tcp://$actorSystemName@127.0.0.1:4343" ]
      |ring.leveldb.dir = rng_data_127.0.0.1_4343""".stripMargin
    Runtime.default.unsafeRun(app.provideLayer(actorSystem(actorSystemName, ConfigFactory.parseString(cfg))).fold(
      err => { println(err); 1 },
      _   => {               0 }
    ))
  }
}