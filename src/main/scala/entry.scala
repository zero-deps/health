package .stats

import zd.proto.api.N

case class Node
  ( @N(1) ipaddr: String
  , @N(2) time: Long
  , @N(3) host: String
  )

case class AvgData
  ( @N(1) value: Double
  , @N(2) id: Long
  , @N(3) n: Long
  ) {
  val value_str = value.toString
}

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

case class TimedErr
  ( @N(1) msg: Option[String]
  , @N(2) cause: String
  , @N(3) st: Seq[String]
  , @N(4) time: Long
  )