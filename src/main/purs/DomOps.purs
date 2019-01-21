module DomOps
  ( byId
  , host
  , openUrl
  ) where

import Data.Maybe (Maybe(Nothing, Just))
import Data.String (null)
import Effect (Effect)
import Effect.Exception (throw)
import Prelude
import Web.DOM.Element (Element)
import Web.DOM.NonElementParentNode (NonElementParentNode, getElementById)
import Web.HTML (window)
import Web.HTML.HTMLDocument (HTMLDocument, toNonElementParentNode)
import Web.HTML.Location (host, setHref) as DOM
import Web.HTML.Window (document, location)

byId :: String -> Effect Element
byId id = do
  d :: NonElementParentNode <- map toNonElementParentNode doc
  e :: Maybe Element <- getElementById id d
  case e of
    Just e' -> pure e'
    Nothing -> throw $ "not found="<>id

doc :: Effect HTMLDocument
doc = window >>= document

host :: Effect (Maybe String)
host = do
  l <- window >>= location
  h <- DOM.host l
  pure $ if null h then Nothing else Just h

openUrl :: String -> Effect Unit
openUrl url = window >>= location >>= DOM.setHref url
