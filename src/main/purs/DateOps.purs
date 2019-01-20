module DateOps
  ( localDateTime
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
  day = datePart $ getUTCDate d
  month = datePart $ (getUTCMonth d) + 1.0
  year = datePart $ getUTCFullYear d
  hours = datePart $ getUTCHours d
  minutes = datePart $ getUTCMinutes d
  seconds = datePart $ getUTCSeconds d
  in day<>"."<>month<>"."<>year<>" "<>hours<>":"<>minutes<>":"<>seconds

datePart :: Number -> String
datePart num =
  let str = show $ floor num
  in if length str < 2 then "0"<>str else str
