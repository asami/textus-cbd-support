package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, LinkOption, Path, SimpleFileVisitor}
import java.security.MessageDigest
import java.util.Arrays

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/*
 * @since   Sep. 27, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelVerifiedSourceSnapshot(
  artifactId: String,
  packageRelativePath: String,
  required: Boolean,
  bytes: Option[Vector[Byte]]
)

private[runtime] final case class InternalModelVerifiedRealization(
  artifactId: String,
  role: String,
  packageRelativePath: String,
  required: Boolean,
  dependencies: Vector[String],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedRealizationPackage(
  realization: InternalModelVerifiedRealization,
  sourceSnapshots: Vector[InternalModelVerifiedSourceSnapshot]
)

private[runtime] final case class InternalModelVerifiedProjection(
  artifactId: String,
  role: String,
  packageRelativePath: String,
  required: Boolean,
  dependencies: Vector[String],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedProjectionContinuityPackage(
  realizationPackage: InternalModelVerifiedRealizationPackage,
  projection: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelVerifiedDecision(
  artifactId: String,
  role: String,
  packageRelativePath: String,
  required: Boolean,
  dependencies: Vector[String],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedDecisionPackage(
  realizationPackage: InternalModelVerifiedRealizationPackage,
  decision: InternalModelVerifiedDecision
)

/** Validates the closed, project-bound V1 internal-model package structure. */
object InternalModelPackageValidator {
  private final case class Artifact(
    id: String,
    role: String,
    path: String,
    required: Boolean,
    sha256: String,
    dependencies: Vector[String]
  )

  private final case class VerifiedArtifact(artifact: Artifact, bytes: Option[Vector[Byte]])

  private val _manifest_fields = Set(
    "artifacts", "lifecycleState", "packageDigest", "packageId", "projectId", "projectNamespace", "revision", "schemaVersion"
  )
  private val _artifact_fields = Set("artifactId", "dependsOn", "path", "required", "role", "sha256")
  private val _roles = Set("resume", "source-snapshot", "decision", "open-issue", "realization", "projection", "approval", "validation")
  private val _token_pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r
  private val _path_segment_pattern = "[A-Za-z0-9][A-Za-z0-9._-]*".r
  private val _uuid_pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def validateStructure(projectRoot: Path): Consequence[Unit] =
    try {
      _validate(projectRoot).fold(Consequence.operationInvalid, _ => Consequence.unit)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedSourceSnapshots(projectRoot: Path): Consequence[Vector[InternalModelVerifiedSourceSnapshot]] =
    try {
      _validate(projectRoot).map(_source_snapshots).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedPresentRealization(projectRoot: Path): Consequence[InternalModelVerifiedRealizationPackage] =
    try {
      _validate(projectRoot).flatMap(_present_realization).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedProjectionContinuity(projectRoot: Path): Consequence[InternalModelVerifiedProjectionContinuityPackage] =
    try {
      _validate(projectRoot).flatMap(_projection_continuity).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedDecisionRecords(projectRoot: Path): Consequence[InternalModelVerifiedDecisionPackage] =
    try {
      _validate(projectRoot).flatMap(_decision_records).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _validate(projectroot: Path): Either[String, Vector[VerifiedArtifact]] =
    for {
      root <- _project_root(projectroot)
      identity <- _project_identity(root)
      packageroot <- _package_root(root)
      manifestbytes <- _regular_bytes(packageroot.resolve("manifest.yaml"), "manifest.yaml")
      manifest <- _manifest(manifestbytes)
      artifacts <- _artifacts(manifest, identity)
      _ <- _package_digest(manifest)
      _ <- _inventory_order(artifacts)
      verified <- _filesystem_inventory(packageroot, artifacts)
    } yield verified

  private def _project_root(projectroot: Path): Either[String, Path] = {
    val root = projectroot.toAbsolutePath.normalize
    if Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(root) then Right(root)
    else Left("project root must be a non-symbolic directory")
  }

  private def _project_identity(projectroot: Path): Either[String, (String, String)] =
    for {
      bytes <- _regular_bytes(projectroot.resolve("project.yaml"), "project.yaml")
      content <- _decode_utf8(bytes, "project.yaml")
      values <- _yaml_project_values(content)
      namespace <- _source_token(values, "namespace")
      id <- _source_token(values, "id")
    } yield namespace -> id

  private def _yaml_project_values(content: String): Either[String, java.util.Map[?, ?]] =
    try {
      val options = new LoaderOptions()
      options.setAllowDuplicateKeys(false)
      options.setMaxAliasesForCollections(0)
      options.setAllowRecursiveKeys(false)
      val parsed = new Yaml(new SafeConstructor(options)).load[Any](content)
      parsed match {
        case root: java.util.Map[?, ?] =>
          root.get("project") match {
            case project: java.util.Map[?, ?] => Right(project)
            case _ => Left("project.yaml must contain a project mapping")
          }
        case _ => Left("project.yaml must be a mapping")
      }
    } catch {
      case NonFatal(_) => Left("project.yaml is not parseable YAML")
    }

  private def _source_token(values: java.util.Map[?, ?], key: String): Either[String, String] =
    values.get(key) match {
      case value: String if _is_token(value) => Right(value)
      case _ => Left(s"project.yaml project.$key must be an ASCII token")
    }

  private def _package_root(projectroot: Path): Either[String, Path] = {
    val packageroot = projectroot.resolve("src/main/internal-model").normalize
    if !packageroot.startsWith(projectroot) then Left("internal-model package path escapes the project root")
    else if Files.isSymbolicLink(packageroot) then Left("internal-model package root must not be a symbolic link")
    else if Files.isDirectory(packageroot, LinkOption.NOFOLLOW_LINKS) then {
      try {
        if packageroot.toRealPath().startsWith(projectroot.toRealPath()) then Right(packageroot)
        else Left("internal-model package root resolves outside the project root")
      } catch {
        case NonFatal(_) => Left("internal-model package root cannot be resolved")
      }
    } else Left("internal-model package root is missing or is not a directory")
  }

  private def _regular_bytes(path: Path, label: String): Either[String, Array[Byte]] =
    try {
      if Files.isSymbolicLink(path) then Left(s"$label must not be a symbolic link")
      else if !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) then Left(s"$label must be a regular file")
      else Right(Files.readAllBytes(path))
    } catch {
      case NonFatal(_) => Left(s"$label cannot be read")
    }

  private def _manifest(bytes: Array[Byte]): Either[String, JsonObject] =
    for {
      _ <- Either.cond(!_has_bom(bytes), (), "manifest.yaml must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "manifest.yaml")
      json <- _json_parser.parse(content).left.map(_ => "manifest.yaml must be valid JSON without duplicate members")
      root <- json.asObject.toRight("manifest.yaml root must be an object")
      _ <- Either.cond(Arrays.equals(bytes, _canonical_bytes(json)), (), "manifest.yaml is not canonical V1 JSON bytes")
      _ <- Either.cond(root.keys.toSet == _manifest_fields, (), "manifest.yaml root fields are not the closed V1 schema")
      _ <- _root_fields(root)
    } yield root

  private def _root_fields(root: JsonObject): Either[String, Unit] =
    for {
      schema <- _string(root, "schemaVersion")
      _ <- Either.cond(schema == "1.0", (), "manifest schemaVersion must be 1.0")
      packageid <- _string(root, "packageId")
      _ <- Either.cond(_uuid_pattern.matches(packageid), (), "manifest packageId must be a lowercase UUID")
      _ <- _token_field(root, "projectNamespace")
      _ <- _token_field(root, "projectId")
      _ <- _token_field(root, "lifecycleState")
      digest <- _string(root, "packageDigest")
      _ <- Either.cond(_is_digest(digest), (), "manifest packageDigest is invalid")
      revision <- root("revision").flatMap(_.asNumber).toRight("manifest revision must be a JSON integer")
      _ <- Either.cond(revision.toLong.exists(_ > 0) && _number_is_canonical_integer(revision), (), "manifest revision must be a positive JSON integer")
      _ <- root("artifacts").flatMap(_.asArray).toRight("manifest artifacts must be an array").map(_ => ())
    } yield ()

  private def _artifacts(root: JsonObject, identity: (String, String)): Either[String, Vector[Artifact]] =
    for {
      namespace <- _string(root, "projectNamespace")
      projectid <- _string(root, "projectId")
      _ <- Either.cond(namespace == identity._1 && projectid == identity._2, (), "manifest identity does not match project.yaml")
      values <- root("artifacts").flatMap(_.asArray).toRight("manifest artifacts must be an array")
      artifacts <- values.toVector.zipWithIndex.foldLeft[Either[String, Vector[Artifact]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          artifact <- _artifact(value, index)
        } yield collected :+ artifact
      }
      _ <- _unique(artifacts.map(_.id), "artifact IDs")
      _ <- _unique(artifacts.map(_.path), "artifact paths")
    } yield artifacts

  private def _artifact(value: Json, index: Int): Either[String, Artifact] =
    for {
      objectvalue <- value.asObject.toRight(s"artifact $index must be an object")
      _ <- Either.cond(objectvalue.keys.toSet == _artifact_fields, (), s"artifact $index fields are not the closed V1 schema")
      id <- _token_field(objectvalue, "artifactId")
      role <- _token_field(objectvalue, "role")
      _ <- Either.cond(_roles.contains(role), (), s"artifact $id has an unsupported V1 role")
      path <- _string(objectvalue, "path")
      _ <- Either.cond(_is_path(path), (), s"artifact $id has an unsafe path")
      _ <- Either.cond(path != "manifest.yaml", (), s"artifact $id must not name manifest.yaml")
      required <- objectvalue("required").flatMap(_.asBoolean).toRight(s"artifact $id required must be a JSON Boolean")
      digest <- _string(objectvalue, "sha256")
      _ <- Either.cond(_is_digest(digest), (), s"artifact $id sha256 is invalid")
      dependencies <- _string_array(objectvalue, "dependsOn", s"artifact $id dependsOn")
      _ <- Either.cond(dependencies.forall(_is_token), (), s"artifact $id dependsOn contains an invalid token")
      _ <- _unique(dependencies, s"artifact $id dependencies")
      _ <- Either.cond(dependencies == dependencies.sorted, (), s"artifact $id dependencies are not in canonical order")
    } yield Artifact(id, role, path, required, digest, dependencies)

  private def _package_digest(root: JsonObject): Either[String, Unit] =
    _string(root, "packageDigest").flatMap { declared =>
      val actual = _sha256(_canonical_bytes(root.remove("packageDigest").toJson))
      Either.cond(declared == actual, (), "manifest packageDigest does not match its canonical content")
    }

  private def _inventory_order(artifacts: Vector[Artifact]): Either[String, Unit] = {
    val ids = artifacts.map(_.id).toSet
    val unresolved = artifacts.flatMap(artifact => artifact.dependencies.filterNot(ids.contains).map(dependency => s"${artifact.id} -> $dependency"))
    if unresolved.nonEmpty then Left(s"artifact dependencies are unresolved: ${unresolved.mkString(", ")}")
    else if artifacts.exists(artifact => artifact.dependencies.contains(artifact.id)) then Left("artifact dependencies must not be self-referential")
    else {
      val remaining = artifacts.map(artifact => artifact.id -> artifact.dependencies.toSet).toMap
      _topological_order(remaining).flatMap { expected =>
        Either.cond(artifacts.map(_.id) == expected, (), "artifacts are not in deterministic topological order")
      }
    }
  }

  private def _topological_order(remaining: Map[String, Set[String]]): Either[String, Vector[String]] = {
    @annotation.tailrec
    def _loop_(pending: Map[String, Set[String]], completed: Vector[String]): Either[String, Vector[String]] =
      if pending.isEmpty then Right(completed)
      else {
        val available = pending.collect { case (id, dependencies) if dependencies.isEmpty => id }.toVector.sorted
        available.headOption match {
          case None => Left("artifact dependencies contain a cycle")
          case Some(next) =>
            _loop_(pending.removed(next).view.mapValues(_ - next).toMap, completed :+ next)
        }
      }
    _loop_(remaining, Vector.empty)
  }

  private def _filesystem_inventory(packageroot: Path, artifacts: Vector[Artifact]): Either[String, Vector[VerifiedArtifact]] =
    for {
      actualpaths <- _walk_regular_paths(packageroot)
      listedpaths = artifacts.map(_.path).toSet
      unlisted = actualpaths -- listedpaths
      _ <- Either.cond(unlisted.isEmpty, (), s"internal-model package contains unlisted files: ${unlisted.toVector.sorted.mkString(", ")}")
      verified <- artifacts.foldLeft[Either[String, Vector[VerifiedArtifact]]](Right(Vector.empty)) { (result, artifact) =>
        for {
          collected <- result
          entry <- _artifact_file(packageroot, actualpaths, artifact)
        } yield collected :+ entry
      }
      present = verified.collect { case VerifiedArtifact(artifact, Some(_)) => artifact.id }.toSet
      _ <- _present_dependencies(artifacts, present)
    } yield verified

  private def _source_snapshots(verified: Vector[VerifiedArtifact]): Vector[InternalModelVerifiedSourceSnapshot] =
    verified.collect {
      case VerifiedArtifact(artifact, bytes) if artifact.role == "source-snapshot" =>
        InternalModelVerifiedSourceSnapshot(artifact.id, artifact.path, artifact.required, bytes)
    }

  private def _present_realization(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedRealizationPackage] = {
    val realizations = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.role == "realization" =>
        InternalModelVerifiedRealization(
          artifactId = artifact.id,
          role = artifact.role,
          packageRelativePath = artifact.path,
          required = artifact.required,
          dependencies = artifact.dependencies,
          bytes = bytes
        )
    }
    realizations match {
      case Vector(realization) => Right(InternalModelVerifiedRealizationPackage(realization, _source_snapshots(verified)))
      case Vector() => Left("internal-model package has no present realization artifact")
      case _ => Left("internal-model package has multiple present realization artifacts")
    }
  }

  private def _projection_continuity(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedProjectionContinuityPackage] =
    for {
      realizationpackage <- _present_realization(verified)
      projection <- _present_projection(verified)
      _ <- Either.cond(
        projection.dependencies.contains(realizationpackage.realization.artifactId),
        (),
        "present projection artifact must depend on the selected realization artifact"
      )
    } yield InternalModelVerifiedProjectionContinuityPackage(realizationpackage, projection)

  private def _decision_records(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedDecisionPackage] =
    for {
      realizationpackage <- _present_realization(verified)
      decision <- _present_decision(verified)
      _ <- Either.cond(
        decision.dependencies.contains(realizationpackage.realization.artifactId),
        (),
        "present decision artifact must depend on the selected realization artifact"
      )
    } yield InternalModelVerifiedDecisionPackage(realizationpackage, decision)

  private def _present_projection(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedProjection] = {
    val projections = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.role == "projection" =>
        InternalModelVerifiedProjection(
          artifactId = artifact.id,
          role = artifact.role,
          packageRelativePath = artifact.path,
          required = artifact.required,
          dependencies = artifact.dependencies,
          bytes = bytes
        )
    }
    projections match {
      case Vector(projection) => Right(projection)
      case Vector() => Left("internal-model package has no present projection artifact")
      case _ => Left("internal-model package has multiple present projection artifacts")
    }
  }

  private def _present_decision(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedDecision] = {
    val decisions = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.role == "decision" =>
        InternalModelVerifiedDecision(
          artifactId = artifact.id,
          role = artifact.role,
          packageRelativePath = artifact.path,
          required = artifact.required,
          dependencies = artifact.dependencies,
          bytes = bytes
        )
    }
    decisions match {
      case Vector(decision) => Right(decision)
      case Vector() => Left("internal-model package has no present decision artifact")
      case _ => Left("internal-model package has multiple present decision artifacts")
    }
  }

  private def _walk_regular_paths(packageroot: Path): Either[String, Set[String]] =
    try {
      var failure: Option[String] = None
      var paths = Set.empty[String]
      var directories = Set.empty[String]
      Files.walkFileTree(packageroot, new SimpleFileVisitor[Path] {
        override def preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult =
          if attributes.isSymbolicLink then {
            failure = Some(s"internal-model package contains a symbolic-link directory: ${_relative_path(packageroot, directory)}")
            FileVisitResult.TERMINATE
          } else {
            val relative = _relative_path(packageroot, directory)
            if relative.nonEmpty then directories = directories + relative
            FileVisitResult.CONTINUE
          }

        override def visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult =
          if attributes.isSymbolicLink then {
            failure = Some(s"internal-model package contains a symbolic link: ${_relative_path(packageroot, file)}")
            FileVisitResult.TERMINATE
          } else if !attributes.isRegularFile then {
            failure = Some(s"internal-model package contains a nonregular object: ${_relative_path(packageroot, file)}")
            FileVisitResult.TERMINATE
          } else {
            val relative = _relative_path(packageroot, file)
            if relative != "manifest.yaml" then paths = paths + relative
            FileVisitResult.CONTINUE
          }

        override def visitFileFailed(file: Path, error: java.io.IOException): FileVisitResult = {
          failure = Some(s"internal-model package cannot inspect ${_relative_path(packageroot, file)}")
          FileVisitResult.TERMINATE
        }
      })
      failure match {
        case Some(reason) => Left(reason)
        case None => directories.find(directory => !paths.exists(_.startsWith(directory + "/"))) match {
          case Some(directory) => Left(s"internal-model package contains an empty directory: $directory")
          case None => Right(paths)
        }
      }
    } catch {
      case NonFatal(_) => Left("internal-model package inventory cannot be read")
    }

  private def _artifact_file(packageroot: Path, actualpaths: Set[String], artifact: Artifact): Either[String, VerifiedArtifact] =
    if !actualpaths.contains(artifact.path) then
      if !artifact.required then Right(VerifiedArtifact(artifact, None))
      else Left(s"required artifact ${artifact.id} is absent")
    else {
      val path = packageroot.resolve(artifact.path).normalize
      for {
        _ <- Either.cond(path.startsWith(packageroot), (), s"artifact ${artifact.id} escapes the package root")
        bytes <- _regular_bytes(path, s"artifact ${artifact.id}")
        _ <- Either.cond(_sha256(bytes) == artifact.sha256, (), s"artifact ${artifact.id} raw-byte digest does not match")
      } yield VerifiedArtifact(artifact, Some(bytes.toVector))
    }

  private def _present_dependencies(artifacts: Vector[Artifact], present: Set[String]): Either[String, Unit] =
    artifacts.find(artifact => present.contains(artifact.id) && artifact.dependencies.exists(dependency => !present.contains(dependency))) match {
      case Some(artifact) => Left(s"present artifact ${artifact.id} depends on an absent optional artifact")
      case None => Right(())
    }

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }

  private def _string(objectvalue: JsonObject, key: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"manifest $key must be a JSON string")

  private def _token_field(objectvalue: JsonObject, key: String): Either[String, String] =
    _string(objectvalue, key).flatMap(value => Either.cond(_is_token(value), value, s"manifest $key must be an ASCII token"))

  private def _string_array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    objectvalue(key).flatMap(_.asArray).toRight(s"$label must be an array").flatMap { values =>
      values.toVector.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          stringvalue <- value.asString.toRight(s"$label must contain only strings")
        } yield collected :+ stringvalue
      }
    }

  private def _unique(values: Vector[String], label: String): Either[String, Unit] =
    Either.cond(values.distinct.size == values.size, (), s"$label must be unique")

  private def _is_token(value: String): Boolean =
    _token_pattern.matches(value)

  private def _is_path(value: String): Boolean =
    value.nonEmpty && !value.startsWith("/") && !value.endsWith("/") &&
      !value.contains("\\") && !value.contains(":") &&
      value.split("/", -1).forall(segment => _path_segment_pattern.matches(segment) && segment != "." && segment != "..")

  private def _is_digest(value: String): Boolean =
    _digest_pattern.matches(value)

  private def _number_is_canonical_integer(number: io.circe.JsonNumber): Boolean =
    number.toLong.exists(value => number.toString == value.toString)

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private def _relative_path(root: Path, path: Path): String =
    root.relativize(path).toString.replace('\\', '/')
}
