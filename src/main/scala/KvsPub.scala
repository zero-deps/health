package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import zd.kvs.Kvs
import java.time.LocalDateTime

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    kvs.all[StatEn]("cpu_mem.live").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,value,time,host,ip) =>
        self ! StatMsg(Metric("cpu_mem", value), StatMeta(time, host, ip))
    })
    kvs.all[StatEn]("cpu.hour").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.groupBy(_.host).foreach{ case (host, xs) =>
      val xs1 = xs.toVector.sortBy(_.time.toLong)
      val min = xs1.last.time.toLong-60*60*1000
      xs1.dropWhile(_.time.toLong < min).foreach{ case StatEn(_,_,_,value,time,host,ip) =>
        self ! StatMsg(Metric("cpu.hour", value), StatMeta(time, host, ip))
      }
    })
    List(("search.ts", 5)
       , ("search.wc", 5)
       , ("static.gen", 5)
       , ("reindex.ts", 100)
       , ("reindex.wc", 100)
       , ("reindex.files", 100)
       )
    .foreach{ case (name, n) =>
      kvs.all[StatEn](s"${name}.latest").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.groupBy(_.host).foreach{ case (host, xs) =>
        val xs1 = xs.toVector
        val thirdQ = xs1.sortBy(_.data).apply((xs1.length*0.7).toInt).data
        self ! StatMsg(Measure(s"${name}.thirdQ", thirdQ), StatMeta(time="0", host, ""))
        xs1.sortBy(_.time).takeRight(n).foreach(x =>
          self ! StatMsg(Measure(s"${name}", x.data), StatMeta(x.time, host, ""))
        )
      })
    }
    List("static.gen.year").foreach(name =>
      kvs.all[StatEn](name).map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.groupBy(_.host).foreach{ case (host, xs) =>
        val xs1 = xs.toVector.sortBy(_.time.toLong)
        val min = LocalDateTime.now().minusYears(1)
        xs1.dropWhile(_.time.toLong.toLocalDataTime.isBefore(min)).foreach{ case StatEn(_,_,_,value, time, host, ip) =>
          self ! StatMsg(Measure(name, value), StatMeta(time, host, ip))
        }
    }))
    List("feature", "kvs.size.year").foreach(name =>
      kvs.all[StatEn](name).map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.groupBy(_.host).foreach{ case (host, xs) =>
        val xs1 = xs.toVector.sortBy(_.time.toLong)
        val min = LocalDateTime.now().minusYears(1)
        xs1.dropWhile(_.time.toLong.toLocalDataTime.isBefore(min)).foreach{ case StatEn(_,_,_,value, time, host, ip) =>
          self ! StatMsg(Metric(name, value), StatMeta(time, host, ip))
        }
      }))
    kvs.all[StatEn]("action.live").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,action,time,host,ip) =>
        self ! StatMsg(Action(action), StatMeta(time, host, ip))
    })
    kvs.all[StatEn]("metrics").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.foreach{
      case StatEn(_,_,_,stat,time,host,ip) =>
        stat.split('|') match {
          case Array(name, value) =>
            self ! StatMsg(Metric(name, value), StatMeta(time, host, ip))
          case _ =>
        }
    })
    kvs.all[StatEn]("errors").map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,stat,time,host,ip) =>
        stat.split('|') match {
          case Array(exception, stacktrace, toptrace) =>
            self ! StatMsg(Error(exception, stacktrace, toptrace), StatMeta(time, host, ip))
          case _ =>
        }
    })
  }

  def receive: Receive = {
    case _: StatMsg => stash()
    case stageActor: ActorRef =>
      unstashAll()
      context.become(ready(stageActor))
  }

  def ready(stageActor: ActorRef): Receive = {
    case msg: StatMsg => stageActor ! msg
  }
}
