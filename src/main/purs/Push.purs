module Push where

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
import CommonApi

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

data Push = StatMsg StatMsg | NodeRemoveOk NodeRemoveOk | NodeRemoveErr NodeRemoveErr
type StatMsg = { stat :: Stat, meta :: StatMeta }
type StatMsg' = { stat :: Maybe Stat, meta :: Maybe StatMeta }
data Stat = Metric Metric | Measure Measure | Error Error | Action Action
type Metric = { name :: String, value :: String }
type Metric' = { name :: Maybe String, value :: Maybe String }
type Measure = { name :: String, value :: String }
type Measure' = { name :: Maybe String, value :: Maybe String }
type Error = { exception :: String, stacktrace :: String, toptrace :: String }
type Error' = { exception :: Maybe String, stacktrace :: Maybe String, toptrace :: Maybe String }
type Action = { action :: String }
type Action' = { action :: Maybe String }
type StatMeta = { time :: String, host :: String, ip :: String }
type StatMeta' = { time :: Maybe String, host :: Maybe String, ip :: Maybe String }
type NodeRemoveOk = { addr :: String }
type NodeRemoveOk' = { addr :: Maybe String }
type NodeRemoveErr = { addr :: String, msg :: String }
type NodeRemoveErr' = { addr :: Maybe String, msg :: Maybe String }

decodePush :: Uint8Array -> Decode.Result Push
decodePush _xs_ = do
  { pos: pos1, val: tag } <- Decode.uint32 _xs_ 0
  case tag `zshr` 3 of
    1 -> do
      { pos: pos2, val } <- decodeStatMsg _xs_ pos1
      pure { pos: pos2, val: StatMsg val }
    10 -> do
      { pos: pos2, val } <- decodeNodeRemoveOk _xs_ pos1
      pure { pos: pos2, val: NodeRemoveOk val }
    11 -> do
      { pos: pos2, val } <- decodeNodeRemoveErr _xs_ pos1
      pure { pos: pos2, val: NodeRemoveErr val }
    i ->
      Left $ Decode.BadType i

decodeStatMsg :: Uint8Array -> Int -> Decode.Result StatMsg
decodeStatMsg _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { stat: Nothing, meta: Nothing } pos
  case val of
    { stat: Just stat, meta: Just meta } -> pure { pos: pos1, val: { stat, meta } }
    _ -> Left $ Decode.MissingFields "StatMsg"
    where
    decode :: Int -> StatMsg' -> Int -> Decode.Result StatMsg'
    decode end acc pos1 =
      if pos1 < end then
        case Decode.uint32 _xs_ pos1 of
          Left x -> Left x
          Right { pos: pos2, val: tag } ->
            case tag `zshr` 3 of
              1 ->
                case decodeStat _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { stat = Just val }) pos3
              2 ->
                case decodeStatMeta _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { meta = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeStat :: Uint8Array -> Int -> Decode.Result Stat
decodeStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  decode end Nothing pos
    where
    decode :: Int -> Maybe Stat -> Int -> Decode.Result Stat
    decode end acc pos1 | pos1 < end =
      case Decode.uint32 _xs_ pos1 of
        Left x -> Left x
        Right { pos: pos2, val: tag } ->
          case tag `zshr` 3 of
            1 ->
              case decodeMetric _xs_ pos2 of
                Left x -> Left x
                Right { pos: pos3, val } ->
                  decode end (Just $ Metric val) pos3
            2 ->
              case decodeMeasure _xs_ pos2 of
                Left x -> Left x
                Right { pos: pos3, val } ->
                  decode end (Just $ Measure val) pos3
            3 ->
              case decodeError _xs_ pos2 of
                Left x -> Left x
                Right { pos: pos3, val } ->
                  decode end (Just $ Error val) pos3
            4 ->
              case decodeAction _xs_ pos2 of
                Left x -> Left x
                Right { pos: pos3, val } ->
                  decode end (Just $ Action val) pos3
            _ ->
              case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                Left x -> Left x
                Right { pos: pos3 } ->
                  decode end acc pos3
    decode end (Just acc) pos1 = pure { pos: pos1, val: acc }
    decode end acc@Nothing pos1 = Left $ Decode.MissingFields "Stat"

decodeMetric :: Uint8Array -> Int -> Decode.Result Metric
decodeMetric _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Metric"
    where
    decode :: Int -> Metric' -> Int -> Decode.Result Metric'
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
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeMeasure :: Uint8Array -> Int -> Decode.Result Measure
decodeMeasure _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Measure"
    where
    decode :: Int -> Measure' -> Int -> Decode.Result Measure'
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
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeError :: Uint8Array -> Int -> Decode.Result Error
decodeError _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { exception: Nothing, stacktrace: Nothing, toptrace: Nothing } pos
  case val of
    { exception: Just exception, stacktrace: Just stacktrace, toptrace: Just toptrace } -> pure { pos: pos1, val: { exception, stacktrace, toptrace } }
    _ -> Left $ Decode.MissingFields "Error"
    where
    decode :: Int -> Error' -> Int -> Decode.Result Error'
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
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeAction :: Uint8Array -> Int -> Decode.Result Action
decodeAction _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { action: Nothing } pos
  case val of
    { action: Just action } -> pure { pos: pos1, val: { action } }
    _ -> Left $ Decode.MissingFields "Action"
    where
    decode :: Int -> Action' -> Int -> Decode.Result Action'
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
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeStatMeta :: Uint8Array -> Int -> Decode.Result StatMeta
decodeStatMeta _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { time: Nothing, host: Nothing, ip: Nothing } pos
  case val of
    { time: Just time, host: Just host, ip: Just ip } -> pure { pos: pos1, val: { time, host, ip } }
    _ -> Left $ Decode.MissingFields "StatMeta"
    where
    decode :: Int -> StatMeta' -> Int -> Decode.Result StatMeta'
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
                    decode end (acc { time = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { host = Just val }) pos3
              3 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { ip = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeNodeRemoveOk :: Uint8Array -> Int -> Decode.Result NodeRemoveOk
decodeNodeRemoveOk _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { addr: Nothing } pos
  case val of
    { addr: Just addr } -> pure { pos: pos1, val: { addr } }
    _ -> Left $ Decode.MissingFields "NodeRemoveOk"
    where
    decode :: Int -> NodeRemoveOk' -> Int -> Decode.Result NodeRemoveOk'
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
                    decode end (acc { addr = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }

decodeNodeRemoveErr :: Uint8Array -> Int -> Decode.Result NodeRemoveErr
decodeNodeRemoveErr _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  let end = pos + msglen
  { pos: pos1, val } <- decode end { addr: Nothing, msg: Nothing } pos
  case val of
    { addr: Just addr, msg: Just msg } -> pure { pos: pos1, val: { addr, msg } }
    _ -> Left $ Decode.MissingFields "NodeRemoveErr"
    where
    decode :: Int -> NodeRemoveErr' -> Int -> Decode.Result NodeRemoveErr'
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
                    decode end (acc { addr = Just val }) pos3
              2 ->
                case Decode.string _xs_ pos2 of
                  Left x -> Left x
                  Right { pos: pos3, val } ->
                    decode end (acc { msg = Just val }) pos3
              _ ->
                case Decode.skipType _xs_ pos2 $ tag .&. 7 of
                  Left x -> Left x
                  Right { pos: pos3 } ->
                    decode end acc pos3
      else pure { pos: pos1, val: acc }
