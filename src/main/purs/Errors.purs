module Errors
  ( reactClass
  , ErrorInfo
  ) where

import DateOps (localDateTime)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, createLeafElement, modifyState)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr', div')
import React.DOM.Props (className, onClick, style)

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
                  , tbody' $ map (createLeafElement errorReactClass) props.errors
                  ]
                ]
              ]
            ]
          ]
        ]

type ErrorState =
  { expandStack :: Boolean
  }
type ErrorProps = ErrorInfo

errorReactClass :: ReactClass ErrorProps
errorReactClass = component "Error" \this -> do
  pure
    { state:
      { expandStack: false
      }
    , render: render this
    }
  where
    render :: ReactThis ErrorProps ErrorState -> Effect ReactElement
    render this = do
      s <- getState this
      props <- getProps this
      pure $
        tr'
        [ td [ className "align-top" ] [ text props.addr ]
        , td [ className "align-top" ] [ text $ localDateTime props.time ]
        , td [ className "align-top" ] $ map (\y -> div' [ text y ]) props.exception
        , case s.expandStack of
            false -> 
              td [ className "align-top", onClick \_ -> fullStack, style { cursor: "zoom-in" } ]
              [ text props.file ]
            true -> 
              td [ className "align-top", onClick \_ -> shortStack, style { cursor: "zoom-out" } ] $ map (\y -> 
                div' [ text y ]) props.stacktrace
        ]
      where
      fullStack :: Effect Unit
      fullStack = modifyState this _{ expandStack = true }
      shortStack :: Effect Unit
      shortStack = modifyState this _{ expandStack = false }
