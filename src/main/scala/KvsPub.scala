package .stats

import akka.actor.{ActorLogging, Actor, ActorRef, Props, Stash}
import .kvs.Kvs

object KvsPub {
  def props(kvs: Kvs): Props = Props(new KvsPub(kvs))
}

class KvsPub(kvs: Kvs) extends Actor with Stash with ActorLogging {
  override def preStart(): Unit = {
    // get data from kvs
    val xs: List[Msg] = Nil
    xs foreach (self ! _)
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
