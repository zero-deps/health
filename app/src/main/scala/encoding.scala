package .stats

import zd.proto.api
import zio._
import ftier.Throwed

object encoding {
  type Codec[A] = api.MessageCodec[A]

  def encode[A: Codec](a: A): Array[Byte] = api.encode(a)
  def decode[A: Codec](b: Array[Byte]): IO[Throwed, A] = IO.effect(api.decode(b)).mapError(Throwed(_))
}
