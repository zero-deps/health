package .stats

import zero.ext._, option._
import zd.proto.api.N

case class EnData
  ( @N(1) value: String
  , @N(2) time: Long
  , @N(3) host: Option[String]
  )

object EnData {
  def apply(value: String, time: Long): EnData = new EnData(value=value, time=time, host=none)
  def apply(value: String, time: Long, host: String): EnData = new EnData(value=value, time=time, host=host.some)
}

case class AvgData
  ( @N(1) value: Double
  , @N(2) id: Long
  , @N(3) n: Long
  )