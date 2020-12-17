package metrics
package api

import zd.proto._, api._, macrosapi._

sealed trait Msg

@N(1) case class MetricMsg
  ( @N(1) name: String
  , @N(2) value: String
  ) extends Msg

object MetricMsg {
  def uptime ( sec   : Long  ): MetricMsg = MetricMsg(name="uptime"  , value=s"$sec")
  def cpu_mem( cpu   : String
             , free  : Long
             , total : Long
             , heap  : Long   ): MetricMsg = MetricMsg(name="cpu_mem" , value=s"$cpu~$free~$total~$heap")
  def threads( all   : Int
             , daemon: Int
             , peak  : Int
             , total : Long  ): MetricMsg = MetricMsg(name="thr"     , value=s"$all~$daemon~$peak~$total")
  def dbSize ( size  : Long  ): MetricMsg = MetricMsg(name="kvs.size", value=s"$size")
  def fileD  ( open  : Long
             , max   : Long  ): MetricMsg = MetricMsg(name="fd"      , value=s"$open~$max")
  def fs     ( usable: Long
             , total : Long  ): MetricMsg = MetricMsg(name="fs./"    , value=s"$usable~$total")
}

@N(2) case class MeasureMsg
  ( @N(1) name: String
  , @N(2) value: Int
  ) extends Msg

@N(4) case class ActionMsg
  ( @N(1) action: String
  ) extends Msg

object ActionMsg {
  def gc(name: String, t: Long): ActionMsg = ActionMsg(s"$name in $t ms")
}

@N(5) case class ErrorMsg
  ( @N(1) msg: Option[String]
  , @N(2) cause: Option[String]
  , @N(3) st: Seq[String]
  ) extends Msg

object Msg {
  implicit val MsgCodec = {
    implicit val MetricMsgCodec  = caseCodecAuto[MetricMsg]
    implicit val MeasureMsgCodec = caseCodecAuto[MeasureMsg]
    implicit val ErrorMsgCodec   = caseCodecAuto[ErrorMsg]
    implicit val ActionMsgCodec  = caseCodecAuto[ActionMsg]
    sealedTraitCodecAuto[Msg]
  }
}
