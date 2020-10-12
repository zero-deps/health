package 

import java.time.{LocalDateTime, ZoneId, Instant}

import zd.proto._, api._, macrosapi._
import kvs._

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

  implicit object EnDataCodec extends DataCodec[EnData] {
    implicit val c = caseCodecAuto[EnData]
    def extract(xs: Bytes): EnData = decode[EnData](xs)
    def insert(x: EnData): Bytes = encodeToBytes(x)
  }

  object as_str extends DataCodec[String] {
    case class Str(@N(1) unwrap: String)
    implicit val strc = caseCodecAuto[Str]
    def extract(xs: Bytes): String = decode[Str](xs).unwrap
    def insert(x: String): Bytes = encodeToBytes(Str(x))
  }
}
