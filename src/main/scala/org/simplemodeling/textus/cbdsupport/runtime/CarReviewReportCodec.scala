package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant

import io.circe.{Decoder, Encoder, Json, JsonObject, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object CarReviewReportCodec {
  private val _identifier_pattern = "^[A-Za-z0-9][A-Za-z0-9._:/-]*$".r
  private val _digest_pattern = "^sha256:[0-9a-f]{64}$".r
  private val _printer = Printer.noSpaces.copy(dropNullValues = true, sortKeys = true)

  private given Encoder[ReviewId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewId] = Decoder.decodeString.map(ReviewId.apply)
  private given Encoder[ReviewReportId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewReportId] = Decoder.decodeString.map(ReviewReportId.apply)
  private given Encoder[ReviewEvidenceId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewEvidenceId] = Decoder.decodeString.map(ReviewEvidenceId.apply)
  private given Encoder[ReviewObservationId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewObservationId] = Decoder.decodeString.map(ReviewObservationId.apply)
  private given Encoder[ReviewCapabilityId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewCapabilityId] = Decoder.decodeString.map(ReviewCapabilityId.apply)
  private given Encoder[ReviewProviderId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewProviderId] = Decoder.decodeString.map(ReviewProviderId.apply)
  private given Encoder[ReviewRuleId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewRuleId] = Decoder.decodeString.map(ReviewRuleId.apply)
  private given Encoder[ReviewDigest] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewDigest] = Decoder.decodeString.map(ReviewDigest.apply)
  private given Encoder[ReviewVersion] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewVersion] = Decoder.decodeString.map(ReviewVersion.apply)
  private given Encoder[ReviewProfile] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewProfile] = Decoder.decodeString.map(ReviewProfile.apply)
  private given Encoder[ReviewInstant] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewInstant] = Decoder.decodeString.map(ReviewInstant.apply)
  private given Encoder[ReviewSchemaVersion] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewSchemaVersion] = Decoder.decodeString.map(ReviewSchemaVersion.apply)
  private given Encoder[ReviewDocumentType] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewDocumentType] = Decoder.decodeString.map(ReviewDocumentType.apply)
  private given Encoder[ReviewTargetKind] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewTargetKind] = Decoder.decodeString.map(ReviewTargetKind.apply)
  private given Encoder[ReviewProviderState] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewProviderState] = Decoder.decodeString.map(ReviewProviderState.apply)
  private given Encoder[ReviewLimitationScope] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewLimitationScope] = Decoder.decodeString.map(ReviewLimitationScope.apply)
  private given Encoder[ReviewObservationType] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewObservationType] = Decoder.decodeString.map(ReviewObservationType.apply)
  private given Encoder[ReviewSeverity] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewSeverity] = Decoder.decodeString.map(ReviewSeverity.apply)
  private given Encoder[ReviewConfidence] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewConfidence] = Decoder.decodeString.map(ReviewConfidence.apply)
  private given Encoder[ReviewDispositionState] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewDispositionState] = Decoder.decodeString.map(ReviewDispositionState.apply)
  private given Encoder[ReviewApplicability] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewApplicability] = Decoder.decodeString.map(ReviewApplicability.apply)
  private given Encoder[ReviewMaturity] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewMaturity] = Decoder.decodeString.map(ReviewMaturity.apply)
  private given Encoder[ReviewGateResult] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewGateResult] = Decoder.decodeString.map(ReviewGateResult.apply)

  private given Encoder[ReviewTarget] = deriveEncoder
  private given Decoder[ReviewTarget] = deriveDecoder
  private given Encoder[ReviewProviderIdentity] = deriveEncoder
  private given Decoder[ReviewProviderIdentity] = deriveDecoder
  private given Encoder[ReviewRuleIdentity] = deriveEncoder
  private given Decoder[ReviewRuleIdentity] = deriveDecoder
  private given Encoder[ReviewLimitation] = deriveEncoder
  private given Decoder[ReviewLimitation] = deriveDecoder
  private given Encoder[ReviewProviderExecution] = deriveEncoder
  private given Decoder[ReviewProviderExecution] = deriveDecoder
  private given Encoder[ReviewSubject] = deriveEncoder
  private given Decoder[ReviewSubject] = deriveDecoder
  private given Encoder[ReviewLocation] = deriveEncoder
  private given Decoder[ReviewLocation] = deriveDecoder
  private given Encoder[ReviewEvidence] = deriveEncoder
  private given Decoder[ReviewEvidence] = deriveDecoder
  private given Encoder[ReviewProviderAttribution] = deriveEncoder
  private given Decoder[ReviewProviderAttribution] = deriveDecoder
  private given Encoder[ReviewDisposition] = deriveEncoder
  private given Decoder[ReviewDisposition] = deriveDecoder
  private given Encoder[ReviewMappings] = deriveEncoder
  private given Decoder[ReviewMappings] = deriveDecoder
  private given Encoder[ReviewObservation] = deriveEncoder
  private given Decoder[ReviewObservation] = deriveDecoder
  private given Encoder[ReviewCoverage] = deriveEncoder
  private given Decoder[ReviewCoverage] = deriveDecoder
  private given Encoder[ReviewAssessment] = deriveEncoder
  private given Decoder[ReviewAssessment] = deriveDecoder
  private given Encoder[ReviewBaseline] = deriveEncoder
  private given Decoder[ReviewBaseline] = deriveDecoder
  private given Encoder[ReviewGate] = deriveEncoder
  private given Decoder[ReviewGate] = deriveDecoder
  private given Encoder[ReviewExecution] = deriveEncoder
  private given Decoder[ReviewExecution] = deriveDecoder
  private given Encoder[CarReviewReport] = deriveEncoder
  private given Decoder[CarReviewReport] = deriveDecoder

  def decode(value: String): Either[CarReviewCodecFailure, CarReviewReport] =
    for {
      json <- parse(value).left.map(_ => _failure("invalid-json", "$", "Review Report JSON is invalid."))
      _ <- _validate_wire_shape(json)
      report <- summon[Decoder[CarReviewReport]].decodeJson(json).left.map(
        _ => _failure("invalid-document", "$", "Review Report fields do not match the v1 contract.")
      )
      _ <- _validate_model(report)
      _ <- _verify_digest(report)
    } yield report

  def encode(report: CarReviewReport): Either[CarReviewCodecFailure, String] =
    for {
      _ <- _validate_model(report)
      _ <- _verify_digest(report)
    } yield _printer.print(_canonicalize_arrays(summon[Encoder[CarReviewReport]].apply(report)))

  def calculateDigest(report: CarReviewReport): Either[CarReviewCodecFailure, ReviewDigest] =
    _validate_model(report).map { _ =>
      val json = summon[Encoder[CarReviewReport]].apply(report)
      ReviewDigest(_sha256(_canonical_report_content(json)))
    }

  def withCalculatedDigest(report: CarReviewReport): Either[CarReviewCodecFailure, CarReviewReport] =
    calculateDigest(report).map(digest => report.copy(reportDigest = digest))

  private def _verify_digest(report: CarReviewReport): Either[CarReviewCodecFailure, Unit] =
    if !_valid_digest(report.reportDigest.value) then
      Left(_failure("invalid-digest", "$.reportDigest", "Report digest must be a lowercase SHA-256 value."))
    else
      calculateDigest(report).flatMap { calculated =>
        Either.cond(
          calculated == report.reportDigest,
          (),
          _failure("digest-mismatch", "$.reportDigest", "Report digest does not match deterministic content.")
        )
      }

  private def _validate_model(report: CarReviewReport): Either[CarReviewCodecFailure, Unit] = {
    val evidenceids = report.evidence.map(_.id)
    val observationids = report.observations.map(_.id)
    val capabilityids = report.assessments.map(_.capabilityId)
    val evidenceset = evidenceids.toSet
    val observationset = observationids.toSet
    val findingset = report.observations.filter(_.`type`.value == "finding").map(_.id).toSet
    val providerbindings = report.execution.providers.map(_provider_binding)
    val providerbindingset = providerbindings.toSet
    val provideridentities = providerbindings.map(binding => (
      binding.providerid,
      binding.providerversion,
      binding.rulesetid,
      binding.rulesetversion
    ))
    val providerids = providerbindings.map(_.providerid).toSet

    val failures = Vector(
      _failure_unless(
        report.schemaVersion.value == CarReviewVocabulary.SCHEMA_VERSION,
        "incompatible-schema",
        "$.schemaVersion",
        "Review Report schema version is not supported."
      ),
      _failure_unless(
        report.documentType.value == CarReviewVocabulary.DOCUMENT_TYPE,
        "incompatible-document",
        "$.documentType",
        "Document type is not review-report."
      ),
      _identifier_failure(report.reportId.value, "$.reportId"),
      _identifier_failure(report.reviewId.value, "$.reviewId"),
      _instant_failure(report.createdAt.value, "$.createdAt"),
      _identifier_failure(report.profile.value, "$.profile"),
      _failure_unless(
        CarReviewVocabulary.TARGET_KINDS.contains(report.target.kind.value),
        "invalid-target",
        "$.target.kind",
        "Target kind is not supported."
      ),
      _identifier_failure(report.target.name, "$.target.name"),
      report.target.organization.flatMap(_identifier_failure(_, "$.target.organization")),
      report.target.version.flatMap(value => _version_failure(value.value, "$.target.version")),
      _digest_failure(report.target.digest.value, "$.target.digest"),
      _instant_failure(report.execution.startedAt.value, "$.execution.startedAt"),
      _instant_failure(report.execution.completedAt.value, "$.execution.completedAt"),
      _instant_order_failure(
        report.execution.startedAt,
        report.execution.completedAt,
        "$.execution",
        "Review execution completion cannot precede its start."
      ),
      _instant_order_failure(
        report.execution.completedAt,
        report.createdAt,
        "$",
        "Report creation cannot precede Review execution completion."
      ),
      _failure_unless(
        evidenceids.distinct == evidenceids,
        "duplicate-identity",
        "$.evidence",
        "Evidence IDs must be unique."
      ),
      _failure_unless(
        observationids.distinct == observationids,
        "duplicate-identity",
        "$.observations",
        "Observation IDs must be unique."
      ),
      _failure_unless(
        capabilityids.distinct == capabilityids,
        "duplicate-identity",
        "$.assessments",
        "Capability assessment IDs must be unique."
      ),
      _failure_unless(
        provideridentities.distinct == provideridentities,
        "duplicate-provider",
        "$.execution.providers",
        "Provider execution identities must be unique."
      ),
      _failure_unless(
        report.observations.flatMap(_.evidenceIds).forall(evidenceset.contains),
        "unresolved-reference",
        "$.observations[*].evidenceIds",
        "Observation Evidence references must resolve inside the report."
      ),
      _failure_unless(
        report.assessments.flatMap(_.evidenceIds).forall(evidenceset.contains),
        "unresolved-reference",
        "$.assessments[*].evidenceIds",
        "Assessment Evidence references must resolve inside the report."
      ),
      _failure_unless(
        report.assessments.flatMap(_.observationIds).forall(observationset.contains),
        "unresolved-reference",
        "$.assessments[*].observationIds",
        "Assessment Observation references must resolve inside the report."
      ),
      _failure_unless(
        report.gate.blockingObservationIds.forall(findingset.contains),
        "invalid-gate",
        "$.gate.blockingObservationIds",
        "Gate blockers must resolve to Findings in the report."
      ),
      _failure_unless(
        report.gate.blockingObservationIds.distinct == report.gate.blockingObservationIds,
        "duplicate-reference",
        "$.gate.blockingObservationIds",
        "Gate blocking Observation IDs must be unique."
      ),
      _identifier_failure(report.gate.policyId, "$.gate.policyId"),
      _version_failure(report.gate.policyVersion.value, "$.gate.policyVersion"),
      _failure_unless(
        report.gate.reasons.forall(reason => reason.nonEmpty && reason.length <= 1024),
        "invalid-gate",
        "$.gate.reasons",
        "Gate reasons must be present and bounded."
      ),
      _failure_unless(
        report.evidence.forall { evidence =>
          providerbindingset.exists(binding =>
            binding.providerid == evidence.providerId && binding.bundledigest.contains(evidence.bundleDigest)
          )
        },
        "provider-mismatch",
        "$.evidence",
        "Evidence provider and bundle must resolve to one execution."
      ),
      _failure_unless(
        report.observations.forall(observation => providerbindingset.contains(_provider_binding(observation.provider))),
        "provider-mismatch",
        "$.observations[*].provider",
        "Observation provider attribution must resolve to one execution."
      ),
      _failure_unless(
        report.assessments.flatMap(_.providerIds).forall(providerids.contains),
        "provider-mismatch",
        "$.assessments[*].providerIds",
        "Assessment providers must resolve to report executions."
      )
    ) ++
      report.execution.providers.zipWithIndex.flatMap { case (provider, index) =>
        _provider_failures(provider, s"$$.execution.providers[$index]")
      } ++
      report.evidence.zipWithIndex.flatMap { case (evidence, index) =>
        _evidence_failures(evidence, s"$$.evidence[$index]")
      } ++
      report.observations.zipWithIndex.flatMap { case (observation, index) =>
        _observation_failures(observation, s"$$.observations[$index]")
      } ++
      report.assessments.zipWithIndex.flatMap { case (assessment, index) =>
        _assessment_failures(assessment, s"$$.assessments[$index]")
      } ++
      report.assessments.zipWithIndex.map { case (assessment, index) =>
        _failure_unless(
          assessment.maturity.value != "operational" ||
            CarReviewRuntimeEvidencePolicy.supportsOperational(assessment, report.evidence, report.observations),
          "runtime-evidence-required",
          s"$$.assessments[$index]",
          "Operational maturity requires admitted runtime-observation Evidence and an attributable mapped Observation."
        )
      } ++
      report.limitations.zipWithIndex.flatMap { case (limitation, index) =>
        _limitation_failures(limitation, s"$$.limitations[$index]")
      } ++
      _baseline_failures(report.baseline, observationset) ++
      Vector(
        _failure_unless(
          CarReviewVocabulary.GATE_RESULTS.contains(report.gate.result.value),
          "invalid-gate",
          "$.gate.result",
          "Gate result is not supported."
        )
      )

    failures.collectFirst { case Some(failure) => failure }.toLeft(())
  }

  private def _provider_failures(
    provider: ReviewProviderExecution,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] =
    Vector(
      _identifier_failure(provider.provider.id.value, s"$path.provider.id"),
      _version_failure(provider.provider.version.value, s"$path.provider.version"),
      _identifier_failure(provider.ruleSet.id.value, s"$path.ruleSet.id"),
      _version_failure(provider.ruleSet.version.value, s"$path.ruleSet.version"),
      _failure_unless(
        CarReviewVocabulary.PROVIDER_STATES.contains(provider.state.value),
        "invalid-provider-state",
        s"$path.state",
        "Provider execution state is not supported."
      ),
      _failure_unless(
        provider.state.value != "completed" ||
          (provider.bundleDigest.nonEmpty && provider.startedAt.nonEmpty && provider.completedAt.nonEmpty),
        "incomplete-provider",
        path,
        "Completed provider execution requires bundle digest and start/completion times."
      ),
      provider.bundleDigest.flatMap(digest => _digest_failure(digest.value, s"$path.bundleDigest")),
      provider.startedAt.flatMap(value => _instant_failure(value.value, s"$path.startedAt")),
      provider.completedAt.flatMap(value => _instant_failure(value.value, s"$path.completedAt")),
      (provider.startedAt, provider.completedAt) match {
        case (Some(start), Some(end)) =>
          _instant_order_failure(start, end, path, "Provider completion cannot precede its start.")
        case _ => None
      }
    ) ++ provider.limitations.zipWithIndex.flatMap { case (limitation, index) =>
      _limitation_failures(limitation, s"$path.limitations[$index]")
    }

  private def _evidence_failures(
    evidence: ReviewEvidence,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] =
    Vector(
      _identifier_failure(evidence.id.value, s"$path.id"),
      _identifier_failure(evidence.kind, s"$path.kind"),
      _identifier_failure(evidence.subject.kind, s"$path.subject.kind"),
      _identifier_failure(evidence.subject.id, s"$path.subject.id"),
      _identifier_failure(evidence.providerId.value, s"$path.providerId"),
      _digest_failure(evidence.bundleDigest.value, s"$path.bundleDigest"),
      _identifier_failure(evidence.providerEvidenceId, s"$path.providerEvidenceId"),
      evidence.digest.flatMap(value => _digest_failure(value.value, s"$path.digest")),
      _failure_unless(
        evidence.facts.size <= 256,
        "facts-bound-exceeded",
        s"$path.facts",
        "Evidence facts exceed the maximum property count."
      ),
      _failure_unless(
        evidence.kind.nonEmpty && evidence.kind.length <= 180,
        "invalid-evidence",
        s"$path.kind",
        "Evidence kind must be present and bounded."
      )
    ) ++ evidence.location.toVector.flatMap(_location_failures(_, s"$path.location"))

  private def _observation_failures(
    observation: ReviewObservation,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] = {
    val finding = observation.`type`.value == "finding"
    val nonactive = observation.disposition.state.value != "active"
    Vector(
      _identifier_failure(observation.id.value, s"$path.id"),
      _identifier_failure(observation.rule.id.value, s"$path.rule.id"),
      _version_failure(observation.rule.version.value, s"$path.rule.version"),
      _identifier_failure(observation.subject.kind, s"$path.subject.kind"),
      _identifier_failure(observation.subject.id, s"$path.subject.id"),
      _failure_unless(
        CarReviewVocabulary.OBSERVATION_TYPES.contains(observation.`type`.value),
        "invalid-observation",
        s"$path.type",
        "Observation type is not supported."
      ),
      _failure_unless(
        finding == observation.severity.nonEmpty,
        "invalid-severity",
        s"$path.severity",
        "Severity is required only for Findings."
      ),
      observation.severity.flatMap { severity =>
        _failure_unless(
          CarReviewVocabulary.SEVERITIES.contains(severity.value),
          "invalid-severity",
          s"$path.severity",
          "Finding severity is not supported."
        )
      },
      _failure_unless(
        CarReviewVocabulary.CONFIDENCES.contains(observation.confidence.value),
        "invalid-confidence",
        s"$path.confidence",
        "Observation confidence is not supported."
      ),
      _failure_unless(
        observation.`type`.value != "assurance" || observation.evidenceIds.nonEmpty,
        "unsupported-assurance",
        s"$path.evidenceIds",
        "Assurance requires admitted Evidence."
      ),
      _failure_unless(
        observation.evidenceIds.distinct == observation.evidenceIds,
        "duplicate-reference",
        s"$path.evidenceIds",
        "Observation Evidence IDs must be unique."
      ),
      _failure_unless(
        observation.message.nonEmpty && observation.message.length <= 2048 &&
          observation.rationale.nonEmpty && observation.rationale.length <= 4096,
        "invalid-observation",
        path,
        "Observation message and rationale must be present and bounded."
      ),
      _failure_unless(
        CarReviewVocabulary.DISPOSITIONS.contains(observation.disposition.state.value),
        "invalid-disposition",
        s"$path.disposition.state",
        "Disposition state is not supported."
      ),
      _failure_unless(
        !nonactive || (observation.disposition.reason.exists(_.nonEmpty) && observation.disposition.author.nonEmpty),
        "invalid-disposition",
        s"$path.disposition",
        "Non-active disposition requires reason and author."
      ),
      observation.disposition.reason.flatMap { reason =>
        _failure_unless(
          reason.length <= 1024,
          "invalid-disposition",
          s"$path.disposition.reason",
          "Disposition reason exceeds its bound."
        )
      },
      observation.disposition.author.flatMap(_identifier_failure(_, s"$path.disposition.author")),
      _failure_unless(
        observation.mappings.cncfFeatures.distinct == observation.mappings.cncfFeatures &&
          observation.mappings.implementationSubjects.distinct == observation.mappings.implementationSubjects &&
          observation.mappings.qualityCapabilities.distinct == observation.mappings.qualityCapabilities,
        "duplicate-reference",
        s"$path.mappings",
        "Observation mapping identities must be unique."
      ),
      observation.disposition.expiresAt.flatMap(value => _instant_failure(value.value, s"$path.disposition.expiresAt"))
    ) ++
      observation.mappings.cncfFeatures.zipWithIndex.map { case (value, index) =>
        _identifier_failure(value, s"$path.mappings.cncfFeatures[$index]")
      } ++
      observation.mappings.implementationSubjects.zipWithIndex.map { case (value, index) =>
        _identifier_failure(value, s"$path.mappings.implementationSubjects[$index]")
      } ++
      observation.mappings.qualityCapabilities.zipWithIndex.map { case (value, index) =>
        _identifier_failure(value.value, s"$path.mappings.qualityCapabilities[$index]")
      } ++
      observation.locations.zipWithIndex.flatMap { case (location, index) =>
        _location_failures(location, s"$path.locations[$index]")
      }
  }

  private def _assessment_failures(
    assessment: ReviewAssessment,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] = {
    val applicable = assessment.applicability.value == "applicable"
    Vector(
      _identifier_failure(assessment.capabilityId.value, s"$path.capabilityId"),
      _failure_unless(
        CarReviewVocabulary.APPLICABILITIES.contains(assessment.applicability.value),
        "invalid-applicability",
        s"$path.applicability",
        "Assessment applicability is not supported."
      ),
      _failure_unless(
        CarReviewVocabulary.MATURITIES.contains(assessment.maturity.value),
        "invalid-maturity",
        s"$path.maturity",
        "Assessment maturity is not supported."
      ),
      _failure_unless(
        CarReviewVocabulary.CONFIDENCES.contains(assessment.confidence.value),
        "invalid-confidence",
        s"$path.confidence",
        "Assessment confidence is not supported."
      ),
      _failure_unless(
        applicable == assessment.coverage.nonEmpty,
        "invalid-coverage",
        s"$path.coverage",
        "Coverage exists exactly when the capability is applicable."
      ),
      _failure_unless(
        assessment.providerIds.distinct == assessment.providerIds &&
          assessment.observationIds.distinct == assessment.observationIds &&
          assessment.evidenceIds.distinct == assessment.evidenceIds,
        "duplicate-reference",
        path,
        "Assessment provider, Observation, and Evidence references must be unique."
      ),
      _failure_unless(
        assessment.strengths.forall(_.length <= 1024) && assessment.gaps.forall(_.length <= 1024),
        "invalid-assessment",
        path,
        "Assessment strengths and gaps must be bounded."
      ),
      assessment.coverage.flatMap { coverage =>
        _failure_unless(
          coverage.applicableSubjects > 0 && coverage.assessedSubjects >= 0 && coverage.unknownSubjects >= 0 &&
            coverage.assessedSubjects + coverage.unknownSubjects == coverage.applicableSubjects &&
            coverage.basisPoints == coverage.assessedSubjects * 10000 / coverage.applicableSubjects,
          "invalid-coverage",
          s"$path.coverage",
          "Coverage counts and basis points are inconsistent."
        )
      }
    )
  }

  private def _baseline_failures(
    baseline: Option[ReviewBaseline],
    observations: Set[ReviewObservationId]
  ): Vector[Option[CarReviewCodecFailure]] =
    baseline.toVector.flatMap { value =>
      val added = value.addedObservationIds.toSet
      val removed = value.removedObservationIds.toSet
      val unchanged = value.unchangedObservationIds.toSet
      Vector(
        _identifier_failure(value.reportId.value, "$.baseline.reportId"),
        _digest_failure(value.digest.value, "$.baseline.digest"),
        _failure_unless(
          added.subsetOf(observations) && unchanged.subsetOf(observations),
          "invalid-baseline",
          "$.baseline",
          "Added and unchanged baseline IDs must resolve inside the current report."
        ),
        _failure_unless(
          value.addedObservationIds.distinct == value.addedObservationIds &&
            value.removedObservationIds.distinct == value.removedObservationIds &&
            value.unchangedObservationIds.distinct == value.unchangedObservationIds &&
            added.intersect(removed).isEmpty && added.intersect(unchanged).isEmpty &&
            removed.intersect(unchanged).isEmpty,
          "invalid-baseline",
          "$.baseline",
          "Baseline observation sets must be unique and disjoint."
        )
      ) ++ value.removedObservationIds.zipWithIndex.map { case (observationid, index) =>
        _identifier_failure(observationid.value, s"$$.baseline.removedObservationIds[$index]")
      }
    }

  private def _limitation_failures(
    limitation: ReviewLimitation,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] =
    Vector(
      _identifier_failure(limitation.code, s"$path.code"),
      limitation.subjectId.flatMap(_identifier_failure(_, s"$path.subjectId")),
      _failure_unless(
        CarReviewVocabulary.LIMITATION_SCOPES.contains(limitation.scope.value),
        "invalid-limitation",
        s"$path.scope",
        "Limitation scope is not supported."
      ),
      _failure_unless(
        limitation.message.nonEmpty && limitation.message.length <= 1024,
        "invalid-limitation",
        s"$path.message",
        "Limitation message must be present and bounded."
      )
    )

  private def _location_failures(
    location: ReviewLocation,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] =
    Vector(
      _failure_unless(
        location.uri.nonEmpty || location.path.nonEmpty,
        "invalid-location",
        path,
        "Location requires an admitted URI or relative path."
      ),
      _failure_unless(
        Vector(location.line, location.column, location.endLine, location.endColumn).flatten.forall(_ > 0),
        "invalid-location",
        path,
        "Location coordinates must be positive."
      ),
      _failure_unless(
        location.uri.forall(_.length <= 2048) && location.path.forall(_.length <= 2048),
        "invalid-location",
        path,
        "Location URI and path must be bounded."
      ),
      location.path.flatMap { value =>
        _failure_unless(
          _safe_relative_path(value),
          "unsafe-location",
          s"$path.path",
          "Location path must be a normalized relative target path."
        )
      },
      location.uri.flatMap { value =>
        _failure_unless(
          _safe_evidence_uri(value),
          "unsafe-location",
          s"$path.uri",
          "Location URI must be an admitted credential-free HTTP(S) evidence URI."
        )
      }
    )

  private def _safe_relative_path(value: String): Boolean =
    try {
      val path = Path.of(value)
      val normalized = path.normalize()
      value.nonEmpty && !value.contains('\\') && !path.isAbsolute && normalized == path &&
        !normalized.startsWith("..") &&
        !value.exists(character => Character.isISOControl(character))
    } catch {
      case _: RuntimeException => false
    }

  private def _safe_evidence_uri(value: String): Boolean =
    try {
      val uri = URI.create(value)
      uri.isAbsolute && Option(uri.getScheme).exists(scheme =>
        scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
      ) &&
        uri.normalize() == uri && uri.getHost != null && uri.getUserInfo == null &&
        uri.getQuery == null && uri.getFragment == null &&
        !value.exists(character => Character.isISOControl(character))
    } catch {
      case _: RuntimeException => false
    }

  private final case class ProviderBinding(
    providerid: ReviewProviderId,
    providerversion: ReviewVersion,
    rulesetid: ReviewRuleId,
    rulesetversion: ReviewVersion,
    bundledigest: Option[ReviewDigest]
  )

  private def _provider_binding(provider: ReviewProviderExecution): ProviderBinding =
    ProviderBinding(
      provider.provider.id,
      provider.provider.version,
      provider.ruleSet.id,
      provider.ruleSet.version,
      provider.bundleDigest
    )

  private def _provider_binding(provider: ReviewProviderAttribution): ProviderBinding =
    ProviderBinding(
      provider.provider.id,
      provider.provider.version,
      provider.ruleSet.id,
      provider.ruleSet.version,
      Some(provider.bundleDigest)
    )

  private def _identifier_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      value.nonEmpty && value.length <= 180 && _identifier_pattern.matches(value),
      "invalid-identifier",
      path,
      "Identifier does not match the bounded v1 syntax."
    )

  private def _version_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      value.nonEmpty && value.length <= 80,
      "invalid-version",
      path,
      "Version must be present and bounded."
    )

  private def _digest_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      _valid_digest(value),
      "invalid-digest",
      path,
      "Digest must be a lowercase SHA-256 value."
    )

  private def _instant_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      try {
        Instant.parse(value)
        true
      } catch {
        case _: RuntimeException => false
      },
      "invalid-instant",
      path,
      "Timestamp must be an ISO-8601 instant."
    )

  private def _instant_order_failure(
    start: ReviewInstant,
    end: ReviewInstant,
    path: String,
    message: String
  ): Option[CarReviewCodecFailure] =
    _failure_unless(
      _parse_instant(start.value).zip(_parse_instant(end.value)).forall { case (startinstant, endinstant) =>
        !endinstant.isBefore(startinstant)
      },
      "invalid-instant-order",
      path,
      message
    )

  private def _parse_instant(value: String): Option[Instant] =
    try Some(Instant.parse(value))
    catch {
      case _: RuntimeException => None
    }

  private def _valid_digest(value: String): Boolean =
    _digest_pattern.matches(value)

  private def _failure_unless(
    condition: Boolean,
    code: String,
    path: String,
    message: String
  ): Option[CarReviewCodecFailure] =
    Option.unless(condition)(_failure(code, path, message))

  private def _failure(code: String, path: String, message: String): CarReviewCodecFailure =
    CarReviewCodecFailure(code, path, message)

  private def _canonical_report_content(json: Json): Json = {
    val withoutrootvolatile = json.mapObject(
      _.remove("reportDigest").remove("reportId").remove("reviewId").remove("createdAt")
    )
    val withoutexecutionvolatile = withoutrootvolatile.mapObject { root =>
      val execution = root("execution").getOrElse(Json.obj()).mapObject { value =>
        val providers = value("providers").flatMap(_.asArray).getOrElse(Vector.empty).map(
          _.mapObject(_.remove("startedAt").remove("completedAt"))
        )
        value
          .remove("startedAt")
          .remove("completedAt")
          .add("providers", Json.fromValues(providers))
      }
      val withoutbaselineidentity = root("baseline").map(_.mapObject(_.remove("reportId")))
      val withexecution = root.add("execution", execution)
      withoutbaselineidentity.fold(withexecution)(withexecution.add("baseline", _))
    }
    _canonicalize_arrays(withoutexecutionvolatile)
  }

  private def _canonicalize_arrays(json: Json): Json =
    json.arrayOrObject(
      json,
      values => {
        val normalized = values.map(_canonicalize_arrays)
        Json.fromValues(normalized.sortBy(_printer.print))
      },
      fields => Json.fromJsonObject(
        JsonObject.fromIterable(fields.toVector.map { case (key, value) => key -> _canonicalize_arrays(value) })
      )
    )

  private def _sha256(json: Json): String = {
    val bytes = _printer.print(json).getBytes(StandardCharsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }

  private def _validate_wire_shape(json: Json): Either[CarReviewCodecFailure, Unit] =
    _report_wire_failures(json, "$").headOption.toLeft(())

  private def _report_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "schemaVersion", "documentType", "reportId", "reportDigest", "reviewId", "createdAt", "target", "profile",
      "execution", "evidence", "observations", "assessments", "limitations", "baseline", "gate"
    ), path) ++
      _object_child(json, "target", path)(_target_wire_failures) ++
      _object_child(json, "execution", path)(_execution_wire_failures) ++
      _array_child(json, "evidence", path)(_evidence_wire_failures) ++
      _array_child(json, "observations", path)(_observation_wire_failures) ++
      _array_child(json, "assessments", path)(_assessment_wire_failures) ++
      _array_child(json, "limitations", path)(_limitation_wire_failures) ++
      _object_child(json, "baseline", path)(_baseline_wire_failures) ++
      _object_child(json, "gate", path)(_gate_wire_failures)

  private def _target_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("kind", "organization", "name", "version", "digest"), path)

  private def _execution_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("startedAt", "completedAt", "providers"), path) ++
      _array_child(json, "providers", path)(_provider_execution_wire_failures)

  private def _provider_execution_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "provider", "ruleSet", "state", "bundleDigest", "startedAt", "completedAt", "limitations"
    ), path) ++
      _object_child(json, "provider", path)(_identity_wire_failures) ++
      _object_child(json, "ruleSet", path)(_identity_wire_failures) ++
      _array_child(json, "limitations", path)(_limitation_wire_failures)

  private def _limitation_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("code", "scope", "subjectId", "message", "retryable"), path)

  private def _identity_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("id", "version"), path)

  private def _evidence_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "id", "kind", "subject", "providerId", "bundleDigest", "providerEvidenceId", "location", "digest", "facts"
    ), path) ++
      _object_child(json, "subject", path)(_subject_wire_failures) ++
      _object_child(json, "location", path)(_location_wire_failures)

  private def _subject_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("kind", "id"), path)

  private def _location_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("uri", "path", "line", "column", "endLine", "endColumn"), path)

  private def _observation_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "id", "type", "rule", "subject", "message", "rationale", "severity", "confidence", "evidenceIds",
      "locations", "provider", "disposition", "mappings"
    ), path) ++
      _object_child(json, "rule", path)(_identity_wire_failures) ++
      _object_child(json, "subject", path)(_subject_wire_failures) ++
      _array_child(json, "locations", path)(_location_wire_failures) ++
      _object_child(json, "provider", path)(_attribution_wire_failures) ++
      _object_child(json, "disposition", path)(_disposition_wire_failures) ++
      _object_child(json, "mappings", path)(_mappings_wire_failures)

  private def _attribution_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("provider", "ruleSet", "bundleDigest"), path) ++
      _object_child(json, "provider", path)(_identity_wire_failures) ++
      _object_child(json, "ruleSet", path)(_identity_wire_failures)

  private def _disposition_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("state", "reason", "author", "expiresAt"), path)

  private def _mappings_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("cncfFeatures", "implementationSubjects", "qualityCapabilities"), path)

  private def _assessment_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "capabilityId", "applicability", "maturity", "coverage", "confidence", "providerIds", "observationIds",
      "evidenceIds", "strengths", "gaps"
    ), path) ++ _object_child(json, "coverage", path)(_coverage_wire_failures)

  private def _coverage_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("applicableSubjects", "assessedSubjects", "unknownSubjects", "basisPoints"), path)

  private def _baseline_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "reportId", "digest", "addedObservationIds", "removedObservationIds", "unchangedObservationIds"
    ), path)

  private def _gate_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("policyId", "policyVersion", "result", "reasons", "blockingObservationIds"), path)

  private def _unknown_fields(
    json: Json,
    allowed: Set[String],
    path: String
  ): Vector[CarReviewCodecFailure] =
    json.asObject.toVector.flatMap { fields =>
      fields.keys.filterNot(allowed.contains).toVector.sorted.map { field =>
        _failure("unknown-field", s"$path.$field", "Review Report contains a field outside the v1 contract.")
      }
    }

  private def _object_child(
    json: Json,
    field: String,
    path: String
  )(check: (Json, String) => Vector[CarReviewCodecFailure]): Vector[CarReviewCodecFailure] =
    json.asObject.flatMap(_(field)).filterNot(_.isNull).toVector.flatMap(value => check(value, s"$path.$field"))

  private def _array_child(
    json: Json,
    field: String,
    path: String
  )(check: (Json, String) => Vector[CarReviewCodecFailure]): Vector[CarReviewCodecFailure] =
    json.asObject.flatMap(_(field)).flatMap(_.asArray).toVector.flatten.zipWithIndex.flatMap { case (value, index) =>
      check(value, s"$path.$field[$index]")
    }
}
