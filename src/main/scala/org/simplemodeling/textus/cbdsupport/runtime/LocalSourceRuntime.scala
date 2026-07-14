package org.simplemodeling.textus.cbdsupport.runtime

import java.io.BufferedInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path}
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.zip.ZipFile
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final case class LocalInspectionPolicy(
  maxDevelopmentDirectories: Int = 16,
  maxCarArtifacts: Int = 512,
  maxDirectories: Int = 2048,
  maxDirectoryEntries: Int = 1024,
  maxDepth: Int = 6,
  maxMetadataBytes: Int = 1024 * 1024,
  maxArtifactBytes: Long = 512L * 1024L * 1024L
) {
  require(maxDevelopmentDirectories > 0, "Development-directory limit must be positive.")
  require(maxCarArtifacts > 0, "CAR artifact limit must be positive.")
  require(maxDirectories > 0, "Directory traversal limit must be positive.")
  require(maxDirectoryEntries > 0, "Directory entry limit must be positive.")
  require(maxDepth > 0, "Directory depth limit must be positive.")
  require(maxMetadataBytes > 0, "Metadata byte limit must be positive.")
  require(maxArtifactBytes > 0, "CAR artifact byte limit must be positive.")
}

object LocalInspectionPolicy {
  val DEFAULT: LocalInspectionPolicy = LocalInspectionPolicy()
}

final case class LocalPathSource(
  descriptor: InformationSourceDescriptor,
  root: Path,
  inspectionRoot: Path,
  versionState: String
)

final case class LocalInformationSourceConfiguration(
  developmentSources: Vector[LocalPathSource],
  carStorageSources: Vector[LocalPathSource],
  warnings: Vector[String]
) {
  def sources: Vector[LocalPathSource] = developmentSources ++ carStorageSources
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
  observedAt: Instant
)

object LocalInformationSourceConfig {
  private val _reserved_source_ids = Set("local-car", "cache-car")

  def parse(
    developmentdirectories: Option[String],
    localcarroot: Option[String],
    cachecarroot: Option[String],
    homeroot: Path,
    policy: LocalInspectionPolicy = LocalInspectionPolicy.DEFAULT
  ): LocalInformationSourceConfiguration = {
    val developmentresults = developmentdirectories.toVector
      .flatMap(_.split(","))
      .map(_.trim)
      .filter(_.nonEmpty)
      .take(policy.maxDevelopmentDirectories + 1)
      .zipWithIndex
      .map { case (entry, index) => _development_source(entry, index) }
    val developmentoverflow = Option.when(developmentresults.size > policy.maxDevelopmentDirectories) {
      s"Development-directory configuration exceeds the limit of ${policy.maxDevelopmentDirectories}."
    }.toVector
    val boundeddevelopment = developmentresults.take(policy.maxDevelopmentDirectories)
    val validdevelopment = boundeddevelopment.collect { case Right(source) => source }
    val developmentwarnings = boundeddevelopment.collect { case Left(warning) => warning }
    val (development, duplicatewarnings) = _deduplicate(validdevelopment)

    val localvalue = localcarroot.getOrElse(homeroot.resolve(".cncf/local").toString)
    val cachevalue = cachecarroot.getOrElse(homeroot.resolve(".cncf/cache").toString)
    val localresult = _car_storage_source(
      "local-car",
      localvalue,
      "repository/car",
      VersionAvailabilityState.LOCAL_PUBLISHED,
      localcarroot.fold(InformationSourceAuthorization.CANONICAL_STORAGE_ROOT)(_ => InformationSourceAuthorization.EXPLICIT_PATH_ALLOWLIST)
    )
    val cacheresult = _car_storage_source(
      "cache-car",
      cachevalue,
      "car",
      VersionAvailabilityState.CACHED,
      cachecarroot.fold(InformationSourceAuthorization.CANONICAL_STORAGE_ROOT)(_ => InformationSourceAuthorization.EXPLICIT_PATH_ALLOWLIST)
    )
    val carresults = Vector(localresult, cacheresult)
    LocalInformationSourceConfiguration(
      development,
      carresults.collect { case Right(source) => source },
      (developmentoverflow ++ developmentwarnings ++ duplicatewarnings ++
        carresults.collect { case Left(warning) => warning }).distinct
    )
  }

  private def _development_source(entry: String, index: Int): Either[String, LocalPathSource] = {
    val pair = entry.split("=", 2)
    val pathvalue = if (pair.length == 2) pair(1).trim else pair(0).trim
    val defaultid = Path.of(pathvalue).getFileName match {
      case null => s"development-${index + 1}"
      case name => name.toString.replaceAll("[^A-Za-z0-9._-]", "-")
    }
    val id = if (pair.length == 2) pair(0).trim else defaultid
    if (!id.matches("[A-Za-z0-9._-]+"))
      Left(s"Development-directory entry ${index + 1} has an invalid source ID.")
    else _canonical_directory(pathvalue).map { root =>
      LocalPathSource(
        InformationSourceDescriptor(
          id,
          InformationSourceKind.DEVELOPMENT_DIRECTORY,
          root.toString,
          300 + index,
          true,
          InformationSourceAuthorization.EXPLICIT_PATH_ALLOWLIST
        ),
        root,
        root,
        VersionAvailabilityState.WORKING
      )
    }.left.map(reason => s"Development-directory entry ${index + 1} was rejected: $reason")
  }

  private def _car_storage_source(
    id: String,
    value: String,
    layout: String,
    versionstate: String,
    authorization: String
  ): Either[String, LocalPathSource] =
    _canonical_directory(value).map { root =>
      LocalPathSource(
        InformationSourceDescriptor(
          id,
          InformationSourceKind.CAR_STORAGE,
          root.toString,
          if (versionstate == VersionAvailabilityState.LOCAL_PUBLISHED) 400 else 500,
          true,
          authorization
        ),
        root,
        root.resolve(layout),
        versionstate
      )
    }.left.map(reason => s"CAR storage source $id was rejected: $reason")

  private def _canonical_directory(value: String): Either[String, Path] = {
    try {
      val normalized = Path.of(value).toAbsolutePath.normalize()
      if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS))
        Left("path does not exist")
      else if (Files.isSymbolicLink(normalized))
        Left("symbolic-link roots are not allowed")
      else if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS))
        Left("path is not a directory")
      else {
        val real = normalized.toRealPath()
        if (real != normalized) Left("symbolic path components are not allowed")
        else Right(real)
      }
    } catch {
      case NonFatal(_) => Left("path is invalid or unreadable")
    }
  }

  private def _deduplicate(
    sources: Vector[LocalPathSource]
  ): (Vector[LocalPathSource], Vector[String]) = {
    val initial = (Vector.empty[LocalPathSource], _reserved_source_ids, Vector.empty[String])
    val (accepted, _, warnings) = sources.foldLeft(initial) { case ((xs, ids, ws), source) =>
      if (ids.contains(source.descriptor.id))
        (xs, ids, ws :+ s"Development-directory source ${source.descriptor.id} was rejected because its source ID is reserved or duplicated.")
      else
        (xs :+ source, ids + source.descriptor.id, ws)
    }
    (accepted, warnings)
  }
}

object LocalInformationSourceInventory {
  def inspect(
    configuration: LocalInformationSourceConfiguration,
    policy: LocalInspectionPolicy = LocalInspectionPolicy.DEFAULT,
    clock: Clock = Clock.systemUTC()
  ): LocalInformationInventory = {
    val developmentresults = configuration.developmentSources.map(_inspect_development(_, policy))
    val carresults = configuration.carStorageSources.map(_inspect_car_storage(_, policy))
    LocalInformationInventory(
      configuration.sources.map(_.descriptor),
      developmentresults.flatMap(_._1) ++ carresults.flatMap(_._1),
      (configuration.warnings ++ developmentresults.flatMap(_._2) ++ carresults.flatMap(_._2)).distinct,
      clock.instant()
    )
  }

  private def _inspect_development(
    source: LocalPathSource,
    policy: LocalInspectionPolicy
  ): (Vector[LocalComponentObservation], Vector[String]) = {
    val projectyaml = source.inspectionRoot.resolve("project.yaml")
    if (!Files.isRegularFile(projectyaml, LinkOption.NOFOLLOW_LINKS))
      Vector.empty -> Vector(s"Development source ${source.descriptor.id} has no regular project.yaml file.")
    else _read_bounded(projectyaml, policy.maxMetadataBytes) match {
      case Left(warning) => Vector.empty -> Vector(s"Development source ${source.descriptor.id}: $warning")
      case Right(content) =>
        val values = _project_yaml_values(content)
        val name = values.get("project.component.name").orElse(values.get("project.name"))
        val version = values.get("project.component.version")
        val diagnostics = (
          Option.when(name.isEmpty)("project.yaml does not declare project.component.name or project.name.").toVector ++
            Option.when(version.isEmpty)("project.yaml does not declare project.component.version.").toVector
        )
        val observation = LocalComponentObservation(
          source.descriptor.id,
          source.descriptor.sourceKind,
          name,
          values.get("project.organization"),
          values.get("project.kind"),
          version,
          "project-yaml",
          source.versionState,
          projectyaml.toUri.toString,
          None,
          None,
          None,
          diagnostics
        )
        Vector(observation) -> Vector.empty
    }
  }

  private def _inspect_car_storage(
    source: LocalPathSource,
    policy: LocalInspectionPolicy
  ): (Vector[LocalComponentObservation], Vector[String]) = {
    if (!Files.isDirectory(source.inspectionRoot, LinkOption.NOFOLLOW_LINKS))
      Vector.empty -> Vector(s"CAR storage source ${source.descriptor.id} has no ${source.inspectionRoot} directory.")
    else {
      val (paths, discoverywarnings) = _discover_car_files(source.inspectionRoot, policy)
      val observations = paths.map(_inspect_car(source, _, policy))
      observations.collect { case Right(observation) => observation } ->
        (discoverywarnings ++ observations.collect { case Left(warning) => warning })
    }
  }

  private def _inspect_car(
    source: LocalPathSource,
    path: Path,
    policy: LocalInspectionPolicy
  ): Either[String, LocalComponentObservation] = {
    try {
      _sha256(path, policy.maxArtifactBytes).map { checksum =>
        val relative = source.inspectionRoot.relativize(path)
        val segments = relative.iterator().asScala.map(_.toString).toVector
        val pathname = segments.headOption
        val pathversion = segments.drop(1).headOption
        val descriptorresult = _car_descriptor(path, policy.maxMetadataBytes)
        val descriptor = descriptorresult.toOption
        val descriptorname = descriptor.flatMap(_descriptor_name)
        val descriptorversion = descriptor.flatMap(_.hcursor.get[String]("version").toOption).map(_.trim).filter(_.nonEmpty)
        val name = descriptorname.orElse(pathname)
        val version = descriptorversion.orElse(pathversion)
        val versionevidence = if (descriptorversion.nonEmpty) "component-descriptor" else if (pathversion.nonEmpty) "repository-path" else "absent"
        val diagnostics = (
          descriptorresult.left.toOption.toVector ++
            Option.when(descriptorname.isEmpty && pathname.nonEmpty)("component-descriptor.json has no component name; repository path name is retained as path evidence.").toVector ++
            Option.when(descriptorversion.isEmpty && pathversion.nonEmpty)("component-descriptor.json has no version; repository path version is retained as path evidence.").toVector ++
            Option.when(descriptorname.nonEmpty && pathname.nonEmpty && descriptorname != pathname)(s"Component name conflict: descriptor=${descriptorname.get}, path=${pathname.get}.").toVector ++
            Option.when(descriptorversion.nonEmpty && pathversion.nonEmpty && descriptorversion != pathversion)(s"Component version conflict: descriptor=${descriptorversion.get}, path=${pathversion.get}.").toVector
        )
        LocalComponentObservation(
          source.descriptor.id,
          source.descriptor.sourceKind,
          name,
          None,
          Some("car"),
          version,
          versionevidence,
          source.versionState,
          path.toUri.toString,
          descriptorversion,
          pathversion,
          Some(checksum),
          diagnostics
        )
      }
    } catch {
      case NonFatal(exception) => Left(s"CAR artifact ${path.getFileName} could not be inspected: ${exception.getClass.getSimpleName}.")
    }
  }

  private def _discover_car_files(
    root: Path,
    policy: LocalInspectionPolicy
  ): (Vector[Path], Vector[String]) = {
    val files = ArrayBuffer.empty[Path]
    val warnings = ArrayBuffer.empty[String]
    var directorycount = 0
    var truncated = false

    def _walk_(directory: Path, depth: Int): Unit = {
      if (files.size >= policy.maxCarArtifacts || directorycount >= policy.maxDirectories) {
        truncated = true
      } else {
        directorycount += 1
        val stream = Files.newDirectoryStream(directory)
        val entries = try stream.iterator().asScala.take(policy.maxDirectoryEntries + 1).toVector
        finally stream.close()
        if (entries.size > policy.maxDirectoryEntries) {
          truncated = true
          warnings += s"Directory entry limit reached at $directory."
        }
        entries.take(policy.maxDirectoryEntries).sortBy(_.getFileName.toString).foreach { entry =>
          if (files.size >= policy.maxCarArtifacts) truncated = true
          else if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
            if (depth < policy.maxDepth) _walk_(entry, depth + 1)
            else {
              truncated = true
              warnings += s"Directory depth limit reached at $entry."
            }
          }
          else if (
            Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS) &&
              entry.getFileName.toString.toLowerCase(java.util.Locale.ROOT).endsWith(".car")
          ) files += entry
        }
      }
    }

    try _walk_(root, 0)
    catch {
      case NonFatal(exception) => warnings += s"CAR discovery failed at $root: ${exception.getClass.getSimpleName}."
    }
    if (truncated)
      warnings += s"CAR discovery was truncated at ${policy.maxCarArtifacts} artifacts, ${policy.maxDirectories} directories, depth ${policy.maxDepth}, or ${policy.maxDirectoryEntries} entries per directory."
    files.toVector.sortBy(_.toString) -> warnings.toVector.distinct
  }

  private def _car_descriptor(path: Path, maxbytes: Int): Either[String, Json] = {
    try {
      val zip = new ZipFile(path.toFile)
      try {
        Option(zip.getEntry("component-descriptor.json")) match {
          case None => Left("CAR has no component-descriptor.json entry.")
          case Some(entry) if entry.getSize > maxbytes => Left(s"component-descriptor.json exceeds $maxbytes bytes.")
          case Some(entry) =>
            val input = new BufferedInputStream(zip.getInputStream(entry))
            val bytes = try input.readNBytes(maxbytes + 1) finally input.close()
            if (bytes.length > maxbytes) Left(s"component-descriptor.json exceeds $maxbytes bytes.")
            else parse(new String(bytes, StandardCharsets.UTF_8)).left.map(_ => "component-descriptor.json is not valid JSON.")
        }
      } finally zip.close()
    } catch {
      case NonFatal(_) => Left("CAR archive is unreadable.")
    }
  }

  private def _descriptor_name(descriptor: Json): Option[String] = {
    val cursor = descriptor.hcursor
    cursor.get[String]("name").toOption
      .orElse(cursor.get[String]("component").toOption)
      .orElse(cursor.downField("component").get[String]("name").toOption)
      .map(_.trim).filter(_.nonEmpty)
  }

  private def _sha256(path: Path, maxbytes: Long): Either[String, String] = {
    val digest = MessageDigest.getInstance("SHA-256")
    val input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)
    val buffer = Array.ofDim[Byte](8192)
    try {
      var total = 0L
      var complete = false
      var oversized = false
      while (!complete && !oversized) {
        val remaining = maxbytes - total
        if (remaining == 0) {
          oversized = input.read() >= 0
          complete = !oversized
        } else {
          val requested = math.min(buffer.length.toLong, remaining).toInt
          val count = input.read(buffer, 0, requested)
          if (count < 0) complete = true
          else if (count > 0) {
            digest.update(buffer, 0, count)
            total += count
          }
        }
      }
      if (oversized) Left(s"CAR artifact ${path.getFileName} exceeds $maxbytes bytes.")
      else Right(digest.digest().map(byte => f"${byte & 0xff}%02x").mkString)
    } finally input.close()
  }

  private def _read_bounded(path: Path, maxbytes: Int): Either[String, String] = {
    try {
      if (Files.size(path) > maxbytes) Left(s"${path.getFileName} exceeds $maxbytes bytes.")
      else Right(Files.readString(path, StandardCharsets.UTF_8))
    } catch {
      case NonFatal(_) => Left(s"${path.getFileName} is unreadable.")
    }
  }

  private def _project_yaml_values(content: String): Map[String, String] = {
    val keyvalue = """^(\s*)([A-Za-z0-9_.-]+):(?:\s*(.*))?$""".r
    val initial = (Vector.empty[(Int, String)], Map.empty[String, String])
    content.linesIterator.foldLeft(initial) { case ((stack, values), line) =>
      if (line.trim.isEmpty || line.trim.startsWith("#")) (stack, values)
      else line match {
        case keyvalue(spaces, key, rawvalue) =>
          val indent = spaces.length
          val parent = stack.reverse.dropWhile(_._1 >= indent).reverse
          val path = (parent.map(_._2) :+ key).mkString(".")
          val value = Option(rawvalue).map(_.trim).getOrElse("")
          if (value.isEmpty) (parent :+ (indent -> key), values)
          else (parent, values.updated(path, _unquote(value)))
        case _ => (stack, values)
      }
    }._2
  }

  private def _unquote(value: String): String =
    if (value.length >= 2 && Set('"', '\'').contains(value.head) && value.last == value.head)
      value.substring(1, value.length - 1)
    else value
}
