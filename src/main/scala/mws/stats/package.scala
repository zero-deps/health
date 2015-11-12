package 

package object stats {
  type Segment = String

  sealed trait Path
  object Root extends Path
  case class SegmentPath(prev: Path, head: Segment) extends Path

  import akka.http.scaladsl.model.Uri

  object Path {
    def unapply(uri: Uri): Option[Path] = {
      def path(p: Uri.Path): Path = {
        p.reverse
        p match {
          case Uri.Path.Empty => Root
          case Uri.Path.Segment(head, slashOrEmpty) => SegmentPath(path(slashOrEmpty), head)
          case Uri.Path.Slash(tail) => path(tail) }}

      if(uri.isEmpty) None else Option(path(uri.path.reverse)) }}

  object / {
    def unapply(l: Path): Option[(Path, String)] = l match {
      case Root => None
      case s: SegmentPath => Option((s.prev, s.head)) }}

  object Mime {
    import akka.http.scaladsl.model.MediaType
    import akka.http.scaladsl.model.MediaTypes._
    def apply(u:Uri):MediaType = u.toString.dropWhile(_!='.') match {
      case ".css"  | ".min.css" => `text/css`
      case ".woff2"| ".woff"    => `application/font-woff`
      case ".ttf"               => `application/x-font-truetype`
      case ".html" | ".htm"     => `text/html`
      case ".pdf"               => `application/pdf`
      case ".js" | ".min.js"    => `application/javascript`
      case _ => `application/octet-stream`
    }
  }

  import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
  import akka.stream.io.{InputStreamSource => IsSource}
  import akka.http.scaladsl.model.{HttpRequest,HttpResponse}
  import akka.http.scaladsl.model.StatusCodes.{NotFound,NotImplemented}
  import akka.http.scaladsl.model._

  def chunks(d:Option[String],r:String) =
    Option(getClass.getClassLoader.getResourceAsStream(d match {case None=>r;case Some(p)=>p+"/"+r})) match {
      case Some(stream) => HttpResponse(entity=HttpEntity.Chunked(Mime(r), IsSource(() => stream).map(ChunkStreamPart.apply)))
      case None => HttpResponse(NotFound) }

  lazy val notImplemented:PartialFunction[HttpRequest,HttpResponse] = {case x:HttpRequest => HttpResponse(status=NotImplemented)}
}
