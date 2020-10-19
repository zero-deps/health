module Schema where

import Data.Maybe (Maybe)
import Prelude (class Eq)

type NodeAddr = String
type Feature = String

type ErrorInfo =
  { exception :: Array String
  , stacktrace :: Array String
  , time :: Number
  , host :: String
  , key :: String -- for React
  }

type UpdateData =
  { host :: String
  , time :: Number
  , metrics :: Maybe MetricsUpdate
  , measure :: Maybe MeasureUpdate
  , action :: Maybe String
  , err :: Maybe ErrorInfo
  , feature :: Maybe String
  }

type MetricsUpdate =
  { cpu_mem :: Maybe (Array String)
  , cpu_hour :: Maybe String
  , uptime :: Maybe String
  , version :: Maybe String
  , fs :: Maybe (Array String)
  , fd :: Maybe (Array String)
  , thr :: Maybe (Array String)
  , name :: String
  , value :: String
  }

type MeasureUpdate =
  { searchTs :: Maybe String
  , searchTs_thirdQ :: Maybe String
  , searchWc :: Maybe String
  , searchWc_thirdQ :: Maybe String
  , searchFs :: Maybe String
  , searchFs_thirdQ :: Maybe String
  , staticGen :: Maybe String
  , staticGen_thirdQ :: Maybe String
  , staticGen_year :: Maybe String
  , reindexAll :: Maybe String
  , reindexAll_thirdQ :: Maybe String
  }

type NodeInfo =
  { host :: String
  , ipaddr :: String
  , lastUpdate_ms :: Number
  , historyLoaded :: Boolean
  , nodeData :: Maybe NodeData
  }

type NodeData =
  { version :: Maybe String
  , cpuPoints :: Array NumPoint
  , cpuHourPoints :: Array NumPoint
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
  , searchTs_points :: Array { t :: String, y :: Number }
  , searchTs_thirdQ :: Maybe String
  , searchWc_points :: Array { t :: String, y :: Number }
  , searchWc_thirdQ :: Maybe String
  , searchFs_points :: Array { t :: String, y :: Number }
  , searchFs_thirdQ :: Maybe String
  , reindexAll_points :: Array { t :: String, y :: Number }
  , reindexAll_thirdQ :: Maybe String
  , staticGen_points :: Array { t :: String, y :: Number }
  , staticGenYear_points :: Array NumPoint
  , staticGen_thirdQ :: Maybe String
  , importLog :: Array { t :: String, msg :: String }
  , metrics :: Array { name :: String, value :: String }
  }

data ChartRange = Live | Hour
derive instance eqChartRange :: Eq ChartRange

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
