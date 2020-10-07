val zionio = "1.0.0-RC9"

lazy val frontier2 = project.in(file(".")).settings(
  libraryDependencies += "dev.zio" %% "zio-nio" % zionio
)
