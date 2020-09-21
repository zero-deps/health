module Errors
  ( reactClass
  ) where

import FormatOps (dateTime)
import Data.Array (singleton)
import DomOps (cn)
import Effect (Effect)
import Global (readInt)
import Prelude (Unit, bind, map, pure, ($), (<>), (==))
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, createLeafElement, modifyState)
import React.DOM (div, h5, table, tbody', td, text, th', thead, tr', div')
import React.DOM.Props (onClick, style, colSpan)
import Schema (ErrorInfo)

type State = {}
type Props =
  { errors :: Array ErrorInfo
  , showAddr :: Boolean
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
                    [ tr' $ ( if props.showAddr then 
                      [ th' [ text "Address" ] ] else [] ) <>
                      [ th' [ text "Time" ]
                      , th' [ text "Exception" ]
                      , th' [ text "Stacktrace" ]
                      ]
                    ]
                  , tbody' $ map (\x -> createLeafElement errorReactClass { err: x, showAddr: props.showAddr }) props.errors
                  ]
                ]
              ]
            ]
          ]
        ]

type ErrorState =
  { expandStack :: Boolean
  }
type ErrorProps =
  { err :: ErrorInfo
  , showAddr :: Boolean
  }

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
      dt <- dateTime props.err.time
      let nocause = props.err.toptrace == "--"
      pure $
        tr' $ (if props.showAddr then
        [ td [ cn "align-top" ] [ text props.err.host ] ] else [] ) <>
        [ td [ cn "align-top" ] [ text dt ]
        , td [ cn "align-top", style { width: "40%" }, colSpan (if nocause then 2 else 1) ] $ map (\y -> 
            div [ style { wordBreak: "break-all" } ]
              [ text y ]) props.err.exception
        ] <> (if nocause then [] else singleton $
          case s.expandStack of
            false -> 
              td [ cn "align-top", onClick \_ -> fullStack, style { cursor: "zoom-in", fontFamily: "Fira Code", wordBreak: "break-all" } ]
              [ text props.err.toptrace ]
            true -> 
              td [ cn "align-top", onClick \_ -> shortStack, style { cursor: "zoom-out" } ]
              [ div [ style { fontFamily: "Fira Code", wordBreak: "break-all" } ] $ map (\y -> 
                  div' [ text y ]) props.err.stacktrace
              ])
      where
      fullStack :: Effect Unit
      fullStack = modifyState this _{ expandStack = true }
      shortStack :: Effect Unit
      shortStack = modifyState this _{ expandStack = false }
