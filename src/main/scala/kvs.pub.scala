package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import zd.kvs.Kvs
import zero.ext._, either._

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    /* nodes */
    kvs.all(fid(fid.Nodes())).map_(_.collect{ case Right(a) => extract(a) }.foreach{ en =>
      import en.{value, time, host}
      self ! HostMsg(host=host, ipaddr=value, time=time)
    })
    /* features */
    kvs.all(fid(fid.Feature())).map_(_.collect{ case Right(a) => en_id.feature(a.key.id) -> extract(a)}.groupBy(_._2.host).foreach{ case (host, xs) =>
      val xs1 = xs.toVector.sortBy(_._2.time)
      xs1.dropWhile(_._2.time.toLocalDataTime().isBefore(year_ago())).foreach{ case (key, EnData(value, time, host)) =>
        self ! StatMsg(Metric("feature", s"${key.name}~$value"), time=time, host=host)
      }
    })
    //todo
    // // get unique errrors
    // kvs.all(fid(fid.AllErrors())).map_(_.foldLeft(Map.empty[String, StatMsg]){
    //   case (acc, Right(a)) => a.data.split('|') match {
    //     case Array(exception, stacktrace) if !(acc contains stacktrace) =>
    //       acc + (stacktrace -> StatMsg(Error(exception, stacktrace), StatMeta(a.time, a.host, a.ip)))
    //     case _ => acc
    //   }
    //   case (acc, _) => acc
    // }.values.to(Vector).sortBy(_.meta.time).takeRight(20).foreach(self ! _))
  }

  def receive: Receive = {
    case _: Push => stash()
    case stageActor: ActorRef =>
      unstashAll()
      context.become(ready(stageActor))
  }

  def ready(stageActor: ActorRef): Receive = {
    case msg: Push => stageActor ! msg
  }
}
