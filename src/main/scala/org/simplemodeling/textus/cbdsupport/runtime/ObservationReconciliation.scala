package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
object ReconciliationPurpose {
  val DEVELOPMENT_WORK = "development-work"
  val LOCAL_EXECUTION = "local-execution"
  val PUBLISHED_REUSE = "published-reuse"
  val ARTIFACT_VERIFICATION = "artifact-verification"

  val ALL: Vector[String] = Vector(
    DEVELOPMENT_WORK,
    LOCAL_EXECUTION,
    PUBLISHED_REUSE,
    ARTIFACT_VERIFICATION
  )
}

object ReconciliationIssueCode {
  val DUPLICATE = "duplicate"
  val MISSING = "missing"
  val STALE = "stale"
  val INCOMPATIBLE = "incompatible"
  val VERSION_CONFLICT = "version-conflict"
  val CHECKSUM_CONFLICT = "checksum-conflict"
}

final case class ReconciliationObservation(
  sourceId: String,
  sourceKind: String,
  organization: Option[String],
  componentName: Option[String],
  componentKind: Option[String],
  version: Option[String],
  versionState: Option[String],
  freshness: String,
  runtimeMinimum: Option[String],
  runtimeMaximum: Option[String],
  artifactChecksumSha256: Option[String],
  evidenceLocation: String,
  diagnostics: Vector[String]
)

final case class ReconciliationPrecedenceTier(
  rank: Int,
  sourceKinds: Vector[String],
  versionStates: Vector[String],
  authority: String
)

final case class ReconciliationIssue(
  code: String,
  message: String,
  sourceIds: Vector[String],
  evidenceLocations: Vector[String]
)

final case class ObservationReconciliationReport(
  purpose: String,
  precedence: Vector[ReconciliationPrecedenceTier],
  observations: Vector[ReconciliationObservation],
  issues: Vector[ReconciliationIssue],
  selectedObservation: Option[ReconciliationObservation]
)

object ReconciliationObservation {
  def fromCatalog(
    profile: ComponentProfile,
    observation: ComponentObservation
  ): ReconciliationObservation =
    ReconciliationObservation(
      observation.sourceId,
      observation.sourceKind,
      profile.organization,
      Some(profile.name),
      Some(profile.kind),
      observation.version,
      Some("remotely-published"),
      observation.freshness,
      profile.runtimeMinimum,
      profile.runtimeMaximum,
      observation.artifactChecksumSha256,
      observation.evidenceLocation,
      observation.diagnostics
    )

  def fromLocal(observation: LocalComponentObservation): ReconciliationObservation =
    ReconciliationObservation(
      observation.sourceId,
      observation.sourceKind,
      observation.organization,
      observation.componentName,
      observation.componentKind,
      observation.version,
      Some(observation.versionState),
      "observed",
      None,
      None,
      observation.artifactChecksumSha256,
      observation.evidenceLocation,
      observation.diagnostics
    )
}

object ObservationReconciler {
  def reconcile(
    observations: Vector[ReconciliationObservation],
    purpose: String,
    requestedversion: Option[String] = None,
    runtimeversion: Option[String] = None
  ): ObservationReconciliationReport = {
    require(ReconciliationPurpose.ALL.contains(purpose), s"Unsupported reconciliation purpose: $purpose")
    val issues = (
      _missing_issues(observations, purpose, runtimeversion) ++
        _duplicate_issues(observations) ++
        _stale_issues(observations) ++
        _incompatible_issues(observations, requestedversion, runtimeversion) ++
        _version_conflict_issues(observations) ++
        _checksum_conflict_issues(observations)
    ).sortBy(issue => (issue.code, issue.message, issue.sourceIds.mkString("\u0000")))
    ObservationReconciliationReport(
      purpose,
      _precedence(purpose),
      observations,
      issues,
      None
    )
  }

  private def _missing_issues(
    observations: Vector[ReconciliationObservation],
    purpose: String,
    runtimeversion: Option[String]
  ): Vector[ReconciliationIssue] =
    observations.flatMap { observation =>
      val missing = Vector(
        Option.when(observation.componentName.isEmpty)("component-name"),
        Option.when(observation.componentKind.isEmpty)("component-kind"),
        Option.when(observation.version.isEmpty)("version"),
        Option.when(
          purpose == ReconciliationPurpose.ARTIFACT_VERIFICATION && observation.artifactChecksumSha256.isEmpty
        )("artifact-checksum"),
        Option.when(
          purpose == ReconciliationPurpose.PUBLISHED_REUSE &&
            runtimeversion.nonEmpty && observation.sourceKind == InformationSourceKind.PUBLISHED_CATALOG &&
            observation.runtimeMinimum.isEmpty
        )("runtime-compatibility")
      ).flatten
      Option.when(missing.nonEmpty)(ReconciliationIssue(
        ReconciliationIssueCode.MISSING,
        s"Observation from ${observation.sourceId} is missing purpose-required evidence: ${missing.mkString(", ")}.",
        Vector(observation.sourceId),
        Vector(observation.evidenceLocation)
      ))
    }

  private def _duplicate_issues(
    observations: Vector[ReconciliationObservation]
  ): Vector[ReconciliationIssue] =
    _identified(observations).groupBy(_identity_version_key).toVector.flatMap { case (key, entries) =>
      Option.when(entries.size > 1)(ReconciliationIssue(
        ReconciliationIssueCode.DUPLICATE,
        s"Multiple observations remain for ${_identity_version_label(key)}.",
        entries.map(_.sourceId).distinct.sorted,
        entries.map(_.evidenceLocation).distinct.sorted
      ))
    }

  private def _stale_issues(
    observations: Vector[ReconciliationObservation]
  ): Vector[ReconciliationIssue] =
    observations.filter(_.freshness == "stale").map { observation =>
      ReconciliationIssue(
        ReconciliationIssueCode.STALE,
        s"Observation from ${observation.sourceId} is stale and remains evidence rather than current fact.",
        Vector(observation.sourceId),
        Vector(observation.evidenceLocation)
      )
    }

  private def _incompatible_issues(
    observations: Vector[ReconciliationObservation],
    requestedversion: Option[String],
    runtimeversion: Option[String]
  ): Vector[ReconciliationIssue] =
    observations.flatMap { observation =>
      val versionmismatch = requestedversion.exists(requested => observation.version.exists(_ != requested))
      val runtimemismatch = runtimeversion.exists { actual =>
        observation.runtimeMinimum.exists(!_version_lte(_, actual)) ||
          observation.runtimeMaximum.exists(!_version_lte(actual, _))
      }
      Option.when(versionmismatch || runtimemismatch)(ReconciliationIssue(
        ReconciliationIssueCode.INCOMPATIBLE,
        s"Observation from ${observation.sourceId} is incompatible with the requested version or runtime constraint.",
        Vector(observation.sourceId),
        Vector(observation.evidenceLocation)
      ))
    }

  private def _version_conflict_issues(
    observations: Vector[ReconciliationObservation]
  ): Vector[ReconciliationIssue] =
    _identified(observations).groupBy(_identity_key).toVector.flatMap { case (key, entries) =>
      val versions = entries.flatMap(_.version).distinct.sorted
      Option.when(versions.size > 1)(ReconciliationIssue(
        ReconciliationIssueCode.VERSION_CONFLICT,
        s"Conflicting versions remain for ${_identity_label(key)}: ${versions.mkString(", ")}.",
        entries.map(_.sourceId).distinct.sorted,
        entries.map(_.evidenceLocation).distinct.sorted
      ))
    }

  private def _checksum_conflict_issues(
    observations: Vector[ReconciliationObservation]
  ): Vector[ReconciliationIssue] =
    _identified(observations).groupBy(_identity_version_key).toVector.flatMap { case (key, entries) =>
      val checksums = entries.flatMap(_.artifactChecksumSha256).distinct.sorted
      Option.when(checksums.size > 1)(ReconciliationIssue(
        ReconciliationIssueCode.CHECKSUM_CONFLICT,
        s"Conflicting artifact checksums remain for ${_identity_version_label(key)}.",
        entries.map(_.sourceId).distinct.sorted,
        entries.map(_.evidenceLocation).distinct.sorted
      ))
    }

  private def _precedence(purpose: String): Vector[ReconciliationPrecedenceTier] =
    purpose match {
      case ReconciliationPurpose.DEVELOPMENT_WORK => Vector(
        ReconciliationPrecedenceTier(1, Vector(InformationSourceKind.DEVELOPMENT_DIRECTORY), Vector("working"), "Current working-state evidence."),
        ReconciliationPrecedenceTier(2, Vector(InformationSourceKind.CAR_STORAGE), Vector("local-published", "cached"), "Locally available artifact evidence."),
        ReconciliationPrecedenceTier(3, Vector(InformationSourceKind.PUBLISHED_CATALOG), Vector("remotely-published"), "Published comparison evidence.")
      )
      case ReconciliationPurpose.LOCAL_EXECUTION => Vector(
        ReconciliationPrecedenceTier(1, Vector(InformationSourceKind.CAR_STORAGE), Vector("local-published"), "Locally published artifact availability."),
        ReconciliationPrecedenceTier(2, Vector(InformationSourceKind.CAR_STORAGE), Vector("cached"), "Cached artifact availability."),
        ReconciliationPrecedenceTier(3, Vector(InformationSourceKind.DEVELOPMENT_DIRECTORY, InformationSourceKind.PUBLISHED_CATALOG), Vector("working", "remotely-published"), "Supporting identity evidence only.")
      )
      case ReconciliationPurpose.PUBLISHED_REUSE => Vector(
        ReconciliationPrecedenceTier(1, Vector(InformationSourceKind.PUBLISHED_CATALOG), Vector("remotely-published"), "Published reuse and compatibility authority."),
        ReconciliationPrecedenceTier(2, Vector(InformationSourceKind.DEVELOPMENT_DIRECTORY, InformationSourceKind.CAR_STORAGE), Vector("working", "local-published", "cached"), "Local comparison evidence, not publication proof.")
      )
      case ReconciliationPurpose.ARTIFACT_VERIFICATION => Vector(
        ReconciliationPrecedenceTier(1, Vector(InformationSourceKind.PUBLISHED_CATALOG, InformationSourceKind.DEVELOPMENT_DIRECTORY, InformationSourceKind.CAR_STORAGE), Vector("working", "local-published", "cached", "remotely-published"), "Peer checksum evidence; disagreement has no winner.")
      )
    }

  private def _identified(
    observations: Vector[ReconciliationObservation]
  ): Vector[ReconciliationObservation] =
    observations.filter(x => x.componentName.nonEmpty && x.componentKind.nonEmpty)

  private def _identity_key(observation: ReconciliationObservation): (String, String, String) =
    (
      observation.organization.map(_normalize).getOrElse("?"),
      _normalize(observation.componentName.get),
      _normalize(observation.componentKind.get)
    )

  private def _identity_version_key(observation: ReconciliationObservation): (String, String, String, String) =
    (
      observation.organization.map(_normalize).getOrElse("?"),
      _normalize(observation.componentName.get),
      _normalize(observation.componentKind.get),
      observation.version.getOrElse("?")
    )

  private def _identity_label(key: (String, String, String)): String =
    s"${key._3}:${key._1}:${key._2}"

  private def _identity_version_label(key: (String, String, String, String)): String =
    s"${key._3}:${key._1}:${key._2}@${key._4}"

  private def _normalize(value: String): String =
    value.trim.toLowerCase(java.util.Locale.ROOT)

  private def _version_lte(minimum: String, actual: String): Boolean =
    _version_parts(minimum).zipAll(_version_parts(actual), 0, 0).find { case (lhs, rhs) => lhs != rhs } match {
      case Some((lhs, rhs)) => lhs <= rhs
      case None => true
    }

  private def _version_parts(value: String): Vector[Int] =
    value.takeWhile(x => x.isDigit || x == '.').split("\\.").toVector.filter(_.nonEmpty).map(_.toIntOption.getOrElse(0))
}
