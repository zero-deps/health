package stats
package client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import argonaut._
import Argonaut._

object Client {

  implicit def StackTraceEncodeJson: EncodeJson[StackTraceElement] =
    EncodeJson((element: StackTraceElement) =>
      ("className" := element.getClassName) ->:
        ("method" := element.getMethodName) ->:
        ("fileName" := element.getFileName) ->:
        ("lineNumber" := element.getLineNumber) ->: jEmptyObject)

  final def create(remote: String, port: Int)(implicit system: ActorSystem): ActorRef =
    system.actorOf(props(new InetSocketAddress(remote, port)))

  def props(socket: InetSocketAddress): Props = Props(new Client(socket))

  def measure[R](name:String)(block: => R)(implicit system: ActorSystem): R ={
    val config = system.settings.config
    if (config.hasPath("stats.client.enabled") && config.getBoolean("stats.client.enabled")) {
      val remote = config.getString("stats.client.remote.host")
      val port = config.getInt("stats.client.remote.port")
      val mtr = create(remote, port)
      val t0 = System.nanoTime()
      val result = block
      val t1 = System.nanoTime()
      mtr ! (name, t1-t0)
      mtr ! "die"
      result
    } else block
  }

  def history(casino: String, user: String, message: String)(implicit system: ActorSystem) = {
    val config = system.settings.config
    if (config.hasPath("stats.client.enabled") && config.getBoolean("stats.client.enabled")) {
      val remote = config.getString("stats.client.remote.host")
      val port = config.getInt("stats.client.remote.port")
      val mtr = create(remote, port)
      mtr ! (casino, user, message)
      mtr ! "die"
    }
  }

  def error(e: Throwable)(implicit system: ActorSystem) = {
    val config = system.settings.config
    if (config.hasPath("stats.client.enabled") && config.getBoolean("stats.client.enabled")) {
      val remote = config.getString("stats.client.remote.host")
      val port = config.getInt("stats.client.remote.port")
      val mtr = create(remote, port)
      mtr ! e
      mtr ! "die"
    }
  }

}

class Client(remote: InetSocketAddress) extends Actor {
  import context.system
  import akka.cluster.Cluster
  import Client.StackTraceEncodeJson
  private val selfAddress = Cluster(system).selfAddress
  private val host = selfAddress.host.getOrElse("")
  private val port = selfAddress.port.getOrElse("")


  IO(Udp) ! Udp.SimpleSender


  final def receive = {
    case Udp.SimpleSenderReady => context become ready(sender)
  }

  final def send(send: ActorRef)(data: Seq[Any]): Unit = {
    send ! Udp.Send(ByteString(data mkString "::"), remote)
  }


  def ready(udp: ActorRef): Receive = {
    case err: Throwable =>
      val stackTrace = (err.getStackTrace.toList map {_.asJson}).asJson.toString

      send(udp)("error" :: system.name :: s"$host:$port" :: s"${err.getClass.getName}:${err.getMessage}" :: stackTrace :: Nil)

    case (param: String, value: Any) =>
      val sender = send(udp) _
      send(udp)("metric" :: system.name :: s"$host:$port" :: param :: value :: Nil)

    case (casino: String, user: String, message: String) =>
      send(udp)("history" :: casino :: user :: message :: Nil)

    case "die" => context.stop(self)

    case x => system.log.warning(s"unexpected message $x")

  }

}