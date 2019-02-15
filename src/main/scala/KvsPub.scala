package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import .kvs.Kvs
import scalaz._

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    kvs.stream_safe[MetricEn](fid="metrics").fold(
      l => log.error(l.toString),
      r => r.foreach{
        case -\/(l) => log.error(l.toString)
        case \/-(MetricEn(_,_,_,name,value,time,addr)) =>
          self ! Msg(MetricStat(name, value), StatMeta(time, addr))
      }
    )
  }

  def receive: Receive = {
    case _: Msg => stash()
    case stageActor: ActorRef =>
      log.debug("got stage actor")
      unstashAll()
      context.become(ready(stageActor))
  }

  def ready(stageActor: ActorRef): Receive = {
    case msg: Msg =>
      log.debug("sourceFeeder received message, forwarding to stage: {} ", msg)
      stageActor ! msg
  }
}
