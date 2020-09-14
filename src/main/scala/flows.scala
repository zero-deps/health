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
      val logIn = Flow[Pull].map{ msg => system.log.debug(s"IN: ${msg}"); msg }
      val logOut = Flow[Push].map{ msg => system.log.debug(s"OUT: ${msg}"); msg }
      val kvspub = Source.fromGraph(new MsgSource(system.actorOf(KvsPub.props(kvs))))
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
        val live_start = kvs.all[StatEn](keys.`cpu_mem.live`).toOption.flatMap(_.collect{ case Right(a) => a }.filter(_.host == host).sortBy(_.time).map{
          case a@StatEn(_,_,_,value,time,host,ip) =>
            system.eventStream publish StatMsg(Metric("cpu_mem", value), StatMeta(time, host, ip)); a
        }.force.headOption.map(_.time)).getOrElse("0");
        {
          kvs.all[StatEn](keys.`cpu.hour`).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => a }.filter(_.host == host).sortBy(_.time)
            val min = xs1.lastOption.map(_.time.toLong).getOrElse(0L)-60*60*1000 //todo
            xs1.dropWhile(_.time.toLong < min).foreach{ case StatEn(_,_,_,value,time,host,ip) =>
              system.eventStream publish StatMsg(Metric("cpu.hour", value), StatMeta(time, host, ip))
            }
          }
        }
        List((keys.`search.ts.latest`, 5)
          , (keys.`search.wc.latest`, 5)
          , (keys.`search.fs.latest`, 5)
          , (keys.`static.gen.latest`, 5)
          , (keys.`reindex.all.latest`, 5)
          ).foreach{ case (key, n) =>
          val name = key.stripPrefix(".latest")
          kvs.all[StatEn](key).map_{ xs =>
            val xs1 = xs.collect{ case Right(a) => a }.filter(_.host == host)
            val thirdQ = xs1.sortBy(_.data).apply((xs1.length*0.7).toInt).data
            system.eventStream publish StatMsg(Measure(s"$name.thirdQ", thirdQ), StatMeta(time="0", host, ""))
            xs1.sortBy(_.time).takeRight(n).foreach(x =>
              system.eventStream publish StatMsg(Measure(name, x.data), StatMeta(x.time, host, ""))
            )
          }
        }
        kvs.all[StatEn](keys.`static.gen.year`).map_(_.collect{ case Right(a) => a }.filter(_.host == host).sortBy(_.time).dropWhile(_.time.toLong.toLocalDataTime.isBefore(year_ago())).foreach{
          case StatEn(_,_,_, value, time, host, ip) => system.eventStream publish StatMsg(Measure(keys.`static.gen.year`, value), StatMeta(time, host, ip))
        })
        kvs.all[StatEn](keys.`kvs.size.year`).map_(_.collect{ case Right(a) => a }.filter(_.host == host).sortBy(_.time).dropWhile(_.time.toLong.toLocalDataTime.isBefore(year_ago())).foreach{
          case StatEn(_,_,_, value, time, host, ip) => system.eventStream publish StatMsg(Metric(keys.`kvs.size.year`, value), StatMeta(time, host, ip))
        })
        kvs.all[StatEn](keys.`action.live`).map_(_.collect{ case Right(a) => a }.filter(_.host == host).sortBy(_.time).dropWhile(_.time < live_start).foreach{
          case StatEn(_,_,_, action, time, host, ip) => system.eventStream publish StatMsg(Action(action), StatMeta(time, host, ip))
        })
        kvs.all[StatEn](keys.`metrics`).map_(_.collect{ case Right(a) => a }.filter(_.host == host).foreach{
          case StatEn(_,_,_, stat, time, host, ip) => stat.split('|') match {
            case Array(name, value) =>
              system.eventStream publish StatMsg(Metric(name, value), StatMeta(time, host, ip))
            case _ =>
          }
        })
    }

  def udp(system: ActorSystem, kvs: Kvs): RunnableGraph[NotUsed] = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      import keys._
      import zd.kvs.empty

      val udppub = Source.fromGraph(new MsgSource(system.actorOf(UdpPub.props)))
      val logIn = Flow[Push].map{ msg => system.log.debug("UDP: {}", msg); msg }
      val save_metric = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem"|"kvs.size"|"feature", _), _) =>
        case StatMsg(Metric(name, value), StatMeta(time, host, ip)) =>
          kvs.put(StatEn(fid=`metrics`, id=s"${host}${name}", prev=empty, s"${name}|${value}", time, host, ip))
      }
      val save_cpumem = Flow[Push].collect{
        case StatMsg(Metric("cpu_mem", value), StatMeta(time, host, ip)) =>
          { // live
            val i = kvs.el.get[String](s"cpu_mem.live.idx.${host}").toOption.flatten.getOrElse("0")
            for {
              _ <- kvs.put(StatEn(fid=`cpu_mem.live`, id=s"${host}${i}", prev=empty, value, time, host, ip))
              i1 = ((i.toInt + 1) % 20).toString
              _ <- kvs.el.put(s"cpu_mem.live.idx.${host}", i1)
            } yield ()
          }
          value.split('~') match {
            case Array(cpu, _, _, _) =>
              { // hour
                val i = ((time.toLong / 1000 / 60) % 60) / 3 // [0, 19]
                val now = System.currentTimeMillis
                val last = kvs.el.get[String](s"cpu.hour.t.${host}${i}").toOption.flatten.map(_.toLong).getOrElse(now)
                val n =
                  if (now - last >= 3*60*1000) 0
                  else kvs.el.get[String](s"cpu.hour.n.${host}${i}").toOption.flatten.map(_.toInt).getOrElse(0)
                val v = kvs.el.get[String](s"cpu.hour.v.${host}${i}").toOption.flatten.map(_.toFloat).getOrElse(0f)
                val n1 = n + 1
                val v1 = (v * n + cpu.toInt) / n1
                kvs.el.put(s"cpu.hour.t.${host}${i}", time)
                kvs.el.put(s"cpu.hour.n.${host}${i}", n1.toString)
                kvs.el.put(s"cpu.hour.v.${host}${i}", v1.toString)
                val time1 = (((time.toLong / 1000 / 60 / 60 * 60) + i * 3) * 60 * 1000).toString
                kvs.put(StatEn(fid=`cpu.hour`, id=s"$host$i", prev=empty, v1.toInt.toString, time1, host, ip))
                system.eventStream.publish(StatMsg(Metric("cpu.hour", v1.toInt.toString), StatMeta(time1, host, ip)))
              }
            case _ =>
          }
      }
      val save_measure = Flow[Push].collect{
        case StatMsg(Measure(name, value), StatMeta(time, host, ip)) =>
          val limit = name match {
            case "reindex.all" => 100
            case _ => 20
          }
          val i = kvs.el.get[String](s"${name}.latest.idx.${host}").toOption.flatten.getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid=s"${name}.latest", id=s"${host}${i}", prev=empty, value, time, host, ip))
            i1 = ((i.toInt + 1) % limit).toString
            _ <- kvs.el.put(s"${name}.latest.idx.${host}", i1)
          } yield ()
          // calculate new quartile
          kvs.all[StatEn](s"${name}.latest").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map{ xs =>
            val xs1 = xs.filter(_.host == host).toVector.sortBy(_.data)
            val thirdQ = xs1((xs1.length*0.7).toInt).data
            val msg = StatMsg(Measure(s"${name}.thirdQ", thirdQ), StatMeta(time="0", host, ip))
            system.eventStream.publish(msg)
          }
      }

      def saveYearValue(name: String, value: Long, time: String, host: String, ip: String): (Long, String) = {
        val date = time.toLong.toLocalDataTime
        val i = date.getMonthValue - 1
        val now = LocalDateTime.now()
        val last = kvs.el.get[String](s"${name}.year.t.${host}${i}").toOption.flatten.map(_.toLong.toLocalDataTime).getOrElse(now)
        val n =
          if (date.getYear != last.getYear) 0
          else kvs.el.get[String](s"${name}.year.n.${host}${i}").toOption.flatten.map(_.toInt).getOrElse(0)
        val v = kvs.el.get[String](s"${name}.year.v.${host}${i}").toOption.flatten.map(_.toLong).getOrElse(0L)
        val n1 = n + 1
        val v1 = (v * n + value.toLong) / n1
        kvs.el.put(s"${name}.year.t.${host}${i}", time)
        kvs.el.put(s"${name}.year.n.${host}${i}", n1.toString)
        kvs.el.put(s"${name}.year.v.${host}${i}", v1.toString)
        val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis.toString
        kvs.put(StatEn(fid=s"${name}.year", id=s"${host}${i}", prev=empty, v1.toString, time1, host, ip))
        (v1, time1)
      }
      val save_year_value = Flow[Push].collect{
        case StatMsg(Measure(name@("static.gen"), value), StatMeta(time, host, ip)) =>
          val (v1, t1) = saveYearValue(name, value.toLong, time, host, ip)
          system.eventStream.publish(StatMsg(Measure(s"$name.year", v1.toString), StatMeta(t1, host, ip)))
        case StatMsg(Metric(name@"kvs.size", value), StatMeta(time, host, ip)) =>
          val (v1, t1) = saveYearValue(name, value.toLong/1024, time, host, ip)
          system.eventStream.publish(StatMsg(Metric(s"$name.year", v1.toString), StatMeta(t1, host, ip)))
      }
      val save_feature = Flow[Push].collect{
        case StatMsg(Metric("feature", name), StatMeta(time, host, ip)) =>
          val date = time.toLong.toLocalDataTime
          val i = date.getMonthValue - 1
          val now = LocalDateTime.now()
          val last = kvs.el.get[String](s"feature.${name}.t.${host}${i}").toOption.flatten.map(_.toLong.toLocalDataTime).getOrElse(now)
          val n =
            if (date.getYear != last.getYear) 0
            else kvs.el.get[String](s"feature.${name}.n.${host}${i}").toOption.flatten.map(_.toInt).getOrElse(0)
          val n1 = n + 1
          kvs.el.put(s"feature.${name}.t.${host}${i}", time)
          kvs.el.put(s"feature.${name}.n.${host}${i}", n1.toString)
          val time1 = LocalDateTime.of(date.getYear, date.getMonthValue, 1, 12, 0).toMillis.toString
          kvs.put(StatEn(fid=`feature`, id=s"${host}${i}${name}", prev=empty, s"${name}~${n1}", time1, host, ip))
      }
      val save_action = Flow[Push].collect{
        case StatMsg(Action(action), StatMeta(time, host, ip)) =>
          val i = kvs.el.get[String](s"action.live.idx.${host}").toOption.flatten.getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid=`action.live`, id=s"${host}${i}", prev=empty, action, time, host, ip))
            i1 = ((i.toInt + 1) % 20).toString
            _ <- kvs.el.put(s"action.live.idx.${host}", i1)
          } yield ()
      }
      val save_error = Flow[Push].collect{
        case StatMsg(Error(exception, stacktrace, toptrace), StatMeta(time, host, ip)) =>
          val i = kvs.el.get[String](s"errors.idx.${host}").toOption.flatten.getOrElse("0")
          for {
            _ <- kvs.put(StatEn(fid=`errors`, id=s"${host}${i}", prev=empty, s"${exception}|${stacktrace}|${toptrace}", time, host, ip))
            i1 = ((i.toInt + 1) % 100).toString
            _ <- kvs.el.put(s"errors.idx.${host}", i1)
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
