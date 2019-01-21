module Errors
  ( reactClass
  , ErrorInfo
  ) where

import DateOps (localDateTime)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr', div')
import React.DOM.Props (className)

type State = 
  {
  }
type ErrorInfo =
  { exception :: Array String
  , stacktrace :: Array String
  , file :: String
  , time :: String
  , addr :: String
  }
type Props =
  { errors :: Array ErrorInfo
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
                [ text "Errors" ]
              ]
            , div [ className "card-body" ]
              [ div [ className "table-responsive" ]
                [ table [ className "table tablesorter" ]
                  [ thead [ className "text-primary" ]
                    [ tr'
                      [ th' [ text "Address" ]
                      , th' [ text "Time" ]
                      , th' [ text "Exception" ]
                      , th' [ text "Stacktrace" ]
                      ]
                    ]
                  , tbody' $ map (\x ->
                      tr'
                      [ td [ className "align-top" ] [ text x.addr ]
                      , td [ className "align-top" ] [ text $ localDateTime x.time ]
                      , td [ className "align-top" ] $ map (\y -> div' [ text y ]) x.exception
                      , td [ className "align-top" ] [ text x.file ]
                      -- , td [ className "align-top" ] $ map (\y -> div' [ text y ]) x.stacktrace
                      ]) props.errors
                  ]
                ]
              ]
            ]
          ]
        ]
