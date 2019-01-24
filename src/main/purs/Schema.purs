module Schema where

import Data.Maybe (Maybe)

type NodeAddr = String

type ErrorInfo =
  { exception :: Array String
  , stacktrace :: Array String
  , file :: String
  , time :: String
  , addr :: String
  }

type UpdateData =
  { addr :: String
  , time :: String
  , cpu :: Maybe Number
  , err :: Maybe ErrorInfo
  , action :: Maybe String
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuLoad :: Array CpuPoint
  , cpuLast :: String
  , actions :: Array ActionPoint
  }

type CpuPoint =
  { t :: Number
  , y :: Number
  }

type ActionPoint =
  { t :: Number
  , label :: String
  }
