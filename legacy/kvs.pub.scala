package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import kvs.Kvs
import zero.ext._, either._

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    /* nodes */
    // kvs.all(fid(fid.Nodes())).map_(_.collect{ case Right((_, a)) => a }.foreach{ en =>
    //   import en.{value, time, host}
    //   self ! HostMsg(host=host, ipaddr=value, time=time)
    // })
    /* features */
    // kvs.all(fid(fid.Feature())).map_(_.collect{ case Right((k, a)) => en_id.feature(k) -> a }.groupBy(_._2.host).foreach{ case (host, xs) =>
    //   val xs1 = xs.toVector.sortBy(_._2.time)
    //   xs1.dropWhile(_._2.time.toLocalDataTime().isBefore(year_ago())).foreach{ case (key, EnData(value, time, host)) =>
    //     self ! StatMsg(Metric("feature", s"${key.name}~$value"), time=time, host=host)
    //   }
    // })
    /* common errors */
    kvs.all(fid(fid.CommonErrors())).map_(_.collect{ case Right((k, a)) => en_id.str(k) -> a }.foreach{ case (st, en) =>
      import en.{value, time, host}
      self ! StatMsg(Error(exception=value, stacktrace=st), time=time, host=host)
    })
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
