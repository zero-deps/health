package .stats.http

import play.twirl.api.HtmlFormat
import spray.http.StatusCodes._
import spray.http.{HttpResponse, StatusCode}

object Template {
  def apply(template: HtmlFormat.Appendable, status: StatusCode = OK): HttpResponse = {
    Content(template.toString, status = status)
  }
}
