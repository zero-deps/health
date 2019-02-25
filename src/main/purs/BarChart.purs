module BarChart
  ( reactClass
  ) where

import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, component, getProps)
import React.DOM (canvas)
import ReactOps (Ref, ref', createRef)
import Schema (NumPoint)

type State = {}
type Props = {
  points :: Array NumPoint
}

reactClass :: ReactClass Props
reactClass = component "BarChart" \this -> do
  p <- getProps this
  let r = createRef
  pure
    { state: {}
    , render: pure $ canvas [ ref' r ] []
    , componentDidMount: createChart r p.points
    , componentDidUpdate: \p' _ _ -> updateChart p'.points
    , componentWillUnmount: destroyChart
    }

foreign import createChart
  :: Ref
  -> Array NumPoint
  -> Effect Unit
foreign import updateChart
  :: Array NumPoint
  -> Effect Unit
foreign import destroyChart
  :: Effect Unit
