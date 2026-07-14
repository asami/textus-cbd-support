package org.simplemodeling.textus.cbdsupport.runtime

import java.util.Locale

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
object VersionAvailabilityState {
  val WORKING = "working"
  val LOCAL_PUBLISHED = "local-published"
  val CACHED = "cached"
  val REMOTELY_PUBLISHED = "remotely-published"

  val ALL: Vector[String] = Vector(
    WORKING,
    LOCAL_PUBLISHED,
    CACHED,
    REMOTELY_PUBLISHED
  )
}

object VersionMaturity {
  val SNAPSHOT = "snapshot"
  val RELEASE = "release"
  val UNKNOWN = "unknown"
  val CONFLICTING = "conflicting"

  val ALL: Vector[String] = Vector(SNAPSHOT, RELEASE, UNKNOWN, CONFLICTING)
}

final case class ComponentVersionStateObservation(
  sourceId: String,
  sourceKind: String,
  organization: Option[String],
  componentName: Option[String],
  componentKind: Option[String],
  version: Option[String],
  availabilityState: String,
  maturity: String,
  channel: Option[String],
  status: Option[String],
  artifactChecksumSha256: Option[String],
  evidenceLocation: String,
  diagnostics: Vector[String]
) {
  require(VersionAvailabilityState.ALL.contains(availabilityState), s"Unsupported version availability state: $availabilityState")
  require(VersionMaturity.ALL.contains(maturity), s"Unsupported version maturity: $maturity")
}

final case class VersionStateReconciliationReport(
  observations: Vector[ComponentVersionStateObservation],
  releaseObservations: Vector[ComponentVersionStateObservation],
  snapshotObservations: Vector[ComponentVersionStateObservation],
  unknownObservations: Vector[ComponentVersionStateObservation],
  conflictingObservations: Vector[ComponentVersionStateObservation],
  selectedObservation: Option[ComponentVersionStateObservation]
)

object ComponentVersionStateObservation {
  def fromCatalog(
    profile: ComponentProfile,
    observation: ComponentObservation
  ): Vector[ComponentVersionStateObservation] = {
    val versions = (
      profile.versionEvidence.map(_.version) ++
        profile.versions ++
        profile.latestStable.toVector ++
        profile.latestSnapshot.toVector ++
        profile.selectedVersion.toVector
    ).map(_.trim).filter(_.nonEmpty).distinct
    if (versions.isEmpty)
      Vector(_catalog_observation(profile, observation, None, None))
    else
      versions.map { version =>
        val evidence = profile.versionEvidence.find(_.version == version)
        _catalog_observation(profile, observation, Some(version), evidence)
      }
  }

  def fromLocal(observation: LocalComponentObservation): ComponentVersionStateObservation = {
    val classification = _classify(observation.version, None, None, Vector.empty)
    ComponentVersionStateObservation(
      observation.sourceId,
      observation.sourceKind,
      observation.organization,
      observation.componentName,
      observation.componentKind,
      observation.version,
      observation.versionState,
      classification._1,
      None,
      None,
      observation.artifactChecksumSha256,
      observation.evidenceLocation,
      observation.diagnostics ++ classification._2
    )
  }

  private def _catalog_observation(
    profile: ComponentProfile,
    observation: ComponentObservation,
    version: Option[String],
    evidence: Option[ComponentVersionEvidence]
  ): ComponentVersionStateObservation = {
    val channel = evidence.flatMap(_.channel)
      .orElse(Option.when(version == profile.selectedVersion)(profile.selectedChannel).flatten)
    val status = evidence.flatMap(_.status)
      .orElse(Option.when(version == profile.selectedVersion)(profile.selectedStatus).flatten)
    val declared = Vector(
      Option.when(version.nonEmpty && version == profile.latestStable)(VersionMaturity.RELEASE),
      Option.when(version.nonEmpty && version == profile.latestSnapshot)(VersionMaturity.SNAPSHOT)
    ).flatten
    val classification = _classify(version, channel, status, declared)
    ComponentVersionStateObservation(
      observation.sourceId,
      observation.sourceKind,
      profile.organization,
      Some(profile.name),
      Some(profile.kind),
      version,
      VersionAvailabilityState.REMOTELY_PUBLISHED,
      classification._1,
      channel,
      status,
      evidence.flatMap(_.artifactChecksumSha256)
        .orElse(Option.when(version == profile.selectedVersion)(observation.artifactChecksumSha256).flatten),
      observation.evidenceLocation,
      (profile.warnings ++ observation.diagnostics ++ classification._2).distinct
    )
  }

  private def _classify(
    version: Option[String],
    channel: Option[String],
    status: Option[String],
    declared: Vector[String]
  ): (String, Vector[String]) = {
    val normalizedversion = version.map(_.trim).filter(_.nonEmpty)
    val normalizedchannel = channel.map(_.trim.toLowerCase(Locale.ROOT)).filter(_.nonEmpty)
    val normalizedstatus = status.map(_.trim.toLowerCase(Locale.ROOT)).filter(_.nonEmpty)
    val snapshotsignal = declared.contains(VersionMaturity.SNAPSHOT) ||
      normalizedversion.exists(_.toUpperCase(Locale.ROOT).contains("SNAPSHOT")) ||
      normalizedchannel.exists(Set("snapshot", "development")) ||
      normalizedstatus.contains("snapshot")
    val releasesignal = declared.contains(VersionMaturity.RELEASE) ||
      normalizedversion.exists(_.matches("(?i)^v?[0-9]+(?:\\.[0-9]+){1,3}(?:\\+[0-9A-Za-z.-]+)?$")) ||
      normalizedchannel.exists(Set("stable", "release")) ||
      normalizedstatus.exists(Set("released", "release"))
    if (snapshotsignal && releasesignal)
      VersionMaturity.CONFLICTING -> Vector(
        s"Version maturity evidence conflicts for ${normalizedversion.getOrElse("an unidentified version")}."
      )
    else if (snapshotsignal)
      VersionMaturity.SNAPSHOT -> Vector.empty
    else if (releasesignal)
      VersionMaturity.RELEASE -> Vector.empty
    else
      VersionMaturity.UNKNOWN -> Vector(
        normalizedversion.fold("Version identity is missing; maturity is unknown.")(x => s"Version maturity is unknown for $x.")
      )
  }
}

object VersionStateReconciler {
  def reconcile(observations: Vector[ComponentVersionStateObservation]): VersionStateReconciliationReport =
    VersionStateReconciliationReport(
      observations,
      observations.filter(_.maturity == VersionMaturity.RELEASE),
      observations.filter(_.maturity == VersionMaturity.SNAPSHOT),
      observations.filter(_.maturity == VersionMaturity.UNKNOWN),
      observations.filter(_.maturity == VersionMaturity.CONFLICTING),
      None
    )
}
