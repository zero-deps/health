package 

import java.time.{LocalDateTime, ZoneId, Instant}

import zd.proto._, api._, macrosapi._
import zd.proto.Bytes

package object stats {
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

  implicit val EnDataC = caseCodecAuto[EnData]

  def extract(x: zd.kvs.en.`Key,En`): EnData = decode[EnData](x.en.data)
  def insert(x: EnData): Bytes = encodeToBytes[EnData](x)
}
