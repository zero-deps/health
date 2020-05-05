module FormatOps where

import Prelude

import Effect (Effect)
import Global (readInt)

import Data.Int (floor)
import Data.JSDate (fromTime, getUTCDate, getUTCFullYear, getUTCHours, getUTCMinutes, getUTCMonth, getUTCSeconds, getTimezoneOffset, now)
import Data.String.CodePoints (length)

foreign import formatNum :: Number -> String

dateTime :: Number -> Effect String
dateTime ms' = do
  timezone <- now >>= getTimezoneOffset
  let ms = ms' - timezone * 60.0 * 1000.0
  let d = fromTime ms
  let day = datePart $ getUTCDate d
  let month = datePart $ (getUTCMonth d) + 1.0
  let year = datePart $ getUTCFullYear d
  let hours = datePart $ getUTCHours d
  let minutes = datePart $ getUTCMinutes d
  let seconds = datePart $ getUTCSeconds d
  pure $ year<>"."<>month<>"."<>day<>" "<>hours<>":"<>minutes<>":"<>seconds
  where
  datePart :: Number -> String
  datePart num =
    let str = show $ floor num
    in if length str < 2 then "0"<>str else str

duration :: String -> { value :: String, unit :: String }
duration sec = let sec' = floor $ readInt 10 sec in
  if sec' >= 3600 then { value: show $ sec' / 3600, unit: "hour" }
  else if sec' >= 60 then { value: show $ sec' / 60, unit: "min" }
  else { value: show sec', unit: "sec" }

milliseconds :: String -> String
milliseconds ms = case readInt 10 ms of
  x | x < 100000.0 -> formatNum x <> " ms"
  _ -> let y = duration ms in y.value <> " " <> y.unit
