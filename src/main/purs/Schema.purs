module Schema where

import Data.Maybe (Maybe)

type NodeAddr = String

type ErrorInfo =
  { exception :: Array String
  , stacktrace :: Array String
  , toptrace :: String
  , time :: String
  , addr :: String
  , key :: String -- for React
  }

type UpdateData =
  { addr :: String
  , time :: String
  , metrics :: Maybe MetricsUpdate
  , action :: Maybe String
  , err :: Maybe ErrorInfo
  }

type MetricsUpdate =
  { cpu :: Maybe String
  , mem :: Maybe (Array String)
  , uptime :: Maybe String
  , fs :: Maybe (Array String)
  , fd :: Maybe (Array String)
  , thr :: Maybe (Array String)
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuLoad :: Array CpuPoint
  , memLoad :: Array MemPoint
  , actions :: Array ActionPoint
  , cpuLast :: Maybe String
  , memLast :: Maybe Number
  , uptime :: Maybe String
  , memFree :: Maybe Number
  , memTotal :: Maybe Number
  , fsUsed :: Maybe Number
  , fsFree :: Maybe Number
  , fsTotal :: Maybe Number
  , fdOpen :: Maybe Number
  , fdMax :: Maybe Number
  , thr :: Maybe ThrInfo
  }

type ThrInfo =
  { all :: Number
  , daemon :: Number
  , nondaemon :: Number
  , peak :: Number
  , total :: Number
  }

type CpuPoint =
  { t :: Number
  , y :: Number
  }

type MemPoint =
  { t :: Number
  , y :: Number
  }

type ActionPoint =
  { t :: Number
  , label :: String
  }
