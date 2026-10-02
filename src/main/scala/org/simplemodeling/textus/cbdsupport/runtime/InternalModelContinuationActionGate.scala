package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import scala.util.control.NonFatal
import org.goldenport.Consequence

/**
 * Eligibility of the exact recorded action; evaluation performs no action.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelContinuationActionGate {
  import InternalModelContinuationCondition.*
  import InternalModelContinuationProblemKind.{HumanUnresolvedItems => HumanUnresolvedItemsProblem, *}
  import InternalModelContinuationEvidenceAdmission.Evidence

  private final case class Row(
    stage: InternalModelContinuationStage,
    action: InternalModelContinuationAction,
    required: Vector[InternalModelContinuationCondition]
  )

  private val _core_conditions = Vector(ModelAdmitted, SourceVersionsKnown, DecisionsComplete)
  private val _matrix = Vector(
    Row(InternalModelContinuationStage.ModelReady, InternalModelContinuationAction.InspectProjections, _core_conditions),
    Row(InternalModelContinuationStage.CandidateReady, InternalModelContinuationAction.ReviewCandidate, _core_conditions :+ MappingsComplete),
    Row(InternalModelContinuationStage.ReviewReady, InternalModelContinuationAction.RequestHumanDecision, _core_conditions ++ Vector(MappingsComplete, ReviewAdmitted)),
    Row(InternalModelContinuationStage.HumanDecisionRecorded, InternalModelContinuationAction.HandoffApprovedCandidate,
      _core_conditions ++ Vector(MappingsComplete, ReviewAdmitted, HumanApproved, NoApprovalBlockingIssues))
  )

  def evaluate(projectRoot: Path, request: InternalModelContinuationRequest): Consequence[InternalModelContinuationReport] =
    InternalModelPackageValidator.verifiedContinuation(projectRoot).flatMap(evaluateVerified(_, request))

  /** Core structural/semantic failures propagate; subsequent evidence problems are typed values. */
  def evaluateVerified(
    handoff: InternalModelVerifiedContinuationPackage,
    request: InternalModelContinuationRequest
  ): Consequence[InternalModelContinuationReport] = {
    InternalModelRehydrationValidator.validateVerified(handoff).flatMap { state =>
      try {
        val inputs = for {
          _ <- InternalModelContinuationEvidenceAdmission.validateRequest(request)
          continuity <- InternalModelPackageValidator.selectCapturedContinuity(state.artifacts)
          _ <- Either.cond(!request.candidateartifact.contains(continuity.projection.reference) &&
            !request.semanticdiffartifact.contains(continuity.projection.reference), (), "candidate/diff selection cannot alias core continuity")
        } yield continuity
        inputs.fold(Consequence.operationInvalid, continuity => Consequence.success(_evaluate(state, continuity, request)))
      } catch {
        case NonFatal(error) => Consequence.operationInvalid("continuation action request is invalid: " +
          Option(error.getMessage).getOrElse(error.getClass.getSimpleName))
      }
    }
  }

  private def _evaluate(state: InternalModelRehydratedState, continuity: InternalModelVerifiedProjectionContinuityPackage,
    request: InternalModelContinuationRequest): InternalModelContinuationReport = {
    val cursor = state.cursor
    val stage = InternalModelContinuationStage.values.find(_.token == cursor.currentStage)
    val action = cursor.nextPermittedAction.flatMap(token => InternalModelContinuationAction.values.find(_.token == token))
    val row = _matrix.find(value => stage.contains(value.stage))
    val problems = Vector.newBuilder[InternalModelContinuationProblem]
    if (stage.isEmpty) problems += _problem(UnsupportedStage, "currentStage", "recorded stage is unsupported")
    if (cursor.nextPermittedAction.isEmpty) {
      problems += _problem(MissingPrerequisite, "nextPermittedAction", "recorded next action is missing")
    } else if (action.isEmpty) {
      problems += _problem(UnsupportedAction, "nextPermittedAction", "recorded next action is unsupported")
    }
    if (row.nonEmpty && action.nonEmpty && !action.contains(row.get.action)) {
      problems += _problem(StageActionMismatch, "stage/action", "recorded stage and action are not the same matrix row")
    }
    cursor.lastCompletedAction.foreach { token =>
      if (!InternalModelContinuationAction.values.exists(_.token == token)) {
        problems += _problem(UnsupportedAction, "lastCompletedAction", "recorded last action is unsupported; no history is inferred")
      }
    }
    val conditions = Vector(
      InternalModelContinuationConditionList.Preconditions -> cursor.preconditions,
      InternalModelContinuationConditionList.InvalidationChecks -> cursor.invalidationChecks,
      InternalModelContinuationConditionList.AcceptanceCriteria -> cursor.acceptanceCriteria,
      InternalModelContinuationConditionList.Blockers -> cursor.blockers
    ).flatMap { case (origin, values) => values.map(InternalModelContinuationConditionAttribution(origin, _)) }
    val recognized = conditions.flatMap { attribution =>
      InternalModelContinuationCondition.values.find(_.token == attribution.condition.code) match {
        case Some(code) if code.blocker == (attribution.origin == InternalModelContinuationConditionList.Blockers) => Some(code -> attribution)
        case _ =>
          problems += _problem(UnsupportedCondition, "condition.code/list", "condition code is unknown or in the wrong list", Some(attribution))
          None
      }
    }
    val required = row.toVector.flatMap(_.required).toSet ++ recognized.map(_._1)
    val evidence = InternalModelContinuationEvidenceAdmission.admit(state, continuity, request, required)
    problems ++= evidence.problems.map(_.problem)
    recognized.foreach { case (code, attribution) =>
      if (!_condition_owner(code, attribution.condition.artifactId, state, evidence, continuity, request)) {
        val reference = attribution.condition.artifactId.flatMap(id => state.cursor.selectedArtifacts.find(_.artifactId.value == id))
        problems += InternalModelContinuationProblem(ConditionOwnerMismatch, "condition.artifactId", reference, None,
          "condition attribution is not the relevant consumed exact selection", Some(attribution))
      }
      val failures = _predicate_problems(code, evidence)
      if (failures.nonEmpty) problems ++= failures.map(_.copy(attribution = Some(attribution)))
      else code match {
        case ApprovalBlockingIssues => evidence.openissues.foreach { item =>
          item.admission.ledger.issues.filter(_.blocking.semanticApproval).foreach { issue =>
            problems += InternalModelContinuationProblem(BlockingIssue, "issue.blocking.semanticApproval", Some(item.artifactreference), Some(issue.issueReference),
              "recorded blocker is true under admitted issue evidence", Some(attribution))
          }
        }
        case HumanUnresolvedItems => evidence.approval.foreach { item =>
          if (item.record.approval.unresolvedItems.nonEmpty) problems += InternalModelContinuationProblem(HumanUnresolvedItemsProblem,
            "human.unresolvedItems", Some(item.approvalArtifactReference), Some(item.record.approval.approvalReference),
            "recorded blocker is true under independently admitted human input", Some(attribution))
        }
        case _ => ()
      }
    }
    val retained = problems.result()
    val eligibility =
      if (retained.exists(_.kind != MissingPrerequisite)) InternalModelContinuationEligibility.Inconsistent
      else if (retained.nonEmpty) InternalModelContinuationEligibility.Incomplete
      else InternalModelContinuationEligibility.Eligible
    InternalModelContinuationReport(eligibility,
      if (eligibility == InternalModelContinuationEligibility.Eligible) action else None,
      state, evidence.candidate, evidence.semanticdiff, evidence.review, evidence.approval, evidence.decisions, evidence.openissues, retained)
  }

  /** Dependency failures suppress only unavailable downstream owner calls, retaining their causes. */
  private def _predicate_problems(code: InternalModelContinuationCondition, evidence: Evidence): Vector[InternalModelContinuationProblem] = {
    val dependencies = code match {
      case ModelAdmitted => Set(ModelAdmitted)
      case SourceVersionsKnown => Set(SourceVersionsKnown)
      case DecisionsComplete => Set(DecisionsComplete)
      case MappingsComplete => Set(ModelAdmitted, MappingsComplete)
      case ReviewAdmitted => Set(ModelAdmitted, MappingsComplete, ReviewAdmitted)
      case HumanApproved | HumanUnresolvedItems => Set(ModelAdmitted, MappingsComplete, ReviewAdmitted, HumanApproved)
      case NoApprovalBlockingIssues | ApprovalBlockingIssues => Set(NoApprovalBlockingIssues)
    }
    evidence.problems.filter(value => dependencies.contains(value.predicate)).map(_.problem)
  }

  private def _condition_owner(code: InternalModelContinuationCondition, artifactid: Option[String],
    state: InternalModelRehydratedState, evidence: Evidence, continuity: InternalModelVerifiedProjectionContinuityPackage,
    request: InternalModelContinuationRequest): Boolean = {
    artifactid.forall { id =>
      state.cursor.selectedArtifacts.find(_.artifactId.value == id).exists { reference =>
        evidence.consumed.contains(reference) && (code match {
          case ModelAdmitted => reference == continuity.projection.reference || reference == continuity.realizationpackage.realization.reference
          case SourceVersionsKnown => reference.role == InternalModelArtifactRole.SourceSnapshot
          case DecisionsComplete => reference.role == InternalModelArtifactRole.Decision
          case MappingsComplete => request.candidateartifact.contains(reference)
          case ReviewAdmitted => request.reviewartifact.contains(reference)
          case HumanApproved | HumanUnresolvedItems => request.approvalartifact.contains(reference)
          case NoApprovalBlockingIssues | ApprovalBlockingIssues => reference.role == InternalModelArtifactRole.OpenIssue
        })
      }
    }
  }

  private def _problem(kind: InternalModelContinuationProblemKind, dimension: String, diagnostic: String,
    attribution: Option[InternalModelContinuationConditionAttribution] = None): InternalModelContinuationProblem =
    InternalModelContinuationProblem(kind, dimension, None, None, diagnostic, attribution)
}
