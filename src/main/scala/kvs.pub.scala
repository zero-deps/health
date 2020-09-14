package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import zd.kvs.Kvs
import zero.ext._, either._

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    // find all nodes
    kvs.all[StatEn](keys.`cpu_mem.live`).map_(_.foldLeft(Map.empty[String, StatMeta]){
      case (acc, Right(a)) => acc.get(a.host) match {
        case Some(b) if b.time >= a.time => acc
        case _ => acc + (a.host -> StatMeta(a.time, a.host, a.ip))
      }
      case (acc, _) => acc
    }.values.foreach(self ! StatMsg(Metric("", ""), _)))
    // features
    kvs.all[StatEn](keys.`feature`).map(_.takeWhile(_.isRight).flatMap(_.toOption)).map(_.groupBy(_.host).foreach{ case (host, xs) =>
      val xs1 = xs.toVector.sortBy(_.time.toLong)
      xs1.dropWhile(_.time.toLong.toLocalDataTime.isBefore(year_ago())).foreach{ case StatEn(_,_,_, value, time, host, ip) =>
        self ! StatMsg(Metric(keys.`feature`, value), StatMeta(time, host, ip))
      }
    })
    // get unique errrors
    kvs.all[StatEn](keys.`errors`).map_(_.foldLeft(Map.empty[String, StatMsg]){
      case (acc, Right(a)) => a.data.split('|') match {
        case Array(exception, stacktrace, toptrace) if !(acc contains stacktrace) =>
          acc + (stacktrace -> StatMsg(Error(exception, stacktrace, toptrace), StatMeta(a.time, a.host, a.ip)))
        case _ => acc
      }
      case (acc, _) => acc
    }.values.to(Vector).sortBy(_.meta.time).takeRight(20).foreach(self ! _))
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
