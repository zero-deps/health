package .stats

case class Metric(name: String, node: String, param: String, time: String, value: String) {
  lazy val key = s"$name#$node#$param"
  lazy val serialize = s"$name#$node#$param#$time#$value"
}
