package metrics

import zio._
import ftier._, ws._
import zd.proto._, api._

package object app {
  def send(x: Push): ZIO[WsContext, Err, Unit] = {
    Ws.send(Binary(Chunk.fromArray(encode[Push](x))))
  }
}
