package .stats

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message => WsMessage}
import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.{Flow, Sink, Source, Merge}
import .kvs.Kvs

object Flows {
  def ws(system: ActorSystem, kvs: Kvs): Flow[WsMessage, WsMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val msgIn = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })
      val logIn = Flow[String].map{ msg => system.log.info(s"IN: ${msg}"); msg }
      val logOut = Flow[String].map{ msg => system.log.debug(s"OUT: ${msg}"); msg }
      val kvspub = Source.actorPublisher(KvsPub.props(kvs))
      val wspub = Source.actorPublisher(WsPub.props)
      val pub = b.add(Merge[Msg](2))
      val toMsg = b.add(Flow[Msg].map{ 
        case Msg(MetricStat(name, value), StatMeta(time, addr)) =>
          s"metric::${name}::${value}::${time}::${addr}"
        case Msg(ErrorStat(exception, stacktrace), StatMeta(time, addr)) =>
          s"error::${exception}::${stacktrace}::${time}::${addr}"
        case Msg(ActionStat(action), StatMeta(time, addr)) =>
          s"action::${action}::${time}::${addr}"
      })
      val msgOut = b.add(Flow[String].map[TextMessage] { TextMessage.Strict })

      msgIn ~> logIn ~> Sink.ignore // ignore income messages
      wspub  ~> pub
      kvspub ~> pub ~> toMsg ~> logOut ~> msgOut

      FlowShape(msgIn.in, msgOut.out)
    })

  def udp(system: ActorSystem, kvs: Kvs) = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val udpPub = Source.actorPublisher(UdpPub.props)
      val logIn = Flow[String].map{ msg => system.log.debug(s"UDP: ${msg}"); msg }
      val convert = Flow[String].
        map(_.split("::").toList).
        collect{
          case "metric" :: name :: value :: port :: host :: Nil =>
            Msg(MetricStat(name, value), StatMeta(now_ms(), addr=s"${host}:${port}"))
          case "error" :: exception :: stacktrace :: port :: host :: Nil =>
            Msg(ErrorStat(exception, stacktrace), StatMeta(now_ms(), addr=s"${host}:${port}"))
          case "action" :: action :: port :: host :: Nil =>
            Msg(ActionStat(action), StatMeta(now_ms(), addr=s"${host}:${port}"))
        }
      val save = Flow[Msg].map{ msg =>
        //todo: save to kvs
        msg
      }
      val pub = Sink.foreach[Msg]{ msg =>
        system.log.debug(s"pub=${msg}")
        system.eventStream.publish(msg)
      }

      udpPub ~> logIn ~> convert ~> save ~> pub

      ClosedShape
    })
  }
}
