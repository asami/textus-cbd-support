package org.simplemodeling.textus.cbdsupport.impl

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityConditionalTransition, EntityConditionalTransitionResult, EntityPersistent, EntitySnapshot, EntitySuccessorIntent, EntityTransitionDefinition, EntityTransitionField, EntityQuery}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.id.UniversalId
import org.simplemodeling.model.directive.{Condition, Update}
import org.simplemodeling.model.value.{AuditAttributesUpdate, ContentAttributesUpdate, ContextualAttributesUpdate, DescriptiveAttributesUpdate, LifecycleAttributesUpdate, MediaAttributesUpdate, NameAttributesUpdate, PublicationAttributesUpdate, ResourceAttributesUpdate, SecurityAttributesUpdate}
import org.simplemodeling.textus.cbdsupport.entity.{ReviewDiagnosis as ReviewDiagnosisEntity, ReviewTargetSnapshot as ReviewTargetSnapshotEntity, ReviewRunSnapshot as ReviewRunSnapshotEntity, ReviewReportSnapshot as ReviewReportSnapshotEntity, ReviewAttestationSnapshot as ReviewAttestationSnapshotEntity}
import org.simplemodeling.textus.cbdsupport.entity.create.{ReviewDiagnosis as ReviewDiagnosisCreate, ReviewTargetSnapshot as ReviewTargetSnapshotCreate, ReviewRunSnapshot as ReviewRunSnapshotCreate, ReviewReportSnapshot as ReviewReportSnapshotCreate, ReviewAttestationSnapshot as ReviewAttestationSnapshotCreate, ReviewRetentionEvent as ReviewRetentionEventCreate}
import org.simplemodeling.textus.cbdsupport.entity.query.{ReviewDiagnosis as ReviewDiagnosisQuery, ReviewReportSnapshot as ReviewReportSnapshotQuery, ReviewAttestationSnapshot as ReviewAttestationSnapshotQuery}
import org.simplemodeling.textus.cbdsupport.entity.update.{ReviewDiagnosis as ReviewDiagnosisUpdate}
import org.simplemodeling.textus.cbdsupport.runtime.{CarReviewAttestationCodec, CarReviewAuthorization, CarReviewCanonicalResponse, CarReviewDiagnosisAdmission, CarReviewDiagnosisTerminalState, CarReviewExecutionPlan, CarReviewProductionTerminalLease, CarReviewReport, CarReviewReportCodec, CarReviewRetentionPolicy, CarReviewRun, CarReviewRunCodec, CarReviewRunLifecycle, CarReviewRunVocabulary, CarReviewVocabulary, ReviewDigest, ReviewDocumentType, ReviewId, ReviewInstant, ReviewReportId, ReviewRunState, ReviewSchemaVersion}
import org.simplemodeling.textus.cbdsupport.value.{ComponentName as EntityComponentName, ComponentOrganization as EntityComponentOrganization, ReviewAttestationId as EntityReviewAttestationId, ReviewDigest as EntityReviewDigest, ReviewId as EntityReviewId, ReviewInstant as EntityReviewInstant, ReviewProfile as EntityReviewProfile, ReviewReportId as EntityReviewReportId, ReviewReuseKeyDefinition as EntityReviewReuseKeyDefinition, ReviewRunState as EntityReviewRunState, ReviewRetentionAction as EntityReviewRetentionAction, ReviewRetentionRecordType as EntityReviewRetentionRecordType, ReviewSubmissionDocument as EntityReviewSubmissionDocument}

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[cbdsupport] final class ReviewDiagnosisAdmissionProgram(
  val core: ActionCall.Core
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  def admit(plan: CarReviewExecutionPlan): ExecUowM[CarReviewDiagnosisAdmission] =
    for {
      existing <- entity_load_option_internal[ReviewDiagnosisEntity](_diagnosis_id(plan))
      admission <- existing match {
        case Some(_) =>
          for {
            snapshot <- entity_load_snapshot_internal[ReviewDiagnosisEntity](_diagnosis_id(plan))
            result <- _admission_from_snapshot(snapshot, plan)
          } yield result
        case None =>
          for {
            candidate <- exec_from(_diagnosis_candidate(plan))
            claimed <- entity_claim_or_load_internal[ReviewDiagnosisCreate, ReviewDiagnosisEntity](candidate)
            result <- exec_from(_admission_from(claimed, plan))
          } yield result
      }
    } yield admission

  def admitAndStart(
    plan: CarReviewExecutionPlan
  )(
    start: CarReviewDiagnosisAdmission.Owner => ExecUowM[Unit]
  ): ExecUowM[CarReviewDiagnosisAdmission] =
    admit(plan).flatMap {
      case owner: CarReviewDiagnosisAdmission.Owner =>
        start(owner).map(_ => owner)
      case existing =>
        exec_from(Consequence.success(existing))
    }

  private def _admission_from_snapshot(
    snapshot: EntitySnapshot[ReviewDiagnosisEntity],
    plan: CarReviewExecutionPlan
  ): ExecUowM[CarReviewDiagnosisAdmission] =
    snapshot.entity.state.value match {
      case "failed" | "cancelled" | "expired" | "incompatible" =>
        for {
          transition <- exec_from(_successor_transition(snapshot, plan))
          result <- entity_conditional_transition_internal(transition)
          admission <- exec_from(_admission_from_transition(result, plan))
        } yield admission
      case _ =>
        exec_from(_admission_from_loaded(snapshot.entity, plan))
    }

  private def _successor_transition(
    snapshot: EntitySnapshot[ReviewDiagnosisEntity],
    plan: CarReviewExecutionPlan
  ): Consequence[
    EntityConditionalTransition[
      ReviewDiagnosisEntity,
      ReviewDiagnosisUpdate,
      ReviewRunSnapshotEntity
    ]
  ] = {
    val rootpersistent = summon[EntityPersistent[ReviewDiagnosisEntity]]
    for {
      _ <- _validate_identity(snapshot.entity, plan)
      activeid <- snapshot.entity.active_review_id match {
        case Some(value) => Consequence.success(value)
        case None =>
          Consequence.operationInvalid(
            "review-diagnosis-terminal-run-missing"
          )
      }
      statefield <-
        EntityTransitionField.encoded[
          ReviewDiagnosisEntity,
          EntityReviewRunState
        ]("state", rootpersistent)(_.value)
      activefield <-
        EntityTransitionField.encoded[
          ReviewDiagnosisEntity,
          EntityReviewId
        ]("active_review_id", rootpersistent)(_.value)
      definition <- EntityTransitionDefinition.create(
        rootpersistent,
        Vector(statefield, activefield)
      )
      expectedstate <- statefield.expected(snapshot.entity.state)
      expectedactive <- activefield.expected(activeid)
      expectation <- definition.expectation(
        snapshot.revision,
        expectedstate,
        expectedactive
      )
      successor <- _successor_snapshot(snapshot.entity.id, plan)
      intent <- EntitySuccessorIntent.create[
        ReviewRunSnapshotCreate,
        ReviewRunSnapshotEntity
      ](successor)
      transition <- EntityConditionalTransition.create(
        snapshot.entity.id,
        expectation,
        _successor_root_update(plan),
        intent
      )
    } yield transition
  }

  private def _successor_root_update(
    plan: CarReviewExecutionPlan
  ): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(
      state = Update.set(EntityReviewRunState("claimed")),
      activereviewid = Update.set(EntityReviewId(plan.request.reviewId.value)),
      completedat = Update.setNull,
      reportid = Update.setNull,
      reportdigest = Update.setNull
    )

  private def _successor_snapshot(
    diagnosisid: org.simplemodeling.model.datatype.EntityId,
    plan: CarReviewExecutionPlan
  ): Consequence[ReviewRunSnapshotCreate] =
    CarReviewRunLifecycle
      .admitted(
        plan.request.reviewId,
        plan.request.target,
        plan.request.profile,
        plan.request.startedAt
      )
      .left
      .map(_.code) match {
      case Left(code) =>
        Consequence.operationInvalid(code)
      case Right(run) =>
        CarReviewRunCodec.encode(run).left.map(_.code) match {
          case Left(code) =>
            Consequence.operationInvalid(code)
          case Right(document) =>
            ReviewRunSnapshotCreate.Builder()
              .withDiagnosis_id(diagnosisid)
              .withReview_id(EntityReviewId(plan.request.reviewId.value))
              .withState(EntityReviewRunState("admitted"))
              .withProfile(EntityReviewProfile(plan.request.profile.value))
              .withRun_document(ReviewDiagnosisEntityPrograms._json_document(document))
              .withStarted_at(EntityReviewInstant(plan.request.startedAt.value))
              .buildC()
              .map(_.copy(
                id = Some(_snapshot_id(
                  "successor",
                  diagnosisid,
                  plan.request.reviewId.value,
                  ReviewRunSnapshotCreate.collectionId
                ))
              ))
        }
    }

  private def _admission_from_transition(
    result:
      EntityConditionalTransitionResult[
        ReviewDiagnosisEntity,
        ReviewRunSnapshotEntity
      ],
    plan: CarReviewExecutionPlan
  ): Consequence[CarReviewDiagnosisAdmission] =
    result match {
      case EntityConditionalTransitionResult.Transitioned(root, _) =>
        Consequence.success(
          CarReviewDiagnosisAdmission.Owner.issue(
            root.entity.id.print,
            plan.request.reviewId,
            plan.reuseKey.digest
          )
        )
      case EntityConditionalTransitionResult.NotMatched(existing) =>
        _admission_from_loaded(existing.entity, plan)
    }

  def complete(
    owner: CarReviewDiagnosisAdmission.Owner,
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): ExecUowM[CarReviewDiagnosisAdmission] = {
    val encoded = _completion_documents(plan, response)
    for {
      documents <- exec_from(encoded)
      diagnosis <- _existing_diagnosis(plan)
      _ <- exec_from(_validate_owner(owner, diagnosis, plan, response))
      diagnosisid = _diagnosis_id(plan)
      target <- exec_from(_target_snapshot(diagnosisid, plan))
      run <- exec_from(_run_snapshot(diagnosisid, plan, response, documents.runDocument))
      report <- exec_from(_report_snapshot(diagnosisid, response, documents.reportDocument))
      attestation <- exec_from(_attestation_snapshot(diagnosisid, response, documents.attestationDocument))
      _ <- entity_create_internal(target)
      _ <- entity_create_internal(run)
      _ <- entity_create_internal(report)
      _ <- entity_create_internal(attestation)
      _ <- _update_diagnosis(diagnosisid, _completion_update(response))
    } yield CarReviewDiagnosisAdmission.Reused(
      diagnosisid.print,
      response.report.reviewId,
      response.report.reportId,
      response.report.reportDigest
    )
  }

  def recordTerminal(
    owner: CarReviewDiagnosisAdmission.Owner,
    plan: CarReviewExecutionPlan,
    state: CarReviewDiagnosisTerminalState,
    runDocument: String,
    completedAt: ReviewInstant
  ): ExecUowM[Unit] = {
    for {
      current <- _existing_diagnosis(plan)
      _ <- exec_from(_validate_terminal_owner(owner, current, plan))
      diagnosisid = _diagnosis_id(plan)
      run <- exec_from(ReviewRunSnapshotCreate.Builder()
        .withDiagnosis_id(diagnosisid)
        .withReview_id(EntityReviewId(plan.request.reviewId.value))
        .withState(EntityReviewRunState(state.value))
        .withProfile(EntityReviewProfile(plan.request.profile.value))
        .withRun_document(ReviewDiagnosisEntityPrograms._json_document(runDocument))
        .withStarted_at(EntityReviewInstant(plan.request.startedAt.value))
        .buildC().map(_.copy(
          id = Some(_snapshot_id(s"terminal-${state.value}", diagnosisid, plan.request.reviewId.value, ReviewRunSnapshotCreate.collectionId)),
          completed_at = Some(EntityReviewInstant(completedAt.value))
        )))
      _ <- entity_create_internal(run)
      _ <- _update_diagnosis(diagnosisid, _terminal_update(state, completedAt))
    } yield ()
  }

  /**
   * Settles one successful CNCF job using only the opaque terminal lease
   * issued by the production job gateway.  This deliberately accepts neither
   * an Owner capability nor caller-supplied run/report material.
   */
  def completeFromJob(
    lease: CarReviewProductionTerminalLease.Completed
  ): ExecUowM[CarReviewDiagnosisAdmission.Reused] =
    new ReviewProductionSettlementProgram(core).completeFromJob(lease)

  def recordTerminalFromJob(
    lease: CarReviewProductionTerminalLease.Failed
  ): ExecUowM[Unit] =
    new ReviewProductionSettlementProgram(core).recordTerminalFromJob(lease)

  def recordTerminalFromJob(
    lease: CarReviewProductionTerminalLease.Cancelled
  ): ExecUowM[Unit] =
    new ReviewProductionSettlementProgram(core).recordTerminalFromJob(lease)

  private def _update_diagnosis(
    id: org.simplemodeling.model.datatype.EntityId,
    patch: ReviewDiagnosisUpdate
  ): ExecUowM[Unit] =
    for {
      snapshot <- entity_load_snapshot_internal[ReviewDiagnosisEntity](id)
      _ <- entity_update_internal(id, patch, snapshot.revision)
    } yield ()

  private def _completion_update(
    response: CarReviewCanonicalResponse
  ): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(
      state = Update.set(EntityReviewRunState("completed")),
      completedat = Update.set(EntityReviewInstant(response.report.execution.completedAt.value)),
      reportid = Update.set(EntityReviewReportId(response.report.reportId.value)),
      reportdigest = Update.set(EntityReviewDigest(response.report.reportDigest.value))
    )

  private def _terminal_update(
    state: CarReviewDiagnosisTerminalState,
    completedat: ReviewInstant
  ): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(
      state = Update.set(EntityReviewRunState(state.value)),
      completedat = Update.set(EntityReviewInstant(completedat.value))
    )

  private def _diagnosis_candidate(
    plan: CarReviewExecutionPlan
  ): Consequence[ReviewDiagnosisCreate] =
    ReviewDiagnosisCreate.Builder()
      .withKey_definition_id(EntityReviewReuseKeyDefinition(plan.reuseKey.definitionId))
      .withReuse_key_digest(EntityReviewDigest(plan.reuseKey.digest.value))
      .withState(EntityReviewRunState("claimed"))
      .withTarget_digest(EntityReviewDigest(plan.request.target.digest.value))
      .withProfile(EntityReviewProfile(plan.request.profile.value))
      .withCreated_at(EntityReviewInstant(plan.request.startedAt.value))
      .buildC().map(
      _.copy(
        id = Some(_diagnosis_id(plan)),
        active_review_id = Some(EntityReviewId(plan.request.reviewId.value))
      )
    )

  private final case class CompletionDocuments(
    runDocument: String,
    reportDocument: String,
    attestationDocument: String
  )

  private def _completion_documents(
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): Consequence[CompletionDocuments] =
    _validate_response(plan, response).flatMap { _ =>
      val run = CarReviewRun(
        ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
        ReviewDocumentType(CarReviewRunVocabulary.DOCUMENT_TYPE),
        response.report.reviewId,
        response.report.target,
        response.report.profile,
        ReviewRunState("completed"),
        response.report.execution.providers,
        response.report.limitations,
        response.report.execution.startedAt,
        response.report.execution.completedAt,
        Some(response.report.execution.completedAt),
        Some(response.report.reportId),
        Some(response.report.reportDigest),
        None
      )
      (for {
        rundocument <- CarReviewRunCodec.encode(run).left.map(_.code)
        reportdocument <- CarReviewReportCodec.encode(response.report).left.map(_.code)
        attestationdocument <- CarReviewAttestationCodec.encode(response.attestation).left.map(_.code)
      } yield CompletionDocuments(rundocument, reportdocument, attestationdocument)) match {
        case Right(value) => Consequence.success(value)
        case Left(code) => Consequence.operationInvalid(code)
      }
    }

  private def _validate_response(
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): Consequence[Unit] =
    if response.report.reviewId != plan.request.reviewId ||
        response.report.target != plan.request.target ||
        response.report.profile != plan.request.profile ||
        response.gate != response.report.gate ||
        response.attestation.reviewId != response.report.reviewId ||
        response.attestation.reportId != response.report.reportId ||
        response.attestation.reportDigest != response.report.reportDigest then
      Consequence.operationInvalid("review-diagnosis-completion-binding-invalid")
    else Consequence.unit

  /**
   * A completion/terminal call must read the persisted Aggregate root
   * directly through the internal Entity DSL.  A claim-or-load lookup may
   * legitimately return a resident Entity-space value, which is unsuitable
   * for validating a state transition after a save.  Direct internal load is
   * still inside the UnitOfWork/Entity boundary, but makes the datastore's
   * persisted state authoritative for this transition.
   */
  private def _existing_diagnosis(
    plan: CarReviewExecutionPlan
  ): ExecUowM[ReviewDiagnosisEntity] =
    entity_load_internal[ReviewDiagnosisEntity](_diagnosis_id(plan))

  private def _validate_owner(
    owner: CarReviewDiagnosisAdmission.Owner,
    diagnosis: ReviewDiagnosisEntity,
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): Consequence[Unit] =
    if diagnosis.key_definition_id.value != plan.reuseKey.definitionId ||
        diagnosis.reuse_key_digest.value != plan.reuseKey.digest.value ||
        diagnosis.state.value != "claimed" ||
        !diagnosis.active_review_id.exists(_.value == response.report.reviewId.value) ||
        !owner.isOwnerFor(diagnosis.id.print, plan) then
      Consequence.operationInvalid("review-diagnosis-completion-not-owner")
    else Consequence.unit

  private def _validate_terminal_owner(
    owner: CarReviewDiagnosisAdmission.Owner,
    diagnosis: ReviewDiagnosisEntity,
    plan: CarReviewExecutionPlan
  ): Consequence[Unit] =
    if diagnosis.key_definition_id.value != plan.reuseKey.definitionId ||
        diagnosis.reuse_key_digest.value != plan.reuseKey.digest.value ||
        !diagnosis.active_review_id.exists(_.value == plan.request.reviewId.value) ||
        diagnosis.state.value != "claimed" ||
        !owner.isOwnerFor(diagnosis.id.print, plan) then
      Consequence.operationInvalid("review-diagnosis-terminal-not-owner")
    else Consequence.unit

  private def _target_snapshot(id: org.simplemodeling.model.datatype.EntityId, plan: CarReviewExecutionPlan): Consequence[ReviewTargetSnapshotCreate] =
    ReviewTargetSnapshotCreate.Builder()
      .withDiagnosis_id(id)
      .withTarget_kind(org.simplemodeling.textus.cbdsupport.value.ReviewTargetKind(plan.request.target.kind.value))
      .withComponent_name(EntityComponentName(plan.request.target.name))
      .withTarget_digest(EntityReviewDigest(plan.request.target.digest.value))
      .withCreated_at(EntityReviewInstant(plan.request.startedAt.value))
      .buildC().map(_.copy(
      id = Some(_snapshot_id("target", id, plan.request.target.digest.value, ReviewTargetSnapshotCreate.collectionId)),
      organization = plan.request.target.organization.map(EntityComponentOrganization.apply),
      component_version = plan.request.target.version.map(value => org.simplemodeling.textus.cbdsupport.value.ReviewVersion(value.value))
    ))

  private def _run_snapshot(id: org.simplemodeling.model.datatype.EntityId, plan: CarReviewExecutionPlan, response: CarReviewCanonicalResponse, document: String): Consequence[ReviewRunSnapshotCreate] =
    ReviewRunSnapshotCreate.Builder()
      .withDiagnosis_id(id)
      .withReview_id(EntityReviewId(response.report.reviewId.value))
      .withState(EntityReviewRunState("completed"))
      .withProfile(EntityReviewProfile(response.report.profile.value))
      .withRun_document(ReviewDiagnosisEntityPrograms._json_document(document))
      .withStarted_at(EntityReviewInstant(response.report.execution.startedAt.value))
      .buildC().map(_.copy(
      id = Some(_snapshot_id("run", id, response.report.reviewId.value, ReviewRunSnapshotCreate.collectionId)),
      completed_at = Some(EntityReviewInstant(response.report.execution.completedAt.value))
    ))

  private def _report_snapshot(id: org.simplemodeling.model.datatype.EntityId, response: CarReviewCanonicalResponse, document: String): Consequence[ReviewReportSnapshotCreate] =
    ReviewReportSnapshotCreate.Builder()
      .withDiagnosis_id(id)
      .withReview_id(EntityReviewId(response.report.reviewId.value))
      .withAttestation_id(EntityReviewAttestationId(response.attestation.attestationId.value))
      .withReport_id(EntityReviewReportId(response.report.reportId.value))
      .withReport_digest(EntityReviewDigest(response.report.reportDigest.value))
      .withReport_document(ReviewDiagnosisEntityPrograms._json_document(document))
      .withCreated_at(EntityReviewInstant(response.report.createdAt.value))
      .buildC().map(_.copy(
      id = Some(_snapshot_id("report", id, response.report.reportId.value, ReviewReportSnapshotCreate.collectionId))
    ))

  private def _attestation_snapshot(id: org.simplemodeling.model.datatype.EntityId, response: CarReviewCanonicalResponse, document: String): Consequence[ReviewAttestationSnapshotCreate] =
    ReviewAttestationSnapshotCreate.Builder()
      .withDiagnosis_id(id)
      .withReview_id(EntityReviewId(response.report.reviewId.value))
      .withReport_id(EntityReviewReportId(response.report.reportId.value))
      .withReport_digest(EntityReviewDigest(response.report.reportDigest.value))
      .withAttestation_document(ReviewDiagnosisEntityPrograms._json_document(document))
      .withCreated_at(EntityReviewInstant(response.attestation.createdAt.value))
      .buildC().map(_.copy(
      id = Some(_snapshot_id("attestation", id, response.attestation.attestationId.value, ReviewAttestationSnapshotCreate.collectionId))
    ))

  private def _diagnosis_id(plan: CarReviewExecutionPlan): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${plan.reuseKey.definitionId}:${plan.reuseKey.digest.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection = ReviewDiagnosisEntity.collectionId
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _snapshot_id(kind: String, diagnosis: org.simplemodeling.model.datatype.EntityId, identity: String, collection: org.simplemodeling.model.datatype.EntityCollectionId): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${diagnosis.value}:$kind:$identity"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _admission_from(
    result: org.goldenport.cncf.entity.EntityStore.EntityClaimResult[ReviewDiagnosisCreate, ReviewDiagnosisEntity],
    plan: CarReviewExecutionPlan
  ): Consequence[CarReviewDiagnosisAdmission] = result match {
    case claimed: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Claimed[ReviewDiagnosisCreate] @unchecked =>
      Consequence.success(CarReviewDiagnosisAdmission.Owner.issue(
        claimed.id.print,
        plan.request.reviewId,
        plan.reuseKey.digest
      ))
    case loaded: org.goldenport.cncf.entity.EntityStore.EntityClaimResult.Loaded[ReviewDiagnosisEntity] @unchecked =>
      _admission_from_loaded(loaded.entity, plan)
  }

  private def _admission_from_loaded(
    diagnosis: ReviewDiagnosisEntity,
    plan: CarReviewExecutionPlan
  ): Consequence[CarReviewDiagnosisAdmission] =
    _validate_identity(diagnosis, plan).flatMap { _ =>
      diagnosis.state.value match {
      case "completed" =>
        (diagnosis.active_review_id, diagnosis.report_id, diagnosis.report_digest) match {
          case (Some(reviewid), Some(reportid), Some(reportdigest)) =>
            Consequence.success(CarReviewDiagnosisAdmission.Reused(
              _diagnosis_id(plan).print,
              ReviewId(reviewid.value),
              ReviewReportId(reportid.value),
              ReviewDigest(reportdigest.value)
            ))
          case _ => Consequence.operationInvalid("review-diagnosis-completed-binding-missing")
        }
      case "claimed" | "queued" | "running" | "cancelling" =>
        diagnosis.active_review_id match {
          case Some(reviewid) => Consequence.success(CarReviewDiagnosisAdmission.Joined(_diagnosis_id(plan).print, ReviewId(reviewid.value)))
          case None => Consequence.operationInvalid("review-diagnosis-active-run-missing")
        }
      case _ => Consequence.operationInvalid("review-diagnosis-not-reusable")
      }
    }

  private def _validate_identity(
    diagnosis: ReviewDiagnosisEntity,
    plan: CarReviewExecutionPlan
  ): Consequence[Unit] =
    if (
      diagnosis.key_definition_id.value != plan.reuseKey.definitionId ||
      diagnosis.reuse_key_digest.value != plan.reuseKey.digest.value
    )
      Consequence.operationInvalid("review-diagnosis-identity-conflict")
    else
      Consequence.unit
}

private[cbdsupport] final class ReviewDiagnosisHistoryProgram(
  val core: ActionCall.Core
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  private val _retention_policy = CarReviewRetentionPolicy(30, 20, 20)

  def loadReport(
    reportId: ReviewReportId
  ): ExecUowM[CarReviewReport] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.read-run", CarReviewAuthorization.roles(core.executionContext)))
      // The caller authorization above is intentional.  This is a
      // ServiceInternal exact-record read, so the normal public Entity
      // visibility filter must not reinterpret the generated snapshot's
      // opaque canonical document as an external search predicate.
      snapshots <- entity_search_internal[ReviewReportSnapshotEntity](_query(reportId))
      snapshot <- exec_from(_exact_report_snapshot(reportId, snapshots.data))
      root <- entity_load_internal[ReviewDiagnosisEntity](snapshot.diagnosis_id)
      _ <- exec_from(_validate_report_root(root, snapshot))
      report <- exec_from(_decode_exact(reportId, Vector(snapshot)))
    } yield report

  def expireReport(
    reportId: ReviewReportId,
    effectiveAt: ReviewInstant
  ): ExecUowM[ReviewRetentionEventCreate] =
    for {
      _ <- exec_from(CarReviewAuthorization.authorize("review.retention-expire", CarReviewAuthorization.roles(core.executionContext)))
      _ <- exec_from(_retention_policy.validate match {
        case Right(_) => Consequence.unit
        case Left(error) => Consequence.operationInvalid(error.code)
      })
      report <- _one_report_snapshot(reportId)
      diagnosis <- _one_diagnosis(reportId)
      _ <- exec_from(_validate_expiry_due(report, effectiveAt))
      attestations <- entity_search_internal[ReviewAttestationSnapshotEntity](_attestation_query(reportId))
      event <- exec_from(_retention_event(report, diagnosis, effectiveAt))
      _ <- entity_create_internal(event)
      _ <- _expire_attestations(attestations.data, diagnosis, report, effectiveAt)
      _ <- entity_delete(report.id)
      _ <- _update_diagnosis(diagnosis.id, _expiry_update(effectiveAt))
    } yield event

  private def _update_diagnosis(
    id: org.simplemodeling.model.datatype.EntityId,
    patch: ReviewDiagnosisUpdate
  ): ExecUowM[Unit] =
    for {
      snapshot <- entity_load_snapshot_internal[ReviewDiagnosisEntity](id)
      _ <- entity_update_internal(id, patch, snapshot.revision)
    } yield ()

  private def _one_report_snapshot(reportid: ReviewReportId): ExecUowM[ReviewReportSnapshotEntity] =
    entity_search_internal[ReviewReportSnapshotEntity](_query(reportid)).flatMap { results =>
      exec_from(results.data match {
        case Vector(snapshot) => Consequence.success(snapshot)
        case Vector() => Consequence.operationNotFound(s"review report: ${reportid.value}")
        case _ => Consequence.operationInvalid("review-history-report-id-not-unique")
      })
    }

  private def _exact_report_snapshot(
    reportid: ReviewReportId,
    snapshots: Vector[ReviewReportSnapshotEntity]
  ): Consequence[ReviewReportSnapshotEntity] =
    snapshots match {
      case Vector(snapshot) => Consequence.success(snapshot)
      case Vector() => Consequence.operationNotFound(s"review report: ${reportid.value}")
      case _ => Consequence.operationInvalid("review-history-report-id-not-unique")
    }

  private def _validate_report_root(
    root: ReviewDiagnosisEntity,
    snapshot: ReviewReportSnapshotEntity
  ): Consequence[Unit] =
    if root.id != snapshot.diagnosis_id then
      Consequence.operationInvalid("review-history-report-root-id-mismatch")
    else if root.state.value != "completed" then
      Consequence.operationInvalid("review-history-report-root-not-completed")
    else if !root.active_review_id.exists(_.value == snapshot.review_id.value) then
      Consequence.operationInvalid("review-history-report-root-review-mismatch")
    else if !root.report_id.exists(_.value == snapshot.report_id.value) ||
        !root.report_digest.exists(_.value == snapshot.report_digest.value) then
      Consequence.operationInvalid("review-history-report-root-report-mismatch")
    else
      Consequence.unit

  private def _one_diagnosis(reportid: ReviewReportId): ExecUowM[ReviewDiagnosisEntity] =
    entity_search_internal[ReviewDiagnosisEntity](_diagnosis_query(reportid)).flatMap { results =>
      exec_from(results.data match {
        case Vector(diagnosis) => Consequence.success(diagnosis)
        case Vector() => Consequence.operationNotFound(s"review diagnosis for report: ${reportid.value}")
        case _ => Consequence.operationInvalid("review-history-report-root-not-unique")
      })
    }

  private def _query(reportid: ReviewReportId): EntityQuery[ReviewReportSnapshotEntity] = {
    val condition = ReviewReportSnapshotQuery.create()
      .withReport_id(Condition.is(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(reportid.value)))
    EntityQuery(
      ReviewReportSnapshotEntity.collectionId,
      Query(Query.Plan(condition, limit = Some(2)))
    )
  }

  private def _diagnosis_query(reportid: ReviewReportId): EntityQuery[ReviewDiagnosisEntity] = {
    val condition = ReviewDiagnosisQuery.create()
      .withReport_id(Condition.is(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(reportid.value)))
    EntityQuery(
      ReviewDiagnosisEntity.collectionId,
      Query(Query.Plan(condition, limit = Some(2)))
    )
  }

  private def _attestation_query(reportid: ReviewReportId): EntityQuery[ReviewAttestationSnapshotEntity] = {
    val condition = ReviewAttestationSnapshotQuery.create()
      .withReport_id(Condition.is(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(reportid.value)))
    EntityQuery(
      ReviewAttestationSnapshotEntity.collectionId,
      Query(Query.Plan(condition, limit = Some(2)))
    )
  }

  private def _validate_expiry_due(
    report: ReviewReportSnapshotEntity,
    effectiveat: ReviewInstant
  ): Consequence[Unit] =
    for {
      created <- _instant(report.created_at.value, "review-history-created-at-invalid")
      effective <- _instant(effectiveat.value, "review-retention-effective-at-invalid")
      _ <- if effective.isBefore(created.plusSeconds(_retention_policy.maxAgeDays.toLong * 24L * 60L * 60L)) then
        Consequence.operationInvalid("review-retention-not-due")
      else
        Consequence.unit
    } yield ()

  private def _instant(value: String, code: String): Consequence[Instant] =
    scala.util.Try(Instant.parse(value)).toEither match {
      case Right(instant) => Consequence.success(instant)
      case Left(_) => Consequence.operationInvalid(code)
    }

  private def _retention_event(
    report: ReviewReportSnapshotEntity,
    diagnosis: ReviewDiagnosisEntity,
    effectiveat: ReviewInstant
  ): Consequence[ReviewRetentionEventCreate] =
    ReviewRetentionEventCreate.Builder()
      .withDiagnosis_id(diagnosis.id)
      .withAction(EntityReviewRetentionAction("expired"))
      .withRecord_type(EntityReviewRetentionRecordType("report"))
      .withRecord_id(report.id)
      .withRecord_digest(EntityReviewDigest(report.report_digest.value))
      .withTarget_digest(EntityReviewDigest(diagnosis.target_digest.value))
      .withEffective_at(EntityReviewInstant(effectiveat.value))
      .buildC().map(_.copy(
      id = Some(_retention_event_id("retention-expired-report", diagnosis.id, report.id.value, effectiveat)),
      report_id = Some(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(report.report_id.value)),
      report_digest = Some(EntityReviewDigest(report.report_digest.value))
    ))

  private def _expire_attestations(
    attestations: Vector[ReviewAttestationSnapshotEntity],
    diagnosis: ReviewDiagnosisEntity,
    report: ReviewReportSnapshotEntity,
    effectiveat: ReviewInstant
  ): ExecUowM[Unit] =
    attestations.foldLeft(exec_from(Consequence.unit)) { (z, attestation) =>
      for {
        _ <- z
        event <- exec_from(ReviewRetentionEventCreate.Builder()
          .withDiagnosis_id(diagnosis.id)
          .withAction(EntityReviewRetentionAction("expired"))
          .withRecord_type(EntityReviewRetentionRecordType("attestation"))
          .withRecord_id(attestation.id)
          .withRecord_digest(EntityReviewDigest(attestation.report_digest.value))
          .withTarget_digest(EntityReviewDigest(diagnosis.target_digest.value))
          .withEffective_at(EntityReviewInstant(effectiveat.value))
          .buildC().map(_.copy(
          id = Some(_retention_event_id("retention-expired-attestation", diagnosis.id, attestation.id.value, effectiveat)),
          report_id = Some(org.simplemodeling.textus.cbdsupport.value.ReviewReportId(report.report_id.value)),
          report_digest = Some(EntityReviewDigest(report.report_digest.value))
        )))
        _ <- entity_create_internal(event)
        _ <- entity_delete(_history_snapshot_id("attestation", diagnosis.id, report.attestation_id.value, ReviewAttestationSnapshotCreate.collectionId))
      } yield ()
    }

  private def _retention_event_id(
    kind: String,
    diagnosis: org.simplemodeling.model.datatype.EntityId,
    recordid: String,
    effectiveat: ReviewInstant
  ): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${diagnosis.value}:$kind:$recordid:${effectiveat.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection = ReviewRetentionEventCreate.collectionId
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _history_snapshot_id(
    kind: String,
    diagnosis: org.simplemodeling.model.datatype.EntityId,
    identity: String,
    collection: org.simplemodeling.model.datatype.EntityCollectionId
  ): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${diagnosis.value}:$kind:$identity"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId(
      collection.major,
      collection.minor,
      collection,
      timestamp = Some(UniversalId.StableTimestamp),
      entropy = Some(key)
    )
  }

  private def _expiry_update(effectiveat: ReviewInstant): ReviewDiagnosisUpdate =
    ReviewDiagnosisEntityPrograms._diagnosis_update(
      state = Update.set(EntityReviewRunState("expired")),
      completedat = Update.set(EntityReviewInstant(effectiveat.value)),
      reportid = Update.setNull,
      reportdigest = Update.setNull
    )

  private def _decode_exact(
    reportid: ReviewReportId,
    snapshots: Vector[ReviewReportSnapshotEntity]
  ): Consequence[CarReviewReport] =
    snapshots match {
      case Vector(snapshot) =>
        CarReviewReportCodec.decode(snapshot.report_document.value.value) match {
          case Left(error) => Consequence.operationInvalid(error.code)
          case Right(report) =>
          if report.reportId != reportid || report.reportDigest.value != snapshot.report_digest.value || report.reviewId.value != snapshot.review_id.value then
            Consequence.operationInvalid("review-history-snapshot-attribution-invalid")
          else Consequence.success(report)
        }
      case Vector() => Consequence.operationNotFound(s"review report: ${reportid.value}")
      case _ => Consequence.operationInvalid("review-history-report-id-not-unique")
    }
}

private[cbdsupport] object ReviewDiagnosisEntityPrograms {
  private[impl] def _diagnosis_update(
    keydefinitionid: Update[EntityReviewReuseKeyDefinition] = Update.noop,
    reusekeydigest: Update[EntityReviewDigest] = Update.noop,
    state: Update[EntityReviewRunState] = Update.noop,
    activereviewid: Update[EntityReviewId] = Update.noop,
    targetdigest: Update[EntityReviewDigest] = Update.noop,
    profile: Update[EntityReviewProfile] = Update.noop,
    createdat: Update[EntityReviewInstant] = Update.noop,
    completedat: Update[EntityReviewInstant] = Update.noop,
    reportid: Update[EntityReviewReportId] = Update.noop,
    reportdigest: Update[EntityReviewDigest] = Update.noop
  ): ReviewDiagnosisUpdate =
    ReviewDiagnosisUpdate(
      id = Update.noop,
      nameAttributes = NameAttributesUpdate(),
      descriptiveAttributes = DescriptiveAttributesUpdate(),
      contentAttributes = ContentAttributesUpdate(),
      lifecycleAttributes = LifecycleAttributesUpdate(),
      publicationAttributes = PublicationAttributesUpdate(),
      securityAttributes = SecurityAttributesUpdate(),
      resourceAttributes = ResourceAttributesUpdate(),
      auditAttributes = AuditAttributesUpdate(),
      mediaAttributes = MediaAttributesUpdate(),
      contextualAttribute = ContextualAttributesUpdate(),
      key_definition_id = keydefinitionid,
      reuse_key_digest = reusekeydigest,
      state = state,
      active_review_id = activereviewid,
      target_digest = targetdigest,
      profile = profile,
      created_at = createdat,
      completed_at = completedat,
      report_id = reportid,
      report_digest = reportdigest
    )

  private[impl] def _json_document(document: String): EntityReviewSubmissionDocument =
    EntityReviewSubmissionDocument(org.simplemodeling.model.value.ContentBody(document))
}
