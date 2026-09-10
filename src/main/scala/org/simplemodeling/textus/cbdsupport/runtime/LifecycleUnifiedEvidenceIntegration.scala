package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait LifecycleUnifiedEvidenceRelationshipFamily

case object LifecycleUnifiedEvidenceComposition extends LifecycleUnifiedEvidenceRelationshipFamily

case object LifecycleUnifiedEvidenceAggregation extends LifecycleUnifiedEvidenceRelationshipFamily

case object LifecycleUnifiedEvidenceAssociation extends LifecycleUnifiedEvidenceRelationshipFamily

object LifecycleUnifiedEvidenceRelationshipFamily {
  val presentationSequence: Vector[LifecycleUnifiedEvidenceRelationshipFamily] = Vector(
    LifecycleUnifiedEvidenceComposition,
    LifecycleUnifiedEvidenceAggregation,
    LifecycleUnifiedEvidenceAssociation
  )
}

final case class LifecycleUnifiedEvidenceDeclaredRelationship(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  family: LifecycleUnifiedEvidenceRelationshipFamily,
  subject: ComponentDashboardSemanticTargetIdentity,
  objectTarget: ComponentDashboardSemanticTargetIdentity,
  sourceOwner: String,
  sourceLocator: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class LifecycleUnifiedEvidenceRuntimeEvidence(
  id: String,
  declaredRelationshipId: String,
  observedValue: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

sealed trait LifecycleUnifiedEvidenceEvidenceAssociation

final case class LifecycleUnifiedEvidenceDeclaredRelationshipAssociation(
  declaredRelationshipId: String
) extends LifecycleUnifiedEvidenceEvidenceAssociation

case object LifecycleUnifiedEvidenceComponentContextAssociation extends LifecycleUnifiedEvidenceEvidenceAssociation

final case class LifecycleUnifiedEvidenceUsageEvidence(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  association: LifecycleUnifiedEvidenceEvidenceAssociation,
  representation: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class LifecycleUnifiedEvidenceOperationEvidence(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  association: LifecycleUnifiedEvidenceEvidenceAssociation,
  representation: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class LifecycleUnifiedEvidenceQualityReviewEvidence(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  association: LifecycleUnifiedEvidenceEvidenceAssociation,
  representation: String,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class LifecycleUnifiedEvidenceEmptyLaneCondition(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class LifecycleUnifiedEvidenceRuntimeLane(
  evidence: Vector[LifecycleUnifiedEvidenceRuntimeEvidence],
  emptyCondition: Option[LifecycleUnifiedEvidenceEmptyLaneCondition]
)

final case class LifecycleUnifiedEvidenceUsageLane(
  evidence: Vector[LifecycleUnifiedEvidenceUsageEvidence],
  emptyCondition: Option[LifecycleUnifiedEvidenceEmptyLaneCondition]
)

final case class LifecycleUnifiedEvidenceOperationLane(
  evidence: Vector[LifecycleUnifiedEvidenceOperationEvidence],
  emptyCondition: Option[LifecycleUnifiedEvidenceEmptyLaneCondition]
)

final case class LifecycleUnifiedEvidenceQualityReviewLane(
  evidence: Vector[LifecycleUnifiedEvidenceQualityReviewEvidence],
  emptyCondition: Option[LifecycleUnifiedEvidenceEmptyLaneCondition]
)

final case class LifecycleUnifiedEvidenceIntegrationFailure(violations: Vector[String])

final case class LifecycleUnifiedEvidenceIntegration(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  declaredRelationships: Vector[LifecycleUnifiedEvidenceDeclaredRelationship],
  runtimeLane: LifecycleUnifiedEvidenceRuntimeLane,
  usageLane: LifecycleUnifiedEvidenceUsageLane,
  operationLane: LifecycleUnifiedEvidenceOperationLane,
  qualityReviewLane: LifecycleUnifiedEvidenceQualityReviewLane
)

sealed trait LifecycleUnifiedEvidenceIntegrationResult

final case class LifecycleUnifiedEvidenceProjectedIntegration(
  sourceIntegration: LifecycleUnifiedEvidenceIntegration
) extends LifecycleUnifiedEvidenceIntegrationResult

final case class LifecycleUnifiedEvidenceRejectedIntegration(
  failure: LifecycleUnifiedEvidenceIntegrationFailure
) extends LifecycleUnifiedEvidenceIntegrationResult

object LifecycleUnifiedEvidenceIntegration {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    declaredRelationships: Vector[LifecycleUnifiedEvidenceDeclaredRelationship],
    runtimeLane: LifecycleUnifiedEvidenceRuntimeLane,
    usageLane: LifecycleUnifiedEvidenceUsageLane,
    operationLane: LifecycleUnifiedEvidenceOperationLane,
    qualityReviewLane: LifecycleUnifiedEvidenceQualityReviewLane
  ): LifecycleUnifiedEvidenceIntegrationResult = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_declared_relationships(declaredRelationships, context, component, violations)

    val declaredrelationshipids = declaredRelationships.map(_.id).toSet
    _validate_runtime_lane(runtimeLane, context, component, declaredrelationshipids, violations)
    _validate_usage_lane(usageLane, context, component, declaredrelationshipids, violations)
    _validate_operation_lane(operationLane, context, component, declaredrelationshipids, violations)
    _validate_quality_review_lane(qualityReviewLane, context, component, declaredrelationshipids, violations)

    val result = violations.result()
    if (result.nonEmpty)
      LifecycleUnifiedEvidenceRejectedIntegration(LifecycleUnifiedEvidenceIntegrationFailure(result))
    else
      LifecycleUnifiedEvidenceProjectedIntegration(
        LifecycleUnifiedEvidenceIntegration(
          context,
          component,
          _order_declared_relationships(declaredRelationships),
          LifecycleUnifiedEvidenceRuntimeLane(
            _order_runtime_evidence(runtimeLane.evidence),
            runtimeLane.emptyCondition
          ),
          LifecycleUnifiedEvidenceUsageLane(
            _order_usage_evidence(usageLane.evidence),
            usageLane.emptyCondition
          ),
          LifecycleUnifiedEvidenceOperationLane(
            _order_operation_evidence(operationLane.evidence),
            operationLane.emptyCondition
          ),
          LifecycleUnifiedEvidenceQualityReviewLane(
            _order_quality_review_evidence(qualityReviewLane.evidence),
            qualityReviewLane.emptyCondition
          )
        )
      )
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Lifecycle Unified Evidence Integration bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Lifecycle Unified Evidence Integration Component identity", violations)

  private def _validate_declared_relationships(
    relationships: Vector[LifecycleUnifiedEvidenceDeclaredRelationship],
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    if (relationships.isEmpty)
      violations += "Lifecycle Unified Evidence Integration requires one or more declared relationships."
    _validate_duplicate_ids(relationships.map(_.id), "declared relationship", violations)
    relationships.foreach(_validate_declared_relationship(_, integrationcontext, integrationcomponent, violations))
  }

  private def _validate_declared_relationship(
    relationship: LifecycleUnifiedEvidenceDeclaredRelationship,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(relationship.id, "Declared relationship identity", violations)
    _validate_scope(
      relationship.context,
      relationship.component,
      integrationcontext,
      integrationcomponent,
      s"Declared relationship '${relationship.id}'",
      violations
    )
    if (!LifecycleUnifiedEvidenceRelationshipFamily.presentationSequence.contains(relationship.family))
      violations += s"Declared relationship '${relationship.id}' must use an exact admitted relationship family."
    _require_nonblank(relationship.subject.value, s"Declared relationship '${relationship.id}' subject identity", violations)
    _require_nonblank(relationship.objectTarget.value, s"Declared relationship '${relationship.id}' object identity", violations)
    _require_nonblank(relationship.sourceOwner, s"Declared relationship '${relationship.id}' source owner", violations)
    _require_nonblank(relationship.sourceLocator, s"Declared relationship '${relationship.id}' source locator", violations)
    _validate_attribution(relationship.attribution, s"Declared relationship '${relationship.id}'", violations)
    _validate_condition(relationship.condition, s"Declared relationship '${relationship.id}'", violations)
    _validate_limitations(relationship.limitations, s"Declared relationship '${relationship.id}'", violations)
    _validate_stable_tie_key(relationship.stableTieKey, s"Declared relationship '${relationship.id}'", violations)
  }

  private def _validate_runtime_lane(
    lane: LifecycleUnifiedEvidenceRuntimeLane,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_lane_occupancy("Runtime lifecycle evidence", lane.evidence.nonEmpty, lane.emptyCondition.nonEmpty, violations)
    _validate_duplicate_ids(lane.evidence.map(_.id), "runtime lifecycle evidence", violations)
    lane.evidence.foreach(_validate_runtime_evidence(_, integrationcontext, integrationcomponent, declaredrelationshipids, violations))
    lane.emptyCondition.foreach(_validate_empty_lane_condition(_, "Runtime lifecycle evidence", integrationcontext, integrationcomponent, violations))
  }

  private def _validate_runtime_evidence(
    evidence: LifecycleUnifiedEvidenceRuntimeEvidence,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(evidence.id, "Runtime lifecycle evidence identity", violations)
    _validate_scope(
      evidence.context,
      evidence.component,
      integrationcontext,
      integrationcomponent,
      s"Runtime lifecycle evidence '${evidence.id}'",
      violations
    )
    _require_nonblank(evidence.declaredRelationshipId, s"Runtime lifecycle evidence '${evidence.id}' declared relationship identity", violations)
    if (!declaredrelationshipids.contains(evidence.declaredRelationshipId))
      violations += s"Runtime lifecycle evidence '${evidence.id}' must reference an admitted declared relationship."
    _require_nonblank(evidence.observedValue, s"Runtime lifecycle evidence '${evidence.id}' observed value", violations)
    _validate_attribution(evidence.attribution, s"Runtime lifecycle evidence '${evidence.id}'", violations)
    _validate_condition(evidence.condition, s"Runtime lifecycle evidence '${evidence.id}'", violations)
    _validate_limitations(evidence.limitations, s"Runtime lifecycle evidence '${evidence.id}'", violations)
    _validate_stable_tie_key(evidence.stableTieKey, s"Runtime lifecycle evidence '${evidence.id}'", violations)
  }

  private def _validate_usage_lane(
    lane: LifecycleUnifiedEvidenceUsageLane,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_lane_occupancy("Usage/Discovery evidence", lane.evidence.nonEmpty, lane.emptyCondition.nonEmpty, violations)
    _validate_duplicate_ids(lane.evidence.map(_.id), "Usage/Discovery evidence", violations)
    lane.evidence.foreach(_validate_usage_evidence(_, integrationcontext, integrationcomponent, declaredrelationshipids, violations))
    lane.emptyCondition.foreach(_validate_empty_lane_condition(_, "Usage/Discovery evidence", integrationcontext, integrationcomponent, violations))
  }

  private def _validate_usage_evidence(
    evidence: LifecycleUnifiedEvidenceUsageEvidence,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_associated_evidence(
      evidence.id,
      evidence.context,
      evidence.component,
      evidence.association,
      evidence.representation,
      evidence.attribution,
      evidence.condition,
      evidence.limitations,
      evidence.stableTieKey,
      "Usage/Discovery evidence",
      integrationcontext,
      integrationcomponent,
      declaredrelationshipids,
      violations
    )
  }

  private def _validate_operation_lane(
    lane: LifecycleUnifiedEvidenceOperationLane,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_lane_occupancy("Operation evidence", lane.evidence.nonEmpty, lane.emptyCondition.nonEmpty, violations)
    _validate_duplicate_ids(lane.evidence.map(_.id), "Operation evidence", violations)
    lane.evidence.foreach(_validate_operation_evidence(_, integrationcontext, integrationcomponent, declaredrelationshipids, violations))
    lane.emptyCondition.foreach(_validate_empty_lane_condition(_, "Operation evidence", integrationcontext, integrationcomponent, violations))
  }

  private def _validate_operation_evidence(
    evidence: LifecycleUnifiedEvidenceOperationEvidence,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_associated_evidence(
      evidence.id,
      evidence.context,
      evidence.component,
      evidence.association,
      evidence.representation,
      evidence.attribution,
      evidence.condition,
      evidence.limitations,
      evidence.stableTieKey,
      "Operation evidence",
      integrationcontext,
      integrationcomponent,
      declaredrelationshipids,
      violations
    )
  }

  private def _validate_quality_review_lane(
    lane: LifecycleUnifiedEvidenceQualityReviewLane,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_lane_occupancy("Quality/Review evidence", lane.evidence.nonEmpty, lane.emptyCondition.nonEmpty, violations)
    _validate_duplicate_ids(lane.evidence.map(_.id), "Quality/Review evidence", violations)
    lane.evidence.foreach(_validate_quality_review_evidence(_, integrationcontext, integrationcomponent, declaredrelationshipids, violations))
    lane.emptyCondition.foreach(_validate_empty_lane_condition(_, "Quality/Review evidence", integrationcontext, integrationcomponent, violations))
  }

  private def _validate_quality_review_evidence(
    evidence: LifecycleUnifiedEvidenceQualityReviewEvidence,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_associated_evidence(
      evidence.id,
      evidence.context,
      evidence.component,
      evidence.association,
      evidence.representation,
      evidence.attribution,
      evidence.condition,
      evidence.limitations,
      evidence.stableTieKey,
      "Quality/Review evidence",
      integrationcontext,
      integrationcomponent,
      declaredrelationshipids,
      violations
    )
  }

  private def _validate_associated_evidence(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    association: LifecycleUnifiedEvidenceEvidenceAssociation,
    representation: String,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    limitations: Vector[String],
    stabletiekey: Option[String],
    lanename: String,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    declaredrelationshipids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(id, s"$lanename identity", violations)
    _validate_scope(context, component, integrationcontext, integrationcomponent, s"$lanename '$id'", violations)
    _validate_association(association, declaredrelationshipids, s"$lanename '$id'", violations)
    _require_nonblank(representation, s"$lanename '$id' representation", violations)
    _validate_attribution(attribution, s"$lanename '$id'", violations)
    _validate_condition(condition, s"$lanename '$id'", violations)
    _validate_limitations(limitations, s"$lanename '$id'", violations)
    _validate_stable_tie_key(stabletiekey, s"$lanename '$id'", violations)
  }

  private def _validate_association(
    association: LifecycleUnifiedEvidenceEvidenceAssociation,
    declaredrelationshipids: Set[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    association match {
      case LifecycleUnifiedEvidenceDeclaredRelationshipAssociation(declaredrelationshipid) =>
        _require_nonblank(declaredrelationshipid, s"$subject declared relationship association", violations)
        if (!declaredrelationshipids.contains(declaredrelationshipid))
          violations += s"$subject explicit relationship association must reference an admitted declared relationship."
      case LifecycleUnifiedEvidenceComponentContextAssociation =>
    }

  private def _validate_empty_lane_condition(
    condition: LifecycleUnifiedEvidenceEmptyLaneCondition,
    lanename: String,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(condition.id, s"$lanename empty-lane condition identity", violations)
    _validate_scope(
      condition.context,
      condition.component,
      integrationcontext,
      integrationcomponent,
      s"$lanename empty-lane condition '${condition.id}'",
      violations
    )
    _validate_attribution(condition.attribution, s"$lanename empty-lane condition '${condition.id}'", violations)
    _validate_condition(condition.condition, s"$lanename empty-lane condition '${condition.id}'", violations)
    _validate_limitations(condition.limitations, s"$lanename empty-lane condition '${condition.id}'", violations)
    _validate_stable_tie_key(condition.stableTieKey, s"$lanename empty-lane condition '${condition.id}'", violations)
  }

  private def _validate_lane_occupancy(
    lanename: String,
    hasevidence: Boolean,
    hasemptycondition: Boolean,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (hasevidence == hasemptycondition)
      violations += s"$lanename lane must contain evidence or exactly one caller-admitted empty-lane condition, but not both or neither."

  private def _validate_scope(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    integrationcontext: MonoKotoProjectionContextIdentity,
    integrationcomponent: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(context.value, s"$subject bounded CCDM context identity", violations)
    if (context != integrationcontext)
      violations += s"$subject is outside the exact Lifecycle Unified Evidence Integration CCDM context."
    _require_nonblank(component.value, s"$subject Component identity", violations)
    if (component != integrationcomponent)
      violations += s"$subject is outside the exact Lifecycle Unified Evidence Integration Component scope."
  }

  private def _validate_attribution(
    attribution: ComponentDashboardSourceAttribution,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(attribution.sourceId, s"$subject source identity", violations)
    _require_nonblank(attribution.authorityScope, s"$subject source authority scope", violations)
    _require_nonblank(attribution.sourceLocator, s"$subject source attribution locator", violations)
  }

  private def _validate_condition(
    condition: ComponentDashboardCondition,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(condition.availability, s"$subject condition availability", violations)
    _require_nonblank(condition.authorization, s"$subject condition authorization", violations)
    condition.redaction.foreach(_require_nonblank(_, s"$subject condition redaction", violations))
    condition.explicitAbsence.foreach(_require_nonblank(_, s"$subject condition explicit absence", violations))
    condition.ambiguity.foreach(_require_nonblank(_, s"$subject condition ambiguity", violations))
    condition.conflict.foreach(_require_nonblank(_, s"$subject condition conflict", violations))
    condition.staleness.foreach(_require_nonblank(_, s"$subject condition staleness", violations))
    condition.malformedEvidence.foreach(_require_nonblank(_, s"$subject condition malformed evidence", violations))
    condition.limitations.foreach(_require_nonblank(_, s"$subject condition limitation", violations))
  }

  private def _validate_limitations(
    limitations: Vector[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    limitations.foreach(_require_nonblank(_, s"$subject limitation", violations))

  private def _validate_stable_tie_key(
    stabletiekey: Option[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    stabletiekey.foreach(_require_nonblank(_, s"$subject stable tie key", violations))

  private def _validate_duplicate_ids(
    ids: Vector[String],
    lanename: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(ids).foreach { id =>
      violations += s"Duplicate $lanename identity '$id'."
    }

  private def _order_declared_relationships(
    relationships: Vector[LifecycleUnifiedEvidenceDeclaredRelationship]
  ): Vector[LifecycleUnifiedEvidenceDeclaredRelationship] =
    relationships.sortBy { relationship =>
      val familyindex = LifecycleUnifiedEvidenceRelationshipFamily.presentationSequence.indexOf(relationship.family)
      (familyindex, relationship.id, relationship.stableTieKey.getOrElse(""))
    }

  private def _order_runtime_evidence(
    evidence: Vector[LifecycleUnifiedEvidenceRuntimeEvidence]
  ): Vector[LifecycleUnifiedEvidenceRuntimeEvidence] =
    evidence.sortBy { item =>
      (item.declaredRelationshipId, item.id, item.stableTieKey.getOrElse(""))
    }

  private def _order_usage_evidence(
    evidence: Vector[LifecycleUnifiedEvidenceUsageEvidence]
  ): Vector[LifecycleUnifiedEvidenceUsageEvidence] =
    evidence.sortBy { item =>
      (item.id, item.stableTieKey.getOrElse(""))
    }

  private def _order_operation_evidence(
    evidence: Vector[LifecycleUnifiedEvidenceOperationEvidence]
  ): Vector[LifecycleUnifiedEvidenceOperationEvidence] =
    evidence.sortBy { item =>
      (item.id, item.stableTieKey.getOrElse(""))
    }

  private def _order_quality_review_evidence(
    evidence: Vector[LifecycleUnifiedEvidenceQualityReviewEvidence]
  ): Vector[LifecycleUnifiedEvidenceQualityReviewEvidence] =
    evidence.sortBy { item =>
      (item.id, item.stableTieKey.getOrElse(""))
    }

  private def _duplicate_ids(ids: Vector[String]): Vector[String] =
    ids.groupBy(identity).collect { case (id, values) if values.size > 1 => id }.toVector.sorted

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
