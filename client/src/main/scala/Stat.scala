package .stats.client

sealed trait Stat
final case class MetricStat(name: String, value: String) extends Stat
final case class ErrorStat(exception: String, stacktrace: String) extends Stat
final case class ActionStat(action: String) extends Stat
