module Api where

import Data.Array (snoc)
import Data.ArrayBuffer.Types (Uint8Array)
import Data.Either (Either(Left, Right))
import Data.Int.Bits (zshr, (.&.))
import Data.Map (Map)
import Data.Map as Map
import Data.Maybe (Maybe(Just, Nothing))
import Data.Tuple (Tuple(Tuple), fst, snd)
import Prelude (bind, pure, ($), (+), (<))
import Proto.Decode as Decode

decodeStringString :: Uint8Array -> Int -> Decode.Result (Tuple String String)
decodeStringString _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { first: Nothing, second: Nothing } pos
  case val of
    { first: Just first, second: Just second } -> pure { pos: pos1, val: Tuple first second }
    _ -> Left $ Decode.MissingFields "StringString"
    where
    decode :: Int -> { first :: Maybe String, second :: Maybe String } -> Int -> Decode.Result { first :: Maybe String, second :: Maybe String }
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { first = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { second = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

data StatMsg = MetricStat MetricStat | MeasureStat MeasureStat | ErrorStat ErrorStat | ActionStat ActionStat
type MetricStat = { name :: String, value :: String, time :: String, addr :: String }
type MetricStat' = { name :: Maybe String, value :: Maybe String, time :: Maybe String, addr :: Maybe String }
type MeasureStat = { name :: String, value :: String, time :: String, addr :: String }
type MeasureStat' = { name :: Maybe String, value :: Maybe String, time :: Maybe String, addr :: Maybe String }
type ErrorStat = { exception :: String, stacktrace :: String, toptrace :: String, time :: String, addr :: String }
type ErrorStat' = { exception :: Maybe String, stacktrace :: Maybe String, toptrace :: Maybe String, time :: Maybe String, addr :: Maybe String }
type ActionStat = { action :: String, time :: String, addr :: String }
type ActionStat' = { action :: Maybe String, time :: Maybe String, addr :: Maybe String }

decodeStatMsg :: Uint8Array -> Decode.Result StatMsg
decodeStatMsg _xs_ = do
  { pos: pos1, val: tag } <- Decode.uint32 _xs_ 0
  case tag `zshr` 3 of
    1 -> do
      { pos: pos2, val } <- decodeMetricStat _xs_ pos1
      pure { pos: pos2, val: MetricStat val }
    2 -> do
      { pos: pos2, val } <- decodeMeasureStat _xs_ pos1
      pure { pos: pos2, val: MeasureStat val }
    3 -> do
      { pos: pos2, val } <- decodeErrorStat _xs_ pos1
      pure { pos: pos2, val: ErrorStat val }
    4 -> do
      { pos: pos2, val } <- decodeActionStat _xs_ pos1
      pure { pos: pos2, val: ActionStat val }
    i ->
      Left $ Decode.BadType i

decodeMetricStat :: Uint8Array -> Int -> Decode.Result MetricStat
decodeMetricStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { name: Nothing, value: Nothing, time: Nothing, addr: Nothing } pos
  case val of
    { name: Just name, value: Just value, time: Just time, addr: Just addr } -> pure { pos: pos1, val: { name, value, time, addr } }
    _ -> Left $ Decode.MissingFields "MetricStat"
    where
    decode :: Int -> MetricStat' -> Int -> Decode.Result MetricStat'
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { name = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { value = Just val }) pos3
              3 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { time = Just val }) pos3
              4 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { addr = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeMeasureStat :: Uint8Array -> Int -> Decode.Result MeasureStat
decodeMeasureStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { name: Nothing, value: Nothing, time: Nothing, addr: Nothing } pos
  case val of
    { name: Just name, value: Just value, time: Just time, addr: Just addr } -> pure { pos: pos1, val: { name, value, time, addr } }
    _ -> Left $ Decode.MissingFields "MeasureStat"
    where
    decode :: Int -> MeasureStat' -> Int -> Decode.Result MeasureStat'
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { name = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { value = Just val }) pos3
              3 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { time = Just val }) pos3
              4 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { addr = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeErrorStat :: Uint8Array -> Int -> Decode.Result ErrorStat
decodeErrorStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { exception: Nothing, stacktrace: Nothing, toptrace: Nothing, time: Nothing, addr: Nothing } pos
  case val of
    { exception: Just exception, stacktrace: Just stacktrace, toptrace: Just toptrace, time: Just time, addr: Just addr } -> pure { pos: pos1, val: { exception, stacktrace, toptrace, time, addr } }
    _ -> Left $ Decode.MissingFields "ErrorStat"
    where
    decode :: Int -> ErrorStat' -> Int -> Decode.Result ErrorStat'
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { exception = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { stacktrace = Just val }) pos3
              3 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { toptrace = Just val }) pos3
              4 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { time = Just val }) pos3
              5 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { addr = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeActionStat :: Uint8Array -> Int -> Decode.Result ActionStat
decodeActionStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { action: Nothing, time: Nothing, addr: Nothing } pos
  case val of
    { action: Just action, time: Just time, addr: Just addr } -> pure { pos: pos1, val: { action, time, addr } }
    _ -> Left $ Decode.MissingFields "ActionStat"
    where
    decode :: Int -> ActionStat' -> Int -> Decode.Result ActionStat'
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { action = Just val }) pos3
              4 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { time = Just val }) pos3
              5 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { addr = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }
