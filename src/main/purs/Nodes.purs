module Nodes
  ( reactClass
  ) where

import Data.Array (filter, (!!))
import Data.Foldable (maximum)
import Data.Maybe (fromMaybe)
import Data.String (Pattern(Pattern), split)
import Data.String.CodePoints (length) as String
import Data.String.Common (toLower)
import DomOps (cn, onClickEff, onChangeValue)
import Effect (Effect)
import Ext.String (includes, startsWith)
import Prelude (Unit, apply, bind, map, not, pure, unit, ($), (<<<), (<=), (<>), (||))
import Pull(Pull(NodeRemove), encodePull)
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, modifyState)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr, tr', i, a, input, button)
import React.DOM.Props (onClick, style, href, _type, placeholder, value)
import Schema (NodeInfo)
import Web.HTML (window)
import Web.HTML.Window (confirm)
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps

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
    render this = ado
      props <- getProps this
      state <- getState this
      in div [ cn "row" ]
        [ div [ cn "col-md-12" ]
          [ div [ cn "card" ]
            [ div [ cn "card-header" ]
              [ h4 [ cn "card-title" ]
                [ text "Nodes" ]
              , div []
                [ button  [ _type "button"
                          , cn $ "btn btn-primary" <> if state.active then " active" else ""
                          , onClick \_ -> modifyState this \s -> s { active = not s.active }
                          ]
                  [ text "Active" ]
                , input [ _type "search"
                        , cn "form-control"
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
                      , th' [ text "IP Address" ]
                      , th' [ text "Last Update" ]
                      ]
                    ]
                  , tbody' $ map (\x ->
                      tr [ onClick \_ -> props.openNode x.host
                         , style { cursor: "pointer" }
                         ]
                      [ td [ style { fontFamily: "Fira Code" } ] [ text x.host ]
                      , td [ style { fontFamily: "Fira Code" } ] [ text x.ip ]
                      , td [ style { fontFamily: "Fira Code" } ] [ text x.lastUpdate ]
                      , td []
                        [ a [ href ""
                            , onClickEff $ do
                                w <- window
                                res <- confirm "Remove this node?" w
                                if res then WsOps.sendB props.ws $ encodePull $ NodeRemove {addr: x.host}
                                else pure unit
                            ]
                          [ i [ cn "tim-icons icon-trash-simple" ] [] ] 
                        ]
                      ]) $ activeNodes state.active $ filterNodes state.filter props.nodes
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
  in filter (\x -> includes v (toLower x.host) || includes v (toLower x.ip)) xs
  
activeNodes :: Boolean -> Array NodeInfo -> Array NodeInfo
activeNodes false xs = xs
activeNodes true xs =
  let prefix = fromMaybe "" $ maximum $ map (fromMaybe "" <<< (\x -> x !! 0) <<< split (Pattern " ") <<< _.lastUpdate) xs
  in filter (startsWith prefix <<< _.lastUpdate) xs