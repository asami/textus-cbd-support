package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import org.goldenport.Consequence
import ModelEditSession.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Shared candidate composition over existing host-owned projections, with no separate editing state. */
object ModelEditCrossView {
  final case class ReviewState(review: RetainedReview, applies: Boolean)
  final case class View(session: InternalModelRecordReference, candidate: InternalModelRecordReference,
    scope: Scope, monoKoto: MonoKotoProjection, structure: StructureViewProjection,
    editing: ModelEditViewProjection.View, audit: Vector[AppliedOperation], reviews: Vector[ReviewState])
  final case class Applied(session: ModelEditSession, operation: Operation, view: View)

  def project(session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    monoKoto: MonoKotoProjection, structure: StructureViewProjection,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[String, View] = for {
    _ <- Either.cond(monoKoto.context == session.scope.context && monoKoto.component == session.scope.component &&
      structure.context == session.scope.context && structure.component == session.scope.component, (),
      "cross-view projections must share the exact editing scope")
    mono <- MonoKotoProjection.create(monoKoto.context, monoKoto.component, monoKoto.subjects.map(_.sourceSubject)).left.map(_.violations.mkString("; "))
    engineering <- StructureViewProjection.create(structure.context, structure.component,
      structure.relations.map(_.sourceRelation), structure.gaps.map(_.sourceGap)).left.map(_.violations.mkString("; "))
    _ <- Either.cond(mono == monoKoto && engineering == structure, (), "cross-view DTOs must retain their existing admitted projection values")
    editing <- ModelEditViewProjection.project(session, source, capabilityDemands)
  } yield View(session.reference, session.candidate.reference, session.scope, mono, engineering, editing,
    session.history, session.reviews.map(review => ReviewState(review, ModelEditSession.reviewApplies(session, review))))

  def applyOperation(session: ModelEditSession, view: View, surface: ModelEditPalette.Surface, subject: Subject,
    operation: Operation, successorCandidate: InternalModelRecordReference, authorization: Authorization,
    resolutions: Vector[ResolutionEvidence], source: ModelEditSourceProjection.Selection,
    capabilityDemands: Map[ComponentDashboardSemanticTargetIdentity, ModelEditReview.CapabilityDemand]): Either[ModelEditModelMutation.Failure, Applied] = for {
    _ <- Either.cond(view.session == session.reference && view.candidate == session.candidate.reference && view.scope == session.scope,
      (), ModelEditModelMutation.Failure.Palette(ModelEditPalette.Failure.ProjectionMismatch))
    context <- (surface match {
      case ModelEditPalette.Surface.MonoKoto => ModelEditPalette.monoKotoContext(session, view.monoKoto, subject)
      case ModelEditPalette.Surface.Structure => ModelEditPalette.structureContext(session, view.structure, subject)
      case _ => Left(ModelEditPalette.Failure.ProjectionMismatch)
    }).left.map(ModelEditModelMutation.Failure.Palette(_))
    applied <- ModelEditModelMutation.applyOperation(session, context, operation, successorCandidate, authorization, resolutions, source, capabilityDemands)
    projected <- project(applied.session, source, view.monoKoto, view.structure, capabilityDemands).left.map(ModelEditModelMutation.Failure.Projection(_))
  } yield Applied(applied.session, applied.operation, projected)

  /** Recorded historical handoff and current canonical inspection retain separate lifecycle purposes. */
  private[runtime] final case class PromotionRead(historical: InternalModelContinuationReport,
    canonicalpackage: InternalModelVerifiedPackageContext, canonical: InternalModelProjectionContinuity,
    canonicalhasresume: Boolean)

  private[runtime] def readPromotion(historicalRoot: Path, canonicalRoot: Path,
    request: InternalModelContinuationRequest): Consequence[PromotionRead] = for {
    historical <- InternalModelContinuationActionGate.evaluate(historicalRoot, request)
    current <- InternalModelPackageValidator.verifiedContinuation(canonicalRoot)
    canonical <- InternalModelProjectionContinuityValidator.validateVerified(current.continuityPackage)
    _ <- if (historical.state.packageContext.reference == current.packageContext.reference &&
      historical.state.continuity.realization.scope == canonical.realization.scope) Consequence.unit
      else Consequence.operationInvalid("historical editing package and current canonical projection must name the same package and semantic scope")
  } yield PromotionRead(historical, current.packageContext, canonical,
    current.artifacts.exists(_.context.reference.role == InternalModelArtifactRole.Resume))
}
