package .stats

import zero.protopurs.{Purescript, io}

object Purs extends App {
  val res = Purescript.generate[Push, Pull](moduleEncode="Api.Pull", moduleDecode="Api.Push", "Api.Common", Nil)
  res.purs.foreach{ case (filename, content) =>
    io.writeToFile(s"web/src/$filename.purs", content)
  }
}
