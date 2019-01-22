module Main
  ( view
  ) where

import Data.Array (findIndex, index, slice, modifyAt, snoc, head, find, (:), (!!))
import Data.List (List)
import Data.Maybe (Maybe(Just, Nothing), fromMaybe)
import Data.String (Pattern(Pattern), split)
import Data.Traversable (sequence)
import DomOps as DomOps
import DomOps (cn)
import Effect (Effect)
import Effect.Console (error)
import Errors as ErrorsCom
import Node as NodeCom
import Nodes as NodesCom
import Prelude hiding (div)
import React (ReactClass, ReactThis, ReactElement, createLeafElement, modifyState, component, getState, getProps)
import React.DOM (a, button, div, i, li, nav, p, p', span, span', text, ul)
import React.DOM.Props (href, target, onClick)
import ReactDOM as ReactDOM
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps
import Global (readInt)

type State = 
  { menu :: Menu
  , nodes :: Array NodesCom.NodeInfo
  , node :: Maybe NodeAddr
  , errors :: Array ErrorsCom.ErrorInfo
  , ws :: WebSocket
  }
type NodeAddr = String
data Menu = Nodes | Errors | Legacy
derive instance eqMenu :: Eq Menu
instance showMenu :: Show Menu where
  show Nodes = "Nodes"
  show Errors = "Errors"
  show Legacy = "Legacy"
type Props =
  { menu :: Array Menu
  }

view :: Effect Unit
view = do
  container <- DomOps.byId "container"
  let props =
        { menu: [ Nodes, Errors, Legacy ]
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
      , nodes: []
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
      menuIcon Legacy = "icon-compass-05"

      menuContent :: State -> Effect ReactElement
      menuContent { menu: Nodes, node: Just addr, nodes: nodes } =
        case find (\a -> a.addr == addr) nodes of
          Just node -> pure $ createLeafElement NodeCom.reactClass node
          Nothing -> map (\_ -> createLeafElement dummy {}) $ error "bad node"
      menuContent { menu: Nodes, nodes: nodes } =
        pure $ createLeafElement NodesCom.reactClass { nodes: nodes, openNode: \addr -> modifyState this _{ node = Just addr } }
      menuContent { menu: Errors, errors: errors } =
        pure $ createLeafElement ErrorsCom.reactClass { errors: errors }
      menuContent { menu: Legacy } =
        map (\_ -> createLeafElement dummy {}) $ error "impossible"

      dummy :: ReactClass {}
      dummy = component "Dummy" \_ -> pure { render: pure $ span' [] }

      goto :: Menu -> Effect Unit
      goto Nodes = modifyState this _{ menu = Nodes, node = Nothing }
      goto Errors = modifyState this _{ menu = Errors }
      goto Legacy = DomOps.openUrl "../legacy/index.html"

    onMsg :: ReactThis Props State -> String -> Effect Unit
    onMsg this payload = do
      let xs = split (Pattern "::") payload
      case index xs 0 of
        Just "metric" ->
          case slice 1 5 xs of
            [ name, value, time, addr ] -> do
              s <- getState this
              let lastUpdate = time
              let cpuLoad = if name == "cpu.load" then [{ t: readInt 10 time, y: readInt 10 value }] else []
              let cpuLast = if name == "cpu.load" then Just value else Nothing
              let nodes' = case findIndex (\x -> x.addr == addr) s.nodes of
                    Just a -> fromMaybe s.nodes $ modifyAt a (\b -> { addr: addr, lastUpdate: lastUpdate, cpuLoad: b.cpuLoad<>cpuLoad, cpuLast: fromMaybe b.cpuLast cpuLast }) s.nodes
                    Nothing -> snoc s.nodes { addr: addr, lastUpdate: lastUpdate, cpuLoad: cpuLoad, cpuLast: fromMaybe "CPU" cpuLast }
              modifyState this _{ nodes = nodes' }
            _ -> error "bad size"
        Just "error" ->
          case slice 1 5 xs of
            [ exception', stacktrace', time, addr ] -> do
              s <- getState this
              let lastUpdate = time
              let nodes' = case findIndex (\x -> x.addr == addr) s.nodes of
                    Just a -> fromMaybe s.nodes $ modifyAt a _{ addr=addr, lastUpdate=lastUpdate } s.nodes
                    Nothing -> snoc s.nodes { addr: addr, lastUpdate: lastUpdate, cpuLoad: [], cpuLast: "CPU" }
              modifyState this _{ nodes = nodes' }
              let exception = split (Pattern "~") exception'
              let stacktrace'' = map (split (Pattern "~")) $ split (Pattern "~~") stacktrace'
              let stacktrace = map (case _ of
                    [ method, file ] -> method<>"("<>file<>")"
                    _ -> "bad format") stacktrace''
              let file = fromMaybe "bad format" $ head stacktrace'' >>= (_ !! 1)
              let error = { exception, stacktrace, file, time, addr }
              modifyState this _{ errors = error : (slice 0 100 s.errors) }
            _ -> error "bad size"
        Just "action" ->
          case slice 1 4 xs of
            [ action, time, addr ] -> do
              s <- getState this
              let lastUpdate = time
              let nodes' = case findIndex (\x -> x.addr == addr) s.nodes of
                    Just a -> fromMaybe s.nodes $ modifyAt a _{ addr=addr, lastUpdate=lastUpdate } s.nodes
                    Nothing -> snoc s.nodes { addr: addr, lastUpdate: lastUpdate, cpuLoad: [], cpuLast: "CPU" }
              modifyState this _{ nodes = nodes' }
            _ -> error "bad size"
        _ -> error "unknown type"

    errHandler :: List String -> Effect Unit
    errHandler xs = void $ sequence $ map error xs
