module Node
  ( reactClass
  ) where

import Data.Maybe (fromMaybe)
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
    , componentDidMount: createChart p.cpuPoints p.memPoints p.actionPoints
    , componentDidUpdate: \p' _ _ -> updateChart p'.cpuPoints p'.memPoints p'.actionPoints
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
                    , text $ " " <> fromMaybe "--" p.cpuLast <> "% / " <> fromMaybe "--" (map formatNum p.memLast) <> " MB"
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
        [ fromMaybe (div' []) (map fsCard p.fs)
        , fromMaybe (div' []) (map fdCard p.fd)
        , fromMaybe (div' []) (map thrCard p.thr)
        , othCard p
        ]
      ]
  card :: String -> Array ReactElement -> Array ReactElement -> ReactElement
  card title xs ys =
    div [ cn "col-lg-6 col-md-12" ]
      [ div [ cn "card" ]
        [ div [ cn "card-header" ]
          [ h4 [ cn "card-title" ]
            [ text title ]
          ]
        , div [ cn "card-body" ]
          [ div [ cn "table-responsive" ]
            [ table [ cn "table tablesorter" ]
              [ thead [ cn "text-primary" ]
                [ tr' xs ]
              , tbody' ys
              ]
            ]
          ]
        ]
      ]
  othCard :: Props -> ReactElement
  othCard p = card "Other Metrics"
    [ th' [ text "Name" ]
    , th [ cn "text-right" ] [ text "Value" ]
    , th' [ text "Unit" ]
    ]
    [ tr'
      [ td' [ text "Uptime" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" $ map duration p.uptime ]
      , td' [ text "HH:MM:SS" ]
      ]
    , tr'
      [ td' [ text "CPU Load" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" p.cpuLast ]
      , td' [ text "%" ]
      ]
    , tr'
      [ td' [ text "Memory: Used" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" $ map formatNum p.memLast ]
      , td' [ text "MB" ]
      ]
    , tr'
      [ td' [ text "Memory: Free" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" $ map formatNum p.memFree ]
      , td' [ text "MB" ]
      ]
    , tr'
      [ td' [ text "Memory: Total" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" $ map formatNum p.memTotal ]
      , td' [ text "MB" ]
      ]
    ]
  fsCard :: FsInfo -> ReactElement
  fsCard x = card "File System"
    [ th' [ text "" ]
    , th' [ text "Megabytes" ]
    ]
    [ tr'
      [ td' [ text "Used" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.used ]
      ]
    , tr'
      [ td' [ text "Usable" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.usable ]
      ]
    , tr'
      [ td' [ text "Total" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.total ]
      ]
    ]
  fdCard :: FdInfo -> ReactElement
  fdCard x = card "File Descriptors"
    [ th' [ text "" ]
    , th' [ text "Count" ]
    ]
    [ tr'
      [ td' [ text "Open" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.open ]
      ]
    , tr'
      [ td' [ text "Max" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.max ]
      ]
    ]
  thrCard :: ThrInfo -> ReactElement
  thrCard x = card "Threads"
    [ th' [ text "" ]
    , th' [ text "Count" ]
    ]
    [ tr'
      [ td' [ text "All" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.all ]
      ]
    , tr'
      [ td' [ text "Non-daemon" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.nondaemon ]
      ]
    , tr'
      [ td' [ text "Daemon" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.daemon ]
      ]
    , tr'
      [ td' [ text "Peak" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.peak ]
      ]
    , tr'
      [ td' [ text "Total" ]
      , td [ style { fontFamily: "Fira Code" } ]
        [ text $ formatNum x.total ]
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
