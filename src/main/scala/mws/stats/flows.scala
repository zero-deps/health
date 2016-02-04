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
import .kvs.handle.`package`.En
import .stats.actors.DataSource.SourceMsg

case class Flows(kvs: Kvs)(implicit system: ActorSystem) {
  def logIn[T] = Flow[T].map[T] { x => println(s"> $x"); x }
  def logOut[T] = Flow[T].map[T] { x => println(s"< $x"); x }

  private val udpPublisher = Source.actorPublisher(UdpListener.props)
  private val updated = Source.actorPublisher(DataSource.props(kvs))

  def stats: Flow[WsMessage, WsMessage, Unit] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      println(s"in da flow $kvs")
      val collect = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })

      val toMsg = b.add(Flow[Data].map[TextMessage] { x =>
        val wsMsg = x match {
          case message: Message => s"msg::${message.casino}::${message.user}::${message.time.toMillis}::${message.msg}"
          case metric: Metric => s"metric::${metric.name}::${metric.node}::${metric.param}::${metric.time}::${metric.value}"
        }
        TextMessage.Strict(wsMsg)
      })

      // connect the graph
      collect ~> logIn[String] ~> Sink.ignore
      updated ~> logOut[Data] ~> toMsg

      FlowShape(collect.in, toMsg.out)
    })

  def saveDataFromUdp = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val saveToKvs = b.add(Flow[Data].map[Data] { data =>
      kvs.add[En[String]](data) match {
        case Right(en) =>
          println(s"added $en")
          data
        case Left(error) =>
          println(s"Error:$error")
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
