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
  , measure :: Maybe MeasureUpdate
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

type MeasureUpdate =
  { searchTs :: Maybe String
  , searchTs_thirdQ :: Maybe String
  , searchWc :: Maybe String
  , searchWc_thirdQ :: Maybe String
  , staticCreate :: Maybe String
  , staticCreate_thirdQ :: Maybe String
  , staticGen :: Maybe String
  , staticGen_thirdQ :: Maybe String
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuPoints :: Array NumPoint
  , memPoints :: Array NumPoint
  , actPoints :: Array StrPoint
  , uptime :: Maybe String
  , cpuLast :: Maybe String
  , memLast :: Maybe Number
  , memFree :: Maybe Number
  , memTotal :: Maybe Number
  , fs :: Maybe FsInfo
  , fd :: Maybe FdInfo
  , thr :: Maybe ThrInfo
  , errs :: Array ErrorInfo
  , searchTs_points :: Array NumPoint
  , searchTs_thirdQ :: Maybe String
  , searchWc_points :: Array NumPoint
  , searchWc_thirdQ :: Maybe String
  , staticCreate_points :: Array NumPoint
  , staticCreate_thirdQ :: Maybe String
  , staticGen_points :: Array NumPoint
  , staticGen_thirdQ :: Maybe String
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

type NumPoint =
  { t :: Number
  , y :: Number
  }

type StrPoint =
  { t :: Number
  , label :: String
  }
