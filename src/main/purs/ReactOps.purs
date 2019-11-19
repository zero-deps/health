module ReactOps where

import React.DOM.Props (Props, unsafeMkProps)

foreign import data Ref :: Type

foreign import createRef :: Ref

ref' :: Ref -> Props
ref' = unsafeMkProps "ref"
