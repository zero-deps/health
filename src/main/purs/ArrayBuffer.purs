module Ops.ArrayBuffer where

import Proto.Uint8Array (Uint8Array)
import Data.ArrayBuffer.Types (ArrayBuffer)

foreign import uint8Array :: ArrayBuffer -> Uint8Array
