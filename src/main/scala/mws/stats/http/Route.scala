package .stats.http

import spray.http.HttpCharsets._
import spray.http.Uri.Query
import spray.http.{HttpEntity, HttpMethod, HttpRequest, Uri}
import spray.httpx.unmarshalling.Unmarshaller

object Route {
  def apply(method: HttpMethod, path: List[String], query: Option[Query]): Unit = ()

  def unapply(request: HttpRequest): Option[(HttpMethod, List[String], Option[Query])] = {
    val method = request.method
    val path = request.uri.path.toString.split('/').toList.filterNot(_.isEmpty)
    val query = getQuery(request)
    Some((method, path, query))
  }

  def getQuery(request: HttpRequest) : Option[Query] = {
    request.entity match {
      case HttpEntity.NonEmpty(contentType, data) =>
        Unmarshaller[Query](contentType.mediaType) {
          case _ =>
            val nioCharset = contentType.definedCharset.getOrElse(`UTF-8`).nioCharset
            Uri.Query(data.asString(nioCharset), nioCharset)
        }(request.entity) match {
          case Right(query) => Some(query)
          case _ => None
        }
      case _ => if (request.uri.query.nonEmpty) Some(request.uri.query) else None
    }
  }
}
