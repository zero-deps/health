package 

import java.time.{LocalDateTime, ZoneId, Instant}

// import zd.proto.api.{MessageCodec, encode, decode}
// import zd.proto.macrosapi.{caseCodecAuto, caseCodecNums}
// import zd.kvs
// import zero.ext._, either._
import zd.proto.Bytes
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.CodedInputStream

package object stats {
  // implicit object FdHandler extends kvs.en.FdHandler {
  //   implicit val fdCodec: MessageCodec[kvs.en.Fd] = caseCodecNums[kvs.en.Fd]("id"->1, "top"->2, "count"->3)
  //   def pickle(e: kvs.en.Fd): kvs.Res[Array[Byte]] = encode[kvs.en.Fd](e).right
  //   def unpickle(a: Array[Byte]): kvs.Res[kvs.en.Fd] = decode[kvs.en.Fd](a).right
  // }

  // implicit object StatEnHandler extends kvs.en.EnHandler[StatEn] {
  //   val fh: kvs.en.FdHandler = FdHandler
  //   implicit val statEnCodec: MessageCodec[StatEn] = caseCodecAuto[StatEn]
  //   def pickle(e: StatEn): kvs.Res[Array[Byte]] = encode(e).right
  //   def unpickle(a: Array[Byte]): kvs.Res[StatEn] = decode[StatEn](a).right
  //   protected def update(en: StatEn, prev: String): StatEn = en.copy(prev=prev)
  //   protected def update(en: StatEn, id: String, prev: String): StatEn = en.copy(id=id, prev=prev)
  // }

  def now_ms(): String = System.currentTimeMillis.toString
  def year_ago(): LocalDateTime = LocalDateTime.now.minusYears(1)

  implicit class LocalDateTimeWrapper(v: LocalDateTime) {
    def toMillis(): Long = {
      v.atZone(ZoneId.systemDefault).toInstant.toEpochMilli
    }
  }

  implicit class EposhTimeWrapper(v: Long) {
    def toLocalDataTime(): LocalDateTime = {
      Instant.ofEpochMilli(v).atZone(ZoneId.systemDefault).toLocalDateTime
    }
  }

  implicit val EnDataC = zd.proto.macrosapi.caseCodecAuto[EnData]

  def extract(x: zd.kvs.en.IdEn): EnData = zd.proto.api.decode[EnData](x.en.data)
  def insert(x: EnData): Bytes = zd.proto.api.encodeToBytes[EnData](x)

  def str_to_bytes(x: String): Bytes = Bytes.unsafeWrap(x.getBytes("utf8")) //todo: use protobuf?
  def int_to_bytes(x: Int): Bytes = {
    val b = new Array[Byte](CodedOutputStream.computeInt32SizeNoTag(x))
    CodedOutputStream.newInstance(b).writeInt32NoTag(x)
    Bytes.unsafeWrap(b)
  }
  def long_to_bytes(x: Long): Bytes = {
    val b = new Array[Byte](CodedOutputStream.computeInt64SizeNoTag(x))
    CodedOutputStream.newInstance(b).writeInt64NoTag(x)
    Bytes.unsafeWrap(b)
  }
  def float_to_bytes(x: Float): Bytes = {
    val b = new Array[Byte](4)
    CodedOutputStream.newInstance(b).writeFloatNoTag(x)
    Bytes.unsafeWrap(b)
  }
  def str(x: Bytes): String = new String(x.unsafeArray, "utf8")
  def integer(x: Bytes): Int = {
    CodedInputStream.newInstance(x.unsafeArray).readInt32()
  }
  def long(x: Bytes): Long = {
    CodedInputStream.newInstance(x.unsafeArray).readInt64()
  }
  def float(x: Bytes): Float = {
    CodedInputStream.newInstance(x.unsafeArray).readFloat()
  }
}
