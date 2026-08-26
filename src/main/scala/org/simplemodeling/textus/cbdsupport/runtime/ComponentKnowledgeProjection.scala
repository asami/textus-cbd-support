package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.util.Locale

import org.goldenport.cncf.component.repository.{
  ComponentResourceAuthorization,
  ComponentResourceAvailability,
  ComponentResourceIntegrity,
  ComponentResourceSourceKind
}
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeCarrier,
  ComponentKnowledgeManifestConsumerContract,
  ComponentKnowledgeManifestConsumerResourceEvidence
}

/**
 * Value-only CBD projection of an admitted Component knowledge contract.
 *
 * The carrier has already established exact Component/release/digest
 * admission.  This layer neither reads an archive resource nor converts a
 * manifest membership record into disclosure, operation, MCP, or runtime
 * authority.
 */
final case class ComponentKnowledgeResourceDetail(
  componentId: String,
  logicalRelease: String,
  parentComponentId: Option[String],
  childRole: String,
  logicalResource: String,
  logicalPath: String,
  kind: String,
  role: String,
  language: Option[String],
  mediaType: String,
  size: Long,
  sha256: String,
  authority: String,
  stability: String,
  source: String,
  license: String,
  disclosure: String,
  availability: String,
  integrity: String,
  authorization: String,
  sourceKind: String,
  artifactCoordinate: String,
  logicalSource: String,
  resolutionStep: String,
  externalDeploymentRequired: Boolean,
  matchingDigest: String
)

final case class ComponentKnowledgeDetail(
  sourceId: String,
  sourceKind: String,
  evidenceLocation: String,
  artifactChecksumSha256: Option[String],
  componentId: String,
  logicalRelease: String,
  carrierSchema: String,
  carrierLogicalPath: String,
  carrierSha256: String,
  resources: Vector[ComponentKnowledgeResourceDetail],
  truncatedResourceCount: Int
)

final case class ComponentKnowledgeBackedProfile(
  observation: LocalComponentObservation,
  contract: ComponentKnowledgeManifestConsumerContract,
  profile: ComponentProfile,
  detail: ComponentKnowledgeDetail
)

object ComponentKnowledgeProjection {
  /** Bounds a single detail/MCP response independently of archive size. */
  val MAXIMUM_RESOURCES = 100
  val KNOWLEDGE_ABSENT = "component-knowledge-absent"
  val KNOWLEDGE_REJECTED = "component-knowledge-rejected"

  def all(inventory: Option[LocalInformationInventory]): Vector[ComponentKnowledgeBackedProfile] =
    inventory.toVector.flatMap(_.observations).flatMap(fromObservation)

  def fromObservation(observation: LocalComponentObservation): Option[ComponentKnowledgeBackedProfile] =
    observation.componentKnowledge match {
      case ComponentKnowledgeIntegration.Admitted(contract, carrier) =>
        val componentid = contract.componentId.name
        val name = observation.componentName.getOrElse(_local_name(componentid))
        val organization = observation.organization.orElse(_organization(componentid, name))
        val resources = contract.resources.map(_resource_detail)
        val truncated = (resources.size - MAXIMUM_RESOURCES).max(0)
        val detail = ComponentKnowledgeDetail(
          observation.sourceId,
          observation.sourceKind,
          observation.evidenceLocation,
          observation.artifactChecksumSha256,
          componentid,
          contract.logicalRelease,
          carrier.carrierSchema,
          carrier.logicalPath,
          carrier.sha256,
          resources.take(MAXIMUM_RESOURCES),
          truncated
        )
        val evidenceuri = URI.create("urn:cncf:cbd:component-knowledge:" +
          carrier.sha256)
        val versionevidence = ComponentVersionEvidence(
          contract.logicalRelease,
          None,
          Vector.empty,
          None,
          None,
          hasDependencyMetadata = false,
          status = Some("admitted"),
          component = Some(componentid),
          artifactChecksumSha256 = observation.artifactChecksumSha256
        )
        val profile = ComponentProfile(
          catalogId = observation.sourceId,
          organization = organization,
          name = name,
          title = componentid,
          summary = Some("Exact Component knowledge is available as admitted value-only evidence."),
          kind = observation.componentKind.getOrElse("car"),
          versions = Vector(contract.logicalRelease),
          selectedVersion = Some(contract.logicalRelease),
          dependencyMetadataVersion = None,
          latestStable = Option.when(!contract.logicalRelease.endsWith("-SNAPSHOT"))(contract.logicalRelease),
          latestSnapshot = Option.when(contract.logicalRelease.endsWith("-SNAPSHOT"))(contract.logicalRelease),
          runtimeMinimum = None,
          tags = detail.resources.map(_.kind).distinct.sorted,
          terms = (Vector(componentid, name) ++ detail.resources.map(_.logicalPath)).distinct.sorted,
          dependencies = Vector.empty,
          artifactUri = None,
          evidenceUri = evidenceuri,
          modelMetadataUri = None,
          documentationUri = None,
          versionEvidence = Vector(versionevidence),
          warnings = observation.diagnostics ++
            Option.when(truncated > 0)(s"Component knowledge resources were truncated at $MAXIMUM_RESOURCES of ${resources.size}.").toVector :+
            "Component knowledge is descriptive value-only evidence; it grants no runtime authority.",
          selectedStatus = Some("admitted"),
          selectedComponent = Some(componentid),
          artifactChecksumSha256 = observation.artifactChecksumSha256
        )
        Some(ComponentKnowledgeBackedProfile(observation, contract, profile, detail))
      case _ => None
    }

  /**
   * Build the same value-only detail for a catalog-selected profile.  The
   * caller already admitted the version-scoped endpoint against the carrier;
   * this projection receives no archive, raw contract bytes, resolver result,
   * or authority.
   */
  def fromCatalog(
    profile: ComponentProfile,
    carrier: ComponentKnowledgeCarrier,
    contract: ComponentKnowledgeManifestConsumerContract,
    evidenceLocation: String
  ): ComponentKnowledgeBackedProfile = {
    val resources = contract.resources.map(_resource_detail)
    val truncated = (resources.size - MAXIMUM_RESOURCES).max(0)
    val detail = ComponentKnowledgeDetail(
      profile.catalogId,
      InformationSourceKind.PUBLISHED_CATALOG,
      evidenceLocation,
      profile.artifactChecksumSha256,
      contract.componentId.name,
      contract.logicalRelease,
      carrier.carrierSchema,
      carrier.logicalPath,
      carrier.sha256,
      resources.take(MAXIMUM_RESOURCES),
      truncated
    )
    val observation = LocalComponentObservation(
      profile.catalogId,
      InformationSourceKind.PUBLISHED_CATALOG,
      Some(profile.name),
      profile.organization,
      Some(profile.kind),
      profile.selectedVersion,
      "catalog-declared",
      "catalog-declared",
      evidenceLocation,
      None,
      None,
      profile.artifactChecksumSha256,
      Vector.empty,
      ComponentKnowledgeIntegration.Admitted(contract, carrier)
    )
    ComponentKnowledgeBackedProfile(observation, contract, profile, detail)
  }

  def matches(
    value: ComponentKnowledgeBackedProfile,
    name: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    sourceId: Option[String]
  ): Boolean = {
    val profile = value.profile
    val requested = _normalize(name)
    (profile.name.equalsIgnoreCase(name.trim) || value.contract.componentId.name.equalsIgnoreCase(name.trim)) &&
      organization.forall(x => profile.organization.exists(_.equalsIgnoreCase(x))) &&
      kind.forall(_.equalsIgnoreCase(profile.kind)) &&
      version.forall(_ == value.contract.logicalRelease) &&
      sourceId.forall(_ == profile.catalogId) &&
      requested.nonEmpty
  }

  def missingAbsences(
    inventory: Option[LocalInformationInventory],
    name: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    sourceId: Option[String]
  ): Vector[ComponentEvidenceAbsence] =
    inventory.toVector.flatMap(_.observations).flatMap { observation =>
      if (!_matches_observation(observation, name, organization, kind, version, sourceId)) Vector.empty
      else observation.componentKnowledge match {
        case ComponentKnowledgeIntegration.Absent => Vector(ComponentEvidenceAbsence(
          KNOWLEDGE_ABSENT,
          "component-detail",
          "The exact local Component observation declares no Component knowledge carrier.",
          Vector(observation.sourceId),
          observation.version.toVector,
          Vector.empty
        ))
        case ComponentKnowledgeIntegration.Rejected(_) => Vector(ComponentEvidenceAbsence(
          KNOWLEDGE_REJECTED,
          "component-detail",
          "The exact local Component knowledge carrier was rejected before consumer projection.",
          Vector(observation.sourceId),
          observation.version.toVector,
          Vector.empty
        ))
        case ComponentKnowledgeIntegration.Admitted(_, _) => Vector.empty
      }
    }.distinct

  /**
   * Preserve a selected catalog profile while making missing or rejected
   * carrier evidence explicit to read-only consumers. This does not turn the
   * carrier result into a second selection path or expose its raw content.
   */
  def catalogAbsence(
    profile: ComponentProfile,
    result: ComponentKnowledgeIntegration.Result
  ): Option[ComponentEvidenceAbsence] =
    result match {
      case ComponentKnowledgeIntegration.Admitted(_, _) => None
      case ComponentKnowledgeIntegration.Absent => Some(ComponentEvidenceAbsence(
        KNOWLEDGE_ABSENT,
        "component-detail",
        "The selected published catalog Component version declares no Component knowledge carrier.",
        Vector(profile.catalogId),
        profile.selectedVersion.toVector,
        Vector.empty
      ))
      case ComponentKnowledgeIntegration.Rejected(reason) => Some(ComponentEvidenceAbsence(
        KNOWLEDGE_REJECTED,
        "component-detail",
        InformationSourceDiagnosticPolicy.sanitize(
          s"The selected published catalog Component knowledge carrier was rejected: $reason"
        ),
        Vector(profile.catalogId),
        profile.selectedVersion.toVector,
        Vector.empty
      ))
    }

  def usage(value: ComponentKnowledgeBackedProfile, intent: Option[String]): ComponentUsage = {
    val resources = value.detail.resources
    val visible = resources.filter(_is_reference_visible)
    val withheld = resources.filterNot(_is_reference_visible)
    val references = visible.flatMap { resource =>
      _logical_uri(resource.logicalResource).map(uri => ("component-knowledge", uri, true))
    }
    val guidance = visible.map { resource =>
      ComponentUsageGuidance(
        "declared-resource",
        intent,
        s"Exact ${resource.kind} evidence is admitted at logical path ${resource.logicalPath}; only its value metadata is available.",
        value.detail.sourceId,
        value.detail.sourceKind,
        Some(value.detail.logicalRelease),
        None,
        None,
        None,
        _logical_uri(resource.logicalResource).toVector,
        "The carrier-admitted consumer contract identifies this resource; CBD did not read resource content."
      )
    }
    val absences = withheld.map { resource =>
      ComponentEvidenceAbsence(
        "component-knowledge-reference-withheld",
        resource.logicalPath,
        "The admitted resource is not referenceable because its availability or authorization state is not usable.",
        Vector(value.detail.sourceId),
        Vector(value.detail.logicalRelease),
        Vector.empty
      )
    }
    ComponentUsage(
      value.profile,
      Vector.empty,
      references,
      Option.when(resources.isEmpty)("No Component knowledge resources were admitted.").toVector ++
        Option.when(withheld.nonEmpty)("Some Component knowledge references were withheld by admitted availability or authorization evidence.").toVector ++
        Vector("Component knowledge is descriptive value-only evidence and does not define executable operations."),
      intent,
      Some(value.detail.sourceId),
      Some(value.detail.sourceKind),
      Some(value.detail.logicalRelease),
      guidance,
      absences
    )
  }

  private def _resource_detail(value: ComponentKnowledgeManifestConsumerResourceEvidence): ComponentKnowledgeResourceDetail = {
    val identity = value.logicalIdentity
    val provenance = value.provenance
    ComponentKnowledgeResourceDetail(
      identity.componentId.name,
      identity.logicalRelease,
      identity.parentComponentId.map(_.name),
      identity.childRole,
      identity.logicalResource,
      value.logicalPath,
      value.kind.code,
      value.role.code,
      value.language,
      value.mediaType.value,
      value.size,
      value.sha256,
      value.metadata.authority.code,
      value.metadata.stability.code,
      value.metadata.source.code,
      value.metadata.license,
      value.metadata.disclosure.code,
      _availability(value.availability),
      _integrity(value.integrity),
      _authorization(value.authorization),
      _source_kind(value.provenance.sourceKind),
      provenance.artifactCoordinate,
      provenance.logicalSource,
      provenance.resolutionStep,
      provenance.externalDeploymentRequired,
      provenance.matchingDigest
    )
  }

  private def _matches_observation(
    observation: LocalComponentObservation,
    name: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    sourceId: Option[String]
  ): Boolean =
    observation.componentName.exists(_.equalsIgnoreCase(name.trim)) &&
      organization.forall(x => observation.organization.exists(_.equalsIgnoreCase(x))) &&
      kind.forall(x => observation.componentKind.exists(_.equalsIgnoreCase(x))) &&
      version.forall(x => observation.version.contains(x)) &&
      sourceId.forall(_ == observation.sourceId)

  private def _is_reference_visible(value: ComponentKnowledgeResourceDetail): Boolean =
    value.availability == "available" && value.authorization == "granted"

  private def _logical_uri(value: String): Option[URI] =
    try Some(URI.create(value))
    catch { case _: IllegalArgumentException => None }

  private def _local_name(componentId: String): String =
    componentId.split("\\.").lastOption.getOrElse(componentId)

  private def _organization(componentId: String, name: String): Option[String] =
    Option(componentId.stripSuffix("." + name)).filter(_.contains("."))

  private def _normalize(value: String): String = value.trim.toLowerCase(Locale.ROOT)

  private def _availability(value: ComponentResourceAvailability): String = value match {
    case ComponentResourceAvailability.Available => "available"
    case ComponentResourceAvailability.Restricted => "restricted"
    case ComponentResourceAvailability.Unavailable => "unavailable"
    case ComponentResourceAvailability.Missing => "missing"
    case ComponentResourceAvailability.Stale => "stale"
    case ComponentResourceAvailability.Incompatible => "incompatible"
    case ComponentResourceAvailability.Corrupt => "corrupt"
  }

  private def _integrity(value: ComponentResourceIntegrity): String = value match {
    case ComponentResourceIntegrity.NotEvaluated => "not-evaluated"
    case ComponentResourceIntegrity.Verified => "verified"
    case ComponentResourceIntegrity.Unverified => "unverified"
  }

  private def _authorization(value: ComponentResourceAuthorization): String = value match {
    case ComponentResourceAuthorization.NotEvaluated => "not-evaluated"
    case ComponentResourceAuthorization.Granted => "granted"
    case ComponentResourceAuthorization.Denied => "denied"
  }

  private def _source_kind(value: ComponentResourceSourceKind): String = value match {
    case ComponentResourceSourceKind.EmbeddedPrimary => "embedded-primary"
    case ComponentResourceSourceKind.DevelopmentDirectory => "development-directory"
    case ComponentResourceSourceKind.ExpandedCar => "expanded-car"
    case ComponentResourceSourceKind.LocalRepository => "local-repository"
    case ComponentResourceSourceKind.ManagedCache => "managed-cache"
    case ComponentResourceSourceKind.OfflineBundle => "offline-bundle"
    case ComponentResourceSourceKind.RemoteRepository => "remote-repository"
  }
}
