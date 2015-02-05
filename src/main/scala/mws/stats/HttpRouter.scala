package .stats

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import http.{Content, Resource, Route, Template}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.http.HttpMethods._
import spray.http.StatusCodes
import util.Success

object HttpRouter {
  def props(stats: ActorRef): Props = Props(new HttpRouter(stats))
}

class HttpRouter(stats: ActorRef) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1 second
  implicit val formats = Serialization.formats(NoTypeHints)

  def receive: Receive = {
    case Route(GET, Nil, _) =>
      sender ! Template(html.home())

    case Route(GET, "get" :: Nil, _) =>
      val client = sender
      stats ? StatsKvs.All onComplete {
        case Success(StatsKvs.AllAck(nodes)) =>
          client ! Content(write(nodes))
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
