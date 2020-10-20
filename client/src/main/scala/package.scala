package .stats

import com.sun.management.{OperatingSystemMXBean, UnixOperatingSystemMXBean}
import java.lang.management.{ManagementFactory}
import zero.ext._, int._, option._
import zd.proto.api.MessageCodec
import zd.proto.macrosapi.{caseCodecAuto, sealedTraitCodecAuto}

package object client {
  def cpu_mem(): Option[String] = {
    ManagementFactory.getOperatingSystemMXBean match {
      case os: OperatingSystemMXBean =>
        /* CPU load (percentage) */
        val cpu = os.getCpuLoad match {
          case x if x < 0 => "" /* not available */
          case x => (100*x).toInt.toString
        }
        /* Memory (Mbytes) */
        val free = os.getFreeMemorySize
        val total = os.getTotalMemorySize
        val mem = ManagementFactory.getMemoryMXBean
        val heapMem = mem.getHeapMemoryUsage.getUsed
        s"${cpu}~${free/i"1'000'000"}~${total/i"1'000'000"}~${heapMem/i"1'000'000"}".some
      case _ => none
    }
  }

  def fd(): Option[String] = {
    ManagementFactory.getOperatingSystemMXBean match {
      case os: UnixOperatingSystemMXBean =>
        /* File descriptor count */
        val open = os.getOpenFileDescriptorCount
        val max = os.getMaxFileDescriptorCount
        s"${open}~${max}".some
      case _ => none
    }
  }

  def toErrorStat(msg: Option[String], cause: Throwable): Client.ErrorStat = {
    Client.ErrorStat(
      msg = msg
    , cause = cause.getMessage
    , st = cause.getStackTrace.filter(_.getClassName.startsWith("cms")).take(2).map(_.toString).toList
    // , st = cause.getStackTrace.take(2).map(_.toString).toList
    )
  }

  implicit val ClientMsgCodec: MessageCodec[ClientMsg] = {
    implicit val MetricMsgCodec: MessageCodec[MetricMsg] = caseCodecAuto[MetricMsg]
    implicit val MeasureMsgCodec: MessageCodec[MeasureMsg] = caseCodecAuto[MeasureMsg]
    implicit val ErrorMsgCodec: MessageCodec[ErrorMsg] = caseCodecAuto[ErrorMsg]
    implicit val ActionMsgCodec: MessageCodec[ActionMsg] = caseCodecAuto[ActionMsg]
    sealedTraitCodecAuto[ClientMsg]
  }
}