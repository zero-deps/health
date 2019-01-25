module Errors
  ( reactClass
  ) where

import DateOps (localDateTime)
import DomOps (cn)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, createLeafElement, modifyState)
import React.DOM (div, h4, table, tbody', td, text, th', thead, tr', div')
import React.DOM.Props (onClick, style)
import Schema

type State = {}
type Props =
  { errors :: Array ErrorInfo
  }

reactClass :: ReactClass Props
reactClass = component "Errors" \this -> do
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
                [ text "Errors" ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "table-responsive" ]
                [ table [ cn "table tablesorter" ]
                  [ thead [ cn "text-primary" ]
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
        [ td [ cn "align-top" ] [ text props.addr ]
        , td [ cn "align-top" ] [ text $ localDateTime props.time ]
        , td [ cn "align-top" ] $ map (\y -> div' [ text y ]) props.exception
        , case s.expandStack of
            false -> 
              td [ cn "align-top", onClick \_ -> fullStack, style { cursor: "zoom-in", fontFamily: "Fira Code", wordBreak: "break-all" } ]
              [ text props.toptrace ]
            true -> 
              td [ cn "align-top", onClick \_ -> shortStack, style { cursor: "zoom-out" } ]
              [ div [ style { fontFamily: "Fira Code", wordBreak: "break-all" } ] $ map (\y -> 
                  div' [ text y ]) props.stacktrace
              ]
        ]
      where
      fullStack :: Effect Unit
      fullStack = modifyState this _{ expandStack = true }
      shortStack :: Effect Unit
      shortStack = modifyState this _{ expandStack = false }
