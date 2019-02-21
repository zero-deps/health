package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import .kvs.Kvs
import scalaz._

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    kvs.stream_safe[StatEn]("cpu_mem.live").map(
      _.take(20).collect{ case \/-(r) => r }.sortBy(_.time).foreach{
        case StatEn(_,_,_,stat,time,addr) =>
          stat.split('|') match {
            case Array(name, value) =>
              self ! Msg(MetricStat(name, value), StatMeta(time, addr))
            case _ =>
          }
      }
    )
    kvs.stream_safe[StatEn]("action.live").map(
      _.take(20).collect{ case \/-(r) => r }.sortBy(_.time).foreach{
        case StatEn(_,_,_,action,time,addr) =>
          self ! Msg(ActionStat(action), StatMeta(time, addr))
      }
    )
    kvs.stream_safe[StatEn]("metrics").map(
      _.take(500).collect{ case \/-(r) => r }.foreach{
        case StatEn(_,_,_,stat,time,addr) =>
          stat.split('|') match {
            case Array(name, value) =>
              self ! Msg(MetricStat(name, value), StatMeta(time, addr))
            case _ =>
          }
      }
    )
    kvs.stream_safe[StatEn]("errors").map(
      _.take(100).collect{ case \/-(r) => r }.foreach{
        case StatEn(_,_,_,stat,time,addr) =>
          stat.split('|') match {
            case Array(exception, stacktrace, toptrace) =>
              self ! Msg(ErrorStat(exception, stacktrace, toptrace), StatMeta(time, addr))
            case _ =>
          }
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
    case msg: Msg => stageActor ! msg
  }
}
