module Nodes
  ( reactClass
  , NodeInfo
  ) where

import DateOps (localDateTime)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (div, h4, table, tbody', td', text, th', thead, tr')
import React.DOM.Props (className)

type State = 
  {
  }
type NodeInfo =
  { addr :: String
  , lastUpdate :: String
  }
type Props =
  { nodes :: Array NodeInfo
  }

reactClass :: ReactClass Props
reactClass = component "Errors" \this -> do
  pure
    { state:
      {
      }
    , render: render this
    }
  where
    render :: ReactThis Props State -> Effect ReactElement
    render this = do
      props <- getProps this
      pure $
        div [ className "row" ]
        [ div [ className "col-md-12" ]
          [ div [ className "card" ]
            [ div [ className "card-header" ]
              [ h4 [ className "card-title" ]
                [ text "Nodes" ]
              ]
            , div [ className "card-body" ]
              [ div [ className "table-responsive" ]
                [ table [ className "table tablesorter" ]
                  [ thead [ className "text-primary" ]
                    [ tr'
                      [ th' [ text "Address" ]
                      , th' [ text "Last Update" ]
                      ]
                    ]
                  , tbody' $ map (\x ->
                      tr'
                      [ td' [ text x.addr ]
                      , td' [ text $ localDateTime x.lastUpdate ]
                      ]) props.nodes
                  ]
                ]
              ]
            ]
          ]
        ]
