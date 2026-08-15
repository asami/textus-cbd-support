package org.simplemodeling.textus.cbdsupport.runtime

import java.util.Locale

import io.circe.Json
import io.circe.parser.parse

/*
 * @since   Jul. 24, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
enum CarReviewQualityProviderAuthority(val value: String) {
  case Deterministic extends CarReviewQualityProviderAuthority("deterministic")
  case Runtime extends CarReviewQualityProviderAuthority("runtime")
  case Advisory extends CarReviewQualityProviderAuthority("advisory")
}

/** Explicit authority, finite cost, and redaction policy for one provider run. */
final case class CarReviewQualityProviderPolicy(
  authority: CarReviewQualityProviderAuthority,
  declaredCostUnits: Long,
  maximumCostUnits: Long
)

/**
 * CBD-owned quality-provider admission layer. It composes the strict v1 bundle
 * admission with authority and redaction checks; callers must successfully
 * preflight this policy before starting a cost-bearing provider.
 */
object CarReviewQualityProviderAdmission {
  private val _forbidden_fact_compact_keys = Set(
    "secret", "credential", "password", "apikey", "authorization", "authorizationheader",
    "rawrequest", "rawresponse", "endpoint", "url"
  )
  private val _forbidden_fact_tokens = Set("secret", "credential", "password", "authorization", "endpoint", "url")
  private val _forbidden_fact_token_pairs = Set(
    "api" -> "key", "raw" -> "request", "raw" -> "response", "authorization" -> "header"
  )

  def preflight(
    provider: ReviewProviderIdentity,
    policy: CarReviewQualityProviderPolicy
  ): Either[ProviderBundleUnknown, Unit] =
    if policy.declaredCostUnits < 0 || policy.maximumCostUnits < 0 then Left(_unknown(provider, "provider-cost-invalid", retryable = false))
    else if policy.declaredCostUnits > policy.maximumCostUnits then Left(_unknown(provider, "provider-cost-limit-exceeded", retryable = true))
    else Right(())

  def admit(
    context: ProviderBundleAdmissionContext,
    policy: CarReviewQualityProviderPolicy
  ): ProviderBundleAdmissionOutcome = {
    val provider = CarReviewProviderBundleAdmission.describeDescriptor(context.descriptor).toOption.map(_.provider)
    provider match {
      case Some(value) =>
        preflight(value, policy) match {
          case Left(unknown) => ProviderBundleAdmissionOutcome.Refused(unknown)
          case Right(_) =>
            CarReviewProviderBundleAdmission.admit(context) match {
              case admitted @ ProviderBundleAdmissionOutcome.Admitted(_) =>
                _validate(context.bundle, value, policy) match {
                  case Right(_) => admitted
                  case Left(code) => ProviderBundleAdmissionOutcome.Refused(_unknown(value, code, retryable = false))
                }
              case refused => refused
            }
        }
      case None => CarReviewProviderBundleAdmission.admit(context)
    }
  }

  private def _validate(bundle: String, provider: ReviewProviderIdentity, policy: CarReviewQualityProviderPolicy): Either[String, Unit] =
    for {
      json <- parse(bundle).left.map(_ => "quality-provider-bundle-json-invalid")
      evidence <- _array(json, "evidence")
      observations <- _array(json, "observations")
      _ <- Either.cond(!evidence.exists(_has_forbidden_fact_key), (), "quality-provider-evidence-redaction-required")
      _ <- policy.authority match {
        case CarReviewQualityProviderAuthority.Deterministic => _deterministic(observations, evidence)
        case CarReviewQualityProviderAuthority.Runtime => _runtime(observations, evidence)
        case CarReviewQualityProviderAuthority.Advisory => _advisory(observations)
      }
    } yield ()

  private def _deterministic(observations: Vector[Json], evidence: Vector[Json]): Either[String, Unit] =
    Either.cond(
      !observations.exists(value => _string(value, "ruleId").exists(_.startsWith("ai.advisory."))) &&
        !evidence.exists(value => _string(value, "kind").contains(CarReviewRuntimeEvidencePolicy.RuntimeObservationKind)),
      (),
      "deterministic-provider-authority-violation"
    )

  private def _runtime(observations: Vector[Json], evidence: Vector[Json]): Either[String, Unit] = {
    val kinds = evidence.flatMap(value => _string(value, "id").map(_ -> _string(value, "kind").getOrElse(""))).toMap
    val valid = observations.filter(_string(_, "type").contains("assurance")).forall { observation =>
      _strings(observation, "evidenceIds").exists(ids => ids.nonEmpty && ids.forall(id => kinds.get(id).contains(CarReviewRuntimeEvidencePolicy.RuntimeObservationKind)))
    }
    Either.cond(valid, (), "runtime-assurance-evidence-required")
  }

  private def _advisory(observations: Vector[Json]): Either[String, Unit] =
    Either.cond(
      observations.forall(value =>
        !_string(value, "type").contains("assurance") &&
          _string(value, "ruleId").exists(_.startsWith("ai.advisory."))
      ),
      (),
      "advisory-provider-authority-violation"
    )

  private def _has_forbidden_fact_key(value: Json): Boolean =
    value.asObject.exists { fields =>
      fields.toMap.exists { case (key, nested) =>
        _forbidden_fact_key(key) || _has_forbidden_fact_key(nested)
      }
    } || value.asArray.exists(_.exists(_has_forbidden_fact_key))

  private def _forbidden_fact_key(key: String): Boolean = {
    val tokens = key
      .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
      .toLowerCase(Locale.ROOT)
      .split("[^a-z0-9]+")
      .toVector
      .filter(_.nonEmpty)
    val compact = tokens.mkString
    _forbidden_fact_compact_keys.contains(compact) ||
      tokens.exists(_forbidden_fact_tokens.contains) ||
      tokens.sliding(2).exists(pair => pair.length == 2 && _forbidden_fact_token_pairs.contains(pair(0) -> pair(1)))
  }

  private def _array(value: Json, field: String): Either[String, Vector[Json]] =
    value.hcursor.downField(field).focus.flatMap(_.asArray).map(_.toVector).toRight(s"quality-provider-$field-invalid")

  private def _string(value: Json, field: String): Option[String] = value.hcursor.get[String](field).toOption
  private def _strings(value: Json, field: String): Option[Vector[String]] = value.hcursor.get[Vector[String]](field).toOption

  private def _unknown(provider: ReviewProviderIdentity, code: String, retryable: Boolean): ProviderBundleUnknown =
    ProviderBundleUnknown(
      Some(provider),
      ReviewProviderState("incompatible"),
      ReviewLimitation(code, ReviewLimitationScope("provider"), Some(provider.id.value), s"Quality provider was not admitted: $code.", retryable),
      runFailure = false
    )
}
