package .stats

import akka.actor.{ActorRef, ActorSystem,Actor,Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.{Flow, Source, Sink, FlowGraph}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

import akka.http.scaladsl.model.ws.{UpgradeToWebsocket, TextMessage, BinaryMessage}
import akka.stream.actor.{ActorPublisher,ActorSubscriber,ActorSubscriberMessage,MaxInFlightRequestStrategy}

import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic,Router,Routee,RemoveRoutee,AddRoutee}

import scala.language.postfixOps

object VMStatsPublisher{
  def props(router:ActorRef)=Props(classOf[VMStatsPublisher],router)
}
class VMStatsPublisher(router: ActorRef) extends ActorPublisher[String]{
  case class QueueUpdated()
  import akka.stream.actor.ActorPublisherMessage._
  import scala.collection.mutable
  import context.system

  val MaxBufferSize = 50
  val queue = mutable.Queue[String]()
  var queueUpdated = false;

  override def preStart() {
    router ! AddRoutee(ActorRefRoutee(self))
    system.eventStream.subscribe(self, classOf[Metric])
    system.eventStream.subscribe(self, classOf[Message])
  }

  override def postStop(): Unit = {
    router ! RemoveRoutee(ActorRefRoutee(self))
    system.eventStream.unsubscribe(self)
  }

  def receive = {
    case m: Metric  => self ! "metric::" + m.serialize
    case m: Message => self ! "msg::" + m.serialize
    case stats: String =>
      if (queue.size == MaxBufferSize) queue.dequeue()
      queue += stats
      if (!queueUpdated) {
        queueUpdated = true
        self ! QueueUpdated
      }
    case QueueUpdated => deliver()
    case Request(amount) => deliver()
    case Cancel => context.stop(self)
  }

  import scala.annotation.tailrec

  @tailrec final def deliver(): Unit = {
    if (totalDemand == 0) {
      println(s"No more demand for: $this")
    }

    if (queue.size == 0 && totalDemand != 0) {
      // we can response to queueupdated msgs again, since
      // we can't do anything until our queue contains stuff again.
      queueUpdated = false
    } else if (totalDemand > 0 && queue.size > 0) {
      onNext(queue.dequeue())
      deliver()
    }
  }

}

class RouterActor extends Actor{
  var routees = Set[Routee]()
  def receive = {
    case ar: AddRoutee => routees = routees + ar.routee
    case rr: RemoveRoutee => routees = routees - rr.routee
    case msg => routees.foreach(_.send(msg, sender))
  }
}


object Flows {
  import akka.http.scaladsl.model.ws.Message
  def wsFlow(router:ActorRef, kvs:Kvs): Flow[Message, Message, Unit] = {
    Flow() { implicit b =>
      import FlowGraph.Implicits._

      val st = Source.actorPublisher(Props(classOf[VMStatsPublisher],router))
      val ls = Sink.actorSubscriber(LastMetric.props(kvs))

      val collect = b.add(Flow[Message].collect[String]{case TextMessage.Strict(t) => t})

      val last = b.add(ls)
      val log = b.add(Flow[String].map[String]{x => println(s"| $x");x})

      val stat = b.add(st)
      //val log2 = b.add(Flow[String].map[String]{x => println(s"< $x");x})

      val toMsg = b.add(Flow[String].map[TextMessage](TextMessage.Strict))

      // connect the graph
      collect ~> log  ~> last
      stat ~> /*log2 ~>*/ toMsg

      (collect.inlet, toMsg.outlet)
  }}
}


object StatsApp extends App {
  System.setProperty("java.library.path", System.getProperty("java.library.path") + ":native")

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()
  val c = system.settings.config

  var kvs: Option[Kvs] = Some(LeveldbKvs(c.getConfig("leveldb")))
  var udpListener: Option[ActorRef] = Some(system.actorOf(UdpListener.props, "udp-listener"))

  val lastMsg = system.actorOf(LastMessage.props(kvs.get), name = "last-msg")

  val router: ActorRef = system.actorOf(Props[RouterActor], "router")

  def handle(req: HttpRequest) = req.header[UpgradeToWebsocket].get.handleMessages(Flows.wsFlow(router, kvs.get))

  system.actorOf(MetricsListener.props)

  val bf = Http().bindAndHandle(Flow[HttpRequest].collect(Rt(system).route),
    interface="0.0.0.0", port=c.getInt("http.port"))

  sys.addShutdownHook {
    kvs foreach (_.close())
    udpListener foreach (_ ! "close")
    import system.dispatcher
    bf.flatMap(_.unbind()).onComplete(_ => system.shutdown())
    system.awaitTermination()
    println("Bye!")
  }
}
