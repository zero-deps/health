module Main
  ( view
  ) where

import Data.Array (fromFoldable, index, slice, head, last, dropEnd, filter, (:), (!!))
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
        div [ cn "wrapper" ]
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
            [ nav [ cn "navbar navbar-expand-lg navbar-absolute navbar-transparent" ]
              [ div [ cn "container-fluid" ]
                [ div [ cn "navbar-wrapper" ]
                  [ div [ cn "navbar-toggle d-inline" ]
                    [ button [ cn "navbar-toggler" ]
                      [ span [ cn "navbar-toggler-bar bar1" ] []
                      , span [ cn "navbar-toggler-bar bar2" ] []
                      , span [ cn "navbar-toggler-bar bar3" ] []
                      ]
                    ]
                  , a [ href "#", cn "navbar-brand" ]
                    [ text "Monitor" ]
                  ]
                , button [ cn "navbar-toggler" ]
                  [ span [ cn "navbar-toggler-bar navbar-kebab" ] []
                  , span [ cn "navbar-toggler-bar navbar-kebab" ] []
                  , span [ cn "navbar-toggler-bar navbar-kebab" ] []
                  ]
                , div [ cn "collapse navbar-collapse" ]
                  [ ul [ cn "navbar-nav ml-auto" ]
                    [ li [ cn "dropdown nav-item" ]
                      [ a [ href "#", cn "dropdown-toggle nav-link" ]
                        [ div [ cn "notification d-none d-lg-block d-xl-block" ] []
                        , i [ cn "tim-icons icon-sound-wave" ] []
                        , p [ cn "d-lg-none" ]
                          [ text "Notifications" ]
                        ]
                      , ul [ cn "dropdown-menu dropdown-menu-right dropdown-navbar" ]
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
            , div [ cn "content" ]
              [ menuContent'
              ]
            , div [ cn "footer" ]
              [ div [ cn "container-fluid" ]
                [ ul [ cn "nav" ]
                  [ li [ cn "nav-item" ]
                    [ a [ href "http://ua--doc.ee..corp/health.html", cn "nav-link" ]
                      [ text "Documentation" ]
                    ]
                  ]
                , div [ cn "copyright" ]
                  [ text "© "
                  , a [ href "https://demos.creative-tim.com/black-dashboard/examples/dashboard.html", target "_blank" ]
                    [ text "CT" ]
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

    onMsg :: ReactThis Props State -> String -> Effect Unit
    onMsg this payload = do
      let xs = split (Pattern "::") payload
      case index xs 0 of
        Just "metric" ->
          case slice 1 5 xs of
            [ name, value, time, addr ] -> updateWith
              { addr: addr
              , time: time
              , metrics: Just
                { cpu: if name == "cpu.load" then Just value else Nothing
                , mem: if name == "mem.used" then Just value else Nothing 
                , uptime: if name == "sys.uptime" then Just value else Nothing
                , memFree: if name == "mem.free" then Just value else Nothing
                , memTotal: if name == "mem.total" then Just value else Nothing
                , fsUsed: if name == "fs./.used" then Just value else Nothing
                , fsFree: if name == "fs./.free" then Just value else Nothing
                , fsTotal: if name == "fs./.total" then Just value else Nothing
                }
              , err: Nothing
              , action: Nothing
              }
            _ -> error "bad format"
        Just "error" ->
          case slice 1 5 xs of
            [ exception', stacktrace', time, addr ] -> do
              let exception = split (Pattern "~") exception'
              let stacktrace'' = map (split (Pattern "~")) $ split (Pattern "~~") stacktrace'
              let stacktrace = map (case _ of
                    [ method, file ] -> method<>"("<>file<>")"
                    _ -> "bad format") stacktrace''
              let file = fromMaybe "bad format" $ head stacktrace'' >>= (_ !! 1)
              let err = { exception, stacktrace, file, time, addr }
              updateWith
                { addr: addr
                , time: time
                , metrics: Nothing
                , err: Just err
                , action: Nothing
                }
            _ -> error "bad format"
        Just "action" ->
          case slice 1 4 xs of
            [ action, time, addr ] -> updateWith
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
        let uptime' = a.metrics >>= _.uptime
        let cpu' = a.metrics >>= _.cpu
        let mem' = map (readInt 10) $ a.metrics >>= _.mem
        let memFree' = map (readInt 10) $ a.metrics >>= _.memFree
        let memTotal' = map (readInt 10) $ a.metrics >>= _.memTotal
        let fsUsed' = map (readInt 10) $ a.metrics >>= _.fsUsed
        let fsFree' = map (readInt 10) $ a.metrics >>= _.fsFree
        let fsTotal' = map (readInt 10) $ a.metrics >>= _.fsTotal
        let cpuLoad = fromMaybe [] $ map (\b -> [{ t: readInt 10 a.time, y: readInt 10 b }]) cpu'
        let memLoad = fromMaybe [] $ map (\b -> [{ t: readInt 10 a.time, y: b / 1000.0 }]) mem'
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
                  , cpuLast = fromMaybe node.cpuLast cpu'
                  , memLast = fromMaybe node.memLast mem'
                  , uptime = fromMaybe node.uptime uptime'
                  , memFree = fromMaybe node.memFree memFree'
                  , memTotal = fromMaybe node.memTotal memTotal'
                  , fsUsed = fromMaybe node.fsUsed fsUsed'
                  , fsFree = fromMaybe node.fsFree fsFree'
                  , fsTotal = fromMaybe node.fsTotal fsTotal'
                  }
              Nothing ->
                { addr: a.addr
                , lastUpdate: a.time
                , cpuLoad: cpuLoad
                , memLoad: memLoad
                , actions: action
                , cpuLast: fromMaybe "0" cpu'
                , memLast: fromMaybe 0.0 mem'
                , uptime: fromMaybe "0" uptime'
                , memFree: fromMaybe 0.0 memFree'
                , memTotal: fromMaybe 0.0 memTotal'
                , fsUsed: fromMaybe 0.0 fsUsed'
                , fsFree: fromMaybe 0.0 fsFree'
                , fsTotal: fromMaybe 0.0 fsTotal'
                }
        modifyState this \s' -> s' { nodes = Map.insert node'.addr node' s'.nodes }
        case a.err of
          Just err -> modifyState this \s' -> s' { errors = err : (slice 0 99 s'.errors) }
          Nothing -> pure unit

    errHandler :: List String -> Effect Unit
    errHandler xs = void $ sequence $ map error xs
