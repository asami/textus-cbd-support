package org.simplemodeling.textus.cbdsupport.impl

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.entity.{EntityConditionalTransition, EntityConditionalTransitionResult, EntityPersistent, EntitySnapshot, EntitySuccessorIntent, EntityTransitionDefinition, EntityTransitionField}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.id.UniversalId
import org.simplemodeling.model.directive.Update
import org.simplemodeling.textus.cbdsupport.entity.{ReviewAttestationSnapshot as ReviewAttestationSnapshotEntity, ReviewDiagnosis as ReviewDiagnosisEntity, ReviewReportSnapshot as ReviewReportSnapshotEntity, ReviewRunSnapshot as ReviewRunSnapshotEntity, ReviewTargetSnapshot as ReviewTargetSnapshotEntity}
import org.simplemodeling.textus.cbdsupport.entity.create.{ReviewAttestationSnapshot as ReviewAttestationSnapshotCreate, ReviewReportSnapshot as ReviewReportSnapshotCreate, ReviewRunSnapshot as ReviewRunSnapshotCreate, ReviewTargetSnapshot as ReviewTargetSnapshotCreate}
import org.simplemodeling.textus.cbdsupport.entity.update.{ReviewDiagnosis as ReviewDiagnosisUpdate}
import org.simplemodeling.textus.cbdsupport.runtime.*
import org.simplemodeling.textus.cbdsupport.value.{ComponentName as EntityComponentName, ComponentOrganization as EntityComponentOrganization, ReviewAttestationId as EntityReviewAttestationId, ReviewDigest as EntityReviewDigest, ReviewId as EntityReviewId, ReviewInstant as EntityReviewInstant, ReviewProfile as EntityReviewProfile, ReviewReportId as EntityReviewReportId, ReviewRunState as EntityReviewRunState}

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[cbdsupport] final class ReviewProductionSettlementProgram(
  val core: ActionCall.Core
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  def completeFromJob(
    lease: CarReviewProductionTerminalLease.Completed
  ): ExecUowM[CarReviewDiagnosisAdmission.Reused] =
    for {
      diagnosisid <- exec_from(_diagnosis_id(lease))
      snapshot <- entity_load_snapshot_internal[ReviewDiagnosisEntity](diagnosisid)
      result <- snapshot.entity.state.value match {
        case "claimed" => _complete_claimed(snapshot, diagnosisid, lease)
        case "completed" => _complete_replay(snapshot.entity, diagnosisid, lease)
        case _ => exec_from(Consequence.operationInvalid("review-job-lease-completion-conflict"))
      }
    } yield result

  def recordTerminalFromJob(
    lease: CarReviewProductionTerminalLease.Failed
  ): ExecUowM[Unit] =
    _record_terminal(lease, CarReviewDiagnosisTerminalState.Failed)

  def recordTerminalFromJob(
    lease: CarReviewProductionTerminalLease.Cancelled
  ): ExecUowM[Unit] =
    _record_terminal(lease, CarReviewDiagnosisTerminalState.Cancelled)

  /* Entity conditional transitions can atomically publish the root and one
   * successor only. Target/Run/Attestation are deterministic pending members
   * while the root is claimed; root plus Report is the read visibility boundary. */
  private def _complete_claimed(
    snapshot: EntitySnapshot[ReviewDiagnosisEntity],
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease.Completed
  ): ExecUowM[CarReviewDiagnosisAdmission.Reused] =
    for {
      _ <- exec_from(_validate_completed_lease(lease))
      _ <- exec_from(_validate_claimed_root(snapshot.entity, lease))
      material <- exec_from(_material(diagnosisid, lease))
      _ <- _claim_target(material.target, lease)
      _ <- _claim_run(material.run, lease)
      _ <- _claim_attestation(material.attestation, lease)
      transition <- exec_from(_completion_transition(snapshot, material.report, lease))
      result <- entity_conditional_transition_internal(transition)
      settled <- _completion_result(result, diagnosisid, lease, material)
    } yield settled

  private def _complete_replay(
    diagnosis: ReviewDiagnosisEntity,
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease.Completed
  ): ExecUowM[CarReviewDiagnosisAdmission.Reused] =
    for {
      material <- exec_from(_material(diagnosisid, lease))
      _ <- exec_from(_validate_completed_root(diagnosis, lease))
      _ <- _validate_members(diagnosisid, material, lease)
    } yield _reused(diagnosisid, lease)

  private def _completion_result(
    result: EntityConditionalTransitionResult[ReviewDiagnosisEntity, ReviewReportSnapshotEntity],
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease.Completed,
    material: CompletionMaterial
  ): ExecUowM[CarReviewDiagnosisAdmission.Reused] = result match {
    case EntityConditionalTransitionResult.Transitioned(_, _) => exec_from(Consequence.success(_reused(diagnosisid, lease)))
    case EntityConditionalTransitionResult.NotMatched(root) =>
      for {
        _ <- exec_from(_validate_completed_root(root.entity, lease))
        _ <- _validate_members(diagnosisid, material, lease)
      } yield _reused(diagnosisid, lease)
  }

  private def _record_terminal(
    lease: CarReviewProductionTerminalLease,
    state: CarReviewDiagnosisTerminalState
  ): ExecUowM[Unit] =
    for {
      diagnosisid <- exec_from(_diagnosis_id(lease))
      snapshot <- entity_load_snapshot_internal[ReviewDiagnosisEntity](diagnosisid)
      _ <- snapshot.entity.state.value match {
        case "claimed" => _terminal_claimed(snapshot, diagnosisid, lease, state)
        case value if value == state.value => _terminal_replay(snapshot.entity, diagnosisid, lease, state)
        case _ => exec_from(Consequence.operationInvalid("review-job-lease-terminal-conflict"))
      }
    } yield ()

  private def _terminal_claimed(
    snapshot: EntitySnapshot[ReviewDiagnosisEntity],
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease,
    state: CarReviewDiagnosisTerminalState
  ): ExecUowM[Unit] =
    for {
      _ <- exec_from(_validate_terminal_lease(lease, state))
      _ <- exec_from(_validate_claimed_root(snapshot.entity, lease))
      run <- exec_from(_terminal_run(diagnosisid, lease, state))
      transition <- exec_from(_terminal_transition(snapshot, run, lease, state))
      result <- entity_conditional_transition_internal(transition)
      _ <- result match {
        case EntityConditionalTransitionResult.Transitioned(_, _) => exec_from(Consequence.unit)
        case EntityConditionalTransitionResult.NotMatched(root) => _terminal_replay(root.entity, diagnosisid, lease, state)
      }
    } yield ()

  private def _terminal_replay(
    root: ReviewDiagnosisEntity,
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease,
    state: CarReviewDiagnosisTerminalState
  ): ExecUowM[Unit] =
    for {
      candidate <- exec_from(_terminal_run(diagnosisid, lease, state))
      _ <- exec_from(_validate_terminal_root(root, lease, state))
      run <- entity_load_internal[ReviewRunSnapshotEntity](candidate.id.get)
      _ <- exec_from(_run_matches(run, candidate, lease))
    } yield ()

  private final case class CompletionMaterial(
    target: ReviewTargetSnapshotCreate,
    run: ReviewRunSnapshotCreate,
    report: ReviewReportSnapshotCreate,
    attestation: ReviewAttestationSnapshotCreate
  )

  private def _material(
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    lease: CarReviewProductionTerminalLease.Completed
  ): Consequence[CompletionMaterial] =
    for {
      rundocument <- _encode_run(lease.run)
      reportdocument <- _encode_report(lease.response.report)
      attestationdocument <- _encode_attestation(lease.response.attestation)
      target <- _target(diagnosisid, lease)
      run <- _run(diagnosisid, lease, rundocument, "run")
      report <- _report(diagnosisid, lease, reportdocument)
      attestation <- _attestation(diagnosisid, lease, attestationdocument)
    } yield CompletionMaterial(target, run, report, attestation)

  private def _diagnosis_id(lease: CarReviewProductionTerminalLease): Consequence[org.simplemodeling.model.datatype.EntityId] =
    org.simplemodeling.model.datatype.EntityId.parse(lease.binding.diagnosisId).flatMap { id =>
      if id.print == lease.binding.diagnosisId then Consequence.success(id)
      else Consequence.operationInvalid("review-job-lease-diagnosis-id-invalid")
    }

  private def _validate_claimed_root(root: ReviewDiagnosisEntity, lease: CarReviewProductionTerminalLease): Consequence[Unit] =
    if _root_matches(root, lease) && root.state.value == "claimed" && root.report_id.isEmpty && root.report_digest.isEmpty then Consequence.unit
    else Consequence.operationInvalid("review-job-lease-root-mismatch")

  private def _validate_completed_root(root: ReviewDiagnosisEntity, lease: CarReviewProductionTerminalLease.Completed): Consequence[Unit] =
    if _root_matches(root, lease) && root.state.value == "completed" &&
        root.completed_at.exists(value => lease.run.completedAt.exists(done => _same_instant(value.value, done.value))) &&
        root.report_id.exists(_.value == lease.response.report.reportId.value) &&
        root.report_digest.exists(_.value == lease.response.report.reportDigest.value) then Consequence.unit
    else Consequence.operationInvalid("review-job-lease-completion-conflict")

  private def _validate_terminal_root(root: ReviewDiagnosisEntity, lease: CarReviewProductionTerminalLease, state: CarReviewDiagnosisTerminalState): Consequence[Unit] =
    if _root_matches(root, lease) && root.state.value == state.value &&
        root.completed_at.exists(value => lease.run.completedAt.exists(done => _same_instant(value.value, done.value))) &&
        root.report_id.isEmpty && root.report_digest.isEmpty then Consequence.unit
    else Consequence.operationInvalid("review-job-lease-terminal-conflict")

  private def _root_matches(root: ReviewDiagnosisEntity, lease: CarReviewProductionTerminalLease): Boolean =
    root.id.print == lease.binding.diagnosisId && root.key_definition_id.value == lease.binding.reuseKeyDefinition &&
      root.reuse_key_digest.value == lease.binding.reuseKeyDigest.value && root.target_digest.value == lease.binding.target.digest.value &&
      root.profile.value == lease.binding.profile.value && root.active_review_id.exists(_.value == lease.binding.reviewId.value)

  private def _validate_completed_lease(lease: CarReviewProductionTerminalLease.Completed): Consequence[Unit] = {
    val report = lease.response.report
    CarReviewAttestationCodec.fromReport(report).fold(
      error => Consequence.operationInvalid(error.code),
      attestation => if lease.run.state == ReviewRunState("completed") && lease.run.reviewId == lease.binding.reviewId &&
          lease.run.target == lease.binding.target && lease.run.profile == lease.binding.profile &&
          lease.run.startedAt == lease.binding.startedAt && lease.run.completedAt.contains(report.execution.completedAt) &&
          lease.run.reportId.contains(report.reportId) && lease.run.reportDigest.contains(report.reportDigest) &&
          lease.run.providers == report.execution.providers && lease.run.limitations == report.limitations &&
          report.reviewId == lease.binding.reviewId && report.target == lease.binding.target && report.profile == lease.binding.profile &&
          report.execution.startedAt == lease.binding.startedAt && report.createdAt == report.execution.completedAt &&
          lease.response.gate == report.gate && lease.response.attestation == attestation then Consequence.unit
        else Consequence.operationInvalid("review-job-lease-completed-binding-invalid")
    )
  }

  private def _validate_terminal_lease(lease: CarReviewProductionTerminalLease, state: CarReviewDiagnosisTerminalState): Consequence[Unit] = {
    val run = lease.run
    val valid = state match {
      case CarReviewDiagnosisTerminalState.Failed => run.state == ReviewRunState("failed") && run.failureCode.nonEmpty
      case CarReviewDiagnosisTerminalState.Cancelled => run.state == ReviewRunState("cancelled") && run.failureCode.isEmpty
      case _ => false
    }
    if valid && run.reviewId == lease.binding.reviewId && run.target == lease.binding.target && run.profile == lease.binding.profile &&
        run.startedAt == lease.binding.startedAt && run.completedAt.nonEmpty && run.reportId.isEmpty && run.reportDigest.isEmpty then Consequence.unit
    else Consequence.operationInvalid("review-job-lease-terminal-binding-invalid")
  }

  private def _target(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease): Consequence[ReviewTargetSnapshotCreate] =
    ReviewTargetSnapshotCreate.Builder().withDiagnosis_id(id)
      .withTarget_kind(org.simplemodeling.textus.cbdsupport.value.ReviewTargetKind(lease.binding.target.kind.value))
      .withComponent_name(EntityComponentName(lease.binding.target.name)).withTarget_digest(EntityReviewDigest(lease.binding.target.digest.value))
      .withCreated_at(EntityReviewInstant(lease.binding.startedAt.value)).buildC().map(_.copy(
        id = Some(_snapshot_id("target", id, lease.binding.target.digest.value, ReviewTargetSnapshotCreate.collectionId)),
        organization = lease.binding.target.organization.map(EntityComponentOrganization.apply),
        component_version = lease.binding.target.version.map(value => org.simplemodeling.textus.cbdsupport.value.ReviewVersion(value.value))
      ))

  private def _run(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease, document: String, kind: String): Consequence[ReviewRunSnapshotCreate] =
    lease.run.completedAt.fold[Consequence[ReviewRunSnapshotCreate]](Consequence.operationInvalid("review-job-lease-run-completed-at-missing")) { completed =>
      ReviewRunSnapshotCreate.Builder().withDiagnosis_id(id).withReview_id(EntityReviewId(lease.run.reviewId.value))
        .withState(EntityReviewRunState(lease.run.state.value)).withProfile(EntityReviewProfile(lease.run.profile.value))
        .withRun_document(ReviewDiagnosisEntityPrograms._json_document(document)).withStarted_at(EntityReviewInstant(lease.run.startedAt.value)).buildC().map(_.copy(
          id = Some(_snapshot_id(kind, id, lease.run.reviewId.value, ReviewRunSnapshotCreate.collectionId)), completed_at = Some(EntityReviewInstant(completed.value))
        ))
    }

  private def _terminal_run(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease, state: CarReviewDiagnosisTerminalState): Consequence[ReviewRunSnapshotCreate] =
    for { document <- _encode_run(lease.run); run <- _run(id, lease, document, s"terminal-${state.value}") } yield run

  private def _report(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease.Completed, document: String): Consequence[ReviewReportSnapshotCreate] =
    ReviewReportSnapshotCreate.Builder().withDiagnosis_id(id).withReview_id(EntityReviewId(lease.response.report.reviewId.value))
      .withAttestation_id(EntityReviewAttestationId(lease.response.attestation.attestationId.value)).withReport_id(EntityReviewReportId(lease.response.report.reportId.value))
      .withReport_digest(EntityReviewDigest(lease.response.report.reportDigest.value)).withReport_document(ReviewDiagnosisEntityPrograms._json_document(document))
      .withCreated_at(EntityReviewInstant(lease.response.report.createdAt.value)).buildC().map(_.copy(
        id = Some(_snapshot_id("report", id, lease.response.report.reportId.value, ReviewReportSnapshotCreate.collectionId))
      ))

  private def _attestation(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease.Completed, document: String): Consequence[ReviewAttestationSnapshotCreate] =
    ReviewAttestationSnapshotCreate.Builder().withDiagnosis_id(id).withReview_id(EntityReviewId(lease.response.attestation.reviewId.value))
      .withReport_id(EntityReviewReportId(lease.response.attestation.reportId.value)).withReport_digest(EntityReviewDigest(lease.response.attestation.reportDigest.value))
      .withAttestation_document(ReviewDiagnosisEntityPrograms._json_document(document)).withCreated_at(EntityReviewInstant(lease.response.attestation.createdAt.value)).buildC().map(_.copy(
        id = Some(_snapshot_id("attestation", id, lease.response.attestation.attestationId.value, ReviewAttestationSnapshotCreate.collectionId))
      ))

  private def _claim_target(candidate: ReviewTargetSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): ExecUowM[Unit] =
    entity_claim_or_load_internal[ReviewTargetSnapshotCreate, ReviewTargetSnapshotEntity](candidate).flatMap {
      case _: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Claimed[ReviewTargetSnapshotCreate] @unchecked => exec_from(Consequence.unit)
      case loaded: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Loaded[ReviewTargetSnapshotEntity] @unchecked => exec_from(_target_matches(loaded.entity, candidate, lease))
    }

  private def _claim_run(candidate: ReviewRunSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): ExecUowM[Unit] =
    entity_claim_or_load_internal[ReviewRunSnapshotCreate, ReviewRunSnapshotEntity](candidate).flatMap {
      case _: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Claimed[ReviewRunSnapshotCreate] @unchecked => exec_from(Consequence.unit)
      case loaded: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Loaded[ReviewRunSnapshotEntity] @unchecked => exec_from(_run_matches(loaded.entity, candidate, lease))
    }

  private def _claim_attestation(candidate: ReviewAttestationSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): ExecUowM[Unit] =
    entity_claim_or_load_internal[ReviewAttestationSnapshotCreate, ReviewAttestationSnapshotEntity](candidate).flatMap {
      case _: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Claimed[ReviewAttestationSnapshotCreate] @unchecked => exec_from(Consequence.unit)
      case loaded: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Loaded[ReviewAttestationSnapshotEntity] @unchecked => exec_from(_attestation_matches(loaded.entity, candidate, lease))
    }

  private def _validate_members(id: org.simplemodeling.model.datatype.EntityId, material: CompletionMaterial, lease: CarReviewProductionTerminalLease.Completed): ExecUowM[Unit] =
    for {
      target <- entity_load_internal[ReviewTargetSnapshotEntity](material.target.id.get)
      run <- entity_load_internal[ReviewRunSnapshotEntity](material.run.id.get)
      report <- entity_load_internal[ReviewReportSnapshotEntity](material.report.id.get)
      attestation <- entity_load_internal[ReviewAttestationSnapshotEntity](material.attestation.id.get)
      _ <- exec_from(_target_matches(target, material.target, lease))
      _ <- exec_from(_run_matches(run, material.run, lease))
      _ <- exec_from(_report_matches(report, material.report, lease))
      _ <- exec_from(_attestation_matches(attestation, material.attestation, lease))
    } yield ()

  private def _completion_transition(snapshot: EntitySnapshot[ReviewDiagnosisEntity], report: ReviewReportSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): Consequence[EntityConditionalTransition[ReviewDiagnosisEntity, ReviewDiagnosisUpdate, ReviewReportSnapshotEntity]] =
    _expectation(snapshot).flatMap { expectation =>
      EntitySuccessorIntent.create[ReviewReportSnapshotCreate, ReviewReportSnapshotEntity](report).flatMap { intent =>
        EntityConditionalTransition.create(snapshot.entity.id, expectation, _completion_update(lease), intent)
      }
    }

  private def _terminal_transition(snapshot: EntitySnapshot[ReviewDiagnosisEntity], run: ReviewRunSnapshotCreate, lease: CarReviewProductionTerminalLease, state: CarReviewDiagnosisTerminalState): Consequence[EntityConditionalTransition[ReviewDiagnosisEntity, ReviewDiagnosisUpdate, ReviewRunSnapshotEntity]] =
    lease.run.completedAt.fold[Consequence[EntityConditionalTransition[ReviewDiagnosisEntity, ReviewDiagnosisUpdate, ReviewRunSnapshotEntity]]](Consequence.operationInvalid("review-job-lease-run-completed-at-missing")) { completed =>
      _expectation(snapshot).flatMap { expectation =>
        EntitySuccessorIntent.create[ReviewRunSnapshotCreate, ReviewRunSnapshotEntity](run).flatMap { intent =>
          EntityConditionalTransition.create(snapshot.entity.id, expectation, _terminal_update(state, completed), intent)
        }
      }
    }

  private def _expectation(snapshot: EntitySnapshot[ReviewDiagnosisEntity]) = {
    val rootpersistent = summon[EntityPersistent[ReviewDiagnosisEntity]]
    for {
      statefield <- EntityTransitionField.encoded[ReviewDiagnosisEntity, EntityReviewRunState]("state", rootpersistent)(_.value)
      activefield <- EntityTransitionField.encoded[ReviewDiagnosisEntity, EntityReviewId]("active_review_id", rootpersistent)(_.value)
      definition <- EntityTransitionDefinition.create(rootpersistent, Vector(statefield, activefield))
      expectedstate <- statefield.expected(snapshot.entity.state)
      activeid <- snapshot.entity.active_review_id.toRight("review-job-lease-root-mismatch").fold(Consequence.operationInvalid, Consequence.success)
      expectedactive <- activefield.expected(activeid)
      expectation <- definition.expectation(snapshot.revision, expectedstate, expectedactive)
    } yield expectation
  }

  private def _target_matches(snapshot: ReviewTargetSnapshotEntity, candidate: ReviewTargetSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): Consequence[Unit] =
    if snapshot.id != candidate.id.get then Consequence.operationInvalid("review-job-lease-target-snapshot-id-conflict")
    else if snapshot.diagnosis_id != candidate.diagnosis_id then Consequence.operationInvalid("review-job-lease-target-snapshot-diagnosis-conflict")
    else if snapshot.target_kind.value != candidate.target_kind.value || snapshot.organization != candidate.organization || snapshot.component_name.value != candidate.component_name.value then Consequence.operationInvalid("review-job-lease-target-snapshot-identity-conflict")
    else if snapshot.component_version != candidate.component_version then Consequence.operationInvalid("review-job-lease-target-snapshot-version-conflict")
    else if snapshot.target_digest.value != lease.binding.target.digest.value then Consequence.operationInvalid("review-job-lease-target-snapshot-digest-conflict")
    else if !_valid_created(snapshot.created_at.value, lease.binding.startedAt.value) then Consequence.operationInvalid("review-job-lease-target-snapshot-created-at-conflict")
    else Consequence.unit

  private def _run_matches(snapshot: ReviewRunSnapshotEntity, candidate: ReviewRunSnapshotCreate, lease: CarReviewProductionTerminalLease): Consequence[Unit] =
    _run_document(snapshot.run_document.value.value, lease.run).flatMap { _ =>
      if snapshot.id == candidate.id.get && snapshot.diagnosis_id == candidate.diagnosis_id && snapshot.review_id.value == lease.run.reviewId.value &&
          snapshot.state.value == lease.run.state.value && snapshot.profile.value == lease.run.profile.value &&
          _same_instant(snapshot.started_at.value, lease.run.startedAt.value) && snapshot.completed_at.exists(value => lease.run.completedAt.exists(done => _same_instant(value.value, done.value))) then Consequence.unit
      else Consequence.operationInvalid("review-job-lease-run-snapshot-conflict")
    }

  private def _report_matches(snapshot: ReviewReportSnapshotEntity, candidate: ReviewReportSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): Consequence[Unit] =
    _report_document(snapshot.report_document.value.value, lease.response.report).flatMap { _ =>
      if snapshot.id == candidate.id.get && snapshot.diagnosis_id == candidate.diagnosis_id && snapshot.review_id.value == lease.response.report.reviewId.value &&
          snapshot.attestation_id.value == lease.response.attestation.attestationId.value && snapshot.report_id.value == lease.response.report.reportId.value &&
          snapshot.report_digest.value == lease.response.report.reportDigest.value && _valid_created(snapshot.created_at.value, lease.response.report.createdAt.value) then Consequence.unit
      else Consequence.operationInvalid("review-job-lease-report-snapshot-conflict")
    }

  private def _attestation_matches(snapshot: ReviewAttestationSnapshotEntity, candidate: ReviewAttestationSnapshotCreate, lease: CarReviewProductionTerminalLease.Completed): Consequence[Unit] =
    _attestation_document(snapshot.attestation_document.value.value, lease.response.attestation).flatMap { _ =>
      if snapshot.id == candidate.id.get && snapshot.diagnosis_id == candidate.diagnosis_id && snapshot.review_id.value == lease.response.attestation.reviewId.value &&
          snapshot.report_id.value == lease.response.attestation.reportId.value && snapshot.report_digest.value == lease.response.attestation.reportDigest.value && _valid_created(snapshot.created_at.value, lease.response.attestation.createdAt.value) then Consequence.unit
      else Consequence.operationInvalid("review-job-lease-attestation-snapshot-conflict")
    }

  private def _run_document(document: String, expected: CarReviewRun): Consequence[Unit] =
    CarReviewRunCodec.decode(document).fold(error => Consequence.operationInvalid(error.code), decoded => _encode_run(decoded).flatMap(canonical => _encode_run(expected).flatMap(expectedvalue => if canonical == document && expectedvalue == document then Consequence.unit else Consequence.operationInvalid("review-job-lease-run-document-conflict"))))

  private def _report_document(document: String, expected: CarReviewReport): Consequence[Unit] =
    CarReviewReportCodec.decode(document).fold(error => Consequence.operationInvalid(error.code), decoded => _encode_report(decoded).flatMap(canonical => _encode_report(expected).flatMap(expectedvalue => if canonical == document && expectedvalue == document then Consequence.unit else Consequence.operationInvalid("review-job-lease-report-document-conflict"))))

  private def _attestation_document(document: String, expected: CarReviewAttestation): Consequence[Unit] =
    _encode_attestation(expected).flatMap(value => if value == document then Consequence.unit else Consequence.operationInvalid("review-job-lease-attestation-document-conflict"))

  private def _completion_update(lease: CarReviewProductionTerminalLease.Completed): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(state = Update.set(EntityReviewRunState("completed")), completedat = Update.set(EntityReviewInstant(lease.response.report.execution.completedAt.value)), reportid = Update.set(EntityReviewReportId(lease.response.report.reportId.value)), reportdigest = Update.set(EntityReviewDigest(lease.response.report.reportDigest.value)))

  private def _terminal_update(state: CarReviewDiagnosisTerminalState, completed: ReviewInstant): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(state = Update.set(EntityReviewRunState(state.value)), completedat = Update.set(EntityReviewInstant(completed.value)), reportid = Update.setNull, reportdigest = Update.setNull)

  private def _encode_run(value: CarReviewRun): Consequence[String] = CarReviewRunCodec.encode(value).fold(error => Consequence.operationInvalid(error.code), Consequence.success)
  private def _encode_report(value: CarReviewReport): Consequence[String] = CarReviewReportCodec.encode(value).fold(error => Consequence.operationInvalid(error.code), Consequence.success)
  private def _encode_attestation(value: CarReviewAttestation): Consequence[String] = CarReviewAttestationCodec.encode(value).fold(error => Consequence.operationInvalid(error.code), Consequence.success)
  private def _same_instant(left: String, right: String): Boolean = (scala.util.Try(Instant.parse(left)).toOption, scala.util.Try(Instant.parse(right)).toOption) match { case (Some(a), Some(b)) => a == b; case _ => false }
  /* Generated Entity storage collides lifecycle createdAt with domain created_at; canonical documents are exact authority. */
  private def _valid_created(actual: String, earliest: String): Boolean = (scala.util.Try(Instant.parse(actual)).toOption, scala.util.Try(Instant.parse(earliest)).toOption) match { case (Some(a), Some(b)) => !a.isBefore(b); case _ => false }
  private def _reused(id: org.simplemodeling.model.datatype.EntityId, lease: CarReviewProductionTerminalLease.Completed): CarReviewDiagnosisAdmission.Reused = CarReviewDiagnosisAdmission.Reused(id.print, lease.response.report.reviewId, lease.response.report.reportId, lease.response.report.reportDigest)
  private def _snapshot_id(kind: String, diagnosis: org.simplemodeling.model.datatype.EntityId, identity: String, collection: org.simplemodeling.model.datatype.EntityCollectionId): org.simplemodeling.model.datatype.EntityId = {
    val key = "d" + UUID.nameUUIDFromBytes(s"${diagnosis.value}:$kind:$identity".getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId(collection.major, collection.minor, collection, timestamp = Some(UniversalId.StableTimestamp), entropy = Some(key))
  }
}
