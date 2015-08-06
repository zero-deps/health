package .stats

case class Metric(name: String, node: String, param: String, time: String, value: String) {
  def serialize(): String = s"$name#$node#$param#$time#$value"
}

case class Message(casino: String, user: String, msg: String, time: String) {
  def serialize(): String = s"$casino#$user#$msg#$time"
}
