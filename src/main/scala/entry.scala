package .stats

import zero.ext._, option._
import zd.proto.api.N

case class EnData
  ( @N(1) value: String
  , @N(2) time: Long
  , @N(3) host: Option[String]
  )

object EnData {
  def apply(value: String, time: Long): EnData = new EnData(value=value, time=time, host=none) //todo: Timed[A]
  def apply(value: String, time: Long, host: String): EnData = new EnData(value=value, time=time, host=host.some)
}

case class AvgData
  ( @N(1) value: Double
  , @N(2) id: Long
  , @N(3) n: Long
  )

case class QData
  ( @N(1) xs: Vector[Timed[Int]]
  , @N(2) q: Int
  ) {
  val q_str = q.toString
}

case class Timed[A]
  ( @N(1) value: A
  , @N(2) time: Long
  ) {
  val value_str = value.toString
}