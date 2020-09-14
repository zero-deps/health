module Api.Pull
  ( Pull(..)
  , HealthAsk
  , encodePull
  ) where

import Prelude (($))
import Proto.Encode as Encode
import Proto.Uint8Array (Uint8Array, length, concatAll)

data Pull = HealthAsk HealthAsk
type HealthAsk = { host :: String }

encodePull :: Pull -> Uint8Array
encodePull (HealthAsk x) = concatAll [ Encode.uint32 162, encodeHealthAsk x ]

encodeHealthAsk :: HealthAsk -> Uint8Array
encodeHealthAsk msg = do
  let xs = concatAll
        [ Encode.uint32 10
        , Encode.string msg.host
        ]
  concatAll [ Encode.uint32 $ length xs, xs ]