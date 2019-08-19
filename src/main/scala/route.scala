package .stats

import akka.actor.ExtendedActorSystem
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import .ftier._
import zd.kvs.Kvs
import scala.concurrent.Future

class Route(implicit val system: ExtendedActorSystem) extends RouteGrip[HttpRequest,Future[HttpResponse]] {
  import system.log

  lazy val kvs = Kvs(system)

  val route:PartialFunction[HttpRequest,Future[HttpResponse]] = {
    case req@HttpRequest(GET,Path(Root/"stats"/"ws"),_,_,_) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upg) =>
          log.debug(s"Run stream for websocket: $upg")
          Future.successful(upg.handleMessages(Flows.ws(system, kvs)))
        case None => Future.successful(HttpResponse(BadRequest))
      }
    case HttpRequest(GET,Path(Root),_,_,_) =>
      Future.successful(chunks(Some("assets"),"index.html"))
    case HttpRequest(GET,Path(Root/"css"/request),_,_,_) =>
      Future.successful(chunks(Some("assets/css"),request))
    case HttpRequest(GET,Path(Root/"js"/request),_,_,_) =>
      Future.successful(chunks(Some("assets/js"),request))
    case HttpRequest(GET,Path(Root/"fonts"/request),_,_,_) =>
      Future.successful(chunks(Some("assets/fonts"),request))
  }
}
