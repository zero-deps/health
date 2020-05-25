package .stats

import zd.proto.purs.{Purescript, io}

object Purs extends App {
  val res = Purescript.generate[Push, Pull](moduleEncode="Api.Pull", moduleDecode="Api.Push", "Api.Common", Nil)
  res.purs.foreach{ case (filename, content) =>
    io.writeToFile(s"src/main/purs/$filename.purs", content)
  }
}
