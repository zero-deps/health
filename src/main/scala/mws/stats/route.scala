package .stats

import akka.actor.ActorSystem

import akka.http.scaladsl.model.HttpMethods.GET

import akka.http.scaladsl.model.MediaTypes.`text/html`
import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}

import akka.stream.io.{InputStreamSource => IsSource}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.StatusCodes.NotFound

import .kvs.Kvs
import ftier.ws._

case class Route(implicit val system:ActorSystem,kvs:Kvs) extends RouteGrip[HttpRequest,HttpResponse]{
  def chunks(d:Option[String],r:String) =
    Option(getClass.getClassLoader.getResourceAsStream(d match {case None=>r;case Some(p)=>p+"/"+r})) match {
      case Some(stream) => HttpResponse(entity=HttpEntity.Chunked(Mime(r), IsSource(() => stream).map(ChunkStreamPart.apply)))
      case None => HttpResponse(NotFound) }

  val route:PartialFunction[HttpRequest,HttpResponse] = {
    case req @ HttpRequest(GET, Path(Root / "websocket"),_,_,_) => StatsApp.handleStats(req)
    case HttpRequest(GET, Path(Root / request),_,_,_)
      if Seq("home.css", "home.js","messages.js","util.js","metrics.js").filter(_ == request).size == 1 =>
      chunks(Some("public"),request)
    case HttpRequest(GET, Path(Root / "react" / request),_,_,_) =>
      println(s"react $request")
      chunks(Some("public/react"),request)
    case HttpRequest(GET, Path(Root / "bootstrap" / request),_,_,_) => chunks(Some("public/bootstrap"),request)
    case HttpRequest(GET, Path(Root / "bootstrap" / "fonts" / request),_,_,_) =>
      chunks(Some("public/bootstrap/fonts"),request)
    case HttpRequest(GET, Path(Root),_,_,_) => StatsApp.index
  }
}
