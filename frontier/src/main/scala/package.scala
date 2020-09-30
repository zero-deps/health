package zero

import zio._

package object ws {
  def parseDouble(v: String): IO[ParseErr, Double] = IO.fromOption(v.toDoubleOption).orElseFail(ParseErr(v))
  def parseInt(v: String): IO[ParseErr, Int] = IO.fromOption(v.toIntOption).orElseFail(ParseErr(v))
}