module Nodes
  ( reactClass
  ) where

import DomOps (cn, onClickEff)
import Effect (Effect)
import Prelude (Unit, bind, map, pure, ($), unit)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr, tr', i, a)
import React.DOM.Props (onClick, style, href)
import Schema (NodeInfo)
import Web.Socket.WebSocket (WebSocket)
import Web.HTML (window)
import Web.HTML.Window (confirm)
import WsOps as WsOps
import Pull(Pull(NodeRemove), encodePull)

type State = {}
type Props =
  { nodes :: Array NodeInfo
  , openNode :: String -> Effect Unit
  , ws :: WebSocket
  }

reactClass :: ReactClass Props
reactClass = component "Nodes" \this -> do
  pure
    { state: {}
    , render: render this
    }
  where
    render :: ReactThis Props State -> Effect ReactElement
    render this = do
      props <- getProps this
      pure $
        div [ cn "row" ]
        [ div [ cn "col-md-12" ]
          [ div [ cn "card" ]
            [ div [ cn "card-header" ]
              [ h4 [ cn "card-title" ]
                [ text "Nodes" ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "table-responsive" ]
                [ table [ cn "table tablesorter" ]
                  [ thead [ cn "text-primary" ]
                    [ tr'
                      [ th' [ text "Address" ]
                      , th' [ text "Last Update" ]
                      ]
                    ]
                  , tbody' $ map (\x ->
                      tr [ onClick \_ -> 
                           props.openNode x.addr
                         , style { cursor: "zoom-in" }
                         ]
                      [ td [ style { fontFamily: "Fira Code" } ] [ text x.addr ]
                      , td [ style { fontFamily: "Fira Code" } ] [ text x.lastUpdate ]
                      , td [ ] 
                        [ a [ href ""
                            , onClickEff $ do
                                w <- window
                                res <- confirm "Delete statistics of this node?" w
                                if res then WsOps.sendB props.ws $ encodePull $ NodeRemove {addr: x.addr}
                                else pure unit
                            ]
                          [ i [ cn "tim-icons icon-trash-simple" ] [] ] 
                        ]
                      ]) props.nodes
                  ]
                ]
              ]
            ]
          ]
        ]
