package .stats

import akka.http.scaladsl.model.{HttpRequest,HttpResponse,HttpEntity}
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.MediaTypes.`text/html`

abstract class RouteDelegate[-Q,+S](delegate: PartialFunction[Q,S]) extends PartialFunction[Q,S]{
  def apply(t: Q) = delegate.apply(t)
  def isDefinedAt(t: Q) = delegate.isDefinedAt(t)
}

object Route extends RouteDelegate[HttpRequest,HttpResponse]({
  case req @ HttpRequest(GET, Path(Root / "websocket"),_,_,_) => StatsApp.handleStats(req)
  case HttpRequest(GET, u @ Path(Root / request),_,_,_) => chunks(Some("public"),request)
  case HttpRequest(GET, u @ Path(Root / "react" / request),_,_,_) => chunks(Some("public/react"),request)
  case HttpRequest(GET, u @ Path(Root / "bootstrap" / request),_,_,_) => chunks(Some("public/bootstrap"),request)
  case HttpRequest(GET, u @ Path(Root / "bootstrap" / "fonts" / request),_,_,_) =>
    chunks(Some("public/bootstrap/fonts"),request)
  case HttpRequest(GET, Path(Root),_,_,_) =>
    import .stats.Template._
    HttpResponse(entity=HttpEntity(`text/html`, 
      html.home(HomeContext(9010, "/websocket", List.empty, List.empty)).toString))
})
