module Features
  ( reactClass
  ) where

import Prelude (bind, pure, show, ($), (+), (<>))

import Effect (Effect)

import Data.Array (foldl)
import Data.Int as Int
import Data.Map (Map, lookup)
import Data.Maybe (Maybe, fromMaybe)

import React (ReactClass, ReactElement, ReactThis, component, createLeafElement, getProps)
import React.DOM (div, h2, h5, text)
import React.DOM.Props (style)
import DomOps (cn)

import Schema (NumPoint, Feature)
import YearChart as YearChart

type State = {}
type Props =
  { features :: Map Feature (Array NumPoint)
  }

reactClass :: ReactClass Props
reactClass = component "Features" \this -> do
  pure
    { state: {}
    , render: render this
    }
  where
    render :: ReactThis Props State -> Effect ReactElement
    render this = do
      props <- getProps this
      pure $
        div [ cn "row" ]
        [ usageCard "Disable a user"           $ lookup "disabled-users" props.features
        , usageCard "Review/reject of content" $ lookup "wc-flow"        props.features
        ]

    usageCard :: String -> Maybe (Array NumPoint) -> _
    usageCard title xs' =
      div [ cn "col-12" ]
      [ div [ cn "card card-chart" ]
        [ div [ cn "card-header" ]
          [ h5 [ cn "card-category", style { textTransform: "none" } ] [ text title ]
          , h2 [ cn "card-title" ]
            [ text $ "Total usage: " <> (show $ fromMaybe 0 $ do
                xs <- xs'
                Int.fromNumber $ foldl (\acc x -> acc + x.y) 0.0 xs) ]
          ]
        , div [ cn "card-body" ]
          [ div [ cn "chart-area" ]
            [ createLeafElement YearChart.reactClass 
              { points: fromMaybe [] xs'
              , label: ""
              }
            ]
          ]
        ]
      ]
