package .stats

import zd.proto._, api._, macrosapi._
import kvs._

object fid {
  sealed trait Fid
  @N(1) final case class CpuMemLive(@N(1) host: String) extends Fid
  @N(2) final case class CpuDay    (@N(1) host: String) extends Fid
  @N(3) final case class ActionLive(@N(1) host: String) extends Fid
  @N(4) final case class Metrics   (@N(1) host: String) extends Fid
  @N(5) final case class Nodes     ()                   extends Fid
  @N(6) final case class Measures  (@N(1) host: String) extends Fid
  @N(7) final case class Errors    (@N(1) host: String) extends Fid
  @N(8) final case class CommonErrors()                 extends Fid
  implicit val cpumemlivec = caseCodecAuto[CpuMemLive]
  implicit val cpuhourc = caseCodecAuto[CpuDay]
  implicit val actionlivec = caseCodecAuto[ActionLive]
  implicit val metricsc = caseCodecAuto[Metrics]
  implicit val nodesc = caseCodecAuto[Nodes]
  implicit val measuresc = caseCodecAuto[Measures]
  implicit val errorsc = caseCodecAuto[Errors]
  implicit val commonerrorsc = caseCodecAuto[CommonErrors]
  implicit val fidc = sealedTraitCodecAuto[Fid]
  def apply(x: Fid): FdKey = FdKey(encodeToBytes[Fid](x))
}

object en_id {
  final case class Host   (@N(1) host: String)
  final case class Metric (@N(1) name: String)
  final case class Measure(@N(1) name: String)
  implicit val hostc    = caseCodecAuto[Host]
  implicit val metricc  = caseCodecAuto[Metric]
  implicit val measurec = caseCodecAuto[Measure]

  def apply(x: Host   ): ElKey = ElKey(encodeToBytes(x))
  def apply(x: Metric ): ElKey = ElKey(encodeToBytes(x))
  def apply(x: Measure): ElKey = ElKey(encodeToBytes(x))

  def metric (k: ElKey): Metric  = decode[Metric] (k.bytes)
  def measure(k: ElKey): Measure = decode[Measure](k.bytes)
}
