package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import .kvs.Kvs

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    kvs.stream_unsafe[StatEn]("cpu_mem.live").map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,stat,time,addr) =>
        stat.split('|') match {
          case Array(name, value) =>
            self ! Msg(MetricStat(name, value), StatMeta(time, addr))
          case _ =>
        }
    })
    List("search.ts", "search.wc", "static.create", "static.gen").foreach(name =>
      kvs.stream_unsafe[StatEn](s"${name}.latest").map(_.groupBy(_.addr).foreach{ case (addr, xs) =>
        val xs1 = xs.toVector
        val thirdQ = xs1.sortBy(_.data).apply((xs1.length*0.7).toInt).data
        self ! Msg(MeasureStat(s"${name}.thirdQ", thirdQ), StatMeta(time="0", addr))
        xs1.sortBy(_.time).takeRight(5).foreach(x =>
          self ! Msg(MeasureStat(s"${name}", x.data), StatMeta(x.time, addr))
        )
      })
    )
    kvs.stream_unsafe[StatEn]("action.live").map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,action,time,addr) =>
        self ! Msg(ActionStat(action), StatMeta(time, addr))
    })
    kvs.stream_unsafe[StatEn]("metrics").map(_.foreach{
      case StatEn(_,_,_,stat,time,addr) =>
        stat.split('|') match {
          case Array(name, value) =>
            self ! Msg(MetricStat(name, value), StatMeta(time, addr))
          case _ =>
        }
    })
    kvs.stream_unsafe[StatEn]("errors").map(_.sortBy(_.time).foreach{
      case StatEn(_,_,_,stat,time,addr) =>
        stat.split('|') match {
          case Array(exception, stacktrace, toptrace) =>
            self ! Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(time, addr))
          case _ =>
        }
    })
  }

  def receive: Receive = {
    case _: Msg => stash()
    case stageActor: ActorRef =>
      log.debug("got stage actor")
      unstashAll()
      context.become(ready(stageActor))
  }

  def ready(stageActor: ActorRef): Receive = {
    case msg: Msg => stageActor ! msg
  }
}
