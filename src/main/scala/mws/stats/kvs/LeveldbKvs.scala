package .stats.kvs

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import java.io.File
import org.fusesource.leveldbjni.JniDBFactory
import org.fusesource.leveldbjni.JniDBFactory._
import org.iq80.leveldb._
import scala.collection.immutable
import scala.util.Try

object LeveldbKvs {
  /** This method opens LevelDB file. Throws exception if database is already opened */
  def open(config: Config): DB = {
    val file = new File(config.dir)
    val options = new Options().createIfMissing(config.createIfMissing)
    JniDBFactory.factory.open(file, options)
  }

  implicit class LeveldbKvsConfig(config: Config) {
    def dir: String = config.getString("dir")
    /** Create LevelDB storage if it doesn't exist */
    def createIfMissing: Boolean = getBoolean("create-if-missing", true)
    /** Verify checksum on read */
    def checksum: Boolean = getBoolean("checksum", false)
    /** Use fsync on write */
    def fsync: Boolean = getBoolean("fsync", true)
    /** Helper */
    def getBoolean(path: String, default: Boolean): Boolean =
      if (config.hasPath(path)) config.getBoolean(path) else default
  }
}

trait LeveldbKvs extends Kvs with Actor with ActorLogging {
  import LeveldbKvs.LeveldbKvsConfig

  val leveldbConfigPath: String
  val leveldbConfig = context.system.settings.config.getConfig(leveldbConfigPath)

  def leveldbReadOptions = new ReadOptions().verifyChecksums(leveldbConfig.checksum)
  val leveldbWriteOptions = new WriteOptions().sync(leveldbConfig.fsync).snapshot(false)

  val leveldb: DB

  def put(key: String, value: String): Unit = {
    leveldb.put(key, value)
  }

  def get(key: String): Option[String] = {
    leveldb.get(key)
  }

  def get(): immutable.Seq[(String, String)] = {
    val ro = leveldbReadOptions.snapshot(leveldb.getSnapshot)
    try {
      val it = leveldb.iterator(ro)
      try {
        it.seekToFirst()
        iteratorToList(it, acc = Nil)
      } finally Try(it.close())
    } finally Try(ro.snapshot().close())
  }

  def delete(key: String): Unit = {
    leveldb.delete(key)
  }
  
  def iteratorToList(it: DBIterator, acc: immutable.Seq[(String, String)]): immutable.Seq[(String, String)] = {
    if (it.hasNext) {
      val tuple: (String, String) = it.peekNext().getKey -> it.peekNext().getValue
      it.next()
      iteratorToList(it, tuple +: acc)
    } else acc
  }

  import scala.language.implicitConversions
  implicit def strToBytes(value: String): Array[Byte] = bytes(value)
  implicit def bytesToStr(value: Array[Byte]): Option[String] = Option(asString(value))
  implicit def tupleToStr(tuple: (Array[Byte], Array[Byte])): (String, String) =
    asString(tuple._1) -> asString(tuple._2)
}
