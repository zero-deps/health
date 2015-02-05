package .stats.http

import java.io.InputStream
import java.net.URL
import scala.util.Try
import spray.http.HttpCharsets.`UTF-8`
import spray.http.MediaTypes._
import spray.http.StatusCodes.NotFound
import spray.http._

object Resource {
  def apply(path: String): HttpResponse = {
    this.getClass.getClassLoader.getResource(path) match {
      case null =>
        HttpResponse(status = NotFound)
      case url =>
        val contentType = getContentType(path)
        val bytes = convertUrlToByteArray(url)
        HttpResponse(entity = HttpEntity(contentType, bytes))
    }
  }

  private def convertUrlToByteArray(url: URL): Array[Byte] = {
    var is: InputStream = null
    try {
      is = url.openStream()
      Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
    } finally {
      if (is != null) Try(is.close())
    }
  }

  private def getContentType(fileName: String): ContentType = {
    val ext = fileName.lastIndexOf('.') match {
      case -1 => ""
      case x => fileName.substring(x + 1)
    }
    val mediaType = MediaTypes.forExtension(ext) getOrElse `application/octet-stream`
    mediaType match {
      case x if !x.binary ⇒ ContentType(x, `UTF-8`)
      case x ⇒ ContentType(x)
    }
  }
}
