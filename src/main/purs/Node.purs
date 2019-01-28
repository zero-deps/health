module Node
  ( reactClass
  ) where

import DomOps (cn)
import Effect (Effect)
import FormatOps (formatNum, duration)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (canvas, div, div', h2, h4, h5, label, span, text, i, table, thead, tbody', th', th, tr', td', td)
import React.DOM.Props (_id, style)
import Schema

type State = {}
type Props = NodeInfo

reactClass :: ReactClass Props
reactClass = component "Node" \this -> do
  p <- getProps this
  pure
    { state: {}
    , render: render this
    , componentDidMount: createChart p.cpuLoad p.memLoad p.actions
    , componentDidUpdate: \p' _ _ -> updateChart p'.cpuLoad p'.memLoad p'.actions
    , componentWillUnmount: destroyChart
    }
  where
  render :: ReactThis Props State -> Effect ReactElement
  render this = do
    p <- getProps this
    pure $
      div'
      [ div [ cn "row" ]
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              [ div [ cn "row" ]
                [ div [ cn "col-7 col-sm-6 text-left" ]
                  [ h5 [ cn "card-category" ]
                    [ text "Performance" ]
                  , h2 [ cn "card-title" ]
                    [ i [ cn "tim-icons icon-spaceship text-primary" ] []
                    , text $ " " <> p.cpuLast <> "% / " <> formatNum p.memLast <> " MB"
                    ]
                  ]
                , div [ cn "col-5 col-sm-6" ]
                  [ div [ cn "btn-group btn-group-toggle float-right" ]
                    [ label [ cn "btn btn-sm btn-primary btn-simple active" ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Live" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "L" ]
                      ]
                    , label [ cn "btn btn-sm btn-primary btn-simple" ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Hour" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "H" ]
                      ]
                    , label [ cn "btn btn-sm btn-primary btn-simple" ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Week" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "W" ]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "chart-area" ]
                [ canvas [ _id "chartBig1" ] [] ]
              ]
            ]
          ]
        ]
      , div [ cn "row" ]
        [ div [ cn "col-lg-6 col-md-12" ]
          [ div [ cn "card" ]
            [ div [ cn "card-header" ]
              [ h4 [ cn "card-title" ]
                [ text "All Metrics" ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "table-responsive" ]
                [ table [ cn "table tablesorter" ]
                  [ thead [ cn "text-primary" ]
                    [ tr'
                      [ th' [ text "Name" ]
                      , th [ cn "text-right" ] [ text "Value" ]
                      , th' [ text "Unit" ]
                      ]
                    ]
                  , tbody'
                    [ tr'
                      [ td' [ text "Uptime" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ duration p.uptime ]
                      , td' [ text "HH:MM:SS" ]
                      ]
                    , tr'
                      [ td' [ text "CPU Load" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text p.cpuLast ]
                      , td' [ text "%" ]
                      ]
                    , tr'
                      [ td' [ text "Memory: Used" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.memLast ]
                      , td' [ text "MB" ]
                      ]
                    , tr'
                      [ td' [ text "Memory: Free" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.memFree ]
                      , td' [ text "MB" ]
                      ]
                    , tr'
                      [ td' [ text "Memory: Total" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.memTotal ]
                      , td' [ text "MB" ]
                      ]
                    , tr'
                      [ td' [ text "Storage: Used" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.fsUsed ]
                      , td' [ text "MB" ]
                      ]
                    , tr'
                      [ td' [ text "Storage: Free" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.fsFree ]
                      , td' [ text "MB" ]
                      ]
                    , tr'
                      [ td' [ text "Storage: Total" ]
                      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
                        [ text $ formatNum p.fsTotal ]
                      , td' [ text "MB" ]
                      ]
                    ]
                  ]
                ]
              ]
            ]
          ]
        ]
      ]

foreign import createChart
  :: Array CpuPoint
  -> Array MemPoint
  -> Array ActionPoint
  -> Effect Unit
foreign import updateChart
  :: Array CpuPoint
  -> Array MemPoint
  -> Array ActionPoint
  -> Effect Unit
foreign import destroyChart
  :: Effect Unit
