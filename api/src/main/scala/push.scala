package metrics

import zd.proto._, api._, macrosapi._

object Push {
  implicit val StatC = {
    implicit val MetricC = caseCodecAuto[Metric]
    implicit val MeasureC = caseCodecAuto[Measure]
    implicit val ErrorC = caseCodecAuto[Error]
    implicit val ActionC = caseCodecAuto[Action]
    sealedTraitCodecAuto[Stat]
  }
  implicit val StatMsgC = caseCodecAuto[StatMsg]
  implicit val HostMsgC = caseCodecAuto[HostMsg]
  implicit val pushCodec = sealedTraitCodecAuto[Push]
}

sealed trait Push
@N(1) case class StatMsg(@N(1) stat: Stat, @N(2) time: Long, @N(3) host: String) extends Push
@N(2) case class HostMsg(@N(1) host: String, @N(2) ipaddr: String, @N(3) time: Long) extends Push

sealed trait Stat
@N(1) case class Metric(@N(1) name: String, @N(2) value: String) extends Stat
@N(2) case class Measure(@N(1) name: String, @N(2) value: String) extends Stat
@N(3) case class Error(@N(1) msg: Option[String], @N(2) cause: Option[String], @N(3) st: Seq[String]) extends Stat
@N(4) case class Action(@N(1) action: String) extends Stat
