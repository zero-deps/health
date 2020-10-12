package .stats

import zd.proto._, api._, macrosapi._
import kvs._

object fid {
  sealed trait Fid
  @N( 1) final case class CpuMemLive(@N(1) host: String) extends Fid
  @N( 2) final case class CpuHour(@N(1) host: String) extends Fid
  @N( 3) final case class SearchTs(@N(1) host: String) extends Fid
  @N( 4) final case class SearchWc(@N(1) host: String) extends Fid
  @N( 5) final case class SearchFs(@N(1) host: String) extends Fid
  @N( 6) final case class StaticGen(@N(1) host: String) extends Fid
  @N( 7) final case class ReindexAll(@N(1) host: String) extends Fid
  @N( 8) final case class StaticGenYear(@N(1) host: String) extends Fid
  @N( 9) final case class KvsSizeYear(@N(1) host: String) extends Fid
  @N(10) final case class ActionLive(@N(1) host: String) extends Fid
  @N(11) final case class Metrics(@N(1) host: String) extends Fid
  @N(12) final case class Nodes() extends Fid
  @N(13) final case class MeasureLatest(@N(1) name: String, @N(2) host: String) extends Fid
  @N(14) final case class MeasureYear(@N(1) name: String, @N(2) host: String) extends Fid
  @N(15) final case class Feature() extends Fid
  @N(16) final case class Errors(@N(1) host: String) extends Fid
  @N(17) final case class CommonErrors() extends Fid
  @N(18) final case class CommonErrorsStacks() extends Fid
  implicit val cpumemlivec = caseCodecAuto[CpuMemLive]
  implicit val cpuhourc = caseCodecAuto[CpuHour]
  implicit val searchtsc = caseCodecAuto[SearchTs]
  implicit val searchwcc = caseCodecAuto[SearchWc]
  implicit val searchfsc = caseCodecAuto[SearchFs]
  implicit val staticgenc = caseCodecAuto[StaticGen]
  implicit val reindexallc = caseCodecAuto[ReindexAll]
  implicit val staticgenyearc = caseCodecAuto[StaticGenYear]
  implicit val kvssizeyearc = caseCodecAuto[KvsSizeYear]
  implicit val actionlivec = caseCodecAuto[ActionLive]
  implicit val metricsc = caseCodecAuto[Metrics]
  implicit val nodesc = caseCodecAuto[Nodes]
  implicit val measurelatestc = caseCodecAuto[MeasureLatest]
  implicit val measureyearc = caseCodecAuto[MeasureYear]
  implicit val featurec = caseCodecAuto[Feature]
  implicit val errorsc = caseCodecAuto[Errors]
  implicit val commonerrorsc = caseCodecAuto[CommonErrors]
  implicit val commonerrorsstacksc = caseCodecAuto[CommonErrorsStacks]
  implicit val fidc = sealedTraitCodecAuto[Fid]

  def apply(x: Fid): FdKey = FdKey(encodeToBytes[Fid](x))
}

object en_id {
  final case class Str(@N(1) unwrap: String)
  final case class Metric(@N(1) name: String)
  final case class IntV(@N(1) unwrap: Int)
  final case class Feature(@N(1) name: String, @N(2) host: String, @N(3) i: Int)
  implicit val strc = caseCodecAuto[Str]
  implicit val metricc = caseCodecAuto[Metric]
  implicit val intc = caseCodecAuto[IntV]
  implicit val featurec = caseCodecAuto[Feature]
  def str(x: String): ElKey = ElKey(encodeToBytes(Str(x)))
  def int(x: Int): ElKey = ElKey(encodeToBytes(IntV(x)))

  def apply(x: Feature): ElKey = ElKey(encodeToBytes(x))
  def apply(x: Metric): ElKey = ElKey(encodeToBytes(x))

  def feature(k: ElKey): Feature = decode[Feature](k.bytes)
  def metric(k: ElKey): Metric = decode[Metric](k.bytes)
  def str(k: ElKey): String = decode[Str](k.bytes).unwrap
}

object el_id {
  sealed trait ElId
  @N( 1) final case class CpuMemLiveIdx(@N(1) host: String) extends ElId
  @N( 2) final case class CpuHourT(@N(1) host: String, @N(2) i: Int) extends ElId
  @N( 3) final case class CpuHourN(@N(1) host: String, @N(2) i: Int) extends ElId
  @N( 4) final case class CpuHourV(@N(1) host: String, @N(2) i: Int) extends ElId
  @N( 5) final case class ActionLiveIdx(@N(1) host: String) extends ElId
  @N( 6) final case class MeasureLatestIdx(@N(1) name: String, @N(2) host: String) extends ElId
  @N( 7) final case class MeasureYearT(@N(1) name: String, @N(2) host: String, @N(3) i: Int) extends ElId
  @N( 8) final case class MeasureYearN(@N(1) name: String, @N(2) host: String, @N(3) i: Int) extends ElId
  @N( 9) final case class MeasureYearV(@N(1) name: String, @N(2) host: String, @N(3) i: Int) extends ElId
  @N(10) final case class FeatureT(@N(1) name: String, @N(2) host: String, @N(3) i: Int) extends ElId
  @N(11) final case class FeatureN(@N(1) name: String, @N(2) host: String, @N(3) i: Int) extends ElId
  @N(12) final case class ErrorsIdx(@N(1) host: String) extends ElId
  @N(13) final case class CommonErrorsIdx() extends ElId
  implicit val cpumemliveidxc = caseCodecAuto[CpuMemLiveIdx]
  implicit val cpuhourtc = caseCodecAuto[CpuHourT]
  implicit val cpuhournc = caseCodecAuto[CpuHourN]
  implicit val cpuhourvc = caseCodecAuto[CpuHourV]
  implicit val actionliveidxc = caseCodecAuto[ActionLiveIdx]
  implicit val measurelatestidxc = caseCodecAuto[MeasureLatestIdx]
  implicit val measureyeartc = caseCodecAuto[MeasureYearT]
  implicit val measureyearnc = caseCodecAuto[MeasureYearN]
  implicit val measureyearvc = caseCodecAuto[MeasureYearV]
  implicit val featuretc = caseCodecAuto[FeatureT]
  implicit val featurenc = caseCodecAuto[FeatureN]
  implicit val errorsidxc = caseCodecAuto[ErrorsIdx]
  implicit val commonerrorsidxc = caseCodecAuto[CommonErrorsIdx]
  implicit val elidc = sealedTraitCodecAuto[ElId]
  def apply(x: ElId): ElKey = ElKey(encodeToBytes[ElId](x))
}

object el_v {
  final case class IntV(@N(1) x: Int)
  final case class LongV(@N(1) x: Long)
  final case class FloatV(@N(1) x: Float)
  implicit val intc = caseCodecAuto[IntV]
  implicit val longc = caseCodecAuto[LongV]
  implicit val floatc = caseCodecAuto[FloatV]
  def int(x: Int): Bytes = encodeToBytes(IntV(x))
  def int(x: Bytes): Int = decode[IntV](x).x
  def long(x: Long): Bytes = encodeToBytes(LongV(x))
  def long(x: Bytes): Long = decode[LongV](x).x
  def float(x: Float): Bytes = encodeToBytes(FloatV(x))
  def float(x: Bytes): Float = decode[FloatV](x).x
}
