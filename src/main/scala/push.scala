package 
package stats

import zd.proto.api.{MessageCodec, N}
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

object Push {

  implicit val StatMetaCode: MessageCodec[StatMeta] = caseCodecAuto[StatMeta]

  implicit val StatCodec: MessageCodec[Stat] = {
    implicit val StatMetaCodec: MessageCodec[StatMeta] = caseCodecAuto[StatMeta]
    implicit val MetricCodec: MessageCodec[Metric] = caseCodecAuto[Metric]
    implicit val MeasureCodec: MessageCodec[Measure] = caseCodecAuto[Measure]
    implicit val ErrorCodec: MessageCodec[Error] = caseCodecAuto[Error]
    implicit val ActionCodec: MessageCodec[Action] = caseCodecAuto[Action]
    sealedTraitCodecAuto[Stat]
  }

  implicit val pushCodec: MessageCodec[Push] = {
    implicit val StatPushCodec: MessageCodec[StatMsg] = caseCodecAuto[StatMsg]
    implicit val NodeRemoveOkCodec: MessageCodec[NodeRemoveOk] = caseCodecAuto[NodeRemoveOk]
    implicit val NodeRemoveErrCodec: MessageCodec[NodeRemoveErr] = caseCodecAuto[NodeRemoveErr]
    sealedTraitCodecAuto[Push]
  }
}

sealed trait Push

@N(1) final case class StatMsg(@N(1) stat: Stat, @N(2) meta: StatMeta) extends Push

sealed trait Stat
@N(1) final case class Metric(@N(1) name: String, @N(2) value: String) extends Stat
@N(2) final case class Measure(@N(1) name: String, @N(2) value: String) extends Stat
@N(3) final case class Error(@N(1) exception: String, @N(2) stacktrace: String, @N(3) toptrace: String) extends Stat
@N(4) final case class Action(@N(1) action: String) extends Stat

final case class StatMeta(@N(1) time: String, @N(2) host: String, @N(3) ip: String)

@N(10) final case class NodeRemoveOk(@N(1) addr: String) extends Push
@N(11) final case class NodeRemoveErr(@N(1) addr: String, @N(2) msg: String) extends Push
