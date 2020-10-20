libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"
libraryDependencies += "io.github.zero-deps" %% "ext-git" % "2.2.0"

scalacOptions in Compile ++= Seq("-deprecation", "-feature")

resolvers += Resolver.jcenterRepo
