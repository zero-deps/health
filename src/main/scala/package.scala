package 

import java.time.{LocalDateTime, ZoneId, Instant}

import zd.proto.api.{MessageCodec, encode, decode}
import zd.proto.macrosapi.{caseCodecAuto, caseCodecNums}
import zd.kvs
import zero.ext._, either._

package object stats {
  implicit object FdHandler extends kvs.en.FdHandler {
    implicit val fdCodec: MessageCodec[kvs.en.Fd] = caseCodecNums[kvs.en.Fd]("id"->1, "top"->2, "count"->3)
    def pickle(e: kvs.en.Fd): kvs.Res[Array[Byte]] = encode[kvs.en.Fd](e).right
    def unpickle(a: Array[Byte]): kvs.Res[kvs.en.Fd] = decode[kvs.en.Fd](a).right
  }

  implicit object StatEnHandler extends kvs.en.EnHandler[StatEn] {
    val fh: kvs.en.FdHandler = FdHandler
    implicit val statEnCodec: MessageCodec[StatEn] = caseCodecAuto[StatEn]
    def pickle(e: StatEn): kvs.Res[Array[Byte]] = encode(e).right
    def unpickle(a: Array[Byte]): kvs.Res[StatEn] = decode[StatEn](a).right
    protected def update(en: StatEn, prev: String): StatEn = en.copy(prev=prev)
    protected def update(en: StatEn, id: String, prev: String): StatEn = en.copy(id=id, prev=prev)
  }

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
}
