package .stats

import akka.actor.{ActorRef,Props}
import akka.http.scaladsl.model.ws.{UpgradeToWebsocket,Message=>WsMessage,TextMessage}
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.{Flow,Source,Sink,FlowGraph}

object Flows {
  def stats(router:ActorRef, kvs:Kvs): Flow[WsMessage, WsMessage, Unit] = {
    Flow() { implicit b =>
      import FlowGraph.Implicits._

      val collect   = b.add(Flow[WsMessage].collect[String]{case TextMessage.Strict(t) => t})
      val last      = b.add(Sink.actorSubscriber(LastMetric.props(kvs, router)))
      val stat      = b.add(Source.actorPublisher(LastMetric.props(kvs, router)))
      val toMsg     = b.add(Flow[String].map[TextMessage](TextMessage.Strict))
      val log1      = b.add(Flow[String].map[String]{x => println(s"> $x");x})
      val log2      = b.add(Flow[String].map[String]{x => println(s"< $x");x})

      // connect the graph
      collect ~> log1 ~> last
      stat    ~> log2 ~> toMsg

      (collect.inlet, toMsg.outlet)
  }}
}
