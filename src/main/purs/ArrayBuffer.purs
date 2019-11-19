module Ops.ArrayBuffer where

import Data.ArrayBuffer.Types (ArrayBuffer, Uint8Array)

foreign import uint8Array :: ArrayBuffer -> Uint8Array
