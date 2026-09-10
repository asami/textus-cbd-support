package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CompositionDashboardCandidateIdentity(value: String)

final case class CompositionDashboardAssociationIdentity(value: String)

final case class CompositionDashboardAssociationLocator(value: String)

final case class CompositionDashboardNavigationCandidate(
  candidateIdentity: CompositionDashboardCandidateIdentity,
  intent: ApplicationIntent,
  requiredCapability: RequiredCapability,
  component: ComponentDashboardComponentIdentity,
  provenance: CompositionProvenance,
  attribution: ComponentDashboardSourceAttribution,
  condition: ComponentDashboardCondition,
  limitations: Vector[String],
  presentationTieKey: Option[String]
)

final case class CompositionDashboardNavigationAssociation(
  associationIdentity: CompositionDashboardAssociationIdentity,
  candidateIdentity: CompositionDashboardCandidateIdentity,
  component: ComponentDashboardComponentIdentity,
  attribution: ComponentDashboardSourceAttribution,
  locator: CompositionDashboardAssociationLocator,
  condition: ComponentDashboardCondition,
  limitations: Vector[String]
)

final case class CompositionDashboardNavigationLink(
  candidate: CompositionDashboardNavigationCandidate,
  association: CompositionDashboardNavigationAssociation,
  dashboard: ComponentDashboard
) {
  def usableNavigationRecords: Vector[ComponentDashboardProjectedRecord] =
    CompositionDashboardCrossSurfaceNavigation
      .dashboardRecords(dashboard)
      .filter(_.navigationTarget.isDefined)
}

final case class CompositionDashboardNavigationFailure(violations: Vector[String])

sealed trait CompositionDashboardNavigationAdmission

final case class CompositionDashboardNavigationLinkProjected(
  link: CompositionDashboardNavigationLink
) extends CompositionDashboardNavigationAdmission

final case class CompositionDashboardNavigationLinkRejected(
  failure: CompositionDashboardNavigationFailure
) extends CompositionDashboardNavigationAdmission

object CompositionDashboardCrossSurfaceNavigation {
  def create(
    candidate: CompositionDashboardNavigationCandidate,
    association: CompositionDashboardNavigationAssociation,
    dashboard: ComponentDashboard
  ): CompositionDashboardNavigationAdmission = {
    val violations = Vector.newBuilder[String]

    _validate_candidate(candidate, violations)
    _validate_association(association, violations)
    _validate_dashboard(dashboard, violations)
    _validate_exact_binding(candidate, association, dashboard, violations)
    _validate_zero_target_condition(association, dashboard, violations)

    val result = violations.result()
    if (result.nonEmpty)
      CompositionDashboardNavigationLinkRejected(CompositionDashboardNavigationFailure(result))
    else
      CompositionDashboardNavigationLinkProjected(CompositionDashboardNavigationLink(candidate, association, dashboard))
  }

  def dashboardRecords(dashboard: ComponentDashboard): Vector[ComponentDashboardProjectedRecord] =
    dashboard.concernGroups.flatMap(_.records) ++ dashboard.unclassifiedRecords

  private def _validate_candidate(
    candidate: CompositionDashboardNavigationCandidate,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(candidate.candidateIdentity.value, "Composition Dashboard candidate identity", violations)
    _require_nonblank(candidate.intent.id, "Composition Dashboard candidate application intent identity", violations)
    _require_nonblank(candidate.intent.applicationScope, "Composition Dashboard candidate application scope", violations)
    _validate_composition_provenance(candidate.intent.provenance, "Composition Dashboard candidate application intent", violations)
    _require_nonblank(candidate.requiredCapability.id, "Composition Dashboard candidate required capability identity", violations)
    _require_nonblank(candidate.requiredCapability.applicationIntentId, "Composition Dashboard candidate required capability application intent identity", violations)
    _validate_composition_provenance(candidate.requiredCapability.provenance, "Composition Dashboard candidate required capability", violations)
    if (candidate.requiredCapability.applicationIntentId != candidate.intent.id)
      violations += "Composition Dashboard candidate required capability must remain within its exact application intent scope."
    _validate_component_identity(candidate.component, "Composition Dashboard candidate Component identity", violations)
    _validate_composition_provenance(candidate.provenance, "Composition Dashboard candidate", violations)
    _validate_attribution(candidate.attribution, "Composition Dashboard candidate", violations)
    _validate_condition(candidate.condition, "Composition Dashboard candidate", violations)
    _validate_limitations(candidate.limitations, "Composition Dashboard candidate", violations)
    candidate.presentationTieKey.foreach(_require_nonblank(_, "Composition Dashboard candidate presentation tie key", violations))
  }

  private def _validate_association(
    association: CompositionDashboardNavigationAssociation,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(association.associationIdentity.value, "Composition Dashboard association identity", violations)
    _require_nonblank(association.candidateIdentity.value, "Composition Dashboard association candidate identity", violations)
    _validate_component_identity(association.component, "Composition Dashboard association Component identity", violations)
    _validate_attribution(association.attribution, "Composition Dashboard association", violations)
    _require_nonblank(association.locator.value, "Composition Dashboard association locator", violations)
    _validate_condition(association.condition, "Composition Dashboard association", violations)
    _validate_limitations(association.limitations, "Composition Dashboard association", violations)
    if (association.condition.availability != "available")
      violations += "Composition Dashboard association availability must be available."
    if (association.condition.authorization != "admitted")
      violations += "Composition Dashboard association authorization must be admitted."
  }

  private def _validate_dashboard(
    dashboard: ComponentDashboard,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _validate_component_identity(dashboard.component, "Composition Dashboard entry Component identity", violations)

  private def _validate_exact_binding(
    candidate: CompositionDashboardNavigationCandidate,
    association: CompositionDashboardNavigationAssociation,
    dashboard: ComponentDashboard,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    if (association.candidateIdentity != candidate.candidateIdentity)
      violations += "Composition Dashboard association must retain the exact caller-supplied candidate identity."
    if (association.associationIdentity.value == candidate.candidateIdentity.value)
      violations += "Composition Dashboard candidate and association identities must not be duplicated."
    if (association.component != candidate.component)
      violations += "Composition Dashboard association Component identity must equal the exact candidate Component identity."
    if (dashboard.component != candidate.component)
      violations += "Composition Dashboard entry Component identity must equal the exact candidate Component identity."
    if (dashboard.component != association.component)
      violations += "Composition Dashboard entry Component identity must equal the exact association Component identity."
  }

  private def _validate_zero_target_condition(
    association: CompositionDashboardNavigationAssociation,
    dashboard: ComponentDashboard,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (dashboardRecords(dashboard).forall(_.navigationTarget.isEmpty) && association.condition.explicitAbsence.isEmpty)
      violations += "Composition Dashboard association with zero usable Dashboard targets requires an explicit zero-target condition."

  private def _validate_component_identity(
    component: ComponentDashboardComponentIdentity,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    _require_nonblank(component.value, subject, violations)

  private def _validate_composition_provenance(
    provenance: CompositionProvenance,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(provenance.sourceId, s"$subject provenance source identity", violations)
    _require_nonblank(provenance.authorityScope, s"$subject provenance authority scope", violations)
    _require_nonblank(provenance.sourceLocator, s"$subject provenance source locator", violations)
  }

  private def _validate_attribution(
    attribution: ComponentDashboardSourceAttribution,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit = {
    _require_nonblank(attribution.sourceId, s"$subject attribution source identity", violations)
    _require_nonblank(attribution.authorityScope, s"$subject attribution authority scope", violations)
    _require_nonblank(attribution.sourceLocator, s"$subject attribution source locator", violations)
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

  private def _require_nonblank(
    value: String,
    subject: String,
    violations: scala.collection.mutable.Builder[String, Vector[String]]
  ): Unit =
    if (value.trim.isEmpty)
      violations += s"$subject must not be blank."
}
