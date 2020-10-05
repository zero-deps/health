package .stats

import zd.proto.api
import zio._

object encoding {
  type Codec[A] = api.MessageCodec[A]
  case class DecodeErr(e: Throwable)

  def encode[A: Codec](a: A): Array[Byte] = api.encode(a)
  def decode[A: Codec](b: Array[Byte]): IO[DecodeErr, A] = IO.effect(api.decode(b)).mapError(DecodeErr(_))
}
