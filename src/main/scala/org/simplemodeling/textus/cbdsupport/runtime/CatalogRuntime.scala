package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.{Clock, Duration, Instant}
import java.util.concurrent.{CountDownLatch, Semaphore}
import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence

/*
 * @since   Jul. 14, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CatalogSource(
  id: String,
  baseUri: URI,
  priority: Int,
  enabled: Boolean,
  sourceKind: String = InformationSourceKind.PUBLISHED_CATALOG,
  authorization: String = InformationSourceAuthorization.EXPLICIT,
  authentication: Option[SourceAuthentication] = None
) {
  def descriptor: InformationSourceDescriptor =
    InformationSourceDescriptor(
      id,
      sourceKind,
      baseUri.toString,
      priority,
      enabled,
      authorization,
      authentication.map(_.scheme).getOrElse(SourceAuthentication.NONE),
      authentication.nonEmpty
    )
}

object InformationSourceKind {
  val PUBLISHED_CATALOG = "published-catalog"
  val BOK_SITE = "bok-site"
  val SIE_BOK = "sie-bok"
  val DEVELOPMENT_DIRECTORY = "development-directory"
  val CAR_STORAGE = "car-storage"

  val ALL: Vector[String] = Vector(
    PUBLISHED_CATALOG,
    BOK_SITE,
    SIE_BOK,
    DEVELOPMENT_DIRECTORY,
    CAR_STORAGE
  )
}

object InformationSourceAuthorization {
  val BUILT_IN = "built-in"
  val EXACT_ORIGIN_ALLOWLIST = "exact-origin-allowlist"
  val EXPLICIT = "explicit"
  val EXPLICIT_PATH_ALLOWLIST = "explicit-path-allowlist"
  val CANONICAL_STORAGE_ROOT = "canonical-storage-root"
  val COMPONENT_ROUTE_ALLOWLIST = "component-route-allowlist"
}

final case class InformationSourceDescriptor(
  id: String,
  sourceKind: String,
  location: String,
  priority: Int,
  enabled: Boolean,
  authorization: String,
  authenticationScheme: String = SourceAuthentication.NONE,
  credentialConfigured: Boolean = false
)

final case class InformationSourceFreshness(
  status: String,
  observedAt: Option[Instant],
  expiresAt: Option[Instant],
  lastRefreshAttemptAt: Option[Instant],
  nextRefreshAttemptAt: Option[Instant]
)

final case class InformationSourceState(
  descriptor: InformationSourceDescriptor,
  status: String,
  observationCount: Int,
  freshness: InformationSourceFreshness,
  diagnostics: Vector[String]
)

final case class CatalogSourceConfiguration(
  sources: Vector[CatalogSource],
  warnings: Vector[String]
)

private[runtime] object CatalogUriPolicy {
  def origin(uri: URI): String = {
    val scheme = uri.getScheme.toLowerCase(java.util.Locale.ROOT)
    val host = uri.getHost.toLowerCase(java.util.Locale.ROOT)
    val normalizedport = (scheme, uri.getPort) match {
      case ("http", 80) => -1
      case ("https", 443) => -1
      case (_, port) => port
    }
    new URI(scheme, null, host, normalizedport, null, null, null).toString
  }

  def sameOrigin(left: URI, right: URI): Boolean =
    left.getScheme != null && left.getHost != null &&
      right.getScheme != null && right.getHost != null &&
      origin(left) == origin(right)

  def isAuthorizedFetch(base: URI, candidate: URI): Boolean =
    candidate.getUserInfo == null && sameOrigin(base, candidate)
}

final case class InformationSourceRefreshPolicy(
  interval: Duration,
  retryInitialInterval: Duration,
  retryMaximumInterval: Duration,
  maxConcurrentRefreshes: Int
) {
  require(
    interval.compareTo(InformationSourceRefreshPolicy.MINIMUM_INTERVAL) >= 0,
    "Information-source refresh interval must be at least one minute."
  )
  require(
    interval.compareTo(InformationSourceRefreshPolicy.MAXIMUM_INTERVAL) <= 0,
    "Information-source refresh interval must not exceed 24 hours."
  )
  require(
    retryInitialInterval.compareTo(InformationSourceRefreshPolicy.MINIMUM_RETRY_INTERVAL) >= 0,
    "Information-source initial retry interval must be at least one minute."
  )
  require(
    retryInitialInterval.compareTo(retryMaximumInterval) <= 0,
    "Information-source initial retry interval must not exceed its maximum."
  )
  require(
    retryMaximumInterval.compareTo(interval) <= 0,
    "Information-source maximum retry interval must not exceed the normal refresh interval."
  )
  require(
    maxConcurrentRefreshes >= InformationSourceRefreshPolicy.MINIMUM_CONCURRENT_REFRESHES &&
      maxConcurrentRefreshes <= InformationSourceRefreshPolicy.MAXIMUM_CONCURRENT_REFRESHES,
    "Information-source concurrent refresh limit must be from one through eight."
  )
}

object InformationSourceRefreshPolicy {
  val MINIMUM_INTERVAL: Duration = Duration.ofMinutes(1)
  val MAXIMUM_INTERVAL: Duration = Duration.ofHours(24)
  val DEFAULT_INTERVAL: Duration = Duration.ofMinutes(15)
  val MINIMUM_RETRY_INTERVAL: Duration = Duration.ofMinutes(1)
  val DEFAULT_RETRY_INITIAL_INTERVAL: Duration = Duration.ofMinutes(1)
  val DEFAULT_RETRY_MAXIMUM_INTERVAL: Duration = Duration.ofMinutes(15)
  val MINIMUM_CONCURRENT_REFRESHES: Int = 1
  val MAXIMUM_CONCURRENT_REFRESHES: Int = 8
  val DEFAULT_MAX_CONCURRENT_REFRESHES: Int = 2
  val MAXIMUM_CONSECUTIVE_FAILURES: Int = 30

  def apply(interval: Duration): InformationSourceRefreshPolicy =
    InformationSourceRefreshPolicy(
      interval,
      DEFAULT_RETRY_INITIAL_INTERVAL,
      interval,
      DEFAULT_MAX_CONCURRENT_REFRESHES
    )

  val DEFAULT: InformationSourceRefreshPolicy = InformationSourceRefreshPolicy(
    DEFAULT_INTERVAL,
    DEFAULT_RETRY_INITIAL_INTERVAL,
    DEFAULT_RETRY_MAXIMUM_INTERVAL,
    DEFAULT_MAX_CONCURRENT_REFRESHES
  )
}

final case class CatalogCachePolicy(
  ttl: Duration,
  refreshPolicy: InformationSourceRefreshPolicy
) {
  require(!ttl.isZero && !ttl.isNegative, "Catalog cache TTL must be positive.")
  require(ttl.compareTo(CatalogCachePolicy.MAXIMUM_TTL) <= 0, "Catalog cache TTL must not exceed 24 hours.")
  require(
    refreshPolicy.interval.compareTo(ttl) <= 0,
    "Catalog refresh interval must not exceed the cache TTL."
  )
}

object CatalogCachePolicy {
  val DEFAULT_TTL: Duration = Duration.ofMinutes(15)
  val MAXIMUM_TTL: Duration = Duration.ofHours(24)

  def apply(ttl: Duration): CatalogCachePolicy =
    CatalogCachePolicy(ttl, InformationSourceRefreshPolicy(ttl))

  val DEFAULT: CatalogCachePolicy = CatalogCachePolicy(DEFAULT_TTL, InformationSourceRefreshPolicy.DEFAULT)
}

final case class CatalogInspectionPolicy(
  maxConfiguredSources: Int = 15,
  maxAllowedOrigins: Int = 32,
  maxProfiles: Int = 10000,
  maxIndexBytes: Int = 8 * 1024 * 1024,
  maxMetadataBytes: Int = 2 * 1024 * 1024
) {
  require(maxConfiguredSources > 0, "Catalog source limit must be positive.")
  require(maxAllowedOrigins > 0, "Catalog allowed-origin limit must be positive.")
  require(maxProfiles > 0, "Catalog profile limit must be positive.")
  require(maxIndexBytes > 0, "Catalog index byte limit must be positive.")
  require(maxMetadataBytes > 0, "Catalog metadata byte limit must be positive.")
}

object CatalogInspectionPolicy {
  val DEFAULT: CatalogInspectionPolicy = CatalogInspectionPolicy()
}

final case class InformationSourceRetentionPolicy(
  maxSources: Int = InformationSourceRetentionPolicy.MAXIMUM_SOURCES,
  maxCatalogObservations: Int = InformationSourceRetentionPolicy.MAXIMUM_CATALOG_OBSERVATIONS,
  maxBokObservations: Int = InformationSourceRetentionPolicy.MAXIMUM_BOK_OBSERVATIONS,
  maxSieBokObservations: Int = InformationSourceRetentionPolicy.MAXIMUM_SIE_BOK_OBSERVATIONS,
  maxLocalObservations: Int = InformationSourceRetentionPolicy.MAXIMUM_LOCAL_OBSERVATIONS
) {
  require(maxSources > 0, "Retained source limit must be positive.")
  require(
    maxSources <= InformationSourceRetentionPolicy.MAXIMUM_SOURCES,
    "Retained source limit must not exceed 64."
  )
  require(maxCatalogObservations > 0, "Retained catalog observation limit must be positive.")
  require(
    maxCatalogObservations <= InformationSourceRetentionPolicy.MAXIMUM_CATALOG_OBSERVATIONS,
    "Retained catalog observation limit must not exceed 20000."
  )
  require(maxBokObservations > 0, "Retained BoK observation limit must be positive.")
  require(
    maxBokObservations <= InformationSourceRetentionPolicy.MAXIMUM_BOK_OBSERVATIONS,
    "Retained BoK observation limit must not exceed 20000."
  )
  require(maxSieBokObservations > 0, "Retained SIE BoK observation limit must be positive.")
  require(
    maxSieBokObservations <= InformationSourceRetentionPolicy.MAXIMUM_SIE_BOK_OBSERVATIONS,
    "Retained SIE BoK observation limit must not exceed 800."
  )
  require(maxLocalObservations > 0, "Retained local observation limit must be positive.")
  require(
    maxLocalObservations <= InformationSourceRetentionPolicy.MAXIMUM_LOCAL_OBSERVATIONS,
    "Retained local observation limit must not exceed 512."
  )
}

object InformationSourceRetentionPolicy {
  val MAXIMUM_SOURCES = 64
  val MAXIMUM_CATALOG_OBSERVATIONS = 20000
  val MAXIMUM_BOK_OBSERVATIONS = 20000
  val MAXIMUM_SIE_BOK_OBSERVATIONS = 800
  val MAXIMUM_LOCAL_OBSERVATIONS = 512
  val DEFAULT: InformationSourceRetentionPolicy = InformationSourceRetentionPolicy()
}

final case class ComponentDependency(
  name: String,
  version: Option[String],
  kind: Option[String]
)

final case class ResolvedComponentDependency(
  dependency: ComponentDependency,
  status: String,
  depth: Int,
  path: String,
  resolvedProfile: Option[ComponentProfile]
)

final case class ComponentDependencyConflict(
  name: String,
  kind: Option[String],
  versions: Vector[String],
  paths: Vector[String],
  message: String
)

final case class ComponentDependencyResolution(
  directDependencies: Vector[ComponentDependency],
  resolutions: Vector[ResolvedComponentDependency],
  conflicts: Vector[ComponentDependencyConflict],
  warnings: Vector[String],
  absences: Vector[ComponentEvidenceAbsence] = Vector.empty
)

final case class ComponentOperation(
  service: Option[String],
  operation: String,
  kind: Option[String],
  description: Option[String]
)

final case class ComponentVersionEvidence(
  version: String,
  runtimeMinimum: Option[String],
  dependencies: Vector[ComponentDependency],
  artifactUri: Option[URI],
  modelMetadataUri: Option[URI],
  hasDependencyMetadata: Boolean,
  channel: Option[String] = None,
  status: Option[String] = None,
  component: Option[String] = None,
  publishedAt: Option[String] = None,
  runtimeMaximum: Option[String] = None,
  runtimeTested: Vector[String] = Vector.empty,
  artifactChecksumSha256: Option[String] = None
)

final case class ComponentProfile(
  catalogId: String,
  organization: Option[String],
  name: String,
  title: String,
  summary: Option[String],
  kind: String,
  versions: Vector[String],
  selectedVersion: Option[String],
  dependencyMetadataVersion: Option[String],
  latestStable: Option[String],
  latestSnapshot: Option[String],
  runtimeMinimum: Option[String],
  tags: Vector[String],
  terms: Vector[String],
  dependencies: Vector[ComponentDependency],
  artifactUri: Option[URI],
  evidenceUri: URI,
  modelMetadataUri: Option[URI],
  documentationUri: Option[URI],
  versionEvidence: Vector[ComponentVersionEvidence],
  warnings: Vector[String],
  selectedChannel: Option[String] = None,
  selectedStatus: Option[String] = None,
  selectedComponent: Option[String] = None,
  selectedPublishedAt: Option[String] = None,
  runtimeMaximum: Option[String] = None,
  runtimeTested: Vector[String] = Vector.empty,
  artifactChecksumSha256: Option[String] = None,
  observationContext: Option[ComponentObservationContext] = None
) {
  private def _version_neutral_warnings: Vector[String] =
    warnings.filterNot { warning =>
      warning.startsWith("Selected version ") ||
      warning == "Catalog entry does not publish an artifact path for the selected version."
    }

  def identity: String =
    organization.filter(_.nonEmpty).map(x => s"$x:$name").getOrElse(name)

  def selectVersion(version: String): ComponentProfile = {
    val requested = version.trim
    versionEvidence.find(_.version == requested) match {
      case Some(evidence) =>
        copy(
          selectedVersion = Some(requested),
          dependencyMetadataVersion = Option.when(evidence.hasDependencyMetadata)(requested),
          runtimeMinimum = evidence.runtimeMinimum,
          runtimeMaximum = evidence.runtimeMaximum,
          runtimeTested = evidence.runtimeTested,
          dependencies = evidence.dependencies,
          artifactUri = evidence.artifactUri,
          modelMetadataUri = evidence.modelMetadataUri,
          selectedChannel = evidence.channel,
          selectedStatus = evidence.status,
          selectedComponent = evidence.component,
          selectedPublishedAt = evidence.publishedAt,
          artifactChecksumSha256 = evidence.artifactChecksumSha256,
          warnings = _version_neutral_warnings ++
            Option.when(evidence.artifactUri.isEmpty)(s"Selected version $requested does not publish an artifact path.")
        )
      case None =>
        copy(
          selectedVersion = Some(requested),
          dependencyMetadataVersion = None,
          runtimeMinimum = None,
          runtimeMaximum = None,
          runtimeTested = Vector.empty,
          dependencies = Vector.empty,
          artifactUri = None,
          modelMetadataUri = None,
          selectedChannel = None,
          selectedStatus = None,
          selectedComponent = None,
          selectedPublishedAt = None,
          artifactChecksumSha256 = None,
          warnings = _version_neutral_warnings :+
            s"Selected version $requested is listed without version-specific metadata."
        )
    }
  }
}

final case class CatalogSnapshot(
  source: CatalogSource,
  profiles: Vector[ComponentProfile],
  refreshedAt: Instant,
  warning: Option[String]
)

final case class CatalogSourceState(
  source: CatalogSource,
  status: String,
  componentCount: Int,
  refreshedAt: Option[Instant],
  expiresAt: Option[Instant],
  lastRefreshAttemptAt: Option[Instant],
  nextRefreshAttemptAt: Option[Instant],
  cacheStatus: String,
  warning: Option[String]
) {
  def informationSourceState: InformationSourceState =
    InformationSourceState(
      source.descriptor,
      status,
      componentCount,
      InformationSourceFreshness(
        cacheStatus,
        refreshedAt,
        expiresAt,
        lastRefreshAttemptAt,
        nextRefreshAttemptAt
      ),
      warning.toVector
    )
}

final case class ComponentObservation(
  sourceId: String,
  sourceKind: String,
  evidenceLocation: String,
  version: Option[String],
  freshness: String,
  observedAt: Option[Instant],
  expiresAt: Option[Instant],
  artifactChecksumSha256: Option[String],
  diagnostics: Vector[String]
)

final case class ComponentObservationContext(
  sourceId: String,
  sourceKind: String,
  observedAt: Instant,
  expiresAt: Instant,
  diagnostics: Vector[String]
)

final case class ComponentMatch(
  profile: ComponentProfile,
  matchKind: String,
  score: Double,
  rationale: String,
  semanticEvidenceIds: Vector[String] = Vector.empty
)

final case class ComponentUsageGuidance(
  statementKind: String,
  intent: Option[String],
  statement: String,
  sourceId: String,
  sourceKind: String,
  version: Option[String],
  service: Option[String],
  operation: Option[String],
  score: Option[Double],
  evidenceUris: Vector[URI],
  rationale: String
)

final case class ComponentUsage(
  profile: ComponentProfile,
  operations: Vector[ComponentOperation],
  references: Vector[(String, URI, Boolean)],
  warnings: Vector[String],
  intent: Option[String] = None,
  selectedSourceId: Option[String] = None,
  selectedSourceKind: Option[String] = None,
  selectedVersion: Option[String] = None,
  guidance: Vector[ComponentUsageGuidance] = Vector.empty,
  absences: Vector[ComponentEvidenceAbsence] = Vector.empty
)

trait CatalogFetcher {
  def get(uri: URI): Consequence[String]

  def get(uri: URI, maxbytes: Int): Consequence[String] =
    get(uri).flatMap { body =>
      if (body.getBytes(StandardCharsets.UTF_8).length <= maxbytes) Consequence.success(body)
      else Consequence.serviceUnavailable(s"Response exceeds $maxbytes bytes: ${InformationSourceDiagnosticPolicy.renderUri(uri)}")
    }

  def get(source: CatalogSource, uri: URI, maxbytes: Int): Consequence[String] =
    get(uri, maxbytes)
}

trait ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot]

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage]

  def readUsage(
    source: CatalogSource,
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    readUsage(profile, fetcher)
}

private[runtime] sealed trait CozyCatalogReadDecision

private[runtime] object CozyCatalogReadDecision {
  final case class Accepted(snapshot: CatalogSnapshot) extends CozyCatalogReadDecision
  final case class Unavailable(diagnostic: String) extends CozyCatalogReadDecision
  final case class Incompatible(diagnostic: String) extends CozyCatalogReadDecision
}

final class CompatibleComponentCatalogProvider(
  cozy: CozyComponentCatalogProvider,
  publication: SimpleModelingPublicationCatalogProvider
) extends ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
    cozy.compatibilityDecision(source, fetcher) match {
      case CozyCatalogReadDecision.Accepted(snapshot) => Consequence.success(snapshot)
      case CozyCatalogReadDecision.Incompatible(diagnostic) =>
        Consequence.serviceUnavailable(diagnostic)
      case CozyCatalogReadDecision.Unavailable(cozydiagnostic) =>
        publication.read(source, fetcher) match {
          case success: Consequence.Success[CatalogSnapshot] => success
          case Consequence.Failure(publicationfailure) =>
            Consequence.serviceUnavailable(
              s"Cozy repository catalog unavailable: $cozydiagnostic; publication catalog unavailable: ${publicationfailure.display}"
            )
        }
    }

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    if (profile.modelMetadataUri.nonEmpty) cozy.readUsage(profile, fetcher)
    else publication.readUsage(profile, fetcher)

  override def readUsage(
    source: CatalogSource,
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    if (profile.modelMetadataUri.nonEmpty) cozy.readUsage(source, profile, fetcher)
    else publication.readUsage(source, profile, fetcher)
}

final class CozyComponentCatalogProvider(
  policy: CatalogInspectionPolicy = CatalogInspectionPolicy.DEFAULT,
  clock: Clock
) extends ComponentCatalogProvider {
  private final case class ParsedIndex(
    profiles: Vector[ComponentProfile],
    warnings: Vector[String]
  )

  private final case class IndexDocumentRead(
    index: Option[ParsedIndex],
    unavailable: Option[String],
    incompatible: Option[String]
  )

  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
    compatibilityDecision(source, fetcher) match {
      case CozyCatalogReadDecision.Accepted(snapshot) => Consequence.success(snapshot)
      case CozyCatalogReadDecision.Unavailable(diagnostic) => Consequence.serviceUnavailable(diagnostic)
      case CozyCatalogReadDecision.Incompatible(diagnostic) => Consequence.serviceUnavailable(diagnostic)
    }

  private[runtime] def compatibilityDecision(
    source: CatalogSource,
    fetcher: CatalogFetcher
  ): CozyCatalogReadDecision = {
    val documents = Vector("car", "sar").map { kind =>
      val uri = source.baseUri.resolve(s"metadata/repository/$kind/index.json")
      fetcher.get(source, uri, policy.maxIndexBytes) match {
        case Consequence.Success(body) =>
          _parse_index(source, kind, uri, body) match {
            case Consequence.Success(index) => IndexDocumentRead(Some(index), None, None)
            case Consequence.Failure(conclusion) => IndexDocumentRead(
              None,
              None,
              Some(InformationSourceDiagnosticPolicy.sanitize(conclusion.display))
            )
          }
        case Consequence.Failure(conclusion) => IndexDocumentRead(
          None,
          Some(InformationSourceDiagnosticPolicy.sanitize(conclusion.display)),
          None
        )
      }
    }
    val incompatiblediagnostics = documents.flatMap(_.incompatible)
    val unavailabilitydiagnostics = documents.flatMap(_.unavailable)
    val indexes = documents.flatMap(_.index)
    if (incompatiblediagnostics.nonEmpty)
      CozyCatalogReadDecision.Incompatible(
        s"Cozy repository catalog is incompatible and publication fallback was not attempted: ${incompatiblediagnostics.mkString("; ")}"
      )
    else if (indexes.nonEmpty) {
      val discoveredprofiles = indexes.flatMap(_.profiles)
      val overflowwarning = Option.when(discoveredprofiles.size > policy.maxProfiles) {
        s"Catalog profiles were truncated at ${policy.maxProfiles} entries."
      }.toVector
      val warnings = unavailabilitydiagnostics ++ indexes.flatMap(_.warnings) ++ overflowwarning
      CozyCatalogReadDecision.Accepted(CatalogSnapshot(
        source,
        discoveredprofiles.take(policy.maxProfiles).sortBy(x => (x.kind, x.name, x.catalogId)),
        clock.instant(),
        if (warnings.nonEmpty) Some(warnings.mkString("; ")) else None
      ))
    }
    else CozyCatalogReadDecision.Unavailable(unavailabilitydiagnostics.mkString("; "))
  }

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    _read_usage(None, profile, fetcher)

  override def readUsage(
    source: CatalogSource,
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    _read_usage(Some(source), profile, fetcher)

  private def _read_usage(
    source: Option[CatalogSource],
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] = {
    val references = Vector(
      Some(("catalog", profile.evidenceUri, true)),
      profile.artifactUri.map(("artifact", _, true)),
      profile.modelMetadataUri.map(("model-metadata", _, true)),
      profile.documentationUri.map(("documentation", _, true))
    ).flatten
    profile.modelMetadataUri match {
      case Some(uri) if !CatalogUriPolicy.isAuthorizedFetch(profile.evidenceUri, uri) =>
        val reason = if (uri.getUserInfo != null) "contains credentials" else "its origin differs from the catalog"
        Consequence.success(ComponentUsage(
          profile,
          Vector.empty,
          references,
          profile.warnings :+ s"Model metadata was not fetched because $reason: ${InformationSourceDiagnosticPolicy.renderUri(uri)}."
        ))
      case Some(uri) =>
        source.fold(fetcher.get(uri, policy.maxMetadataBytes))(fetcher.get(_, uri, policy.maxMetadataBytes)) match {
          case Consequence.Success(body) =>
            _parse_operations(body, uri).map { operations =>
              ComponentUsage(profile, operations, references, profile.warnings)
            }
          case Consequence.Failure(conclusion) =>
            Consequence.success(ComponentUsage(
              profile,
              Vector.empty,
              references,
              profile.warnings :+ InformationSourceDiagnosticPolicy.sanitize(
                s"Model metadata unavailable at ${InformationSourceDiagnosticPolicy.renderUri(uri)}: ${conclusion.display}"
              )
            ))
        }
      case None =>
        Consequence.success(ComponentUsage(
          profile,
          Vector.empty,
          references,
          profile.warnings :+ "Catalog does not publish model metadata."
        ))
    }
  }

  private def _parse_index(
    source: CatalogSource,
    kind: String,
    uri: URI,
    body: String
  ): Consequence[ParsedIndex] =
    parse(body) match {
      case Left(error) => Consequence.failure(s"Invalid component catalog JSON at $uri: ${error.getMessage}")
      case Right(json) =>
        val cursor = json.hcursor
        val schemafield = cursor.downField("schemaVersion").focus.orElse(cursor.downField("schema").focus)
        val declaredschema = _string(json, "schemaVersion").orElse(_string(json, "schema"))
        val diagnosticsvalue = cursor.downField("diagnostics").focus
        if (schemafield.nonEmpty)
          Consequence.failure(
            s"Unsupported declared Cozy repository catalog schema at $uri: ${declaredschema.getOrElse("non-string schema")}"
          )
        else if (diagnosticsvalue.exists(value => !value.isArray))
          Consequence.failure(s"Invalid Cozy repository catalog diagnostics at $uri: diagnostics must be an array.")
        else cursor.get[Vector[Json]]("entries").toOption match {
          case None => Consequence.failure(s"Invalid Cozy repository catalog contract at $uri: entries must be an array.")
          case Some(entries) =>
            val boundedentries = entries.take(policy.maxProfiles)
            val parsedprofiles = boundedentries.map(_profile(source, kind, uri, _))
            val invalidentry = parsedprofiles.indexWhere(_.isEmpty)
            if (invalidentry >= 0)
              Consequence.failure(s"Invalid Cozy repository catalog entry ${invalidentry + 1} at $uri: component identity is required.")
            else Consequence.success(ParsedIndex(
              parsedprofiles.flatten,
              _diagnostics(json) ++ Option.when(entries.size > policy.maxProfiles) {
                s"Catalog $kind profiles were truncated at ${policy.maxProfiles} entries."
              }
            ))
        }
    }

  private def _profile(
    source: CatalogSource,
    kind: String,
    evidenceuri: URI,
    json: Json
  ): Option[ComponentProfile] = {
    val name = _string(json, "artifact_id").orElse(_string(json, "name")).map(_.trim).filter(_.nonEmpty)
    name.map { componentname =>
      val versions = _versions(json)
      val recommended = _string(json, "recommended")
      val lateststable = _string(json, "latest_stable").orElse(_string(json, "latestStable"))
      val latestsnapshot = _string(json, "latest_snapshot").orElse(_string(json, "latestSnapshot"))
      val selectedversion = recommended.orElse(lateststable).orElse(latestsnapshot).orElse(versions.headOption)
      val artifactpath = _artifact_path(json, selectedversion)
      val catalogroot = source.baseUri.resolve(s"repository/catalog/$kind/")
      val modelmetadatapath = _string_at(json, "sidecars", "model_metadata_json")
      val versionevidence = _version_evidence(
        source,
        json,
        versions,
        selectedversion,
        modelmetadatapath,
        catalogroot,
        componentname
      )
      val selectedevidence = selectedversion.flatMap(v => versionevidence.find(_.version == v))
      ComponentProfile(
        catalogId = source.id,
        organization = _string(json, "organization").orElse(_string_at(json, "project", "organization")),
        name = componentname,
        title = _string(json, "title").getOrElse(componentname),
        summary = _string(json, "summary").orElse(_string(json, "description")),
        kind = kind,
        versions = versions,
        selectedVersion = selectedversion,
        dependencyMetadataVersion = selectedevidence.filter(_.hasDependencyMetadata).map(_.version),
        latestStable = lateststable,
        latestSnapshot = latestsnapshot,
        runtimeMinimum = selectedevidence.flatMap(_.runtimeMinimum),
        tags = (_strings(json, "tags") ++ _strings(json, "aliases")).distinct,
        terms = _strings(json, "terms"),
        dependencies = selectedevidence.toVector.flatMap(_.dependencies),
        artifactUri = selectedevidence.flatMap(_.artifactUri).orElse(artifactpath.map(source.baseUri.resolve)),
        evidenceUri = evidenceuri,
        modelMetadataUri = selectedevidence.flatMap(_.modelMetadataUri),
        documentationUri = _string(json, "documentation").map(source.baseUri.resolve)
          .orElse(Some(source.baseUri.resolve(s"repository/$kind/$componentname/index.html"))),
        versionEvidence = versionevidence,
        warnings = Vector.empty ++
          (if (versions.isEmpty) Vector("Catalog entry does not publish versions.") else Vector.empty) ++
          (if (artifactpath.isEmpty) Vector("Catalog entry does not publish an artifact path for the selected version.") else Vector.empty),
        selectedChannel = selectedevidence.flatMap(_.channel),
        selectedStatus = selectedevidence.flatMap(_.status),
        selectedComponent = selectedevidence.flatMap(_.component),
        selectedPublishedAt = selectedevidence.flatMap(_.publishedAt),
        runtimeMaximum = selectedevidence.flatMap(_.runtimeMaximum),
        runtimeTested = selectedevidence.toVector.flatMap(_.runtimeTested),
        artifactChecksumSha256 = selectedevidence.flatMap(_.artifactChecksumSha256)
      )
    }
  }

  private def _version_evidence(
    source: CatalogSource,
    json: Json,
    versions: Vector[String],
    selectedversion: Option[String],
    modelmetadatapath: Option[String],
    catalogroot: URI,
    componentname: String
  ): Vector[ComponentVersionEvidence] = {
    val commondependencies = _dependencies(json)
    val entries = _array(json, "versions").flatMap { entry =>
      _string(entry, "version").map { version =>
        val isselected = selectedversion.contains(version)
        val artifactpath = _string(entry, "file").orElse(_string(entry, "path"))
          .orElse(Option.when(isselected)(_string(json, "file")).flatten)
        val metadatapath = _string_at(entry, "sidecars", "model_metadata_json")
          .orElse(Option.when(isselected)(modelmetadatapath).flatten)
        ComponentVersionEvidence(
          version = version,
          runtimeMinimum = _string_at(entry, "runtime", "cncf", "minimum")
            .orElse(Option.when(isselected)(_string_at(json, "runtime", "minimum")).flatten)
            .orElse(Option.when(isselected)(_string_at(json, "runtime", "cncf", "minimum")).flatten),
          dependencies = (commondependencies ++ _dependencies(entry)).distinct,
          artifactUri = artifactpath.map(source.baseUri.resolve),
          modelMetadataUri = metadatapath.map(source.baseUri.resolve)
            .orElse(Option.when(isselected)(Some(catalogroot.resolve(s"$componentname.model-metadata.json"))).flatten),
          hasDependencyMetadata = _has_dependency_metadata(json) || _has_dependency_metadata(entry),
          channel = _string(entry, "channel"),
          status = _string(entry, "status"),
          component = _string(entry, "component"),
          publishedAt = _string(entry, "published_at").orElse(_string(entry, "publishedAt")),
          runtimeMaximum = _string_at(entry, "runtime", "cncf", "maximum"),
          runtimeTested = _strings_at(entry, "runtime", "cncf", "tested"),
          artifactChecksumSha256 = _string_at(entry, "checksum", "sha256")
        )
      }
    }
    val entryversions = entries.map(_.version).toSet
    val placeholders = versions.filterNot(entryversions).map { version =>
      val isselected = selectedversion.contains(version)
      ComponentVersionEvidence(
        version = version,
        runtimeMinimum = Option.when(isselected)(_string_at(json, "runtime", "minimum")).flatten
          .orElse(Option.when(isselected)(_string_at(json, "runtime", "cncf", "minimum")).flatten),
        dependencies = if (isselected) commondependencies else Vector.empty,
        artifactUri = Option.when(isselected)(_string(json, "file").map(source.baseUri.resolve)).flatten,
        modelMetadataUri = Option.when(isselected) {
          Some(modelmetadatapath.map(source.baseUri.resolve).getOrElse(catalogroot.resolve(s"$componentname.model-metadata.json")))
        }.flatten,
        hasDependencyMetadata = isselected && _has_dependency_metadata(json)
      )
    }
    (entries ++ placeholders).sortBy(_.version)
  }

  private def _parse_operations(body: String, uri: URI): Consequence[Vector[ComponentOperation]] =
    parse(body) match {
      case Left(error) => Consequence.failure(s"Invalid model metadata JSON at $uri: ${error.getMessage}")
      case Right(json) =>
        val services = _array_at(json, "surface", "component", "services")
        val operations = services.flatMap { service =>
          val servicename = _string(service, "name")
          _array(service, "operations").flatMap { operation =>
            _string(operation, "name").map { name =>
              ComponentOperation(
                servicename,
                name,
                _string(operation, "kind").orElse(_string(operation, "type")),
                _string(operation, "description").orElse(_string(operation, "summary"))
              )
            }
          }
        }
        Consequence.success(operations.sortBy(x => (x.service.getOrElse(""), x.operation)))
    }

  private def _versions(json: Json): Vector[String] = {
    val direct = _strings(json, "versions")
    val objects = _array(json, "versions").flatMap(_string(_, "version"))
    (direct ++ objects ++
      _string(json, "latest_stable").toVector ++
      _string(json, "latest_snapshot").toVector).map(_.trim).filter(_.nonEmpty).distinct
  }

  private def _artifact_path(json: Json, version: Option[String]): Option[String] = {
    val entries = _array(json, "versions")
    entries.find(x => version.forall(v => _string(x, "version").contains(v)))
      .flatMap(x => _string(x, "file").orElse(_string(x, "path")))
      .orElse(_string(json, "file"))
  }

  private def _dependencies(json: Json): Vector[ComponentDependency] = {
    val values = _array(json, "dependencies") ++
      _array_at(json, "component_descriptor", "dependencies") ++
      _array_at(json, "abi_manifest", "dependencies") ++
      _array_at(json, "abi_manifest", "abi", "dependencies")
    values.flatMap { dependency =>
      _string(dependency, "name").map { name =>
        ComponentDependency(name, _string(dependency, "version"), _string(dependency, "kind"))
      }
    }.distinct
  }

  private def _has_dependency_metadata(json: Json): Boolean =
    Vector(
      Vector("dependencies"),
      Vector("component_descriptor", "dependencies"),
      Vector("abi_manifest", "dependencies"),
      Vector("abi_manifest", "abi", "dependencies")
    ).exists(path => _json_at(json, path).exists(!_.isNull))

  private def _diagnostics(json: Json): Vector[String] =
    _array(json, "diagnostics").flatMap { diagnostic =>
      _string(diagnostic, "code").map { code =>
        val artifact = _string(diagnostic, "artifact_id").map(x => s" for $x").getOrElse("")
        val version = _string(diagnostic, "version").map(x => s"@$x").getOrElse("")
        val metadata = Vector(
          _string(diagnostic, "metadata_name").map(x => s"metadata_name=$x"),
          _string(diagnostic, "metadata_version").map(x => s"metadata_version=$x"),
          _string(diagnostic, "project_path").map(x => s"project_path=$x")
        ).flatten
        val details = Option.when(metadata.nonEmpty)(s" (${metadata.mkString(", ")})").getOrElse("")
        InformationSourceDiagnosticPolicy.sanitize(s"Cozy catalog diagnostic $code$artifact$version$details.")
      }
    }

  private def _json_at(json: Json, path: Vector[String]): Option[Json] =
    path.foldLeft[io.circe.ACursor](json.hcursor)((cursor, segment) => cursor.downField(segment)).focus

  private def _array(json: Json, name: String): Vector[Json] =
    json.hcursor.downField(name).as[Vector[Json]].getOrElse(Vector.empty)

  private def _array_at(json: Json, path: String*): Vector[Json] =
    path.foldLeft[io.circe.ACursor](json.hcursor)((cursor, segment) => cursor.downField(segment)).as[Vector[Json]].getOrElse(Vector.empty)

  private def _string(json: Json, name: String): Option[String] =
    json.hcursor.get[String](name).toOption.map(_.trim).filter(_.nonEmpty)

  private def _string_at(json: Json, path: String*): Option[String] =
    if (path.isEmpty) None
    else path.dropRight(1).foldLeft[io.circe.ACursor](json.hcursor)((cursor, segment) => cursor.downField(segment))
      .get[String](path.last).toOption.map(_.trim).filter(_.nonEmpty)

  private def _strings_at(json: Json, path: String*): Vector[String] =
    path.foldLeft[io.circe.ACursor](json.hcursor)((cursor, segment) => cursor.downField(segment))
      .as[Vector[String]].getOrElse(Vector.empty).map(_.trim).filter(_.nonEmpty)

  private def _strings(json: Json, name: String): Vector[String] =
    json.hcursor.get[Vector[String]](name).getOrElse(Vector.empty).map(_.trim).filter(_.nonEmpty)

}

final class SimpleModelingPublicationCatalogProvider(
  policy: CatalogInspectionPolicy = CatalogInspectionPolicy.DEFAULT,
  clock: Clock
) extends ComponentCatalogProvider {
  private val _supported_schema = "cozy.publish-project.v1"
  private val _metadata_link = """href=[\"']([^\"'/]+)/metadata\.html[\"']""".r
  private val _non_component_entries = Set("maven-repository")

  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] = {
    val cataloguri = source.baseUri.resolve("en/catalog/index.html")
    fetcher.get(source, cataloguri, policy.maxIndexBytes).flatMap { body =>
      val discoverednames = _metadata_link.findAllMatchIn(body)
        .map(_.group(1).trim)
        .filter(x => x.nonEmpty && !_non_component_entries.contains(x))
        .take(policy.maxProfiles + 1)
        .toVector
        .distinct
      val names = discoverednames.take(policy.maxProfiles)
      val results = names.map(name => name -> _read_profile(source, fetcher, name))
      val profiles = results.collect { case (_, Consequence.Success(Some(profile))) => profile }
      val warnings = results.collect {
        case (name, Consequence.Failure(conclusion)) =>
          InformationSourceDiagnosticPolicy.sanitize(s"$name: ${conclusion.display}")
      } ++ Option.when(discoverednames.size > policy.maxProfiles) {
        s"Publication catalog profiles were truncated at ${policy.maxProfiles} entries."
      }
      if (profiles.nonEmpty)
        Consequence.success(CatalogSnapshot(
          source,
          profiles.sortBy(x => (x.kind, x.name, x.catalogId)),
          clock.instant(),
          Option.when(warnings.nonEmpty)(warnings.mkString("; "))
        ))
      else
        Consequence.serviceUnavailable(s"No CAR or SAR publication metadata found from $cataloguri")
    }
  }

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] = {
    val references = Vector(
      Some(("catalog", profile.evidenceUri, true)),
      profile.artifactUri.map(("artifact", _, true)),
      profile.documentationUri.map(("documentation", _, true))
    ).flatten
    Consequence.success(ComponentUsage(
      profile,
      Vector.empty,
      references,
      profile.warnings :+ "The publication catalog does not expose generated operation metadata; use a Cozy repository index for operation-level usage."
    ))
  }

  private def _read_profile(
    source: CatalogSource,
    fetcher: CatalogFetcher,
    name: String
  ): Consequence[Option[ComponentProfile]] = {
    val catalogprojecturi = source.baseUri.resolve(s"metadata/catalog/projects/$name.json")
    fetcher.get(source, catalogprojecturi, policy.maxMetadataBytes).flatMap { catalogbody =>
      parse(catalogbody) match {
        case Left(error) =>
          Consequence.failure(s"Invalid publication project JSON at $catalogprojecturi: ${error.getMessage}")
        case Right(catalogjson) =>
          _validate_publication_contract(catalogjson, "catalog-project", catalogprojecturi) match {
            case Left(diagnostic) => Consequence.failure(diagnostic)
            case Right(_) =>
              val kind = _string_at(catalogjson, "project", "kind")
                .map(_.toLowerCase(java.util.Locale.ROOT))
              kind match {
                case Some(componentkind @ ("car" | "sar")) =>
                  val artifacturi = source.baseUri.resolve(s"metadata/artifacts/repository/$name.json")
                  fetcher.get(source, artifacturi, policy.maxMetadataBytes).flatMap { artifactbody =>
                    parse(artifactbody) match {
                      case Left(error) =>
                        Consequence.failure(s"Invalid publication artifact JSON at $artifacturi: ${error.getMessage}")
                      case Right(artifactjson) =>
                        _validate_publication_contract(artifactjson, "repository-artifact", artifacturi) match {
                          case Left(diagnostic) => Consequence.failure(diagnostic)
                          case Right(_) =>
                            _profile(source, componentkind, artifacturi, artifactjson)
                              .map(x => Consequence.success(Some(x)))
                              .getOrElse(Consequence.failure(s"Publication artifact has no component identity at $artifacturi"))
                        }
                    }
                  }
                case Some(_) => Consequence.success(None)
                case None => Consequence.failure(s"Publication project has no kind at $catalogprojecturi")
              }
          }
      }
    }
  }

  private def _validate_publication_contract(
    json: Json,
    expectedtype: String,
    uri: URI
  ): Either[String, Unit] = {
    val cursor = json.hcursor
    val schemafield = cursor.downField("schema").focus.orElse(cursor.downField("schemaVersion").focus)
    val schema = _string(json, "schema").orElse(_string(json, "schemaVersion"))
    val documenttype = _string(json, "type")
    if (schemafield.isEmpty) Right(())
    else if (!schema.contains(_supported_schema))
      Left(s"Unsupported publication schema at $uri: ${schema.getOrElse("non-string schema")}")
    else if (!documenttype.contains(expectedtype))
      Left(s"Invalid $_supported_schema document at $uri: type must be $expectedtype.")
    else Right(())
  }

  private def _profile(
    source: CatalogSource,
    kind: String,
    evidenceuri: URI,
    json: Json
  ): Option[ComponentProfile] = {
    val project = json.hcursor.downField("project").focus.getOrElse(Json.Null)
    val artifact = json.hcursor.downField("artifact").focus.getOrElse(Json.Null)
    val name = _string(project, "name")
    name.map { componentname =>
      val versions = _array(artifact, "kinds")
        .filter(x => _string(x, "type").exists(_.equalsIgnoreCase(kind)))
        .flatMap(_strings(_, "versions")) ++ _string(project, "version").toVector
      val kindmetadata = _array(artifact, "kinds")
        .find(x => _string(x, "type").exists(_.equalsIgnoreCase(kind)))
      val projectversion = _string(project, "version")
      val stable = kindmetadata.flatMap(x => _string(x, "latestRelease"))
        .orElse(projectversion.filterNot(_is_snapshot))
      val snapshot = kindmetadata.flatMap(x => _string(x, "latestSnapshot"))
        .orElse(projectversion.filter(_is_snapshot))
      val selectedversion = stable.orElse(snapshot)
      val versionevidence = versions.map(_.trim).filter(_.nonEmpty).distinct.map { version =>
        val versionfile = _array(artifact, "files")
          .filter(x => _string(x, "type").exists(_.equalsIgnoreCase(kind)))
          .find(x => _string(x, "version").contains(version))
        ComponentVersionEvidence(
          version,
          None,
          Vector.empty,
          versionfile.flatMap(x => _string(x, "publicPath").orElse(_string(x, "warehousePath"))).map(source.baseUri.resolve),
          None,
          hasDependencyMetadata = false
        )
      }
      val selectedevidence = selectedversion.flatMap(v => versionevidence.find(_.version == v))
      ComponentProfile(
        catalogId = source.id,
        organization = _string(project, "organization"),
        name = componentname,
        title = _string(project, "title").getOrElse(componentname),
        summary = _string(project, "summary").orElse(_string(project, "description")),
        kind = kind,
        versions = versions.map(_.trim).filter(_.nonEmpty).distinct,
        selectedVersion = selectedversion,
        dependencyMetadataVersion = None,
        latestStable = stable,
        latestSnapshot = snapshot,
        runtimeMinimum = None,
        tags = Vector.empty,
        terms = Vector.empty,
        dependencies = Vector.empty,
        artifactUri = selectedevidence.flatMap(_.artifactUri),
        evidenceUri = evidenceuri,
        modelMetadataUri = None,
        documentationUri = Some(source.baseUri.resolve(s"en/catalog/$componentname/index.html")),
        versionEvidence = versionevidence,
        warnings = Option.when(selectedversion.nonEmpty && selectedevidence.flatMap(_.artifactUri).isEmpty)(
          s"Selected version ${selectedversion.get} does not publish an artifact path."
        ).toVector
      )
    }
  }

  private def _array(json: Json, name: String): Vector[Json] =
    json.hcursor.downField(name).as[Vector[Json]].getOrElse(Vector.empty)

  private def _string(json: Json, name: String): Option[String] =
    json.hcursor.get[String](name).toOption.map(_.trim).filter(_.nonEmpty)

  private def _string_at(json: Json, path: String*): Option[String] =
    if (path.isEmpty) None
    else path.dropRight(1).foldLeft[io.circe.ACursor](json.hcursor)((cursor, segment) => cursor.downField(segment))
      .get[String](path.last).toOption.map(_.trim).filter(_.nonEmpty)

  private def _strings(json: Json, name: String): Vector[String] =
    json.hcursor.get[Vector[String]](name).getOrElse(Vector.empty).map(_.trim).filter(_.nonEmpty)

  private def _is_snapshot(version: String): Boolean =
    version.toUpperCase(java.util.Locale.ROOT).contains("SNAPSHOT")
}

final class InMemoryComponentCatalogProvider(
  profiles: Vector[ComponentProfile],
  operations: Map[String, Vector[ComponentOperation]] = Map.empty,
  clock: Clock
) extends ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
    Consequence.success(CatalogSnapshot(source, profiles.map(_.copy(catalogId = source.id)), clock.instant(), None))

  def readUsage(profile: ComponentProfile, fetcher: CatalogFetcher): Consequence[ComponentUsage] =
    Consequence.success(ComponentUsage(
      profile,
      operations.getOrElse(profile.name, Vector.empty),
      Vector(("catalog", profile.evidenceUri, true)),
      profile.warnings
    ))
}

final class CbdRuntime(
  val sources: Vector[CatalogSource],
  provider: ComponentCatalogProvider,
  cachepolicy: CatalogCachePolicy,
  clock: Clock,
  configurationwarnings: Vector[String],
  val bokSources: Vector[BokSource],
  bokprovider: BokKnowledgeSourceProvider,
  bokpolicy: BokInspectionPolicy,
  val sieBokSources: Vector[SieBokSource],
  siebokprovider: SieBokProvider,
  siebokpolicy: SieBokPolicy,
  localconfiguration: LocalInformationSourceConfiguration,
  localpolicy: LocalInspectionPolicy,
  retentionpolicy: InformationSourceRetentionPolicy,
  admittedlocalinventory: Option[LocalInformationInventory] = None
) {
  def this(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider,
    cachepolicy: CatalogCachePolicy,
    clock: Clock,
    configurationwarnings: Vector[String],
    boksources: Vector[BokSource],
    bokprovider: BokKnowledgeSourceProvider,
    bokpolicy: BokInspectionPolicy,
    sieboksources: Vector[SieBokSource],
    siebokprovider: SieBokProvider,
    siebokpolicy: SieBokPolicy,
    localconfiguration: LocalInformationSourceConfiguration,
    localpolicy: LocalInspectionPolicy
  ) = this(
    sources,
    provider,
    cachepolicy,
    clock,
    configurationwarnings,
    boksources,
    bokprovider,
    bokpolicy,
    sieboksources,
    siebokprovider,
    siebokpolicy,
    localconfiguration,
    localpolicy,
    InformationSourceRetentionPolicy.DEFAULT
  )

  require(
    sources.size + bokSources.size + sieBokSources.size + localconfiguration.sources.size <= retentionpolicy.maxSources,
    s"Configured information sources must not exceed the runtime retention limit of ${retentionpolicy.maxSources}."
  )
  private val _runtime_started_at = clock.instant()
  private val _refresh_slots = new Semaphore(
    math.min(
      cachepolicy.refreshPolicy.maxConcurrentRefreshes,
      bokpolicy.refreshPolicy.maxConcurrentRefreshes
    ),
    true
  )
  private var _snapshots = Map.empty[String, CatalogSnapshot]
  private var _failures = Map.empty[String, String]
  private var _refresh_failure_counts = Map.empty[String, Int]
  private var _last_refresh_attempts = Map.empty[String, Instant]
  private var _bok_snapshots = Map.empty[String, BokSourceSnapshot]
  private var _bok_failures = Map.empty[String, String]
  private var _bok_refresh_failure_counts = Map.empty[String, Int]
  private var _bok_last_refresh_attempts = Map.empty[String, Instant]
  private var _refresh_flights = Map.empty[String, CountDownLatch]
  private var _sie_bok_snapshots = Map.empty[String, SieBokSnapshot]
  private var _sie_bok_failures = Map.empty[String, String]
  private var _sie_bok_last_refresh_attempts = Map.empty[String, Instant]
  private var _local_inventory: Option[LocalInformationInventory] = None

  def ensureReady(fetcher: CatalogFetcher): Consequence[Unit] = {
    val now = clock.instant()
    val selected = synchronized {
      sources.filter(_.enabled).filter { source =>
        _is_catalog_refresh_due(source, now) || _is_refresh_in_flight(s"catalog:${source.id}")
      }
    }
    val refreshed = if (selected.nonEmpty) _refresh_sources(selected, fetcher, forced = false) else Consequence.success(sourceStates(includeDisabled = true))
    refreshed.flatMap { _ =>
      synchronized {
        if (_snapshots.nonEmpty) Consequence.success(())
        else Consequence.serviceUnavailable(_failures.values.toVector.sorted.mkString("; "))
      }
    }
  }

  def ensureInputsReady(fetcher: CatalogFetcher & BokFetcher): Consequence[Unit] = {
    val catalogresult = ensureReady(fetcher)
    val now = clock.instant()
    val selected = synchronized {
      bokSources.filter(_.enabled).filter { source =>
        _is_bok_refresh_due(source, now) || _is_refresh_in_flight(s"bok:${source.id}")
      }
    }
    if (selected.nonEmpty) _refresh_bok_sources(selected, fetcher)
    val inventory = _bounded_local_inventory(
      admittedlocalinventory.getOrElse(
        LocalInformationInventory(Vector.empty, Vector.empty, localconfiguration.warnings, clock.instant(), Map.empty)
      )
    )
    synchronized {
      _local_inventory = Some(inventory)
    }
    catalogresult match {
      case Consequence.Success(_) => Consequence.success(())
      case Consequence.Failure(_) if synchronized(_bok_snapshots.nonEmpty || inventory.sources.nonEmpty) =>
        Consequence.success(())
      case Consequence.Failure(conclusion) => Consequence.Failure(conclusion)
    }
  }

  def refresh(
    sourceid: Option[String],
    fetcher: CatalogFetcher
  ): Consequence[Vector[CatalogSourceState]] = {
    val selected = sources.filter(_.enabled).filter(x => sourceid.forall(_ == x.id))
    if (selected.isEmpty)
      Consequence.failure(s"No enabled catalog source matched: ${sourceid.getOrElse("all")}")
    else _refresh_sources(selected, fetcher, forced = true)
  }

  def search(
    requirement: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    runtimeversion: Option[String],
    limit: Int,
    sourceid: Option[String] = None,
    semanticevidence: Vector[SemanticRequirementEvidence] = Vector.empty
  ): Vector[ComponentMatch] = {
    val querytokens = _tokens(requirement)
    _profiles.flatMap { profile =>
      version match {
        case Some(requested) if profile.versions.contains(requested) => Some(profile.selectVersion(requested))
        case Some(_) => None
        case None => Some(profile)
      }
    }.filter { profile =>
      sourceid.forall(_ == profile.catalogId) &&
        organization.forall(x => profile.organization.exists(_.equalsIgnoreCase(x))) &&
        kind.forall(_.equalsIgnoreCase(profile.kind)) &&
        runtimeversion.forall { actual =>
          profile.runtimeMinimum.exists(_version_lte(_, actual)) &&
            profile.runtimeMaximum.forall(_version_lte(actual, _))
        }
    }.flatMap { profile =>
      val exact = Vector(profile.name, profile.identity, profile.title).exists(_.equalsIgnoreCase(requirement.trim))
      val texttokens = _tokens((Vector(profile.name, profile.title) ++ profile.summary ++ profile.tags ++ profile.terms).mkString(" "))
      val matched = querytokens.intersect(texttokens)
      val directscore = if (exact) 1.0 else if (querytokens.isEmpty) 0.0 else matched.size.toDouble / querytokens.size.toDouble
      val semanticevidenceids = SemanticRequirementMatcher.matchingEvidenceIds(profile, semanticevidence)
      val semanticscore = semanticevidence.filter(evidence => semanticevidenceids.contains(evidence.id)).map(_.score).maxOption.getOrElse(0.0)
      val score = directscore.max(semanticscore)
      if (score <= 0.0 && semanticevidenceids.isEmpty) None
      else Some(ComponentMatch(
        profile,
        if (exact) "exact" else if (directscore > 0.0) "candidate" else "semantic",
        score,
        if (exact) s"Exact component identity match for ${profile.identity}."
        else if (directscore > 0.0) s"Catalog metadata matched ${matched.toVector.sorted.mkString(", ")}."
        else s"Catalog terms cite ${semanticevidenceids.mkString(", ")}.",
        semanticevidenceids
      ))
    }.sortBy(x => (if (x.matchKind == "exact") 0 else 1, -x.score, x.profile.name, x.profile.catalogId))
      .take(limit.max(1).min(100))
  }

  def get(
    name: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    catalogid: Option[String]
  ): Option[ComponentProfile] =
    selectComponent(name, organization, kind, version, catalogid).selectedProfile

  def selectComponent(
    name: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    catalogid: Option[String]
  ): ExactComponentSelection = {
    val candidates = _profiles.filter(_.name.equalsIgnoreCase(name.trim))
      .filter(x => organization.forall(y => x.organization.exists(_.equalsIgnoreCase(y))))
      .filter(x => kind.forall(_.equalsIgnoreCase(x.kind)))
      .filter(x => version.forall(x.versions.contains))
      .filter(x => catalogid.forall(_ == x.catalogId))
      .sortBy(x => (_source_priority(x.catalogId), x.catalogId, x.name))
      .map(x => version.map(x.selectVersion).getOrElse(x))
    ExactComponentSelection.fromCandidates(candidates)
  }

  def usage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] = usage(profile, None, fetcher)

  def usage(
    profile: ComponentProfile,
    intent: Option[String],
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    sources.find(_.id == profile.catalogId)
      .fold(provider.readUsage(profile, fetcher))(provider.readUsage(_, profile, fetcher))
      .map(IntentAwareUsageGuidance.enrich(_, intent))

  def resolveDependencies(
    profile: ComponentProfile,
    maxdepth: Int
  ): ComponentDependencyResolution =
    resolveDependencies(profile, None, maxdepth)

  def resolveDependencies(
    profile: ComponentProfile,
    requestedversion: Option[String],
    maxdepth: Int
  ): ComponentDependencyResolution = {
    val boundeddepth = maxdepth.max(1).min(CbdRuntime.MAX_DEPENDENCY_DEPTH)
    val rootversion = requestedversion.orElse(_selected_version(profile))
    val rootlabel = _profile_label(profile, rootversion)
    val rootkey = _profile_key(profile, rootversion)
    val rootmetadataavailable = rootversion.exists(profile.dependencyMetadataVersion.contains)
    val directdependencies = if (rootmetadataavailable) profile.dependencies else Vector.empty

    def _walk_(
      parent: ComponentProfile,
      depth: Int,
      ancestors: Set[String],
      path: Vector[String]
    ): Vector[ResolvedComponentDependency] =
      parent.dependencies.flatMap { dependency =>
        val dependencylabel = _dependency_label(dependency)
        val dependencypath = path :+ dependencylabel
        val renderedpath = dependencypath.mkString(" -> ")
        _dependency_candidates(profile.catalogId, dependency) match {
          case Vector() =>
            Vector(ResolvedComponentDependency(dependency, "unresolved", depth, renderedpath, None))
          case Vector(target) =>
            val targetkey = _profile_key(target, dependency.version.orElse(_selected_version(target)))
            val metadataavailable = dependency.version.forall(target.dependencyMetadataVersion.contains)
            if (ancestors.contains(targetkey))
              Vector(ResolvedComponentDependency(dependency, "cycle", depth, renderedpath, Some(target)))
            else {
              val resolved = ResolvedComponentDependency(dependency, "resolved", depth, renderedpath, Some(target))
              if (depth >= boundeddepth) Vector(resolved)
              else if (!metadataavailable) Vector(resolved)
              else resolved +: _walk_(target, depth + 1, ancestors + targetkey, dependencypath)
            }
          case _ =>
            Vector(ResolvedComponentDependency(dependency, "ambiguous", depth, renderedpath, None))
        }
      }

    val resolutions =
      if (rootmetadataavailable) _walk_(profile, 1, Set(rootkey), Vector(rootlabel))
      else Vector.empty
    val conflicts = _dependency_conflicts(resolutions)
    val warnings = (
      Option.when(!rootmetadataavailable) {
        val requested = rootversion.getOrElse("unknown")
        val selected = profile.dependencyMetadataVersion.getOrElse("unknown")
        s"Direct dependency metadata is unavailable for requested root version $requested; catalog metadata selects $selected: $rootlabel."
      }.toVector ++ resolutions.collect {
        case resolution if resolution.status == "unresolved" =>
          s"Dependency is not published in catalog ${profile.catalogId}: ${resolution.path}."
        case resolution if resolution.status == "ambiguous" =>
          s"Dependency matches multiple profiles in catalog ${profile.catalogId}: ${resolution.path}."
        case resolution if resolution.status == "cycle" =>
          s"Dependency cycle detected: ${resolution.path}."
        case resolution
          if resolution.status == "resolved" &&
            resolution.dependency.version.exists(x => !resolution.resolvedProfile.flatMap(_.dependencyMetadataVersion).contains(x)) =>
          val requested = resolution.dependency.version.get
          val selected = resolution.resolvedProfile.flatMap(_.dependencyMetadataVersion).getOrElse("unknown")
          s"Transitive dependency metadata is unavailable for requested version $requested; catalog metadata selects $selected: ${resolution.path}."
      } ++
        resolutions.collect {
          case resolution
            if resolution.status == "resolved" &&
              resolution.depth == boundeddepth &&
              resolution.resolvedProfile.exists(_.dependencies.nonEmpty) =>
            s"Dependency traversal stopped at maxDepth=$boundeddepth: ${resolution.path}."
        } ++ conflicts.map(_.message)
    ).distinct.sorted
    val absences = Option.when(!rootmetadataavailable) {
      ComponentEvidenceAbsence(
        ExactComponentSelection.DEPENDENCY_METADATA_ABSENT,
        "dependency-resolution",
        "Dependency metadata is not published for the selected component version.",
        Vector(profile.catalogId),
        rootversion.toVector,
        Vector(profile.evidenceUri)
      )
    }.toVector
    ComponentDependencyResolution(directdependencies, resolutions, conflicts, warnings, absences)
  }

  def sourceStates(includeDisabled: Boolean): Vector[CatalogSourceState] = synchronized {
    val now = clock.instant()
    sources.filter(x => includeDisabled || x.enabled).sortBy(x => (x.priority, x.id)).map { source =>
      val snapshot = _snapshots.get(source.id)
      val warnings = (_failures.get(source.id).toVector ++ snapshot.toVector.flatMap(_.warning)).distinct
      val failure = Option.when(warnings.nonEmpty)(warnings.mkString("; "))
      val stale = snapshot.exists(_is_stale(_, now))
      CatalogSourceState(
        source,
        if (!source.enabled) "disabled" else if (failure.nonEmpty || stale) "degraded" else if (snapshot.nonEmpty) "ready" else "not-started",
        snapshot.map(_.profiles.size).getOrElse(0),
        snapshot.map(_.refreshedAt),
        snapshot.map(_expires_at),
        _last_refresh_attempts.get(source.id),
        _next_catalog_refresh_attempt_at(source),
        if (!source.enabled) "disabled" else if (snapshot.isEmpty) "empty" else if (stale) "stale" else "fresh",
        failure
      )
    }
  }

  def bokSourceStates(includeDisabled: Boolean): Vector[BokSourceState] = synchronized {
    val now = clock.instant()
    bokSources.filter(x => includeDisabled || x.enabled).sortBy(x => (x.priority, x.id)).map { source =>
      val snapshot = _bok_snapshots.get(source.id)
      val diagnostics = (_bok_failures.get(source.id).toVector ++ snapshot.toVector.flatMap(_.warnings)).distinct
      val stale = snapshot.exists(_is_bok_stale(_, now))
      BokSourceState(
        source,
        if (!source.enabled) "disabled" else if (diagnostics.nonEmpty || stale) "degraded" else if (snapshot.nonEmpty) "ready" else "not-started",
        snapshot.map(_.terms.size).getOrElse(0),
        snapshot.map(_.observedAt),
        snapshot.map(_bok_expires_at),
        _bok_last_refresh_attempts.get(source.id),
        _next_bok_refresh_attempt_at(source),
        if (!source.enabled) "disabled" else if (snapshot.isEmpty) "empty" else if (stale) "stale" else "fresh",
        diagnostics
      )
    }
  }

  def searchSieTerms(
    query: String,
    category: Option[String],
    limit: Int,
    transport: SieBokTransport
  ): Consequence[Vector[SieBokSnapshot]] = {
    val selected = sieBokSources.filter(_.enabled)
    val snapshots = selected.flatMap { source =>
      val attemptedat = clock.instant()
      synchronized {
        _sie_bok_last_refresh_attempts = _sie_bok_last_refresh_attempts.updated(source.id, attemptedat)
      }
      siebokprovider.searchTerms(source, query, category, limit, transport, siebokpolicy) match {
        case Consequence.Success(snapshot) => synchronized {
          val boundedsnapshot = _bounded_sie_bok_snapshot(source.id, snapshot)
          _sie_bok_snapshots = _sie_bok_snapshots.updated(source.id, boundedsnapshot)
          _sie_bok_failures = _sie_bok_failures.removed(source.id)
          Some(boundedsnapshot)
        }
        case Consequence.Failure(conclusion) => synchronized {
          _sie_bok_failures = _sie_bok_failures.updated(
            source.id,
            InformationSourceDiagnosticPolicy.sanitize(conclusion.display)
          )
          None
        }
      }
    }
    Consequence.success(snapshots)
  }

  def sieBokSourceStates(includedisabled: Boolean): Vector[SieBokSourceState] = synchronized {
    sieBokSources.filter(x => includedisabled || x.enabled).sortBy(x => (x.priority, x.id)).map { source =>
      val snapshot = _sie_bok_snapshots.get(source.id)
      val diagnostics = (_sie_bok_failures.get(source.id).toVector ++ snapshot.toVector.flatMap(_.warnings)).distinct
      SieBokSourceState(
        source,
        if (!source.enabled) "disabled" else if (diagnostics.nonEmpty) "degraded" else if (snapshot.nonEmpty) "ready" else "not-started",
        snapshot.map(_.terms.size).getOrElse(0),
        snapshot.map(_.observedAt),
        _sie_bok_last_refresh_attempts.get(source.id),
        diagnostics
      )
    }
  }

  def informationSourceStates(includeDisabled: Boolean): Vector[InformationSourceState] =
    (sourceStates(includeDisabled).map(_.informationSourceState) ++
      bokSourceStates(includeDisabled).map(_.informationSourceState) ++
      sieBokSourceStates(includeDisabled).map(_.informationSourceState) ++
      localSourceStates(includeDisabled))
      .sortBy(state => (state.descriptor.priority, state.descriptor.id))

  def localSourceStates(includedisabled: Boolean): Vector[InformationSourceState] = synchronized {
    admittedlocalinventory.map(_.sources).getOrElse(localconfiguration.sources)
      .filter(descriptor => includedisabled || descriptor.enabled)
      .sortBy(descriptor => (descriptor.priority, descriptor.id)).map { descriptor =>
        val inventory = _local_inventory
        val observations = inventory.toVector.flatMap(_.observations).filter(_.sourceId == descriptor.id)
        val diagnostics = inventory.toVector.flatMap(_.sourceDiagnostics.getOrElse(descriptor.id, Vector.empty)).distinct
        InformationSourceState(
          descriptor,
          if (!descriptor.enabled) "disabled" else if (inventory.isEmpty) "not-started" else if (diagnostics.nonEmpty) "degraded" else "ready",
          observations.size,
          InformationSourceFreshness(
            if (!descriptor.enabled) "disabled" else if (inventory.isEmpty) "empty" else "observed",
            inventory.map(_.observedAt),
            None,
            inventory.map(_.observedAt),
            None
          ),
          diagnostics
        )
      }
  }

  def bokSnapshots: Vector[BokSourceSnapshot] = synchronized {
    _bok_snapshots.values.toVector.sortBy(snapshot => (snapshot.source.priority, snapshot.source.id))
  }

  def bokTerms: Vector[BokTermObservation] =
    bokSnapshots.flatMap(_.terms)

  def sieBokSnapshots: Vector[SieBokSnapshot] = synchronized {
    _sie_bok_snapshots.values.toVector.sortBy(snapshot => (snapshot.source.priority, snapshot.source.id))
  }

  def localInventory: Option[LocalInformationInventory] = synchronized {
    _local_inventory
  }

  def searchSourceAware(
    query: SourceAwareComponentSearchQuery,
    currentsiesnapshots: Vector[SieBokSnapshot] = Vector.empty
  ): SourceAwareComponentSearchResult = {
    val semanticevidence = SemanticRequirementMatcher.matchEvidence(
      query.requirement,
      bokSnapshots,
      currentsiesnapshots,
      query.limit,
      clock,
      bokpolicy.refreshTtl,
      query.sourceId,
      query.sourceKind
    )
    val catalogmatches = search(
      query.requirement,
      query.organization,
      query.componentKind,
      query.version,
      query.runtimeVersion,
      SourceAwareRetrieval.MAXIMUM_RESULTS,
      query.sourceId,
      semanticevidence
    )
    val catalogentries = catalogmatches.flatMap { result =>
      observation(result.profile).map { observed =>
        result -> ReconciliationObservation.fromCatalog(result.profile, observed)
      }
    }
    val localobservations = localInventory.toVector.flatMap(_.observations)
    SourceAwareRetrieval.search(query, catalogentries, localobservations, semanticevidence)
  }

  def observation(profile: ComponentProfile): Option[ComponentObservation] =
    profile.observationContext.map { context =>
      val matchingstate = sourceStates(includeDisabled = true).find { state =>
        state.source.id == context.sourceId && state.refreshedAt.contains(context.observedAt)
      }
      ComponentObservation(
        context.sourceId,
        context.sourceKind,
        profile.evidenceUri.toString,
        _selected_version(profile),
        matchingstate.map(_.cacheStatus).getOrElse {
          if (clock.instant().isBefore(context.expiresAt)) "fresh" else "stale"
        },
        Some(context.observedAt),
        Some(context.expiresAt),
        profile.artifactChecksumSha256,
        (context.diagnostics ++ matchingstate.toVector.flatMap(_.warning) ++ profile.warnings).distinct
      )
    }

  def overallStatus: String = {
    val states = informationSourceStates(includeDisabled = false)
    if (states.exists(_.status == "ready") && states.exists(_.status == "degraded")) "degraded"
    else if (states.exists(_.status == "ready")) "ready"
    else if (states.exists(_.status == "degraded")) "degraded"
    else "not-started"
  }

  def componentCount: Int = _profiles.size

  def configurationWarnings: Vector[String] = configurationwarnings

  private def _profiles: Vector[ComponentProfile] = synchronized {
    _snapshots.values.toVector.sortBy(x => (x.source.priority, x.source.id)).flatMap(_.profiles)
  }

  private def _refresh_sources(
    selected: Vector[CatalogSource],
    fetcher: CatalogFetcher,
    forced: Boolean
  ): Consequence[Vector[CatalogSourceState]] = {
    selected.foreach { source =>
      _with_refresh_single_flight(s"catalog:${source.id}") {
        if (forced || synchronized(_is_catalog_refresh_due(source, clock.instant())))
          _refresh_catalog_source(source, fetcher)
      }
    }
    Consequence.success(sourceStates(includeDisabled = true))
  }

  private def _refresh_bok_sources(
    selected: Vector[BokSource],
    fetcher: BokFetcher
  ): Unit = {
    selected.foreach { source =>
      _with_refresh_single_flight(s"bok:${source.id}") {
        if (synchronized(_is_bok_refresh_due(source, clock.instant())))
          _refresh_bok_source(source, fetcher)
      }
    }
  }

  private def _refresh_catalog_source(source: CatalogSource, fetcher: CatalogFetcher): Unit = {
    val attemptedat = clock.instant()
    synchronized {
      _last_refresh_attempts = _last_refresh_attempts.updated(source.id, attemptedat)
    }
    provider.read(source, fetcher) match {
      case Consequence.Success(snapshot) => synchronized {
        val observedat = clock.instant()
        val boundedcandidate = _bounded_catalog_snapshot(source.id, snapshot.copy(
          source = source,
          refreshedAt = observedat
        ))
        val observationcontext = ComponentObservationContext(
          source.id,
          source.sourceKind,
          observedat,
          observedat.plus(cachepolicy.ttl),
          boundedcandidate.warning.toVector
        )
        val profiles = boundedcandidate.profiles.map(_.copy(observationContext = Some(observationcontext)))
        val boundedsnapshot = boundedcandidate.copy(profiles = profiles)
        _snapshots = _snapshots.updated(source.id, boundedsnapshot)
        _failures = _failures.removed(source.id)
        _refresh_failure_counts = _refresh_failure_counts.removed(source.id)
      }
      case Consequence.Failure(conclusion) => synchronized {
        _failures = _failures.updated(source.id, InformationSourceDiagnosticPolicy.sanitize(conclusion.display))
        _refresh_failure_counts = _refresh_failure_counts.updated(
          source.id,
          math.min(
            _refresh_failure_counts.getOrElse(source.id, 0) + 1,
            InformationSourceRefreshPolicy.MAXIMUM_CONSECUTIVE_FAILURES
          )
        )
      }
    }
  }

  private def _refresh_bok_source(source: BokSource, fetcher: BokFetcher): Unit = {
    val attemptedat = clock.instant()
    synchronized {
      _bok_last_refresh_attempts = _bok_last_refresh_attempts.updated(source.id, attemptedat)
    }
    bokprovider.read(source, fetcher, bokpolicy) match {
      case Consequence.Success(snapshot) => synchronized {
        val boundedsnapshot = _bounded_bok_snapshot(source.id, snapshot.copy(
          source = source.descriptor,
          observedAt = clock.instant()
        ))
        _bok_snapshots = _bok_snapshots.updated(source.id, boundedsnapshot)
        _bok_failures = _bok_failures.removed(source.id)
        _bok_refresh_failure_counts = _bok_refresh_failure_counts.removed(source.id)
      }
      case Consequence.Failure(conclusion) => synchronized {
        _bok_failures = _bok_failures.updated(source.id, InformationSourceDiagnosticPolicy.sanitize(conclusion.display))
        _bok_refresh_failure_counts = _bok_refresh_failure_counts.updated(
          source.id,
          math.min(
            _bok_refresh_failure_counts.getOrElse(source.id, 0) + 1,
            InformationSourceRefreshPolicy.MAXIMUM_CONSECUTIVE_FAILURES
          )
        )
      }
    }
  }

  private def _with_refresh_single_flight(key: String)(work: => Unit): Unit = {
    val (leader, flight) = synchronized {
      _refresh_flights.get(key) match {
        case Some(x) => (false, x)
        case None =>
          val created = new CountDownLatch(1)
          _refresh_flights = _refresh_flights.updated(key, created)
          (true, created)
      }
    }
    if (leader) {
      _refresh_slots.acquireUninterruptibly()
      try work
      finally {
        flight.countDown()
        synchronized {
          if (_refresh_flights.get(key).contains(flight))
            _refresh_flights = _refresh_flights.removed(key)
        }
        _refresh_slots.release()
      }
    } else {
      var waiting = true
      var interrupted = false
      while (waiting) {
        try {
          flight.await()
          waiting = false
        } catch {
          case _: InterruptedException => interrupted = true
        }
      }
      if (interrupted)
        Thread.currentThread().interrupt()
    }
  }

  private def _bounded_catalog_snapshot(sourceid: String, snapshot: CatalogSnapshot): CatalogSnapshot = {
    val capacity = _retained_observation_capacity(
      retentionpolicy.maxCatalogObservations,
      sources.map(source => (source.id, source.priority)),
      sourceid
    )
    if (snapshot.profiles.size <= capacity) snapshot
    else snapshot.copy(
      profiles = snapshot.profiles.take(capacity),
      warning = Some(_append_retention_warning(
        snapshot.warning.toVector,
        "Catalog",
        sourceid,
        retentionpolicy.maxCatalogObservations
      ).mkString("; "))
    )
  }

  private def _bounded_bok_snapshot(sourceid: String, snapshot: BokSourceSnapshot): BokSourceSnapshot = {
    val capacity = _retained_observation_capacity(
      retentionpolicy.maxBokObservations,
      bokSources.map(source => (source.id, source.priority)),
      sourceid
    )
    if (snapshot.terms.size <= capacity) snapshot
    else snapshot.copy(
      terms = snapshot.terms.take(capacity),
      warnings = _append_retention_warning(
        snapshot.warnings,
        "BoK",
        sourceid,
        retentionpolicy.maxBokObservations
      )
    )
  }

  private def _bounded_sie_bok_snapshot(sourceid: String, snapshot: SieBokSnapshot): SieBokSnapshot = {
    val capacity = _retained_observation_capacity(
      retentionpolicy.maxSieBokObservations,
      sieBokSources.map(source => (source.id, source.priority)),
      sourceid
    )
    if (snapshot.terms.size <= capacity) snapshot
    else snapshot.copy(
      terms = snapshot.terms.take(capacity),
      warnings = _append_retention_warning(
        snapshot.warnings,
        "SIE BoK",
        sourceid,
        retentionpolicy.maxSieBokObservations
      )
    )
  }

  private def _bounded_local_inventory(inventory: LocalInformationInventory): LocalInformationInventory = {
    val configuredlocalsources = inventory.sources.map(source => (source.id, source.priority))
    val retained = inventory.sources.sortBy(source => (source.priority, source.id)).flatMap { source =>
      val capacity = _retained_observation_capacity(
        retentionpolicy.maxLocalObservations,
        configuredlocalsources,
        source.id
      )
      inventory.observations.filter(_.sourceId == source.id).take(capacity)
    }
    if (retained.size == inventory.observations.size) inventory
    else {
      val warning =
        s"Local snapshot retention truncated observations under the runtime total policy limit of ${retentionpolicy.maxLocalObservations}."
      val retainedcounts = retained.groupMapReduce(_.sourceId)(_ => 1)(_ + _)
      val originalcounts = inventory.observations.groupMapReduce(_.sourceId)(_ => 1)(_ + _)
      val truncatedsourceids = originalcounts.collect {
        case (sourceid, count) if retainedcounts.getOrElse(sourceid, 0) < count => sourceid
      }.toSet
      inventory.copy(
        observations = retained,
        warnings = (inventory.warnings :+ warning).distinct,
        sourceDiagnostics = inventory.sourceDiagnostics.map { case (sourceid, diagnostics) =>
          sourceid -> (if (truncatedsourceids.contains(sourceid)) (diagnostics :+ warning).distinct else diagnostics)
        }
      )
    }
  }

  private def _append_retention_warning(
    warnings: Vector[String],
    sourcekind: String,
    sourceid: String,
    limit: Int
  ): Vector[String] =
    (warnings :+
      s"$sourcekind snapshot retention truncated source $sourceid under the runtime total policy limit of $limit observations.").distinct

  private def _retained_observation_capacity(
    total: Int,
    configuredsources: Vector[(String, Int)],
    sourceid: String
  ): Int = {
    val orderedsourceids = configuredsources.sortBy { case (id, priority) => (priority, id) }.map(_._1)
    val sourcecount = orderedsourceids.size
    if (sourcecount == 0) 0
    else orderedsourceids.indexOf(sourceid) match {
      case -1 => 0
      case index => total / sourcecount + Option.when(index < total % sourcecount)(1).getOrElse(0)
    }
  }

  private def _is_refresh_in_flight(key: String): Boolean = synchronized {
    _refresh_flights.contains(key)
  }

  private def _expires_at(snapshot: CatalogSnapshot): Instant =
    snapshot.refreshedAt.plus(cachepolicy.ttl)

  private def _is_stale(snapshot: CatalogSnapshot, now: Instant): Boolean =
    !now.isBefore(_expires_at(snapshot))

  private def _next_catalog_refresh_attempt_at(source: CatalogSource): Option[Instant] =
    if (!source.enabled)
      None
    else
      _failures.get(source.id).flatMap { _ =>
        _last_refresh_attempts.get(source.id).map(_.plus(_retry_interval(
          cachepolicy.refreshPolicy,
          _refresh_failure_counts.getOrElse(source.id, 1)
        )))
      }.orElse(
        _snapshots.get(source.id).map(_.refreshedAt.plus(cachepolicy.refreshPolicy.interval))
      ).orElse(
        _last_refresh_attempts.get(source.id).map(_.plus(cachepolicy.refreshPolicy.interval))
      ).orElse(Some(_runtime_started_at))

  private def _is_catalog_refresh_due(source: CatalogSource, now: Instant): Boolean =
    _next_catalog_refresh_attempt_at(source).exists(nextattempt => !now.isBefore(nextattempt))

  private def _bok_expires_at(snapshot: BokSourceSnapshot): Instant =
    snapshot.observedAt.plus(bokpolicy.refreshTtl)

  private def _is_bok_stale(snapshot: BokSourceSnapshot, now: Instant): Boolean =
    !now.isBefore(_bok_expires_at(snapshot))

  private def _next_bok_refresh_attempt_at(source: BokSource): Option[Instant] =
    if (!source.enabled)
      None
    else
      _bok_failures.get(source.id).flatMap { _ =>
        _bok_last_refresh_attempts.get(source.id).map(_.plus(_retry_interval(
          bokpolicy.refreshPolicy,
          _bok_refresh_failure_counts.getOrElse(source.id, 1)
        )))
      }.orElse(
        _bok_snapshots.get(source.id).map(_.observedAt.plus(bokpolicy.refreshPolicy.interval))
      ).orElse(
        _bok_last_refresh_attempts.get(source.id).map(_.plus(bokpolicy.refreshPolicy.interval))
      ).orElse(Some(_runtime_started_at))

  private def _is_bok_refresh_due(source: BokSource, now: Instant): Boolean =
    _next_bok_refresh_attempt_at(source).exists(nextattempt => !now.isBefore(nextattempt))

  private def _retry_interval(
    policy: InformationSourceRefreshPolicy,
    failurecount: Int
  ): Duration = {
    val exponent = math.max(
      0,
      math.min(failurecount - 1, InformationSourceRefreshPolicy.MAXIMUM_CONSECUTIVE_FAILURES - 1)
    )
    (0 until exponent).foldLeft(policy.retryInitialInterval) { (current, _) =>
      if (current.compareTo(policy.retryMaximumInterval) >= 0)
        policy.retryMaximumInterval
      else {
        val doubled = current.multipliedBy(2)
        if (doubled.compareTo(policy.retryMaximumInterval) > 0)
          policy.retryMaximumInterval
        else
          doubled
      }
    }
  }

  private def _source_priority(sourceid: String): Int =
    sources.find(_.id == sourceid).map(_.priority).getOrElse(Int.MaxValue)

  private def _dependency_candidates(
    catalogid: String,
    dependency: ComponentDependency
  ): Vector[ComponentProfile] =
    _profiles.filter(_.catalogId == catalogid)
      .filter(_.name.equalsIgnoreCase(dependency.name))
      .filter(x => dependency.kind.forall(_.equalsIgnoreCase(x.kind)))
      .filter(x => dependency.version.forall(x.versions.contains))
      .map(x => dependency.version.map(x.selectVersion).getOrElse(x))
      .sortBy(x => (x.kind, x.organization.getOrElse(""), x.name))

  private def _dependency_conflicts(
    resolutions: Vector[ResolvedComponentDependency]
  ): Vector[ComponentDependencyConflict] =
    resolutions.filter(_.dependency.version.nonEmpty)
      .groupBy { resolution =>
        val kind = resolution.dependency.kind.orElse(resolution.resolvedProfile.map(_.kind))
        resolution.dependency.name.toLowerCase(java.util.Locale.ROOT) -> kind.map(_.toLowerCase(java.util.Locale.ROOT))
      }
      .toVector
      .flatMap { case ((_, normalizedkind), entries) =>
        val versions = entries.flatMap(_.dependency.version).distinct.sorted
        Option.when(versions.size > 1) {
          val name = entries.head.dependency.name
          val kind = normalizedkind
          val paths = entries.map(_.path).distinct.sorted
          ComponentDependencyConflict(
            name,
            kind,
            versions,
            paths,
            s"Conflicting dependency versions for ${kind.map(x => s"$x:").getOrElse("")}$name: ${versions.mkString(", ")}."
          )
        }
      }
      .sortBy(x => (x.name, x.kind.getOrElse("")))

  private def _profile_key(profile: ComponentProfile, version: Option[String]): String =
    s"${profile.catalogId}:${profile.kind}:${profile.identity}:${version.getOrElse("?")}".toLowerCase(java.util.Locale.ROOT)

  private def _profile_label(profile: ComponentProfile, version: Option[String]): String =
    s"${profile.kind}:${profile.identity}@${version.getOrElse("?")}"

  private def _dependency_label(dependency: ComponentDependency): String =
    s"${dependency.kind.map(x => s"$x:").getOrElse("")}${dependency.name}@${dependency.version.getOrElse("?")}"

  private def _selected_version(profile: ComponentProfile): Option[String] =
    profile.selectedVersion.orElse(profile.latestStable).orElse(profile.latestSnapshot).orElse(profile.versions.headOption)

  private def _tokens(value: String): Set[String] =
    Option(value).getOrElse("").toLowerCase(java.util.Locale.ROOT)
      .split("[^\\p{L}\\p{N}._:-]+").toSet.map(_.trim).filter(_.nonEmpty)

  private def _version_lte(minimum: String, actual: String): Boolean =
    _version_parts(minimum).zipAll(_version_parts(actual), 0, 0).find { case (lhs, rhs) => lhs != rhs } match {
      case Some((lhs, rhs)) => lhs <= rhs
      case None => true
    }

  private def _version_parts(value: String): Vector[Int] =
    value.takeWhile(x => x.isDigit || x == '.').split("\\.").toVector.filter(_.nonEmpty).map(_.toIntOption.getOrElse(0))
}

object CbdRuntime {
  val DEFAULT_DEPENDENCY_DEPTH = 8
  val MAX_DEPENDENCY_DEPTH = 32
  private val _empty_local_configuration = LocalInformationSourceConfiguration(Vector.empty, Vector.empty, Vector.empty)

  /**
   * Runtime-owned input assembled by the CNCF component boundary. Raw host
   * environment access is deliberately outside this value object.
   */
  final case class Configuration(
    catalogs: Option[String] = None,
    catalogAllowedOrigins: Option[String] = None,
    bokSites: Option[String] = None,
    bokAllowedOrigins: Option[String] = None,
    sieBokRoutes: Option[String] = None,
    sieAllowedOrigins: Option[String] = None,
    sourceAuthentication: Option[String] = None,
    admittedLocalInventory: Option[LocalInformationInventory] = None
  )

  def create(
    configuration: Configuration,
    clock: Clock
  ): CbdRuntime = {
    val cozy = new CozyComponentCatalogProvider(clock = clock)
    val publication = new SimpleModelingPublicationCatalogProvider(clock = clock)
    val catalogconfiguration = CatalogSourceConfig.parse(configuration.catalogs, configuration.catalogAllowedOrigins)
    val bokconfiguration = BokSourceConfig.parse(
      configuration.bokSites,
      configuration.bokAllowedOrigins,
      catalogconfiguration.sources.map(_.id).toSet ++ Set("local-car", "cache-car")
    )
    val siebokconfiguration = SieBokConfig.parse(
      configuration.sieBokRoutes,
      configuration.sieAllowedOrigins,
      catalogconfiguration.sources.map(_.id).toSet ++ bokconfiguration.sources.map(_.id) ++ Set("local-car", "cache-car")
    )
    val localconfiguration = _empty_local_configuration
    val remoteids = catalogconfiguration.sources.map(_.id).toSet ++ bokconfiguration.sources.map(_.id) ++ siebokconfiguration.sources.map(_.id)
    val authenticationconfiguration = SourceAuthenticationConfig.parse(configuration.sourceAuthentication, remoteids)
    val catalogsources = catalogconfiguration.sources.map(source => source.copy(authentication = authenticationconfiguration.authenticationFor(source.id)))
    val boksources = bokconfiguration.sources.map(source => source.copy(authentication = authenticationconfiguration.authenticationFor(source.id)))
    val sieboksources = siebokconfiguration.sources.map(source => source.copy(authentication = authenticationconfiguration.authenticationFor(source.id)))
    new CbdRuntime(
      catalogsources,
      new CompatibleComponentCatalogProvider(cozy, publication),
      CatalogCachePolicy.DEFAULT,
      clock,
      catalogconfiguration.warnings ++ bokconfiguration.warnings ++ siebokconfiguration.warnings ++ authenticationconfiguration.warnings,
      boksources,
      new BokKnowledgeSourceProvider(clock),
      BokInspectionPolicy.DEFAULT,
      sieboksources,
      new SieBokProvider(clock),
      SieBokPolicy.DEFAULT,
      localconfiguration,
      LocalInspectionPolicy.DEFAULT,
      InformationSourceRetentionPolicy.DEFAULT,
      configuration.admittedLocalInventory
    )
  }

  def create(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider,
    clock: Clock
  ): CbdRuntime = create(sources, provider, CatalogCachePolicy.DEFAULT, clock)

  def create(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider,
    cachepolicy: CatalogCachePolicy,
    clock: Clock
  ): CbdRuntime = new CbdRuntime(
    sources,
    provider,
    cachepolicy,
    clock,
    Vector.empty,
    Vector.empty,
    new BokKnowledgeSourceProvider(clock),
    BokInspectionPolicy.DEFAULT,
    Vector.empty,
    new SieBokProvider(clock),
    SieBokPolicy.DEFAULT,
    _empty_local_configuration,
    LocalInspectionPolicy.DEFAULT,
    InformationSourceRetentionPolicy.DEFAULT
  )

  def createFederated(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider,
    cachepolicy: CatalogCachePolicy,
    clock: Clock,
    boksources: Vector[BokSource],
    bokprovider: BokKnowledgeSourceProvider,
    bokpolicy: BokInspectionPolicy,
    sieboksources: Vector[SieBokSource],
    siebokprovider: SieBokProvider,
    siebokpolicy: SieBokPolicy,
    localconfiguration: LocalInformationSourceConfiguration,
    localpolicy: LocalInspectionPolicy
  ): CbdRuntime = createFederated(
    sources,
    provider,
    cachepolicy,
    clock,
    boksources,
    bokprovider,
    bokpolicy,
    sieboksources,
    siebokprovider,
    siebokpolicy,
    localconfiguration,
    localpolicy,
    InformationSourceRetentionPolicy.DEFAULT
  )

  def createFederated(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider,
    cachepolicy: CatalogCachePolicy,
    clock: Clock,
    boksources: Vector[BokSource],
    bokprovider: BokKnowledgeSourceProvider,
    bokpolicy: BokInspectionPolicy = BokInspectionPolicy.DEFAULT,
    sieboksources: Vector[SieBokSource] = Vector.empty,
    siebokprovider: SieBokProvider,
    siebokpolicy: SieBokPolicy = SieBokPolicy.DEFAULT,
    localconfiguration: LocalInformationSourceConfiguration = _empty_local_configuration,
    localpolicy: LocalInspectionPolicy = LocalInspectionPolicy.DEFAULT,
    retentionpolicy: InformationSourceRetentionPolicy = InformationSourceRetentionPolicy.DEFAULT,
    admittedlocalinventory: Option[LocalInformationInventory] = None
  ): CbdRuntime = new CbdRuntime(
    sources,
    provider,
    cachepolicy,
    clock,
    Vector.empty,
    boksources,
    bokprovider,
    bokpolicy,
    sieboksources,
    siebokprovider,
    siebokpolicy,
    localconfiguration,
    localpolicy,
    retentionpolicy,
    admittedlocalinventory
  )
}

object CatalogSourceConfig {
  private val _default_source = CatalogSource(
    "simplemodeling",
    URI.create("https://www.simplemodeling.org/"),
    100,
    true,
    InformationSourceKind.PUBLISHED_CATALOG,
    InformationSourceAuthorization.BUILT_IN
  )

  def parse(
    catalogs: Option[String],
    allowedorigins: Option[String],
    policy: CatalogInspectionPolicy = CatalogInspectionPolicy.DEFAULT
  ): CatalogSourceConfiguration = {
    val allowedentries = allowedorigins.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val allowedoverflow = Option.when(allowedentries.size > policy.maxAllowedOrigins) {
      s"Catalog allowed-origin configuration exceeds the limit of ${policy.maxAllowedOrigins}."
    }.toVector
    val allowedresults = allowedentries.take(policy.maxAllowedOrigins).zipWithIndex.map { case (value, index) =>
      try {
        val uri = _base_uri(value)
        if (!Set("", "/").contains(Option(uri.getPath).getOrElse("")))
          Left(s"Catalog allowlist entry ${index + 1} is not an origin without a path.")
        else Right(CatalogUriPolicy.origin(uri))
      } catch {
        case NonFatal(_) => Left(s"Catalog allowlist entry ${index + 1} is not a valid HTTP(S) origin.")
      }
    }
    val allowed = allowedresults.collect { case Right(origin) => origin }.toSet
    val allowwarnings = allowedresults.collect { case Left(warning) => warning }
    val configuredentries = catalogs.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val sourceoverflow = Option.when(configuredentries.size > policy.maxConfiguredSources) {
      s"Catalog source configuration exceeds the limit of ${policy.maxConfiguredSources}."
    }.toVector
    val configuredresults = configuredentries.take(policy.maxConfiguredSources).zipWithIndex.map { case (value, index) =>
      val pair = value.split("=", 2)
      val id = if (pair.length == 2) pair(0).trim else s"configured-${index + 1}"
      val urivalue = if (pair.length == 2) pair(1).trim else pair(0).trim
      if (!id.matches("[A-Za-z0-9._-]+"))
        Left(s"Configured catalog entry ${index + 1} was rejected because its source ID is invalid.")
      else {
        try {
          val uri = _base_uri(urivalue)
          val origin = CatalogUriPolicy.origin(uri)
          if (allowed.contains(origin)) Right(CatalogSource(
            id,
            uri,
            200 + index,
            true,
            InformationSourceKind.PUBLISHED_CATALOG,
            InformationSourceAuthorization.EXACT_ORIGIN_ALLOWLIST
          ))
          else Left(s"Configured catalog entry ${index + 1} was rejected because origin $origin is not allowlisted.")
        } catch {
          case NonFatal(_) => Left(s"Configured catalog entry ${index + 1} was rejected because its base URI is invalid.")
        }
      }
    }
    val candidates = configuredresults.collect { case Right(source) => source }
    val rejectionwarnings = configuredresults.collect { case Left(warning) => warning }
    val initial = (Vector(_default_source), Set(_default_source.id), Vector.empty[String])
    val (sources, _, duplicatewarnings) = candidates.foldLeft(initial) { case ((accepted, ids, warnings), source) =>
      if (ids.contains(source.id))
        (accepted, ids, warnings :+ s"Configured catalog source ${source.id} was rejected because its source ID is duplicated.")
      else
        (accepted :+ source, ids + source.id, warnings)
    }
    CatalogSourceConfiguration(
      sources.sortBy(x => (x.priority, x.id)),
      (allowedoverflow ++ allowwarnings ++ sourceoverflow ++ rejectionwarnings ++ duplicatewarnings).toVector
    )
  }

  private def _base_uri(value: String): URI = {
    val normalized = if (value.endsWith("/")) value else s"$value/"
    val uri = URI.create(normalized)
    if (
      !Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase(java.util.Locale.ROOT)) ||
      uri.getHost == null ||
      uri.getUserInfo != null ||
      uri.getQuery != null ||
      uri.getFragment != null
    )
      throw new IllegalArgumentException(s"Catalog URI must be absolute HTTP(S): $value")
    uri
  }

}
