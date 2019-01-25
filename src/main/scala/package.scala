package 

package object stats {
  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class ErrorStat(exception: String, stacktrace: String, toptrace: String) extends Stat
  final case class ActionStat(action: String) extends Stat

  final case class StatMeta(time: String, addr: String)

  final case class Msg(stat: Stat, meta: StatMeta)

  def now_ms(): String = System.currentTimeMillis.toString
}
