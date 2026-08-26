package org.simplemodeling.textus.cbdsupport.runtime

import java.io.{BufferedInputStream, ByteArrayInputStream}
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.zip.ZipInputStream
import scala.util.control.NonFatal

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.knowledge.{ComponentKnowledgeCarrier, ComponentKnowledgeCarrierCodec}
import org.goldenport.cncf.resource.{ResourceTreeQueryResult, ResourceTreeSnapshot}

/*
 * @since   Jul. 14, 2026
 * @version Jul. 20, 2026
 * @author  ASAMI, Tomoharu
 */
final case class LocalInspectionPolicy(
  maxDevelopmentDirectories: Int = 16,
  maxCarArtifacts: Int = 512,
  maxMetadataBytes: Int = 1024 * 1024,
  maxArtifactBytes: Long = 512L * 1024L * 1024L
) {
  require(maxDevelopmentDirectories > 0)
  require(maxCarArtifacts > 0)
  require(maxMetadataBytes > 0)
  require(maxArtifactBytes > 0)
}

object LocalInspectionPolicy {
  val DEFAULT: LocalInspectionPolicy = LocalInspectionPolicy()
}

final case class LocalInformationSourceConfiguration(
  developmentSources: Vector[InformationSourceDescriptor],
  carStorageSources: Vector[InformationSourceDescriptor],
  warnings: Vector[String]
) {
  def sources: Vector[InformationSourceDescriptor] = developmentSources ++ carStorageSources
}

final case class LocalComponentObservation(
  sourceId: String,
  sourceKind: String,
  componentName: Option[String],
  organization: Option[String],
  componentKind: Option[String],
  version: Option[String],
  versionEvidence: String,
  versionState: String,
  evidenceLocation: String,
  descriptorVersion: Option[String],
  pathVersion: Option[String],
  artifactChecksumSha256: Option[String],
  diagnostics: Vector[String],
  componentKnowledge: ComponentKnowledgeIntegration.Result = ComponentKnowledgeIntegration.Absent
)

final case class LocalInformationInventory(
  sources: Vector[InformationSourceDescriptor],
  observations: Vector[LocalComponentObservation],
  warnings: Vector[String],
  observedAt: Instant,
  sourceDiagnostics: Map[String, Vector[String]]
)

object LocalInformationSourceInventory {
  def inspectDevelopmentQuery(
    source: InformationSourceDescriptor,
    result: ResourceTreeQueryResult,
    versionstate: String,
    policy: LocalInspectionPolicy,
    clock: Clock
  ): LocalInformationInventory = {
    val inspected = result.entries.map(_inspect_development(source, _, versionstate, policy, result.query.reference.name))
    val observations = inspected.flatMap(_._1)
    val warnings = inspected.flatMap(_._2) ++ Option.when(result.entries.isEmpty)(
      "Development source " + source.id + " has no project.yaml entry."
    ).toVector
    _inventory(source, observations, warnings, clock)
  }

  /**
   * Joins fixed-name, bounded development evidence queries by the exact
   * project root.  A carrier is considered only when the generated
   * descriptor declares it; matching leaf names never become discovery.
   */
  def inspectDevelopmentEvidenceQueries(
    source: InformationSourceDescriptor,
    projects: ResourceTreeQueryResult,
    descriptors: ResourceTreeQueryResult,
    carriers: ResourceTreeQueryResult,
    runtimeManifests: ResourceTreeQueryResult,
    versionstate: String,
    policy: LocalInspectionPolicy,
    clock: Clock
  ): LocalInformationInventory = {
    val descriptorbypath = descriptors.entries.map(x => x.relativePath -> x).toMap
    val carrierbypath = carriers.entries.map(x => x.relativePath -> x).toMap
    val manifestbypath = runtimeManifests.entries.map(x => x.relativePath -> x).toMap
    val inspected = projects.entries.map { project =>
      val root = _project_root(project.relativePath)
      _inspect_development_evidence(
        source,
        project,
        descriptorbypath.get(_development_path(root, "target/cncf.d/component-descriptor.json")),
        carrierbypath.get(_development_path(root, "target/cncf.d/component-knowledge.json")),
        manifestbypath.get(_development_path(root, "target/cncf.d/car-runtime-manifest.json")),
        versionstate,
        policy,
        projects.query.reference.name
      )
    }
    _inventory(source, inspected.flatMap(_._1), inspected.flatMap(_._2), clock)
  }

  def inspectDevelopmentSnapshot(
    source: InformationSourceDescriptor,
    snapshot: ResourceTreeSnapshot,
    versionstate: String,
    policy: LocalInspectionPolicy,
    clock: Clock
  ): LocalInformationInventory = {
    val result = snapshot.entries.find(_.relativePath == "project.yaml") match {
      case Some(entry) => _inspect_development(source, entry, versionstate, policy, snapshot.reference.name)
      case None => Vector.empty[LocalComponentObservation] -> Vector("Development source " + source.id + " has no project.yaml entry.")
    }
    _inventory(source, result._1, result._2, clock)
  }

  def inspectCarStorageSnapshot(
    source: InformationSourceDescriptor,
    snapshot: ResourceTreeSnapshot,
    versionstate: String,
    policy: LocalInspectionPolicy,
    clock: Clock
  ): LocalInformationInventory = {
    val artifacts = snapshot.entries.filter(_.relativePath.toLowerCase(java.util.Locale.ROOT).endsWith(".car"))
    val overflow = Option.when(artifacts.size > policy.maxCarArtifacts)(
      "CAR discovery was truncated at " + policy.maxCarArtifacts + " artifacts."
    ).toVector
    val results = artifacts.take(policy.maxCarArtifacts).map(_inspect_car(source, _, versionstate, policy, snapshot.reference.name))
    _inventory(source, results.collect { case Right(x) => x }, overflow ++ results.collect { case Left(x) => x }, clock)
  }

  private def _inventory(
    source: InformationSourceDescriptor,
    observations: Vector[LocalComponentObservation],
    warnings: Vector[String],
    clock: Clock
  ): LocalInformationInventory =
    LocalInformationInventory(
      Vector(source), observations, warnings.distinct, clock.instant(),
      Map(source.id -> warnings.distinct)
    )

  private def _inspect_development_evidence(
    source: InformationSourceDescriptor,
    project: org.goldenport.cncf.resource.ResourceTreeEntry,
    descriptorentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    carrierentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    runtimeentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    versionstate: String,
    policy: LocalInspectionPolicy,
    treename: String
  ): (Vector[LocalComponentObservation], Vector[String]) =
    if (project.byteSize > policy.maxMetadataBytes)
      Vector.empty[LocalComponentObservation] -> Vector(
        "Development source " + source.id + ": " + project.relativePath + " exceeds " + policy.maxMetadataBytes + " bytes."
      )
    else {
      val values = _project_yaml_values(new String(project.bytes.toArray, StandardCharsets.UTF_8))
      val projectname = values.get("project.component.name").orElse(values.get("project.name"))
      val projectversion = values.get("project.component.version")
      val parseddescriptor = descriptorentry match {
        case None => Right(None)
        case Some(entry) if entry.byteSize > policy.maxMetadataBytes => Left("Development Component descriptor exceeds " + policy.maxMetadataBytes + " bytes.")
        case Some(entry) => _strict_json_parser.parse(new String(entry.bytes.toArray, StandardCharsets.UTF_8))
          .left.map(_ => "Development Component descriptor is not valid JSON.")
          .map(json => Some(_car_descriptor(json)))
      }
      val knowledge = parseddescriptor match {
        case Left(reason) => ComponentKnowledgeIntegration.Rejected(reason)
        case Right(None) => ComponentKnowledgeIntegration.Absent
        case Right(Some(descriptor)) => _development_component_knowledge(descriptor, carrierentry, runtimeentry, policy)
      }
      val descriptor = parseddescriptor.toOption.flatten
      val diagnostics = Option.when(projectname.isEmpty)("project.yaml has no component name.").toVector ++
        Option.when(projectversion.isEmpty)("project.yaml has no component version.").toVector ++
        _knowledge_diagnostic(knowledge)
      Vector(LocalComponentObservation(
        source.id,
        source.sourceKind,
        descriptor.flatMap(_.componentName).orElse(projectname),
        descriptor.flatMap(_.organization).orElse(values.get("project.organization")),
        values.get("project.kind").orElse(Some("car")),
        descriptor.flatMap(_.version).orElse(projectversion),
        if (descriptor.nonEmpty) "development-component-descriptor" else "project-yaml",
        versionstate,
        "resource-tree:" + treename + "/" + project.relativePath,
        descriptor.flatMap(_.version),
        None,
        knowledge match {
          case ComponentKnowledgeIntegration.Admitted(_, carrier) => Some(carrier.sha256)
          case _ => None
        },
        diagnostics,
        knowledge
      )) -> Vector.empty[String]
    }

  private def _development_component_knowledge(
    descriptor: CarDescriptor,
    carrierentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    runtimeentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    policy: LocalInspectionPolicy
  ): ComponentKnowledgeIntegration.Result =
    descriptor.knowledgeDeclaration match {
      case Left(reason) => ComponentKnowledgeIntegration.Rejected(reason)
      case Right(None) => ComponentKnowledgeIntegration.Absent
      case Right(Some(carrier)) =>
        descriptor.expectedKnowledgeIdentity match {
          case None => ComponentKnowledgeIntegration.Rejected("Development Component knowledge declaration lacks a canonical Component identity and release.")
          case Some((componentid, release)) =>
            carrierentry match {
              case None => ComponentKnowledgeIntegration.Rejected("Development Component knowledge declaration has no target/cncf.d/component-knowledge.json evidence.")
              case Some(entry) if entry.byteSize > policy.maxMetadataBytes => ComponentKnowledgeIntegration.Rejected("Development Component knowledge carrier exceeds the metadata limit.")
              case Some(entry) =>
                _development_runtime_carrier_intact(runtimeentry, carrier.logicalPath, entry.bytes, policy) match {
                  case Left(reason) => ComponentKnowledgeIntegration.Rejected(reason)
                  case Right(_) => ComponentKnowledgeIntegration.admit(ComponentKnowledgeIntegration.Input(
                    Some(carrier), entry.bytes, componentid, release
                  ))
                }
            }
        }
    }

  private def _development_runtime_carrier_intact(
    runtimeentry: Option[org.goldenport.cncf.resource.ResourceTreeEntry],
    carrierpath: String,
    carrierbytes: Vector[Byte],
    policy: LocalInspectionPolicy
  ): Either[String, Unit] =
    runtimeentry match {
      case None => Left("Development Component knowledge carrier has no target/cncf.d/car-runtime-manifest.json evidence.")
      case Some(entry) if entry.byteSize > policy.maxMetadataBytes => Left("Development runtime manifest exceeds the metadata limit.")
      case Some(entry) =>
        _strict_json_parser.parse(new String(entry.bytes.toArray, StandardCharsets.UTF_8)).left.map(_ => "Development runtime manifest is not valid JSON.").flatMap { json =>
          val evidence = json.hcursor.get[Vector[Json]]("evidence").getOrElse(Vector.empty)
          val expectedpath = "target/cncf.d/" + carrierpath
          val matches = evidence.filter { item =>
            item.hcursor.get[String]("path").toOption.contains(expectedpath)
          }
          matches match {
            case Vector(item) if item.hcursor.get[String]("sha256").toOption.contains(_sha256(carrierbytes)) => Right(())
            case Vector(_) => Left("Development runtime manifest digest does not match the declared Component knowledge carrier.")
            case Vector() => Left("Development runtime manifest does not declare the Component knowledge carrier.")
            case _ => Left("Development runtime manifest declares the Component knowledge carrier more than once.")
          }
        }
    }

  private def _project_root(projectpath: String): String =
    projectpath.stripSuffix("project.yaml").stripSuffix("/")

  private def _development_path(root: String, child: String): String =
    if (root.isEmpty) child else root + "/" + child

  private def _inspect_development(
    source: InformationSourceDescriptor,
    entry: org.goldenport.cncf.resource.ResourceTreeEntry,
    versionstate: String,
    policy: LocalInspectionPolicy,
    treename: String
  ): (Vector[LocalComponentObservation], Vector[String]) =
    if (entry.byteSize > policy.maxMetadataBytes)
      Vector.empty[LocalComponentObservation] -> Vector(
        "Development source " + source.id + ": " + entry.relativePath + " exceeds " + policy.maxMetadataBytes + " bytes."
      )
    else {
      val values = _project_yaml_values(new String(entry.bytes.toArray, StandardCharsets.UTF_8))
      val name = values.get("project.component.name").orElse(values.get("project.name"))
      val version = values.get("project.component.version")
      val diagnostics = Option.when(name.isEmpty)("project.yaml has no component name.").toVector ++
        Option.when(version.isEmpty)("project.yaml has no component version.").toVector
      Vector(LocalComponentObservation(
        source.id, source.sourceKind, name, values.get("project.organization"),
        values.get("project.kind"), version, "project-yaml", versionstate,
        "resource-tree:" + treename + "/" + entry.relativePath,
        None, None, None, diagnostics
      )) -> Vector.empty[String]
    }

  private def _inspect_car(
    source: InformationSourceDescriptor,
    entry: org.goldenport.cncf.resource.ResourceTreeEntry,
    versionstate: String,
    policy: LocalInspectionPolicy,
    treename: String
  ): Either[String, LocalComponentObservation] = {
    val artifact = entry.relativePath
    if (entry.byteSize > policy.maxArtifactBytes)
      Left("CAR artifact " + artifact + " exceeds " + policy.maxArtifactBytes + " bytes.")
    else _descriptor(entry.bytes, policy.maxMetadataBytes).flatMap { descriptorjson =>
      val segments = artifact.split("/").toVector
      val pathname = segments.headOption
      val pathversion = segments.drop(1).headOption
      val descriptor = _car_descriptor(descriptorjson)
      val name = descriptor.componentName
      val version = descriptor.version
      if (name.isEmpty) Left("component-descriptor.json has no component name.")
      else if (descriptor.declaredVersion && version.isEmpty) Left("component-descriptor.json version must be a non-empty string when declared.")
      else if (_path_name_conflicts(descriptor, pathname)) Left("Component name conflicts: descriptor=" + descriptor.pathName.get + ", path=" + pathname.get + ".")
      else if (version.nonEmpty && pathversion.nonEmpty && version != pathversion) Left("Component version conflicts: descriptor=" + version.get + ", path=" + pathversion.get + ".")
      else {
        val evidence = if (version.nonEmpty) "component-descriptor" else if (pathversion.nonEmpty) "repository-path" else "absent"
        val knowledge = _component_knowledge(descriptor, entry.bytes, policy.maxMetadataBytes)
        val diagnostics = Option.when(version.isEmpty && pathversion.nonEmpty)(
          "component-descriptor.json has no version; repository path version is retained as supported legacy path evidence."
        ).toVector ++ _knowledge_diagnostic(knowledge)
        Right(LocalComponentObservation(
          source.id, source.sourceKind, name, descriptor.organization, Some("car"), version.orElse(pathversion),
          evidence, versionstate, s"resource-tree:$treename/$artifact", version, pathversion,
          Some(_sha256(entry.bytes)), diagnostics, knowledge
        ))
      }
    }.left.map(x => s"CAR artifact $artifact was rejected: $x")
  }

  private final case class CarDescriptor(
    componentName: Option[String],
    organization: Option[String],
    version: Option[String],
    declaredVersion: Boolean,
    pathName: Option[String],
    expectedKnowledgeIdentity: Option[(ComponentId, String)],
    knowledgeDeclaration: Either[String, Option[ComponentKnowledgeCarrier]]
  )

  private val _strict_json_parser = JawnParser(allowDuplicateKeys = false)

  private def _descriptor(bytes: Vector[Byte], maximum: Int): Either[String, Json] =
    _archive_entry(bytes, "component-descriptor.json", maximum).flatMap {
      case None => Left("CAR has no component-descriptor.json entry.")
      case Some(content) => _strict_json_parser.parse(new String(content.toArray, StandardCharsets.UTF_8))
        .left.map(_ => "component-descriptor.json is not valid JSON.")
    }

  /**
   * This is an exact archive lookup, not discovery.  Callers supply the only
   * entry name they may read; duplicate named entries fail closed.
   */
  private def _archive_entry(
    bytes: Vector[Byte],
    logicalpath: String,
    maximum: Int
  ): Either[String, Option[Vector[Byte]]] = try {
    val input = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.toArray)))
    try {
      var found: Option[Vector[Byte]] = None
      var entry = input.getNextEntry
      while (entry != null) {
        if (entry.getName == logicalpath) {
          if (found.nonEmpty) return Left(s"CAR has duplicate $logicalpath entries.")
          else if (entry.getSize > maximum) return Left(s"$logicalpath exceeds $maximum bytes.")
          else {
            val content = input.readNBytes(maximum + 1)
            if (content.length > maximum) return Left(s"$logicalpath exceeds $maximum bytes.")
            found = Some(content.toVector)
          }
        }
        entry = input.getNextEntry
      }
      Right(found)
    } finally input.close()
  } catch {
    case NonFatal(_) => Left("CAR archive is unreadable.")
  }

  private def _car_descriptor(json: Json): CarDescriptor = {
    val root = json.asObject.getOrElse(JsonObject.empty)
    val canonical = root("component").flatMap(_.asObject)
    val namespace = canonical.flatMap(_string(_, "namespace"))
    val id = canonical.flatMap(_string(_, "id"))
    val canonicalversion = canonical.flatMap(_string(_, "version"))
    val legacyname = _string(root, "name").orElse(root("component").flatMap(_.asString).flatMap(_trimmed))
    val legacyversion = _string(root, "version")
    val artifact = root("extensions").flatMap(_.asObject).flatMap(_string(_, "artifact"))
    val knowledge = root("componentKnowledge") match {
      case None => Right(None)
      case Some(value) => ComponentKnowledgeCarrierCodec.decodeC(value.noSpaces).toOption
        .toRight("component-descriptor.json componentKnowledge declaration is invalid.")
        .map(Some(_))
    }
    val identity = for {
      ns <- namespace
      localid <- id
      release <- canonicalversion
    } yield ComponentId(ns + "." + localid) -> release
    CarDescriptor(
      id.orElse(legacyname),
      namespace,
      canonicalversion.orElse(legacyversion),
      canonical.exists(_.contains("version")) || root.contains("version"),
      artifact.orElse(if (canonical.isEmpty) id.orElse(legacyname) else None),
      identity,
      knowledge
    )
  }

  private def _component_knowledge(
    descriptor: CarDescriptor,
    archive: Vector[Byte],
    maximum: Int
  ): ComponentKnowledgeIntegration.Result = descriptor.knowledgeDeclaration match {
    case Left(reason) => ComponentKnowledgeIntegration.Rejected(reason)
    case Right(None) => ComponentKnowledgeIntegration.Absent
    case Right(Some(carrier)) => descriptor.expectedKnowledgeIdentity match {
      case None => ComponentKnowledgeIntegration.Rejected(
        "declared Component knowledge requires canonical component namespace, id, and version."
      )
      case Some((componentid, release)) => _archive_entry(archive, carrier.logicalPath, maximum) match {
        case Left(reason) => ComponentKnowledgeIntegration.Rejected(reason)
        case Right(None) => ComponentKnowledgeIntegration.Rejected(
          s"declared archive entry ${carrier.logicalPath} is missing."
        )
        case Right(Some(bytes)) => ComponentKnowledgeIntegration.admit(
          ComponentKnowledgeIntegration.Input(Some(carrier), bytes, componentid, release)
        )
      }
    }
  }

  private def _path_name_conflicts(descriptor: CarDescriptor, pathname: Option[String]): Boolean =
    descriptor.pathName.nonEmpty && pathname.nonEmpty && descriptor.pathName != pathname

  private def _knowledge_diagnostic(result: ComponentKnowledgeIntegration.Result): Vector[String] = result match {
    case ComponentKnowledgeIntegration.Rejected(reason) => Vector("Component knowledge carrier was rejected: " + reason)
    case _ => Vector.empty
  }

  private def _string(value: JsonObject, field: String): Option[String] =
    value(field).flatMap(_.asString).flatMap(_trimmed)

  private def _trimmed(value: String): Option[String] = Option(value.trim).filter(_.nonEmpty)

  private def _sha256(bytes: Vector[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes.toArray).map(x => "%02x".format(x & 0xff)).mkString

  private def _project_yaml_values(content: String): Map[String, String] = {
    val pattern = """^(\s*)([A-Za-z0-9_.-]+):(?:\s*(.*))?$""".r
    content.linesIterator.foldLeft((Vector.empty[(Int, String)], Map.empty[String, String])) { case ((stack, values), line) =>
      line match {
        case pattern(spaces, key, raw) =>
          val parent = stack.reverse.dropWhile(_._1 >= spaces.length).reverse
          val value = Option(raw).map(_.trim).getOrElse("")
          val path = (parent.map(_._2) :+ key).mkString(".")
          if (value.isEmpty) (parent :+ (spaces.length -> key), values) else (parent, values.updated(path, _unquote(value)))
        case _ => (stack, values)
      }
    }._2
  }

  private def _unquote(value: String): String =
    if (value.length >= 2 && Set('"', '\'').contains(value.head) && value.last == value.head) value.substring(1, value.length - 1) else value
}
