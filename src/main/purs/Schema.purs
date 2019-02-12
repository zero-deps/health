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
  { cpu_mem :: Maybe (Array String)
  , uptime :: Maybe String
  , fs :: Maybe (Array String)
  , fd :: Maybe (Array String)
  , thr :: Maybe (Array String)
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuPoints :: Array CpuPoint
  , memPoints :: Array MemPoint
  , actionPoints :: Array ActionPoint
  , uptime :: Maybe String
  , cpuLast :: Maybe String
  , memLast :: Maybe Number
  , memFree :: Maybe Number
  , memTotal :: Maybe Number
  , fs :: Maybe FsInfo
  , fd :: Maybe FdInfo
  , thr :: Maybe ThrInfo
  }

type FsInfo =
  { used :: Number
  , usable :: Number
  , total :: Number
  }

type FdInfo =
  { open :: Number
  , max :: Number
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
