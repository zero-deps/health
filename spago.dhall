{ name = "health"
, dependencies =
  [ "console"
  , "effect"
  , "protobuf"
  , "psci-support"
  , "react-dom"
  , "web-html"
  , "web-socket"
  ]
, packages = ./packages.dhall
, sources = [ "src/main/purs/**/*.purs" ]
}
