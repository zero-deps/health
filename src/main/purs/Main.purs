module Main
  ( view
  ) where

import Control.Alt ((<|>))
import Data.Array (dropEnd, filter, fromFoldable, index, last, slice, (:))
import Data.List (List)
import Data.Map (Map, lookup)
import Data.Map as Map
import Data.Maybe (Maybe(Just, Nothing), fromMaybe)
import Data.String (Pattern(Pattern), split)
import Data.Traversable (sequence)
import DomOps (cn)
import DomOps as DomOps
import Effect (Effect)
import Effect.Console (error)
import Errors as ErrorsCom
import Global (readInt)
import Node as NodeCom
import Nodes as NodesCom
import Prelude hiding (div)
import React (ReactClass, ReactThis, ReactElement, createLeafElement, modifyState, component, getState, getProps)
import React.DOM (a, button, div, i, li, nav, p, p', span, span', text, ul)
import React.DOM.Props (href, target, onClick)
import ReactDOM as ReactDOM
import Schema
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps

type State = 
  { menu :: Menu
  , nodes :: Map NodeAddr NodeInfo
  , node :: Maybe NodeAddr
  , errors :: Array ErrorInfo
  , ws :: WebSocket
  , leftMenu :: Boolean
  , notifications :: Boolean
  , topMenu :: Boolean
  }
type Props =
  { menu :: Array Menu
  }

data Menu = Nodes | Errors
derive instance eqMenu :: Eq Menu
instance showMenu :: Show Menu where
  show Nodes = "Nodes"
  show Errors = "Errors"

view :: Effect Unit
view = do
  container <- DomOps.byId "container"
  let props =
        { menu: [ Nodes, Errors ]
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
                      [ div [ cn "notification d-none d-lg-block d-xl-block" ] []
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
          , div [ cn "footer" ]
            [ div [ cn "container-fluid" ]
              [ ul [ cn "nav" ]
                [ li [ cn "nav-item" ]
                  [ a [ href "http://ua--doc.ee..corp/health.html", cn "nav-link" ] [ text "Documentation" ] ]
                ]
              , div [ cn "copyright" ]
                [ text "© "
                , a [ href "https://demos.creative-tim.com/black-dashboard/examples/dashboard.html", target "_blank" ] [ text "CT" ]
                ]
              ]
            ]
          ]
        ]
      where
      menuIcon :: Menu -> String
      menuIcon Nodes = "icon-app"
      menuIcon Errors = "icon-alert-circle-exc"

      menuContent :: State -> Effect ReactElement
      menuContent { menu: Nodes, node: Just addr, nodes: nodes } =
        case lookup addr nodes of
          Just node -> pure $ createLeafElement NodeCom.reactClass node
          Nothing -> map (\_ -> createLeafElement dummy {}) $ error "bad node"
        where
        dummy :: ReactClass {}
        dummy = component "Dummy" \_ -> pure { render: pure $ span' [] }
      menuContent { menu: Nodes, nodes: nodes } =
        pure $ createLeafElement NodesCom.reactClass { nodes: fromFoldable nodes, openNode: \addr -> modifyState this _{ node = Just addr } }
      menuContent { menu: Errors, errors: errors } =
        pure $ createLeafElement ErrorsCom.reactClass { errors: errors }

      goto :: Menu -> Effect Unit
      goto Nodes = modifyState this _{ menu = Nodes, node = Nothing }
      goto Errors = modifyState this _{ menu = Errors }

      toggleLeftMenu :: Effect Unit
      toggleLeftMenu = modifyState this \s -> s{ leftMenu = not s.leftMenu }

      toggleNotifications :: Effect Unit
      toggleNotifications = modifyState this \s -> s{ notifications = not s.notifications }

      toggleTopMenu :: Effect Unit
      toggleTopMenu = modifyState this \s -> s{ topMenu = not s.topMenu }

    onMsg :: ReactThis Props State -> String -> Effect Unit
    onMsg this payload = do
      let xs = split (Pattern "::") payload
      case index xs 0 of
        Just "metric" ->
          case xs of
            [ _, name, value, time, addr ] -> do
              let mem = map (split (Pattern "~")) $ if name == "mem" then Just value else Nothing
              let fs = map (split (Pattern "~")) $ if name == "fs./" then Just value else Nothing
              let fd = map (split (Pattern "~")) $ if name == "fd" then Just value else Nothing
              let thr = map (split (Pattern "~")) $ if name == "thr" then Just value else Nothing
              updateWith
                { addr: addr
                , time: time
                , metrics: Just
                  { cpu: if name == "cpu.load" then Just value else Nothing
                  , mem: mem
                  , uptime: if name == "uptime" then Just value else Nothing
                  , fs: fs
                  , fd: fd
                  , thr: thr
                  }
                , err: Nothing
                , action: Nothing
                }
            _ -> error "bad format"
        Just "error" ->
          case xs of
            [ _, exception', stacktrace', toptrace, time, addr ] -> do
              let exception = split (Pattern "~") exception'
              let stacktrace = split (Pattern "~") stacktrace'
              let key = addr<>time
              let err = { exception, stacktrace, toptrace, time, addr, key }
              updateWith
                { addr: addr
                , time: time
                , metrics: Nothing
                , err: Just err
                , action: Nothing
                }
            _ -> error "bad format"
        Just "action" ->
          case xs of
            [ _, action, time, addr ] -> updateWith
              { addr: addr
              , time: time
              , metrics: Nothing
              , err: Nothing
              , action: Just action
              }
            _ -> error "bad format"
        _ -> error "unknown type"
      where
      updateWith :: UpdateData -> Effect Unit
      updateWith a = do
        let uptime = a.metrics >>= _.uptime
        let cpu = a.metrics >>= _.cpu
        let cpuLoad = fromMaybe [] $ map (\b -> [{ t: readInt 10 a.time, y: readInt 10 b }]) cpu
        
        mem <- case a.metrics >>= _.mem of
          Just ([ free', total' ]) -> do
            let free = readInt 10 free'
            let total = readInt 10 total'
            let used = total - free
            pure $ Just { used, free, total }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing
        let memUsed = map _.used mem
        let memFree = map _.free mem
        let memTotal = map _.total mem
        let memLoad = fromMaybe [] $ map (\b -> [{ t: readInt 10 a.time, y: b.used / 1000.0 }]) mem
        
        fs <- case a.metrics >>= _.fs of
          Just ([ usable', total' ]) -> do
            let usable = readInt 10 usable'
            let total = readInt 10 total'
            let used = total - usable
            pure $ Just { used, usable, total }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing
        let fsUsed = map _.used fs
        let fsFree = map _.usable fs
        let fsTotal = map _.total fs
        
        fd <- case a.metrics >>= _.fd of
          Just ([ open', max' ]) -> do
            let open = readInt 10 open'
            let max = readInt 10 max'
            pure $ Just { open, max }
          Just xs -> map (\_ -> Nothing) (error $ "bad format="<>show xs)
          Nothing -> pure Nothing
        let fdOpen = map _.open fd
        let fdMax = map _.max fd

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

        let action = fromMaybe [] $ map (\b -> [{t: readInt 10 a.time, label: b }]) a.action
        
        s <- getState this
        let node' = case lookup a.addr s.nodes of
              Just node -> do
                let cpuLoad' = node.cpuLoad <> cpuLoad
                let memLoad' = node.memLoad <> memLoad
                let actions' = node.actions <> action
                let minTime = max
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 cpuLoad') $ max
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 memLoad')
                      (fromMaybe 0.0 $ map _.t $ last $ dropEnd 20 actions')
                let cpuLoad'' = filter (\x -> x.t > minTime) cpuLoad'
                let memLoad'' = filter (\x -> x.t > minTime) memLoad'
                let actions'' = filter (\x -> x.t > minTime) actions'
                node
                  { lastUpdate = a.time
                  , cpuLoad = cpuLoad''
                  , memLoad = memLoad''
                  , actions = actions''
                  , cpuLast = cpu <|> node.cpuLast
                  , memLast = memUsed <|> node.memLast
                  , uptime = uptime <|> node.uptime
                  , memFree = memFree <|> node.memFree
                  , memTotal = memTotal <|> node.memTotal
                  , fsUsed = fsUsed <|> node.fsUsed
                  , fsFree = fsFree <|> node.fsFree
                  , fsTotal = fsTotal <|> node.fsTotal
                  , fdOpen = fdOpen <|> node.fdOpen
                  , fdMax = fdMax <|> node.fdMax
                  , thr = thr <|> node.thr
                  }
              Nothing ->
                { addr: a.addr
                , lastUpdate: a.time
                , cpuLoad: cpuLoad
                , memLoad: memLoad
                , actions: action
                , cpuLast: cpu
                , memLast: memUsed
                , uptime: uptime
                , memFree: memFree
                , memTotal: memTotal
                , fsUsed: fsUsed
                , fsFree: fsFree
                , fsTotal: fsTotal
                , fdOpen: Nothing
                , fdMax: Nothing
                , thr: Nothing
                }
        modifyState this \s' -> s' { nodes = Map.insert node'.addr node' s'.nodes }
        case a.err of
          Just err -> modifyState this \s' -> s' { errors = err : (slice 0 99 s'.errors) }
          Nothing -> pure unit

    errHandler :: List String -> Effect Unit
    errHandler xs = void $ sequence $ map error xs
