package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source, Merge, GraphDSL, RunnableGraph, Broadcast}
import akka.stream.{ClosedShape, FlowShape}
import akka.util.ByteString
import java.time.{LocalDateTime}
import scala.util.Try
import zd.kvs._
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
        val live_start = kvs.all(fid(fid.CpuMemLive(host))).toOption.flatMap(_.collect{ case Right(a) => extract(a) }.sortBy(_.time).map{
          case EnData(value, time, host) =>
            system.eventStream publish StatMsg(Metric("cpu_mem", value), time=time, host=host); time
        }.force.headOption).getOrElse(0L);
        {
          kvs.all(fid(fid.CpuHour(host))).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => extract(a) }.sortBy(_.time)
            val min = xs1.lastOption.map(_.time).getOrElse(0L)-60*60*1000 //todo
            xs1.dropWhile(_.time < min).foreach{
              case EnData(value, time, host) =>
                system.eventStream publish StatMsg(Metric("cpu.hour", value), time=time, host=host)
            }
          }
        }
        kvs.all(fid(fid.SearchTs(host))).map_{ xs =>
          val xs1 = xs.collect{ case Right(a) => extract(a) }
          val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
          system.eventStream publish StatMsg(Measure(s"search.ts.thirdQ", thirdQ), time=0, host=host)
          xs1.sortBy(_.time).takeRight(5).foreach(x =>
            system.eventStream publish StatMsg(Measure("search.ts", x.value), time=x.time, host=host)
          )
        }
        kvs.all(fid(fid.SearchWc(host))).map_{ xs =>
          val xs1 = xs.collect{ case Right(a) => extract(a) }
          val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
          system.eventStream publish StatMsg(Measure(s"search.wc.thirdQ", thirdQ), time=0, host=host)
          xs1.sortBy(_.time).takeRight(5).foreach(x =>
            system.eventStream publish StatMsg(Measure("search.wc", x.value), time=x.time, host=host)
          )
        }
        kvs.all(fid(fid.SearchFs(host))).map_{ xs =>
          val xs1 = xs.collect{ case Right(a) => extract(a) }
          val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
          system.eventStream publish StatMsg(Measure(s"search.fs.thirdQ", thirdQ), time=0, host=host)
          xs1.sortBy(_.time).takeRight(5).foreach(x =>
            system.eventStream publish StatMsg(Measure("search.fs", x.value), time=x.time, host=host)
          )
        }
        kvs.all(fid(fid.StaticGen(host))).map_{ xs =>
          val xs1 = xs.collect{ case Right(a) => extract(a) }
          val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
          system.eventStream publish StatMsg(Measure(s"static.gen.thirdQ", thirdQ), time=0, host=host)
          xs1.sortBy(_.time).takeRight(5).foreach(x =>
            system.eventStream publish StatMsg(Measure("static.gen", x.value), time=x.time, host=host)
          )
        }
        kvs.all(fid(fid.ReindexAll(host))).map_{ xs =>
          val xs1 = xs.collect{ case Right(a) => extract(a) }
          val thirdQ = xs1.sortBy(_.value.toInt).apply((xs1.length*0.7).toInt).value
          system.eventStream publish StatMsg(Measure(s"reindex.all.thirdQ", thirdQ), time=0, host=host)
          xs1.sortBy(_.time).takeRight(5).foreach(x =>
            system.eventStream publish StatMsg(Measure("reindex.all", x.value), time=x.time, host=host)
          )
        }
        kvs.all(fid(fid.StaticGenYear(host))).map_(_.collect{ case Right(a) => extract(a) }.sortBy(_.time).dropWhile(_.time.toLocalDataTime().isBefore(year_ago())).foreach{
          case EnData(value, time, host) => system.eventStream publish StatMsg(Measure("static.gen.year", value), time=time, host=host)
        })
        kvs.all(fid(fid.KvsSizeYear(host))).map_(_.collect{ case Right(a) => extract(a) }.sortBy(_.time).dropWhile(_.time.toLocalDataTime().isBefore(year_ago())).foreach{
          case EnData(value, time, host) => system.eventStream publish StatMsg(Metric("kvs.size.year", value), time=time, host=host)
        })
        kvs.all(fid(fid.ActionLive(host))).map_(_.collect{ case Right(a) => extract(a) }.sortBy(_.time).dropWhile(_.time < live_start).foreach{
          case EnData(action, time, host) => system.eventStream publish StatMsg(Action(action), time=time, host=host)
        })
        kvs.all(fid(fid.Metrics(host))).map_(_.collect{ case Right(a) => extract(a) }.foreach{
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
      val udppub = Source.fromGraph(new MsgSource(system.actorOf(UdpPub.props)))
      val logIn = Flow[Push].map{ msg => system.log.debug("UDP: {}", msg); msg }
      val save_host = Flow[Push].collect{
        case msg: HostMsg =>
          import msg.{host, ipaddr, time}
          kvs.put(EnKey(fid(fid.Nodes()), en_id.str(host)), insert(EnData(value=ipaddr, time=time, host=host)))
      }
      val save_metric = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem"|"kvs.size"|"feature", _), _, _) =>
        case StatMsg(Metric(name, value), time, host) =>
          kvs.put(EnKey(fid(fid.Metrics(host)), en_id.str(name)), insert(EnData(value=s"${name}|${value}", time=time, host=host)))
      }
      val save_cpumem = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem", value), time, host) =>
          { // live
            val i = kvs.el.get(el_id(el_id.CpuMemLiveIdx(host))).toOption.flatten.map(el_v.int).getOrElse(0)
            for {
              _ <- kvs.put(EnKey(fid(fid.CpuMemLive(host)), en_id.int(i)), insert(EnData(value=value, time=time, host=host)))
              i1 = (i + 1) % 20
              _ <- kvs.el.put(el_id(el_id.CpuMemLiveIdx(host)), el_v.int(i1))
            } yield ()
          }
          value.split('~') match {
            case Array(cpu, _, _, _) =>
              { // hour
                val i = (((time / 1000 / 60) % 60) / 3).toInt // [0, 19]
                val now = System.currentTimeMillis
                val last = kvs.el.get(el_id(el_id.CpuHourT(host, i))).toOption.flatten.map(el_v.long).getOrElse(now)
                val n =
                  if (now - last >= 3*60*1000) 0
                  else kvs.el.get(el_id(el_id.CpuHourN(host, i))).toOption.flatten.map(el_v.int).getOrElse(0)
                val v = kvs.el.get(el_id(el_id.CpuHourV(host, i))).toOption.flatten.map(el_v.float).getOrElse(0f)
                val n1 = n + 1
                val v1 = (v * n + cpu.toInt) / n1
                kvs.el.put(el_id(el_id.CpuHourT(host, i)), el_v.long(time))
                kvs.el.put(el_id(el_id.CpuHourN(host, i)), el_v.int(n1))
                kvs.el.put(el_id(el_id.CpuHourV(host, i)), el_v.float(v1))
                val time1 = ((time / 1000 / 60 / 60 * 60) + i * 3) * 60 * 1000
                kvs.put(EnKey(fid(fid.CpuHour(host)), en_id.int(i)), insert(EnData(value=v1.toInt.toString, time=time1, host=host)))
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
          val i = kvs.el.get(el_id(el_id.MeasureLatestIdx(name=name, host=host))).toOption.flatten.map(el_v.int).getOrElse(0)
          for {
            _ <- kvs.put(EnKey(fid(fid.MeasureLatest(name=name, host=host)), en_id.int(i)), insert(EnData(value=value, time=time, host=host)))
            i1 = (i + 1) % limit
            _ <- kvs.el.put(el_id(el_id.MeasureLatestIdx(name=name, host=host)), el_v.int(i1))
          } yield ()
          // calculate new quartile
          kvs.all(fid(fid.MeasureLatest(name=name, host=host))).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => extract(a)}.toVector.sortBy(_.value.toInt)
            val thirdQ = xs1((xs1.length*0.7).toInt).value
            val msg = StatMsg(Measure(s"${name}.thirdQ", thirdQ), time=0, host=host)
            system.eventStream.publish(msg)
          }
      }

      def saveYearValue(name: String, value: Long, time: Long, host: String): (Long, Long) = {
        val date = time.toLocalDataTime()
        val i = date.getMonthValue - 1
        val now = LocalDateTime.now()
        val last = kvs.el.get(el_id(el_id.MeasureYearT(name=name, host=host, i))).toOption.flatten.map(el_v.long(_).toLocalDataTime()).getOrElse(now)
        val n =
          if (date.getYear != last.getYear) 0
          else kvs.el.get(el_id(el_id.MeasureYearN(name=name, host=host, i))).toOption.flatten.map(el_v.int).getOrElse(0)
        val v = kvs.el.get(el_id(el_id.MeasureYearV(name=name, host=host, i))).toOption.flatten.map(el_v.long).getOrElse(0L)
        val n1 = n + 1
        val v1 = (v * n + value.toLong) / n1
        kvs.el.put(el_id(el_id.MeasureYearT(name=name, host=host, i)), el_v.long(time))
        kvs.el.put(el_id(el_id.MeasureYearN(name=name, host=host, i)), el_v.int(n1))
        kvs.el.put(el_id(el_id.MeasureYearV(name=name, host=host, i)), el_v.long(v1))
        val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis()
        kvs.put(EnKey(fid(fid.MeasureYear(name=name, host=host)), en_id.int(i)), insert(EnData(value=v1.toString, time=time1, host=host)))
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
          val last = kvs.el.get(el_id(el_id.FeatureT(name=name, host=host, i))).toOption.flatten.map(el_v.long(_).toLocalDataTime()).getOrElse(now)
          val n =
            if (date.getYear != last.getYear) 0
            else kvs.el.get(el_id(el_id.FeatureN(name=name, host=host, i))).toOption.flatten.map(el_v.int).getOrElse(0)
          val n1 = n + 1
          kvs.el.put(el_id(el_id.FeatureT(name=name, host=host, i)), el_v.long(time))
          kvs.el.put(el_id(el_id.FeatureN(name=name, host=host, i)), el_v.int(n1))
          val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis()
          kvs.put(EnKey(fid(fid.Feature()), en_id.name_host_i(name=name, host=host, i=i)), insert(EnData(value=s"${name}~${n1}", time=time1, host=host)))
      }
      val save_action = Flow[Push].collect{
        case StatMsg(Action(action), time, host) =>
          val i = kvs.el.get(el_id(el_id.ActionLiveIdx(host))).toOption.flatten.map(el_v.int).getOrElse(0)
          for {
            _ <- kvs.put(EnKey(fid(fid.ActionLive(host)), id=en_id.int(i)), insert(EnData(value=action, time=time, host=host)))
            i1 = (i + 1) % 20
            _ <- kvs.el.put(el_id(el_id.ActionLiveIdx(host)), el_v.int(i1))
          } yield ()
      }
      val save_error = Flow[Push].collect{
        case StatMsg(Error(exception, stacktrace, toptrace), time, host) =>
          val i = kvs.el.get(el_id(el_id.ErrorsIdx(host))).toOption.flatten.map(el_v.int).getOrElse(0)
          for {
            _ <- kvs.put(EnKey(fid(fid.Errors(host)), en_id.int(i)), insert(EnData(value=s"${exception}|${stacktrace}|${toptrace}", time=time, host=host)))
            i1 = (i + 1) % 100
            _ <- kvs.el.put(el_id(el_id.ErrorsIdx(host)), el_v.int(i1))
          } yield ()
      }
      def pub = Sink.foreach[Push]{ case msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }
      val b1 = b.add(Broadcast[Push](9))

      udppub ~> logIn ~> b1 ~> pub
                         b1 ~> save_host       ~> Sink.ignore
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
