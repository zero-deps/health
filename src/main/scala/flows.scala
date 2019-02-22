package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source, Merge, GraphDSL, RunnableGraph, Broadcast}
import akka.stream.{ClosedShape, FlowShape}
import .kvs.Kvs
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
        case Msg(MeasureStat(name, value), StatMeta(time, addr)) =>
          s"measure::${name}::${value.toString}::${time}::${addr}"
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
      val save_metric = Flow[Msg].collect{
        case Msg(MetricStat(name, value), StatMeta(time, addr)) =>
          kvs.put(StatEn(fid="metrics", id=s"${addr}${name}", prev=.kvs.empty, s"${name}|${value}", time, addr))
      }
      val save_cpumem = Flow[Msg].collect{
        case Msg(MetricStat("cpu_mem", value), StatMeta(time, addr)) =>
          val i = kvs.el.get[String](s"cpu_mem.live.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid="cpu_mem.live", id=s"${addr}${i}", prev=.kvs.empty, s"cpu_mem|${value}", time, addr))
            i1 = ((i.toInt + 1) % 20).toString
            _ <- kvs.el.put(s"cpu_mem.live.idx.${addr}", i1)
          } yield ()
      }
      val save_measure = Flow[Msg].collect{
        case Msg(MeasureStat(name, value), StatMeta(time, addr)) =>
          val i = kvs.el.get[String](s"${name}.latest.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid=s"${name}.latest", id=s"${addr}${i}", prev=.kvs.empty, value, time, addr))
            i1 = ((i.toInt + 1) % 20).toString
            _ <- kvs.el.put(s"${name}.latest.idx.${addr}", i1)
          } yield ()
          // calculate new quartile
          kvs.stream_unsafe[StatEn](s"${name}.latest").map{ xs =>
            val xs1 = xs.filter(_.addr == addr).toVector.sortBy(_.data)
            val thirdQ = xs1((xs1.length*0.7).toInt).data
            val msg = Msg(MeasureStat(s"${name}.thirdQ", thirdQ), StatMeta(time="", addr))
            system.eventStream.publish(msg)
          }
      }
      val save_action = Flow[Msg].collect{
        case Msg(ActionStat(action), StatMeta(time, addr)) =>
          val i = kvs.el.get[String](s"action.live.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid="action.live", id=s"${addr}${i}", prev=.kvs.empty, action, time, addr))
            i1 = ((i.toInt + 1) % 20).toString
            _ <- kvs.el.put(s"action.live.idx.${addr}", i1)
          } yield ()
      }
      val save_error = Flow[Msg].collect{
        case Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(time, addr)) =>
          val i = kvs.el.get[String](s"errors.idx.${addr}").getOrElse("")
          for {
            _ <- kvs.put(StatEn(fid="errors", id=s"${addr}${i}", prev=.kvs.empty, s"${exception}|${stacktrace}|${toptrace}", time, addr))
            i1 = ((i.toInt + 1) % 100).toString
            _ <- kvs.el.put(s"errors.idx.${addr}", i1)
          } yield ()
      }
      def pub = Sink.foreach[Msg]{ case msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }
      val b1 = b.add(Broadcast[Msg](6))

      udppub ~> logIn ~> b1 ~> pub
                         b1 ~> save_metric  ~> Sink.ignore
                         b1 ~> save_cpumem  ~> Sink.ignore
                         b1 ~> save_measure ~> Sink.ignore
                         b1 ~> save_action  ~> Sink.ignore
                         b1 ~> save_error   ~> Sink.ignore

      ClosedShape
    })
  }
}
