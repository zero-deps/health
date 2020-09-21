module Main
  ( view
  ) where

import Api.Push (Push(StatMsg), Stat(Metric, Measure, Error, Action), decodePush)
import Control.Alt ((<|>))
import Data.Array (dropEnd, filter, fromFoldable, head, last, singleton, snoc, take, takeEnd, (:))
import Data.Either (Either(Right))
import Data.Map (Map, lookup, unionWith, insertWith, update)
import Data.Map as Map
import Data.Maybe (Maybe(Just, Nothing), fromMaybe, maybe)
import Data.String (Pattern(Pattern), split)
import Data.Traversable (sequence)
import Data.Tuple (Tuple(Tuple))
import DomOps (cn)
import DomOps as DomOps
import Effect (Effect)
import Effect.Console (error)
import Errors as ErrorsCom
import Ext.String (startsWith)
import Features as FeaturesCom
import FormatOps (dateTime)
import Global (readInt)
import Node as NodeCom
import Nodes as NodesCom
import Prelude (class Eq, class Show, Unit, add, bind, discard, map, max, not, pure, show, unit, void, ($), (&&), (*), (-), (/=), (<>), (==), (>), (>=), (>>=), (<<<))
import Proto.Uint8Array (Uint8Array)
import React (ReactClass, ReactThis, ReactElement, createLeafElement, modifyState, component, getState, getProps)
import React.DOM (a, button, div, i, li, nav, p, p', span, span', text, ul, h5, h2, iframe)
import React.DOM.Props (href, onClick, src, style)
import ReactDOM as ReactDOM
import Schema (ErrorInfo, NodeAddr, NodeInfo, UpdateData, Feature)
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps

type State = 
  { menu :: Menu
  , nodes :: Map NodeAddr NodeInfo
  , node :: Maybe NodeAddr
  , features :: Map Feature (Map Number Number)
  , errors :: Array ErrorInfo
  , ws :: WebSocket
  , leftMenu :: Boolean
  , notifications :: Boolean
  , topMenu :: Boolean
  }
type Props =
  { menu :: Array Menu
  }

data Menu = Nodes | Features | Errors | Paper | Notice
derive instance eqMenu :: Eq Menu
instance showMenu :: Show Menu where
  show Nodes = "Health"
  show Features = "Features"
  show Errors = "Errors"
  show Paper = "Paper"
  show Notice = "Notice"

menuIcon :: Menu -> String
menuIcon Nodes = "icon-heart-2"
menuIcon Features = "icon-bulb-63"
menuIcon Errors = "icon-alert-circle-exc"
menuIcon Paper = "icon-paper"
menuIcon Notice = "icon-caps-small"

view :: Effect Unit
view = do
  container <- DomOps.byId "container"
  let props =
        { menu: [ Nodes, Features, Errors, Paper, Notice ]
        }
  let element = createLeafElement reactClass props
  void $ ReactDOM.render element container

reactClass :: ReactClass Props
reactClass = component "Main" \this -> do
  h <- map (fromMaybe "127.0.0.1:8002") DomOps.host 
  ws <- WsOps.create $ "ws:/"<>h<>"/stats/ws"
  pure
    { state:
      { menu: Nodes
      , nodes: Map.empty
      , node: Nothing
      , features: Map.empty
      , errors: []
      , ws: ws
      , leftMenu: false
      , notifications: false
      , topMenu: false
      }
    , render: render this
    , componentDidMount: do
        WsOps.onMsg ws (onMsg this) (errHandler)
    }
  where
    render :: ReactThis Props State -> Effect ReactElement
    render this = do
      s <- getState this
      props <- getProps this
      menuContent' <- menuContent s
      pure $
        div [ cn $ "wrapper"<>if s.leftMenu then " nav-open" else "" ]
        [ div [ cn "sidebar" ]
          [ div [ cn "sidebar-wrapper" ]
            [ ul [ cn "nav" ] $ map (\x ->
                li (if x == s.menu then [ cn "active" ] else [])
                [ a [ href "#", onClick \_ -> goto x ]
                  [ i [ cn $ "tim-icons " <> menuIcon x ] []
                  , p' [ text $ show x ]
                  ]
                ]) props.menu
            ]
          ]
        , div [ cn "main-panel" ]
          [ nav [ cn $ "navbar navbar-expand-lg navbar-absolute"<>if s.topMenu then " bg-white" else " navbar-transparent" ]
            [ div [ cn "container-fluid" ]
              [ div [ cn "navbar-wrapper" ]
                [ div [ cn $ "navbar-toggle d-inline"<>if s.leftMenu then " toggled" else "" ]
                  [ button [ cn "navbar-toggler", onClick \_ -> toggleLeftMenu ]
                    [ span [ cn "navbar-toggler-bar bar1" ] []
                    , span [ cn "navbar-toggler-bar bar2" ] []
                    , span [ cn "navbar-toggler-bar bar3" ] []
                    ]
                  ]
                , a [ href "#", cn "navbar-brand" ] [ text "Monitor" ]
                ]
              , button [ cn "navbar-toggler", onClick \_ -> toggleTopMenu ]
                [ span [ cn "navbar-toggler-bar navbar-kebab" ] []
                , span [ cn "navbar-toggler-bar navbar-kebab" ] []
                , span [ cn "navbar-toggler-bar navbar-kebab" ] []
                ]
              , div [ cn $ "collapse navbar-collapse"<>if s.topMenu then " show" else "" ]
                [ ul [ cn "navbar-nav ml-auto" ]
                  [ li [ cn $ "dropdown nav-item"<>if s.notifications then " show" else "" ]
                    [ a [ href "#", cn "dropdown-toggle nav-link", onClick \_ -> toggleNotifications ]
                      -- [ div [ cn "notification d-none d-lg-block" ] []
                      [ div [ cn "notification d-none" ] []
                      , i [ cn "tim-icons icon-sound-wave" ] []
                      , p [ cn "d-lg-none" ]
                        [ text "Notifications" ]
                      ]
                    , ul [ cn $ "dropdown-menu dropdown-menu-right dropdown-navbar"<>if s.notifications then " show" else "" ]
                      [ li [ cn "nav-link" ]
                        [ a [ href "#", cn "nav-item dropdown-item" ]
                          [ text "No notifications" ]
                        ]
                      ]
                    ]
                  , li [ cn "separator d-lg-none" ] []
                  ]
                ]
              ]
            ]
          , div [ cn "content" ] [ menuContent' ]
          ]
        ]
      where
      menuContent :: State -> Effect ReactElement
      menuContent { menu: Nodes, node: Just host, nodes } =
        case lookup host nodes of
          Just node -> pure $ createLeafElement NodeCom.reactClass node
          Nothing -> map (\_ -> createLeafElement dummy {}) $ error "bad node"
        where
        dummy :: ReactClass {}
        dummy = component "Dummy" \_ -> pure { render: pure $ span' [] }
      menuContent { menu: Nodes, nodes, ws } =
        pure $ createLeafElement NodesCom.reactClass 
                { nodes: fromFoldable nodes
                , ws
                , openNode: \host -> modifyState this \s -> s{ node = Just host, nodes = update (Just <<< _{ loaded = true }) host s.nodes }
                }
      menuContent { menu: Features, features } =
        pure $ createLeafElement FeaturesCom.reactClass { features: map (\x -> map (\(Tuple t y) -> { t, y }) $ Map.toUnfoldable x) features }
      menuContent { menu: Errors, errors } =
        pure $ createLeafElement ErrorsCom.reactClass { errors, showAddr: true, showTitle: false }
      menuContent { menu: Paper } =
        pure $
          div [ cn "row" ]
            [ div [ cn "col-md-12" ]
              [ div [ cn "card" ]
                [ div [ cn "card-header" ]
                  [ h5 [ cn "card-category" ] [ text "Paper" ]
                  , h2 [ cn "card-title" ] [ text "Monitoring and Management" ]
                  ]
                , div [ cn "card-body", style { height: "calc(100vh - 220px)" } ]
                  [ iframe [ src "http://ua--doc.ee..corp/health.html#start" ] []
                  ]
                ]
              ]
            ]
      menuContent { menu: Notice } =
        pure $
          div [ cn "row" ]
            [ div [ cn "col-md-12" ]
              [ div [ cn "card" ]
                [ div [ cn "card-header" ]
                  [ h5 [ cn "card-category" ] [ text "Notice" ]
                  , h2 [ cn "card-title" ] [ text "MIT License" ]
                  ]
                , div [ cn "card-body" ]
                  [ p [] [ text "Copyright (c) 2017 Creative Tim (https://www.creative-tim.com)" ]
                  , p [] [ text "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:" ]
                  , p [] [ text "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software." ]
                  , p [] [ text "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE." ]
                  ]
                ]
              ]
            ]

      goto :: Menu -> Effect Unit
      goto Nodes = modifyState this _{ menu = Nodes, node = Nothing }
      goto Features = modifyState this _{ menu = Features }
      goto Errors = modifyState this _{ menu = Errors }
      goto Paper = modifyState this _{ menu = Paper }
      goto Notice = modifyState this _{ menu = Notice }

      toggleLeftMenu :: Effect Unit
      toggleLeftMenu = modifyState this \s -> s{ leftMenu = not s.leftMenu }

      toggleNotifications :: Effect Unit
      toggleNotifications = modifyState this \s -> s{ notifications = not s.notifications }

      toggleTopMenu :: Effect Unit
      toggleTopMenu = modifyState this \s -> s{ topMenu = not s.topMenu }

    onMsg :: ReactThis Props State -> Uint8Array -> Effect Unit
    onMsg this bytes = do
      case decodePush bytes of
        Right { val: StatMsg { stat: Metric { name, value}, time, host }} -> do
          let cpu_mem = map (split (Pattern "~")) $ if name == "cpu_mem" then Just value else Nothing
          let cpu_hour = if name == "cpu.hour" then Just value else Nothing
          let uptime = if name == "uptime" then Just value else Nothing
          let version = if name == "v" then Just value else Nothing
          let fs = map (split (Pattern "~")) $ if name == "fs./" then Just value else Nothing
          let fd = map (split (Pattern "~")) $ if name == "fd" then Just value else Nothing
          let thr = map (split (Pattern "~")) $ if name == "thr" then Just value else Nothing
          let kvsSize_year = if name == "kvs.size.year" then Just value else Nothing
          let feature = if name == "feature" then Just value else Nothing
          updateWith
            { host: host
            , time: time
            , metrics: Just { cpu_mem, cpu_hour, uptime, version, fs, fd, thr, kvsSize_year }
            , measure: Nothing
            , err: Nothing
            , action: Nothing
            , feature
            }
        Right { val: StatMsg { stat: Measure { name, value }, time, host }} -> do
          let value' = readInt 10 value
          let searchTs = if name == "search.ts" then Just value else Nothing
          let searchTs_thirdQ = if name == "search.ts.thirdQ" then Just value else Nothing
          let searchWc = if name == "search.wc" then Just value else Nothing
          let searchWc_thirdQ = if name == "search.wc.thirdQ" then Just value else Nothing
          let searchFs = if name == "search.fs" then Just value else Nothing
          let searchFs_thirdQ = if name == "search.fs.thirdQ" then Just value else Nothing
          let staticGen = if name == "static.gen" then Just value else Nothing
          let staticGen_thirdQ = if name == "static.gen.thirdQ" then Just value else Nothing
          let staticGen_year = if name == "static.gen.year" then Just value else Nothing
          let reindexAll = if name == "reindex.all" then Just value else Nothing
          let reindexAll_thirdQ = if name == "reindex.all.thirdQ" then Just value else Nothing
          updateWith
            { host: host
            , time: time
            , metrics: Nothing
            , measure: Just
              { searchTs, searchTs_thirdQ
              , searchWc, searchWc_thirdQ
              , searchFs, searchFs_thirdQ
              , staticGen, staticGen_thirdQ, staticGen_year
              , reindexAll, reindexAll_thirdQ
              }
            , err: Nothing
            , action: Nothing
            , feature: Nothing
            }
        Right { val: StatMsg { stat: Error { exception: exception', stacktrace: stacktrace', toptrace}, time, host }} -> do
          let exception = split (Pattern "~") exception'
          let stacktrace = split (Pattern "~") stacktrace'
          let key = host<> (show time)
          let err = { exception, stacktrace, toptrace, time, host, key }
          updateWith
            { host: host
            , time: time
            , metrics: Nothing
            , measure: Nothing
            , err: Just err
            , action: Nothing
            , feature: Nothing
            }
        Right { val: StatMsg { stat: Action { action }, time, host }} -> updateWith
          { host: host
          , time: time
          , metrics: Nothing
          , measure: Nothing
          , err: Nothing
          , action: Just action
          , feature: Nothing
          }
        _ -> error "unknown type"
      where
      updateWith :: UpdateData -> Effect Unit
      updateWith a = do
        let time' = a.time
        dt <- dateTime time'
        let uptime = a.metrics >>= _.uptime
        let version = a.metrics >>= _.version
        let cpu_mem = a.metrics >>= _.cpu_mem
        let cpu = cpu_mem >>= head
        let cpuPoints = fromMaybe [] $ map (\b -> [{ t: time', y: readInt 10 b }]) cpu
        let cpuHourPoint = map (\b -> { t: time', y: readInt 10 b }) $ a.metrics >>= _.cpu_hour
        let kvsSizeYearPoint = map (\b -> { t: time', y: readInt 10 b }) $ a.metrics >>= _.kvsSize_year
        
        mem <- case cpu_mem of
          Just ([ _, free', total', heap' ]) -> do
            let free = readInt 10 free'
            let total = readInt 10 total'
            let used = total - free
            let heap = readInt 10 heap'
            pure $ Just { used, free, total, heap }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing
        let memUsed = map _.used mem
        let memFree = map _.free mem
        let memTotal = map _.total mem
        let memPoints = fromMaybe [] $ map (\b -> [{ t: time', y: b.heap }]) mem
        
        fs <- case a.metrics >>= _.fs of
          Just ([ usable', total' ]) -> do
            let usable = readInt 10 usable'
            let total = readInt 10 total'
            let used = total - usable
            pure $ Just { used, usable, total }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing
        
        fd <- case a.metrics >>= _.fd of
          Just ([ open', max' ]) -> do
            let open = readInt 10 open'
            let max = readInt 10 max'
            pure $ Just { open, max }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing

        thr <- case a.metrics >>= _.thr of
          Just [ all', daemon', peak', total' ] -> do
            let all = readInt 10 all'
            let daemon = readInt 10 daemon'
            let nondaemon = all - daemon
            let peak = readInt 10 peak'
            let total = readInt 10 total'
            pure $ Just { all, daemon, nondaemon, peak, total }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing

        let action = fromMaybe [] $ map (\label -> [{ t: time', label }]) a.action

        let searchTs_points = fromMaybe [] $ map (\y -> [{t:dt,y:readInt 10 y}]) $ a.measure >>= _.searchTs
        let searchTs_thirdQ = a.measure >>= _.searchTs_thirdQ
        let searchWc_points = fromMaybe [] $ map (\y -> [{t:dt,y:readInt 10 y}]) $ a.measure >>= _.searchWc
        let searchWc_thirdQ = a.measure >>= _.searchWc_thirdQ
        let searchFs_points = fromMaybe [] $ map (\y -> [{t:dt,y:readInt 10 y}]) $ a.measure >>= _.searchFs
        let searchFs_thirdQ = a.measure >>= _.searchFs_thirdQ
        let staticGen_points = fromMaybe [] $ map (\y -> [{t:dt,y:readInt 10 y}]) $ a.measure >>= _.staticGen
        let staticGen_thirdQ = a.measure >>= _.staticGen_thirdQ
        let staticGenYearPoint = map (\b -> { t: time', y: readInt 10 b }) $ a.measure >>= _.staticGen_year
        let reindexAll_points = fromMaybe [] $ map (\y -> [{t:dt,y:readInt 10 y}]) $ a.measure >>= _.reindexAll
        let reindexAll_thirdQ = a.measure >>= _.reindexAll_thirdQ

        let errs = maybe [] singleton a.err
        
        s <- getState this
        let node' = case lookup a.host s.nodes of
              Just node -> do
                let cpuPoints' = node.cpuPoints <> cpuPoints
                let memPoints' = node.memPoints <> memPoints
                let actPoints' = node.actPoints <> action
                let minTime = max
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 cpuPoints') $ max
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 memPoints')
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 actPoints')
                let cpuPoints'' = filter (\x -> x.t > minTime) cpuPoints'
                let memPoints'' = filter (\x -> x.t > minTime) memPoints'
                let actPoints'' = filter (\x -> x.t > minTime) actPoints'

                let cpuHourPoints' = maybe node.cpuHourPoints (\x -> snoc (filter (\y -> y.t /= x.t && y.t >= x.t - 60.0*60.0*1000.0) node.cpuHourPoints) x) cpuHourPoint
                let kvsSizeYearPoints' = maybe node.kvsSizeYearPoints (\x -> snoc (filter (\y -> y.t /= x.t && y.t >= x.t - 365.0*24.0*3600.0*1000.0) node.kvsSizeYearPoints) x) kvsSizeYearPoint

                let searchTs_points'  = takeEnd 5 $ node.searchTs_points  <> searchTs_points
                let searchWc_points'  = takeEnd 5 $ node.searchWc_points  <> searchWc_points
                let searchFs_points'  = takeEnd 5 $ node.searchFs_points  <> searchFs_points
                let staticGen_points' = takeEnd 5 $ node.staticGen_points <> staticGen_points

                let staticGenYear_points' = maybe node.staticGenYear_points (\x -> snoc (filter (\y -> y.t /= x.t && y.t >= x.t - 365.0*24.0*3600.0*1000.0) node.staticGenYear_points) x) staticGenYearPoint
                
                let reindexAll_points' = takeEnd 5 $ node.reindexAll_points <> reindexAll_points

                let errs' = take 100 $ errs <> node.errs

                let importLog = case a.action of
                                  Just msg | startsWith "import" msg -> { t: dt, msg } : (take 99 node.importLog)
                                  _ -> node.importLog

                node
                  { lastUpdate = dt
                  , lastUpdate_ms = time'
                  , version = version <|> node.version
                  , cpuPoints = cpuPoints''
                  , cpuHourPoints = cpuHourPoints'
                  , memPoints = memPoints''
                  , actPoints = actPoints''
                  , cpuLast = cpu <|> node.cpuLast
                  , memLast = memUsed <|> node.memLast
                  , uptime = uptime <|> node.uptime
                  , memFree = memFree <|> node.memFree
                  , memTotal = memTotal <|> node.memTotal
                  , fs = fs <|> node.fs
                  , fd = fd <|> node.fd
                  , thr = thr <|> node.thr
                  , errs = errs'
                  , searchTs_points = searchTs_points'
                  , searchTs_thirdQ = searchTs_thirdQ <|> node.searchTs_thirdQ
                  , searchWc_points = searchWc_points'
                  , searchWc_thirdQ = searchWc_thirdQ <|> node.searchWc_thirdQ
                  , searchFs_points = searchFs_points'
                  , searchFs_thirdQ = searchFs_thirdQ <|> node.searchFs_thirdQ
                  , reindexAll_points = reindexAll_points'
                  , reindexAll_thirdQ = reindexAll_thirdQ <|> node.reindexAll_thirdQ
                  , staticGen_points = staticGen_points'
                  , staticGenYear_points = staticGenYear_points'
                  , staticGen_thirdQ = staticGen_thirdQ <|> node.staticGen_thirdQ
                  , kvsSizeYearPoints = kvsSizeYearPoints'
                  , importLog = importLog
                  }
              Nothing ->
                { host: a.host
                , ip: ""
                , loaded: false
                , lastUpdate: dt
                , lastUpdate_ms: time'
                , version: version
                , cpuPoints: cpuPoints
                , cpuHourPoints: maybe [] singleton cpuHourPoint
                , memPoints: memPoints
                , actPoints: action
                , cpuLast: cpu
                , memLast: memUsed
                , uptime: uptime
                , memFree: memFree
                , memTotal: memTotal
                , fs: Nothing
                , fd: Nothing
                , thr: Nothing
                , errs: errs
                , searchTs_points: searchTs_points
                , searchTs_thirdQ: searchTs_thirdQ
                , searchWc_points: searchWc_points
                , searchWc_thirdQ: searchWc_thirdQ
                , searchFs_points: searchFs_points
                , searchFs_thirdQ: searchFs_thirdQ
                , reindexAll_points: reindexAll_points
                , reindexAll_thirdQ: reindexAll_thirdQ
                , staticGen_points: staticGen_points
                , staticGenYear_points: maybe [] singleton staticGenYearPoint
                , staticGen_thirdQ: staticGen_thirdQ
                , kvsSizeYearPoints: maybe [] singleton kvsSizeYearPoint
                , importLog: []
                }
        modifyState this \s' -> s' { nodes = Map.insert node'.host node' s'.nodes }
        
        case a.err of
          Just err -> modifyState this \s' -> s' { errors = err : (take 99 s'.errors) }
          Nothing -> pure unit

        case a.feature of
          Just v ->
            case split (Pattern "~") v of
              [ name, num ] -> do
                let v' = Map.singleton time' $ readInt 10 num
                modifyState this \s' -> s'{ features = insertWith (unionWith add) name v' s'.features }
              _ -> pure unit
          Nothing -> pure unit

    errHandler :: Array String -> Effect Unit
    errHandler xs = void $ sequence $ map error xs
