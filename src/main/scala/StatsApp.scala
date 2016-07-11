package .stats

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ftier.ws._

object StatsApp extends App {
  if (!sys.props.contains("config.resource"))
    sys.props += ("config.resource" -> "app.conf")

  implicit val system = ActorSystem("Stats")
  implicit val materializer = ActorMaterializer()

  val ws = Ws(system)
  import ws.kvs

  Flows.saveDataFromUdp.run()

  ws.bindAndHandle
}
