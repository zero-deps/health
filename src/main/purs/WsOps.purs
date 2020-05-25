module WsOps where

import Control.Monad.Except (runExcept)
import Data.Array (singleton, fromFoldable)
import Proto.Uint8Array (Uint8Array)
import Data.ArrayBuffer.Types (ArrayBuffer)
import Data.Bifunctor (lmap)
import Data.Either (Either(Left, Right), either)
import Data.List.NonEmpty (toList)
import Data.Maybe (maybe)
import Effect (Effect)
import Foreign (F, Foreign, renderForeignError, unsafeReadTagged)
import Ops.ArrayBuffer (uint8Array)
import Prelude hiding (div)
import Web.Event.Event (Event)
import Web.Event.EventTarget (addEventListener, eventListener)
import Web.HTML (window) as DOM
import Web.HTML.Location (protocol, host) as DOM
import Web.HTML.Window (location) as DOM
import Web.Socket.BinaryType (BinaryType(ArrayBuffer))
import Web.Socket.Event.EventTypes (onOpen, onMessage, onError, onClose) as WS
import Web.Socket.Event.MessageEvent (MessageEvent, fromEvent, data_)
import Web.Socket.WebSocket (WebSocket)
import Web.Socket.WebSocket (create, sendArrayBufferView, toEventTarget, setBinaryType, close) as WS
import Unsafe.Coerce (unsafeCoerce)

create :: String -> Effect WebSocket
create url = WS.create url []

onOpen :: forall a. WebSocket -> (Event -> Effect a) -> Effect Unit
onOpen ws handler =
  let 
    useCapture = false
    target = WS.toEventTarget ws
  in do
    WS.setBinaryType ws ArrayBuffer
    l <- eventListener handler
    addEventListener WS.onOpen l useCapture target

send :: WebSocket -> Uint8Array -> Effect Unit
send ws = (WS.sendArrayBufferView ws) <<< unsafeCoerce

onMsg :: forall e. WebSocket -> (Uint8Array -> Effect Unit) -> (Array String -> Effect e) -> Effect Unit
onMsg ws success failure = onMsg' readArrayBuffer ws (success <<< uint8Array) failure

onMsg' :: forall e a. (Foreign -> F a) -> WebSocket -> (a -> Effect Unit) -> (Array String -> Effect e) -> Effect Unit
onMsg' f ws success failure =
  let
    useCapture = false
    target = WS.toEventTarget ws
  in do
    l <- eventListener \x -> either (void <<< failure) success $ parseEvent x
    addEventListener WS.onMessage l useCapture target
  where
    parseEvent :: Event -> Either (Array String) a
    parseEvent ev = (readMessageEvent ev) >>= parseMessageEvent f

parseMessageEvent :: forall a. (Foreign -> F a) -> MessageEvent -> Either (Array String) a
parseMessageEvent f = lmap (map renderForeignError <<< fromFoldable <<< toList) <<< runExcept <<< f <<< data_

readMessageEvent :: Event -> Either (Array String) MessageEvent
readMessageEvent = maybe (Left $ singleton "Can't get event") Right <<< fromEvent

readArrayBuffer :: Foreign -> F ArrayBuffer
readArrayBuffer = unsafeReadTagged "ArrayBuffer"
