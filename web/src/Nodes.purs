module Nodes
  ( reactClass
  ) where

import Data.Array (filter)
import Data.Foldable (maximum)
import Data.Maybe (fromMaybe)
import Data.String.CodePoints (length) as String
import Data.String.Common (toLower)
import DomOps (cn, onChangeValue)
import Effect (Effect)
import Ext.String (includes)
import Prelude (Unit, discard, bind, map, not, pure, unit, ($), (<=), (>), (-), (<>))
import Api.Pull(Pull(HealthAsk), encodePull)
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, modifyState)
import React.DOM (div, table, tbody', td, text, th', thead, tr, tr', input, button)
import React.DOM.Props (onClick, style, _type, placeholder, value, key)
import Schema (NodeInfo)
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps
import Data.Traversable (sequence)
import FormatOps (dateTime)

type State = 
  { filter :: String
  , active :: Boolean
  }

type Props =
  { nodes :: Array NodeInfo
  , openNode :: String -> Effect Unit
  , ws :: WebSocket
  }

reactClass :: ReactClass Props
reactClass = component "Nodes" \this -> do
  pure
    { state:
      { filter: ""
      , active: true
      }
    , render: render this
    }
  where
    render :: ReactThis Props State -> Effect ReactElement
    render this = do
      props <- getProps this
      state <- getState this
      rows <- sequence $ map (\x -> do
        lastUpdate <- dateTime x.lastUpdate_ms
        pure $
          tr  [ key x.host
              , onClick \_ -> do
                  if x.historyLoaded then pure unit
                  else WsOps.send props.ws $ encodePull $ HealthAsk { host: x.host }
                  props.openNode x.host
              , style { cursor: "pointer" }
              ]
          [ td [ style { fontFamily: "Fira Code" } ] [ text x.host ]
          , td [ style { fontFamily: "Fira Code" } ] [ text lastUpdate ]
          ]
      ) $ activeNodes state.active $ filterNodes state.filter props.nodes
      pure $ div [ cn "row" ]
        [ div [ cn "col-md-12" ]
          [ div [ cn "card" ]
            [ div [ cn "card-header" ]
              [ div [ cn "form-inline" ]
                [ button  [ _type "button"
                          , cn $ "btn" <> if state.active then " btn-primary" else " btn-outline-primary"
                          , onClick \_ -> modifyState this \s -> s { active = not s.active }
                          ]
                  [ text "Active" ]
                , input [ _type "search"
                        , cn "form-control ml-2"
                        , placeholder "Filter nodes"
                        , value state.filter
                        , onChangeValue \v -> modifyState this _{ filter = v }
                        ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "table-responsive" ]
                [ table [ cn "table tablesorter" ]
                  [ thead [ cn "text-primary" ]
                    [ tr'
                      [ th' [ text "Host Name" ]
                      , th' [ text "Last Update" ]
                      ]
                    ]
                  , tbody' rows
                  ]
                ]
              ]
            ]
          ]
        ]

filterNodes :: String -> Array NodeInfo -> Array NodeInfo
filterNodes v xs | String.length v <= 2 = xs
filterNodes v' xs =
  let v = toLower v'
  in filter (\x -> includes v (toLower x.host)) xs
  
activeNodes :: Boolean -> Array NodeInfo -> Array NodeInfo
activeNodes false xs = xs
activeNodes true xs =
  let max_time = fromMaybe 0.0 $ maximum $ map _.lastUpdate_ms xs
  in filter (\x -> x.lastUpdate_ms > max_time - 3600000.0) xs
