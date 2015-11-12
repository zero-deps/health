package .stats

import akka.actor.{ExtensionKey,Extension,ExtendedActorSystem,ActorRefFactory}
import akka.http.scaladsl.model.StatusCodes.NotImplemented
import akka.http.scaladsl.model.{HttpRequest,HttpResponse}

object Rt extends ExtensionKey[Rt]{
  override def lookup = Rt
  override def createExtension(system: ExtendedActorSystem): Rt = new Rt(system)
}
class Rt(val system:ExtendedActorSystem) extends Extension {
  import scala.collection.JavaConverters._
  import akka.actor.DynamicAccess
  import scala.util.Success

  type Hr = HttpRequest
  type Hs = HttpResponse
  type R = PartialFunction[Hr,Hs]

  lazy val c = system.settings.config
  lazy val ni:R = {case x:Hr => HttpResponse(status=NotImplemented)}

  val fqcns = c.getStringList(".routes").asScala
  println(s"routes to create $fqcns")

  import scala.collection._

  val routes = fqcns.map(
    system.dynamicAccess.createInstanceFor[R](_,immutable.Seq(classOf[ActorRefFactory]->system))
  ).collect{case Success(f)=> f}
  val route:R = routes.foldLeft[R](ni)((b:R, f:R)=> f orElse b)
}
