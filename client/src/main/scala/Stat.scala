package .stats.client

sealed trait Stat
final case class MetricStat(name: String, value: String) extends Stat
final case class ErrorStat(className: String, message: String, stacktrace: String) extends Stat
final case class ActionStat(user: String, action: String) extends Stat
