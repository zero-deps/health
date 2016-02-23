package .stats

import akka.http.scaladsl.model.ws.{ Message => WsMessage, TextMessage }
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, Source, Sink }
import .kvs.Kvs
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.RunnableGraph
import akka.stream.ClosedShape
import akka.actor.ActorSystem
import .stats.actors.DataSource
import .stats.actors.DataSource.SourceMsg

object Flows {

  def logIn[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"IN: $x"); x }
  def logOut[T](implicit system: ActorSystem) = Flow[T].map[T] { x => system.log.debug(s"OUT: $x"); x }

  def stats(implicit system:ActorSystem,kvs:Kvs): Flow[WsMessage, WsMessage, Unit] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val collect = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })

      val updated = Source.actorPublisher(DataSource.props(kvs))
      val toMsg = b.add(Flow[Data].map[String] {
        case History(casino, user, time, action) => s"msg::${casino}::${user}::${time.toMillis}::${action}"
        case Metric(name, node, param, time, value) => s"metric::${name}::${node}::${param}::${time.toMillis}::${value}"
      })

      val toWsMsg = b.add(Flow[String].map[TextMessage] { TextMessage.Strict })

      collect ~> logIn[String] ~> Sink.ignore
      updated ~> toMsg ~> logOut[String] ~> toWsMsg

      FlowShape(collect.in, toWsMsg.out)
    })

  def saveDataFromUdp(implicit system:ActorSystem,kvs:Kvs) = 
    RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      import handlers._
      import system.log

      val udpPublisher = Source.actorPublisher(UdpListener.props)
      
      val saveToKvs = b.add(Flow[Data].map[Data] { data =>
        log.debug(s"Saving data $data....")
       
        (data match {
          case data: Metric => metricHandler.saveToKvs(data)(kvs)
          case data: History => historyHandler.saveToKvs(data)(kvs)

        }) match {
          case Right(en) => data
          case Left(error) =>
            throw new Exception(error.msg)
        }
      })

      val publishEvent = Sink.foreach[Data] { data =>
        system.eventStream.publish(SourceMsg(data))
      }

      udpPublisher ~> logIn[Data] ~> saveToKvs ~> publishEvent

      ClosedShape
    })
}
