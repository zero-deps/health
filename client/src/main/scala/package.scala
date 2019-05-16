package .stats

import zd.proto.api.{N, MessageCodec}
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

package object client {
  sealed trait ClientMsg
  @N(1) final case class MetricMsg
    ( @N(1) name: String
    , @N(2) value: String
    , @N(3) port: String) extends ClientMsg
  @N(2) final case class MeasureMsg
    ( @N(1) name: String
    , @N(2) value: String
    , @N(3) port: String) extends ClientMsg
  @N(3) final case class ErrorMsg
    ( @N(1) exception: String
    , @N(2) stacktrace: String
    , @N(3) toptrace: String
    , @N(4) port: String) extends ClientMsg
  @N(4) final case class ActionMsg
    ( @N(1) action: String
    , @N(2) port: String) extends ClientMsg

  implicit val ClientMsgCodec: MessageCodec[ClientMsg] = {
    implicit val MetricMsgCodec: MessageCodec[MetricMsg] = caseCodecAuto[MetricMsg]
    implicit val MeasureMsgCodec: MessageCodec[MeasureMsg] = caseCodecAuto[MeasureMsg]
    implicit val ErrorMsgCodec: MessageCodec[ErrorMsg] = caseCodecAuto[ErrorMsg]
    implicit val ActionMsgCodec: MessageCodec[ActionMsg] = caseCodecAuto[ActionMsg]
    sealedTraitCodecAuto[ClientMsg]
  }
}
