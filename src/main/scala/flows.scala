package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, BinaryMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source, Merge, GraphDSL, RunnableGraph, Broadcast}
import akka.stream.{ClosedShape, FlowShape}
import zd.kvs.Kvs
import java.time.{LocalDateTime}

import akka.util.ByteString
import zd.proto.api.{encode}

object Flows {
  def ws(system: ActorSystem, kvs: Kvs): Flow[WsMessage, WsMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val msgIn = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })
      val logIn = Flow[String].map{ msg => system.log.info(s"IN: ${msg}"); msg }
      val logOut = Flow[String].map{ msg => system.log.debug(s"OUT: ${msg}"); msg }
      val kvspub = Source.fromGraph(new MsgSource(system.actorOf(KvsPub.props(kvs))))
      val wspub = Source.fromGraph(new MsgSource(system.actorOf(WsPub.props)))
      val pub = b.add(Merge[StatMsg](2))

      val toBytes = b.add(Flow[StatMsg].map{
        case m: StatMsg => encode[StatMsg](m)
      })
      val msgOut = b.add(Flow[Array[Byte]].map[BinaryMessage] { bytes => BinaryMessage.Strict(ByteString(bytes)) })

      msgIn ~> logIn ~> Sink.ignore // ignore income messages
      wspub  ~> pub
      kvspub ~> pub ~> toBytes ~> msgOut

      FlowShape(msgIn.in, msgOut.out)
    })

  def udp(system: ActorSystem, kvs: Kvs): RunnableGraph[NotUsed] = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val udppub = Source.fromGraph(new MsgSource(system.actorOf(UdpPub.props)))
      val logIn = Flow[StatMsg].map{ msg => system.log.debug("UDP: {}", msg); msg }
      val save_metric = Flow[StatMsg].collect{
        case MetricStat("cpu_mem"| "kvs.size", _, _, _) =>
        case MetricStat(name, value, time, addr) =>
          kvs.put(StatEn(fid="metrics", id=s"${addr}${name}", prev=zd.kvs.empty, s"${name}|${value}", time, addr))
      }
      val save_cpumem = Flow[StatMsg].collect{
        case MetricStat("cpu_mem", value, time, addr) =>
          { // live
            val i = kvs.el.get[String](s"cpu_mem.live.idx.${addr}").getOrElse("0")
            for {
              _ <- kvs.put(StatEn(fid="cpu_mem.live", id=s"${addr}${i}", prev=zd.kvs.empty, value, time, addr))
              i1 = ((i.toInt + 1) % 20).toString
              _ <- kvs.el.put(s"cpu_mem.live.idx.${addr}", i1)
            } yield ()
          }
          value.split('~') match {
            case Array(cpu, _, _) =>
              { // hour
                val i = ((time.toLong / 1000 / 60) % 60) / 3 // [0, 19]
                val now = System.currentTimeMillis
                val last = kvs.el.get[String](s"cpu.hour.t.${addr}${i}").map(_.toLong).getOrElse(now)
                val n =
                  if (now - last >= 3*60*1000) 0
                  else kvs.el.get[String](s"cpu.hour.n.${addr}${i}").map(_.toInt).getOrElse(0)
                val v = kvs.el.get[String](s"cpu.hour.v.${addr}${i}").map(_.toFloat).getOrElse(0f)
                val n1 = n + 1
                val v1 = (v * n + cpu.toInt) / n1
                kvs.el.put(s"cpu.hour.t.${addr}${i}", time)
                kvs.el.put(s"cpu.hour.n.${addr}${i}", n1.toString)
                kvs.el.put(s"cpu.hour.v.${addr}${i}", v1.toString)
                val time1 = (((time.toLong / 1000 / 60 / 60 * 60) + i * 3) * 60 * 1000).toString
                kvs.put(StatEn(fid="cpu.hour", id=s"${addr}${i}", prev=zd.kvs.empty, v1.toInt.toString, time1, addr))
                system.eventStream.publish(MetricStat("cpu.hour", v1.toInt.toString, time1, addr))
              }
            case _ =>
          }
      }
      val save_measure = Flow[StatMsg].collect{
        case MeasureStat(name, value, time, addr) =>
          val limit = name match {
            case _ if name.startsWith("reindex") => 100
            case _ => 20
          }
          val i = kvs.el.get[String](s"${name}.latest.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid=s"${name}.latest", id=s"${addr}${i}", prev=zd.kvs.empty, value, time, addr))
            i1 = ((i.toInt + 1) % limit).toString
            _ <- kvs.el.put(s"${name}.latest.idx.${addr}", i1)
          } yield ()
          // calculate new quartile
          kvs.stream_unsafe[StatEn](s"${name}.latest").map{ xs =>
            val xs1 = xs.filter(_.addr == addr).toVector.sortBy(_.data)
            val thirdQ = xs1((xs1.length*0.7).toInt).data
            val msg = MeasureStat(s"${name}.thirdQ", thirdQ, time="0", addr)
            system.eventStream.publish(msg)
          }
      }

      def saveYearValue(name: String, value: Long, time: String, addr: String): (Long, String) = {
        val date = time.toLong.toLocalDataTime
        val i = date.getMonthValue - 1
        val now = LocalDateTime.now()
        val last = kvs.el.get[String](s"${name}.year.t.${addr}${i}").map(_.toLong.toLocalDataTime).getOrElse(now)
        val n =
          if (date.getYear != last.getYear) 0
          else kvs.el.get[String](s"${name}.year.n.${addr}${i}").map(_.toInt).getOrElse(0)
        val v = kvs.el.get[String](s"${name}.year.v.${addr}${i}").map(_.toLong).getOrElse(0L)
        val n1 = n + 1
        val v1 = (v * n + value.toLong) / n1
        kvs.el.put(s"${name}.year.t.${addr}${i}", time)
        kvs.el.put(s"${name}.year.n.${addr}${i}", n1.toString)
        kvs.el.put(s"${name}.year.v.${addr}${i}", v1.toString)
        val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis.toString
        kvs.put(StatEn(fid=s"${name}.year", id=s"${addr}${i}", prev=zd.kvs.empty, v1.toString, time1, addr))
        (v1, time1)
      }
      val save_year_value = Flow[StatMsg].collect{
        case MeasureStat(name@("static.create" | "static.gen"), value, time, addr) => 
          val (v1, t1) = saveYearValue(name, value.toLong, time, addr)
          system.eventStream.publish(MeasureStat(s"${name}.year", v1.toString, t1, addr))
        case MetricStat(name@"kvs.size", value, time, addr) => 
          val (v1, t1) = saveYearValue(name, value.toLong/1024/1024, time, addr)
          system.eventStream.publish(MetricStat(s"${name}.year", v1.toString, t1, addr))
      }
      val save_action = Flow[StatMsg].collect{
        case ActionStat(action, time, addr) =>
          val i = kvs.el.get[String](s"action.live.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid="action.live", id=s"${addr}${i}", prev=zd.kvs.empty, action, time, addr))
            i1 = ((i.toInt + 1) % 20).toString
            _ <- kvs.el.put(s"action.live.idx.${addr}", i1)
          } yield ()
      }
      val save_error = Flow[StatMsg].collect{
        case ErrorStat(exception, stacktrace, toptrace, time, addr) =>
          val i = kvs.el.get[String](s"errors.idx.${addr}").getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid="errors", id=s"${addr}${i}", prev=zd.kvs.empty, s"${exception}|${stacktrace}|${toptrace}", time, addr))
            i1 = ((i.toInt + 1) % 100).toString
            _ <- kvs.el.put(s"errors.idx.${addr}", i1)
          } yield ()
      }
      def pub = Sink.foreach[StatMsg]{ case msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }
      val b1 = b.add(Broadcast[StatMsg](7))

      udppub ~> logIn ~> b1 ~> pub
                         b1 ~> save_metric  ~> Sink.ignore
                         b1 ~> save_cpumem  ~> Sink.ignore
                         b1 ~> save_measure ~> Sink.ignore
                         b1 ~> save_year_value   ~> Sink.ignore
                         b1 ~> save_action  ~> Sink.ignore
                         b1 ~> save_error   ~> Sink.ignore

      ClosedShape
    })
  }
}
