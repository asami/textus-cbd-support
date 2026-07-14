package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.Instant
import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CatalogSource(
  id: String,
  baseUri: URI,
  priority: Int,
  enabled: Boolean
)

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
  warnings: Vector[String]
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
  hasDependencyMetadata: Boolean
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
  warnings: Vector[String]
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
          dependencies = evidence.dependencies,
          artifactUri = evidence.artifactUri,
          modelMetadataUri = evidence.modelMetadataUri,
          warnings = _version_neutral_warnings ++
            Option.when(evidence.artifactUri.isEmpty)(s"Selected version $requested does not publish an artifact path.")
        )
      case None =>
        copy(
          selectedVersion = Some(requested),
          dependencyMetadataVersion = None,
          runtimeMinimum = None,
          dependencies = Vector.empty,
          artifactUri = None,
          modelMetadataUri = None,
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
  warning: Option[String]
)

final case class ComponentMatch(
  profile: ComponentProfile,
  matchKind: String,
  score: Double,
  rationale: String
)

final case class ComponentUsage(
  profile: ComponentProfile,
  operations: Vector[ComponentOperation],
  references: Vector[(String, URI, Boolean)],
  warnings: Vector[String]
)

trait CatalogFetcher {
  def get(uri: URI): Consequence[String]
}

trait ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot]

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage]
}

final class CompatibleComponentCatalogProvider(
  cozy: CozyComponentCatalogProvider,
  publication: SimpleModelingPublicationCatalogProvider
) extends ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
    cozy.read(source, fetcher) match {
      case success: Consequence.Success[CatalogSnapshot] => success
      case Consequence.Failure(cozyfailure) =>
        publication.read(source, fetcher) match {
          case success: Consequence.Success[CatalogSnapshot] => success
          case Consequence.Failure(publicationfailure) =>
            Consequence.serviceUnavailable(
              s"Cozy repository catalog unavailable: ${cozyfailure.display}; publication catalog unavailable: ${publicationfailure.display}"
            )
        }
    }

  def readUsage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] =
    if (profile.modelMetadataUri.nonEmpty) cozy.readUsage(profile, fetcher)
    else publication.readUsage(profile, fetcher)
}

final class CozyComponentCatalogProvider extends ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] = {
    val documents = Vector("car", "sar").map { kind =>
      val uri = source.baseUri.resolve(s"metadata/repository/$kind/index.json")
      fetcher.get(uri).flatMap(_parse_index(source, kind, uri, _))
    }
    _sequence_allow_missing(documents).map { case (profiles, warnings) =>
      CatalogSnapshot(
        source,
        profiles.flatten.sortBy(x => (x.kind, x.name, x.catalogId)),
        Instant.now(),
        if (warnings.nonEmpty) Some(warnings.mkString("; ")) else None
      )
    }
  }

  def readUsage(
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
      case Some(uri) =>
        fetcher.get(uri) match {
          case Consequence.Success(body) =>
            _parse_operations(body, uri).map { operations =>
              ComponentUsage(profile, operations, references, profile.warnings)
            }
          case Consequence.Failure(conclusion) =>
            Consequence.success(ComponentUsage(
              profile,
              Vector.empty,
              references,
              profile.warnings :+ s"Model metadata unavailable at $uri: ${conclusion.display}"
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
  ): Consequence[Vector[ComponentProfile]] =
    parse(body) match {
      case Left(error) => Consequence.failure(s"Invalid component catalog JSON at $uri: ${error.getMessage}")
      case Right(json) =>
        val entries = _array(json, "entries")
        Consequence.success(entries.flatMap(_profile(source, kind, uri, _)))
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
          (if (artifactpath.isEmpty) Vector("Catalog entry does not publish an artifact path for the selected version.") else Vector.empty)
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
          version,
          _string_at(entry, "runtime", "cncf", "minimum")
            .orElse(Option.when(isselected)(_string_at(json, "runtime", "minimum")).flatten)
            .orElse(Option.when(isselected)(_string_at(json, "runtime", "cncf", "minimum")).flatten),
          (commondependencies ++ _dependencies(entry)).distinct,
          artifactpath.map(source.baseUri.resolve),
          metadatapath.map(source.baseUri.resolve)
            .orElse(Option.when(isselected)(Some(catalogroot.resolve(s"$componentname.model-metadata.json"))).flatten),
          hasDependencyMetadata = _has_dependency_metadata(json) || _has_dependency_metadata(entry)
        )
      }
    }
    val entryversions = entries.map(_.version).toSet
    val placeholders = versions.filterNot(entryversions).map { version =>
      val isselected = selectedversion.contains(version)
      ComponentVersionEvidence(
        version,
        Option.when(isselected)(_string_at(json, "runtime", "minimum")).flatten
          .orElse(Option.when(isselected)(_string_at(json, "runtime", "cncf", "minimum")).flatten),
        if (isselected) commondependencies else Vector.empty,
        Option.when(isselected)(_string(json, "file").map(source.baseUri.resolve)).flatten,
        Option.when(isselected) {
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
      _array_at(json, "abi_manifest", "dependencies")
    values.flatMap { dependency =>
      _string(dependency, "name").map { name =>
        ComponentDependency(name, _string(dependency, "version"), _string(dependency, "kind"))
      }
    }.distinct
  }

  private def _has_dependency_metadata(json: Json): Boolean =
    json.hcursor.downField("dependencies").focus.nonEmpty ||
      json.hcursor.downField("component_descriptor").downField("dependencies").focus.nonEmpty ||
      json.hcursor.downField("abi_manifest").downField("dependencies").focus.nonEmpty

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

  private def _strings(json: Json, name: String): Vector[String] =
    json.hcursor.get[Vector[String]](name).getOrElse(Vector.empty).map(_.trim).filter(_.nonEmpty)

  private def _sequence_allow_missing[A](
    values: Vector[Consequence[A]]
  ): Consequence[(Vector[A], Vector[String])] = {
    val successes = values.collect { case Consequence.Success(value) => value }
    val warnings = values.collect { case Consequence.Failure(conclusion) => conclusion.display }
    if (successes.nonEmpty) Consequence.success(successes -> warnings)
    else Consequence.serviceUnavailable(warnings.mkString("; "))
  }
}

final class SimpleModelingPublicationCatalogProvider extends ComponentCatalogProvider {
  private val _metadata_link = """href=[\"']([^\"'/]+)/metadata\.html[\"']""".r
  private val _non_component_entries = Set("maven-repository")

  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] = {
    val cataloguri = source.baseUri.resolve("en/catalog/index.html")
    fetcher.get(cataloguri).flatMap { body =>
      val names = _metadata_link.findAllMatchIn(body)
        .map(_.group(1).trim)
        .filter(x => x.nonEmpty && !_non_component_entries.contains(x))
        .toVector
        .distinct
      val results = names.map(name => name -> _read_profile(source, fetcher, name))
      val profiles = results.collect { case (_, Consequence.Success(Some(profile))) => profile }
      val warnings = results.collect {
        case (name, Consequence.Failure(conclusion)) => s"$name: ${conclusion.display}"
      }
      if (profiles.nonEmpty)
        Consequence.success(CatalogSnapshot(
          source,
          profiles.sortBy(x => (x.kind, x.name, x.catalogId)),
          Instant.now(),
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
    fetcher.get(catalogprojecturi).flatMap { catalogbody =>
      parse(catalogbody) match {
        case Left(error) =>
          Consequence.failure(s"Invalid publication project JSON at $catalogprojecturi: ${error.getMessage}")
        case Right(catalogjson) =>
          val kind = _string_at(catalogjson, "project", "kind")
            .map(_.toLowerCase(java.util.Locale.ROOT))
          kind match {
            case Some(componentkind @ ("car" | "sar")) =>
              val artifacturi = source.baseUri.resolve(s"metadata/artifacts/repository/$name.json")
              fetcher.get(artifacturi).flatMap { artifactbody =>
                parse(artifactbody) match {
                  case Left(error) =>
                    Consequence.failure(s"Invalid publication artifact JSON at $artifacturi: ${error.getMessage}")
                  case Right(artifactjson) =>
                    _profile(source, componentkind, artifacturi, artifactjson)
                      .map(x => Consequence.success(Some(x)))
                      .getOrElse(Consequence.failure(s"Publication artifact has no component identity at $artifacturi"))
                }
              }
            case Some(_) => Consequence.success(None)
            case None => Consequence.failure(s"Publication project has no kind at $catalogprojecturi")
          }
      }
    }
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
  operations: Map[String, Vector[ComponentOperation]] = Map.empty
) extends ComponentCatalogProvider {
  def read(source: CatalogSource, fetcher: CatalogFetcher): Consequence[CatalogSnapshot] =
    Consequence.success(CatalogSnapshot(source, profiles.map(_.copy(catalogId = source.id)), Instant.now(), None))

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
  provider: ComponentCatalogProvider
) {
  private var _snapshots = Map.empty[String, CatalogSnapshot]
  private var _failures = Map.empty[String, String]

  def ensureReady(fetcher: CatalogFetcher): Consequence[Unit] = synchronized {
    if (_snapshots.nonEmpty) Consequence.success(())
    else refresh(None, fetcher).flatMap { _ =>
      if (_snapshots.nonEmpty) Consequence.success(())
      else Consequence.serviceUnavailable(_failures.values.toVector.sorted.mkString("; "))
    }
  }

  def refresh(
    sourceid: Option[String],
    fetcher: CatalogFetcher
  ): Consequence[Vector[CatalogSourceState]] = {
    val selected = sources.filter(_.enabled).filter(x => sourceid.forall(_ == x.id))
    if (selected.isEmpty)
      Consequence.failure(s"No enabled catalog source matched: ${sourceid.getOrElse("all")}")
    else {
      selected.foreach { source =>
        provider.read(source, fetcher) match {
          case Consequence.Success(snapshot) => synchronized {
            _snapshots = _snapshots.updated(source.id, snapshot)
            _failures = _failures.removed(source.id)
          }
          case Consequence.Failure(conclusion) => synchronized {
            _failures = _failures.updated(source.id, conclusion.display)
          }
        }
      }
      Consequence.success(sourceStates(includeDisabled = true))
    }
  }

  def search(
    requirement: String,
    organization: Option[String],
    kind: Option[String],
    version: Option[String],
    runtimeversion: Option[String],
    limit: Int
  ): Vector[ComponentMatch] = {
    val querytokens = _tokens(requirement)
    _profiles.flatMap { profile =>
      version match {
        case Some(requested) if profile.versions.contains(requested) => Some(profile.selectVersion(requested))
        case Some(_) => None
        case None => Some(profile)
      }
    }.filter { profile =>
      organization.forall(x => profile.organization.exists(_.equalsIgnoreCase(x))) &&
        kind.forall(_.equalsIgnoreCase(profile.kind)) &&
        runtimeversion.forall(x => profile.runtimeMinimum.exists(_version_lte(_, x)))
    }.flatMap { profile =>
      val exact = Vector(profile.name, profile.identity, profile.title).exists(_.equalsIgnoreCase(requirement.trim))
      val texttokens = _tokens((Vector(profile.name, profile.title) ++ profile.summary ++ profile.tags ++ profile.terms).mkString(" "))
      val matched = querytokens.intersect(texttokens)
      val score = if (exact) 1.0 else if (querytokens.isEmpty) 0.0 else matched.size.toDouble / querytokens.size.toDouble
      if (score <= 0.0) None
      else Some(ComponentMatch(
        profile,
        if (exact) "exact" else "candidate",
        score,
        if (exact) s"Exact component identity match for ${profile.identity}."
        else s"Catalog metadata matched ${matched.toVector.sorted.mkString(", ")}."
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
    _profiles.filter(_.name.equalsIgnoreCase(name.trim))
      .filter(x => organization.forall(y => x.organization.exists(_.equalsIgnoreCase(y))))
      .filter(x => kind.forall(_.equalsIgnoreCase(x.kind)))
      .filter(x => version.forall(x.versions.contains))
      .filter(x => catalogid.forall(_ == x.catalogId))
      .sortBy(x => (_source_priority(x.catalogId), x.catalogId, x.name))
      .headOption
      .map(x => version.map(x.selectVersion).getOrElse(x))

  def usage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] = provider.readUsage(profile, fetcher)

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
    val rootmetadataavailable = requestedversion.forall(profile.dependencyMetadataVersion.contains)
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
        val requested = requestedversion.get
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
    ComponentDependencyResolution(directdependencies, resolutions, conflicts, warnings)
  }

  def sourceStates(includeDisabled: Boolean): Vector[CatalogSourceState] = synchronized {
    sources.filter(x => includeDisabled || x.enabled).sortBy(x => (x.priority, x.id)).map { source =>
      val snapshot = _snapshots.get(source.id)
      val failure = _failures.get(source.id).orElse(snapshot.flatMap(_.warning))
      CatalogSourceState(
        source,
        if (!source.enabled) "disabled" else if (failure.nonEmpty) "degraded" else if (snapshot.nonEmpty) "ready" else "not-started",
        snapshot.map(_.profiles.size).getOrElse(0),
        snapshot.map(_.refreshedAt),
        failure
      )
    }
  }

  def overallStatus: String = {
    val states = sourceStates(includeDisabled = false)
    if (states.exists(_.status == "ready") && states.exists(_.status == "degraded")) "degraded"
    else if (states.exists(_.status == "ready")) "ready"
    else if (states.exists(_.status == "degraded")) "degraded"
    else "not-started"
  }

  def componentCount: Int = _profiles.size

  private def _profiles: Vector[ComponentProfile] = synchronized {
    _snapshots.values.toVector.sortBy(x => (x.source.priority, x.source.id)).flatMap(_.profiles)
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

  def create(): CbdRuntime = {
    val cozy = new CozyComponentCatalogProvider()
    val publication = new SimpleModelingPublicationCatalogProvider()
    new CbdRuntime(CatalogSourceConfig.load(), new CompatibleComponentCatalogProvider(cozy, publication))
  }

  def create(
    sources: Vector[CatalogSource],
    provider: ComponentCatalogProvider
  ): CbdRuntime = new CbdRuntime(sources, provider)
}

object CatalogSourceConfig {
  private val _default_source = CatalogSource(
    "simplemodeling",
    URI.create("https://www.simplemodeling.org/"),
    100,
    true
  )

  def load(): Vector[CatalogSource] = {
    val additional = sys.env.get("TEXTUS_CBD_CATALOGS").toVector.flatMap(_.split(",")).zipWithIndex.flatMap {
      case (value, index) =>
        val trimmed = value.trim
        if (trimmed.isEmpty) None
        else {
          val pair = trimmed.split("=", 2)
          val id = if (pair.length == 2) pair(0).trim else s"configured-${index + 1}"
          val uri = if (pair.length == 2) pair(1).trim else pair(0).trim
          try Some(CatalogSource(id, _base_uri(uri), 200 + index, true))
          catch { case NonFatal(_) => None }
        }
    }
    (_default_source +: additional).groupBy(_.id).values.map(_.head).toVector.sortBy(x => (x.priority, x.id))
  }

  private def _base_uri(value: String): URI = {
    val normalized = if (value.endsWith("/")) value else s"$value/"
    val uri = URI.create(normalized)
    if (!Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase(java.util.Locale.ROOT)) || uri.getHost == null)
      throw new IllegalArgumentException(s"Catalog URI must be absolute HTTP(S): $value")
    uri
  }
}
