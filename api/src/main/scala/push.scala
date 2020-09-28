package .stats

import zd.proto.api._
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

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

@N(1) final case class StatMsg(@N(1) stat: Stat, @N(2) time: Long, @N(3) host: String) extends Push
@N(2) final case class HostMsg(@N(1) host: String, @N(2) ipaddr: String, @N(3) time: Long) extends Push

sealed trait Stat
@N(1) final case class Metric(@N(1) name: String, @N(2) value: String) extends Stat
@N(2) final case class Measure(@N(1) name: String, @N(2) value: String) extends Stat
@N(3) @RestrictedN(3)
final case class Error(@N(1) exception: String, @N(2) stacktrace: String) extends Stat
@N(4) final case class Action(@N(1) action: String) extends Stat
