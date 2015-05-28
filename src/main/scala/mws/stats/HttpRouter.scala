package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import http.{Content, Resource, Route, Template}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.http.HttpMethods._
import spray.http.StatusCodes
import util.Success
import HttpRouter._

object HttpRouter {
  def props(stats: ActorRef): Props = Props(new HttpRouter(stats))

  val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
}

class HttpRouter(stats: ActorRef) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1 second

  def receive: Receive = {
    case Route(GET, Nil, _) =>
      sender ! Template(html.home())

    case Route(GET, "get" :: Nil, _) =>
      val client = sender
      stats ? StatsKvs.Get onComplete {
        case Success(StatsKvs.GetAck(nodes)) =>
          client ! Content(mapper.writeValueAsString(nodes))
        case x =>
          log.warning(s"Bad response: $x")
          client ! Content("Internal Server Error", StatusCodes.InternalServerError)
      }

    case Route(GET, path, _) if path.last.endsWith(".css") ||
                                path.last.endsWith(".html") ||
                                path.last.endsWith(".js") ||
                                path.last.endsWith(".jpg") ||
                                path.last.endsWith(".png") =>
      val localPath = ("public" :: path) mkString "/"
      sender ! Resource(localPath)

    case _ =>
      sender ! Content("Not Found", StatusCodes.NotFound)
  }
}
