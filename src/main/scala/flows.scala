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
        case (MetricStat(name, value), StatMeta(time, sys, addr)) =>
          s"metric::${name}::${value}::${time}::${sys}::${addr}"
        case (ErrorStat(className, message, stacktrace), StatMeta(time, sys, addr)) =>
          s"error::${className}::${message}::${stacktrace}::${time}::${sys}::${addr}"
        case (ActionStat(user, action), StatMeta(time, sys, addr)) =>
          s"action::${user}::${action}::${time}::${sys}::${addr}"
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
          case "metric" :: sys :: addr :: name :: value :: Nil =>
            MetricStat(name, value) -> StatMeta(now_ms(), sys, addr)
          case "error" :: sys :: addr :: className :: message :: stacktrace :: Nil =>
            ErrorStat(className, message, stacktrace) -> StatMeta(now_ms(), sys, addr)
          case "action" :: sys :: addr :: user :: action :: Nil =>
            ActionStat(user, action) -> StatMeta(now_ms(), sys, addr)
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
