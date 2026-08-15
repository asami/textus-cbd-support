package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, Printer}
import io.circe.parser.parse

/*
 * @since   Jul. 16, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ProviderBundleAvailability {
  def state: ReviewProviderState
  def runFailure: Boolean
}

object ProviderBundleAvailability {
  case object Enabled extends ProviderBundleAvailability {
    val state = ReviewProviderState("running")
    val runFailure = false
  }

  case object Unavailable extends ProviderBundleAvailability {
    val state = ReviewProviderState("unavailable")
    val runFailure = false
  }

  case object Disabled extends ProviderBundleAvailability {
    val state = ReviewProviderState("disabled")
    val runFailure = false
  }

  case object Failed extends ProviderBundleAvailability {
    val state = ReviewProviderState("failed")
    val runFailure = true
  }
}

final case class ProviderBundleAdmissionContext(
  reviewId: ReviewId,
  target: ReviewTarget,
  availability: ProviderBundleAvailability,
  descriptor: String,
  request: String,
  bundle: String
)

final case class AdmittedProviderBundle(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  requestDigest: ReviewDigest,
  bundleDigest: ReviewDigest,
  evidenceIds: Vector[String],
  observationIds: Vector[String],
  limitations: Vector[ReviewLimitation]
)

final case class ReviewProviderCapability(
  id: ReviewCapabilityId,
  version: ReviewVersion,
  evidenceKinds: Vector[String],
  observationKinds: Vector[String]
)

final case class CarReviewProviderDescriptor(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  capabilities: Vector[ReviewProviderCapability]
)

final case class ProviderBundleUnknown(
  provider: Option[ReviewProviderIdentity],
  state: ReviewProviderState,
  limitation: ReviewLimitation,
  runFailure: Boolean
)

sealed trait ProviderBundleAdmissionOutcome

object ProviderBundleAdmissionOutcome {
  final case class Admitted(value: AdmittedProviderBundle) extends ProviderBundleAdmissionOutcome
  final case class Refused(value: ProviderBundleUnknown) extends ProviderBundleAdmissionOutcome
}

/**
 * CBD-owned boundary for provider evidence bundles.  It deliberately retains a
 * refusal as an attributable Unknown-shaped result; callers must not turn a
 * provider's assurance into a canonical assurance before this boundary admits it.
 */
object CarReviewProviderBundleAdmission {
  private val _schema_version = "textus.cbd.review-provider.v1"
  private val _request_fields = Set("schemaVersion", "documentType", "reviewId", "target", "requestedCapabilities", "requestedEvidenceKinds", "rules", "limits", "baseline")
  private val _request_required_fields = Set("schemaVersion", "documentType", "reviewId", "target", "requestedCapabilities", "requestedEvidenceKinds", "rules", "limits")
  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r

  def describeDescriptor(value: String): Either[String, CarReviewProviderDescriptor] =
    _admitted_descriptor(value).map(_._1)

  /** Digest of the complete admitted descriptor document, not its projection. */
  def descriptorDigest(value: String): Either[String, ReviewDigest] =
    _admitted_descriptor(value).map { case (_, descriptor) => ReviewDigest(_sha256(descriptor)) }

  private def _admitted_descriptor(value: String): Either[String, (CarReviewProviderDescriptor, Json)] =
    for {
      descriptor <- _parse(value, "descriptor")
      _ <- _document(descriptor, "provider-descriptor", "descriptor")
      _ <- _shape(descriptor, Set("schemaVersion", "documentType", "provider", "ruleSet", "supportedSchemaVersions", "capabilities", "limitations"), Set("schemaVersion", "documentType", "provider", "ruleSet", "supportedSchemaVersions", "capabilities", "limitations"), "descriptor")
      provider <- _provider_identity(descriptor).toRight("descriptor-provider-missing")
      ruleset <- _rule_identity(descriptor).toRight("descriptor-ruleset-missing")
      _ <- Either.cond(_strings(descriptor, "supportedSchemaVersions").exists(_.contains(_schema_version)), (), "descriptor-schema-not-supported")
      capabilities <- _descriptor_capabilities(descriptor)
      _ <- _limitations(descriptor)
    } yield CarReviewProviderDescriptor(provider, ruleset, capabilities) -> descriptor

  def admit(context: ProviderBundleAdmissionContext): ProviderBundleAdmissionOutcome = {
    val descriptor = _parse(context.descriptor, "descriptor")
    val request = _parse(context.request, "request")
    val bundle = _parse(context.bundle, "bundle")
    val provider = descriptor.toOption.flatMap(_provider_identity)

    context.availability match {
      case ProviderBundleAvailability.Enabled =>
        (descriptor, request, bundle) match {
          case (Right(descriptorjson), Right(requestjson), Right(bundlejson)) =>
            _validate(context, descriptorjson, requestjson, bundlejson) match {
              case Right(admitted) => ProviderBundleAdmissionOutcome.Admitted(admitted)
              case Left(failure) => _refused(provider, ReviewProviderState("incompatible"), failure, runfailure = false)
            }
          case _ =>
            val failure = Vector(descriptor, request, bundle).collectFirst { case Left(value) => value }.getOrElse("invalid-provider-document")
            _refused(provider, ReviewProviderState("incompatible"), failure, runfailure = false)
        }
      case availability =>
        _refused(provider, availability.state, s"provider-${availability.state.value}", availability.runFailure)
    }
  }

  def requestDigest(value: String): Either[String, ReviewDigest] =
    for {
      request <- _parse(value, "request")
      _ <- _document(request, "provider-request", "request")
      _ <- _shape(request, _request_fields, _request_required_fields, "request")
    } yield ReviewDigest(_sha256(request))

  /**
   * Validates an executable provider request against the identity already
   * admitted by its outer provider-selection envelope.
   */
  private[runtime] def requestBinding(
    value: String,
    reviewid: ReviewId,
    target: ReviewTarget
  ): Either[String, Unit] =
    for {
      request <- _parse(value, "request")
      _ <- _document(request, "provider-request", "request")
      _ <- _shape(request, _request_fields, _request_required_fields, "request")
      _ <- _target_matches(target, request, "request")
      _ <- Either.cond(_string(request, "reviewId").contains(reviewid.value), (), "review-id-mismatch")
    } yield ()

  def timeoutMillis(value: String): Either[String, Long] =
    for {
      request <- _parse(value, "request")
      _ <- _document(request, "provider-request", "request")
      limits <- request.hcursor.downField("limits").focus.flatMap(_.asObject).toRight("request-limits-invalid")
      timeout <- limits("timeoutMillis").flatMap(_.asNumber).flatMap(_.toLong).filter(_ > 0).toRight("request-timeout-invalid")
    } yield timeout

  private def _validate(
    context: ProviderBundleAdmissionContext,
    descriptor: Json,
    request: Json,
    bundle: Json
  ): Either[String, AdmittedProviderBundle] =
    for {
      _ <- _document(descriptor, "provider-descriptor", "descriptor")
      _ <- _document(request, "provider-request", "request")
      _ <- _document(bundle, "evidence-bundle", "bundle")
      _ <- _shape(descriptor, Set("schemaVersion", "documentType", "provider", "ruleSet", "supportedSchemaVersions", "capabilities", "limitations"), Set("schemaVersion", "documentType", "provider", "ruleSet", "supportedSchemaVersions", "capabilities", "limitations"), "descriptor")
      _ <- _shape(request, _request_fields, _request_required_fields, "request")
      _ <- _shape(bundle, Set("schemaVersion", "documentType", "reviewId", "target", "provider", "ruleSet", "requestDigest", "bundleDigest", "evidence", "observations", "limitations"), Set("schemaVersion", "documentType", "reviewId", "target", "provider", "ruleSet", "requestDigest", "bundleDigest", "evidence", "observations", "limitations"), "bundle")
      provider <- _provider_identity(descriptor).toRight("descriptor-provider-missing")
      ruleset <- _rule_identity(descriptor).toRight("descriptor-ruleset-missing")
      _ <- Either.cond(_strings(descriptor, "supportedSchemaVersions").exists(_.contains(_schema_version)), (), "descriptor-schema-not-supported")
      requestedcapabilities <- _required_strings(request, "requestedCapabilities")
      requestedevidencekinds <- _strings(request, "requestedEvidenceKinds").toRight("request-evidence-kinds-invalid")
      _ <- _rules(request)
      capabilities <- _capabilities(descriptor)
      _ <- _limitations(descriptor)
      _ <- Either.cond(requestedcapabilities.forall(capabilities.keySet.contains), (), "unsupported-capability")
      _ <- Either.cond(requestedevidencekinds.forall(kind => capabilities.values.exists(_.contains(kind))), (), "unsupported-evidence-kind")
      _ <- _target_matches(context.target, request, "request")
      _ <- Either.cond(_string(request, "reviewId").contains(context.reviewId.value), (), "review-id-mismatch")
      _ <- _target_matches(context.target, bundle, "bundle")
      _ <- Either.cond(_string(bundle, "reviewId").contains(context.reviewId.value), (), "bundle-review-id-mismatch")
      _ <- Either.cond(_provider_identity(bundle).contains(provider), (), "bundle-provider-mismatch")
      _ <- Either.cond(_rule_identity(bundle).contains(ruleset), (), "bundle-ruleset-mismatch")
      requestdigest = ReviewDigest(_sha256(request))
      _ <- Either.cond(_string(bundle, "requestDigest").contains(requestdigest.value), (), "request-digest-mismatch")
      bundledigest <- _bundle_digest(bundle)
      _ <- _bundle_members(bundle, capabilities.keySet, capabilities.values.flatten.toSet)
      _ <- _limits(request, bundle)
      limitations <- _limitations(bundle)
      evidenceids <- _jsons(bundle, "evidence").flatMap(_ids(_, "evidence"))
      observationids <- _jsons(bundle, "observations").flatMap(_ids(_, "observations"))
    } yield AdmittedProviderBundle(
      provider,
      ruleset,
      requestdigest,
      bundledigest,
      evidenceids,
      observationids,
      limitations
    )

  private def _document(json: Json, documenttype: String, name: String): Either[String, Unit] =
    Either.cond(
      _string(json, "schemaVersion").contains(_schema_version) && _string(json, "documentType").contains(documenttype),
      (),
      s"$name-schema-or-document-incompatible"
    )

  private def _shape(json: Json, allowed: Set[String], required: Set[String], name: String): Either[String, Unit] =
    json.asObject match {
      case Some(fields) if fields.keys.forall(allowed.contains) && required.subsetOf(fields.keys.toSet) => Right(())
      case Some(_) => Left(s"$name-contains-unknown-field")
      case None => Left(s"$name-is-not-object")
    }

  private def _rules(request: Json): Either[String, Unit] =
    request.hcursor.downField("rules").focus.flatMap(_.asObject) match {
      case Some(fields) if fields.keys.toSet == Set("include", "exclude") =>
        for {
          included <- fields("include").flatMap(_.asArray).map(_.toVector.flatMap(_.asString)).toRight("request-rule-include-invalid")
          excluded <- fields("exclude").flatMap(_.asArray).map(_.toVector.flatMap(_.asString)).toRight("request-rule-exclude-invalid")
          _ <- Either.cond(included.distinct.size == included.size && excluded.distinct.size == excluded.size && included.intersect(excluded).isEmpty, (), "request-rule-selection-invalid")
        } yield ()
      case _ => Left("request-rules-invalid")
    }

  private def _provider_identity(json: Json): Option[ReviewProviderIdentity] =
    _identity(json, "provider").map { case (id, version) => ReviewProviderIdentity(ReviewProviderId(id), ReviewVersion(version)) }

  private def _rule_identity(json: Json): Option[ReviewRuleIdentity] =
    _identity(json, "ruleSet").map { case (id, version) => ReviewRuleIdentity(ReviewRuleId(id), ReviewVersion(version)) }

  private def _identity(json: Json, name: String): Option[(String, String)] =
    json.hcursor.downField(name).focus.flatMap { value =>
      for {
        fields <- value.asObject
        if fields.keys.toSet == Set("id", "version")
        id <- fields("id").flatMap(_.asString).filter(_valid_identifier)
        version <- fields("version").flatMap(_.asString).filter(_valid_version)
      } yield id -> version
    }

  private def _capabilities(descriptor: Json): Either[String, Map[String, Vector[String]]] =
    _descriptor_capabilities(descriptor).map(_.map(capability => capability.id.value -> capability.evidenceKinds).toMap)

  private def _descriptor_capabilities(descriptor: Json): Either[String, Vector[ReviewProviderCapability]] =
    _jsons(descriptor, "capabilities").flatMap { values =>
      val parsed = values.map { value =>
        for {
          fields <- value.asObject.toRight("descriptor-capability-invalid")
          _ <- Either.cond(fields.keys.toSet == Set("id", "version", "evidenceKinds", "observationKinds"), (), "descriptor-capability-unknown-field")
          id <- fields("id").flatMap(_.asString).filter(_valid_identifier).toRight("descriptor-capability-id-invalid")
          version <- fields("version").flatMap(_.asString).filter(_valid_version).toRight("descriptor-capability-version-invalid")
          evidence <- fields("evidenceKinds").flatMap(_.asArray).map(_.toVector.flatMap(_.asString)).filter(_.size == fields("evidenceKinds").flatMap(_.asArray).fold(0)(_.size)).toRight("descriptor-evidence-kinds-invalid")
          observations <- fields("observationKinds").flatMap(_.asArray).map(_.toVector.flatMap(_.asString)).filter(_.size == fields("observationKinds").flatMap(_.asArray).fold(0)(_.size)).toRight("descriptor-observation-kinds-invalid")
          _ <- Either.cond(evidence.nonEmpty && evidence.distinct.size == evidence.size, (), "descriptor-evidence-kinds-invalid")
          _ <- Either.cond(observations.nonEmpty && observations.distinct.size == observations.size && observations.forall(CarReviewVocabulary.OBSERVATION_TYPES.contains), (), "descriptor-observation-kinds-invalid")
        } yield ReviewProviderCapability(ReviewCapabilityId(id), ReviewVersion(version), evidence, observations)
      }
      parsed.foldLeft[Either[String, Vector[ReviewProviderCapability]]](Right(Vector.empty)) { (z, value) =>
        for { xs <- z; x <- value } yield xs :+ x
      }.flatMap { values =>
        Either.cond(values.nonEmpty && values.map(_.id).distinct.size == values.size, values, "descriptor-capabilities-duplicate-or-empty")
      }
    }

  private def _target_matches(expected: ReviewTarget, json: Json, name: String): Either[String, Unit] =
    json.hcursor.downField("target").focus.flatMap(_.asObject) match {
      case Some(fields) if fields.keys.forall(Set("kind", "organization", "name", "version", "digest").contains) =>
        val same =
          fields("kind").flatMap(_.asString).contains(expected.kind.value) &&
            fields("name").flatMap(_.asString).contains(expected.name) &&
            fields("organization").flatMap(_.asString) == expected.organization &&
            fields("version").flatMap(_.asString) == expected.version.map(_.value) &&
            fields("digest").flatMap(_.asString).contains(expected.digest.value)
        Either.cond(same, (), s"$name-target-mismatch")
      case _ => Left(s"$name-target-invalid")
    }

  private def _bundle_digest(bundle: Json): Either[String, ReviewDigest] =
    for {
      supplied <- _string(bundle, "bundleDigest").filter(_valid_digest).toRight("bundle-digest-invalid")
      calculated = ReviewDigest(_sha256(bundle.mapObject(_.remove("bundleDigest"))))
      _ <- Either.cond(supplied == calculated.value, (), "bundle-digest-mismatch")
    } yield calculated

  private def _bundle_members(
    bundle: Json,
    supportedcapabilities: Set[String],
    supportedevidencekinds: Set[String]
  ): Either[String, Unit] =
    for {
      evidence <- _jsons(bundle, "evidence")
      observations <- _jsons(bundle, "observations")
      evidenceids <- _ids(evidence, "evidence")
      evidencekinds <- evidence.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (z, value) =>
        for { xs <- z; kind <- _string(value, "kind").filter(_valid_identifier).toRight("evidence-kind-invalid") } yield xs :+ kind
      }
      observationids <- _ids(observations, "observations")
      references = observations.flatMap(value => _strings(value, "evidenceIds").getOrElse(Vector("__invalid__")))
      _ <- Either.cond(evidenceids.distinct.size == evidenceids.size, (), "duplicate-evidence-id")
      _ <- Either.cond(evidencekinds.forall(supportedevidencekinds.contains), (), "unsupported-evidence-kind")
      _ <- Either.cond(observationids.distinct.size == observationids.size, (), "duplicate-observation-id")
      _ <- Either.cond(references.forall(evidenceids.contains), (), "unresolved-evidence-reference")
      _ <- observations.foldLeft[Either[String, Unit]](Right(())) { (z, value) =>
        z.flatMap(_ => _mappings(value, supportedcapabilities))
      }
    } yield ()

  /**
   * Mappings are optional so that v1 providers which predate named views remain
   * admissible.  When supplied they are part of the provider's signed bundle
   * content: CBD validates the exact shape and accepts a quality mapping only
   * when that capability was declared by the provider descriptor.
   */
  private def _mappings(observation: Json, supportedcapabilities: Set[String]): Either[String, Unit] =
    observation.hcursor.downField("mappings").focus match {
      case None => Right(())
      case Some(value) =>
        value.asObject match {
          case Some(fields) if fields.keys.toSet == Set("cncfFeatures", "implementationSubjects", "qualityCapabilities") =>
            for {
              features <- fields("cncfFeatures").toRight("observation-mapping-cncf-features-invalid").flatMap(_mapping_strings(_, "observation-mapping-cncf-features-invalid"))
              subjects <- fields("implementationSubjects").toRight("observation-mapping-implementation-subjects-invalid").flatMap(_mapping_strings(_, "observation-mapping-implementation-subjects-invalid"))
              capabilities <- fields("qualityCapabilities").toRight("observation-mapping-quality-capabilities-invalid").flatMap(_mapping_strings(_, "observation-mapping-quality-capabilities-invalid"))
              _ <- Either.cond(features.distinct.size == features.size, (), "observation-mapping-cncf-features-invalid")
              _ <- Either.cond(subjects.distinct.size == subjects.size, (), "observation-mapping-implementation-subjects-invalid")
              _ <- Either.cond(capabilities.distinct.size == capabilities.size, (), "observation-mapping-quality-capabilities-invalid")
              _ <- Either.cond(capabilities.forall(id => supportedcapabilities.contains(id) && CarReviewCapabilityCatalog.definition(ReviewCapabilityId(id)).nonEmpty), (), "observation-mapping-quality-capability-unsupported")
            } yield ()
          case _ => Left("observation-mappings-invalid")
        }
    }

  private def _mapping_strings(value: Json, error: String): Either[String, Vector[String]] =
    value.asArray.map(_.toVector.flatMap(_.asString)).filter(_.size == value.asArray.fold(0)(_.size)).filter(_.forall(_valid_identifier)).toRight(error)

  private def _limits(request: Json, bundle: Json): Either[String, Unit] =
    request.hcursor.downField("limits").focus.flatMap(_.asObject) match {
      case Some(limits) =>
        for {
          maxevidence <- limits("maxEvidenceItems").flatMap(_.asNumber).flatMap(_.toInt).filter(_ > 0).toRight("request-max-evidence-invalid")
          maxobservations <- limits("maxObservations").flatMap(_.asNumber).flatMap(_.toInt).filter(_ > 0).toRight("request-max-observations-invalid")
          maxbytes <- limits("maxInputBytes").flatMap(_.asNumber).flatMap(_.toLong).filter(_ > 0).toRight("request-max-bytes-invalid")
          _ <- Either.cond(_jsons(bundle, "evidence").exists(_.size <= maxevidence), (), "evidence-limit-exceeded")
          _ <- Either.cond(_jsons(bundle, "observations").exists(_.size <= maxobservations), (), "observation-limit-exceeded")
          _ <- Either.cond(_printer.print(bundle).getBytes(StandardCharsets.UTF_8).length.toLong <= maxbytes, (), "bundle-byte-limit-exceeded")
        } yield ()
      case None => Left("request-limits-invalid")
    }

  private def _limitations(bundle: Json): Either[String, Vector[ReviewLimitation]] =
    _jsons(bundle, "limitations").flatMap { values =>
      val parsed = values.map { value =>
        for {
          fields <- value.asObject.toRight("bundle-limitation-invalid")
          code <- fields("code").flatMap(_.asString).filter(_valid_identifier).toRight("bundle-limitation-code-invalid")
          scope <- fields("scope").flatMap(_.asString).filter(Set("provider", "capability", "rule", "target", "evidence", "observation").contains).toRight("bundle-limitation-scope-invalid")
          message <- fields("message").flatMap(_.asString).filter(value => value.nonEmpty && value.length <= 1024).toRight("bundle-limitation-message-invalid")
          retryable <- fields("retryable").flatMap(_.asBoolean).toRight("bundle-limitation-retryable-invalid")
          subjectid = fields("subjectId").flatMap(_.asString)
        } yield ReviewLimitation(code, ReviewLimitationScope(scope), subjectid, message, retryable)
      }
      parsed.foldLeft[Either[String, Vector[ReviewLimitation]]](Right(Vector.empty)) { (z, value) =>
        for { xs <- z; x <- value } yield xs :+ x
      }
    }

  private def _ids(values: Vector[Json], name: String): Either[String, Vector[String]] =
    values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (z, value) =>
      for {
        xs <- z
        id <- _string(value, "id").filter(_valid_identifier).toRight(s"$name-id-invalid")
      } yield xs :+ id
    }

  private def _strings(json: Json, name: String): Option[Vector[String]] =
    json.hcursor.get[Vector[String]](name).toOption

  private def _required_strings(json: Json, name: String): Either[String, Vector[String]] =
    _strings(json, name).filter(values => values.nonEmpty && values.distinct.size == values.size).toRight(s"$name-invalid")

  private def _jsons(json: Json, name: String): Either[String, Vector[Json]] =
    json.hcursor.downField(name).focus.flatMap(_.asArray).map(_.toVector).toRight(s"$name-invalid")

  private def _string(json: Json, name: String): Option[String] =
    json.hcursor.get[String](name).toOption

  private def _parse(value: String, name: String): Either[String, Json] =
    parse(value).left.map(_ => s"$name-json-invalid")

  private def _refused(
    provider: Option[ReviewProviderIdentity],
    state: ReviewProviderState,
    code: String,
    runfailure: Boolean
  ): ProviderBundleAdmissionOutcome.Refused =
    ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(
      provider,
      state,
      ReviewLimitation(code, ReviewLimitationScope("provider"), provider.map(_.id.value), s"Provider bundle was not admitted: $code.", retryable = state.value == "unavailable"),
      runfailure
    ))

  private def _valid_identifier(value: String): Boolean =
    value.nonEmpty && value.length <= 160 && value.matches("^[A-Za-z0-9][A-Za-z0-9._:/-]*$")

  private def _valid_version(value: String): Boolean = value.nonEmpty && value.length <= 80
  private def _valid_digest(value: String): Boolean = _digest_pattern.matches(value)

  private def _sha256(json: Json): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(_printer.print(json).getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
