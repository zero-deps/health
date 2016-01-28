package .stats

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model.ws.{ UpgradeToWebsocket, Message => WsMessage, TextMessage }
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.HttpRequest
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, Source, Sink }
import .kvs.Kvs
import akka.stream.scaladsl.GraphDSL

object Flows {
  def stats(router: ActorRef, kvs: Kvs): Flow[WsMessage, WsMessage, Unit] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      println(s"in da flow $kvs")
      val collect = b.add(Flow[WsMessage].collect[String] { case TextMessage.Strict(t) => t })
      val last = b.add(Sink.actorSubscriber(LastMetric.props(kvs, router)))
      val stat = b.add(Source.actorPublisher(LastMetric.props(kvs, router)))
      val toMsg = b.add(Flow[String].map[TextMessage](TextMessage.Strict))
      val logIn = b.add(Flow[String].map[String] { x => println(s"> $x"); x })
      val logOut = b.add(Flow[String].map[String] { x => println(s"< $x"); x })

      // connect the graph
      collect ~> logIn ~> last
      stat ~> logOut ~> toMsg

      FlowShape(collect.in, toMsg.out)
    })
}
