package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source, Merge, GraphDSL, RunnableGraph, Broadcast}
import akka.stream.{ClosedShape, FlowShape}
import akka.util.ByteString
import java.time.{LocalDateTime}
import scala.util.Try
import zd.kvs.Kvs
import zd.proto.api.{encode, decode}
import zero.ext._, either._

object Flows {
  def ws(system: ActorSystem, kvs: Kvs): Flow[WsMessage, WsMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val msgIn = b.add(Flow[WsMessage].collect[ByteString] { case BinaryMessage.Strict(bytes) => bytes })
      val logIn = Flow[Pull].map{ msg => system.log.info(s"IN: ${msg}"); msg }
      val logOut = Flow[Push].map{ msg => system.log.debug(s"OUT: ${msg}"); msg }
      val kvspub = Source.fromGraph(new MsgSource(system.actorOf(KvsPub.props(kvs)))) //todo: created one time? closed?
      val wspub = Source.fromGraph(new MsgSource(system.actorOf(WsPub.props)))
      val pubStat = b.add(Merge[Push](2))

      val decodeMsg = b.add(
        Flow[ByteString].
          map[Either[Throwable, Pull]]{ bytes => Try(decode[Pull](bytes.toArray)).toEither.leftMap(l => {system.log.error(l.toString);l}) }.
          collect[Pull] {
            case Right(pull) => pull
          }
      )

      val encodeMsg = b.add(Flow[Push].map{ case m: Push => encode[Push](m) })
      val msgOut = b.add(Flow[Array[Byte]].map[BinaryMessage] { bytes => BinaryMessage.Strict(ByteString(bytes)) })

      msgIn ~> decodeMsg ~> logIn ~> wsHandler(system, kvs) ~> Sink.ignore
      wspub  ~> pubStat
      kvspub ~> pubStat ~> logOut ~> encodeMsg ~> msgOut

      FlowShape(msgIn.in, msgOut.out)
    })

  def wsHandler(system: ActorSystem, @annotation.unused kvs: Kvs) =
    Flow[Pull].collect {
      case HealthAsk(host) =>
        val live_start = kvs.all(keys.`cpu_mem.live`).toOption.flatMap(_.collect{ case Right(a) => extract(a) }.filter(_.host == host).sortBy(_.time).map{
          case EnData(value, time, host) =>
            system.eventStream publish StatMsg(Metric("cpu_mem", value), time=time, host=host); time
        }.force.headOption).getOrElse(0L);
        {
          kvs.all(keys.`cpu.hour`).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => extract(a) }.filter(_.host == host).sortBy(_.time)
            val min = xs1.lastOption.map(_.time).getOrElse(0L)-60*60*1000 //todo
            xs1.dropWhile(_.time < min).foreach{
              case EnData(value, time, host) =>
                system.eventStream publish StatMsg(Metric("cpu.hour", value), time=time, host=host)
            }
          }
        }
        List((keys.`search.ts.latest`, "search.ts", 5)
          , (keys.`search.wc.latest`, "search.wc", 5)
          , (keys.`search.fs.latest`, "search.fs", 5)
          , (keys.`static.gen.latest`, "static.gen", 5)
          , (keys.`reindex.all.latest`, "reindex.all", 5)
          ).foreach{ case (key, name, n) =>
          kvs.all(key).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => extract(a) }.filter(_.host == host)
            val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
            system.eventStream publish StatMsg(Measure(s"$name.thirdQ", thirdQ), time=0, host=host)
            xs1.sortBy(_.time).takeRight(n).foreach(x =>
              system.eventStream publish StatMsg(Measure(name, x.value), time=x.time, host=host)
            )
          }
        }
        kvs.all(keys.`static.gen.year`).map_(_.collect{ case Right(a) => extract(a) }.filter(_.host == host).sortBy(_.time).dropWhile(_.time.toLocalDataTime().isBefore(year_ago())).foreach{
          case EnData(value, time, host) => system.eventStream publish StatMsg(Measure("static.gen.year", value), time=time, host=host)
        })
        kvs.all(keys.`kvs.size.year`).map_(_.collect{ case Right(a) => extract(a) }.filter(_.host == host).sortBy(_.time).dropWhile(_.time.toLocalDataTime().isBefore(year_ago())).foreach{
          case EnData(value, time, host) => system.eventStream publish StatMsg(Metric("kvs.size.year", value), time=time, host=host)
        })
        kvs.all(keys.`action.live`).map_(_.collect{ case Right(a) => extract(a) }.filter(_.host == host).sortBy(_.time).dropWhile(_.time < live_start).foreach{
          case EnData(action, time, host) => system.eventStream publish StatMsg(Action(action), time=time, host=host)
        })
        kvs.all(keys.`metrics`).map_(_.collect{ case Right(a) => extract(a) }.filter(_.host == host).foreach{
          case EnData(stat, time, host) => stat.split('|') match { //todo: remove split, use name
            case Array(name, value) =>
              system.eventStream publish StatMsg(Metric(name, value), time=time, host=host)
            case _ =>
          }
        })
    }

  def udp(system: ActorSystem, kvs: Kvs): RunnableGraph[NotUsed] = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      import keys._

      val udppub = Source.fromGraph(new MsgSource(system.actorOf(UdpPub.props)))
      val logIn = Flow[Push].map{ msg => system.log.debug("UDP: {}", msg); msg }
      val save_metric = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem"|"kvs.size"|"feature", _), _, _) =>
        case StatMsg(Metric(name, value), time, host) =>
          kvs.put(fid=`metrics`, id=str_to_bytes(s"${host}${name}"), insert(EnData(value=s"${name}|${value}", time=time, host=host)))
      }
      val save_cpumem = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem", value), time, host) =>
          { // live
            val i = kvs.el.get(str_to_bytes(s"cpu_mem.live.idx.$host")).toOption.flatten.map(integer).getOrElse(0)
            for {
              _ <- kvs.put(fid=`cpu_mem.live`, id=str_to_bytes(s"${host}${i}"), insert(EnData(value=value, time=time, host=host)))
              i1 = (i + 1) % 20
              _ <- kvs.el.put(str_to_bytes(s"cpu_mem.live.idx.$host"), int_to_bytes(i1))
            } yield ()
          }
          value.split('~') match {
            case Array(cpu, _, _, _) =>
              { // hour
                val i = ((time / 1000 / 60) % 60) / 3 // [0, 19]
                val now = System.currentTimeMillis
                val last = kvs.el.get(str_to_bytes(s"cpu.hour.t.$host$i")).toOption.flatten.map(long).getOrElse(now)
                val n =
                  if (now - last >= 3*60*1000) 0
                  else kvs.el.get(str_to_bytes(s"cpu.hour.n.$host$i")).toOption.flatten.map(integer).getOrElse(0)
                val v = kvs.el.get(str_to_bytes(s"cpu.hour.v.$host$i")).toOption.flatten.map(float).getOrElse(0f)
                val n1 = n + 1
                val v1 = (v * n + cpu.toInt) / n1
                kvs.el.put(str_to_bytes(s"cpu.hour.t.$host$i"), long_to_bytes(time))
                kvs.el.put(str_to_bytes(s"cpu.hour.n.$host$i"), int_to_bytes(n1))
                kvs.el.put(str_to_bytes(s"cpu.hour.v.$host$i"), float_to_bytes(v1))
                val time1 = ((time / 1000 / 60 / 60 * 60) + i * 3) * 60 * 1000
                kvs.put(fid=`cpu.hour`, id=str_to_bytes(s"$host$i"), insert(EnData(value=v1.toInt.toString, time=time1, host=host)))
                system.eventStream.publish(StatMsg(Metric("cpu.hour", v1.toInt.toString), time=time1, host=host))
              }
            case _ =>
          }
      }
      val save_measure = Flow[Push].collect{
        case StatMsg(Measure(name, value), time, host) =>
          val limit = name match {
            case "reindex.all" => 100
            case _ => 20
          }
          val i = kvs.el.get(str_to_bytes(s"${name}.latest.idx.$host")).toOption.flatten.map(integer).getOrElse(0)
          for {
            _ <- kvs.put(fid=str_to_bytes(s"${name}.latest"), id=str_to_bytes(s"${host}${i}"), insert(EnData(value=value, time=time, host=host)))
            i1 = (i + 1) % limit
            _ <- kvs.el.put(str_to_bytes(s"$name.latest.idx.$host"), int_to_bytes(i1))
          } yield ()
          // calculate new quartile
          kvs.all(str_to_bytes(s"${name}.latest")).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => extract(a)}.filter(_.host == host).toVector.sortBy(_.value.toInt)
            val thirdQ = xs1((xs1.length*0.7).toInt).value
            val msg = StatMsg(Measure(s"${name}.thirdQ", thirdQ), time=0, host=host)
            system.eventStream.publish(msg)
          }
      }

      def saveYearValue(name: String, value: Long, time: Long, host: String): (Long, Long) = {
        val date = time.toLocalDataTime()
        val i = date.getMonthValue - 1
        val now = LocalDateTime.now()
        val last = kvs.el.get(str_to_bytes(s"${name}.year.t.${host}${i}")).toOption.flatten.map(long(_).toLocalDataTime()).getOrElse(now)
        val n =
          if (date.getYear != last.getYear) 0
          else kvs.el.get(str_to_bytes(s"${name}.year.n.${host}${i}")).toOption.flatten.map(integer).getOrElse(0)
        val v = kvs.el.get(str_to_bytes(s"${name}.year.v.${host}${i}")).toOption.flatten.map(long).getOrElse(0L)
        val n1 = n + 1
        val v1 = (v * n + value.toLong) / n1
        kvs.el.put(str_to_bytes(s"${name}.year.t.${host}${i}"), long_to_bytes(time))
        kvs.el.put(str_to_bytes(s"${name}.year.n.${host}${i}"), int_to_bytes(n1))
        kvs.el.put(str_to_bytes(s"${name}.year.v.${host}${i}"), long_to_bytes(v1))
        val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis()
        kvs.put(fid=str_to_bytes(s"${name}.year"), id=str_to_bytes(s"${host}${i}"), insert(EnData(value=v1.toString, time=time1, host=host)))
        (v1, time1)
      }
      val save_year_value = Flow[Push].collect{
        case StatMsg(Measure(name@("static.gen"), value), time, host) =>
          val (v1, t1) = saveYearValue(name, value.toLong, time, host)
          system.eventStream.publish(StatMsg(Measure(s"$name.year", v1.toString), time=t1, host=host))
        case StatMsg(Metric(name@"kvs.size", value), time, host) =>
          val (v1, t1) = saveYearValue(name, value.toLong/1024, time, host)
          system.eventStream.publish(StatMsg(Metric(s"$name.year", v1.toString), time=t1, host=host))
      }
      val save_feature = Flow[Push].collect{
        case StatMsg(Metric("feature", name), time, host) =>
          val date = time.toLocalDataTime()
          val i = date.getMonthValue - 1
          val now = LocalDateTime.now()
          val last = kvs.el.get(str_to_bytes(s"feature.${name}.t.${host}${i}")).toOption.flatten.map(long(_).toLocalDataTime()).getOrElse(now)
          val n =
            if (date.getYear != last.getYear) 0
            else kvs.el.get(str_to_bytes(s"feature.${name}.n.${host}${i}")).toOption.flatten.map(integer).getOrElse(0)
          val n1 = n + 1
          kvs.el.put(str_to_bytes(s"feature.${name}.t.${host}${i}"), long_to_bytes(time))
          kvs.el.put(str_to_bytes(s"feature.${name}.n.${host}${i}"), int_to_bytes(n1))
          val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis()
          kvs.put(fid=`feature`, id=str_to_bytes(s"${host}${i}${name}"), insert(EnData(value=s"${name}~${n1}", time=time1, host=host)))
      }
      val save_action = Flow[Push].collect{
        case StatMsg(Action(action), time, host) =>
          val i = kvs.el.get(str_to_bytes(s"action.live.idx.$host")).toOption.flatten.map(integer).getOrElse(0)
          for {
            _ <- kvs.put(fid=`action.live`, id=str_to_bytes(s"${host}${i}"), insert(EnData(value=action, time=time, host=host)))
            i1 = (i + 1) % 20
            _ <- kvs.el.put(str_to_bytes(s"action.live.idx.$host"), int_to_bytes(i1))
          } yield ()
      }
      val save_error = Flow[Push].collect{
        case StatMsg(Error(exception, stacktrace, toptrace), time, host) =>
          val i = kvs.el.get(str_to_bytes(s"errors.idx.$host")).toOption.flatten.map(integer).getOrElse(0)
          for {
            _ <- kvs.put(fid=`errors`, id=str_to_bytes(s"${host}${i}"), insert(EnData(value=s"${exception}|${stacktrace}|${toptrace}", time=time, host=host)))
            i1 = (i + 1) % 100
            _ <- kvs.el.put(str_to_bytes(s"errors.idx.$host"), int_to_bytes(i1))
          } yield ()
      }
      def pub = Sink.foreach[Push]{ case msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }
      val b1 = b.add(Broadcast[Push](8))

      udppub ~> logIn ~> b1 ~> pub
                         b1 ~> save_metric     ~> Sink.ignore
                         b1 ~> save_cpumem     ~> Sink.ignore
                         b1 ~> save_measure    ~> Sink.ignore
                         b1 ~> save_year_value ~> Sink.ignore
                         b1 ~> save_action     ~> Sink.ignore
                         b1 ~> save_error      ~> Sink.ignore
                         b1 ~> save_feature    ~> Sink.ignore

      ClosedShape
    })
  }
}
