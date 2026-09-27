package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.channels.Channels
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.{AccessDeniedException, Files, LinkOption, NoSuchFileException, Path, SecureDirectoryStream, StandardOpenOption}

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import com.sun.jna.Platform
import org.goldenport.Consequence

/*
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] enum InternalModelPackageFreshnessInput {
  case SourceObservation(observation: InternalModelLiveSourceObservation)
  case CmlObserved(
    authority: String,
    identity: String,
    revision: Option[String],
    currentProjectRelativePath: String
  )
}

private[runtime] enum InternalModelPackageFreshnessResult {
  case Compared(report: InternalModelSnapshotFreshnessReport)
  case MissingBaseline
}

private[runtime] final case class InternalModelPackageFreshnessEntry(
  artifactId: String,
  packageRelativePath: String,
  result: InternalModelPackageFreshnessResult
)

private[runtime] final case class InternalModelPackageFreshnessReport(
  entries: Vector[InternalModelPackageFreshnessEntry]
)

/** Checks every verified source-snapshot artifact in one internal-model package. */
private[runtime] object InternalModelPackageFreshness {
  private enum CmlReadResult {
    case Read(bytes: Vector[Byte])
    case Unavailable(reason: String)
    case Unauthorized(reason: String)
    case Malformed(reason: String)
  }

  def check(
    projectRoot: Path,
    inputs: Map[String, InternalModelPackageFreshnessInput]
  ): Consequence[InternalModelPackageFreshnessReport] = {
    if inputs == null then Consequence.operationInvalid("source-snapshot freshness inputs are missing")
    else
      try {
        InternalModelPackageValidator.verifiedSourceSnapshots(projectRoot).flatMap { snapshots =>
          _check(projectRoot, snapshots, inputs).fold(Consequence.operationInvalid, Consequence.success)
        }
      } catch {
        case NonFatal(error) => Consequence.operationInvalid(s"internal-model package freshness check failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
      }
  }

  private def _check(
    projectroot: Path,
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    inputs: Map[String, InternalModelPackageFreshnessInput]
  ): Either[String, InternalModelPackageFreshnessReport] =
    _input_ids(snapshots, inputs).map { _ =>
      InternalModelPackageFreshnessReport(snapshots.map(snapshot => _entry(projectroot, snapshot, inputs.get(snapshot.artifactId))))
    }

  private def _input_ids(
    snapshots: Vector[InternalModelVerifiedSourceSnapshot],
    inputs: Map[String, InternalModelPackageFreshnessInput]
  ): Either[String, Unit] =
    if inputs.keys.exists(_ == null) then Left("source-snapshot freshness input IDs must be nonempty manifest artifact IDs")
    else {
      val unknown = inputs.keySet -- snapshots.map(_.artifactId).toSet
      if unknown.nonEmpty then Left(s"source-snapshot freshness inputs name unknown or non-source artifacts: ${unknown.toVector.sorted.mkString(", ")}")
      else Right(())
    }

  private def _entry(
    projectroot: Path,
    snapshot: InternalModelVerifiedSourceSnapshot,
    input: Option[InternalModelPackageFreshnessInput]
  ): InternalModelPackageFreshnessEntry =
    snapshot.bytes match {
      case None =>
        InternalModelPackageFreshnessEntry(
          snapshot.artifactId,
          snapshot.packageRelativePath,
          InternalModelPackageFreshnessResult.MissingBaseline
        )
      case Some(bytes) =>
        val report = InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes.toArray).fold(
          _ => InternalModelSourceSnapshotFreshness.compare(bytes.toArray, _baseline_malformed_observation(input)),
          kind => _compare(projectroot, kind, bytes, input)
        )
        InternalModelPackageFreshnessEntry(
          snapshot.artifactId,
          snapshot.packageRelativePath,
          InternalModelPackageFreshnessResult.Compared(report)
        )
    }

  private def _baseline_malformed_observation(
    input: Option[InternalModelPackageFreshnessInput]
  ): InternalModelLiveSourceObservation =
    input match {
      case Some(InternalModelPackageFreshnessInput.SourceObservation(observation)) if observation != null => observation
      case _ => InternalModelLiveSourceObservation.Malformed("source-snapshot baseline is malformed before a live observation is consulted")
    }

  private def _compare(
    projectroot: Path,
    kind: String,
    bytes: Vector[Byte],
    input: Option[InternalModelPackageFreshnessInput]
  ): InternalModelSnapshotFreshnessReport =
    InternalModelSourceSnapshotFreshness.compare(bytes.toArray, _observation(projectroot, kind, input))

  private def _observation(
    projectroot: Path,
    kind: String,
    input: Option[InternalModelPackageFreshnessInput]
  ): InternalModelLiveSourceObservation =
    input match {
      case None => InternalModelLiveSourceObservation.Unavailable("no source-owner live observation was supplied for this source-snapshot artifact")
      case Some(InternalModelPackageFreshnessInput.SourceObservation(observation)) =>
        if kind == "cml-baseline" && observation.isInstanceOf[InternalModelLiveSourceObservation.Observed] then
          InternalModelLiveSourceObservation.Malformed("CML baselines require a source-owner current project-relative target request")
        else observation
      case Some(request: InternalModelPackageFreshnessInput.CmlObserved) =>
        if kind != "cml-baseline" then
          InternalModelLiveSourceObservation.Malformed("a CML observed request cannot be used for a non-CML source-snapshot baseline")
        else _cml_observation(projectroot, request)
    }

  private def _cml_observation(
    projectroot: Path,
    request: InternalModelPackageFreshnessInput.CmlObserved
  ): InternalModelLiveSourceObservation =
    _cml_request_reason(request) match {
      case Some(reason) => InternalModelLiveSourceObservation.Malformed(reason)
      case None =>
        _read_cml(projectroot, request.currentProjectRelativePath) match {
          case CmlReadResult.Read(bytes) =>
            InternalModelLiveSourceObservation.Observed(
              request.authority,
              request.identity,
              request.revision,
              bytes,
              Some(request.currentProjectRelativePath)
            )
          case CmlReadResult.Unavailable(reason) => InternalModelLiveSourceObservation.Unavailable(reason)
          case CmlReadResult.Unauthorized(reason) => InternalModelLiveSourceObservation.Unauthorized(reason)
          case CmlReadResult.Malformed(reason) => InternalModelLiveSourceObservation.Malformed(reason)
        }
    }

  private def _cml_request_reason(request: InternalModelPackageFreshnessInput.CmlObserved): Option[String] =
    if request == null then Some("CML observed request is missing")
    else if Option(request.authority).forall(_.isEmpty) then Some("CML observed request authority must be nonempty")
    else if Option(request.identity).forall(_.isEmpty) then Some("CML observed request identity must be nonempty")
    else if request.revision == null || request.revision.exists(value => value == null || value.isEmpty) then Some("CML observed request revision must be an explicit optional nonempty value")
    else if !InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(request.currentProjectRelativePath) then Some("CML observed request current project-relative target path is unsafe")
    else None

  private def _read_cml(projectroot: Path, projectrelativepath: String): CmlReadResult =
    _cml_target(projectroot, projectrelativepath) match {
      case Left(result) => result
      case Right(physicalroot) =>
        _read_cml_from_secure_provider(physicalroot, projectrelativepath) match {
          case Some(result) => result
          case None if Platform.isMac => _read_cml_natively(physicalroot, projectrelativepath)
          case None => CmlReadResult.Malformed("CML current target cannot be read safely")
        }
    }

  private def _read_cml_from_secure_provider(
    physicalroot: Path,
    projectrelativepath: String
  ): Option[CmlReadResult] =
    try {
      val stream = Files.newDirectoryStream(Path.of("/"))
      try {
        stream match {
          case secure: SecureDirectoryStream[?] =>
            val components = physicalroot.iterator.asScala.toVector ++ projectrelativepath.split("/", -1).toVector.map(component => Path.of(component))
            Some(_read_cml_from_directory(secure.asInstanceOf[SecureDirectoryStream[Path]], components))
          case _ => None
        }
      } finally {
        stream.close()
      }
    } catch {
      case _: UnsupportedOperationException => None
      case _: NoSuchFileException => Some(CmlReadResult.Unavailable("CML current target is absent"))
      case _: AccessDeniedException => Some(CmlReadResult.Unauthorized("CML current target cannot be read"))
      case _: SecurityException => Some(CmlReadResult.Unauthorized("CML current target cannot be read"))
      case NonFatal(_) => Some(CmlReadResult.Malformed("CML current target cannot be read safely"))
    }

  private def _read_cml_natively(physicalroot: Path, projectrelativepath: String): CmlReadResult =
    NativeCmlFileReader.readPhysical(physicalroot, projectrelativepath) match {
      case NativeCmlFileReader.Result.Read(bytes) => CmlReadResult.Read(bytes)
      case NativeCmlFileReader.Result.Unavailable(reason) => CmlReadResult.Unavailable(reason)
      case NativeCmlFileReader.Result.Unauthorized(reason) => CmlReadResult.Unauthorized(reason)
      case NativeCmlFileReader.Result.Malformed(reason) => CmlReadResult.Malformed(reason)
    }

  private def _read_cml_from_directory(
    directory: SecureDirectoryStream[Path],
    components: Vector[Path]
  ): CmlReadResult =
    components match {
      case Vector(target) =>
        val attributes = directory.getFileAttributeView(target, classOf[BasicFileAttributeView], LinkOption.NOFOLLOW_LINKS).readAttributes()
        if attributes.isSymbolicLink || !attributes.isRegularFile then CmlReadResult.Malformed("CML current target must be a regular file")
        else {
          val options = java.util.Set.of[java.nio.file.OpenOption](StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
          val channel = directory.newByteChannel(target, options)
          try {
            val input = Channels.newInputStream(channel)
            try CmlReadResult.Read(input.readAllBytes().toVector)
            finally input.close()
          }
          finally channel.close()
        }
      case directoryname +: remaining =>
        val child = directory.newDirectoryStream(directoryname, LinkOption.NOFOLLOW_LINKS)
        try _read_cml_from_directory(child, remaining)
        finally child.close()
      case _ => CmlReadResult.Malformed("CML current target path is unsafe")
    }

  private def _cml_target(projectroot: Path, projectrelativepath: String): Either[CmlReadResult, Path] =
    if projectroot == null then Left(CmlReadResult.Malformed("consuming project root is missing"))
    else
      try {
        val root = projectroot.toAbsolutePath.normalize
        val target = root.resolve(projectrelativepath).normalize
        for {
          _ <- Either.cond(target.startsWith(root), (), CmlReadResult.Malformed("CML current target escapes the consuming project root"))
          anchoredroot <- NativeCmlFileReader.normalizeProjectRoot(root).left.map(_native_root_result)
        } yield anchoredroot
      } catch {
        case _: AccessDeniedException => Left(CmlReadResult.Unauthorized("consuming project root cannot be read"))
        case _: SecurityException => Left(CmlReadResult.Unauthorized("consuming project root cannot be read"))
        case NonFatal(_) => Left(CmlReadResult.Malformed("consuming project root cannot be anchored safely"))
      }

  private def _native_root_result(result: NativeCmlFileReader.Result): CmlReadResult =
    result match {
      case NativeCmlFileReader.Result.Read(_) => CmlReadResult.Malformed("consuming project root cannot be anchored safely")
      case NativeCmlFileReader.Result.Unavailable(reason) => CmlReadResult.Unavailable(reason)
      case NativeCmlFileReader.Result.Unauthorized(reason) => CmlReadResult.Unauthorized(reason)
      case NativeCmlFileReader.Result.Malformed(reason) => CmlReadResult.Malformed(reason)
    }
}
