package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import scala.util.control.NonFatal
import io.circe.Json

/**
 * Fresh rooted recorded-action reevaluation using separate caller input.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelDurableHandoffProbe {
  def main(args: Array[String]): Unit = {
    val pid = ProcessHandle.current().pid()
    val (report, exitcode) = try {
      require(args.length == 2, "durable probe requires exactly root and independent input paths")
      val root = Path.of(args(0)).normalize()
      val input = Path.of(args(1)).normalize()
      require(root.isAbsolute && input.isAbsolute, "durable probe paths must be absolute")
      require(!input.startsWith(root), "independent input must be outside consuming root")
      val request = InternalModelDurableHandoffInput.decode(Files.readAllBytes(input).toVector)
        .fold(message => throw new IllegalArgumentException(message), identity)
      val result = InternalModelContinuationActionGate.evaluate(root, request)
      result.fold(
        conclusion => (_envelope("validator-rejected", pid, Json.Null, Json.obj(
          "origin" -> Json.fromString("InternalModelContinuationActionGate.evaluate"),
          "conclusionType" -> Json.fromString(conclusion.getClass.getName), "diagnostic" -> Json.fromString(result.show))), 2),
        value => {
          val status = value.eligibility match {
            case InternalModelContinuationEligibility.Eligible => "eligible"
            case InternalModelContinuationEligibility.Incomplete => "incomplete"
            case InternalModelContinuationEligibility.Inconsistent => "inconsistent"
          }
          (_envelope(status, pid, InternalModelFreshProcessProbe.actionReport(value), Json.Null), if (status == "eligible") 0 else 2)
        }
      )
    } catch {
      case NonFatal(error) => (_envelope("harness-error", pid, Json.Null, Json.obj(
        "origin" -> Json.fromString("InternalModelDurableHandoffProbe"), "errorType" -> Json.fromString(error.getClass.getName),
        "diagnostic" -> Json.fromString(Option(error.getMessage).getOrElse("no diagnostic")))), 3)
    }
    System.out.write(InternalModelFreshProcessProbe.canonicalBytes(report).toArray)
    System.out.flush()
    System.exit(exitcode)
  }

  private def _envelope(status: String, pid: Long, result: Json, failure: Json): Json = Json.obj(
    "status" -> Json.fromString(status), "pid" -> Json.fromLong(pid), "result" -> result, "failure" -> failure)
}
