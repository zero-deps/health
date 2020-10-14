module FormatOps where

import Prelude
import Effect (Effect)
import Effect.Uncurried (EffectFn1, runEffectFn1)
import Global (readInt)

import Data.Int (floor)
import Data.JSDate (JSDate, fromTime)

foreign import formatNum :: Number -> String

foreign import formatLocal :: forall a. { | a } -> EffectFn1 JSDate String
formatLocal' :: forall a. { | a } -> JSDate -> Effect String
formatLocal' o d = runEffectFn1 (formatLocal o) d

dateTime :: Number -> Effect String
dateTime ms = do
  let d = fromTime ms
  formatLocal' { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" } d

duration :: String -> { value :: String, unit :: String }
duration sec = let sec' = floor $ readInt 10 sec in
  if sec' >= 3600 then { value: show $ sec' / 3600, unit: "hour" }
  else if sec' >= 60 then { value: show $ sec' / 60, unit: "min" }
  else { value: show sec', unit: "sec" }

milliseconds :: String -> String
milliseconds ms = case readInt 10 ms of
  x | x < 100000.0 -> formatNum x <> " ms"
  _ -> let y = duration ms in y.value <> " " <> y.unit
