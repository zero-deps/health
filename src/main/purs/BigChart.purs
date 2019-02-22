module BigChart
  ( reactClass
  ) where

import Data.Maybe (fromMaybe)
import DomOps (cn)
import Effect (Effect)
import FormatOps (formatNum, duration)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps, createLeafElement)
import React.DOM (canvas, div, div', h2, h4, h5, label, span, text, i, table, thead, tbody', th', th, tr', td', td)
import React.DOM.Props (_id, style)
import Schema
import Errors as Errors

type State = {}
type Props =
  { cpuPoints :: Array NumPoint
  , memPoints :: Array NumPoint
  , actionPoints :: Array StrPoint
  }

reactClass :: ReactClass Props
reactClass = component "BigChart" \this -> do
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
  render _ = pure $ canvas [ _id "chartBig1" ] []

foreign import createChart
  :: Array NumPoint
  -> Array NumPoint
  -> Array StrPoint
  -> Effect Unit
foreign import updateChart
  :: Array NumPoint
  -> Array NumPoint
  -> Array StrPoint
  -> Effect Unit
foreign import destroyChart
  :: Effect Unit
