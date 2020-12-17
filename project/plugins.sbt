libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"
libraryDependencies += "io.github.zero-deps" %% "ext-bld" % "2.4.1.g7c28a4a"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.2")

githubOwner := "zero-deps"
githubRepository := "ext"
