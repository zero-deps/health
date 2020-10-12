package .stats

import zd.proto.api.N

final case class EnData
  ( @N(1) value: String
  , @N(2) time: Long
  , @N(3) host: String
  )
