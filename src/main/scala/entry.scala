package .stats

import zd.proto.api.N

final case class StatEn
  ( @N(1) fid: String
  , @N(2) id: String
  , @N(3) prev: String
  , @N(4) data: String
  , @N(5) time: String
  , @N(6) host: String
  , @N(7) ip: String
  ) extends zd.kvs.en.En
