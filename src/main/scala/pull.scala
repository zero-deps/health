package 
package stats

import zd.proto.api.{MessageCodec, N}
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

object Pull {
  implicit val pullCodec: MessageCodec[Pull] = {
    implicit val NodeRemoveCodec: MessageCodec[NodeRemove] = caseCodecAuto[NodeRemove]
    sealedTraitCodecAuto[Pull]
  }
}

sealed trait Pull

@N(10) final case class NodeRemove(@N(1) addr: String) extends Pull
