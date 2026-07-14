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

final case class ComponentOperation(
  service: Option[String],
  operation: String,
  kind: Option[String],
  description: Option[String]
)

final case class ComponentProfile(
  catalogId: String,
  organization: Option[String],
  name: String,
  title: String,
  summary: Option[String],
  kind: String,
  versions: Vector[String],
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
  warnings: Vector[String]
) {
  def identity: String =
    organization.filter(_.nonEmpty).map(x => s"$x:$name").getOrElse(name)
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
      val selectedentry = _array(json, "versions").find(x => selectedversion.forall(v => _string(x, "version").contains(v)))
      val artifactpath = _artifact_path(json, selectedversion)
      val catalogroot = source.baseUri.resolve(s"repository/catalog/$kind/")
      val modelmetadatapath = _string_at(json, "sidecars", "model_metadata_json")
      ComponentProfile(
        catalogId = source.id,
        organization = _string(json, "organization").orElse(_string_at(json, "project", "organization")),
        name = componentname,
        title = _string(json, "title").getOrElse(componentname),
        summary = _string(json, "summary").orElse(_string(json, "description")),
        kind = kind,
        versions = versions,
        latestStable = lateststable,
        latestSnapshot = latestsnapshot,
        runtimeMinimum = selectedentry.flatMap(_string_at(_, "runtime", "cncf", "minimum"))
          .orElse(_string_at(json, "runtime", "minimum"))
          .orElse(_string_at(json, "runtime", "cncf", "minimum")),
        tags = (_strings(json, "tags") ++ _strings(json, "aliases")).distinct,
        terms = _strings(json, "terms"),
        dependencies = (_dependencies(json) ++ selectedentry.toVector.flatMap(_dependencies)).distinct,
        artifactUri = artifactpath.map(source.baseUri.resolve),
        evidenceUri = evidenceuri,
        modelMetadataUri = Some(modelmetadatapath.map(source.baseUri.resolve).getOrElse(catalogroot.resolve(s"$componentname.model-metadata.json"))),
        documentationUri = _string(json, "documentation").map(source.baseUri.resolve)
          .orElse(Some(source.baseUri.resolve(s"repository/$kind/$componentname/index.html"))),
        warnings = Vector.empty ++
          (if (versions.isEmpty) Vector("Catalog entry does not publish versions.") else Vector.empty) ++
          (if (artifactpath.isEmpty) Vector("Catalog entry does not publish an artifact path for the selected version.") else Vector.empty)
      )
    }
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
      val selectedfile = _array(artifact, "files")
        .filter(x => _string(x, "type").exists(_.equalsIgnoreCase(kind)))
        .sortBy(x => if (_string(x, "version") == selectedversion) 0 else 1)
        .headOption
      ComponentProfile(
        catalogId = source.id,
        organization = _string(project, "organization"),
        name = componentname,
        title = _string(project, "title").getOrElse(componentname),
        summary = _string(project, "summary").orElse(_string(project, "description")),
        kind = kind,
        versions = versions.map(_.trim).filter(_.nonEmpty).distinct,
        latestStable = stable,
        latestSnapshot = snapshot,
        runtimeMinimum = None,
        tags = Vector.empty,
        terms = Vector.empty,
        dependencies = Vector.empty,
        artifactUri = selectedfile.flatMap(x => _string(x, "publicPath").orElse(_string(x, "warehousePath"))).map(source.baseUri.resolve),
        evidenceUri = evidenceuri,
        modelMetadataUri = None,
        documentationUri = Some(source.baseUri.resolve(s"en/catalog/$componentname/index.html")),
        warnings = Vector.empty
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
    _profiles.filter { profile =>
      organization.forall(x => profile.organization.exists(_.equalsIgnoreCase(x))) &&
        kind.forall(_.equalsIgnoreCase(profile.kind)) &&
        version.forall(profile.versions.contains) &&
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

  def usage(
    profile: ComponentProfile,
    fetcher: CatalogFetcher
  ): Consequence[ComponentUsage] = provider.readUsage(profile, fetcher)

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
