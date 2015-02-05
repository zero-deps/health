package .stats.http

import spray.http.HttpCharsets._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._

object Content {
  def apply(content: String, status: StatusCode = OK, headers: List[HttpHeader] = Nil): HttpResponse = {
    val contentType: ContentType = ContentType(`text/html`, `UTF-8`)
    HttpResponse(entity = HttpEntity(contentType, content), status = status, headers = headers)
  }
}
