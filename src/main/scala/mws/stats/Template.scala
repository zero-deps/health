package .stats

object Template {
  case class HomeContext(hostname: String, port: Int, wsUrl: String)

  def resource(path: String): String = {
    val url = getClass.getResource(s"/public/$path")
    val time = url.openConnection().getLastModified
    s"$path?t=$time"
  }
}
