module Api.Pull
  ( Pull(..)
  , NodeRemove
  , encodePull
  ) where

import Prelude (($))
import Proto.Encode as Encode
import Proto.Uint8Array (Uint8Array, length, concatAll)

data Pull = NodeRemove NodeRemove
type NodeRemove = { addr :: String }

encodePull :: Pull -> Uint8Array
encodePull (NodeRemove x) = concatAll [ Encode.uint32 82, encodeNodeRemove x ]

encodeNodeRemove :: NodeRemove -> Uint8Array
encodeNodeRemove msg = do
  let xs = concatAll
        [ Encode.uint32 10
        , Encode.string msg.addr
        ]
  concatAll [ Encode.uint32 $ length xs, xs ]