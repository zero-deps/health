module Nodes
  ( reactClass
  ) where

import DomOps (cn)
import Effect (Effect)
import Prelude (Unit, bind, map, pure, ($))
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr, tr')
import React.DOM.Props (onClick, style)
import Schema (NodeInfo)

type State = {}
type Props =
  { nodes :: Array NodeInfo
  , openNode :: String -> Effect Unit
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
                      tr [ onClick \_ -> props.openNode x.addr, style { cursor: "zoom-in" } ]
                      [ td [ style { fontFamily: "Fira Code" } ] [ text x.addr ]
                      , td [ style { fontFamily: "Fira Code" } ] [ text x.lastUpdate ]
                      ]) props.nodes
                  ]
                ]
              ]
            ]
          ]
        ]
