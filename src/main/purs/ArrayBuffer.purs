module Ops.ArrayBuffer where

import Data.ArrayBuffer.Types (ArrayBuffer, Uint8Array)
import Prelude
import Unsafe.Coerce (unsafeCoerce)

foreign import uint8Array :: ArrayBuffer -> Uint8Array
