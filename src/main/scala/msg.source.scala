package .stats

import akka.actor.ActorRef
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.collection.immutable.Queue

class MsgSource(sourceFeeder: ActorRef) extends GraphStage[SourceShape[Push]] {
  val out: Outlet[Push] = Outlet("MessageSource")
  override val shape: SourceShape[Push] = SourceShape(out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) with StageLogging {
      var messages: Queue[Push] = Queue()
      lazy val self: StageActor = getStageActor({
        case (_, msg: Push) =>
          log.debug("received msg, queueing: {} ", msg)
          messages = messages.enqueue(msg)
          pump()
        case _ =>
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          log.debug("onPull() called...")
          pump()
        }
      })
      private def pump(): Unit = {
        if (isAvailable(out) && messages.nonEmpty) {
          log.debug("ready to dequeue")
          messages.dequeue match {
            case (msg: Push, newQueue: Queue[Push]) =>
              log.debug("got message from queue, pushing: {} ", msg)
              push(out, msg)
              messages = newQueue
          }
        }
      }
      override def preStart(): Unit = {
        log.debug("pre-starting stage, assigning StageActor to sourceFeeder")
        sourceFeeder ! self.ref
      }
      private def onMessage(x: (ActorRef, Any)): Unit = {
        x match {
          case (_, msg: Push) =>
            log.debug("received msg, queueing: {} ", msg)
            messages = messages.enqueue(msg)
            pump()
          case _ =>
        }
      }
    }
  }
}