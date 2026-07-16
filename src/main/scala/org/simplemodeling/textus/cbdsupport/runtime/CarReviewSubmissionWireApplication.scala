package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** JSON adapter for the transport-neutral P5-31 provider-document submission contract. */
final class CarReviewSubmissionWireApplication(
  submissionApplication: CarReviewProviderDocumentSubmissionApplication,
  retainCanonicalResponse: CarReviewCanonicalResponse => Consequence[Unit] = _ => Consequence.unit
) {
  def submit(value: String, actorroles: Set[String]): Consequence[String] =
    for {
      request <- _decode(value)
      response <- submissionApplication.submit(request, actorroles)
      _ <- retainCanonicalResponse(response)
      document <- _encode(response)
    } yield document

  private def _decode(value: String): Consequence[SuppliedProviderBundleSet] =
    CarReviewSubmissionWireCodec.decodeRequest(value) match {
      case Right(request) => Consequence.success(request)
      case Left(error) => Consequence.operationInvalid(error)
    }

  private def _encode(value: CarReviewCanonicalResponse): Consequence[String] =
    CarReviewSubmissionWireCodec.encodeResponse(value) match {
      case Right(document) => Consequence.success(document)
      case Left(error) => Consequence.operationInvalid(error)
    }
}

object CarReviewSubmissionWireCodec {
  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _schema_version = "textus.cbd.review-submission.v1"

  def decodeRequest(value: String): Either[String, SuppliedProviderBundleSet] =
    for {
      root <- parse(value).left.map(_ => "review-submission-json-invalid")
      fields <- _fields(root, Set("schemaVersion", "documentType", "reviewId", "target", "providers"))
      _ <- _exact(fields, "schemaVersion", _schema_version)
      _ <- _exact(fields, "documentType", "provider-document-submission")
      reviewid <- _string(fields, "reviewId").map(ReviewId.apply)
      target <- _target(fields("target"))
      providers <- _array(fields, "providers").flatMap(_providers(_, reviewid, target))
    } yield SuppliedProviderBundleSet(providers)

  def encodeResponse(value: CarReviewCanonicalResponse): Either[String, String] =
    for {
      report <- CarReviewReportCodec.encode(value.report).left.map(_.code)
      json <- parse(report).left.map(_ => "canonical-report-json-invalid")
      attestation <- CarReviewAttestationCodec.encode(value.attestation).left.map(_.code)
      attestationjson <- parse(attestation).left.map(_ => "canonical-attestation-json-invalid")
    } yield _printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(_schema_version),
      "documentType" -> Json.fromString("canonical-review-response"),
      "report" -> json,
      "attestation" -> attestationjson,
      "gateResult" -> Json.fromString(value.gate.result.value)
    ))

  private def _providers(values: Vector[Json], reviewid: ReviewId, target: ReviewTarget): Either[String, Vector[SuppliedProviderBundleSubmission]] =
    if values.isEmpty || values.size > 8 then Left("review-submission-provider-count-invalid")
    else values.foldLeft[Either[String, Vector[SuppliedProviderBundleSubmission]]](Right(Vector.empty)) { (z, value) =>
      for {
        xs <- z
        fields <- _fields(value, Set("availability", "descriptor", "providerRequest", "bundle"))
        availabilityvalue <- _string(fields, "availability")
        availability <- _availability(availabilityvalue)
        descriptor <- _string(fields, "descriptor")
        request <- _string(fields, "providerRequest")
        bundle <- _string(fields, "bundle")
      } yield xs :+ SuppliedProviderBundleSubmission(reviewid, target, availability, descriptor, request, bundle)
    }

  private def _target(value: Json): Either[String, ReviewTarget] =
    for {
      fields <- _fields(value, Set("kind", "organization", "name", "version", "digest"), Set("kind", "name", "digest"))
      kind <- _string(fields, "kind").map(ReviewTargetKind.apply)
      name <- _string(fields, "name")
      digest <- _string(fields, "digest").map(ReviewDigest.apply)
    } yield ReviewTarget(kind, fields.get("organization").flatMap(_.asString), name, fields.get("version").flatMap(_.asString).map(ReviewVersion.apply), digest)

  private def _availability(value: String): Either[String, ProviderBundleAvailability] = value match {
    case "enabled" => Right(ProviderBundleAvailability.Enabled)
    case "unavailable" => Right(ProviderBundleAvailability.Unavailable)
    case "disabled" => Right(ProviderBundleAvailability.Disabled)
    case "failed" => Right(ProviderBundleAvailability.Failed)
    case _ => Left("review-submission-availability-invalid")
  }

  private def _fields(value: Json, allowed: Set[String], required: Set[String] = Set.empty): Either[String, Map[String, Json]] =
    value.asObject.map(_.toMap).filter(fields => fields.keySet.subsetOf(allowed) && required.subsetOf(fields.keySet)).toRight("review-submission-shape-invalid")

  private def _array(fields: Map[String, Json], name: String): Either[String, Vector[Json]] =
    fields.get(name).flatMap(_.asArray).map(_.toVector).toRight("review-submission-providers-invalid")

  private def _string(fields: Map[String, Json], name: String): Either[String, String] =
    fields.get(name).flatMap(_.asString).filter(_.nonEmpty).toRight(s"review-submission-$name-invalid")

  private def _exact(fields: Map[String, Json], name: String, expected: String): Either[String, Unit] =
    Either.cond(fields.get(name).flatMap(_.asString).contains(expected), (), s"review-submission-$name-invalid")
}
