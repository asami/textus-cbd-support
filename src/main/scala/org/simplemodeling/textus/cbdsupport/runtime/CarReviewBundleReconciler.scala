package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.parser.parse

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class AdmittedProviderBundleInput(
  admitted: AdmittedProviderBundle,
  bundle: String
)

final case class CarReviewReconciliationConflict(
  ruleId: ReviewRuleId,
  subject: ReviewSubject,
  observationIds: Vector[ReviewObservationId]
)

final case class CarReviewReconciliationResult(
  evidence: Vector[ReviewEvidence],
  observations: Vector[ReviewObservation],
  limitations: Vector[ReviewLimitation],
  conflicts: Vector[CarReviewReconciliationConflict]
)

final case class CarReviewReconciliationFailure(
  code: String,
  message: String
)

/**
 * Turns already-admitted provider bundle content into canonical report records.
 * It does not execute a provider, choose a competing result, or promote an
 * unsupported provider assurance.
 */
object CarReviewBundleReconciler {
  def reconcile(
    inputs: Vector[AdmittedProviderBundleInput]
  ): Either[CarReviewReconciliationFailure, CarReviewReconciliationResult] = {
    val unique = inputs.foldLeft(Vector.empty[AdmittedProviderBundleInput]) { (z, input) =>
      if z.exists(_.admitted.bundleDigest == input.admitted.bundleDigest) then z else z :+ input
    }
    for {
      reconciled <- unique.foldLeft[Either[CarReviewReconciliationFailure, Vector[CarReviewReconciliationResult]]](Right(Vector.empty)) { (z, input) =>
        for { xs <- z; value <- _reconcile_bundle(input) } yield xs :+ value
      }
      evidence = reconciled.flatMap(_.evidence).sortBy(_.id.value)
      observations = reconciled.flatMap(_.observations).sortBy(_.id.value)
      limitations = reconciled.flatMap(_.limitations).sortBy(value => (value.code, value.subjectId.getOrElse("")))
      conflicts = _conflicts(observations)
    } yield CarReviewReconciliationResult(evidence, observations, limitations, conflicts)
  }

  private def _reconcile_bundle(
    input: AdmittedProviderBundleInput
  ): Either[CarReviewReconciliationFailure, CarReviewReconciliationResult] =
    for {
      bundle <- parse(input.bundle).left.map(_ => _failure("bundle-json-invalid", "Admitted provider bundle JSON is invalid."))
      provider <- _provider(bundle)
      _ <- Either.cond(provider == input.admitted.provider, (), _failure("provider-identity-mismatch", "Bundle provider differs from the admitted provider."))
      digest <- _string(bundle, "bundleDigest")
      _ <- Either.cond(digest == input.admitted.bundleDigest.value, (), _failure("bundle-digest-mismatch", "Bundle digest differs from the admitted bundle."))
      evidence <- _evidence(bundle, input.admitted)
      observations <- _observations(bundle, input.admitted, evidence)
      limitations <- _limitations(bundle)
    } yield CarReviewReconciliationResult(evidence, observations._1, observations._2 ++ limitations, Vector.empty)

  private def _evidence(
    bundle: Json,
    admitted: AdmittedProviderBundle
  ): Either[CarReviewReconciliationFailure, Vector[ReviewEvidence]] =
    _array(bundle, "evidence").flatMap { values =>
      values.foldLeft[Either[CarReviewReconciliationFailure, Vector[ReviewEvidence]]](Right(Vector.empty)) { (z, value) =>
        for {
          xs <- z
          id <- _string(value, "id")
          kind <- _string(value, "kind")
          subject <- _subject(value)
          originprovider <- value.hcursor.downField("origin").get[String]("providerId").toOption.toRight(_failure("evidence-origin-invalid", "Evidence origin provider is missing."))
          _ <- Either.cond(originprovider == admitted.provider.id.value, (), _failure("evidence-origin-mismatch", "Evidence origin differs from the admitted provider."))
          facts <- value.hcursor.downField("facts").focus.flatMap(_.asObject).toRight(_failure("evidence-facts-invalid", "Evidence facts must be an object."))
          location <- _location(value)
        } yield xs :+ ReviewEvidence(
          ReviewEvidenceId(_canonical_id(admitted.provider, id)),
          kind,
          subject,
          admitted.provider.id,
          admitted.bundleDigest,
          id,
          location,
          None,
          facts
        )
      }
    }

  private def _observations(
    bundle: Json,
    admitted: AdmittedProviderBundle,
    evidence: Vector[ReviewEvidence]
  ): Either[CarReviewReconciliationFailure, (Vector[ReviewObservation], Vector[ReviewLimitation])] =
    _array(bundle, "observations").flatMap { values =>
      values.foldLeft[Either[CarReviewReconciliationFailure, (Vector[ReviewObservation], Vector[ReviewLimitation])]](Right(Vector.empty -> Vector.empty)) { (z, value) =>
        for {
          xs <- z
          localid <- _string(value, "id")
          observationtype <- _string(value, "type")
          ruleid <- _string(value, "ruleId")
          subject <- _subject(value)
          message <- _string(value, "message")
          confidence <- _string(value, "confidence")
          evidenceids <- _string_array(value, "evidenceIds")
          _ <- Either.cond(evidenceids.forall(id => evidence.exists(_.providerEvidenceId == id)), (), _failure("observation-evidence-mismatch", "Observation references Evidence outside its admitted bundle."))
          severity = value.hcursor.get[String]("severity").toOption.map(ReviewSeverity.apply)
          canonicaltype = if observationtype == "assurance" && evidenceids.isEmpty then "unknown" else observationtype
          _ <- Either.cond(CarReviewVocabulary.OBSERVATION_TYPES.contains(canonicaltype), (), _failure("observation-type-invalid", "Observation type is not supported."))
          _ <- Either.cond(canonicaltype != "finding" || severity.nonEmpty, (), _failure("finding-severity-missing", "A Finding requires severity."))
          _ <- Either.cond(canonicaltype == "finding" || severity.isEmpty, (), _failure("observation-severity-invalid", "Only a Finding may carry severity."))
          limitation = Option.when(observationtype == "assurance" && evidenceids.isEmpty)(ReviewLimitation(
            "provider-assurance-without-evidence",
            ReviewLimitationScope("observation"),
            Some(_canonical_id(admitted.provider, localid)),
            "Provider assurance has no admitted Evidence and remains Unknown.",
            false
          ))
          observation = ReviewObservation(
            ReviewObservationId(_canonical_id(admitted.provider, localid)),
            ReviewObservationType(canonicaltype),
            ReviewRuleIdentity(ReviewRuleId(ruleid), admitted.ruleSet.version),
            subject,
            message,
            s"Provider ${admitted.provider.id.value} reported this observation through $ruleid.",
            severity,
            ReviewConfidence(confidence),
            evidenceids.map(id => ReviewEvidenceId(_canonical_id(admitted.provider, id))),
            Vector.empty,
            ReviewProviderAttribution(admitted.provider, admitted.ruleSet, admitted.bundleDigest),
            ReviewDisposition(ReviewDispositionState("active"), None, None, None),
            ReviewMappings(Vector.empty, Vector.empty, Vector.empty)
          )
        } yield (xs._1 :+ observation, xs._2 ++ limitation.toVector)
      }
    }

  private def _limitations(bundle: Json): Either[CarReviewReconciliationFailure, Vector[ReviewLimitation]] =
    _array(bundle, "limitations").flatMap { values =>
      values.foldLeft[Either[CarReviewReconciliationFailure, Vector[ReviewLimitation]]](Right(Vector.empty)) { (z, value) =>
        for {
          xs <- z
          code <- _string(value, "code")
          scope <- _string(value, "scope")
          message <- _string(value, "message")
          retryable <- value.hcursor.get[Boolean]("retryable").toOption.toRight(_failure("limitation-retryable-invalid", "Limitation retryability is missing."))
          reportscope = if CarReviewVocabulary.LIMITATION_SCOPES.contains(scope) then scope else "provider"
        } yield xs :+ ReviewLimitation(code, ReviewLimitationScope(reportscope), value.hcursor.get[String]("subjectId").toOption, message, retryable)
      }
    }

  private def _provider(bundle: Json): Either[CarReviewReconciliationFailure, ReviewProviderIdentity] =
    for {
      id <- bundle.hcursor.downField("provider").get[String]("id").toOption.toRight(_failure("provider-id-invalid", "Bundle provider ID is missing."))
      version <- bundle.hcursor.downField("provider").get[String]("version").toOption.toRight(_failure("provider-version-invalid", "Bundle provider version is missing."))
    } yield ReviewProviderIdentity(ReviewProviderId(id), ReviewVersion(version))

  private def _subject(json: Json): Either[CarReviewReconciliationFailure, ReviewSubject] =
    for {
      kind <- json.hcursor.downField("subject").get[String]("kind").toOption.toRight(_failure("subject-kind-invalid", "Subject kind is missing."))
      id <- json.hcursor.downField("subject").get[String]("id").toOption.toRight(_failure("subject-id-invalid", "Subject ID is missing."))
    } yield ReviewSubject(kind, id)

  private def _location(json: Json): Either[CarReviewReconciliationFailure, Option[ReviewLocation]] =
    json.hcursor.downField("location").focus match {
      case None => Right(None)
      case Some(value) => Right(Some(ReviewLocation(
        value.hcursor.get[String]("uri").toOption,
        value.hcursor.get[String]("path").toOption,
        value.hcursor.get[Int]("line").toOption,
        value.hcursor.get[Int]("column").toOption,
        None,
        None
      )))
    }

  private def _array(json: Json, name: String): Either[CarReviewReconciliationFailure, Vector[Json]] =
    json.hcursor.downField(name).focus.flatMap(_.asArray).map(_.toVector).toRight(_failure(s"$name-invalid", s"Bundle $name array is missing."))

  private def _string(json: Json, name: String): Either[CarReviewReconciliationFailure, String] =
    json.hcursor.get[String](name).toOption.toRight(_failure(s"$name-invalid", s"Bundle $name is missing."))

  private def _string_array(json: Json, name: String): Either[CarReviewReconciliationFailure, Vector[String]] =
    json.hcursor.get[Vector[String]](name).toOption.toRight(_failure(s"$name-invalid", s"Bundle $name array is missing."))

  private def _canonical_id(provider: ReviewProviderIdentity, localid: String): String =
    s"${provider.id.value}:$localid"

  private def _conflicts(observations: Vector[ReviewObservation]): Vector[CarReviewReconciliationConflict] =
    observations.groupBy(value => (value.rule.id, value.subject)).toVector.flatMap { case ((ruleid, subject), values) =>
      Option.when(values.size > 1)(CarReviewReconciliationConflict(ruleid, subject, values.map(_.id).sortBy(_.value)))
    }.sortBy(value => (value.ruleId.value, value.subject.kind, value.subject.id))

  private def _failure(code: String, message: String): CarReviewReconciliationFailure =
    CarReviewReconciliationFailure(code, message)
}
