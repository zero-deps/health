package .stats

import org.scalatest.{ FreeSpecLike, Matchers, EitherValues }
import .kvs.store.Memory
import .kvs.store.Dba
import akka.testkit.TestKit
import akka.actor.ActorSystem
import .kvs.Kvs
import com.typesafe.config.ConfigFactory

object TreeStorageTest {
  val configString = """
     kvs {
		  store=".kvs.store.Memory"
		 }
    """

  val config = ConfigFactory.parseString(configString)
}

class TreeStorageTest extends TestKit(ActorSystem("Test", TreeStorageTest.config)) with FreeSpecLike with Matchers with EitherValues {
  import TreeStorage._
  import .kvs.handle.Handler._

  type EnType = String

  val kvs = Kvs(system)
  val fid = "testfeed"

  val data1: EnType = "data1"
  val data2: EnType = "data2"
  val data3: EnType = "data3"

  "Tree storage should" - {
    "be empty at creation" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2")
      res(0).left.get.name shouldBe "error"
    }

    "save successfully value in 1~2" in {
      val res = kvs.treeAdd(fid, "1" ~ "2", data1)
      res.right.get.fid shouldBe fid
      res.right.get.data shouldBe data1
    }

    "retrieve saved value from 1~2" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2")
      res.size shouldBe 1

      res(0).right.get.fid shouldBe fid
      res(0).right.get.data shouldBe data1
    }

    "save successfully another value 1~2~3" in {
      val res = kvs.treeAdd(fid, "1" ~ "2" ~ "3", data2)
      res.right.get.fid shouldBe fid
      res.right.get.data shouldBe data2
    }

    "retrieve saved another value from 1~2~3" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2" ~ "3")
      res.size shouldBe 1

      res(0).right.get.fid shouldBe fid
      res(0).right.get.data shouldBe data2
    }

    "retrieve two saved values from 1~2" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2")
      res.size shouldBe 2

      res(0).right.get.fid shouldBe fid
      res(0).right.get.data shouldBe data2

      res(1).right.get.fid shouldBe fid
      res(1).right.get.data shouldBe data1
    }

    "delete values from 1~2" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2")

      val en1 = res(0).right.get
      val en2 = res(1).right.get

      println("===========================")
      println(kvs.dba.asInstanceOf[Memory].storage)
      println(en1)
      println(en2)
      println("===========================")

      val res1 = kvs.treeRemove(en1)
      val res2 = kvs.treeRemove(en2)

      println(kvs.dba.asInstanceOf[Memory].storage)
      println(res2.right)
      println(res1.right)
    }

    "shouldn't be values in 1~2" in {
      val res = kvs.treeEntries[EnType](fid, "1" ~ "2")
      res.size shouldBe 0
    }
  }
}