package org.simplemodeling.textus.cbdsupport.runtime

import java.io.{BufferedInputStream, ByteArrayInputStream}
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.zip.ZipInputStream
import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse
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
  diagnostics: Vector[String]
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
    else _descriptor(entry.bytes, policy.maxMetadataBytes).flatMap { descriptor =>
      val segments = artifact.split("/").toVector
      val pathname = segments.headOption
      val pathversion = segments.drop(1).headOption
      val name = _name(descriptor)
      val versionfield = descriptor.hcursor.downField("version").focus
      val version = descriptor.hcursor.get[String]("version").toOption.map(_.trim).filter(_.nonEmpty)
      if (name.isEmpty) Left("component-descriptor.json has no component name.")
      else if (versionfield.nonEmpty && version.isEmpty) Left("component-descriptor.json version must be a non-empty string when declared.")
      else if (pathname.nonEmpty && name != pathname) Left("Component name conflicts: descriptor=" + name.get + ", path=" + pathname.get + ".")
      else if (version.nonEmpty && pathversion.nonEmpty && version != pathversion) Left("Component version conflicts: descriptor=" + version.get + ", path=" + pathversion.get + ".")
      else {
        val evidence = if (version.nonEmpty) "component-descriptor" else if (pathversion.nonEmpty) "repository-path" else "absent"
        val diagnostics = Option.when(version.isEmpty && pathversion.nonEmpty)(
          "component-descriptor.json has no version; repository path version is retained as supported legacy path evidence."
        ).toVector
        Right(LocalComponentObservation(
          source.id, source.sourceKind, name, None, Some("car"), version.orElse(pathversion),
          evidence, versionstate, s"resource-tree:$treename/$artifact", version, pathversion,
          Some(MessageDigest.getInstance("SHA-256").digest(entry.bytes.toArray).map(x => "%02x".format(x & 0xff)).mkString),
          diagnostics
        ))
      }
    }.left.map(x => s"CAR artifact $artifact was rejected: $x")
  }

  private def _descriptor(bytes: Vector[Byte], maximum: Int): Either[String, Json] = try {
    val input = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.toArray)))
    try Iterator.continually(input.getNextEntry).takeWhile(_ != null).find(_.getName == "component-descriptor.json") match {
      case None => Left("CAR has no component-descriptor.json entry.")
      case Some(entry) if entry.getSize > maximum => Left(s"component-descriptor.json exceeds $maximum bytes.")
      case Some(_) =>
        val content = input.readNBytes(maximum + 1)
        if (content.length > maximum) Left(s"component-descriptor.json exceeds $maximum bytes.")
        else parse(new String(content, StandardCharsets.UTF_8)).left.map(_ => "component-descriptor.json is not valid JSON.")
    } finally input.close()
  } catch {
    case NonFatal(_) => Left("CAR archive is unreadable.")
  }

  private def _name(json: Json): Option[String] = {
    val cursor = json.hcursor
    cursor.get[String]("name").toOption.orElse(cursor.get[String]("component").toOption)
      .orElse(cursor.downField("component").get[String]("name").toOption).map(_.trim).filter(_.nonEmpty)
  }

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
