module Api.Push
  ( Push(..)
  , StatMsg
  , Stat(..)
  , Metric
  , Measure
  , Error
  , defaultError
  , Action
  , HostMsg
  , decodePush
  ) where

import Control.Monad.Rec.Class (Step(Loop, Done), tailRecM3)
import Data.Array (snoc)
import Data.Either (Either(Left))
import Data.Eq (class Eq)
import Data.Int.Bits (zshr, (.&.))
import Data.Maybe (Maybe(Just, Nothing))
import Prelude (map, bind, pure, ($), (+), (<), (<<<))
import Proto.Decode as Decode
import Proto.Uint8Array (Uint8Array)

decodeFieldLoop :: forall a b c. Int -> Decode.Result a -> (a -> b) -> Decode.Result' (Step { a :: Int, b :: b, c :: Int } { pos :: Int, val :: c })
decodeFieldLoop end res f = map (\{ pos, val } -> Loop { a: end, b: f val, c: pos }) res

data Push = StatMsg StatMsg | HostMsg HostMsg
derive instance eqPush :: Eq Push
type StatMsg = { stat :: Stat, time :: Number, host :: String }
type StatMsg' = { stat :: Maybe Stat, time :: Maybe Number, host :: Maybe String }
data Stat = Metric Metric | Measure Measure | Error Error | Action Action
derive instance eqStat :: Eq Stat
type Metric = { name :: String, value :: String }
type Metric' = { name :: Maybe String, value :: Maybe String }
type Measure = { name :: String, value :: String }
type Measure' = { name :: Maybe String, value :: Maybe String }
type Error = { msg :: Maybe String, cause :: Maybe String, st :: Array String }
defaultError :: { msg :: Maybe String, cause :: Maybe String, st :: Array String }
defaultError = { msg: Nothing, cause: Nothing, st: [] }
type Action = { action :: String }
type Action' = { action :: Maybe String }
type HostMsg = { host :: String, time :: Number }
type HostMsg' = { host :: Maybe String, time :: Maybe Number }

decodePush :: Uint8Array -> Decode.Result Push
decodePush _xs_ = do
  { pos: pos1, val: tag } <- Decode.unsignedVarint32 _xs_ 0
  case tag `zshr` 3 of
    1 -> decode (decodeStatMsg _xs_ pos1) StatMsg
    2 -> decode (decodeHostMsg _xs_ pos1) HostMsg
    i -> Left $ Decode.BadType i
  where
  decode :: forall a. Decode.Result a -> (a -> Push) -> Decode.Result Push
  decode res f = map (\{ pos, val } -> { pos, val: f val }) res

decodeStatMsg :: Uint8Array -> Int -> Decode.Result StatMsg
decodeStatMsg _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { stat: Nothing, time: Nothing, host: Nothing } pos
  case val of
    { stat: Just stat, time: Just time, host: Just host } -> pure { pos: pos1, val: { stat, time, host } }
    _ -> Left $ Decode.MissingFields "StatMsg"
    where
    decode :: Int -> StatMsg' -> Int -> Decode.Result' (Step { a :: Int, b :: StatMsg', c :: Int } { pos :: Int, val :: StatMsg' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (decodeStat _xs_ pos2) \val -> acc { stat = Just val }
        2 -> decodeFieldLoop end (Decode.signedVarint64 _xs_ pos2) \val -> acc { time = Just val }
        3 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { host = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeStat :: Uint8Array -> Int -> Decode.Result Stat
decodeStat _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  tailRecM3 decode (pos + msglen) Nothing pos
    where
    decode :: Int -> Maybe Stat -> Int -> Decode.Result' (Step { a :: Int, b :: Maybe Stat, c :: Int } { pos :: Int, val :: Stat })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
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
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Metric"
    where
    decode :: Int -> Metric' -> Int -> Decode.Result' (Step { a :: Int, b :: Metric', c :: Int } { pos :: Int, val :: Metric' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { name = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { value = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeMeasure :: Uint8Array -> Int -> Decode.Result Measure
decodeMeasure _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { name: Nothing, value: Nothing } pos
  case val of
    { name: Just name, value: Just value } -> pure { pos: pos1, val: { name, value } }
    _ -> Left $ Decode.MissingFields "Measure"
    where
    decode :: Int -> Measure' -> Int -> Decode.Result' (Step { a :: Int, b :: Measure', c :: Int } { pos :: Int, val :: Measure' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { name = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { value = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeError :: Uint8Array -> Int -> Decode.Result Error
decodeError _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  tailRecM3 decode (pos + msglen) { msg: Nothing, cause: Nothing, st: [] } pos
    where
    decode :: Int -> Error -> Int -> Decode.Result' (Step { a :: Int, b :: Error, c :: Int } { pos :: Int, val :: Error })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { msg = Just val }
        2 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { cause = Just val }
        3 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { st = snoc acc.st val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeAction :: Uint8Array -> Int -> Decode.Result Action
decodeAction _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { action: Nothing } pos
  case val of
    { action: Just action } -> pure { pos: pos1, val: { action } }
    _ -> Left $ Decode.MissingFields "Action"
    where
    decode :: Int -> Action' -> Int -> Decode.Result' (Step { a :: Int, b :: Action', c :: Int } { pos :: Int, val :: Action' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { action = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }

decodeHostMsg :: Uint8Array -> Int -> Decode.Result HostMsg
decodeHostMsg _xs_ pos0 = do
  { pos, val: msglen } <- Decode.unsignedVarint32 _xs_ pos0
  { pos: pos1, val } <- tailRecM3 decode (pos + msglen) { host: Nothing, time: Nothing } pos
  case val of
    { host: Just host, time: Just time } -> pure { pos: pos1, val: { host, time } }
    _ -> Left $ Decode.MissingFields "HostMsg"
    where
    decode :: Int -> HostMsg' -> Int -> Decode.Result' (Step { a :: Int, b :: HostMsg', c :: Int } { pos :: Int, val :: HostMsg' })
    decode end acc pos1 | pos1 < end = do
      { pos: pos2, val: tag } <- Decode.unsignedVarint32 _xs_ pos1
      case tag `zshr` 3 of
        1 -> decodeFieldLoop end (Decode.string _xs_ pos2) \val -> acc { host = Just val }
        2 -> decodeFieldLoop end (Decode.signedVarint64 _xs_ pos2) \val -> acc { time = Just val }
        _ -> decodeFieldLoop end (Decode.skipType _xs_ pos2 $ tag .&. 7) \_ -> acc
    decode end acc pos1 = pure $ Done { pos: pos1, val: acc }