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
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuLoad :: Array CpuPoint
  , memLoad :: Array MemPoint
  , actions :: Array ActionPoint
  , cpuLast :: String
  , memLast :: Number
  , uptime :: String
  , memFree :: Number
  , memTotal :: Number
  , fsUsed :: Number
  , fsFree :: Number
  , fsTotal :: Number
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
