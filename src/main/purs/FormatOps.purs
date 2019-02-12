module FormatOps
  ( localDateTime
  , formatNum
  , duration
  ) where

import Data.Int (floor)
import Data.JSDate (fromTime, getUTCDate, getUTCFullYear, getUTCHours, getUTCMinutes, getUTCMonth, getUTCSeconds)
import Data.String.CodePoints (length)
import Global (readInt)
import Prelude

localDateTime :: String -> String
localDateTime x = let
  ms = readInt 10 x
  d = fromTime ms
  day = datePart $ floor $ getUTCDate d
  month = datePart $ floor $ (getUTCMonth d) + 1.0
  year = datePart $ floor $ getUTCFullYear d
  hours = datePart $ floor $ getUTCHours d
  minutes = datePart $ floor $ getUTCMinutes d
  seconds = datePart $ floor $ getUTCSeconds d
  in day<>"."<>month<>"."<>year<>" "<>hours<>":"<>minutes<>":"<>seconds

datePart :: Int -> String
datePart num =
  let str = show num
  in if length str < 2 then "0"<>str else str

foreign import formatNum :: Number -> String

duration :: String -> { value :: String, unit :: String }
duration sec = let sec' = floor $ readInt 10 sec in
  if sec' >= 3600 then { value: show $ sec' / 3600, unit: "hour" }
  else if sec' >= 60 then { value: show $ sec' / 60, unit: "min" }
  else { value: show sec', unit: "sec" }
