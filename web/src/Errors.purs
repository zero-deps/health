module Errors
  ( reactClass
  ) where

import FormatOps (dateTime)
import Data.Maybe (fromMaybe)
import DomOps (cn)
import Effect (Effect)
import Prelude (bind, map, pure, ($), (<>))
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, createLeafElement)
import React.DOM (div, h5, table, tbody, td, text, th, thead, tr)
import React.DOM.Props (style)
import Schema (ErrorInfo)

type State = {}
type Props =
  { errors :: Array ErrorInfo
  , showTitle :: Boolean
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
          [ div [ cn "card" ] $ (if props.showTitle then
            [ div [ cn "card-header" ]
              [ h5 [ cn "card-category" ]
                [ text "Errors" ]
              ]
            ] else []) <>
            [ div [ cn "card-body" ]
              [ div [ cn "table-responsive" ]
                [ table [ cn "table tablesorter" ]
                  [ thead [ cn "text-primary" ]
                    [ tr []
                      [ th [] [ text "Time" ]
                      , th [] [ text "Exception" ]
                      , th [] [ text "Stacktrace" ]
                      ]
                    ]
                  , tbody [] $ map (\x -> createLeafElement errorReactClass { err: x }) props.errors
                  ]
                ]
              ]
            ]
          ]
        ]

type ErrorState = {}
type ErrorProps =
  { err :: ErrorInfo
  }

errorReactClass :: ReactClass ErrorProps
errorReactClass = component "Error" \this -> do
  pure
    { state: {}
    , render: render this
    }
  where
  render :: ReactThis ErrorProps ErrorState -> Effect ReactElement
  render this = do
    s <- getState this
    props <- getProps this
    dt <- dateTime props.err.time
    pure $
      tr []
      [ td [ cn "align-top" ] [ text dt ]
      , td [ cn "align-top", style { width: "40%" } ]
        [ div [ style { wordBreak: "break-all" } ] [ text $ fromMaybe "" props.err.msg ]
        , div [ style { wordBreak: "break-all" } ] [ text props.err.cause ]
        ]
      , td [ cn "align-top" ]
        [ div [ style { fontFamily: "Fira Code", wordBreak: "break-all" } ] $ map (\y -> 
            div [] [ text y ]) props.err.st
        ]
      ]
