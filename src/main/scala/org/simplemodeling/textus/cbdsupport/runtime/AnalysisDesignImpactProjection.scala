package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait AnalysisDesignImpactOriginCategory

case object AnalysisDesignMonoKotoOrigin extends AnalysisDesignImpactOriginCategory

case object AnalysisDesignUseCaseOrigin extends AnalysisDesignImpactOriginCategory

object AnalysisDesignImpactOriginCategory {
  val presentationSequence: Vector[AnalysisDesignImpactOriginCategory] =
    Vector(AnalysisDesignMonoKotoOrigin, AnalysisDesignUseCaseOrigin)
}

sealed trait AnalysisDesignImpactOriginRole

case object AnalysisDesignMonoKotoSubject extends AnalysisDesignImpactOriginRole

case object AnalysisDesignMonoKotoReference extends AnalysisDesignImpactOriginRole

case object AnalysisDesignUseCaseSubject extends AnalysisDesignImpactOriginRole

case object AnalysisDesignUseCaseReference extends AnalysisDesignImpactOriginRole

case object AnalysisDesignUseCaseFlow extends AnalysisDesignImpactOriginRole

case object AnalysisDesignUseCaseFlowStep extends AnalysisDesignImpactOriginRole

object AnalysisDesignImpactOriginRole {
  def presentationSequence(
    category: AnalysisDesignImpactOriginCategory
  ): Vector[AnalysisDesignImpactOriginRole] =
    category match {
      case AnalysisDesignMonoKotoOrigin =>
        Vector(AnalysisDesignMonoKotoSubject, AnalysisDesignMonoKotoReference)
      case AnalysisDesignUseCaseOrigin =>
        Vector(
          AnalysisDesignUseCaseSubject,
          AnalysisDesignUseCaseReference,
          AnalysisDesignUseCaseFlow,
          AnalysisDesignUseCaseFlowStep
        )
    }
}

sealed trait AnalysisDesignImpactCategory

case object AnalysisDesignImpactEntity extends AnalysisDesignImpactCategory

case object AnalysisDesignImpactEvent extends AnalysisDesignImpactCategory

case object AnalysisDesignImpactStructure extends AnalysisDesignImpactCategory

case object AnalysisDesignImpactWorkflow extends AnalysisDesignImpactCategory

case object AnalysisDesignImpactStateMachine extends AnalysisDesignImpactCategory

case object AnalysisDesignImpactCanonicalSourceLocation extends AnalysisDesignImpactCategory

object AnalysisDesignImpactCategory {
  val presentationSequence: Vector[AnalysisDesignImpactCategory] =
    Vector(
      AnalysisDesignImpactEntity,
      AnalysisDesignImpactEvent,
      AnalysisDesignImpactStructure,
      AnalysisDesignImpactWorkflow,
      AnalysisDesignImpactStateMachine,
      AnalysisDesignImpactCanonicalSourceLocation
    )
}

sealed trait AnalysisDesignImpactRole

case object AnalysisDesignImpactEntitySubject extends AnalysisDesignImpactRole

case object AnalysisDesignImpactEntityMetadata extends AnalysisDesignImpactRole

case object AnalysisDesignImpactEventSubject extends AnalysisDesignImpactRole

case object AnalysisDesignImpactEventAssertion extends AnalysisDesignImpactRole

case object AnalysisDesignImpactEventEndpoint extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStructureRelation extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStructureAssertion extends AnalysisDesignImpactRole

case object AnalysisDesignImpactWorkflowSubject extends AnalysisDesignImpactRole

case object AnalysisDesignImpactWorkflowFlow extends AnalysisDesignImpactRole

case object AnalysisDesignImpactWorkflowEndpoint extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStateMachineSubject extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStateMachineTransition extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStateMachineTransitionAdjunct extends AnalysisDesignImpactRole

case object AnalysisDesignImpactStateMachineEndpoint extends AnalysisDesignImpactRole

case object AnalysisDesignImpactCanonicalSourceLocationTrace extends AnalysisDesignImpactRole

object AnalysisDesignImpactRole {
  def presentationSequence(
    category: AnalysisDesignImpactCategory
  ): Vector[AnalysisDesignImpactRole] =
    category match {
      case AnalysisDesignImpactEntity =>
        Vector(AnalysisDesignImpactEntitySubject, AnalysisDesignImpactEntityMetadata)
      case AnalysisDesignImpactEvent =>
        Vector(
          AnalysisDesignImpactEventSubject,
          AnalysisDesignImpactEventAssertion,
          AnalysisDesignImpactEventEndpoint
        )
      case AnalysisDesignImpactStructure =>
        Vector(AnalysisDesignImpactStructureRelation, AnalysisDesignImpactStructureAssertion)
      case AnalysisDesignImpactWorkflow =>
        Vector(
          AnalysisDesignImpactWorkflowSubject,
          AnalysisDesignImpactWorkflowFlow,
          AnalysisDesignImpactWorkflowEndpoint
        )
      case AnalysisDesignImpactStateMachine =>
        Vector(
          AnalysisDesignImpactStateMachineSubject,
          AnalysisDesignImpactStateMachineTransition,
          AnalysisDesignImpactStateMachineTransitionAdjunct,
          AnalysisDesignImpactStateMachineEndpoint
        )
      case AnalysisDesignImpactCanonicalSourceLocation =>
        Vector(AnalysisDesignImpactCanonicalSourceLocationTrace)
    }
}

final case class AnalysisDesignImpactProposal(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  originSemanticTargetId: ComponentDashboardSemanticTargetIdentity,
  originRecordId: String,
  originCategory: AnalysisDesignImpactOriginCategory,
  originRole: AnalysisDesignImpactOriginRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  proposer: String,
  statement: String,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class AnalysisDesignImpactRecord(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  semanticTargetId: Option[ComponentDashboardSemanticTargetIdentity],
  category: AnalysisDesignImpactCategory,
  role: AnalysisDesignImpactRole,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  stableTieKey: Option[String]
)

final case class AnalysisDesignImpactLink(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  proposalId: String,
  impactRecordId: String,
  semanticRelationshipId: ComponentDashboardSemanticTargetIdentity,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  stableTieKey: Option[String]
)

final case class AnalysisDesignImpactGap(
  id: String,
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  proposalId: String,
  requestedCategory: AnalysisDesignImpactCategory,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitationReason: String,
  stableTieKey: Option[String]
)

final case class AnalysisDesignImpactProjectedProposal(
  sourceProposal: AnalysisDesignImpactProposal
)

final case class AnalysisDesignImpactProjectedRecord(
  sourceRecord: AnalysisDesignImpactRecord
)

final case class AnalysisDesignImpactProjectedLink(
  sourceLink: AnalysisDesignImpactLink,
  proposal: AnalysisDesignImpactProposal,
  impactRecord: AnalysisDesignImpactRecord
)

final case class AnalysisDesignImpactProjectedGap(
  sourceGap: AnalysisDesignImpactGap,
  proposal: AnalysisDesignImpactProposal
)

final case class AnalysisDesignImpactProjectionFailure(violations: Vector[String])

final case class AnalysisDesignImpactProjection(
  context: MonoKotoProjectionContextIdentity,
  component: ComponentDashboardComponentIdentity,
  proposals: Vector[AnalysisDesignImpactProjectedProposal],
  records: Vector[AnalysisDesignImpactProjectedRecord],
  links: Vector[AnalysisDesignImpactProjectedLink],
  gaps: Vector[AnalysisDesignImpactProjectedGap]
)

object AnalysisDesignImpactProjection {
  def create(
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    proposals: Vector[AnalysisDesignImpactProposal],
    records: Vector[AnalysisDesignImpactRecord],
    links: Vector[AnalysisDesignImpactLink],
    gaps: Vector[AnalysisDesignImpactGap]
  ): Either[AnalysisDesignImpactProjectionFailure, AnalysisDesignImpactProjection] = {
    val violations = Vector.newBuilder[String]

    _validate_context(context, violations)
    _validate_component(component, violations)
    _validate_unique_ids(proposals, records, links, gaps, violations)
    proposals.foreach(_validate_proposal(_, context, component, violations))
    records.foreach(_validate_record(_, context, component, violations))

    val proposalbyid = proposals.map(proposal => proposal.id -> proposal).toMap
    val recordbyid = records.map(record => record.id -> record).toMap
    links.foreach(_validate_link(_, context, component, proposalbyid, recordbyid, violations))
    _validate_duplicate_link_pairs(links, violations)
    gaps.foreach(_validate_gap(_, context, component, proposalbyid, recordbyid, links, violations))

    val result = violations.result()
    if (result.nonEmpty)
      Left(AnalysisDesignImpactProjectionFailure(result))
    else {
      val orderedproposals = _order_proposals(proposals)
      val orderedrecords = _order_records(records)
      val orderedlinks = _order_links(links, proposalbyid, recordbyid)
      val orderedgaps = _order_gaps(gaps, proposalbyid)
      Right(
        AnalysisDesignImpactProjection(
          context,
          component,
          orderedproposals.map(AnalysisDesignImpactProjectedProposal.apply),
          orderedrecords.map(AnalysisDesignImpactProjectedRecord.apply),
          orderedlinks.map(link => _project_link(link, proposalbyid, recordbyid)),
          orderedgaps.map(gap => _project_gap(gap, proposalbyid))
        )
      )
    }
  }

  private def _validate_context(
    context: MonoKotoProjectionContextIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(context.value, "Analysis-to-design impact bounded CCDM context identity", violations)

  private def _validate_component(
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, "Analysis-to-design impact Component identity", violations)

  private def _validate_unique_ids(
    proposals: Vector[AnalysisDesignImpactProposal],
    records: Vector[AnalysisDesignImpactRecord],
    links: Vector[AnalysisDesignImpactLink],
    gaps: Vector[AnalysisDesignImpactGap],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _duplicate_ids(proposals.map(_.id) ++ records.map(_.id) ++ links.map(_.id) ++ gaps.map(_.id)).foreach { id =>
      violations += s"Analysis-to-design proposal, impact record, link, or gap identity '$id' is duplicate."
    }

  private def _validate_proposal(
    proposal: AnalysisDesignImpactProposal,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(proposal.id, "Analysis-to-design proposal identity", violations)
    _validate_scope(proposal.context, proposal.component, context, component, s"Analysis-to-design proposal '${proposal.id}'", violations)
    _require_nonblank(proposal.originSemanticTargetId.value, s"Analysis-to-design proposal '${proposal.id}' origin semantic target identity", violations)
    _require_nonblank(proposal.originRecordId, s"Analysis-to-design proposal '${proposal.id}' origin record identity", violations)
    if (!_compatible_origin_role(proposal.originCategory, proposal.originRole))
      violations += s"Analysis-to-design proposal '${proposal.id}' origin role ${proposal.originRole} is incompatible with origin category ${proposal.originCategory}."
    _validate_attribution(proposal.attribution, s"Analysis-to-design proposal '${proposal.id}'", violations)
    _validate_condition(proposal.condition, s"Analysis-to-design proposal '${proposal.id}'", violations)
    _require_nonblank(proposal.proposer, s"Analysis-to-design proposal '${proposal.id}' proposer", violations)
    _require_nonblank(proposal.statement, s"Analysis-to-design proposal '${proposal.id}' statement", violations)
    _validate_stated_limitations(proposal.limitations, s"Analysis-to-design proposal '${proposal.id}'", violations)
    _validate_tie_key(proposal.stableTieKey, s"Analysis-to-design proposal '${proposal.id}'", violations)
  }

  private def _validate_record(
    record: AnalysisDesignImpactRecord,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(record.id, "Analysis-to-design impact record identity", violations)
    _validate_scope(record.context, record.component, context, component, s"Analysis-to-design impact record '${record.id}'", violations)
    if (!_compatible_impact_role(record.category, record.role))
      violations += s"Analysis-to-design impact record '${record.id}' role ${record.role} is incompatible with category ${record.category}."
    _validate_attribution(record.attribution, s"Analysis-to-design impact record '${record.id}'", violations)
    _validate_condition(record.condition, s"Analysis-to-design impact record '${record.id}'", violations)
    _validate_tie_key(record.stableTieKey, s"Analysis-to-design impact record '${record.id}'", violations)
    record.semanticTargetId.foreach { identity =>
      _require_nonblank(identity.value, s"Analysis-to-design impact record '${record.id}' semantic target identity", violations)
    }
    if (record.category != AnalysisDesignImpactCanonicalSourceLocation && record.semanticTargetId.isEmpty)
      violations += s"Analysis-to-design impact record '${record.id}' category ${record.category} requires an exact semantic target identity."
  }

  private def _validate_link(
    link: AnalysisDesignImpactLink,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    proposalbyid: Map[String, AnalysisDesignImpactProposal],
    recordbyid: Map[String, AnalysisDesignImpactRecord],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(link.id, "Analysis-to-design impact link identity", violations)
    _validate_scope(link.context, link.component, context, component, s"Analysis-to-design impact link '${link.id}'", violations)
    _require_nonblank(link.proposalId, s"Analysis-to-design impact link '${link.id}' proposal identity", violations)
    _require_nonblank(link.impactRecordId, s"Analysis-to-design impact link '${link.id}' impact record identity", violations)
    _require_nonblank(link.semanticRelationshipId.value, s"Analysis-to-design impact link '${link.id}' semantic relationship identity", violations)
    _validate_attribution(link.attribution, s"Analysis-to-design impact link '${link.id}'", violations)
    _validate_condition(link.condition, s"Analysis-to-design impact link '${link.id}'", violations)
    _validate_stated_limitations(link.limitations, s"Analysis-to-design impact link '${link.id}'", violations)
    _validate_tie_key(link.stableTieKey, s"Analysis-to-design impact link '${link.id}'", violations)

    if (!proposalbyid.contains(link.proposalId))
      violations += s"Analysis-to-design impact link '${link.id}' proposal '${link.proposalId}' is unresolved."
    if (!recordbyid.contains(link.impactRecordId))
      violations += s"Analysis-to-design impact link '${link.id}' impact record '${link.impactRecordId}' is unresolved."
  }

  private def _validate_duplicate_link_pairs(
    links: Vector[AnalysisDesignImpactLink],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    links
      .groupBy(link => (link.proposalId, link.impactRecordId))
      .collect { case ((proposalid, recordid), values) if values.size > 1 => (proposalid, recordid) }
      .toVector
      .sortBy(pair => (pair._1, pair._2))
      .foreach { pair =>
        violations += s"Analysis-to-design proposal '${pair._1}' has duplicate admitted impact association '${pair._2}'."
      }

  private def _validate_gap(
    gap: AnalysisDesignImpactGap,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    proposalbyid: Map[String, AnalysisDesignImpactProposal],
    recordbyid: Map[String, AnalysisDesignImpactRecord],
    links: Vector[AnalysisDesignImpactLink],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(gap.id, "Analysis-to-design impact gap identity", violations)
    _validate_scope(gap.context, gap.component, context, component, s"Analysis-to-design impact gap '${gap.id}'", violations)
    _require_nonblank(gap.proposalId, s"Analysis-to-design impact gap '${gap.id}' proposal identity", violations)
    _validate_attribution(gap.attribution, s"Analysis-to-design impact gap '${gap.id}'", violations)
    _validate_condition(gap.condition, s"Analysis-to-design impact gap '${gap.id}'", violations)
    _require_nonblank(gap.limitationReason, s"Analysis-to-design impact gap '${gap.id}' limitation reason", violations)
    _validate_tie_key(gap.stableTieKey, s"Analysis-to-design impact gap '${gap.id}'", violations)

    if (!proposalbyid.contains(gap.proposalId))
      violations += s"Analysis-to-design impact gap '${gap.id}' proposal '${gap.proposalId}' is unresolved."
    val linkedcategories = links.flatMap { link =>
      if (link.proposalId == gap.proposalId)
        recordbyid.get(link.impactRecordId).map(_.category)
      else
        None
    }
    if (linkedcategories.contains(gap.requestedCategory))
      violations += s"Analysis-to-design impact gap '${gap.id}' cannot name category ${gap.requestedCategory} already directly linked to proposal '${gap.proposalId}'."
  }

  private def _validate_scope(
    recordcontext: MonoKotoProjectionContextIdentity,
    recordcomponent: ComponentDashboardComponentIdentity,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(recordcontext.value, s"$subject bounded CCDM context identity", violations)
    if (recordcontext != context)
      violations += s"$subject is outside Analysis-to-design impact bounded CCDM context '${context.value}'."
    _require_nonblank(recordcomponent.value, s"$subject Component identity", violations)
    if (recordcomponent != component)
      violations += s"$subject is outside Analysis-to-design impact Component '${component.value}'."
  }

  private def _validate_attribution(
    attribution: ComponentDashboardSourceAttribution,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(attribution.sourceId, s"$subject source identity", violations)
    _require_nonblank(attribution.authorityScope, s"$subject source authority scope", violations)
    _require_nonblank(attribution.sourceLocator, s"$subject source locator", violations)
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
    _validate_limitations(condition.limitations, s"$subject condition", violations)
  }

  private def _validate_limitations(
    limitations: Vector[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    limitations.foreach(_require_nonblank(_, s"$subject limitation", violations))

  private def _validate_stated_limitations(
    limitations: Vector[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    if (limitations.isEmpty)
      violations += s"$subject must state at least one limitation."
    _validate_limitations(limitations, subject, violations)
  }

  private def _validate_tie_key(
    tiekey: Option[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    tiekey.foreach(_require_nonblank(_, s"$subject stable tie key", violations))

  private def _compatible_origin_role(
    category: AnalysisDesignImpactOriginCategory,
    role: AnalysisDesignImpactOriginRole
  ): Boolean =
    AnalysisDesignImpactOriginRole.presentationSequence(category).contains(role)

  private def _compatible_impact_role(
    category: AnalysisDesignImpactCategory,
    role: AnalysisDesignImpactRole
  ): Boolean =
    AnalysisDesignImpactRole.presentationSequence(category).contains(role)

  private def _project_link(
    link: AnalysisDesignImpactLink,
    proposalbyid: Map[String, AnalysisDesignImpactProposal],
    recordbyid: Map[String, AnalysisDesignImpactRecord]
  ): AnalysisDesignImpactProjectedLink =
    AnalysisDesignImpactProjectedLink(link, proposalbyid(link.proposalId), recordbyid(link.impactRecordId))

  private def _project_gap(
    gap: AnalysisDesignImpactGap,
    proposalbyid: Map[String, AnalysisDesignImpactProposal]
  ): AnalysisDesignImpactProjectedGap =
    AnalysisDesignImpactProjectedGap(gap, proposalbyid(gap.proposalId))

  private def _order_proposals(
    proposals: Vector[AnalysisDesignImpactProposal]
  ): Vector[AnalysisDesignImpactProposal] =
    proposals.sortBy { proposal =>
      (
        proposal.originSemanticTargetId.value,
        _origin_category_index(proposal.originCategory),
        _origin_role_index(proposal.originCategory, proposal.originRole),
        proposal.originRecordId,
        proposal.id,
        proposal.stableTieKey.getOrElse("")
      )
    }

  private def _order_records(
    records: Vector[AnalysisDesignImpactRecord]
  ): Vector[AnalysisDesignImpactRecord] =
    records.sortBy { record =>
      (
        record.semanticTargetId.map(_.value).getOrElse(""),
        _impact_category_index(record.category),
        _impact_role_index(record.category, record.role),
        record.id,
        record.stableTieKey.getOrElse("")
      )
    }

  private def _order_links(
    links: Vector[AnalysisDesignImpactLink],
    proposalbyid: Map[String, AnalysisDesignImpactProposal],
    recordbyid: Map[String, AnalysisDesignImpactRecord]
  ): Vector[AnalysisDesignImpactLink] =
    links.sortBy { link =>
      val proposal = proposalbyid(link.proposalId)
      val record = recordbyid(link.impactRecordId)
      (
        (
          proposal.originSemanticTargetId.value,
          _origin_category_index(proposal.originCategory),
          _origin_role_index(proposal.originCategory, proposal.originRole)
        ),
        (
          record.semanticTargetId.map(_.value).getOrElse(""),
          _impact_category_index(record.category),
          _impact_role_index(record.category, record.role)
        ),
        (
          link.semanticRelationshipId.value,
          proposal.id,
          record.id,
          link.id,
          link.stableTieKey.getOrElse("")
        )
      )
    }

  private def _order_gaps(
    gaps: Vector[AnalysisDesignImpactGap],
    proposalbyid: Map[String, AnalysisDesignImpactProposal]
  ): Vector[AnalysisDesignImpactGap] =
    gaps.sortBy { gap =>
      val proposal = proposalbyid(gap.proposalId)
      (
        proposal.originSemanticTargetId.value,
        _origin_category_index(proposal.originCategory),
        _origin_role_index(proposal.originCategory, proposal.originRole),
        _impact_category_index(gap.requestedCategory),
        proposal.id,
        gap.id,
        gap.stableTieKey.getOrElse("")
      )
    }

  private def _origin_category_index(category: AnalysisDesignImpactOriginCategory): Int =
    AnalysisDesignImpactOriginCategory.presentationSequence.indexOf(category)

  private def _origin_role_index(
    category: AnalysisDesignImpactOriginCategory,
    role: AnalysisDesignImpactOriginRole
  ): Int =
    AnalysisDesignImpactOriginRole.presentationSequence(category).indexOf(role)

  private def _impact_category_index(category: AnalysisDesignImpactCategory): Int =
    AnalysisDesignImpactCategory.presentationSequence.indexOf(category)

  private def _impact_role_index(
    category: AnalysisDesignImpactCategory,
    role: AnalysisDesignImpactRole
  ): Int =
    AnalysisDesignImpactRole.presentationSequence(category).indexOf(role)

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
