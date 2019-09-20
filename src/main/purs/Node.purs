module Node
  ( reactClass
  ) where

import BarChart as BarChart
import BigChart as BigChart
import CpuChart as CpuChart
import YearChart as YearChart
import MeasureChart as MeasureChart
import Data.Int (fromNumber) as Int
import Data.Maybe (Maybe, fromMaybe)
import Data.Map (lookup)
import Data.Array (foldl)
import DomOps (cn, onChangeValue)
import Effect (Effect)
import Errors as Errors
import FormatOps (duration, formatNum)
import Prelude (bind, map, pure, ($), (<>), (==), show, (+))
import React (ReactClass, ReactElement, ReactThis, component, getProps, getState, createLeafElement, forceUpdate, modifyState, modifyStateWithCallback)
import React.DOM (div, div', h2, h3, h4, h5, label, span, text, i, table, thead, tbody', th', th, tr', td', td, select, option)
import React.DOM.Props (style, colSpan, onClick, value)
import Schema (FdInfo, FsInfo, NodeInfo, ThrInfo, ChartRange(Live, Hour), ReindexChart(TsReindex, WcReindex, FilesReindex), Feature, StaticChart(Creation, Generation))

type State =
  { bigChartRange :: ChartRange
  , reindexChart :: ReindexChart
  , staticChart :: StaticChart
  , feature :: Feature
  }
type Props = NodeInfo

reactClass :: ReactClass Props
reactClass = component "Node" \this -> do
  p <- getProps this
  pure
    { state: { bigChartRange: Live, reindexChart: TsReindex, staticChart: Creation, feature: "disabled-users" }
    , render: render this
    }
  where
  render :: ReactThis Props State -> Effect ReactElement
  render this = do
    p <- getProps this
    s <- getState this
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
                    [ label [ cn $ "btn btn-sm btn-primary btn-simple" <> if s.bigChartRange == Live then " active" else "", onClick \_ -> modifyState this _{ bigChartRange = Live } ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Live" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "L" ]
                      ]
                    , label [ cn $ "btn btn-sm btn-primary btn-simple" <> if s.bigChartRange == Hour then " active" else "", onClick \_ -> modifyState this _{ bigChartRange = Hour } ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Hour" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "H" ]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn $ "chart-area" <> if s.bigChartRange == Live then "" else " d-none" ]
                [ createLeafElement BigChart.reactClass { cpuPoints: p.cpuPoints, memPoints: p.memPoints, actPoints: map (\x->{t:x.t,y:0.0}) p.actPoints, actLabels: map (_.label) p.actPoints }
                ]
              , div [ cn $ "chart-area" <> if s.bigChartRange == Hour then "" else " d-none" ]
                [ createLeafElement CpuChart.reactClass { cpuPoints: p.cpuHourPoints }
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
      , let thirdQ = case s.reindexChart of
              TsReindex -> p.reindexTs_thirdQ
              WcReindex -> p.reindexWc_thirdQ
              FilesReindex -> p.reindexFiles_thirdQ
        in div [ cn "row" ] 
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              [ div [ cn "row" ]
                [ div [ cn "col-7 col-sm-6 text-left" ]
                  [ h5 [ cn "card-category" ]
                    [ text "Reindex" ]
                  , h2 [ cn "card-title" ]
                    [ i [ cn "tim-icons icon-user-run text-info" ] []
                    , text $ fromMaybe "--" $ map (_<>" ms") thirdQ
                    ]
                  ]
                  , div [ cn "col-5 col-sm-6" ]
                  [ div [ cn "btn-group btn-group-toggle float-right" ]
                    [ label [ cn $ "btn btn-sm btn-primary btn-simple" <> if s.reindexChart == TsReindex then " active" else ""
                            , onClick \_ -> modifyStateWithCallback this _{ reindexChart = TsReindex } (forceUpdate this)
                            ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Traslations" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "Ts" ]
                      ]
                    , label [ cn $ "btn btn-sm btn-primary btn-simple" <> if s.reindexChart == WcReindex then " active" else ""
                            , onClick \_ -> modifyStateWithCallback this _{ reindexChart = WcReindex } (forceUpdate this)
                            ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Web Contents" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "Ws" ]
                      ]
                    , label [ cn $ "btn btn-sm btn-primary btn-simple" <> if s.reindexChart == FilesReindex then " active" else ""
                            , onClick \_ -> modifyStateWithCallback this _{ reindexChart = FilesReindex } (forceUpdate this)
                          ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Files" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "Fs" ]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn $ "chart-area" ]
                [  createLeafElement MeasureChart.reactClass $ case s.reindexChart of
                     TsReindex -> { points: map _.y p.reindexTs_points, labels: map _.t p.reindexTs_points }
                     WcReindex -> { points: map _.y p.reindexWc_points, labels: map _.t p.reindexWc_points }
                     FilesReindex -> { points: map _.y p.reindexFiles_points, labels: map _.t p.reindexFiles_points }
                ]
              ]
            ]
          ]
        ]
      , div [cn "row"]
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              [ div [ cn "row" ]
                [ div [ cn "col-7 col-sm-6 text-left" ]
                  [ h5 [ cn "card-category" ] [ text "Static" ] ]
                  , div [ cn "col-5 col-sm-6" ]
                  [ div [ cn "btn-group btn-group-toggle float-right" ]
                    [ label [ cn $ "btn btn-sm btn-primary btn-simple" <> 
                              if s.staticChart == Creation then " active" else ""
                            , onClick \_ -> modifyStateWithCallback this _{ staticChart = Creation } (forceUpdate this)
                            ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Creation" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "Cr" ]
                      ]
                    , label [ cn $ "btn btn-sm btn-primary btn-simple" <> 
                              if s.staticChart == Generation then " active" else ""
                            , onClick \_ -> modifyStateWithCallback this _{ staticChart = Generation } (forceUpdate this)
                            ]
                      [ span [ cn "d-none d-sm-block d-md-block d-lg-block d-xl-block" ]
                        [ text "Generation" ]
                      , span [ cn "d-block d-sm-none" ]
                        [ text "Gen" ]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "chart-area" ]
                [ createLeafElement YearChart.reactClass $ case s.staticChart of
                    Creation -> { points: p.staticCreateYear_points, label: "ms" }
                    Generation -> { points: p.staticGenYear_points, label: "ms" }
                ]
              ]
            ]
          ]
        ]
      , div [cn "row"]
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              [ h5 [ cn "card-category" ] [ text "Kvs size" ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "chart-area" ]
                [ createLeafElement YearChart.reactClass { points: p.kvsSizeYearPoints, label: "mb" }
                ]
              ]
            ]
          ]
        ]
      , div [cn "row"]
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              [ div [ cn "row" ]
                [ div [ cn "col-9 text-left" ]
                  [ h5 [ cn "card-category" ]
                    [ text "Feature usage" ]
                  , h2 [ cn "card-title" ]
                    [ text $ "Total: " <> (show $ fromMaybe 0 $ do
                            xs <- lookup s.feature p.features
                            Int.fromNumber $ foldl (\acc x -> acc + x.y) 0.0 xs)
                    ]
                  ]
                  , div [ cn "col-3" ]
                  [ div [ cn "input-group" ]
                    [ select [ cn "custom-select"
                             , onChangeValue \v -> modifyStateWithCallback this _{ feature = v } (forceUpdate this)
                             , value s.feature
                             ]
                      [ option [ value "disabled-users" ] [ text "Disabled users"]
                      , option [ value "wc-flow" ] [ text "Webcontents flow"]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ cn "card-body" ]
              [ div [ cn "chart-area" ]
                [ createLeafElement YearChart.reactClass 
                  { points: fromMaybe [] $ lookup s.feature p.features
                  , label: ""
                  }
                ]
              ]
            ]
          ]
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
