package .stats.client

import zd.proto.api.{N, RestrictedN}

sealed trait ClientMsg

@N(1) @RestrictedN(3) final case class MetricMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) hostname: Option[String]
  , @N(5) ipaddr: Option[String]
  ) extends ClientMsg

@N(2) @RestrictedN(3) final case class MeasureMsg
  ( @N(1) name: String
  , @N(2) value: String
  , @N(4) hostname: Option[String]
  , @N(5) ipaddr: Option[String]
  ) extends ClientMsg

@N(3) @RestrictedN(3, 4) final case class ErrorMsg
  ( @N(1) exception: String
  , @N(2) stacktrace: String
  , @N(5) hostname: Option[String]
  , @N(6) ipaddr: Option[String]
  ) extends ClientMsg

@N(4) @RestrictedN(2) final case class ActionMsg
  ( @N(1) action: String
  , @N(3) hostname: Option[String]
  , @N(4) ipaddr: Option[String]
  ) extends ClientMsg
