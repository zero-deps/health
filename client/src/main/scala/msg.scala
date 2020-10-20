package metrics.client

import zd.proto.api.{N, RestrictedN}

@RestrictedN(3)
sealed trait ClientMsg {
  val _host: Option[String] = Some(host)
  val host: String = _host.getOrElse("N/A")
  val _ipaddr: Option[String] = Some(ipaddr)
  val ipaddr: String = _ipaddr.getOrElse("N/A")
}

@N(1) @RestrictedN(3) case class MetricMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) override val _host: Option[String]
  , @N(5) override val _ipaddr: Option[String]
  ) extends ClientMsg

@N(2) @RestrictedN(3) case class MeasureMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) override val _host: Option[String]
  , @N(5) override val _ipaddr: Option[String]
  ) extends ClientMsg

@N(4) @RestrictedN(2) case class ActionMsg
  ( @N(1) action: String
  , @N(3) override val _host: Option[String]
  , @N(4) override val _ipaddr: Option[String]
  ) extends ClientMsg

@N(5) case class ErrorMsg
  ( @N(1) msg: Option[String]
  , @N(2) cause: String
  , @N(3) st: Seq[String]
  , @N(4) override val host: String
  , @N(5) override val ipaddr: String
  ) extends ClientMsg