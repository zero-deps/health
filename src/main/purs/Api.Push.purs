module Api.Push
  ( Push(..)
  , StatMsg
  , Stat(..)
  , Metric
  , Measure
  , Error
  , Action
  , StatMeta
  , decodePush
  ) where

import Control.Monad.Rec.Class (Step(Loop, Done), tailRecM3)
import Data.Either (Either(Left))
import Data.Eq (class Eq)
import Data.Int.Bits (zshr, (.&.))
import Data.Maybe (Maybe(Just, Nothing))
import Prelude (map, bind, pure, ($), (+), (<), (<<<))
import Proto.Decode as Decode
import Proto.Uint8Array (Uint8Array)

decodeFieldLoop :: forall a b c. Int -> Decode.Result a -> (a -> b) -> Decode.Result' (Step { a :: Int, b :: b, c :: Int } { pos :: Int, val :: c })
decodeFieldLoop end res f = map (\{ pos, val } -> Loop { a: end, b: f val, c: pos }) res

data Push = StatMsg StatMsg
type StatMsg = { stat :: Stat, meta :: StatMeta }
type StatMsg' = { stat :: Maybe Stat, meta :: Maybe StatMeta }
data Stat = Metric Metric | Measure Measure | Error Error | Action Action
derive instance eqStat :: Eq Stat
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

decodePush :: Uint8Array -> Decode.Result Push
decodePush _xs_ = do
  { pos: pos1, val: tag } <- Decode.uint32 _xs_ 0
  case tag `zshr` 3 of
    1 -> decode (decodeStatMsg _xs_ pos1) StatMsg
    i -> Left $ Decode.BadType i
  where
  decode :: forall a. Decode.Result a -> (a -> Push) -> Decode.Result Push
  decode res f = map (\{ pos, val } -> { pos, val: f val }) res

decodeStatMsg :: Uint8Array -> Int -> Decode.Result StatMsg
decodeStatMsg _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { stat: Nothing, meta: Nothing } pos
  case val of
    { stat: Just stat, meta: Just meta } -> pure { pos: pos1, val: { stat, meta } }
    _ -> Left $ Decode.MissingFields "StatMsg"
    where
    decode :: Int -> StatMsg' -> Int -> Decode.Result' (Step { a :: Int, b :: StatMsg', c :: Int } { pos :: Int, val :: StatMsg' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (decodeStat _xs_ pos2) \val -> acc { stat = Just val }
        2 -> decodeFieldLoop end (decodeStatMeta _xs_ pos2) \val -> acc { meta = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeStat :: Uint8Array -> Int -> Decode.Result Stat
decodeStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  tailRecM3 decode (pos + msglen) Nothing pos
    where
    decode :: Int -> Maybe Stat -> Int -> Decode.Result' (Step { a :: Int, b :: Maybe Stat, c :: Int } { pos :: Int, val :: Stat })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (decodeMetric _xs_ pos2) (Just <<< Metric)
        2 -> decodeFieldLoop end (decodeMeasure _xs_ pos2) (Just <<< Measure)
        3 -> decodeFieldLoop end (decodeError _xs_ pos2) (Just <<< Error)
        4 -> decodeFieldLoop end (decodeAction _xs_ pos2) (Just <<< Action)
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end (Just acc) pos1 = pure $ Done { pos: pos1, val: acc }
    decode end acc@Nothing pos1 = Left $ Decode.MissingFields "Stat"

decodeMetric :: Uint8Array -> Int -> Decode.Result Metric
decodeMetric _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Metric"
    where
    decode :: Int -> Metric' -> Int -> Decode.Result' (Step { a :: Int, b :: Metric', c :: Int } { pos :: Int, val :: Metric' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { name = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { value = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeMeasure :: Uint8Array -> Int -> Decode.Result Measure
decodeMeasure _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Measure"
    where
    decode :: Int -> Measure' -> Int -> Decode.Result' (Step { a :: Int, b :: Measure', c :: Int } { pos :: Int, val :: Measure' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { name = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { value = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeError :: Uint8Array -> Int -> Decode.Result Error
decodeError _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { exception: Nothing, stacktrace: Nothing, toptrace: Nothing } pos
  case val of
    { exception: Just exception, stacktrace: Just stacktrace, toptrace: Just toptrace } -> pure { pos: pos1, val: { exception, stacktrace, toptrace } }
    _ -> Left $ Decode.MissingFields "Error"
    where
    decode :: Int -> Error' -> Int -> Decode.Result' (Step { a :: Int, b :: Error', c :: Int } { pos :: Int, val :: Error' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { exception = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { stacktrace = Just val }
        3 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { toptrace = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeAction :: Uint8Array -> Int -> Decode.Result Action
decodeAction _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { action: Nothing } pos
  case val of
    { action: Just action } -> pure { pos: pos1, val: { action } }
    _ -> Left $ Decode.MissingFields "Action"
    where
    decode :: Int -> Action' -> Int -> Decode.Result' (Step { a :: Int, b :: Action', c :: Int } { pos :: Int, val :: Action' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { action = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeStatMeta :: Uint8Array -> Int -> Decode.Result StatMeta
decodeStatMeta _xs_ pos0 = do
  { pos, val: msglen } <- Decode.uint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { time: Nothing, host: Nothing, ip: Nothing } pos
  case val of
    { time: Just time, host: Just host, ip: Just ip } -> pure { pos: pos1, val: { time, host, ip } }
    _ -> Left $ Decode.MissingFields "StatMeta"
    where
    decode :: Int -> StatMeta' -> Int -> Decode.Result' (Step { a :: Int, b :: StatMeta', c :: Int } { pos :: Int, val :: StatMeta' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.uint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { time = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { host = Just val }
        3 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { ip = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }