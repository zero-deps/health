module Node
  ( reactClass
  , NodeData
  ) where

import DateOps (localDateTime)
import DomOps (cn)
import Effect (Effect)
import Prelude hiding (div)
import React (ReactClass, ReactElement, ReactThis, component, getProps)
import React.DOM (div, h4, table, tbody', td', text, th', thead, tr, tr')
import React.DOM.Props (onClick)

type State = {}
type NodeData =
  { addr :: String
  }
type Props = NodeData

reactClass :: ReactClass Props
reactClass = component "Node" \this -> do
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
        [ div [ cn "col-12" ]
          [ div [ cn "card card-chart" ]
            [ div [ cn "card-header" ]
              []
            ]
          ]
        ]
