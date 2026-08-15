package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject, Printer}

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewProductionJobBinding(
  diagnosisId: String,
  reviewId: ReviewId,
  reuseKeyDefinition: String,
  reuseKeyDigest: ReviewDigest,
  target: ReviewTarget,
  profile: ReviewProfile,
  startedAt: ReviewInstant
) {
  def validate(execution: CarReviewProductionExecution): Either[String, Unit] = {
    val plan = execution.plan
    if (!CarReviewProductionIdentity.isValid(diagnosisId) || !CarReviewProductionIdentity.isValid(reviewId.value))
      Left("review-job-binding-identity-invalid")
    else Either.cond(
      reviewId == plan.request.reviewId &&
        reuseKeyDefinition == plan.reuseKey.definitionId &&
        reuseKeyDigest == plan.reuseKey.digest &&
        target == plan.request.target &&
        profile == plan.request.profile &&
        startedAt == plan.request.startedAt,
      (),
      "review-job-binding-plan-mismatch"
    )
  }
}

object CarReviewProductionJobBinding {
  val SCHEMA_VERSION = "textus.cbd.review-job-binding.v1"
  private val _parameter_keys = Set(
    "bindingSchema", "diagnosisId", "reviewId", "reuseKeyDefinition",
    "reuseKeyDigest", "target", "profile", "startedAt"
  )
  private val _printer = Printer.noSpaces.copy(sortKeys = true)
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r

  def from(
    diagnosisId: String,
    execution: CarReviewProductionExecution
  ): Either[String, CarReviewProductionJobBinding] = {
    val plan = execution.plan
    val binding = CarReviewProductionJobBinding(
      diagnosisId,
      plan.request.reviewId,
      plan.reuseKey.definitionId,
      plan.reuseKey.digest,
      plan.request.target,
      plan.request.profile,
      plan.request.startedAt
    )
    binding.validate(execution).map(_ => binding)
  }

  def parameters(value: CarReviewProductionJobBinding): Map[String, String] =
    Map(
      "bindingSchema" -> SCHEMA_VERSION,
      "diagnosisId" -> value.diagnosisId,
      "reviewId" -> value.reviewId.value,
      "reuseKeyDefinition" -> value.reuseKeyDefinition,
      "reuseKeyDigest" -> value.reuseKeyDigest.value,
      "target" -> _printer.print(_target(value.target)),
      "profile" -> value.profile.value,
      "startedAt" -> value.startedAt.value
    )

  private[cbdsupport] def parse(parameters: Map[String, String]): Either[String, CarReviewProductionJobBinding] = {
    val bindingparameters = parameters.filter { case (key, _) => _parameter_keys.contains(key) }
    for {
      _ <- Either.cond(parameters.keys.forall(key => _parameter_keys.contains(key) || key.startsWith("cncf.")), (), "review-job-binding-parameters-invalid")
      _ <- Either.cond(bindingparameters.keySet == _parameter_keys, (), "review-job-binding-parameters-invalid")
      _ <- Either.cond(bindingparameters.get("bindingSchema").contains(SCHEMA_VERSION), (), "review-job-binding-schema-invalid")
      diagnosisid <- _required(bindingparameters, "diagnosisId")
      reviewid <- _required(bindingparameters, "reviewId")
      definition <- _required(bindingparameters, "reuseKeyDefinition")
      digest <- _required(bindingparameters, "reuseKeyDigest")
      targetdocument <- _required(bindingparameters, "target")
      profile <- _required(bindingparameters, "profile")
      startedat <- _required(bindingparameters, "startedAt")
      _ <- Either.cond(CarReviewProductionIdentity.isValid(diagnosisid) && CarReviewProductionIdentity.isValid(reviewid), (), "review-job-binding-identity-invalid")
      _ <- Either.cond(definition == CarReviewReuseKey.DEFINITION_ID, (), "review-job-binding-reuse-definition-invalid")
      _ <- Either.cond(_digest_pattern.matches(digest), (), "review-job-binding-reuse-digest-invalid")
      parsedtarget <- _parse_target(targetdocument)
      _ <- Either.cond(_profiles.contains(profile), (), "review-job-binding-profile-invalid")
      _ <- Either.cond(_instant(startedat), (), "review-job-binding-started-at-invalid")
    } yield CarReviewProductionJobBinding(
      diagnosisid,
      ReviewId(reviewid),
      definition,
      ReviewDigest(digest),
      parsedtarget,
      ReviewProfile(profile),
      ReviewInstant(startedat)
    )
  }

  private def _required(parameters: Map[String, String], key: String): Either[String, String] =
    parameters.get(key).map(_.trim).filter(_.nonEmpty).toRight("review-job-binding-parameters-invalid")

  private def _parse_target(value: String): Either[String, ReviewTarget] =
    io.circe.parser.parse(value).left.map(_ => "review-job-binding-target-invalid").flatMap { json =>
      json.asObject.toRight("review-job-binding-target-invalid").flatMap { fields =>
        val expected = Set("kind", "organization", "name", "version", "digest")
        for {
          _ <- Either.cond(fields.keys.toSet == expected, (), "review-job-binding-target-invalid")
          kind <- _string(fields, "kind")
          organization <- _nullable_string(fields, "organization")
          name <- _string(fields, "name")
          version <- _nullable_string(fields, "version")
          digest <- _string(fields, "digest")
          _ <- Either.cond(
            CarReviewVocabulary.TARGET_KINDS.contains(kind) && _identifier(name) &&
              organization.forall(_identifier) && version.forall(_version) && _digest_pattern.matches(digest),
            (),
            "review-job-binding-target-invalid"
          )
        } yield ReviewTarget(
          ReviewTargetKind(kind),
          organization,
          name,
          version.map(ReviewVersion.apply),
          ReviewDigest(digest)
        )
      }
    }

  private def _string(fields: JsonObject, key: String): Either[String, String] =
    fields(key).flatMap(_.asString).filter(_.nonEmpty).toRight("review-job-binding-target-invalid")

  private def _nullable_string(fields: JsonObject, key: String): Either[String, Option[String]] =
    fields(key).toRight("review-job-binding-target-invalid").flatMap { value =>
      if value.isNull then Right(None)
      else value.asString.filter(_.nonEmpty).map(Some.apply).toRight("review-job-binding-target-invalid")
    }

  private def _target(value: ReviewTarget): Json =
    Json.obj(
      "kind" -> Json.fromString(value.kind.value),
      "organization" -> value.organization.fold(Json.Null)(Json.fromString),
      "name" -> Json.fromString(value.name),
      "version" -> value.version.fold(Json.Null)(version => Json.fromString(version.value)),
      "digest" -> Json.fromString(value.digest.value)
    )

  private def _identifier(value: String): Boolean =
    value.nonEmpty && value.length <= 180 && value.forall(character => character.isLetterOrDigit || "._-:/".contains(character))

  private def _version(value: String): Boolean = value.nonEmpty && value.length <= 80
  private def _instant(value: String): Boolean = scala.util.Try(java.time.Instant.parse(value)).isSuccess
  private def _profiles = Set("development", "ci", "release", "server")
}

private[cbdsupport] object CarReviewProductionIdentity {
  private val _pattern = "[A-Za-z0-9][A-Za-z0-9._:/-]{0,179}".r

  def isValid(value: String): Boolean =
    Option(value).exists(_pattern.matches)
}

sealed trait CarReviewProductionTerminalLease {
  private[cbdsupport] def jobId: ReviewJobId
  private[cbdsupport] def binding: CarReviewProductionJobBinding
  private[cbdsupport] def run: CarReviewRun
}

object CarReviewProductionTerminalLease {
  final class Completed private[runtime] (
    private[cbdsupport] val jobId: ReviewJobId,
    private[cbdsupport] val binding: CarReviewProductionJobBinding,
    private[cbdsupport] val run: CarReviewRun,
    private[cbdsupport] val response: CarReviewCanonicalResponse
  ) extends CarReviewProductionTerminalLease

  final class Failed private[runtime] (
    private[cbdsupport] val jobId: ReviewJobId,
    private[cbdsupport] val binding: CarReviewProductionJobBinding,
    private[cbdsupport] val run: CarReviewRun
  ) extends CarReviewProductionTerminalLease

  final class Cancelled private[runtime] (
    private[cbdsupport] val jobId: ReviewJobId,
    private[cbdsupport] val binding: CarReviewProductionJobBinding,
    private[cbdsupport] val run: CarReviewRun
  ) extends CarReviewProductionTerminalLease

  private[runtime] def completed(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    run: CarReviewRun,
    response: CarReviewCanonicalResponse
  ): Completed = new Completed(jobid, binding, run, response)

  private[runtime] def failed(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    run: CarReviewRun
  ): Failed = new Failed(jobid, binding, run)

  private[runtime] def cancelled(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    run: CarReviewRun
  ): Cancelled = new Cancelled(jobid, binding, run)
}

final case class CarReviewDiscoveredProductionJob private[runtime] (
  jobId: ReviewJobId,
  binding: CarReviewProductionJobBinding,
  update: ReviewRunJobUpdate,
  canonicalResponse: Option[CarReviewCanonicalResponse],
  run: CarReviewRun,
  terminalLease: Option[CarReviewProductionTerminalLease]
)
