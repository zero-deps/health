module Node
  ( reactClass
  ) where

import DomOps (cn)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (canvas, div, h2, h5, label, span, text, i)
import React.DOM.Props (_id)
import Schema

type State = {}
type Props = NodeInfo

reactClass :: ReactClass Props
reactClass = component "Node" \this -> do
  p <- getProps this
  pure
    { state: {}
    , render: render this
    , componentDidMount: createChart p.cpuLoad p.actions
    , componentDidUpdate: \p' _ _ -> updateChart p'.cpuLoad p'.actions
    }
  where
  render :: ReactThis Props State -> Effect ReactElement
  render this = do
    props <- getProps this
    pure $
      div [ cn "row" ]
      [ div [ cn "col-12" ]
        [ div [ cn "card card-chart" ]
          [ div [ cn "card-header" ]
            [ div [ cn "row" ]
              [ div [ cn "col-7 col-sm-6 text-left" ]
                [ h5 [ cn "card-category" ]
                  [ text "Performance" ]
                , h2 [ cn "card-title" ]
                  [ i [ cn "tim-icons icon-spaceship text-primary" ] []
                  , text $ " " <> props.cpuLast <> " / 547"
                  ]
                ]
              , div [ cn "col-5 col-sm-6" ]
                [ div [ cn "btn-group btn-group-toggle float-right" ]
                  [ label [ cn "btn btn-sm btn-primary btn-simple" ]
                    [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                      [ text "Hour" ]
                    , span [ cn "d-block d-sm-none" ]
                      [ text "H" ]
                    ]
                  , label [ cn "btn btn-sm btn-primary btn-simple active" ]
                    [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                      [ text "Day" ]
                    , span [ cn "d-block d-sm-none" ]
                      [ text "D" ]
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

foreign import createChart :: Array CpuPoint -> Array ActionPoint -> Effect Unit
foreign import updateChart :: Array CpuPoint -> Array ActionPoint -> Effect Unit
