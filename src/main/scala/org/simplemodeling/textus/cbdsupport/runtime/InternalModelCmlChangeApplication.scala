package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import org.goldenport.Consequence

/**
 * Fresh rooted actual-human admission followed by explicit existing-file effects.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelCmlChangeApplication {
  enum Disposition {
    case Rejected, Failed, AppliedPendingValidation
  }
  final case class ApplicationReport(applicationreference: InternalModelRecordReference,
    disposition: Disposition, gate: InternalModelCmlChangeReport, writes: Option[NativeCmlFileWriter.Report])

  def applyApproved(projectRoot: Path, request: InternalModelCmlChangeRequest,
    applicationReference: InternalModelRecordReference): Consequence[ApplicationReport] =
    applyApprovedWithOperations(projectRoot, request, applicationReference, NativeCmlFileWriter.nativeOperations)

  private[runtime] def applyApprovedWithOperations(projectRoot: Path, request: InternalModelCmlChangeRequest,
    applicationReference: InternalModelRecordReference,
    operations: NativeCmlFileWriter.WriteOperations): Consequence[ApplicationReport] = {
    _validate_reference(applicationReference).fold(Consequence.operationInvalid, _ =>
      InternalModelCmlChangeGate.evaluate(projectRoot, request).map { gate =>
        if (gate.eligibility != InternalModelCmlChangeEligibility.EligibleForSkillApplication) {
          ApplicationReport(applicationReference, Disposition.Rejected, gate, None)
        } else {
          val targets = gate.plan.toVector.flatMap(_.targets).map { target =>
            NativeCmlFileWriter.Target(target.target.targetId, target.target.projectRelativePath, target.payload.proposedRawBytes)
          }
          val writes = NativeCmlFileWriter.replaceExistingWithOperations(projectRoot, targets, operations)
          val complete = targets.nonEmpty && writes.failure.isEmpty && writes.cleanupfailures.isEmpty &&
            writes.outcomes.size == targets.size && writes.outcomes.zip(targets).forall { case (outcome, target) =>
              outcome.targetid == target.targetid && outcome.projectrelativepath == target.projectrelativepath &&
                outcome.disposition == NativeCmlFileWriter.Disposition.Applied && outcome.failure.isEmpty &&
                outcome.byteswritten == target.bytes.size.toLong && outcome.mayhavechanged
            }
          ApplicationReport(applicationReference,
            if (complete) Disposition.AppliedPendingValidation else Disposition.Failed, gate, Some(writes))
        }
      })
  }

  private def _validate_reference(reference: InternalModelRecordReference): Either[String, Unit] = {
    if (reference == null) Left("explicit application RecordReference is required")
    else {
      val identity = reference.recordId.value
      for {
        _ <- InternalModelRecordId.from(identity)
        _ <- Either.cond(identity.codePoints().toArray.forall(code => code < 0xd800 || code > 0xdfff) &&
          identity.codePoints().toArray.exists(code => !Character.isWhitespace(code) && !Character.isSpaceChar(code) && code != 0x85),
          (), "application record identity must be nonblank valid Unicode")
        _ <- InternalModelRecordRevision.from(reference.recordRevision.value)
      } yield ()
    }
  }
}
