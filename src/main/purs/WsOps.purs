module WsOps
  ( create
  , onOpen
  , onMsg
  , send
  ) where

import Control.Monad.Except (runExcept)
import Data.Bifunctor (lmap)
import Data.Either (Either(Left, Right), either)
import Data.Functor (map)
import Data.List (List, singleton)
import Data.List.NonEmpty (toList)
import Data.Maybe (maybe)
import Effect (Effect)
import Foreign (readString, renderForeignError)
import Prelude (Unit, bind, discard, ($), (<<<), (>>=))
import Web.Event.Event (Event)
import Web.Event.EventTarget (addEventListener, eventListener)
import Web.Socket.BinaryType (BinaryType(ArrayBuffer))
import Web.Socket.Event.EventTypes (onOpen, onMessage) as WS
import Web.Socket.Event.MessageEvent (MessageEvent, fromEvent, data_)
import Web.Socket.WebSocket (create, sendString, setBinaryType, toEventTarget) as WS
import Web.Socket.WebSocket (WebSocket)

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

onMsg :: WebSocket -> (String -> Effect Unit) -> (List String -> Effect Unit) -> Effect Unit
onMsg ws success failure =
  let 
    useCapture = false
    target = WS.toEventTarget ws
  in do 
    l <- eventListener \x -> either failure success $ readEvent x
    addEventListener WS.onMessage l useCapture target
  where
    readEvent :: Event -> Either (List String) String
    readEvent e = (messageEvent e) >>= string
    string :: MessageEvent -> Either (List String) String
    string = lmap (map renderForeignError <<< toList) <<< runExcept <<< readString <<< data_
    messageEvent :: Event -> Either (List String) MessageEvent
    messageEvent = maybe (Left $ singleton "Can't get event") Right <<< fromEvent

send :: WebSocket -> String -> Effect Unit
send ws payload = WS.sendString ws payload
