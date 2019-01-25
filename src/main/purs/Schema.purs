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
  , cpu :: Maybe String
  , mem :: Maybe String
  , action :: Maybe String
  , err :: Maybe ErrorInfo
  }

type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  , cpuLoad :: Array CpuPoint
  , memLoad :: Array MemPoint
  , actions :: Array ActionPoint
  , cpuLast :: String
  , memLast :: String
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