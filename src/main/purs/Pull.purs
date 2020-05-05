module Pull where

import Data.ArrayBuffer.Types (Uint8Array)
import Data.Tuple (Tuple(Tuple))
import Proto.Encode as Encode
import Proto.Uint8ArrayExt (length, concatAll)

encodeStringString :: Tuple String String -> Uint8Array
encodeStringString (Tuple k v) = do
  let xs = concatAll [ Encode.uint32 10, Encode.string k, Encode.uint32 18, Encode.string v ]
  let len = length xs
  concatAll [ Encode.uint32 len, xs ]

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
  let len = length xs
  concatAll [ Encode.uint32 len, xs ]
