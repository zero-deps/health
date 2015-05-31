package .stats

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.routes._

object Http {
  val routes = Routes({
    case HttpRequest(request: HttpRequestEvent) => request match {
      case GET(Path("/")) => {
        request.response.write(html.home().toString, "text/html; charset=UTF-8")
      }
    }
  })
}
