package .stats

import akka.actor.{Actor, ActorLogging, Props}
import http.{Content, Resource, Route, Template}
import scala.language.postfixOps
import spray.http.HttpMethods._
import spray.http.StatusCodes

object HttpRouter {
  def props(): Props = Props(new HttpRouter)
}

class HttpRouter extends Actor with ActorLogging {
  def receive: Receive = {
    case Route(GET, Nil, _) =>
      sender ! Template(html.home())

    case Route(GET, path, _) if path.last.endsWith(".css") ||
                                path.last.endsWith(".html") ||
                                path.last.endsWith(".js") ||
                                path.last.endsWith(".jpg") ||
                                path.last.endsWith(".png") =>
      val localPath = ("public" :: path) mkString "/"
      sender ! Resource(localPath)

    case Route(method, page, query) =>
      sender ! Content("Not found", StatusCodes.NotFound)
  }
}
