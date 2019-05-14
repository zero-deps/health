package .stats

import zd.proto.Purescript.generate
import java.io.{BufferedWriter, FileWriter}

object Purs extends App {
  val res = generate[.stats.StatMsg, .stats.Pull](moduleName="Api")
  val w = new BufferedWriter(new FileWriter("../src/main/purs/Api.purs"))
  w.write(res.format)
  w.close()
}
