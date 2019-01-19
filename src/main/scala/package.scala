package 

package object stats {
  sealed trait Stat
  final case class MetricStat(name: String, value: String) extends Stat
  final case class ErrorStat(className: String, message: String, stacktrace: String) extends Stat
  final case class ActionStat(user: String, action: String) extends Stat

  final case class StatMeta(time: String, sys: String, addr: String)

  type Msg = (Stat, StatMeta)

  def now_ms(): String = System.currentTimeMillis.toString
}
