package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, LinkOption, Path, SimpleFileVisitor}
import scala.util.control.NonFatal

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/*
 * @since   Sep. 27, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelVerifiedSourceSnapshot(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  bytes: Option[Vector[Byte]]
)

private[runtime] final case class InternalModelVerifiedRealization(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedRealizationPackage(
  realization: InternalModelVerifiedRealization,
  sourcesnapshots: Vector[InternalModelVerifiedSourceSnapshot]
)

private[runtime] final case class InternalModelVerifiedProjection(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedProjectionContinuityPackage(
  realizationpackage: InternalModelVerifiedRealizationPackage,
  projection: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelVerifiedArtifactContext(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  present: Boolean
)

private[runtime] final case class InternalModelVerifiedPackageContext(
  reference: InternalModelPackageReference,
  schemaversion: String,
  revision: Long,
  lifecyclestate: String,
  artifacts: Vector[InternalModelVerifiedArtifactContext]
)

private[runtime] final case class InternalModelVerifiedCandidateCmlProjectionPackage(
  packagecontext: InternalModelVerifiedPackageContext,
  continuitypackage: InternalModelVerifiedProjectionContinuityPackage,
  candidate: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelVerifiedSemanticDiffPackage(
  candidatepackage: InternalModelVerifiedCandidateCmlProjectionPackage,
  semanticdiff: InternalModelVerifiedProjection
)

private[runtime] final case class InternalModelVerifiedDecision(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedDecisionPackage(
  realizationpackage: InternalModelVerifiedRealizationPackage,
  decision: InternalModelVerifiedDecision
)

private[runtime] final case class InternalModelVerifiedOpenIssue(
  reference: InternalModelArtifactReference,
  path: String,
  required: Boolean,
  dependencies: Vector[InternalModelArtifactReference],
  bytes: Vector[Byte]
)

private[runtime] final case class InternalModelVerifiedOpenIssuePackage(
  realizationpackage: InternalModelVerifiedRealizationPackage,
  openissue: InternalModelVerifiedOpenIssue
)

/** Validates the closed, project-bound V2 carrier without granting semantic readiness. */
object InternalModelPackageValidator {
  private final case class Artifact(
    reference: InternalModelArtifactReference,
    path: String,
    required: Boolean,
    dependencies: Vector[InternalModelArtifactReference]
  )

  private final case class VerifiedArtifact(artifact: Artifact, bytes: Option[Vector[Byte]])

  private final case class CapturedPackage(
    context: InternalModelVerifiedPackageContext,
    artifacts: Vector[VerifiedArtifact]
  )

  private val _manifest_fields = Set(
    "artifacts", "lifecycleState", "packageId", "projectId", "projectNamespace", "revision", "schemaVersion"
  )
  private val _artifact_fields = Set("artifactId", "artifactRevision", "dependsOn", "path", "required", "role")
  private val _reference_fields = Set("artifactId", "artifactRevision", "role")
  private val _token_pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r
  private val _path_segment_pattern = "[A-Za-z0-9][A-Za-z0-9._-]*".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)

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

  /** Retains continuity and every raw inventory entry from one validated capture. */
  private[runtime] def verifiedContinuation(projectRoot: Path): Consequence[InternalModelVerifiedContinuationPackage] =
    try {
      val result = for {
        captured <- _captured_package(projectRoot)
        continuity <- _projection_continuity(captured.artifacts)
      } yield InternalModelVerifiedContinuationPackage(
        captured.context,
        continuity,
        captured.context.artifacts.zip(captured.artifacts).map { case (context, entry) =>
          InternalModelVerifiedContinuationArtifact(context, entry.bytes)
        }
      )
      result.fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  /** Checks declared typed metadata and presence without authenticating content or source identity. */
  private[runtime] def validateCapturedContext(context: InternalModelVerifiedPackageContext): Either[String, Unit] = {
    val artifacts = context.artifacts.map(entry => Artifact(entry.reference, entry.path, entry.required, entry.dependencies))
    for {
      _ <- Either.cond(context.schemaversion == "2.0", (), "captured schemaVersion must be 2.0")
      _ <- Either.cond(context.revision > 0, (), "captured carrier revision must be positive")
      _ <- Either.cond(_is_token(context.lifecyclestate), (), "captured lifecycleState must be an ASCII token")
      _ <- _inventory_metadata(artifacts)
      _ <- _inventory_order(artifacts)
      _ <- Either.cond(context.artifacts.forall(entry => !entry.required || entry.present), (), "captured required artifact is absent")
      _ <- _present_dependencies(artifacts, context.artifacts.filter(_.present).map(_.reference.artifactId).toSet)
    } yield ()
  }

  /** Selects continuity from the complete captured inventory without path access. */
  private[runtime] def selectCapturedContinuity(
    artifacts: Vector[InternalModelVerifiedContinuationArtifact]
  ): Either[String, InternalModelVerifiedProjectionContinuityPackage] = {
    val verified = artifacts.map { entry =>
      val context = entry.context
      VerifiedArtifact(
        Artifact(context.reference, context.path, context.required, context.dependencies),
        entry.bytes
      )
    }
    for {
      _ <- Either.cond(artifacts.forall(entry => entry.context.present == entry.bytes.nonEmpty), (), "captured artifact presence does not match supplied payload availability")
      _ <- Either.cond(artifacts.forall(entry => !entry.context.required || entry.context.present), (), "captured required artifact is absent")
      _ <- _inventory_metadata(verified.map(_.artifact))
      _ <- _inventory_order(verified.map(_.artifact))
      _ <- _present_dependencies(verified.map(_.artifact), artifacts.filter(_.context.present).map(_.context.reference.artifactId).toSet)
      continuity <- _projection_continuity(verified)
    } yield continuity
  }

  private[runtime] def verifiedCandidateCmlProjection(projectRoot: Path): Consequence[InternalModelVerifiedCandidateCmlProjectionPackage] =
    try {
      _captured_package(projectRoot).flatMap(_candidate_cml_projection).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedSemanticDiff(projectRoot: Path): Consequence[InternalModelVerifiedSemanticDiffPackage] =
    try {
      _captured_package(projectRoot).flatMap(_semantic_diff).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  /** Captures the carrier once and selects one explicit validation-role review artifact. */
  private[runtime] def verifiedCandidateReviewBinding(
    projectRoot: Path,
    reviewArtifact: InternalModelArtifactReference
  ): Consequence[InternalModelVerifiedCandidateReviewBindingPackage] =
    try {
      _captured_package(projectRoot).flatMap(_candidate_review_binding(_, reviewArtifact)).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  /** Captures one carrier and explicitly composes its selected review and approval artifacts. */
  private[runtime] def verifiedCandidateHumanApproval(
    projectRoot: Path,
    approvalArtifact: InternalModelArtifactReference,
    reviewArtifact: InternalModelArtifactReference
  ): Consequence[InternalModelVerifiedCandidateHumanApprovalPackage] =
    try {
      _captured_package(projectRoot).flatMap(_candidate_human_approval(_, approvalArtifact, reviewArtifact)).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedDecisionRecords(projectRoot: Path): Consequence[InternalModelVerifiedDecisionPackage] =
    try {
      _validate(projectRoot).flatMap(_decision_records).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def verifiedOpenIssueRecords(projectRoot: Path): Consequence[InternalModelVerifiedOpenIssuePackage] =
    try {
      _validate(projectRoot).flatMap(_open_issue_records).fold(Consequence.operationInvalid, Consequence.success)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model package validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _validate(projectroot: Path): Either[String, Vector[VerifiedArtifact]] =
    _captured_package(projectroot).map(_.artifacts)

  private def _captured_package(projectroot: Path): Either[String, CapturedPackage] =
    for {
      root <- _project_root(projectroot)
      identity <- _project_identity(root)
      packageroot <- _package_root(root)
      manifestbytes <- _regular_bytes(packageroot.resolve("manifest.yaml"), "manifest.yaml")
      manifest <- _manifest(manifestbytes)
      artifacts <- _artifacts(manifest, identity)
      _ <- _inventory_order(artifacts)
      verified <- _filesystem_inventory(packageroot, artifacts)
      context <- _package_context(manifest, verified)
    } yield CapturedPackage(context, verified)

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
      _ <- Either.cond(root.keys.toSet == _manifest_fields, (), "manifest.yaml root fields are not the closed V2 schema")
      _ <- _root_fields(root)
    } yield root

  private def _root_fields(root: JsonObject): Either[String, Unit] =
    for {
      schema <- _string(root, "schemaVersion")
      _ <- Either.cond(schema == "2.0", (), "manifest schemaVersion must be 2.0")
      _ <- _package_reference(root)
      _ <- _token_field(root, "lifecycleState")
      _ <- _positive_long(root, "revision")
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
      _ <- _inventory_metadata(artifacts)
    } yield artifacts

  private def _package_context(
    manifest: JsonObject,
    verified: Vector[VerifiedArtifact]
  ): Either[String, InternalModelVerifiedPackageContext] =
    for {
      schema <- _string(manifest, "schemaVersion")
      reference <- _package_reference(manifest)
      revision <- _positive_long(manifest, "revision")
      lifecycle <- _string(manifest, "lifecycleState")
    } yield InternalModelVerifiedPackageContext(
      reference = reference,
      schemaversion = schema,
      revision = revision,
      lifecyclestate = lifecycle,
      artifacts = verified.map { entry =>
        InternalModelVerifiedArtifactContext(
          reference = entry.artifact.reference,
          path = entry.artifact.path,
          required = entry.artifact.required,
          dependencies = entry.artifact.dependencies,
          present = entry.bytes.nonEmpty
        )
      }
    )

  private def _artifact(value: Json, index: Int): Either[String, Artifact] =
    for {
      objectvalue <- value.asObject.toRight(s"artifact $index must be an object")
      _ <- Either.cond(objectvalue.keys.toSet == _artifact_fields, (), s"artifact $index fields are not the closed V2 schema")
      reference <- _artifact_reference(objectvalue)
      path <- _string(objectvalue, "path")
      required <- objectvalue("required").flatMap(_.asBoolean).toRight(s"artifact $index required must be a JSON Boolean")
      values <- objectvalue("dependsOn").flatMap(_.asArray).toRight(s"artifact $index dependsOn must be an array")
      dependencies <- values.toVector.foldLeft[Either[String, Vector[InternalModelArtifactReference]]](Right(Vector.empty)) { (result, dependency) =>
        for {
          collected <- result
          root <- dependency.asObject.toRight(s"artifact $index dependency must be an object")
          _ <- Either.cond(root.keys.toSet == _reference_fields, (), s"artifact $index dependency fields are not the closed reference shape")
          reference <- _artifact_reference(root)
        } yield collected :+ reference
      }
    } yield Artifact(reference, path, required, dependencies)

  private def _package_reference(root: JsonObject): Either[String, InternalModelPackageReference] =
    for {
      packageid <- _string(root, "packageId").flatMap(InternalModelPackageId.from)
      namespace <- _string(root, "projectNamespace").flatMap(InternalModelProjectToken.from)
      projectid <- _string(root, "projectId").flatMap(InternalModelProjectToken.from)
    } yield InternalModelPackageReference(packageid, namespace, projectid)

  private def _artifact_reference(root: JsonObject): Either[String, InternalModelArtifactReference] =
    for {
      id <- _string(root, "artifactId").flatMap(InternalModelArtifactId.from)
      revision <- _positive_long(root, "artifactRevision").flatMap(InternalModelArtifactRevision.from)
      role <- _string(root, "role").flatMap(InternalModelArtifactRole.fromWire)
    } yield InternalModelArtifactReference(id, revision, role)

  private def _inventory_metadata(artifacts: Vector[Artifact]): Either[String, Unit] =
    for {
      _ <- _unique(artifacts.map(_.reference.artifactId.value), "artifact IDs")
      _ <- _unique(artifacts.map(_.path), "artifact paths")
      _ <- artifacts.foldLeft[Either[String, Unit]](Right(())) { (result, artifact) =>
        val id = artifact.reference.artifactId.value
        val dependencies = artifact.dependencies.map(_.artifactId.value)
        for {
          _ <- result
          _ <- Either.cond(_is_path(artifact.path) && artifact.path != "manifest.yaml", (), s"artifact $id has an unsafe path")
          _ <- _unique(dependencies, s"artifact $id dependency IDs")
          _ <- Either.cond(dependencies == dependencies.sorted, (), s"artifact $id dependencies are not in artifact ID order")
        } yield ()
      }
    } yield ()

  private def _inventory_order(artifacts: Vector[Artifact]): Either[String, Unit] = {
    val references = artifacts.map(artifact => artifact.reference.artifactId -> artifact.reference).toMap
    val unresolved = artifacts.flatMap(artifact => artifact.dependencies.filterNot(dependency => references.get(dependency.artifactId).contains(dependency)).map(dependency => s"${artifact.reference.artifactId.value} -> ${dependency.artifactId.value}"))
    if unresolved.nonEmpty then Left(s"artifact dependencies do not resolve exact ID/revision/role: ${unresolved.mkString(", ")}")
    else if artifacts.exists(artifact => artifact.dependencies.exists(_.artifactId == artifact.reference.artifactId)) then Left("artifact dependencies must not be self-referential")
    else {
      val remaining = artifacts.map(artifact => artifact.reference.artifactId -> artifact.dependencies.map(_.artifactId).toSet).toMap
      _topological_order(remaining).flatMap { expected =>
        Either.cond(artifacts.map(_.reference.artifactId) == expected, (), "artifacts are not in deterministic topological order")
      }
    }
  }

  private def _topological_order(remaining: Map[InternalModelArtifactId, Set[InternalModelArtifactId]]): Either[String, Vector[InternalModelArtifactId]] = {
    @annotation.tailrec
    def _loop_(pending: Map[InternalModelArtifactId, Set[InternalModelArtifactId]], completed: Vector[InternalModelArtifactId]): Either[String, Vector[InternalModelArtifactId]] =
      if pending.isEmpty then Right(completed)
      else {
        val available = pending.collect { case (id, dependencies) if dependencies.isEmpty => id }.toVector.sortBy(_.value)
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
      present = verified.collect { case VerifiedArtifact(artifact, Some(_)) => artifact.reference.artifactId }.toSet
      _ <- _present_dependencies(artifacts, present)
    } yield verified

  private def _source_snapshots(verified: Vector[VerifiedArtifact]): Vector[InternalModelVerifiedSourceSnapshot] =
    verified.collect {
      case VerifiedArtifact(artifact, bytes) if artifact.reference.role == InternalModelArtifactRole.SourceSnapshot =>
        InternalModelVerifiedSourceSnapshot(artifact.reference, artifact.path, artifact.required, artifact.dependencies, bytes)
    }

  private def _present_realization(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedRealizationPackage] = {
    val realizations = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.reference.role == InternalModelArtifactRole.Realization =>
        InternalModelVerifiedRealization(
          reference = artifact.reference,
          path = artifact.path,
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
      projection <- _selected_projection(verified, "continuity")
      _ <- Either.cond(
        projection.dependencies.contains(realizationpackage.realization.reference),
        (),
        "present projection artifact must depend on the selected realization artifact"
      )
    } yield InternalModelVerifiedProjectionContinuityPackage(realizationpackage, projection)

  private def _candidate_cml_projection(captured: CapturedPackage): Either[String, InternalModelVerifiedCandidateCmlProjectionPackage] =
    for {
      continuity <- _projection_continuity(captured.artifacts)
      candidate <- _selected_projection(captured.artifacts, "candidate")
    } yield InternalModelVerifiedCandidateCmlProjectionPackage(captured.context, continuity, candidate)

  private def _semantic_diff(captured: CapturedPackage): Either[String, InternalModelVerifiedSemanticDiffPackage] =
    for {
      candidate <- _candidate_cml_projection(captured)
      semanticdiff <- _selected_projection(captured.artifacts, "semantic-diff")
    } yield InternalModelVerifiedSemanticDiffPackage(candidate, semanticdiff)

  private def _candidate_review_binding(
    captured: CapturedPackage,
    reviewartifact: InternalModelArtifactReference
  ): Either[String, InternalModelVerifiedCandidateReviewBindingPackage] =
    for {
      semanticdiff <- _semantic_diff(captured)
      review <- _selected_validation(captured.artifacts, reviewartifact)
    } yield InternalModelVerifiedCandidateReviewBindingPackage(captured.context, semanticdiff, review)

  private def _candidate_human_approval(
    captured: CapturedPackage,
    approvalartifact: InternalModelArtifactReference,
    reviewartifact: InternalModelArtifactReference
  ): Either[String, InternalModelVerifiedCandidateHumanApprovalPackage] =
    for {
      review <- _candidate_review_binding(captured, reviewartifact)
      approval <- _selected_approval(captured.artifacts, approvalartifact, reviewartifact)
    } yield InternalModelVerifiedCandidateHumanApprovalPackage(review, approval)

  private def _selected_validation(
    verified: Vector[VerifiedArtifact],
    reviewartifact: InternalModelArtifactReference
  ): Either[String, InternalModelVerifiedProjection] =
    verified.filter(_.artifact.reference == reviewartifact) match {
      case Vector(VerifiedArtifact(artifact, Some(bytes))) if artifact.reference.role == InternalModelArtifactRole.Validation =>
        Right(InternalModelVerifiedProjection(artifact.reference, artifact.path, artifact.required, artifact.dependencies, bytes))
      case Vector(VerifiedArtifact(_, Some(_))) => Left("selected review artifact role must be validation")
      case Vector(VerifiedArtifact(_, None)) => Left("selected review artifact is absent")
      case Vector() => Left("selected review artifact is not present in captured package inventory")
      case _ => Left("selected review artifact reference is ambiguous")
    }

  private def _selected_approval(
    verified: Vector[VerifiedArtifact],
    approvalartifact: InternalModelArtifactReference,
    reviewartifact: InternalModelArtifactReference
  ): Either[String, InternalModelVerifiedProjection] =
    verified.filter(_.artifact.reference == approvalartifact) match {
      case Vector(VerifiedArtifact(artifact, Some(bytes))) if artifact.reference.role == InternalModelArtifactRole.Approval && artifact.dependencies == Vector(reviewartifact) =>
        Right(InternalModelVerifiedProjection(artifact.reference, artifact.path, artifact.required, artifact.dependencies, bytes))
      case Vector(VerifiedArtifact(artifact, Some(_))) if artifact.reference.role != InternalModelArtifactRole.Approval => Left("selected approval artifact role must be approval")
      case Vector(VerifiedArtifact(_, Some(_))) => Left("selected approval artifact dependencies must equal exactly the selected review artifact reference")
      case Vector(VerifiedArtifact(_, None)) => Left("selected approval artifact is absent")
      case Vector() => Left("selected approval artifact is not present in captured package inventory")
      case _ => Left("selected approval artifact reference is ambiguous")
    }

  private def _decision_records(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedDecisionPackage] =
    for {
      realizationpackage <- _present_realization(verified)
      decision <- _present_decision(verified)
      _ <- Either.cond(
        decision.dependencies.contains(realizationpackage.realization.reference),
        (),
        "present decision artifact must depend on the selected realization artifact"
      )
    } yield InternalModelVerifiedDecisionPackage(realizationpackage, decision)

  private def _open_issue_records(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedOpenIssuePackage] =
    for {
      realizationpackage <- _present_realization(verified)
      openissue <- _present_open_issue(verified)
      _ <- Either.cond(
        openissue.dependencies.contains(realizationpackage.realization.reference),
        (),
        "present open-issue artifact must depend on the selected realization artifact"
      )
    } yield InternalModelVerifiedOpenIssuePackage(realizationpackage, openissue)

  private def _selected_projection(verified: Vector[VerifiedArtifact], family: String): Either[String, InternalModelVerifiedProjection] =
    for {
      projections <- _present_projections(verified)
      classified <- projections.foldLeft[Either[String, Vector[(String, InternalModelVerifiedProjection)]]](Right(Vector.empty)) { (result, projection) =>
        for {
          collected <- result
          kind <- _projection_family(projection)
        } yield collected :+ (kind -> projection)
      }
      selected = classified.collect { case (`family`, projection) => projection }
      projection <- selected match {
        case Vector(value) => Right(value)
        case Vector() => Left(s"internal-model package has no present $family projection artifact")
        case _ => Left(s"internal-model package has multiple present $family projection artifacts")
      }
    } yield projection

  private def _present_projections(verified: Vector[VerifiedArtifact]): Either[String, Vector[InternalModelVerifiedProjection]] =
    Right(verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.reference.role == InternalModelArtifactRole.Projection =>
        InternalModelVerifiedProjection(
          reference = artifact.reference,
          path = artifact.path,
          required = artifact.required,
          dependencies = artifact.dependencies,
          bytes = bytes
        )
    })

  private def _projection_family(projection: InternalModelVerifiedProjection): Either[String, String] =
    for {
      bytes <- Right(projection.bytes.toArray)
      _ <- Either.cond(!_has_bom(bytes), (), s"projection artifact ${projection.reference.artifactId.value} must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, s"projection artifact ${projection.reference.artifactId.value}")
      json <- _json_parser.parse(content).left.map(_ => s"projection artifact ${projection.reference.artifactId.value} must be valid JSON without duplicate members")
      root <- json.asObject.toRight(s"projection artifact ${projection.reference.artifactId.value} root must be an object")
      profile <- root("profile").flatMap(_.asString).toRight(s"projection artifact ${projection.reference.artifactId.value} profile must be a JSON string")
      schema <- root("schemaVersion").flatMap(_.asString).toRight(s"projection artifact ${projection.reference.artifactId.value} schemaVersion must be a JSON string")
      family <- (profile, schema) match {
        case ("ccdm-projection-binding-v3", "3.0") => Right("continuity")
        case ("ccdm-candidate-cml-projection-v2", "2.0") => Right("candidate")
        case ("ccdm-semantic-diff-v2", "2.0") => Right("semantic-diff")
        case _ => Left(s"projection artifact ${projection.reference.artifactId.value} has an unknown profile/schemaVersion pair")
      }
    } yield family

  private def _present_decision(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedDecision] = {
    val decisions = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.reference.role == InternalModelArtifactRole.Decision =>
        InternalModelVerifiedDecision(
          reference = artifact.reference,
          path = artifact.path,
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

  private def _present_open_issue(verified: Vector[VerifiedArtifact]): Either[String, InternalModelVerifiedOpenIssue] = {
    val openissues = verified.collect {
      case VerifiedArtifact(artifact, Some(bytes)) if artifact.reference.role == InternalModelArtifactRole.OpenIssue =>
        InternalModelVerifiedOpenIssue(
          reference = artifact.reference,
          path = artifact.path,
          required = artifact.required,
          dependencies = artifact.dependencies,
          bytes = bytes
        )
    }
    openissues match {
      case Vector(openissue) => Right(openissue)
      case Vector() => Left("internal-model package has no present open-issue artifact")
      case _ => Left("internal-model package has multiple present open-issue artifacts")
    }
  }

  private def _walk_regular_paths(packageroot: Path): Either[String, Set[String]] =
    try {
      var failure: Option[String] = None
      var paths = Set.empty[String]
      Files.walkFileTree(packageroot, new SimpleFileVisitor[Path] {
        override def preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult =
          if attributes.isSymbolicLink then {
            failure = Some(s"internal-model package contains a symbolic-link directory: ${_relative_path(packageroot, directory)}")
            FileVisitResult.TERMINATE
          } else FileVisitResult.CONTINUE

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
        case None => Right(paths)
      }
    } catch {
      case NonFatal(_) => Left("internal-model package inventory cannot be read")
    }

  private def _artifact_file(packageroot: Path, actualpaths: Set[String], artifact: Artifact): Either[String, VerifiedArtifact] =
    if !actualpaths.contains(artifact.path) then
      if Files.exists(packageroot.resolve(artifact.path), LinkOption.NOFOLLOW_LINKS) then Left(s"artifact ${artifact.reference.artifactId.value} must be a regular file")
      else if !artifact.required then Right(VerifiedArtifact(artifact, None))
      else Left(s"required artifact ${artifact.reference.artifactId.value} is absent")
    else {
      val path = packageroot.resolve(artifact.path).normalize
      for {
        _ <- Either.cond(path.startsWith(packageroot), (), s"artifact ${artifact.reference.artifactId.value} escapes the package root")
        bytes <- _regular_bytes(path, s"artifact ${artifact.reference.artifactId.value}")
      } yield VerifiedArtifact(artifact, Some(bytes.toVector))
    }

  private def _present_dependencies(artifacts: Vector[Artifact], present: Set[InternalModelArtifactId]): Either[String, Unit] =
    artifacts.find(artifact => present.contains(artifact.reference.artifactId) && artifact.dependencies.exists(dependency => !present.contains(dependency.artifactId))) match {
      case Some(artifact) => Left(s"present artifact ${artifact.reference.artifactId.value} depends on an absent optional artifact")
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

  private def _positive_long(objectvalue: JsonObject, key: String): Either[String, Long] =
    objectvalue(key).flatMap(_.asNumber).flatMap { number =>
      number.toLong.filter(value => value > 0 && number.toString == value.toString)
    }.toRight(s"manifest $key must be a positive lexical Long integer")

  private def _unique(values: Vector[String], label: String): Either[String, Unit] =
    Either.cond(values.distinct.size == values.size, (), s"$label must be unique")

  private def _is_token(value: String): Boolean =
    _token_pattern.matches(value)

  private def _is_path(value: String): Boolean =
    value.nonEmpty && !value.startsWith("/") && !value.endsWith("/") &&
      !value.contains("\\") && !value.contains(":") &&
      value.split("/", -1).forall(segment => _path_segment_pattern.matches(segment) && segment != "." && segment != "..")

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _relative_path(root: Path, path: Path): String =
    root.relativize(path).toString.replace('\\', '/')
}
