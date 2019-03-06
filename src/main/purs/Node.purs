module Node
  ( reactClass
  ) where

import BarChart as BarChart
import BigChart as BigChart
import Data.Maybe (Maybe, fromMaybe)
import DomOps (cn)
import Effect (Effect)
import Errors as Errors
import FormatOps (duration, formatNum)
import Prelude (bind, map, pure, ($), (<>))
import React (ReactClass, ReactElement, ReactThis, component, getProps, createLeafElement)
import React.DOM (div, div', h2, h3, h4, h5, label, span, text, i, table, thead, tbody', th', th, tr', td', td)
import React.DOM.Props (style, colSpan)
import Schema (FdInfo, FsInfo, NodeInfo, ThrInfo)

type State = {}
type Props = NodeInfo

reactClass :: ReactClass Props
reactClass = component "Node" \this -> do
  p <- getProps this
  pure
    { state: {}
    , render: render this
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
                [ createLeafElement BigChart.reactClass { cpuPoints: p.cpuPoints, memPoints: p.memPoints, actPoints: map (\x->{t:x.t,y:0.0}) p.actPoints, actLabels: map (_.label) p.actPoints }
                ]
              ]
            ]
          ]
        ]
      , div [ cn "row" ]
        [ barChart "Translations: Search" p.searchTs_thirdQ p.searchTs_points
        , barChart "Web Contents: Search" p.searchWc_thirdQ p.searchWc_points
        , barChart "Static: Creation" p.staticCreate_thirdQ p.staticCreate_points
        , barChart "Static: Generation" p.staticGen_thirdQ p.staticGen_points
        ]
      , div [ cn "row" ]
        [ fromMaybe (div' []) (map fsCard p.fs)
        , fromMaybe (div' []) (map fdCard p.fd)
        ]
      , div [ cn "row" ]
        [ fromMaybe (div' []) (map thrCard p.thr)
        , othCard p
        ]
      , createLeafElement Errors.reactClass { errors: p.errs, showAddr: false }
      ]
  barChart :: String -> Maybe String -> Array {t::String,y::Number} -> ReactElement
  barChart title thirdQ values =
    div [ cn "col-lg-3 col-md-12" ]
      [ div [ cn "card card-chart" ]
        [ div [ cn "card-header" ]
          [ h5 [ cn "card-category" ] [ text title ]
          , h3 [ cn "card-title" ]
            [ i [ cn "tim-icons icon-user-run text-info" ] []
            , text $ fromMaybe "--" $ map (_<>" ms") thirdQ
            ]
          ]
        , div [ cn "card-body" ]
          [ div [ cn "chart-area" ]
            [ createLeafElement BarChart.reactClass { points: map _.y values, labels: map _.t values }
            ]
          ]
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
  othCard :: NodeInfo -> ReactElement
  othCard p = let uptime = map duration p.uptime in card "Other Metrics"
    [ th' [ text "Name" ]
    , th [ cn "text-right" ] [ text "Value" ]
    , th' [ text "Unit" ]
    ]
    [ tr'
      [ td' [ text "Uptime" ]
      , td [ cn "text-right", style { fontFamily: "Fira Code" } ]
        [ text $ fromMaybe "--" $ map _.value uptime ]
      , td' [ text $ fromMaybe "--" $ map _.unit uptime ]
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
    , tr'
      [ td' [ text "Version" ]
      , td [ cn "text-center", style { fontFamily: "Fira Code" }, colSpan 2 ]
        [ text $ fromMaybe "--" p.version ]
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
