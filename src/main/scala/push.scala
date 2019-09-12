package 
package stats
package route

import zd.proto.api.{MessageCodec, N}
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

object Push {
  implicit val pushCodec: MessageCodec[Push] = {
    implicit val StatPushCodec: MessageCodec[StatPush] = caseCodecAuto[StatPush]
    implicit val NodeRemoveOkCodec: MessageCodec[NodeRemoveOk] = caseCodecAuto[NodeRemoveOk]
    implicit val NodeRemoveErrCodec: MessageCodec[NodeRemoveErr] = caseCodecAuto[NodeRemoveErr]
    sealedTraitCodecAuto[Push]
  }
}

sealed trait Push

@N(1) final case class StatPush(@N(1) stat: StatMsg) extends Push

@N(10) final case class NodeRemoveOk(@N(1) addr: String) extends Push
@N(11) final case class NodeRemoveErr(@N(1) addr: String, @N(2) msg: String) extends Push
