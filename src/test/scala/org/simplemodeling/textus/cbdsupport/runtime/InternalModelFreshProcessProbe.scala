package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.util.control.NonFatal
import io.circe.{Json, Printer}

/**
 * Test-only fresh JVM entry point and complete structural evidence transport.
 * This report is not an internal-model artifact, semantic codec, or authority.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelFreshProcessProbe {
  def main(args: Array[String]): Unit = {
    val pid = ProcessHandle.current().pid()
    val (report, exitcode) = try {
      require(args.length == 1, "probe requires exactly one consuming project root")
      val root = Path.of(args(0))
      require(root.isAbsolute, "consuming project root must be absolute")
      val result = InternalModelRehydrationValidator.validate(root)
      result.fold(
        conclusion => (_report("validator-rejected", pid, Json.Null, Json.obj(
          "origin" -> Json.fromString("InternalModelRehydrationValidator.validate"),
          "conclusionType" -> Json.fromString(conclusion.getClass.getName),
          "diagnostic" -> Json.fromString(result.show)
        )), 2),
        state => (_report("success", pid, stateReport(state), Json.Null), 0)
      )
    } catch {
      case NonFatal(error) => (_report("harness-error", pid, Json.Null, Json.obj(
        "origin" -> Json.fromString("InternalModelFreshProcessProbe"),
        "errorType" -> Json.fromString(error.getClass.getName),
        "diagnostic" -> Json.fromString(Option(error.getMessage).getOrElse("no diagnostic"))
      )), 3)
    }
    System.out.write(canonicalBytes(report).toArray)
    System.out.flush()
    System.exit(exitcode)
  }

  def stateReport(state: InternalModelRehydratedState): Json = _value(state)

  def actionReport(report: InternalModelContinuationReport): Json = _value(report)

  def canonicalBytes(report: Json): Vector[Byte] =
    (Printer.noSpacesSortKeys.print(report) + "\n").getBytes(StandardCharsets.UTF_8).toVector

  private def _report(status: String, pid: Long, state: Json, failure: Json): Json = Json.obj(
    "status" -> Json.fromString(status), "pid" -> Json.fromLong(pid),
    "state" -> state, "failure" -> failure
  )

  /** Preserve scalar types, Product field names, and every ordered value, including sidecars. */
  private def _value(value: Any): Json = value match {
    case text: String => _scalar("scala.String", Json.fromString(text))
    case flag: Boolean => _scalar("scala.Boolean", Json.fromBoolean(flag))
    case number: Byte => _scalar("scala.Byte", Json.fromInt(number.toInt))
    case number: Short => _scalar("scala.Short", Json.fromInt(number.toInt))
    case number: Int => _scalar("scala.Int", Json.fromInt(number))
    case number: Long => _scalar("scala.Long", Json.fromLong(number))
    case None => Json.obj("type" -> Json.fromString("scala.None"))
    case Some(item) => Json.obj("type" -> Json.fromString("scala.Some"), "value" -> _value(item))
    case items: Vector[?] => Json.obj(
      "type" -> Json.fromString("scala.collection.immutable.Vector"),
      "values" -> Json.fromValues(items.map(_value))
    )
    case items: List[?] => Json.obj(
      "type" -> Json.fromString("scala.collection.immutable.List"),
      "values" -> Json.fromValues(items.map(_value))
    )
    case product: Product =>
      val names = product.productElementNames.toVector
      val fields = product.productIterator.toVector
      require(names.size == product.productArity && fields.size == names.size, "incomplete Product evidence")
      Json.obj(
        "type" -> Json.fromString(product.getClass.getName),
        "fields" -> Json.fromValues(names.zip(fields).map { case (name, item) =>
          Json.obj("name" -> Json.fromString(name), "value" -> _value(item))
        })
      )
    case null => throw new IllegalArgumentException("null is not supported state evidence")
    case other => throw new IllegalArgumentException(s"unsupported state evidence type: ${other.getClass.getName}")
  }

  private def _scalar(kind: String, value: Json): Json =
    Json.obj("type" -> Json.fromString(kind), "value" -> value)
}
