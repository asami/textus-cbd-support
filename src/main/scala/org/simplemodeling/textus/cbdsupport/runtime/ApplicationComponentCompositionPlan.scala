package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CompositionProvenance(
  sourceId: String,
  authorityScope: String,
  sourceLocator: String
)

final case class CompositionEvidenceAvailability(value: String)

final case class CompositionEvidenceAuthorization(value: String)

final case class CompositionEvidenceCondition(
  availability: CompositionEvidenceAvailability,
  authorization: CompositionEvidenceAuthorization,
  redactionReason: Option[String],
  absenceReason: Option[String],
  limitations: Vector[String]
)

final case class ApplicationIntent(
  id: String,
  applicationScope: String,
  provenance: CompositionProvenance
)

final case class RequiredCapability(
  id: String,
  applicationIntentId: String,
  provenance: CompositionProvenance
)

final case class ComponentEvidence(
  id: String,
  provenance: CompositionProvenance,
  condition: CompositionEvidenceCondition,
  requiredCapabilityIds: Vector[String],
  componentId: Option[String]
)

sealed trait AdvisoryProviderAvailability

case object AvailableAdvisoryProviderAvailability extends AdvisoryProviderAvailability

final case class UnavailableAdvisoryProviderAvailability(reason: String) extends AdvisoryProviderAvailability

final case class AdvisoryProviderContract(
  id: String,
  contractVersion: String,
  provenance: CompositionProvenance,
  availability: AdvisoryProviderAvailability,
  limitations: Vector[String]
)

final case class ComponentProposal(
  id: String,
  providerId: String,
  providerContractVersion: String,
  applicationIntentId: String,
  requiredCapabilityId: String,
  componentId: String,
  rationale: String,
  provenance: CompositionProvenance,
  consideredEvidenceIds: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String]
)

final case class HumanDecision(
  id: String,
  applicationIntentId: String,
  requiredCapabilityId: String,
  componentId: String,
  actor: String,
  rationale: String,
  provenance: CompositionProvenance,
  consideredEvidenceIds: Vector[String],
  conditions: Vector[String],
  limitations: Vector[String],
  promotedProposalId: Option[String] = None
)

sealed trait CoverageDispositionOutcome

sealed trait CoverageGapReason

case object NoLinkedEvidenceCoverageGapReason extends CoverageGapReason

final case class AllEvidenceAbsentCoverageGapReason(absenceReasons: Vector[String]) extends CoverageGapReason

final case class GapCoverageDispositionOutcome(reason: CoverageGapReason) extends CoverageDispositionOutcome

final case class AlternativeCoverageDispositionOutcome(componentIds: Vector[String]) extends CoverageDispositionOutcome

case object UnresolvedCoverageDispositionOutcome extends CoverageDispositionOutcome

final case class SelectedCoverageDispositionOutcome(
  decisionId: String,
  componentId: String,
  actor: String,
  rationale: String,
  provenance: CompositionProvenance,
  consideredEvidenceIds: Vector[String],
  matchingEvidenceIds: Vector[String],
  linkedEvidenceIds: Vector[String],
  linkedEvidenceConditions: Vector[CompositionEvidenceCondition],
  conditions: Vector[String],
  limitations: Vector[String],
  promotedProposalId: Option[String] = None
) extends CoverageDispositionOutcome

final case class AdmittedComponentProposal(
  proposal: ComponentProposal,
  provider: AdvisoryProviderContract,
  matchingEvidenceIds: Vector[String],
  linkedEvidenceIds: Vector[String],
  linkedEvidenceConditions: Vector[CompositionEvidenceCondition]
)

final case class ProposalCoverageDispositionOutcome(
  baseline: CoverageDispositionOutcome,
  proposals: Vector[AdmittedComponentProposal]
) extends CoverageDispositionOutcome

final case class CoverageDisposition(
  capabilityId: String,
  evidenceIds: Vector[String],
  outcome: CoverageDispositionOutcome,
  decisionId: Option[String] = None
)

final case class ApplicationComponentCompositionPlanFailure(violations: Vector[String])

final case class HumanDecisionAdmissionFailure(
  decision: HumanDecision,
  violations: Vector[String]
)

final case class AdvisoryProviderAdmissionFailure(
  provider: AdvisoryProviderContract,
  violations: Vector[String]
)

final case class ComponentProposalAdmissionFailure(
  proposal: ComponentProposal,
  violations: Vector[String]
)

final case class ApplicationComponentCompositionPlan(
  intent: ApplicationIntent,
  capabilities: Vector[RequiredCapability],
  evidence: Vector[ComponentEvidence],
  dispositions: Vector[CoverageDisposition],
  decisionAdmissionFailures: Vector[HumanDecisionAdmissionFailure] = Vector.empty,
  providerAdmissionFailures: Vector[AdvisoryProviderAdmissionFailure] = Vector.empty,
  proposalAdmissionFailures: Vector[ComponentProposalAdmissionFailure] = Vector.empty
)

object ApplicationComponentCompositionPlan {
  private final case class DecisionAdmission(
    capabilityid: String,
    decisionid: String,
    outcome: SelectedCoverageDispositionOutcome
  )

  private final case class ProviderAdmissions(
    providers: Map[String, AdvisoryProviderContract],
    failures: Vector[AdvisoryProviderAdmissionFailure]
  )

  private final case class ProposalAdmissions(
    proposals: Vector[AdmittedComponentProposal],
    failures: Vector[ComponentProposalAdmissionFailure]
  )

  def create(
    intent: ApplicationIntent,
    capabilities: Vector[RequiredCapability],
    evidence: Vector[ComponentEvidence],
    decisions: Vector[HumanDecision] = Vector.empty,
    providers: Vector[AdvisoryProviderContract] = Vector.empty,
    proposals: Vector[ComponentProposal] = Vector.empty
  ): Either[ApplicationComponentCompositionPlanFailure, ApplicationComponentCompositionPlan] = {
    val violations = Vector.newBuilder[String]

    _validate_intent(intent, violations)
    _validate_capabilities(intent, capabilities, violations)
    _validate_evidence(evidence, capabilities.map(_.id).toSet, violations)

    val result = violations.result()
    if (result.nonEmpty) Left(ApplicationComponentCompositionPlanFailure(result))
    else {
      val dispositions = capabilities.map { capability =>
        val linkedevidence = evidence.filter(_.requiredCapabilityIds.contains(capability.id))
        CoverageDisposition(
          capability.id,
          linkedevidence.map(_.id),
          _classify_coverage(linkedevidence)
        )
      }
      val provideradmissions = _admit_providers(providers)
      val proposaladmissions = _admit_proposals(
        intent,
        capabilities,
        evidence,
        providers,
        provideradmissions.providers,
        proposals
      )
      Right(
        _admit_decisions(
          intent,
          capabilities,
          evidence,
          dispositions,
          decisions,
          proposaladmissions.proposals,
          provideradmissions.failures,
          proposaladmissions.failures
        )
      )
    }
  }

  private def _admit_decisions(
    intent: ApplicationIntent,
    capabilities: Vector[RequiredCapability],
    evidence: Vector[ComponentEvidence],
    dispositions: Vector[CoverageDisposition],
    decisions: Vector[HumanDecision],
    admittedproposals: Vector[AdmittedComponentProposal],
    providerfailures: Vector[AdvisoryProviderAdmissionFailure],
    proposalfailures: Vector[ComponentProposalAdmissionFailure]
  ): ApplicationComponentCompositionPlan = {
    val capabilityids = capabilities.map(_.id).toSet
    val evidencebyid = evidence.map(item => item.id -> item).toMap
    val duplicateids = _duplicate_human_decision_ids(decisions)
    val duplicatescopes = _duplicate_human_decision_scopes(decisions)
    val proposalsbyid = admittedproposals.map(item => item.proposal.id -> item).toMap
    val admissions = decisions.map { decision =>
      _admit_decision(
        intent,
        capabilityids,
        evidencebyid,
        evidence,
        decision,
        duplicateids,
        duplicatescopes,
        proposalsbyid
      )
    }
    val selectedbycapability = admissions.collect { case Right(admission) =>
      admission.capabilityid -> admission
    }.toMap
    val selecteddispositions = dispositions.map { disposition =>
      selectedbycapability.get(disposition.capabilityId).map { admission =>
        disposition.copy(outcome = admission.outcome, decisionId = Some(admission.decisionid))
      }.getOrElse(disposition)
    }
    val proposalsbycapability = admittedproposals.groupBy(_.proposal.requiredCapabilityId)
    val updateddispositions = selecteddispositions.map { disposition =>
      disposition.outcome match {
        case _: SelectedCoverageDispositionOutcome => disposition
        case _ => proposalsbycapability.get(disposition.capabilityId).map { items =>
          disposition.copy(outcome = ProposalCoverageDispositionOutcome(disposition.outcome, items))
        }.getOrElse(disposition)
      }
    }
    val admissionfailures = admissions.collect { case Left(failure) => failure }
    ApplicationComponentCompositionPlan(
      intent,
      capabilities,
      evidence,
      updateddispositions,
      admissionfailures,
      providerfailures,
      proposalfailures
    )
  }

  private def _admit_decision(
    intent: ApplicationIntent,
    capabilityids: Set[String],
    evidencebyid: Map[String, ComponentEvidence],
    evidence: Vector[ComponentEvidence],
    decision: HumanDecision,
    duplicateids: Set[String],
    duplicatescopes: Set[(String, String)],
    admittedproposals: Map[String, AdmittedComponentProposal]
  ): Either[HumanDecisionAdmissionFailure, DecisionAdmission] = {
    val violations = Vector.newBuilder[String]

    _require_nonblank(decision.id, "human decision ID", violations)
    _require_nonblank(decision.applicationIntentId, s"human decision '${decision.id}' application intent ID", violations)
    _require_nonblank(decision.requiredCapabilityId, s"human decision '${decision.id}' required capability ID", violations)
    _require_nonblank(decision.componentId, s"human decision '${decision.id}' component ID", violations)
    _require_nonblank(decision.actor, s"human decision '${decision.id}' actor", violations)
    _require_nonblank(decision.rationale, s"human decision '${decision.id}' rationale", violations)
    _validate_provenance(decision.provenance, s"human decision '${decision.id}'", violations)
    if (decision.consideredEvidenceIds.isEmpty)
      violations += s"Human decision '${decision.id}' must name considered component evidence."
    decision.consideredEvidenceIds.foreach { evidenceid =>
      _require_nonblank(evidenceid, s"human decision '${decision.id}' considered component evidence ID", violations)
      if (evidenceid.trim.nonEmpty)
        evidencebyid.get(evidenceid) match {
          case Some(item) if !item.requiredCapabilityIds.contains(decision.requiredCapabilityId) =>
            violations += s"Human decision '${decision.id}' considered component evidence '$evidenceid' is not linked to required capability '${decision.requiredCapabilityId}'."
          case None =>
            violations += s"Human decision '${decision.id}' names unknown considered component evidence '$evidenceid'."
          case _ => ()
        }
    }
    decision.conditions.foreach(_require_nonblank(_, s"human decision '${decision.id}' condition", violations))
    decision.limitations.foreach(_require_nonblank(_, s"human decision '${decision.id}' limitation", violations))
    if (duplicateids.contains(decision.id))
      violations += s"Duplicate human decision ID '${decision.id}'."
    if (duplicatescopes.contains((decision.applicationIntentId, decision.requiredCapabilityId)))
      violations += s"Duplicate human decision scope intent '${decision.applicationIntentId}' capability '${decision.requiredCapabilityId}'."
    if (decision.applicationIntentId != intent.id)
      violations += s"Human decision '${decision.id}' is outside application intent '${intent.id}'."
    if (!capabilityids.contains(decision.requiredCapabilityId))
      violations += s"Human decision '${decision.id}' names unknown required capability '${decision.requiredCapabilityId}'."

    val linkedevidence = evidence.filter(_.requiredCapabilityIds.contains(decision.requiredCapabilityId))
    val matchingevidenceids = decision.consideredEvidenceIds.flatMap { evidenceid =>
      evidencebyid.get(evidenceid).filter { item =>
        item.requiredCapabilityIds.contains(decision.requiredCapabilityId) && item.componentId.contains(decision.componentId)
      }.map(_.id)
    }
    if (matchingevidenceids.isEmpty)
      violations += s"Human decision '${decision.id}' target component '${decision.componentId}' is not asserted by its considered linked evidence."
    decision.promotedProposalId.foreach { proposalid =>
      _require_nonblank(proposalid, s"human decision '${decision.id}' promoted component proposal ID", violations)
      admittedproposals.get(proposalid) match {
        case Some(admitted)
            if admitted.proposal.applicationIntentId == decision.applicationIntentId &&
              admitted.proposal.requiredCapabilityId == decision.requiredCapabilityId &&
              admitted.proposal.componentId == decision.componentId => ()
        case Some(_) =>
          violations += s"Human decision '${decision.id}' promoted component proposal '$proposalid' does not match its exact scope and target."
        case None if proposalid.trim.nonEmpty =>
          violations += s"Human decision '${decision.id}' names unadmitted component proposal '$proposalid'."
        case _ => ()
      }
    }

    val result = violations.result()
    if (result.nonEmpty)
      Left(HumanDecisionAdmissionFailure(decision, result))
    else
      Right(
        DecisionAdmission(
          decision.requiredCapabilityId,
          decision.id,
          SelectedCoverageDispositionOutcome(
            decision.id,
            decision.componentId,
            decision.actor,
            decision.rationale,
            decision.provenance,
            decision.consideredEvidenceIds,
            matchingevidenceids,
            linkedevidence.map(_.id),
            linkedevidence.map(_.condition),
            decision.conditions,
            decision.limitations,
            decision.promotedProposalId
          )
        )
      )
  }

  private def _admit_providers(providers: Vector[AdvisoryProviderContract]): ProviderAdmissions = {
    val duplicateids = _duplicate_advisory_provider_ids(providers)
    val admissions = providers.map { provider =>
      _admit_provider(provider, duplicateids)
    }
    ProviderAdmissions(
      admissions.collect { case Right(provider) => provider.id -> provider }.toMap,
      admissions.collect { case Left(failure) => failure }
    )
  }

  private def _admit_provider(
    provider: AdvisoryProviderContract,
    duplicateids: Set[String]
  ): Either[AdvisoryProviderAdmissionFailure, AdvisoryProviderContract] = {
    val violations = Vector.newBuilder[String]

    _require_nonblank(provider.id, "advisory provider ID", violations)
    _require_nonblank(provider.contractVersion, s"advisory provider '${provider.id}' contract version", violations)
    _validate_provenance(provider.provenance, s"advisory provider '${provider.id}'", violations)
    provider.limitations.foreach(_require_nonblank(_, s"advisory provider '${provider.id}' limitation", violations))
    provider.availability match {
      case AvailableAdvisoryProviderAvailability => ()
      case UnavailableAdvisoryProviderAvailability(reason) =>
        _require_nonblank(reason, s"advisory provider '${provider.id}' unavailable reason", violations)
        violations += s"Advisory provider '${provider.id}' is unavailable."
    }
    if (duplicateids.contains(provider.id))
      violations += s"Duplicate advisory provider ID '${provider.id}'."

    val result = violations.result()
    if (result.nonEmpty) Left(AdvisoryProviderAdmissionFailure(provider, result))
    else Right(provider)
  }

  private def _admit_proposals(
    intent: ApplicationIntent,
    capabilities: Vector[RequiredCapability],
    evidence: Vector[ComponentEvidence],
    providers: Vector[AdvisoryProviderContract],
    admittedproviders: Map[String, AdvisoryProviderContract],
    proposals: Vector[ComponentProposal]
  ): ProposalAdmissions = {
    val capabilityids = capabilities.map(_.id).toSet
    val evidencebyid = evidence.map(item => item.id -> item).toMap
    val providersbyid = providers.groupBy(_.id)
    val duplicateids = _duplicate_component_proposal_ids(proposals)
    val admissions = proposals.map { proposal =>
      _admit_proposal(
        intent,
        capabilityids,
        evidencebyid,
        evidence,
        providersbyid,
        admittedproviders,
        proposal,
        duplicateids
      )
    }
    ProposalAdmissions(
      admissions.collect { case Right(admission) => admission },
      admissions.collect { case Left(failure) => failure }
    )
  }

  private def _admit_proposal(
    intent: ApplicationIntent,
    capabilityids: Set[String],
    evidencebyid: Map[String, ComponentEvidence],
    evidence: Vector[ComponentEvidence],
    providersbyid: Map[String, Vector[AdvisoryProviderContract]],
    admittedproviders: Map[String, AdvisoryProviderContract],
    proposal: ComponentProposal,
    duplicateids: Set[String]
  ): Either[ComponentProposalAdmissionFailure, AdmittedComponentProposal] = {
    val violations = Vector.newBuilder[String]

    _require_nonblank(proposal.id, "component proposal ID", violations)
    _require_nonblank(proposal.providerId, s"component proposal '${proposal.id}' advisory provider ID", violations)
    _require_nonblank(proposal.providerContractVersion, s"component proposal '${proposal.id}' provider contract version", violations)
    _require_nonblank(proposal.applicationIntentId, s"component proposal '${proposal.id}' application intent ID", violations)
    _require_nonblank(proposal.requiredCapabilityId, s"component proposal '${proposal.id}' required capability ID", violations)
    _require_nonblank(proposal.componentId, s"component proposal '${proposal.id}' component ID", violations)
    _require_nonblank(proposal.rationale, s"component proposal '${proposal.id}' rationale", violations)
    _validate_provenance(proposal.provenance, s"component proposal '${proposal.id}'", violations)
    if (proposal.consideredEvidenceIds.isEmpty)
      violations += s"Component proposal '${proposal.id}' must name considered component evidence."
    proposal.consideredEvidenceIds.foreach { evidenceid =>
      _require_nonblank(evidenceid, s"component proposal '${proposal.id}' considered component evidence ID", violations)
      if (evidenceid.trim.nonEmpty)
        evidencebyid.get(evidenceid) match {
          case Some(item) if !item.requiredCapabilityIds.contains(proposal.requiredCapabilityId) =>
            violations += s"Component proposal '${proposal.id}' considered component evidence '$evidenceid' is not linked to required capability '${proposal.requiredCapabilityId}'."
          case None =>
            violations += s"Component proposal '${proposal.id}' names unknown considered component evidence '$evidenceid'."
          case _ => ()
        }
    }
    proposal.conditions.foreach(_require_nonblank(_, s"component proposal '${proposal.id}' condition", violations))
    proposal.limitations.foreach(_require_nonblank(_, s"component proposal '${proposal.id}' limitation", violations))
    if (duplicateids.contains(proposal.id))
      violations += s"Duplicate component proposal ID '${proposal.id}'."
    if (proposal.applicationIntentId != intent.id)
      violations += s"Component proposal '${proposal.id}' is outside application intent '${intent.id}'."
    if (!capabilityids.contains(proposal.requiredCapabilityId))
      violations += s"Component proposal '${proposal.id}' names unknown required capability '${proposal.requiredCapabilityId}'."

    val provider = admittedproviders.get(proposal.providerId)
    provider match {
      case Some(item) if item.contractVersion != proposal.providerContractVersion =>
        violations += s"Component proposal '${proposal.id}' provider contract version '${proposal.providerContractVersion}' does not match advisory provider '${proposal.providerId}'."
      case Some(_) => ()
      case None =>
        providersbyid.get(proposal.providerId).flatMap(_.headOption) match {
          case Some(AdvisoryProviderContract(_, _, _, UnavailableAdvisoryProviderAvailability(_), _)) =>
            violations += s"Component proposal '${proposal.id}' names unavailable advisory provider '${proposal.providerId}'."
          case _ if proposal.providerId.trim.nonEmpty =>
            violations += s"Component proposal '${proposal.id}' names unknown admitted advisory provider '${proposal.providerId}'."
          case _ => ()
        }
    }

    val linkedevidence = evidence.filter(_.requiredCapabilityIds.contains(proposal.requiredCapabilityId))
    val matchingevidenceids = proposal.consideredEvidenceIds.flatMap { evidenceid =>
      evidencebyid.get(evidenceid).filter { item =>
        item.requiredCapabilityIds.contains(proposal.requiredCapabilityId) && item.componentId.contains(proposal.componentId)
      }.map(_.id)
    }
    if (matchingevidenceids.isEmpty)
      violations += s"Component proposal '${proposal.id}' target component '${proposal.componentId}' is not asserted by its considered linked evidence."

    val result = violations.result()
    if (result.nonEmpty) Left(ComponentProposalAdmissionFailure(proposal, result))
    else
      Right(
        AdmittedComponentProposal(
          proposal,
          provider.get,
          matchingevidenceids,
          linkedevidence.map(_.id),
          linkedevidence.map(_.condition)
        )
      )
  }

  private def _duplicate_human_decision_ids(decisions: Vector[HumanDecision]): Set[String] =
    decisions.groupBy(_.id).collect {
      case (id, values) if id.trim.nonEmpty && values.size > 1 => id
    }.toSet

  private def _duplicate_human_decision_scopes(decisions: Vector[HumanDecision]): Set[(String, String)] =
    decisions.groupBy(decision => (decision.applicationIntentId, decision.requiredCapabilityId)).collect {
      case (scope, values) if scope._1.trim.nonEmpty && scope._2.trim.nonEmpty && values.size > 1 => scope
    }.toSet

  private def _duplicate_advisory_provider_ids(providers: Vector[AdvisoryProviderContract]): Set[String] =
    providers.groupBy(_.id).collect {
      case (id, values) if id.trim.nonEmpty && values.size > 1 => id
    }.toSet

  private def _duplicate_component_proposal_ids(proposals: Vector[ComponentProposal]): Set[String] =
    proposals.groupBy(_.id).collect {
      case (id, values) if id.trim.nonEmpty && values.size > 1 => id
    }.toSet

  private def _classify_coverage(evidence: Vector[ComponentEvidence]): CoverageDispositionOutcome =
    if (evidence.isEmpty)
      GapCoverageDispositionOutcome(NoLinkedEvidenceCoverageGapReason)
    else if (evidence.forall(_.condition.absenceReason.isDefined))
      GapCoverageDispositionOutcome(
        AllEvidenceAbsentCoverageGapReason(evidence.flatMap(_.condition.absenceReason))
      )
    else if (
      evidence.exists(_.condition.redactionReason.isDefined) ||
      evidence.exists(_.condition.limitations.nonEmpty) ||
      evidence.exists(_.componentId.isEmpty) ||
      evidence.exists(_.condition.absenceReason.isDefined) && evidence.exists(_.condition.absenceReason.isEmpty)
    )
      UnresolvedCoverageDispositionOutcome
    else
      AlternativeCoverageDispositionOutcome(evidence.flatMap(_.componentId))

  private def _validate_intent(
    intent: ApplicationIntent,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(intent.id, "application intent ID", violations)
    _require_nonblank(intent.applicationScope, "application intent scope", violations)
    _validate_provenance(intent.provenance, "application intent", violations)
  }

  private def _validate_capabilities(
    intent: ApplicationIntent,
    capabilities: Vector[RequiredCapability],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_duplicate_ids(capabilities.map(_.id), "required capability", violations)
    capabilities.foreach { capability =>
      _require_nonblank(capability.id, "required capability ID", violations)
      _require_nonblank(capability.applicationIntentId, "required capability application intent ID", violations)
      if (capability.applicationIntentId != intent.id)
        violations += s"Required capability '${capability.id}' is outside application intent '${intent.id}'."
      _validate_provenance(capability.provenance, s"required capability '${capability.id}'", violations)
    }
  }

  private def _validate_evidence(
    evidence: Vector[ComponentEvidence],
    capabilityids: Set[String],
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _validate_duplicate_ids(evidence.map(_.id), "component evidence", violations)
    evidence.foreach { item =>
      _require_nonblank(item.id, "component evidence ID", violations)
      _validate_provenance(item.provenance, s"component evidence '${item.id}'", violations)
      item.componentId.foreach(_require_nonblank(_, s"component evidence '${item.id}' component ID", violations))
      if (item.requiredCapabilityIds.isEmpty)
        violations += s"Component evidence '${item.id}' must name at least one required capability."
      _validate_duplicate_ids(item.requiredCapabilityIds, s"component evidence '${item.id}' required capability link", violations)
      item.requiredCapabilityIds.foreach { capabilityid =>
        _require_nonblank(capabilityid, s"component evidence '${item.id}' required capability link", violations)
        if (!capabilityids.contains(capabilityid))
          violations += s"Component evidence '${item.id}' names unknown required capability '$capabilityid'."
      }
    }
  }

  private def _validate_provenance(
    provenance: CompositionProvenance,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(provenance.sourceId, s"$subject source ID", violations)
    _require_nonblank(provenance.authorityScope, s"$subject authority scope", violations)
    _require_nonblank(provenance.sourceLocator, s"$subject source locator", violations)
  }

  private def _validate_duplicate_ids(
    ids: Vector[String],
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    ids.groupBy(identity).collect { case (id, values) if values.size > 1 => id }.toVector.sorted.foreach { id =>
      violations += s"Duplicate $subject ID '$id'."
    }

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
