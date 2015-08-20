package .stats

import LastData._
import akka.actor.{Actor, ActorLogging, Props}
import language.implicitConversions

object LastData {
  case class Entry(data: String, prev: Option[String], next: Option[String])

  case object Get
  case class Values(it: Iterator[String])
  case class Delete(key: String)

  private class KvsWrapper(val kvs: Kvs) extends Kvs.Wrapper {
    val bucket = "last"
  }

  def props(kvs: Kvs): Props = Props(new LastData(new KvsWrapper(kvs)))
}

class LastData(kvs: KvsWrapper) extends Actor with ActorLogging {
  import context.system

  override def preStart: Unit =
    system.eventStream.subscribe(self, classOf[Metric])

  override def postStop: Unit =
    system.eventStream.unsubscribe(self, classOf[Metric])

  def receive: Receive = {
    case m @ Metric(name, node, param, time, value) =>
      val key = m.key
      val data = m.serialize
      getEntry(key) match {
        case Some(Entry(_, prev, next)) =>
          kvs.put(key, Entry(data, prev, next))
        case _ =>
          kvs.get("last") match {
            case Some(lastKey) =>
              // insert last
              kvs.put(key, Entry(data, prev = Some(lastKey), next = None))
              // link prev to last
              val last = getEntry(lastKey).get
              kvs.put(lastKey, last.copy(next = Some(key)))
              // update link to last
              kvs.put("last", key)
            case None =>
              kvs.put(key, Entry(data, prev = None, next = None))
              kvs.put("first", key)
              kvs.put("last", key)
          }
      }

    case Message(casino, user, msg, time) => //todo

    case LastData.Get =>
      val it = Iterator.iterate {
        val k = kvs.get("first")
        val v = k.flatMap(getEntry)
        (k, v)
      } { case (_, _v) =>
        val k = _v.flatMap(_.next)
        val v = k.flatMap(getEntry)
        (k, v)
      } takeWhile {
        case (k, v) => k.isDefined && v.isDefined
      } map {
        case (k, v) => v.get.data
      }
      sender ! LastData.Values(it)

    case LastData.Delete(key) =>
      getEntry(key) map { case Entry(_, prev, next) =>
        prev match {
          case Some(prev) =>
            getEntry(prev) map { entry =>
              kvs.put(prev, entry.copy(next = next))
            }
          case _ =>
        }
        next match {
          case Some(next) =>
            getEntry(next) map { entry =>
              kvs.put(next, entry.copy(prev = prev))
            }
          case _ =>
        }
        (kvs.get("first"), kvs.get("last")) match {
          case (Some(`key`), Some(`key`)) =>
            kvs.delete("first")
            kvs.delete("last")
          case (Some(`key`), _) =>
            kvs.put("first", next.get)
          case (_, Some(`key`)) =>
            kvs.put("last", prev.get)
          case _ =>
        }
        kvs.delete(key)
      }
  }

  def getEntry(key: String): Option[Entry] =
    kvs.get(key) map { entry =>
      val xs = entry.split(";", -1)
      val data = xs(0)
      val prev = if (xs(1) != "") Some(xs(1)) else None
      val next = if (xs(2) != "") Some(xs(2)) else None
      Entry(data, prev, next)
    }

  implicit def serialize(entry: Entry): String =
    List(
      entry.data,
      entry.prev.getOrElse(""),
      entry.next.getOrElse("")
    ).mkString(";")
}
