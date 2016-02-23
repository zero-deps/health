package .stats

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, HttpEntity }
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.StatusCodes.{NotFound,BadRequest}
import akka.http.scaladsl.model.ws.UpgradeToWebsocket
import .kvs.Kvs
import ftier.ws._
import akka.stream.scaladsl.StreamConverters
import akka.actor.ExtendedActorSystem

case class Route(implicit val system:ExtendedActorSystem,kvs:Kvs) extends RouteGrip[HttpRequest,HttpResponse] {
  import system.log

  val route: PartialFunction[HttpRequest, HttpResponse] = {
    case req @ HttpRequest(GET,Path(Root/"websocket"),_,_,_) =>
      req.header[UpgradeToWebsocket] match {
        case Some(upg) =>
          log.debug(s"Run stream for websocket: $upg")
          upg.handleMessages(Flows.stats)
        case None => HttpResponse(BadRequest)
      }
    case HttpRequest(GET,Path(Root/request),_,_,_) =>
      chunks(Some("public"),request)
    case HttpRequest(GET,Path(Root/"react"/request),_,_,_) =>
      chunks(Some("public/react"),request)
    case HttpRequest(GET,Path(Root/"bootstrap"/request),_,_,_) => 
      chunks(Some("public/bootstrap"),request)
    case HttpRequest(GET,Path(Root/"bootstrap"/"fonts"/request),_,_,_) =>
      chunks(Some("public/bootstrap/fonts"),request)
  }

  def chunks(d:Option[String],r:String) =
    Option(getClass.getClassLoader.getResourceAsStream(d match { case None => r; case Some(p) => p + "/" + r })) match {
      case Some(stream) => HttpResponse(entity=HttpEntity.Chunked(Mime(r),StreamConverters.fromInputStream(()=>stream).map(ChunkStreamPart.apply)))
      case None => HttpResponse(NotFound)
    }
}
