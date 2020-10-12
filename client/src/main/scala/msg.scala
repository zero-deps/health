package .stats.client

import zd.proto.api.{N, RestrictedN}

sealed trait ClientMsg {
  val _host: Option[String]
  val host: String = _host.getOrElse("")
  val _ipaddr: Option[String]
  val ipaddr: String = _ipaddr.getOrElse("")
}

@N(1) @RestrictedN(3) final case class MetricMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) _host: Option[String]
  , @N(5) _ipaddr: Option[String]
  ) extends ClientMsg

@N(2) @RestrictedN(3) final case class MeasureMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) _host: Option[String]
  , @N(5) _ipaddr: Option[String]
  ) extends ClientMsg

@N(3) @RestrictedN(3, 4) final case class ErrorMsg
  ( @N(1) exception: String
  , @N(2) stacktrace: String
  , @N(5) _host: Option[String]
  , @N(6) _ipaddr: Option[String]
  ) extends ClientMsg

@N(4) @RestrictedN(2) final case class ActionMsg
  ( @N(1) action: String
  , @N(3) _host: Option[String]
  , @N(4) _ipaddr: Option[String]
  ) extends ClientMsg
