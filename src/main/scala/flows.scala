package .stats

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model.ws.{ UpgradeToWebsocket, Message => WsMessage, TextMessage }
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.HttpRequest
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, Source, Sink }
import .kvs.Kvs
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.RunnableGraph
import akka.stream.ClosedShape
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import .stats.actors.DataSource
import .kvs.handle._
import .stats.actors.DataSource.SourceMsg
import scala.util.{ Success, Try, Failure }

object Flows {
  import handlers._

  def logIn[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"IN: $x"); x }
  def logOut[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"OUT: $x"); x }

  def stats(implicit system:ActorSystem,kvs:Kvs): Flow[WsMessage, WsMessage, Unit] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val collect = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })

      val updated = Source.actorPublisher(DataSource.props(kvs))
      val toMsg = b.add(Flow[Data] map { case data: Data => handler.socketMsg(data) } collect { case Success(x) => x })

      val toWsMsg = b.add(Flow[String].map[TextMessage] { TextMessage.Strict })

      collect ~> logIn[String] ~> Sink.ignore
      updated ~> toMsg ~> logOut[String] ~> toWsMsg

      FlowShape(collect.in, toWsMsg.out)
    })

  def saveDataFromUdp(implicit system:ActorSystem,kvs:Kvs) =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val udpPublisher = Source.actorPublisher(UdpListener.props)
      val saveToKvs = Flow[Data] map { data =>
        handler.saveToKvs(kvs)(data)
        data
      }
      val publishEvent = Sink.foreach[Data] { data =>
        system.eventStream.publish(SourceMsg(data))
      }

      udpPublisher ~> logIn[Data] ~> saveToKvs ~> publishEvent

      ClosedShape
    })
}
