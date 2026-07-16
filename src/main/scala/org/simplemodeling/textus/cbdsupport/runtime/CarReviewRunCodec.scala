package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant

import io.circe.{Decoder, Encoder, Json, JsonObject, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object CarReviewRunCodec {
  private val _identifier_pattern = "^[A-Za-z0-9][A-Za-z0-9._:/-]*$".r
  private val _digest_pattern = "^sha256:[0-9a-f]{64}$".r
  private val _printer = Printer.noSpaces.copy(dropNullValues = true, sortKeys = true)

  private given Encoder[ReviewId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewId] = Decoder.decodeString.map(ReviewId.apply)
  private given Encoder[ReviewReportId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewReportId] = Decoder.decodeString.map(ReviewReportId.apply)
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
  private given Encoder[ReviewProviderId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewProviderId] = Decoder.decodeString.map(ReviewProviderId.apply)
  private given Encoder[ReviewRuleId] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewRuleId] = Decoder.decodeString.map(ReviewRuleId.apply)
  private given Encoder[ReviewProviderState] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewProviderState] = Decoder.decodeString.map(ReviewProviderState.apply)
  private given Encoder[ReviewLimitationScope] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewLimitationScope] = Decoder.decodeString.map(ReviewLimitationScope.apply)
  private given Encoder[ReviewRunState] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewRunState] = Decoder.decodeString.map(ReviewRunState.apply)
  private given Encoder[ReviewFailureCode] = Encoder.encodeString.contramap(_.value)
  private given Decoder[ReviewFailureCode] = Decoder.decodeString.map(ReviewFailureCode.apply)

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
  private given Encoder[CarReviewRun] = deriveEncoder
  private given Decoder[CarReviewRun] = deriveDecoder

  def decode(value: String): Either[CarReviewCodecFailure, CarReviewRun] =
    for {
      json <- parse(value).left.map(_ => _failure("invalid-json", "$", "Review Run JSON is invalid."))
      _ <- _validate_wire_shape(json)
      run <- summon[Decoder[CarReviewRun]].decodeJson(json).left.map(
        _ => _failure("invalid-document", "$", "Review Run fields do not match the v1 contract.")
      )
      _ <- validate(run)
    } yield run

  def encode(run: CarReviewRun): Either[CarReviewCodecFailure, String] =
    validate(run).map(_ => _printer.print(_canonicalize_arrays(summon[Encoder[CarReviewRun]].apply(run))))

  def validate(run: CarReviewRun): Either[CarReviewCodecFailure, Unit] = {
    val provideridentities = run.providers.map(provider => (
      provider.provider.id,
      provider.provider.version,
      provider.ruleSet.id,
      provider.ruleSet.version
    ))
    val terminal = CarReviewRunVocabulary.TERMINAL_STATES.contains(run.state.value)
    val failures = Vector(
      _failure_unless(
        run.schemaVersion.value == CarReviewVocabulary.SCHEMA_VERSION,
        "incompatible-schema",
        "$.schemaVersion",
        "Review Run schema version is not supported."
      ),
      _failure_unless(
        run.documentType.value == CarReviewRunVocabulary.DOCUMENT_TYPE,
        "incompatible-document",
        "$.documentType",
        "Document type is not review-run."
      ),
      _identifier_failure(run.reviewId.value, "$.reviewId"),
      _identifier_failure(run.profile.value, "$.profile"),
      _failure_unless(
        CarReviewRunVocabulary.STATES.contains(run.state.value),
        "invalid-run-state",
        "$.state",
        "Review Run state is not supported."
      ),
      _failure_unless(
        CarReviewVocabulary.TARGET_KINDS.contains(run.target.kind.value),
        "invalid-target",
        "$.target.kind",
        "Target kind is not supported."
      ),
      run.target.organization.flatMap(_identifier_failure(_, "$.target.organization")),
      _identifier_failure(run.target.name, "$.target.name"),
      run.target.version.flatMap(value => _bounded_failure(value.value, 80, "invalid-version", "$.target.version")),
      _digest_failure(run.target.digest.value, "$.target.digest"),
      _instant_failure(run.startedAt.value, "$.startedAt"),
      _instant_failure(run.updatedAt.value, "$.updatedAt"),
      _instant_order_failure(run.startedAt, run.updatedAt, "$", "Review Run update cannot precede its start."),
      run.completedAt.flatMap(value => _instant_failure(value.value, "$.completedAt")),
      run.completedAt.flatMap(value =>
        _instant_order_failure(value, run.updatedAt, "$", "Review Run update cannot precede its completion.")
      ),
      _failure_unless(
        terminal == run.completedAt.nonEmpty,
        "invalid-terminal-state",
        "$.completedAt",
        "Exactly terminal Review Runs require a completion time."
      ),
      _failure_unless(
        (run.state.value == "completed") == (run.reportId.nonEmpty && run.reportDigest.nonEmpty),
        "invalid-completion",
        "$",
        "Exactly a completed Review Run requires report identity and digest."
      ),
      _failure_unless(
        run.state.value == "completed" || (run.reportId.isEmpty && run.reportDigest.isEmpty),
        "invalid-completion",
        "$",
        "A non-completed Review Run cannot expose a report."
      ),
      _failure_unless(
        (run.state.value == "failed") == run.failureCode.nonEmpty,
        "invalid-failure",
        "$.failureCode",
        "Exactly a failed Review Run requires a failure code."
      ),
      run.reportId.flatMap(value => _identifier_failure(value.value, "$.reportId")),
      run.reportDigest.flatMap(value => _digest_failure(value.value, "$.reportDigest")),
      run.failureCode.flatMap(value => _identifier_failure(value.value, "$.failureCode")),
      _failure_unless(
        provideridentities.distinct == provideridentities,
        "duplicate-provider",
        "$.providers",
        "Provider execution identities must be unique."
      )
    ) ++ run.providers.zipWithIndex.flatMap { case (provider, index) =>
      _provider_failures(provider, run, s"$$.providers[$index]")
    } ++ run.limitations.zipWithIndex.flatMap { case (limitation, index) =>
      _limitation_failures(limitation, s"$$.limitations[$index]")
    }
    failures.collectFirst { case Some(failure) => failure }.toLeft(())
  }

  private def _provider_failures(
    provider: ReviewProviderExecution,
    run: CarReviewRun,
    path: String
  ): Vector[Option[CarReviewCodecFailure]] =
    Vector(
      _identifier_failure(provider.provider.id.value, s"$path.provider.id"),
      _bounded_failure(provider.provider.version.value, 80, "invalid-version", s"$path.provider.version"),
      _identifier_failure(provider.ruleSet.id.value, s"$path.ruleSet.id"),
      _bounded_failure(provider.ruleSet.version.value, 80, "invalid-version", s"$path.ruleSet.version"),
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
      provider.bundleDigest.flatMap(value => _digest_failure(value.value, s"$path.bundleDigest")),
      provider.startedAt.flatMap(value => _instant_failure(value.value, s"$path.startedAt")),
      provider.completedAt.flatMap(value => _instant_failure(value.value, s"$path.completedAt")),
      (provider.startedAt, provider.completedAt) match {
        case (Some(start), Some(end)) =>
          _instant_order_failure(start, end, path, "Provider completion cannot precede its start.")
        case _ => None
      },
      provider.startedAt.flatMap(value =>
        _instant_order_failure(run.startedAt, value, path, "Provider execution cannot precede the Review Run.")
      ),
      provider.completedAt.flatMap(value =>
        _instant_order_failure(value, run.updatedAt, path, "Provider completion cannot follow the Review Run update.")
      )
    ) ++ provider.limitations.zipWithIndex.flatMap { case (limitation, index) =>
      _limitation_failures(limitation, s"$path.limitations[$index]")
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

  private def _identifier_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      value.nonEmpty && value.length <= 180 && _identifier_pattern.matches(value),
      "invalid-identifier",
      path,
      "Identifier does not match the bounded v1 syntax."
    )

  private def _bounded_failure(
    value: String,
    max: Int,
    code: String,
    path: String
  ): Option[CarReviewCodecFailure] =
    _failure_unless(value.nonEmpty && value.length <= max, code, path, "Value must be present and bounded.")

  private def _digest_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(
      _digest_pattern.matches(value),
      "invalid-digest",
      path,
      "Digest must be a lowercase SHA-256 value."
    )

  private def _instant_failure(value: String, path: String): Option[CarReviewCodecFailure] =
    _failure_unless(_parse_instant(value).nonEmpty, "invalid-instant", path, "Timestamp must be an ISO-8601 instant.")

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

  private def _failure_unless(
    condition: Boolean,
    code: String,
    path: String,
    message: String
  ): Option[CarReviewCodecFailure] =
    Option.unless(condition)(_failure(code, path, message))

  private def _failure(code: String, path: String, message: String): CarReviewCodecFailure =
    CarReviewCodecFailure(code, path, message)

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

  private def _validate_wire_shape(json: Json): Either[CarReviewCodecFailure, Unit] =
    _run_wire_failures(json, "$").headOption.toLeft(())

  private def _run_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "schemaVersion", "documentType", "reviewId", "target", "profile", "state", "providers", "limitations",
      "startedAt", "updatedAt", "completedAt", "reportId", "reportDigest", "failureCode"
    ), path) ++
      _object_child(json, "target", path)(_target_wire_failures) ++
      _array_child(json, "providers", path)(_provider_wire_failures) ++
      _array_child(json, "limitations", path)(_limitation_wire_failures)

  private def _target_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("kind", "organization", "name", "version", "digest"), path)

  private def _provider_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set(
      "provider", "ruleSet", "state", "bundleDigest", "startedAt", "completedAt", "limitations"
    ), path) ++
      _object_child(json, "provider", path)(_identity_wire_failures) ++
      _object_child(json, "ruleSet", path)(_identity_wire_failures) ++
      _array_child(json, "limitations", path)(_limitation_wire_failures)

  private def _identity_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("id", "version"), path)

  private def _limitation_wire_failures(json: Json, path: String): Vector[CarReviewCodecFailure] =
    _unknown_fields(json, Set("code", "scope", "subjectId", "message", "retryable"), path)

  private def _unknown_fields(
    json: Json,
    allowed: Set[String],
    path: String
  ): Vector[CarReviewCodecFailure] =
    json.asObject.toVector.flatMap { fields =>
      fields.keys.filterNot(allowed.contains).toVector.sorted.map { field =>
        _failure("unknown-field", s"$path.$field", "Review Run contains a field outside the v1 contract.")
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
