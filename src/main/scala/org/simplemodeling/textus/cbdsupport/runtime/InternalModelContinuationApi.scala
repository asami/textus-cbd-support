package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant
import scala.util.control.NonFatal
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.context.{ExecutionContext, SessionContext, SubjectKind}

/**
 * Trusted server binding and closed runtime-internal requests; retained history grants no action.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 */
private[runtime] final case class InternalModelHistoryInput(
  storageId: InternalModelHistoryStorageId,
  reference: InternalModelRecordReference,
  payload: InternalModelRetainedHistoryPayload,
  evidence: Vector[InternalModelEvidenceInput]
)

private[runtime] final class InternalModelContinuationApi private (
  private val _core: ActionCall.Core,
  private val _capture: InternalModelVerifiedContinuationPackage,
  private val _subject: InternalModelReviewSubject,
  private val _principal_id: String,
  private val _redaction_references: Set[InternalModelRecordReference],
  private val _store: Option[InternalModelRetainedHistoryStore]
) {
  import InternalModelContinuationApi.*

  def read(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = for {
    _ <- _authorize("read")
    admitted <- _selection(selection)
    result <- _store.fold[Consequence[InternalModelHistoryResult]](Consequence.success(InternalModelHistoryResult.Unavailable))(_.read(admitted))
  } yield result

  def propose(input: InternalModelHistoryInput): Consequence[InternalModelHistoryResult] =
    _produce("propose", Set(InternalModelRetainedHistoryKind.Proposal), input, None)

  def review(input: InternalModelHistoryInput): Consequence[InternalModelHistoryResult] =
    _produce("review", Set(InternalModelRetainedHistoryKind.Review), input, None)

  def record(input: InternalModelHistoryInput,
    endpoints: Option[InternalModelHistorySupersessionEndpoints] = None): Consequence[InternalModelHistoryResult] =
    _produce("record", Set(InternalModelRetainedHistoryKind.Alternative, InternalModelRetainedHistoryKind.Supersession,
      InternalModelRetainedHistoryKind.Evidence), input, endpoints)

  def resume(request: InternalModelContinuationRequest): Consequence[InternalModelContinuationReport] = for {
    _ <- _authorize("resume")
    report <- InternalModelContinuationActionGate.evaluateVerified(_capture, request)
  } yield report

  def expire(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = for {
    _ <- _authorize("expire")
    admitted <- _selection(selection)
    result <- _store.fold[Consequence[InternalModelHistoryResult]](Consequence.success(InternalModelHistoryResult.Unavailable))(_.expire(admitted))
  } yield result

  def delete(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = for {
    _ <- _authorize("delete")
    admitted <- _selection(selection)
    result <- _store.fold[Consequence[InternalModelHistoryResult]](Consequence.success(InternalModelHistoryResult.Unavailable))(_.delete(admitted))
  } yield result

  private def _produce(operation: String, kinds: Set[InternalModelRetainedHistoryKind],
    input: InternalModelHistoryInput, endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[InternalModelHistoryResult] = for {
    authenticated <- _authorize(operation)
    _ <- _check(input != null && input.payload != null && input.evidence != null && input.evidence.size <= 64, "input")
    _ <- _check(kinds.contains(input.payload.kind), "payload-kind")
    evidence <- _retain_evidence(input.evidence)
    selection <- _selection(InternalModelHistorySelection(input.storageId, input.reference, _subject.packageReference, _subject.scope))
    _ <- _endpoints(input.payload.kind, endpoints)
    record = InternalModelRetainedHistoryRecord(input.reference, _subject, authenticated.actor, authenticated.occurredat,
      input.payload, evidence)
    document <- InternalModelRetainedHistoryCodec.validate(
      InternalModelHistoryDocument(selection, InternalModelHistoryDocumentState.Retained(record, authenticated.occurredat)))
    // The output byte bound applies even when persistence is deliberately disabled.
    _ <- InternalModelRetainedHistoryCodec.encode(document)
    result <- _store.fold[Consequence[InternalModelHistoryResult]](Consequence.success(InternalModelHistoryResult.Unavailable))(_.record(document, endpoints))
  } yield result

  private def _retain_evidence(inputs: Vector[InternalModelEvidenceInput]): Consequence[Vector[InternalModelRetainedEvidence]] = {
    Consequence.zipN(inputs.map { input =>
      InternalModelEvidencePolicy.retain(input).flatMap { retained =>
        val admitted = input.payload match {
          case InternalModelEvidenceInputPayload.RedactedText(_, reference) if input.kind == InternalModelEvidenceKind.Narrative =>
            _redaction_references.contains(reference)
          case _ => true
        }
        _check(admitted, "redaction-reference").map(_ => retained)
      }
    }).map(_.toVector)
  }

  private def _endpoints(kind: InternalModelRetainedHistoryKind,
    endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[Unit] = {
    if (endpoints == null || endpoints.exists(_ == null)) _invalid("supersession-endpoints")
    else (kind, endpoints) match {
      case (InternalModelRetainedHistoryKind.Supersession, Some(pair)) => _selection(pair.previous).zip(_selection(pair.successor)).map(_ => ())
      case (InternalModelRetainedHistoryKind.Supersession, None) => _invalid("supersession-endpoints")
      case (_, Some(_)) => _invalid("unexpected-supersession-endpoints")
      case (_, None) => Consequence.unit
    }
  }

  private def _selection(selection: InternalModelHistorySelection): Consequence[InternalModelHistorySelection] =
    InternalModelRetainedHistoryCodec.validateSelection(selection).flatMap { admitted =>
      _check(admitted.packageReference == _subject.packageReference && admitted.scope == _subject.scope,
        "bound-package-scope").map(_ => admitted)
    }

  private def _authorize(operation: String): Consequence[Authentication] = {
    try {
      val context = if (_core == null) null else _core.executionContext
      if (!_safe_context(context)) _denied(operation, "security-context")
      else {
        val security = context.security
        val now = context.clock.instant()
        val kind = security.subjectKind match {
          case SubjectKind.User => Some("user")
          case SubjectKind.Service => Some("service")
          case SubjectKind.Subsystem => Some("subsystem")
          case _ => None
        }
        if (kind.isEmpty || security.principal.id.value != _principal_id || !_valid_session(security.session, now))
          _denied(operation, "principal-session")
        else {
          val allowed = _roles(operation)
          val roles = CarReviewAuthorization.roles(context)
          _role_priority.find(role => allowed.contains(role) && roles.contains(role)) match {
            case Some(role) => Consequence.success(Authentication(
              InternalModelDecisionActor(kind.get, security.principal.id.value, role), now))
            case None => _denied(operation, "role")
          }
        }
      }
    } catch { case NonFatal(_) => _denied(operation, "security-context") }
  }
}

private[runtime] object InternalModelContinuationApi {
  private final case class Authentication(actor: InternalModelDecisionActor, occurredat: Instant)
  private val _role_priority = Vector("admin", "operator", "reviewer", "viewer")

  def create(core: ActionCall.Core, capture: InternalModelVerifiedContinuationPackage,
    subject: InternalModelReviewSubject, principalId: String,
    admittedRedactionReferences: Set[InternalModelRecordReference],
    store: Option[InternalModelRetainedHistoryStore]): Consequence[InternalModelContinuationApi] = {
    try {
      for {
        _ <- _check(core != null && _nonblank(principalId) && store != null && !store.exists(_ == null) &&
          admittedRedactionReferences != null, "server-binding")
        state <- InternalModelRehydrationValidator.validateVerified(capture)
          .recoverWith(_ => _invalid("captured-package"))
        _ <- _validate_subject(subject)
        _ <- _check(subject.packageReference == state.packageContext.reference &&
          subject.scope == state.continuity.realization.scope &&
          subject.artifacts.forall(reference => state.artifacts.exists(artifact => artifact.context.reference == reference &&
            artifact.context.present && artifact.bytes.nonEmpty)), "subject-capture-selection")
        _ <- Consequence.zipN(admittedRedactionReferences.toVector.map(_validate_reference)).map(_ => ())
      } yield new InternalModelContinuationApi(core, capture, subject, principalId, admittedRedactionReferences, store)
    } catch { case NonFatal(_) => _invalid("server-binding") }
  }

  private def _validate_subject(subject: InternalModelReviewSubject): Consequence[Unit] = {
    val reference = InternalModelRecordReference(InternalModelRecordId.from("server-subject-admission").toOption.get,
      InternalModelRecordRevision.from(1L).toOption.get)
    InternalModelRetainedHistoryValidator.validate(InternalModelRetainedHistoryRecord(reference, subject,
      InternalModelDecisionActor("service", "server-subject-admission", "viewer"), Instant.EPOCH,
      InternalModelRetainedHistoryPayload.Evidence(reference), Vector.empty)).map(_ => ())
  }

  private def _validate_reference(reference: InternalModelRecordReference): Consequence[Unit] = {
    if (reference == null || !InternalModelRecordId.from(reference.recordId.value).isRight ||
      !InternalModelRecordRevision.from(reference.recordRevision.value).isRight) _invalid("redaction-reference")
    else Consequence.unit
  }

  private def _safe_context(context: ExecutionContext): Boolean = {
    if (context == null || context.clock == null || context.security == null) false
    else {
      val security = context.security
      security.principal != null && security.principal.id != null && _nonblank(security.principal.id.value) &&
        _safe_attributes(security.principal.attributes) && security.capabilities != null &&
        security.capabilities.forall(capability => capability != null && _nonblank(capability.name)) &&
        security.level != null && _nonblank(security.level.value) && security.subjectKind != null && security.session != null
    }
  }

  private def _valid_session(session: Option[SessionContext], now: Instant): Boolean = session.forall { value =>
    value != null && Vector(value.sessionId, value.tokenId, value.tokenKind, value.issuer, value.audience,
      value.refreshSessionId).forall(option => option != null && option.forall(_nonblank)) &&
      value.authenticatedAt != null && value.authenticatedAt.forall(time => time != null && !time.isAfter(now)) &&
      value.expiresAt != null && value.expiresAt.forall(time => time != null && now.isBefore(time)) &&
      _safe_attributes(value.attributes)
  }

  private def _safe_attributes(attributes: Map[String, String]): Boolean =
    attributes != null && attributes.forall { case (key, value) => _nonblank(key) && value != null }

  private def _roles(operation: String): Set[String] = operation match {
    case "read" | "resume" => Set("viewer", "reviewer", "operator", "admin")
    case "propose" | "review" | "record" => Set("reviewer", "operator", "admin")
    case "expire" | "delete" => Set("operator", "admin")
    case _ => Set.empty
  }
  private def _nonblank(value: String): Boolean = value != null && !value.isBlank
  private def _check(valid: Boolean, dimension: String): Consequence[Unit] = if (valid) Consequence.unit else _invalid(dimension)
  private def _invalid[A](dimension: String): Consequence[A] = Consequence.operationInvalid("internal-model API invalid: " + dimension)
  private def _denied[A](operation: String, dimension: String): Consequence[A] =
    Consequence.securityPermissionDenied("internal-model API denied: " + operation + "/" + dimension)
}
