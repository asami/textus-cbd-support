package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, Printer}

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Builds and renders the CBD-owned CI attestation bound to one canonical report. */
object CarReviewAttestationCodec {
  private val _printer = Printer.noSpaces.copy(sortKeys = true)

  def fromReport(report: CarReviewReport): Either[CarReviewCodecFailure, CarReviewAttestation] =
    _providers(report).map { providers =>
      val id = ReviewAttestationId(s"attestation-${report.reportId.value}")
      val provisional = CarReviewAttestation(
        ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
        ReviewDocumentType("review-attestation"),
        id,
        ReviewDigest(""),
        report.reviewId,
        report.reportId,
        report.reportDigest,
        report.target.digest,
        report.profile,
        providers,
        report.gate,
        report.createdAt
      )
      provisional.copy(attestationDigest = ReviewDigest(_digest(_json(provisional, includeDigest = false))))
    }

  def encode(value: CarReviewAttestation): Either[CarReviewCodecFailure, String] =
    for {
      _ <- _validate(value)
      digest = _digest(_json(value, includeDigest = false))
      _ <- Either.cond(value.attestationDigest.value == digest, (), CarReviewCodecFailure("attestation-digest-invalid", "attestationDigest", "Attestation digest does not bind its canonical content."))
    } yield _printer.print(_json(value, includeDigest = true))

  private def _providers(report: CarReviewReport): Either[CarReviewCodecFailure, Vector[ReviewProviderAttribution]] = {
    val providers = report.execution.providers.map { provider =>
      provider.bundleDigest.map(digest => ReviewProviderAttribution(provider.provider, provider.ruleSet, digest))
    }
    if providers.forall(_.nonEmpty) then Right(providers.flatten.sortBy(value => (value.provider.id.value, value.provider.version.value, value.bundleDigest.value)))
    else Left(CarReviewCodecFailure("attestation-provider-bundle-missing", "execution.providers", "Completed provider execution requires a bundle digest for attestation."))
  }

  private def _validate(value: CarReviewAttestation): Either[CarReviewCodecFailure, Unit] =
    if value.schemaVersion.value != CarReviewVocabulary.SCHEMA_VERSION then Left(CarReviewCodecFailure("attestation-schema-invalid", "schemaVersion", "Unsupported attestation schema version."))
    else if value.documentType.value != "review-attestation" then Left(CarReviewCodecFailure("attestation-document-type-invalid", "documentType", "Unsupported attestation document type."))
    else if !CarReviewVocabulary.GATE_RESULTS.contains(value.gate.result.value) then Left(CarReviewCodecFailure("attestation-gate-invalid", "gate.result", "Unsupported attestation gate result."))
    else if value.providers.isEmpty then Left(CarReviewCodecFailure("attestation-providers-empty", "providers", "Attestation requires at least one provider."))
    else Right(())

  private def _json(value: CarReviewAttestation, includeDigest: Boolean): Json =
    Json.obj((Vector(
      "attestationId" -> Json.fromString(value.attestationId.value),
      "createdAt" -> Json.fromString(value.createdAt.value),
      "documentType" -> Json.fromString(value.documentType.value),
      "gate" -> _gate(value.gate),
      "profile" -> Json.fromString(value.profile.value),
      "providers" -> Json.fromValues(value.providers.map(_provider)),
      "reportDigest" -> Json.fromString(value.reportDigest.value),
      "reportId" -> Json.fromString(value.reportId.value),
      "reviewId" -> Json.fromString(value.reviewId.value),
      "schemaVersion" -> Json.fromString(value.schemaVersion.value),
      "targetDigest" -> Json.fromString(value.targetDigest.value)
    ) ++ (if includeDigest then Vector("attestationDigest" -> Json.fromString(value.attestationDigest.value)) else Vector.empty)): _*)

  private def _provider(value: ReviewProviderAttribution): Json =
    Json.obj(
      "bundleDigest" -> Json.fromString(value.bundleDigest.value),
      "provider" -> Json.obj("id" -> Json.fromString(value.provider.id.value), "version" -> Json.fromString(value.provider.version.value)),
      "ruleSet" -> Json.obj("id" -> Json.fromString(value.ruleSet.id.value), "version" -> Json.fromString(value.ruleSet.version.value))
    )

  private def _gate(value: ReviewGate): Json =
    Json.obj(
      "blockingObservationIds" -> Json.fromValues(value.blockingObservationIds.map(x => Json.fromString(x.value))),
      "policyId" -> Json.fromString(value.policyId),
      "policyVersion" -> Json.fromString(value.policyVersion.value),
      "reasons" -> Json.fromValues(value.reasons.map(Json.fromString)),
      "result" -> Json.fromString(value.result.value)
    )

  private def _digest(value: Json): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(_printer.print(value).getBytes(StandardCharsets.UTF_8)).map(byte => f"${byte & 0xff}%02x").mkString
}
