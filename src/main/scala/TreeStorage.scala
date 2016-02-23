package .stats

import .kvs.handle.EnHandler
import .kvs.handle.Handler
import .kvs.store.Dba
import .kvs.Res
import .kvs.handle.`package`.En
import .kvs.Kvs
import .kvs.Err
import .kvs.store._
import .kvs.handle._
import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer

object TreeStorage {
  def uid = java.util.UUID.randomUUID.toString

  case class TreeFids(fid: String) {
    val tree = s"$fid!!tree"
    val places = s"$fid!!places"
    val entries = s"$fid"

    def treeFidForKey(key: String) = s"$tree@@$key"
  }

  implicit class KeyTreeBuilder(node: String) {
    val keys = ListBuffer[String](node)
    def addNode(node: String): KeyTreeBuilder = {
      keys += node
      this
    }

    def ~(node: String) = addNode(node)
  }

  implicit def builderToTreeKey(builder: KeyTreeBuilder): TreeKey = TreeKey(builder.keys.toList)

  object TreeKey {
    def apply(longestKey: String) = new TreeKey(longestKey.split("$$").toList)
  }

  case class TreeKey(keys: List[String]) {
    lazy val treeKeys: List[String] = {
      val keysList = ListBuffer[String]()
      var curKey = ListBuffer[String]()
      keys map { key =>
        curKey = curKey += key
        keysList += curKey.mkString("$$")
      }

      keysList.toList
    }

    val longestKey = keys.mkString("$$")
  }

  object EmptyTreeKey extends TreeKey(List.empty)
  
  case class Link(linkTo: String)

  implicit object linkHandler extends EnHandler[Link] {
    import scala.pickling._, Defaults._, binary._

    override def pickle(e: En[Link]): Array[Byte] = e.pickle.value
    override def unpickle(a: Array[Byte]): En[Link] = a.unpickle[En[Link]]
  }

  case class TreeEn[T](fid: String, treeKey: TreeKey, id: String, prev: Option[String] = None, next: Option[String] = None, data: T)

  object TreeEn {
    def apply[T](fid: String, key: List[String], data: T): TreeEn[T] = apply[T](fid, TreeKey(key), data)
    def apply[T](fid: String, key: TreeKey, data: T): TreeEn[T] = TreeEn[T](fid, key, uid, data = data)
  }

  implicit class TreeKvs(kvs: Kvs) {
    def treeAdd[T](fid: String, key: TreeKey, data: T)(implicit handler: EnHandler[T]): Res[En[T]] = treeAdd(TreeEn(fid, key, data = data))

    def treeAdd[T](el: TreeEn[T])(implicit handler: Handler[En[T]]): Res[En[T]] = {
      val fids = TreeFids(el.fid)
      val entry = En[T](fids.entries, el.id, el.prev, el.next, el.data)

      el.treeKey.treeKeys map { key =>
        En[Link](fids.treeFidForKey(key), entry.id, data = Link(entry.id))
      } map { en =>
        kvs.add[En[Link]](en)
      } //added link to tree-feed

      kvs.add[En[String]](En(fids.places, el.id, data = el.treeKey.longestKey)) //added link to places-feed

      kvs.add[En[T]](entry) //adden entry to feed
    }

    def treeRemove[T](en: En[T])(implicit enHandler: EnHandler[T]) = {
      val fids = TreeFids(en.fid)

      val res = kvs.get[String](fids.places, en.id)
      res.right map { place =>
        TreeKey(place.data).treeKeys map { key =>
          kvs.get[Link](fids.treeFidForKey(key), en.id).right map { x => kvs.remove(x)}
          kvs.remove(place)
        }
      }

      kvs.remove(en)
    }

    def treeEntries[T](fid: String, treeKey: TreeKey, from: Option[En[T]], count: Option[Int])(implicit enHandler: EnHandler[T]): List[Either[.kvs.Dbe, En[T]]] = {
      val fids = TreeFids(fid)

      val fromEntry = from map { x => En[Link](fids.treeFidForKey(treeKey.longestKey), x.id, data = Link(x.id)) }

      kvs.entries[En[Link]](fids.treeFidForKey(treeKey.longestKey), fromEntry, count) fold (
        l => List(Left(l)),
        enlist => enlist map { x =>
          kvs.get[T](fids.entries, x.data.linkTo)
        })
    }

    def treeEntries[T](fid: String, treeKey: TreeKey)(implicit enHandler: EnHandler[T]): List[Either[.kvs.Dbe, En[T]]] = treeEntries[T](fid, treeKey, None, None)
  }
}