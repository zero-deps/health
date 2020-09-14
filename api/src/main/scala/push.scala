package .stats

import zd.proto.api._
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

object Push {

  implicit val StatMetaCode = caseCodecAuto[StatMeta]

  implicit val StatCodec = {
    implicit val StatMetaCodec = caseCodecAuto[StatMeta]
    implicit val MetricCodec = caseCodecAuto[Metric]
    implicit val MeasureCodec = caseCodecAuto[Measure]
    implicit val ErrorCodec = caseCodecAuto[Error]
    implicit val ActionCodec = caseCodecAuto[Action]
    sealedTraitCodecAuto[Stat]
  }

  implicit val pushCodec = {
    implicit val StatPushCodec = caseCodecAuto[StatMsg]
    sealedTraitCodecAuto[Push]
  }
}

@RestrictedN(10)
sealed trait Push

@N(1) final case class StatMsg(@N(1) stat: Stat, @N(2) meta: StatMeta) extends Push

sealed trait Stat
@N(1) final case class Metric(@N(1) name: String, @N(2) value: String) extends Stat
@N(2) final case class Measure(@N(1) name: String, @N(2) value: String) extends Stat
@N(3) final case class Error(@N(1) exception: String, @N(2) stacktrace: String, @N(3) toptrace: String) extends Stat
@N(4) final case class Action(@N(1) action: String) extends Stat

final case class StatMeta(@N(1) time: String, @N(2) host: String, @N(3) ip: String)
