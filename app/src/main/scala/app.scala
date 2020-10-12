package .stats

import annotation.unused

import zio._, nio._, core._, clock.Clock // core.channels._,
import kvs.{Kvs=>_,Err=>_,_}
import kvs.seq._
import ftier._, ws._
import encoding._
import com.typesafe.config.ConfigFactory

sealed trait NewEvent
case class UdpMsg() extends NewEvent
// case class WsMsg() extends NewEvent

case class KvsErr(e: kvs.Err) extends ForeignErr

object StatsApp extends zio.App {
  implicit object EnDataCodec extends DataCodec[EnData] {
    import zd.proto._, macrosapi._
    implicit val c = caseCodecAuto[EnData]
    def extract(xs: Bytes): EnData = api.decode[EnData](xs)
    def insert(x: EnData): Bytes = api.encodeToBytes(x)
  }
  
  val httpHandler: http.Request => ZIO[Kvs, Err, http.Response] = {
    case UpgradeRequest(r) => upgrade(r)
  }

  def msgHandler(@unused queue: Queue[NewEvent]): Pull => ZIO[Kvs with WsContext, Err, Unit] = {
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
      val time = 0L //todo
      for {
        _ <- Kvs.put(fid(fid.Nodes()), en_id.str(host), EnData(value=ipaddr, time=time, host=host)).mapError(KvsErr)
      } yield ()
  }

  val leveldbConf =  _root_.kvs.Kvs.LeveldbConf("rng_data")
  val conf = _root_.kvs.Kvs.RngConf(leveldbConf=leveldbConf)

  type Stats = Has[client.Stats]
  def stats(leveldbConf: _root_.kvs.Kvs.LeveldbConf): URLayer[ActorSystem, Stats] = {
    ZLayer.fromService(client.Stats(leveldbConf.dir))
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
    } yield ()).provideLayer(stats(leveldbConf) ++ Kvs.live(conf) ++ (Clock.live >>> udp.live(128))) //todo: get size from client conf + warn about size exceed
  }

  def run(@unused args: List[String]): URIO[ZEnv, ExitCode] = {
    val as = "Stats"
    val cfg = s"""
      |akka.remote.netty.tcp.hostname = 127.0.0.1
      |akka.remote.netty.tcp.port = 4343
      |akka.actor.provider = cluster
      |akka.cluster.seed-nodes = [ "akka.tcp://$as@127.0.0.1:4343" ]
      |""".stripMargin
    app.provideLayer(actorSystem(as, ConfigFactory.parseString(cfg))).exitCode
  }
}