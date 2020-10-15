package .stats

import zero.ext._, option._
import zd.proto.api.N

final case class EnData
  ( @N(1) value: String
  , @N(2) time: Option[Long]
  , @N(3) host: Option[String]
  )

object EnData {
  def apply(value: String): EnData = new EnData(value=value, time=none, host=none)
  def apply(value: String, time: Long): EnData = new EnData(value=value, time=time.some, host=none)
  def apply(value: String, time: Long, host: String): EnData = new EnData(value=value, time=time.some, host=host.some)
}
