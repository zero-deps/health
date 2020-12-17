package metrics
package app

import annotation.unused

import zero.ext.{seq=>_,_}, option._, int._
import zio._, nio._, core._, clock._, stream._
import kvs.{Err=>_,Throwed=>_,_}, seq._
import ftier._, ws._, udp._
import com.typesafe.config.ConfigFactory
import zd.proto.api._
import java.util.concurrent.TimeUnit.{MILLISECONDS => `in ms`, HOURS => `in hours`}

sealed trait NewEvent
case class Broadcast(msg: Push) extends NewEvent

case class KvsErr(e: kvs.Err) extends ForeignErr

object MetricsApp extends zio.App {
  implicit object NodeCodec extends DataCodec[Node] {
    import zd.proto._, api._, macrosapi._
    implicit val c = caseCodecAuto[Node]
    def extract(xs: Bytes): Node = decode[Node](xs)
    def insert(x: Node): Bytes = encodeToBytes(x)
  }
  implicit object TimedStringCodec extends DataCodec[Timed[String]] {
    import zd.proto._, api._, macrosapi._
    implicit val c = caseCodecAuto[Timed[String]]
    def extract(xs: Bytes): Timed[String] = decode[Timed[String]](xs)
    def insert(x: Timed[String]): Bytes = encodeToBytes(x)
  }
  implicit object AvgDataCodec extends DataCodec[AvgData] {
    import zd.proto._, api._, macrosapi._
    implicit val c = caseCodecAuto[AvgData]
    def extract(xs: Bytes): AvgData = decode[AvgData](xs)
    def insert(x: AvgData): Bytes = encodeToBytes(x)
  }
  implicit object QDataCodec extends DataCodec[QData] {
    import zd.proto._, api._, macrosapi._
    implicit val tc = caseCodecAuto[Timed[Int]]
    implicit val c = caseCodecAuto[QData]
    def extract(xs: Bytes): QData = decode[QData](xs)
    def insert(x: QData): Bytes = encodeToBytes(x)
  }
  implicit object TimedErrCodec extends DataCodec[TimedErr] {
    import zd.proto._, api._, macrosapi._
    implicit val tc = caseCodecAuto[Timed[Int]]
    implicit val c = caseCodecAuto[TimedErr]
    def extract(xs: Bytes): TimedErr = decode[TimedErr](xs)
    def insert(x: TimedErr): Bytes = encodeToBytes(x)
  }

  type Clients = Set[WsContextData]
  
  val httpHandler: PartialFunction[http.Request, ZIO[Kvs, Err, http.Response]] = {
    case UpgradeRequest(r) => upgrade(r)
  }

  def msgHandler(@unused queue: Queue[NewEvent]): Pull => ZIO[Kvs with WsContext, Err, Unit] = {
    case ask: HealthAsk =>
      import ask.host
      for {
        metrics <- Kvs.all[Timed[String]](fid(fid.Metrics(host)))
        _       <- metrics.mapError(KvsErr).foreach{ case (k, en) => send(StatMsg(stat=Metric(name=en_id.metric(k).name, value=en.value), time=en.time, host=host))}
        cpumeml <- Kvs.array.all[Timed[String]](fid(fid.CpuMemLive(host)))
        _       <- cpumeml.mapError(KvsErr).foreach(en => send(StatMsg(stat=Metric(name="cpu_mem", value=en.value), time=en.time, host=host)))
        actions <- Kvs.array.all[Timed[String]](fid(fid.ActionLive(host)))
        _       <- actions.mapError(KvsErr).foreach(en => send(StatMsg(stat=Action(en.value), time=en.time, host=host)))
        cpuday  <- Kvs.array.all[AvgData](fid(fid.CpuDay(host)))
        _       <- cpuday.mapError(KvsErr).foreach(en => send(StatMsg(stat=Metric(name="cpu.day", value=en.value.toInt.toString), time=en.id*i"3'600'000", host=host)))
        measure <- Kvs.all[QData](fid(fid.Measures(host)))
        _       <- measure.mapError(KvsErr).foreach{ case (k, en) =>
                    val name = en_id.measure(k).name
                    for {
                      _ <- send(StatMsg(stat=Measure(s"$name.thirdQ", en.q_str), time=0, host=host))
                      _ <- ZStream.fromIterable(en.xs.takeRight(5)).foreach(x => send(StatMsg(stat=Measure(name, x.value_str), time=x.time, host=host)))
                    } yield ()
                  }
        errs    <- Kvs.array.all[TimedErr](fid(fid.Errors(host)))
        _       <- errs.mapError(KvsErr).foreach(en => send(StatMsg(stat=Error(msg=en.msg, cause=en.cause, st=en.st), time=en.time, host=host)))
      } yield ()
  }

  def wsHandler(queue: Queue[NewEvent], clients: Ref[Clients]): Msg => ZIO[WsContext with Kvs, Err, Unit] = {
    case msg: Binary =>
      for {
        message  <- IO.effect(decode[Pull](msg.v.toArray)).mapError(Throwed)
        _        <- msgHandler(queue)(message).catchAllCause(cause =>
                        IO.effect(println(s"msg ${cause.failureOption} err ${cause.prettyPrint}"))
                    ).fork.unit
      } yield ()
    case Open =>
      for {
        ctx   <- ZIO.access[WsContext](_.get)
        _     <- clients.update(_ + ctx)
        nodes <- Kvs.all[Node](fid(fid.Nodes()))
        _     <- nodes.map(_._2).mapError(KvsErr).foreach(en => send(HostMsg(host=en.host, time=en.time)))
        errsc <- Kvs.array.all[TimedErr](fid(fid.CommonErrors()))
        _     <- errsc.mapError(KvsErr).foreach(en => send(StatMsg(stat=Error(msg=en.msg, cause=en.cause, st=en.st), time=en.time, host="N/A")))
      } yield ()
    case Close =>
      for {
        ctx <- ZIO.access[WsContext](_.get)
        _   <- clients.update(_ - ctx)
      } yield ()
    case msg => Ws.close
  }

  def broadcast(clients: Ref[Clients], msg: Push): IO[Err, Unit] = {
    for {
      xs <- clients.get
      _  <- ZStream.fromIterable(xs).foreach(ctx => send(msg).provideLayer(ZLayer.succeed(ctx)))
    } yield ()
  }

  def workerLoop(q: Queue[NewEvent], @unused clients: Ref[Clients]): ZIO[Kvs with Clock, Err, Unit] = q.take.flatMap{
    case Broadcast(x) =>
      broadcast(clients, x)
  }

  val conf = store.RngConf()
  val dbDir = conf.conf.leveldbConf.dir
  val metricsServerConf = SocketAddress.inetSocketAddress("127.0.0.1", 50123)
  val metricsClientConf = Metrics.Config("127.0.0.1", 50123, dbDir)
  val udpSize = metricsClientConf.msgSize
  val as = "metrics"
  val cfg = s"""
    akka {
      actor {
        loglevel = error
        loggers = [akka.event.slf4j.Slf4jLogger]
        logging-filter = akka.event.slf4j.Slf4jLoggingFilter
        provider = cluster
      }
      remote {
        artery {
          enabled = true
          transport = tcp
          canonical {
            hostname = 127.0.0.1
            port = 4343
          }
        }
      }
      cluster {
        seed-nodes = [ "akka://$as@127.0.0.1:4343" ]
      }
    }
    """

  val app: ZIO[ZEnv, Any, Unit] = {
    (for {
      q    <- Queue.unbounded[NewEvent]
      clients <- Ref.make(Set.empty: Clients)
      udpa <- metricsServerConf
      _    <- udp.bind(udpa)(channel =>
                for {
                  data <- channel.read
                  (host, bs) = data
                  msg  <- IO.effect(decode[api.Msg](bs.toArray)).mapError(Throwed)
                  time <- currentTime(`in ms`)
                  _    <- Kvs.put(fid(fid.Nodes()), en_id(en_id.Host(host)), Node(time=time, host=host)).mapError(KvsErr)
                  // _    <- q offer Broadcast(HostMsg(host=host, time=time))
                  _    <- msg match {
                    case x: api.MetricMsg if x.name == "cpu_mem" =>
                      for {
                        /* live (cpu+mem) */
                        _ <- Kvs.array.add(fid(fid.CpuMemLive(host)), size=20, Timed(value=x.value, time=time)).mapError(KvsErr)
                        /* day (cpu) */
                        cpu   <- x.value.split('~').head.some.filter(_.nonEmpty).cata(parseInt, IO.succeed(0))
                        hours <- currentTime(`in hours`)
                        hour   = (hours % 24) + 1 /* ∊ [1, 24] */
                        old   <- Kvs.array.get[AvgData](fid(fid.CpuDay(host)), idx=hour).mapError(KvsErr)
                        (value, n) = old.cata({
                          case old if old.id == hours =>
                            /* calculate new average */
                            val n = old.n + 1
                            val value = (old.value * old.n.toDouble + cpu.toDouble) / n.toDouble
                            (value, n)
                          case old =>
                            /* save new average for new hour */
                            (cpu.toDouble, 1L)
                        }
                        , (cpu.toDouble, 1L)
                        )
                        _ <- Kvs.array.put(fid(fid.CpuDay(host)), idx=hour, AvgData(value=value, id=hours, n=n)).mapError(KvsErr)
                      } yield ()
                    case x: api.MetricMsg =>
                      for {
                        _ <- Kvs.put(fid(fid.Metrics(host)), en_id(en_id.Metric(x.name)), Timed(value=x.value, time=time)).mapError(KvsErr)
                      } yield ()
                    case x: api.MeasureMsg =>
                      for {
                        qd <- Kvs.get[QData](fid(fid.Measures(host)), en_id(en_id.Measure(x.name))).mapError(KvsErr)
                        xs  = (Timed(x.value, time) +: qd.cata(_.xs, Vector.empty)).take(20)
                        q   = xs(Math.ceil(xs.size*0.9).toInt-1).value
                        _  <- Kvs.put(fid(fid.Measures(host)), en_id(en_id.Measure(x.name)), QData(xs, q)).mapError(KvsErr)
                      } yield ()
                    case x: api.ErrorMsg =>
                      for {
                        _ <- Kvs.array.add(fid(fid.Errors(host)),   size=10, TimedErr(msg=x.msg, cause=x.cause, st=x.st, time)).mapError(KvsErr)
                        _ <- Kvs.array.add(fid(fid.CommonErrors()), size=20, TimedErr(msg=x.msg, cause=x.cause, st=x.st, time)).mapError(KvsErr)
                      } yield ()
                    case x: api.ActionMsg =>
                      for {
                        _ <- Kvs.array.add(fid(fid.ActionLive(host)), size=20, Timed(value=x.action, time=time)).mapError(KvsErr)
                      } yield ()
                  }
                } yield ()
              ).use(ch => ZIO.never.ensuring(ch.close.ignore)).fork
      _    <- workerLoop(q, clients).forever.fork
      addr <- SocketAddress.inetSocketAddress(8001)
      kvs  <- ZIO.access[Kvs](_.get)
      _    <- httpServer.bind(
                addr,
                IO.succeed(req => httpHandler(req).provideLayer(ZLayer.succeed(kvs))),
                IO.succeed(msg => wsHandler(q, clients)(msg).provideSomeLayer[WsContext](ZLayer.succeed(kvs)))
              )
    } yield ()).provideCustomLayer(
      ((Udp.live(udpSize) ++ ZEnv.live) >>> Metrics.live(metricsClientConf))
      ++ (actorSystem(as, ConfigFactory.parseString(cfg)) >>> Kvs.live(conf))
      ++ (ZEnv.live >>> Udp.live(udpSize))
    )
  }

  def run(@unused args: List[String]): URIO[ZEnv, ExitCode] =
    app.exitCode
}
