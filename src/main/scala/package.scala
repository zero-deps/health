package 

import akka.actor.ActorRef
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.collection.immutable.Queue
import scalaz.Scalaz._
import scala.util.{Try, Success, Failure}

package object stats {
  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class ErrorStat(exception: String, stacktrace: String, toptrace: String) extends Stat
  final case class ActionStat(action: String) extends Stat

  final case class StatMeta(time: String, addr: String)

  final case class Msg(stat: Stat, meta: StatMeta)

  def now_ms(): String = System.currentTimeMillis.toString

  final case class StatEn
    ( fid: String
    , id: String
    , prev: String
    , data: String
    , time: String
    , addr: String) extends kvs.en.En

  implicit object FdHandler extends kvs.en.FdHandler {
    def pickle(e: kvs.en.Fd): kvs.Res[Array[Byte]] = s"${e.id}^${e.top}^${e.count}".getBytes("utf8").right
    def unpickle(a: Array[Byte]): kvs.Res[kvs.en.Fd] = {
      val s = new String(a, "utf8")
      s.split('^') match {
        case Array(id, top, count) =>
          Try(count.toInt) match {
            case Success(count) => kvs.en.Fd(id, top, count).right
            case Failure(_) => kvs.UnpickleFail(s"count=${count} is not a number").left
          }
        case _ => kvs.UnpickleFail(s"bad format=${s}").left
      }
    }
  }

  implicit object StatEnHandler extends kvs.en.EnHandler[StatEn] {
    val fh: kvs.en.FdHandler = FdHandler

    def pickle(e: StatEn): kvs.Res[Array[Byte]] = s"${e.fid}^${e.id}^${e.prev}^${e.data}^${e.time}^${e.addr}".getBytes("utf8").right
    def unpickle(a: Array[Byte]): kvs.Res[StatEn] = {
      val s = new String(a, "utf8")
      s.split('^') match {
        case Array(fid, id, prev, data, time, addr) =>
          StatEn(fid, id, prev, data, time, addr).right
        case _ => kvs.UnpickleFail(s"bad format=${s}").left
      }
    }

    protected def update(en: StatEn, prev: String): StatEn = en.copy(prev=prev)
    protected def update(en: StatEn, id: String, prev: String): StatEn = en.copy(id=id, prev=prev)
  }

  class MsgSource(sourceFeeder: ActorRef) extends GraphStage[SourceShape[Msg]] {
    val out: Outlet[Msg] = Outlet("MessageSource")
    override val shape: SourceShape[Msg] = SourceShape(out)
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
      new GraphStageLogic(shape) with StageLogging {
        var messages: Queue[Msg] = Queue()
        lazy val self: StageActor = getStageActor({
          case (_, msg: Msg) =>
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
              case (msg: Msg, newQueue: Queue[Msg]) =>
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
            case (_, msg: Msg) =>
              log.debug("received msg, queueing: {} ", msg)
              messages = messages.enqueue(msg)
              pump()
            case _ =>
          }
        }
      }
    }
  }
}
