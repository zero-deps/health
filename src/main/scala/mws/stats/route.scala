package .stats

import akka.actor.ActorRefFactory

import akka.http.scaladsl.model.{HttpRequest,HttpResponse}
import akka.http.scaladsl.model.HttpMethods.GET

import akka.http.scaladsl.model.MediaTypes.`text/html`
import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}

import .kvs.StKvs

trait RouteGrip[-Q,+S] extends PartialFunction[Q,S]{
  val route:PartialFunction[Q,S]
  def apply(t: Q) = route.apply(t)
  def isDefinedAt(t: Q) = route.isDefinedAt(t)
}

case class Route(implicit val fa:ActorRefFactory,kvs:StKvs) extends RouteGrip[HttpRequest,HttpResponse]{
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
