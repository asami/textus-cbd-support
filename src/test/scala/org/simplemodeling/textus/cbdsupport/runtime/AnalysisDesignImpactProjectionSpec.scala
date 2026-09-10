package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class AnalysisDesignImpactProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "AnalysisDesignImpactProjection" should {
    "P9-60B typed immutable analysis-to-design impact projection" which {
      "retain every caller-admitted origin, impact category, direct link, condition, locator, and limitation without deriving an association" in {
        Given("one exact Component and CCDM context with all admitted origin and impact vocabularies")
        val context = _context()
        val component = _component()
        val proposals = Vector(
          _proposal("proposal-mono-subject", context, component, "mono-order", AnalysisDesignMonoKotoOrigin, AnalysisDesignMonoKotoSubject),
          _proposal("proposal-mono-reference", context, component, "mono-order-ref", AnalysisDesignMonoKotoOrigin, AnalysisDesignMonoKotoReference),
          _proposal("proposal-use-case-subject", context, component, "use-case-order", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseSubject),
          _proposal("proposal-use-case-reference", context, component, "use-case-reference", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseReference),
          _proposal("proposal-use-case-flow", context, component, "use-case-flow", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseFlow),
          _proposal("proposal-use-case-flow-step", context, component, "use-case-flow-step", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseFlowStep)
        )
        val records = Vector(
          _record("entity-subject", context, component, Some("entity-order"), AnalysisDesignImpactEntity, AnalysisDesignImpactEntitySubject),
          _record("entity-metadata", context, component, Some("entity-order-metadata"), AnalysisDesignImpactEntity, AnalysisDesignImpactEntityMetadata),
          _record("event-subject", context, component, Some("event-order"), AnalysisDesignImpactEvent, AnalysisDesignImpactEventSubject),
          _record("event-assertion", context, component, Some("event-order-assertion"), AnalysisDesignImpactEvent, AnalysisDesignImpactEventAssertion),
          _record("event-endpoint", context, component, Some("event-order-endpoint"), AnalysisDesignImpactEvent, AnalysisDesignImpactEventEndpoint),
          _record("structure-relation", context, component, Some("structure-order"), AnalysisDesignImpactStructure, AnalysisDesignImpactStructureRelation),
          _record("structure-assertion", context, component, Some("structure-order-assertion"), AnalysisDesignImpactStructure, AnalysisDesignImpactStructureAssertion),
          _record("workflow-subject", context, component, Some("workflow-order"), AnalysisDesignImpactWorkflow, AnalysisDesignImpactWorkflowSubject),
          _record("workflow-flow", context, component, Some("workflow-order-flow"), AnalysisDesignImpactWorkflow, AnalysisDesignImpactWorkflowFlow),
          _record("workflow-endpoint", context, component, Some("workflow-order-endpoint"), AnalysisDesignImpactWorkflow, AnalysisDesignImpactWorkflowEndpoint),
          _record("state-subject", context, component, Some("state-order"), AnalysisDesignImpactStateMachine, AnalysisDesignImpactStateMachineSubject),
          _record("state-transition", context, component, Some("state-order-transition"), AnalysisDesignImpactStateMachine, AnalysisDesignImpactStateMachineTransition),
          _record("state-adjunct", context, component, Some("state-order-adjunct"), AnalysisDesignImpactStateMachine, AnalysisDesignImpactStateMachineTransitionAdjunct),
          _record("state-endpoint", context, component, Some("state-order-endpoint"), AnalysisDesignImpactStateMachine, AnalysisDesignImpactStateMachineEndpoint),
          _record("canonical-location", context, component, None, AnalysisDesignImpactCanonicalSourceLocation, AnalysisDesignImpactCanonicalSourceLocationTrace)
        )
        val links = Vector(
          _link("link-entity", context, component, proposals(0).id, records(0).id),
          _link("link-event", context, component, proposals(1).id, records(2).id),
          _link("link-structure", context, component, proposals(2).id, records(5).id),
          _link("link-workflow", context, component, proposals(3).id, records(7).id),
          _link("link-state-machine", context, component, proposals(4).id, records(10).id),
          _link("link-canonical-location", context, component, proposals(5).id, records(14).id)
        )

        When("only the caller-admitted proposal, record, and relationship values are projected")
        val result = AnalysisDesignImpactProjection.create(context, component, proposals, records, links, Vector.empty)

        Then("every closed vocabulary and direct attribution is retained without any inferred association")
        result.isRight shouldBe true
        val projection = result.toOption.get
        projection.proposals.map(value => value.sourceProposal.originCategory -> value.sourceProposal.originRole).toSet shouldBe
          AnalysisDesignImpactOriginCategory.presentationSequence.flatMap { category =>
            AnalysisDesignImpactOriginRole.presentationSequence(category).map(role => category -> role)
          }.toSet
        projection.records.map(value => value.sourceRecord.category -> value.sourceRecord.role).toSet shouldBe
          AnalysisDesignImpactCategory.presentationSequence.flatMap { category =>
            AnalysisDesignImpactRole.presentationSequence(category).map(role => category -> role)
          }.toSet
        projection.links.map(_.sourceLink.impactRecordId).toSet shouldBe links.map(_.impactRecordId).toSet
        projection.links.map(_.impactRecord.category).toSet shouldBe AnalysisDesignImpactCategory.presentationSequence.toSet
        projection.links.foreach { link =>
          link.sourceLink.attribution.sourceLocator shouldBe s"link:${link.sourceLink.id}"
          link.sourceLink.limitations shouldBe Vector("caller-admitted limitation")
          link.sourceLink.condition shouldBe _condition()
        }
        projection.records.find(_.sourceRecord.id == "canonical-location").flatMap(_.sourceRecord.semanticTargetId) shouldBe None
      }

      "retain an explicit bounded gap without converting it into a no-impact conclusion or a synthetic link" in {
        Given("one proposal with an explicitly linked Entity record and an unlinked Event record in the same exact scope")
        val context = _context()
        val component = _component()
        val proposal = _proposal("proposal-gap", context, component, "origin-gap", AnalysisDesignMonoKotoOrigin, AnalysisDesignMonoKotoSubject)
        val entity = _record("impact-entity", context, component, Some("entity-gap"), AnalysisDesignImpactEntity, AnalysisDesignImpactEntitySubject)
        val event = _record("impact-event", context, component, Some("event-gap"), AnalysisDesignImpactEvent, AnalysisDesignImpactEventSubject)
        val link = _link("link-entity", context, component, proposal.id, entity.id)
        val gap = _gap("gap-event", context, component, proposal.id, AnalysisDesignImpactEvent)

        When("the caller admits only the Entity association and an Event bounded gap")
        val result = AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity, event), Vector(link), Vector(gap))

        Then("the projection retains exactly the direct association and the bounded gap")
        result.toOption.toVector.flatMap(_.links.map(_.sourceLink.impactRecordId)) shouldBe Vector(entity.id)
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.requestedCategory)) shouldBe Vector(AnalysisDesignImpactEvent)
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.limitationReason)) shouldBe Vector("No caller-admitted Event impact association exists in this bounded scope.")
      }

      "order all retained values deterministically from identities and admitted tie keys under ScalaCheck caller permutations" in {
        Given("two independent proposals, records, links, and gaps whose input order carries no semantic authority")
        val context = _context()
        val component = _component()
        val proposals = Vector(
          _proposal("proposal-zulu", context, component, "origin-zulu", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseFlow, stabletiekey = Some("z")),
          _proposal("proposal-alpha", context, component, "origin-alpha", AnalysisDesignMonoKotoOrigin, AnalysisDesignMonoKotoSubject, stabletiekey = Some("a"))
        )
        val records = Vector(
          _record("record-zulu", context, component, Some("target-zulu"), AnalysisDesignImpactWorkflow, AnalysisDesignImpactWorkflowFlow, stabletiekey = Some("z")),
          _record("record-alpha", context, component, Some("target-alpha"), AnalysisDesignImpactEntity, AnalysisDesignImpactEntitySubject, stabletiekey = Some("a")),
          _record("record-locator", context, component, None, AnalysisDesignImpactCanonicalSourceLocation, AnalysisDesignImpactCanonicalSourceLocationTrace, stabletiekey = Some("l"))
        )
        val links = Vector(
          _link("link-zulu", context, component, proposals(0).id, records(0).id, stabletiekey = Some("z")),
          _link("link-alpha", context, component, proposals(1).id, records(1).id, stabletiekey = Some("a"))
        )
        val gaps = Vector(
          _gap("gap-zulu", context, component, proposals(0).id, AnalysisDesignImpactStateMachine, stabletiekey = Some("z")),
          _gap("gap-alpha", context, component, proposals(1).id, AnalysisDesignImpactEvent, stabletiekey = Some("a"))
        )

        When("ScalaCheck generates independent caller permutations")
        forAll(Gen.pick(proposals.size, proposals), Gen.pick(records.size, records), Gen.pick(links.size, links), Gen.pick(gaps.size, gaps)) {
          (proposalpermutation, recordpermutation, linkpermutation, gappermutation) =>
            val result = AnalysisDesignImpactProjection.create(
              context,
              component,
              proposalpermutation.toVector,
              recordpermutation.toVector,
              linkpermutation.toVector,
              gappermutation.toVector
            )

            Then("only exact retained identities, closed vocabularies, and admitted tie keys determine presentation order")
            result.toOption.toVector.flatMap(_.proposals.map(_.sourceProposal.id)) shouldBe Vector("proposal-alpha", "proposal-zulu")
            result.toOption.toVector.flatMap(_.records.map(_.sourceRecord.id)) shouldBe Vector("record-locator", "record-alpha", "record-zulu")
            result.toOption.toVector.flatMap(_.links.map(_.sourceLink.id)) shouldBe Vector("link-alpha", "link-zulu")
            result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe Vector("gap-alpha", "gap-zulu")
        }
      }

      "accumulate typed failures for malformed, incompatible, duplicate, unresolved, or partially admitted input without returning a projection" in {
        Given("otherwise admissible values for one exact Component and CCDM context")
        val context = _context()
        val component = _component()
        val proposal = _proposal("proposal", context, component, "origin", AnalysisDesignMonoKotoOrigin, AnalysisDesignMonoKotoSubject)
        val entity = _record("entity", context, component, Some("entity"), AnalysisDesignImpactEntity, AnalysisDesignImpactEntitySubject)
        val link = _link("link", context, component, proposal.id, entity.id)
        val gap = _gap("gap", context, component, proposal.id, AnalysisDesignImpactEvent)
        val invalidresults = Vector(
          AnalysisDesignImpactProjection.create(MonoKotoProjectionContextIdentity(" "), component, Vector(proposal), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, ComponentDashboardComponentIdentity(" "), Vector(proposal), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(context = MonoKotoProjectionContextIdentity("other"))), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(originRecordId = " ")), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(originRole = AnalysisDesignUseCaseFlow)), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(attribution = proposal.attribution.copy(sourceLocator = " "))), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(condition = _condition(availability = " "))), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(limitations = Vector.empty)), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(limitations = Vector(" "))), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal.copy(stableTieKey = Some(" "))), Vector(entity), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity.copy(semanticTargetId = None)), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity.copy(role = AnalysisDesignImpactEventSubject)), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity.copy(id = proposal.id)), Vector.empty, Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link.copy(proposalId = "missing")), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link.copy(impactRecordId = "missing")), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link.copy(semanticRelationshipId = ComponentDashboardSemanticTargetIdentity(" "))), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link.copy(limitations = Vector.empty)), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link.copy(limitations = Vector(" "))), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link, link.copy(id = "link-second")), Vector.empty),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector(link), Vector(gap.copy(requestedCategory = AnalysisDesignImpactEntity))),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector.empty, Vector(gap.copy(proposalId = "missing"))),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector.empty, Vector(gap.copy(attribution = gap.attribution.copy(sourceLocator = " ")))),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector.empty, Vector(gap.copy(condition = _condition(limitations = Vector(" "))))),
          AnalysisDesignImpactProjection.create(context, component, Vector(proposal), Vector(entity), Vector.empty, Vector(gap.copy(limitationReason = " ")))
        )

        When("each malformed or unresolved admission is projected")
        val failures = invalidresults.flatMap(_.left.toOption)

        Then("every result is a typed all-or-nothing failure with no partially projected value")
        failures should have size invalidresults.size
        failures.flatMap(_.violations) should not be empty
        invalidresults.flatMap(_.toOption) shouldBe Vector.empty
      }
    }
  }

  private def _context(): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity("ccdm-analysis-design-impact")

  private def _component(): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity("textus-analysis-design-impact")

  private def _condition(
    availability: String = "available",
    authorization: String = "admitted",
    redaction: Option[String] = None,
    explicitabsence: Option[String] = None,
    ambiguity: Option[String] = None,
    conflict: Option[String] = None,
    staleness: Option[String] = None,
    malformedevidence: Option[String] = None,
    limitations: Vector[String] = Vector.empty
  ): ComponentDashboardCondition =
    ComponentDashboardCondition(
      availability,
      authorization,
      redaction,
      explicitabsence,
      ambiguity,
      conflict,
      staleness,
      malformedevidence,
      limitations
    )

  private def _proposal(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    originsemanticid: String,
    origincategory: AnalysisDesignImpactOriginCategory,
    originrole: AnalysisDesignImpactOriginRole,
    stabletiekey: Option[String] = None
  ): AnalysisDesignImpactProposal =
    AnalysisDesignImpactProposal(
      id,
      context,
      component,
      ComponentDashboardSemanticTargetIdentity(originsemanticid),
      s"origin-record:$id",
      origincategory,
      originrole,
      ComponentDashboardSourceAttribution(s"proposal-source:$id", "caller-admitted", s"proposal:$id"),
      _condition(),
      "stakeholder",
      s"Caller-admitted proposal $id.",
      Vector("caller-admitted limitation"),
      stabletiekey
    )

  private def _record(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    semanticid: Option[String],
    category: AnalysisDesignImpactCategory,
    role: AnalysisDesignImpactRole,
    stabletiekey: Option[String] = None
  ): AnalysisDesignImpactRecord =
    AnalysisDesignImpactRecord(
      id,
      context,
      component,
      semanticid.map(ComponentDashboardSemanticTargetIdentity.apply),
      category,
      role,
      ComponentDashboardSourceAttribution(s"record-source:$id", "caller-admitted", s"record:$id"),
      _condition(),
      stabletiekey
    )

  private def _link(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    proposalid: String,
    recordid: String,
    stabletiekey: Option[String] = None
  ): AnalysisDesignImpactLink =
    AnalysisDesignImpactLink(
      id,
      context,
      component,
      proposalid,
      recordid,
      ComponentDashboardSemanticTargetIdentity(s"relationship:$id"),
      ComponentDashboardSourceAttribution(s"link-source:$id", "caller-admitted", s"link:$id"),
      _condition(),
      Vector("caller-admitted limitation"),
      stabletiekey
    )

  private def _gap(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    proposalid: String,
    category: AnalysisDesignImpactCategory,
    stabletiekey: Option[String] = None
  ): AnalysisDesignImpactGap =
    AnalysisDesignImpactGap(
      id,
      context,
      component,
      proposalid,
      category,
      ComponentDashboardSourceAttribution(s"gap-source:$id", "caller-admitted", s"gap:$id"),
      _condition(),
      "No caller-admitted Event impact association exists in this bounded scope.",
      stabletiekey
    )
}
