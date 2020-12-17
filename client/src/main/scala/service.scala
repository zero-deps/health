import java.io.File
import zero.ext._, option._, int._
import zio._, duration._, clock._
import zio.nio.core.SocketAddress
import zd.proto._, api._
import ftier.udp._
import ftier.{Err, ForeignErr}
import metrics.api._

package object metrics {
  type Metrics = Has[Metrics.Service]

  object Metrics {
    trait Service {
      def metric(name: String, value: String)         : IO[Err, Unit]
      def error(msg: Option[String], cause: Throwable): IO[Err, Unit]
      def measure(name: String, ns: Long)             : IO[Err, Unit]
      def measure(name: String)(z: IO[Err, Unit])     : IO[Err, Unit]
      def action(name: String)                        : IO[Err, Unit]
    }

    def live(cfg: Config): ZLayer[Udp with ZEnv, Err, Metrics] =
      ZLayer.fromFunctionM { env =>
        for {
          _ <-  uptime(cfg).provide(env).fork
          _ <- threads(cfg).provide(env).fork
          _ <-  dbSize(cfg).provide(env).fork
          _ <- cpu_mem(cfg).provide(env).fork
          _ <-   fileD(cfg).provide(env).fork
          _ <-      fs(cfg).provide(env).fork
          _ <-      gc(cfg).provide(env).fork
        } yield new Service {
          
          def metric(name: String, value: String): IO[Err, Unit] =
            send(MetricMsg(name=name, value=value))

          def error(msg: Option[String], cause: Throwable): IO[Err, Unit] =
            for {
              c  <- IO.effectTotal(fromNullable(cause.getMessage))
              st <- IO.effectTotal(cause.getStackTrace.take(2).map(_.toString).toList)
              _  <- send(ErrorMsg(msg=msg, cause=c, st))
            } yield ()

          def measure(name: String, ns: Long): IO[Err, Unit] =
            send(MeasureMsg(name, Math.max(1, ns./(i"1'000'000").toInt)))

          def measure(name: String)(z: IO[Err, Unit]): IO[Err, Unit] =
            (for {
              t0 <- nanoTime
              _  <- z
              t1 <- nanoTime
              _  <- measure(name, t1-t0)
            } yield ()).provide(env)

          def action(name: String): IO[Err, Unit] =
            for {
              _  <- send(ActionMsg(name))
              cm <- sys.cpu_mem()
              _  <- cm.cata(cm => send(cm), IO.unit)
            } yield ()

          private def send(msg: Msg): IO[Err, Unit] =
            metrics.Metrics.send(msg, cfg).provide(env)
        }
      }

    case class Config
      ( host: String
      , port: Int
      , dbDir: String
      , msgSize: Int=256 /* in bytes */
      )

    private def uptime(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      import Schedule.{delayed, forever}
      val schedule = delayed(forever.map{
        case i if i < 12 => 5 seconds
        case i if i < 72 => 1 minute
        case i           => 1 hour
      })
      (for {
        t   <- sys.uptime_sec()
        msg <- IO.succeed(MetricMsg.uptime(sec=t))
        _   <- send(msg, cfg)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def threads(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      val schedule = Schedule.once ++ Schedule.spaced(5 minutes)
      (for {
        msg <- sys.threads()
        _   <- send(msg, cfg)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def dbSize(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      val schedule = Schedule.once ++ Schedule.spaced(1 day)
      (for {
        s <- IO.effectTotal(sys.dirSize(new File(cfg.dbDir)))
        _ <- send(MetricMsg.dbSize(s), cfg)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def cpu_mem(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      val schedule = Schedule.once ++ Schedule.spaced(1 minute)
      (for {
        msg <- sys.cpu_mem()
        _   <- msg.cata(msg => send(msg, cfg), IO.unit)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def fileD(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      val schedule = Schedule.once ++ Schedule.spaced(5 minutes)
      (for {
        msg <- sys.fileD()
        _   <- msg.cata(msg => send(msg, cfg), IO.unit)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def fs(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      val schedule = Schedule.once ++ Schedule.spaced(1 hour)
      (for {
        msg <- sys.fs()
        _   <- msg.cata(msg => send(msg, cfg), IO.unit)
      } yield ()).repeat(schedule).retry(schedule).unit
    }

    private def gc(cfg: Config): ZIO[Udp with ZEnv, Err, Unit] = {
      for {
        msg <- sys.gcEvent()
        _   <- send(msg, cfg)
      } yield ()
    }

    private def send(msg: Msg, cfg: Config): ZIO[Udp with ZEnv, Err, Unit] =
      for {
        bs <- IO.effectTotal(encodeToBytes[Msg](msg))
        _  <- IO.when(bs.length > cfg.msgSize)(IO.fail(LongMsg))
        publicAddress <- SocketAddress.inetSocketAddress(cfg.host, cfg.port).orDie
        _  <- connect(publicAddress).use(_.send(Chunk.fromArray(bs.unsafeArray)))
      } yield ()

    case object LongMsg extends ForeignErr
  }

  def metric(name: String, value: String): ZIO[Metrics, Err, Unit] =
    ZIO.accessM(_.get.metric(name=name, value=value))

  def error(msg: Option[String], cause: Throwable): ZIO[Metrics, Err, Unit] =
    ZIO.accessM(_.get.error(msg, cause))

  def measure(name: String, ns: Long): ZIO[Metrics, Err, Unit] =
    ZIO.accessM(_.get.measure(name, ns))

  def measure(name: String)(z: IO[Err, Unit]): ZIO[Metrics, Err, Unit] =
    ZIO.accessM(_.get.measure(name)(z))

  def action(name: String): ZIO[Metrics, Err, Unit] =
    ZIO.accessM(_.get.action(name))
}
