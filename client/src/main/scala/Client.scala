package stats
package client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import argonaut._
import Argonaut._
import scala.concurrent.duration._
import org.hyperic.sigar._
import scala.language.postfixOps

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

  def scheduleHardwareStats(implicit system: ActorSystem) = {
    import system.dispatcher
    val config = system.settings.config
    if (config.hasPath("stats.client.enabled") && config.getBoolean("stats.client.enabled")) {
      val remote = config.getString("stats.client.remote.host")
      val port = config.getInt("stats.client.remote.port")
      val sigar = new Sigar
      val mtr = create(remote, port)

      val scheduler = system.scheduler
      val schedules = List(
        // Uptime (seconds)
        scheduler.schedule(1 second, 5 seconds) {
          mtr ! ("sys.uptime", system.uptime)
        },
        // CPU load ([0,1])
        scheduler.schedule(1 second, 15 seconds) {
          mtr ! ("cpu.load", sigar.getCpuPerc.getCombined)
        },
        // Memory (bytes)
        scheduler.schedule(1 second, 1 minute) {
          mtr ! ("mem.used", sigar.getMem.getActualUsed)
          mtr ! ("mem.free", sigar.getMem.getActualFree)
          mtr ! ("mem.total", sigar.getMem.getTotal)
        },
        // FS (KB)
        scheduler.schedule(1 second, 1 hour) {
          import scala.util._
          Try(sigar.getFileSystemUsage("/")) match {
            case Success(usage) =>
              mtr ! ("root./.used", usage.getUsed)
              mtr ! ("root./.free", usage.getFree)
              mtr ! ("root./.total", usage.getTotal)
            case Failure(_) =>
              mtr ! ("root./.used", "--")
              mtr ! ("root./.free", "--")
              mtr ! ("root./.total", "--")
          }
        })

    } else {
      system.log.warning("Stats are disabled, no hardware information will be send to stats server.")
    }
  }

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

  { // Sigar loader
    import org.slf4j.bridge.SLF4JBridgeHandler
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    kamon.sigar.SigarProvisioner.provision()
  }

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

  }

}