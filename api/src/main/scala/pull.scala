package 
package stats

import zd.proto.api._
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

object Pull {
  implicit val HealthAskC = caseCodecAuto[HealthAsk]
  implicit val PullC = sealedTraitCodecAuto[Pull]
}

@RestrictedN(10)
sealed trait Pull

@N(20) final case class HealthAsk(@N(1) host: String) extends Pull