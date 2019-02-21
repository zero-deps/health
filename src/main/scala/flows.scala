package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source, Merge, GraphDSL, RunnableGraph}
import akka.stream.{ClosedShape, FlowShape}
import .kvs.Kvs
import scala.util.Try
import scalaz._
import scalaz.Scalaz._

object Flows {
  def ws(system: ActorSystem, kvs: Kvs): Flow[WsMessage, WsMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val msgIn = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })
      val logIn = Flow[String].map{ msg => system.log.info(s"IN: ${msg}"); msg }
      val logOut = Flow[String].map{ msg => system.log.debug(s"OUT: ${msg}"); msg }
      val kvspub = Source.fromGraph(new MsgSource(system.actorOf(KvsPub.props(kvs))))
      val wspub = Source.fromGraph(new MsgSource(system.actorOf(WsPub.props)))
      val pub = b.add(Merge[Msg](2))
      val toMsg = b.add(Flow[Msg].map{
        case Msg(MetricStat(name, value), StatMeta(time, addr)) =>
          s"metric::${name}::${value}::${time}::${addr}"
        case Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(time, addr)) =>
          s"error::${exception}::${stacktrace}::${toptrace}::${time}::${addr}"
        case Msg(ActionStat(action), StatMeta(time, addr)) =>
          s"action::${action}::${time}::${addr}"
      })
      val msgOut = b.add(Flow[String].map[TextMessage] { TextMessage.Strict })

      msgIn ~> logIn ~> Sink.ignore // ignore income messages
      wspub  ~> pub
      kvspub ~> pub ~> toMsg ~> logOut ~> msgOut

      FlowShape(msgIn.in, msgOut.out)
    })

  def udp(system: ActorSystem, kvs: Kvs): RunnableGraph[NotUsed] = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val udppub = Source.fromGraph(new MsgSource(system.actorOf(UdpPub.props)))
      val logIn = Flow[Msg].map{ msg => system.log.debug("UDP: {}", msg); msg }
      val save = Flow[Msg].map{ msg =>
        msg match {
          case Msg(MetricStat(name, value), StatMeta(time, addr)) =>
            kvs.put(StatEn(fid="metrics", id=s"${addr}${name}", prev=.kvs.empty, s"${name}|${value}", time, addr))
          case _ =>
        }
        msg match {
          case Msg(MetricStat("cpu_mem", value), StatMeta(time, addr)) =>
            for {
              c <- kvs.el.get[String]("cpu_mem.live.idx").fold(
                l => l match {
                  case .kvs.NotFound(_) => "0".right
                  case l => l.left
                },
                r => r.right
              )
              _ <- kvs.put(StatEn(fid="cpu_mem.live", id=s"${addr}${c}", prev=.kvs.empty, s"cpu_mem|${value}", time, addr))
              c1 <- c.parseInt.disjunction.bimap(x => .kvs.Fail(x.toString), x => (x + 1) % 20).map(_.toString)
              _ <- kvs.el.put("cpu_mem.live.idx", c1)
            } yield ()
          case _ =>
        }
        msg match {
          case Msg(ActionStat(action), StatMeta(time, addr)) =>
            for {
              c <- kvs.el.get[String]("action.live.idx").fold(
                l => l match {
                  case .kvs.NotFound(_) => "0".right
                  case l => l.left
                },
                r => r.right
              )
              _ <- kvs.put(StatEn(fid="action.live", id=s"${addr}${c}", prev=.kvs.empty, action, time, addr))
              c1 <- c.parseInt.disjunction.bimap(x => .kvs.Fail(x.toString), x => (x + 1) % 20).map(_.toString)
              _ <- kvs.el.put("action.live.idx", c1)
            } yield ()
          case _ =>
        }
        msg match {
          case Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(time, addr)) =>
            for {
              i <- kvs.el.get[String]("errors.idx").fold(
                l => l match {
                  case .kvs.NotFound(_) => "0".right
                  case l => l.left
                },
                r => r.right
              )
              _ <- kvs.put(StatEn(fid="errors", id=s"${addr}${i}", prev=.kvs.empty, s"${exception}|${stacktrace}|${toptrace}", time, addr))
              i1 <- Try(i.toInt).toEither.disjunction.bimap(x => .kvs.Fail(x.toString), x => (x + 1) % 20).map(_.toString)
              _ <- kvs.el.put("errors.idx", i1)
            } yield ()
          case _ =>
        }
        msg
      }
      val pub = Sink.foreach[Msg]{ msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }

      udppub ~> logIn ~> save ~> pub

      ClosedShape
    })
  }
}
