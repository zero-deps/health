module Main
  ( view
  ) where

import Data.Array (findIndex, index, slice, updateAt, snoc)
import Data.List (List)
import Data.Maybe (Maybe(Just, Nothing), fromMaybe)
import Data.String (Pattern(Pattern), split)
import Data.Traversable (sequence)
import DateOps (localDateTime)
import DomOps as DomOps
import Effect (Effect)
import Effect.Console (error)
import Prelude hiding (div)
import React (ReactClass, ReactThis, ReactElement, createLeafElement, modifyState, component, getState)
import React.DOM (a, button, div, h4, i, li, nav, p', span, table, tbody', td', text, th', thead, tr', ul, p)
import React.DOM.Props (className, href, target)
import ReactDOM as ReactDOM
import Web.Socket.WebSocket (WebSocket)
import WsOps as WsOps

type State = 
  { menu :: Menu
  , nodes :: Array NodeInfo
  , ws :: WebSocket
  }
data Menu = Nodes | Errors
type NodeInfo =
  { sys :: String
  , addr :: String
  , lastUpdate :: String
  }
type Props = {}

view :: Effect Unit
view = do
  container <- DomOps.byId "container"
  let element = createLeafElement reactClass {}
  void $ ReactDOM.render element container

reactClass :: ReactClass Props
reactClass = component "Main" \this -> do
  h <- map (fromMaybe "127.0.0.1:8002") DomOps.host 
  ws <- WsOps.create $ "ws:/"<>h<>"/stats/ws"
  pure
    { state:
      { menu: Nodes
      , nodes: []
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
      pure $
        div [ className "wrapper" ]
          [ div [ className "sidebar" ]
            [ div [ className "sidebar-wrapper" ]
              [ ul [ className "nav" ]
                [ li [ className "active" ]
                  [ a [ href "#" ]
                    [ i [ className "tim-icons icon-app" ] []
                    , p' [ text "Nodes" ]
                    ]
                  ]
                , li []
                  [ a [ href "#" ]
                    [ i [ className "tim-icons icon-alert-circle-exc" ] []
                    , p' [ text "Errors" ]
                    ]
                  ]
                , li []
                  [ a [ href "monitor.html" ]
                    [ i [ className "tim-icons icon-compass-05" ] []
                    , p' [ text "Legacy" ]
                    ]
                  ]
                ]
              ]
            ]
          , div [ className "main-panel" ]
            [ nav [ className "navbar navbar-expand-lg navbar-absolute navbar-transparent" ]
              [ div [ className "container-fluid" ]
                [ div [ className "navbar-wrapper" ]
                  [ div [ className "navbar-toggle d-inline" ]
                    [ button [ className "navbar-toggler" ]
                      [ span [ className "navbar-toggler-bar bar1" ] []
                      , span [ className "navbar-toggler-bar bar2" ] []
                      , span [ className "navbar-toggler-bar bar3" ] []
                      ]
                    ]
                  , a [ href "", className "navbar-brand" ]
                    [ text "Monitor" ]
                  ]
                , button [ className "navbar-toggler" ]
                  [ span [ className "navbar-toggler-bar navbar-kebab" ] []
                  , span [ className "navbar-toggler-bar navbar-kebab" ] []
                  , span [ className "navbar-toggler-bar navbar-kebab" ] []
                  ]
                , div [ className "collapse navbar-collapse" ]
                  [ ul [ className "navbar-nav ml-auto" ]
                    [ li [ className "dropdown nav-item" ]
                      [ a [ href "#", className "dropdown-toggle nav-link" ]
                        [ div [ className "notification d-none d-lg-block d-xl-block" ] []
                        , i [ className "tim-icons icon-sound-wave" ] []
                        , p [ className "d-lg-none" ]
                          [ text "Notifications" ]
                        ]
                      , ul [ className "dropdown-menu dropdown-menu-right dropdown-navbar" ]
                        [ li [ className "nav-link" ]
                          [ a [ href "#", className "nav-item dropdown-item" ]
                            [ text "No notifications" ]
                          ]
                        ]
                      ]
                    , li [ className "separator d-lg-none" ] []
                    ]
                  ]
                ]
              ]
            , div [ className "content" ]
              [ div [ className "row" ]
                [ div [ className "col-md-12" ]
                  [ div [ className "card" ]
                    [ div [ className "card-header" ]
                      [ h4 [ className "card-title" ]
                        [ text "Nodes" ]
                      ]
                    , div [ className "card-body" ]
                      [ div [ className "table-responsive" ]
                        [ table [ className "table tablesorter" ]
                          [ thead [ className "text-primary" ]
                            [ tr'
                              [ th' [ text "System" ]
                              , th' [ text "Address" ]
                              , th' [ text "Last Update" ]
                              ]
                            ]
                          , tbody' $ map (\x ->
                              tr'
                              [ td' [ text x.sys ]
                              , td' [ text x.addr ]
                              , td' [ text $ localDateTime x.lastUpdate ]
                              ]) s.nodes
                          ]
                        ]
                      ]
                    ]
                  ]
                ]
              ]
            , div [ className "footer" ]
              [ div [ className "container-fluid" ]
                [ ul [ className "nav" ]
                  [ li [ className "nav-item" ]
                    [ a [ href "http://ua--doc.ee..corp", className "nav-link" ]
                      [ text "Documentation" ]
                    ]
                  ]
                , div [ className "copyright" ]
                  [ text "© 2019 "
                  , a [ href "https://demos.creative-tim.com/black-dashboard/examples/dashboard.html", target "_blank" ]
                    [ text "CT" ]
                  ]
                ]
              ]
            ]
          ]

    onMsg :: ReactThis Props State -> String -> Effect Unit
    onMsg this payload = do
      let xs = split (Pattern "::") payload
      case index xs 0 of
        Just "metric" ->
          case slice 1 6 xs of
            [ name, value, time, sys, addr ] -> do
              s <- getState this
              let lastUpdate = time
              let node = { sys, addr, lastUpdate }
              let nodes' = case findIndex (\x -> x.sys == sys && x.addr == addr) s.nodes of
                    Just a -> fromMaybe s.nodes $ updateAt a node s.nodes
                    Nothing -> snoc s.nodes node
              modifyState this _{ nodes = nodes' }
            _ -> pure unit
        Just "error" -> pure unit
        -- : className : message : stacktrace : time : sys : addr
        Just "action" -> pure unit
        -- : user : action : time : sys : addr
        _ -> pure unit

    errHandler :: List String -> Effect Unit
    errHandler xs = void $ sequence $ map error xs

