module Api.Pull
  ( Pull(..)
  , HealthAsk
  , encodePull
  ) where

import Data.Eq (class Eq)
import Prelude (($))
import Proto.Encode as Encode
import Proto.Uint8Array (Uint8Array, length, concatAll)

data Pull = HealthAsk HealthAsk
derive instance eqPull :: Eq Pull
type HealthAsk = { host :: String }

encodePull :: Pull -> Uint8Array
encodePull (HealthAsk x) = concatAll [ Encode.unsignedVarint32 162, encodeHealthAsk x ]

encodeHealthAsk :: HealthAsk -> Uint8Array
encodeHealthAsk msg = do
  let xs = concatAll
        [ Encode.unsignedVarint32 10
        , Encode.string msg.host
        ]
  concatAll [ Encode.unsignedVarint32 $ length xs, xs ]