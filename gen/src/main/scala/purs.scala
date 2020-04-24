package .stats

import zd.proto.{Res, Purescript}

object Purs extends App {
  val res = Purescript.generate[.stats.Push, .stats.Pull](moduleEncodeName="Pull", moduleDecodeName="Push", "CommonApi")
  Res.writeToFile("../src/main/purs/Common.purs", res.common)
  Res.writeToFile("../src/main/purs/Pull.purs", res.encode)
  Res.writeToFile("../src/main/purs/Push.purs", res.decode)
}