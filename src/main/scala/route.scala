package .stats

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import .kvs.Kvs
import ftier.ws._
import akka.actor.ExtendedActorSystem

case class Route(implicit val system:ExtendedActorSystem,kvs:Kvs) extends RouteGrip[HttpRequest,HttpResponse] {
  import system.log

  val route:PartialFunction[HttpRequest,HttpResponse] = {
    case req@HttpRequest(GET,Path(Root/"stats"/"ws"),_,_,_) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upg) =>
          log.debug(s"Run stream for websocket: $upg")
          upg.handleMessages(Flows.stats)
        case None => HttpResponse(BadRequest)
      }
    case HttpRequest(GET,Path(Root/"monitor.html"),_,_,_) =>
      chunks(None,"monitor.html")
    case HttpRequest(GET,Path(Root/"stats"/request),_,_,_) =>
      chunks(Some("stats"),request)
    case HttpRequest(GET,Path(Root/"stats"/"react"/request),_,_,_) =>
      chunks(Some("stats/react"),request)
    case HttpRequest(GET,Path(Root/"stats"/"bootstrap"/request),_,_,_) =>
      chunks(Some("stats/bootstrap"),request)
    case HttpRequest(GET,Path(Root/"stats"/"bootstrap"/"fonts"/request),_,_,_) =>
      chunks(Some("stats/bootstrap/fonts"),request)
  }
}
