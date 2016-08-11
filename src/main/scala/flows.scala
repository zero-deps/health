package .stats

import akka.NotUsed
import akka.http.scaladsl.model.ws.{TextMessage, Message => WsMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, Sink, Source}
import .kvs.Kvs
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.RunnableGraph
import akka.stream.ClosedShape
import akka.actor.ActorSystem
import .stats.actors.DataSource
import .kvs.handle._
import .stats.actors.DataSource.SourceMsg

import scala.util.Success

object Flows {
  import handlers._

  def logIn[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"IN: $x"); x }
  def logOut[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"OUT: $x"); x }

  def stats(implicit system:ActorSystem,kvs:Kvs): Flow[WsMessage, WsMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val collect = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })

      val pub = Source.actorPublisher(DataSource.props(kvs))
      val toMsg = b.add(Flow[Data] map { case data: Data => handler.socketMsg(data) } collect { case Success(x) => x })

      val toWsMsg = b.add(Flow[String].map[TextMessage] { TextMessage.Strict })

      collect ~> logIn[String] ~> Sink.ignore
      pub ~> toMsg ~> logOut[String] ~> toWsMsg

      FlowShape(collect.in, toWsMsg.out)
    })

  def saveDataFromUdp(implicit system:ActorSystem,kvs:Kvs) =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val udpPublisher = Source.actorPublisher(UdpListener.props)
      val convertUdpMsg = Flow[String] map { msg => handler.udpMessage(msg) } collect { case Success(x) => x }

      val saveToKvs = Flow[Data] map { data =>
        handler.saveToKvs(kvs)(data)
        data
      }
      val publishEvent = Sink.foreach[Data] { data =>
        system.eventStream.publish(SourceMsg(data))
      }

      udpPublisher ~> logIn ~> convertUdpMsg ~> saveToKvs ~> publishEvent

      ClosedShape
    })
}
