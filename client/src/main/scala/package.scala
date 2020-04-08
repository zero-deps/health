package .stats

import zd.proto.api.{N, RestrictedN, MessageCodec}
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

package object client {
  sealed trait ClientMsg
  @N(1) @RestrictedN(3) final case class MetricMsg
    ( @N(1) name: String
    , @N(2) value: String
    , @N(4) hostname: Option[String]
    , @N(5) ipaddr: Option[String]
    ) extends ClientMsg
  @N(2) @RestrictedN(3) final case class MeasureMsg
    ( @N(1) name: String
    , @N(2) value: String
    , @N(4) hostname: Option[String]
    , @N(5) ipaddr: Option[String]
    ) extends ClientMsg
  @N(3) @RestrictedN(4) final case class ErrorMsg
    ( @N(1) exception: String
    , @N(2) stacktrace: String
    , @N(3) toptrace: String
    , @N(5) hostname: Option[String]
    , @N(6) ipaddr: Option[String]
    ) extends ClientMsg
  @N(4) @RestrictedN(2) final case class ActionMsg
    ( @N(1) action: String
    , @N(3) hostname: Option[String]
    , @N(4) ipaddr: Option[String]
    ) extends ClientMsg

  implicit val ClientMsgCodec: MessageCodec[ClientMsg] = {
    implicit val MetricMsgCodec: MessageCodec[MetricMsg] = caseCodecAuto[MetricMsg]
    implicit val MeasureMsgCodec: MessageCodec[MeasureMsg] = caseCodecAuto[MeasureMsg]
    implicit val ErrorMsgCodec: MessageCodec[ErrorMsg] = caseCodecAuto[ErrorMsg]
    implicit val ActionMsgCodec: MessageCodec[ActionMsg] = caseCodecAuto[ActionMsg]
    sealedTraitCodecAuto[ClientMsg]
  }
}
