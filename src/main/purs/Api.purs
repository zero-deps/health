module Api where

import Data.ArrayBuffer.Types (Uint8Array)
import Data.Int.Bits (zshr, (.&.))
import Data.Maybe (Maybe(Just, Nothing))
import Effect (Effect)
import Prelude (bind, discard, pure, ($), (+), (<))
import Proto (Reader, createReader, pos, skipType, string, uint32)

data StatMsg = MetricStat MetricStat | MeasureStat MeasureStat | ErrorStat ErrorStat | ActionStat ActionStat
type MetricStat = { name :: String, value :: String, time :: String, addr :: String }
type MeasureStat = { name :: String, value :: String, time :: String, addr :: String }
type ErrorStat = { exception :: String, stacktrace :: String, toptrace :: String, time :: String, addr :: String }
type ActionStat = { action :: String, time :: String, addr :: String }

decodeStatMsg :: Uint8Array -> Effect (Maybe StatMsg)
decodeStatMsg bytes = do
  let reader = createReader bytes
  tag <- uint32 reader
  case zshr tag 3 of
    1 -> do
      msglen <- uint32 reader
      x <- decodeMetricStat reader msglen
      pure $ Just $ MetricStat x
    2 -> do
      msglen <- uint32 reader
      x <- decodeMeasureStat reader msglen
      pure $ Just $ MeasureStat x
    3 -> do
      msglen <- uint32 reader
      x <- decodeErrorStat reader msglen
      pure $ Just $ ErrorStat x
    4 -> do
      msglen <- uint32 reader
      x <- decodeActionStat reader msglen
      pure $ Just $ ActionStat x
    _ ->
      pure Nothing

decodeMetricStat :: Reader -> Int -> Effect MetricStat
decodeMetricStat reader msglen = do
  let end = pos reader + msglen
  decode end { name: "", value: "", time: "", addr: "" }
  where
    decode :: Int -> MetricStat -> Effect MetricStat
    decode end acc =
      if pos reader < end then do
        tag <- uint32 reader
        case zshr tag 3 of
          1 -> do
            x <- string reader
            decode end $ acc { name = x }
          2 -> do
            x <- string reader
            decode end $ acc { value = x }
          3 -> do
            x <- string reader
            decode end $ acc { time = x }
          4 -> do
            x <- string reader
            decode end $ acc { addr = x }
          _ -> do
            skipType reader $ tag .&. 7
            decode end acc
      else pure acc

decodeMeasureStat :: Reader -> Int -> Effect MeasureStat
decodeMeasureStat reader msglen = do
  let end = pos reader + msglen
  decode end { name: "", value: "", time: "", addr: "" }
  where
    decode :: Int -> MeasureStat -> Effect MeasureStat
    decode end acc =
      if pos reader < end then do
        tag <- uint32 reader
        case zshr tag 3 of
          1 -> do
            x <- string reader
            decode end $ acc { name = x }
          2 -> do
            x <- string reader
            decode end $ acc { value = x }
          3 -> do
            x <- string reader
            decode end $ acc { time = x }
          4 -> do
            x <- string reader
            decode end $ acc { addr = x }
          _ -> do
            skipType reader $ tag .&. 7
            decode end acc
      else pure acc

decodeErrorStat :: Reader -> Int -> Effect ErrorStat
decodeErrorStat reader msglen = do
  let end = pos reader + msglen
  decode end { exception: "", stacktrace: "", toptrace: "", time: "", addr: "" }
  where
    decode :: Int -> ErrorStat -> Effect ErrorStat
    decode end acc =
      if pos reader < end then do
        tag <- uint32 reader
        case zshr tag 3 of
          1 -> do
            x <- string reader
            decode end $ acc { exception = x }
          2 -> do
            x <- string reader
            decode end $ acc { stacktrace = x }
          3 -> do
            x <- string reader
            decode end $ acc { toptrace = x }
          4 -> do
            x <- string reader
            decode end $ acc { time = x }
          5 -> do
            x <- string reader
            decode end $ acc { addr = x }
          _ -> do
            skipType reader $ tag .&. 7
            decode end acc
      else pure acc

decodeActionStat :: Reader -> Int -> Effect ActionStat
decodeActionStat reader msglen = do
  let end = pos reader + msglen
  decode end { action: "", time: "", addr: "" }
  where
    decode :: Int -> ActionStat -> Effect ActionStat
    decode end acc =
      if pos reader < end then do
        tag <- uint32 reader
        case zshr tag 3 of
          1 -> do
            x <- string reader
            decode end $ acc { action = x }
          4 -> do
            x <- string reader
            decode end $ acc { time = x }
          5 -> do
            x <- string reader
            decode end $ acc { addr = x }
          _ -> do
            skipType reader $ tag .&. 7
            decode end acc
      else pure acc
