package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import scala.util.control.NonFatal
import org.goldenport.Consequence

/**
 * Read-only reconstruction from one project-bound package capture.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] object InternalModelRehydrationValidator {
  def validate(projectRoot: Path): Consequence[InternalModelRehydratedState] =
    InternalModelPackageValidator.verifiedContinuation(projectRoot).flatMap(validateVerified)

  def validateVerified(handoff: InternalModelVerifiedContinuationPackage): Consequence[InternalModelRehydratedState] =
    try {
      val admitted = for {
        selected <- _capture_consistency(handoff)
        cursor <- _cursor(handoff, selected)
      } yield (cursor, selected)
      admitted.fold(Consequence.operationInvalid, { case (cursor, selected) =>
        InternalModelProjectionContinuityValidator.validateVerified(selected).map { continuity =>
          InternalModelRehydratedState(handoff.packageContext, cursor, continuity, handoff.artifacts)
        }
      })
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model rehydration failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _cursor(
    handoff: InternalModelVerifiedContinuationPackage,
    selected: InternalModelVerifiedProjectionContinuityPackage
  ): Either[String, InternalModelResumeCursor] =
    for {
      resume <- handoff.artifacts.filter(_.context.reference.role == InternalModelArtifactRole.Resume) match {
        case Vector(value) if value.context.path == "resume.yaml" => Right(value)
        case Vector(_) => Left("continuation resume entry must be at resume.yaml")
        case _ => Left("continuation requires exactly one resume inventory entry")
      }
      bytes <- resume.bytes.toRight("continuation resume entry must be present")
      cursor <- InternalModelResumeCursorCodec.decode(bytes)
      _ <- Either.cond(cursor.packageId == handoff.packageContext.reference.packageId &&
        cursor.packageRevision == handoff.packageContext.revision, (), "resume package identity or revision does not match captured manifest")
      _ <- _selections(cursor, handoff)
      realization = selected.realizationpackage.realization
      consumed = (Vector(selected.projection.reference, realization.reference) ++ realization.dependencies).toSet
      _ <- Either.cond(consumed.subsetOf(cursor.selectedArtifacts.toSet), (),
        "resume selected references omit consumed continuity, realization, or source snapshots")
    } yield cursor

  private def _selections(cursor: InternalModelResumeCursor, handoff: InternalModelVerifiedContinuationPackage): Either[String, Unit] =
    cursor.selectedArtifacts.foldLeft[Either[String, Unit]](Right(())) { (result, reference) =>
      for {
        _ <- result
        captured <- handoff.artifacts.filter(_.context.reference == reference) match {
          case Vector(value) => Right(value)
          case _ => Left(s"resume selected artifact ${reference.artifactId.value} ID, revision or role does not match inventory")
        }
        _ <- Either.cond(captured.bytes.nonEmpty && captured.context.present, (),
          s"resume selected artifact ${reference.artifactId.value} is absent")
      } yield ()
    }

  /** Captured inventory owns payload. A duplicated handoff contributes only matching metadata. */
  private def _capture_consistency(
    handoff: InternalModelVerifiedContinuationPackage
  ): Either[String, InternalModelVerifiedProjectionContinuityPackage] =
    for {
      _ <- Either.cond(handoff != null && handoff.packageContext != null &&
        handoff.artifacts != null && handoff.continuityPackage != null, (), "continuation capture must be present")
      context = handoff.packageContext
      _ <- _package_metadata(context)
      _ <- handoff.artifacts.foldLeft[Either[String, Unit]](Right(())) { (result, artifact) =>
        for {
          _ <- result
          _ <- Either.cond(artifact != null && artifact.context != null && artifact.bytes != null &&
            artifact.bytes.forall(_ != null), (), "continuation artifact context and payload availability must be present")
          _ <- _artifact_metadata(artifact.context.reference, artifact.context.path, artifact.context.dependencies)
          _ <- Either.cond(artifact.context.present == artifact.bytes.nonEmpty &&
            (!artifact.context.required || artifact.bytes.nonEmpty), (),
            s"continuation artifact ${artifact.context.reference.artifactId.value} presence is inconsistent")
        } yield ()
      }
      _ <- Either.cond(context.artifacts == handoff.artifacts.map(_.context), (),
        "continuation inventory differs from captured package context")
      _ <- InternalModelPackageValidator.validateCapturedContext(context)
      selected <- InternalModelPackageValidator.selectCapturedContinuity(handoff.artifacts)
      _ <- _handoff_metadata(handoff.continuityPackage, selected)
    } yield selected

  private def _package_metadata(context: InternalModelVerifiedPackageContext): Either[String, Unit] =
    for {
      _ <- Either.cond(context.reference != null && context.artifacts != null, (), "continuation package reference and inventory must be present")
      _ <- InternalModelPackageId.from(context.reference.packageId.value)
      _ <- InternalModelProjectToken.from(context.reference.projectNamespace.value)
      _ <- InternalModelProjectToken.from(context.reference.projectId.value)
      _ <- context.artifacts.foldLeft[Either[String, Unit]](Right(())) { (result, entry) =>
        for {
          _ <- result
          _ <- Either.cond(entry != null, (), "continuation inventory entry must be present")
          _ <- _artifact_metadata(entry.reference, entry.path, entry.dependencies)
        } yield ()
      }
    } yield ()

  private def _artifact_metadata(
    reference: InternalModelArtifactReference,
    path: String,
    dependencies: Vector[InternalModelArtifactReference]
  ): Either[String, Unit] =
    for {
      _ <- _reference_metadata(reference)
      _ <- Either.cond(path != null && dependencies != null, (), "continuation artifact path and dependencies must be present")
      _ <- dependencies.foldLeft[Either[String, Unit]](Right(())) { (result, item) =>
        result.flatMap(_ => _reference_metadata(item))
      }
    } yield ()

  private def _reference_metadata(reference: InternalModelArtifactReference): Either[String, Unit] =
    for {
      _ <- Either.cond(reference != null && reference.role != null, (), "continuation artifact reference and role must be present")
      _ <- InternalModelArtifactId.from(reference.artifactId.value)
      _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
    } yield ()

  private def _handoff_metadata(
    supplied: InternalModelVerifiedProjectionContinuityPackage,
    selected: InternalModelVerifiedProjectionContinuityPackage
  ): Either[String, Unit] =
    for {
      _ <- Either.cond(supplied.realizationpackage != null && supplied.projection != null, (), "continuation core handoff must be present")
      realization = supplied.realizationpackage.realization
      projection = supplied.projection
      _ <- Either.cond(realization != null && supplied.realizationpackage.sourcesnapshots != null &&
        realization.bytes != null && projection.bytes != null, (), "continuation core payload sidecars must be present")
      _ <- _artifact_metadata(realization.reference, realization.path, realization.dependencies)
      _ <- _artifact_metadata(projection.reference, projection.path, projection.dependencies)
      actualrealization = selected.realizationpackage.realization
      actualprojection = selected.projection
      _ <- Either.cond(realization.reference == actualrealization.reference && realization.path == actualrealization.path &&
        realization.required == actualrealization.required && realization.dependencies == actualrealization.dependencies, (),
        "continuation realization metadata differs from captured inventory")
      _ <- Either.cond(projection.reference == actualprojection.reference && projection.path == actualprojection.path &&
        projection.required == actualprojection.required && projection.dependencies == actualprojection.dependencies, (),
        "continuation projection metadata differs from captured inventory")
      snapshots = supplied.realizationpackage.sourcesnapshots
      _ <- snapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        for {
          _ <- result
          _ <- Either.cond(snapshot != null && snapshot.bytes != null && snapshot.bytes.forall(_ != null), (),
            "continuation source snapshot availability must be present")
          _ <- _artifact_metadata(snapshot.reference, snapshot.path, snapshot.dependencies)
        } yield ()
      }
      _ <- Either.cond(
        snapshots.map(value => (value.reference, value.path, value.required, value.dependencies, value.bytes.nonEmpty)) ==
          selected.realizationpackage.sourcesnapshots.map(value => (value.reference, value.path, value.required, value.dependencies, value.bytes.nonEmpty)),
        (), "continuation source snapshot metadata or availability differs from captured inventory"
      )
    } yield ()
}
