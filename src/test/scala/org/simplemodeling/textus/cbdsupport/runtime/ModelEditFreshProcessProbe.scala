package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Base64
import scala.util.control.NonFatal
import io.circe.Json

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Test-only observation of actual retained packages; no application or approval is reconstructed. */
private[runtime] object ModelEditFreshProcessProbe {
  final case class Observed(reading: ModelEditCrossView.PromotionRead, mapping: ModelEditCmlProjection.Mapping,
    publication: ModelEditSourceProjection.Publication, postvalidation: Json, canonicaltext: String,
    baselinematches: Boolean, resumediagnostic: String)

  def main(args: Array[String]): Unit = {
    val pid = ProcessHandle.current().pid()
    val (report, exitcode) = try {
      require(args.length == 2, "probe requires actual accepted root and independently supplied continuation request")
      val root = Path.of(args(0))
      require(root.isAbsolute, "actual accepted root must be absolute")
      val request = InternalModelDurableHandoffInput.decode(Files.readAllBytes(Path.of(args(1))).toVector)
        .fold(message => throw new IllegalArgumentException(message), value => value)
      val observed = observe(root, request)
      (Json.obj("status" -> Json.fromString("success"), "pid" -> Json.fromLong(pid),
        "observation" -> reportValue(observed), "failure" -> Json.Null), 0)
    } catch {
      case NonFatal(error) => (Json.obj("status" -> Json.fromString("rejected"), "pid" -> Json.fromLong(pid),
        "observation" -> Json.Null, "failure" -> Json.fromString(Option(error.getMessage).getOrElse(error.getClass.getSimpleName))), 2)
    }
    System.out.write(InternalModelFreshProcessProbe.canonicalBytes(report).toArray)
    System.out.flush()
    System.exit(exitcode)
  }

  def observe(root: Path, request: InternalModelContinuationRequest): Observed = {
    val result = ModelEditCrossView.readPromotion(root.resolve("review-materials/approved-candidate"), root, request)
    val reading = result.toOption.getOrElse(throw new IllegalArgumentException(result.show))
    val historical = reading.historical
    require(historical.state.editSessions.size == 1, "historical package must retain one exact edit session")
    val session = historical.state.editSessions.head
    val mapping = historical.state.artifacts.flatMap { entry => entry.bytes.flatMap { bytes =>
      ModelEditCmlProjection.decodeMapping(InternalModelVerifiedProjection(entry.context.reference, entry.context.path,
        entry.context.required, entry.context.dependencies, bytes)).toOption
    } }.filter(_.session == session.reference)
    require(mapping.size == 1, "historical package must retain one exact source mapping")
    ModelEditCmlProjection.validateMapping(mapping.head, session, historical.candidate.get)
      .fold(message => throw new IllegalArgumentException(message), _ => ())
    val current = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
    val baselines = current.artifacts.flatMap(_.bytes).map(_json).filter(value => value.hcursor.get[String]("snapshotKind").toOption.contains("cml-baseline"))
    require(baselines.size == 1, "representative canonical package must retain one applied CML baseline")
    val baseline = baselines.head.hcursor.downField("basis")
    val relative = baseline.get[String]("projectRelativePath").toOption.get
    require(InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(relative), "canonical baseline path must satisfy the existing source-owner rule")
    val bytes = Files.readAllBytes(root.resolve(relative)).toVector
    val expected = Base64.getDecoder.decode(baseline.get[String]("rawBytesBase64").toOption.get).toVector
    val publication = ModelEditSourceProjectionCodec.decodePublication(Files.readString(root.resolve("reproject-applied.json")))
      .fold(message => throw new IllegalArgumentException(message), value => value)
    val postvalidation = io.circe.parser.parse(Files.readString(root.resolve("post-validation.json"))).toOption.get
    val currentresume = InternalModelRehydrationValidator.validate(root)
    Observed(reading, mapping.head, publication, postvalidation, new String(bytes.toArray, StandardCharsets.UTF_8),
      bytes == expected, currentresume.show)
  }

  /** Existing codecs keep typed records complete; this transport is observation, never a package or authority. */
  def reportValue(value: Observed): Json = {
    val historical = value.reading.historical
    val canonical = value.reading.canonical
    Json.obj("historicalEligibility" -> Json.fromString(historical.eligibility.toString),
      "historicalAction" -> historical.action.map(action => Json.fromString(action.token)).getOrElse(Json.Null),
      "session" -> _json(ModelEditSessionCodec.encode(historical.state.editSessions.head)),
      "sourceMapping" -> _json(ModelEditCmlProjection.encodeMapping(value.mapping)),
      "cursor" -> _json(InternalModelResumeCursorCodec.encode(historical.state.cursor)),
      "review" -> historical.review.map(admission => _json(InternalModelCandidateReviewBindingCodec.encode(admission.binding).toVector)).getOrElse(Json.Null),
      "human" -> historical.approval.map(admission => _json(InternalModelCandidateHumanApprovalCodec.encode(admission.record).toVector)).getOrElse(Json.Null),
      "historicalPackageRevision" -> Json.fromLong(historical.state.packageContext.revision),
      "historicalInventory" -> _inventory(historical.state.packageContext),
      "canonicalPackageRevision" -> Json.fromLong(value.reading.canonicalpackage.revision),
      "canonicalInventory" -> _inventory(value.reading.canonicalpackage),
      "canonicalHasResume" -> Json.fromBoolean(value.reading.canonicalhasresume),
      "canonicalResumeDiagnostic" -> Json.fromString(value.resumediagnostic),
      "canonicalRealization" -> _json(InternalModelSemanticRealizationEncoder.encode(canonical.realization).toVector),
      "canonicalBinding" -> _json(InternalModelProjectionBindingCodec.encode(canonical.binding).toVector),
      "actualAppliedSourcePublication" -> io.circe.parser.parse(ModelEditSourceProjectionCodec.encodePublication(value.publication)).toOption.get,
      "actualCanonicalText" -> Json.fromString(value.canonicaltext), "canonicalMatchesBaseline" -> Json.fromBoolean(value.baselinematches),
      "retainedPostValidationObservation" -> value.postvalidation,
      "permission" -> Json.fromString("Historical handoff and current canonical inspection only; no current resume cursor, ApplicationReport or new source authority"))
  }

  private def _inventory(context: InternalModelVerifiedPackageContext): Json = Json.fromValues(context.artifacts.map { entry =>
    Json.obj("reference" -> _json(InternalModelTypedControlCodec.encodeArtifactReference(entry.reference)),
      "path" -> Json.fromString(entry.path), "required" -> Json.fromBoolean(entry.required), "present" -> Json.fromBoolean(entry.present),
      "dependencies" -> Json.fromValues(entry.dependencies.map(reference => _json(InternalModelTypedControlCodec.encodeArtifactReference(reference)))))
  })
  private def _json(bytes: Vector[Byte]): Json = io.circe.parser.parse(new String(bytes.toArray, StandardCharsets.UTF_8)).toOption.get
}
