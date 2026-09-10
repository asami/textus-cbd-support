package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentDashboardContentOverviewSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentDashboardContentOverview" should {
    "P9-21B source-attributed deterministic Content Overview projection" which {
      "retain explicitly assigned records for all eight families with their Dashboard provenance and conditions" in {
        Given("one exact Component Dashboard with eight independently attributed Content records and explicit family assignments")
        val component = _component("textus-order")
        val records = Vector(
          _record("purpose", component, "source-purpose", "purpose statement", targetid = Some("purpose-target"), condition = _condition(redaction = Some("purpose-redaction"))),
          _record("responsibility", component, "source-responsibility", "responsibility statement", targetid = Some("responsibility-target"), condition = _condition(conflict = Some("responsibility-conflict"))),
          _record("domain-summary", component, "source-domain-summary", "domain summary statement", targetid = Some("domain-summary-target"), condition = _condition(ambiguity = Some("domain-summary-ambiguity"))),
          _record("capabilities", component, "source-capabilities", "capabilities statement", targetid = Some("capabilities-target"), condition = _condition(staleness = Some("capabilities-staleness"))),
          _record("entry-points", component, "source-entry-points", "entry points statement", targetid = Some("entry-points-target")),
          _record("rules", component, "source-rules", "rules statement", targetid = Some("rules-target"), condition = _condition(limitations = Vector("rules-limitation"))),
          _record("interfaces-events", component, "source-interfaces-events", "interfaces and events statement", targetid = Some("interfaces-events-target"), condition = _condition(explicitabsence = Some("interfaces-events-bounded-absence"))),
          _record("related-knowledge", component, "source-related-knowledge", "related knowledge statement", targetid = Some("related-knowledge-target"), condition = _condition(malformedevidence = Some("related-knowledge-malformed")))
        )
        val dashboard = _dashboard(component, records)
        val assignments = Vector(
          _assignment("source-purpose", Purpose),
          _assignment("source-responsibility", Responsibility),
          _assignment("source-domain-summary", DomainSummary),
          _assignment("source-capabilities", Capabilities),
          _assignment("source-entry-points", RepresentativeAnalysisModelEntryPoints),
          _assignment("source-rules", Rules),
          _assignment("source-interfaces-events", InterfacesEvents),
          _assignment("source-related-knowledge", RelatedKnowledge)
        )

        When("the explicitly assigned Content families are projected")
        val result = ComponentDashboardContentOverview.create(dashboard, assignments)

        Then("every group retains its original Dashboard record, Component scope, source attribution, statement, and independent condition")
        result.toOption.toVector.flatMap(_.groups.map(_.family)) shouldBe
          ComponentDashboardContentOverviewFamily.presentationSequence
        result.toOption.toVector.flatMap(_.groups.flatMap(_.items).map(_.dashboardRecord.sourceRecord)) shouldBe records
        result.toOption.toVector.flatMap(_.groups.flatMap(_.items).map { item =>
          val source = item.dashboardRecord.sourceRecord
          (source.component, source.sourceRecordId, source.attribution, source.statementRepresentation, source.condition, source.semanticTargetId)
        }) shouldBe records.map { record =>
          (record.component, record.sourceRecordId, record.attribution, record.statementRepresentation, record.condition, record.semanticTargetId)
        }
        result.toOption.toVector.flatMap(_.unassignedContentRecords) shouldBe Vector.empty
      }

      "use fixed family sequence and Dashboard identity-first order independent of Dashboard or assignment iteration" in {
        Given("reverse Dashboard and assignment iteration with two Capability records having opposite target identities")
        val component = _component("textus-order")
        val capabilityzulu = _record("capability-zulu", component, "source-capability-zulu", "zulu capability", targetid = Some("zulu"))
        val capabilityalpha = _record("capability-alpha", component, "source-capability-alpha", "alpha capability", targetid = Some("alpha"))
        val purpose = _record("purpose", component, "source-purpose", "purpose", targetid = Some("purpose"))
        val knowledge = _record("knowledge", component, "source-knowledge", "knowledge", targetid = Some("knowledge"))
        val dashboard = _dashboard(component, Vector(knowledge, purpose, capabilityzulu, capabilityalpha))
        val assignments = Vector(
          _assignment("source-knowledge", RelatedKnowledge),
          _assignment("source-capability-zulu", Capabilities),
          _assignment("source-capability-alpha", Capabilities),
          _assignment("source-purpose", Purpose)
        )

        When("the same admitted Dashboard content is assigned in reverse iteration")
        val result = ComponentDashboardContentOverview.create(dashboard, assignments)

        Then("the eight non-ranking groups stay fixed and Capability items preserve Dashboard target-identity order")
        result.toOption.toVector.flatMap(_.groups.map(_.family)) shouldBe
          Vector(
            Purpose,
            Responsibility,
            DomainSummary,
            Capabilities,
            RepresentativeAnalysisModelEntryPoints,
            Rules,
            InterfacesEvents,
            RelatedKnowledge
          )
        result.toOption.toVector.flatMap { overview =>
          overview.groups
            .find(_.family == Capabilities)
            .toVector
            .flatMap(_.items)
            .flatMap(_.dashboardRecord.sourceRecord.semanticTargetId.map(_.value))
        } shouldBe Vector("alpha", "zulu")
      }

      "leave Content-looking statements unassigned and reject a Usage record as a Content family source" in {
        Given("a Content-looking source statement without family admission and an admitted Usage record")
        val component = _component("textus-order")
        val contentlooking = _record(
          "content-looking",
          component,
          "source-content-looking",
          "Capability: apparent from statement wording only",
          targetid = Some("content-looking-target")
        )
        val usage = _record(
          "usage",
          component,
          "source-usage",
          "usage statement",
          concern = Some(Usage),
          targetid = Some("usage-target")
        )
        val dashboard = _dashboard(component, Vector(usage, contentlooking))

        When("no family is assigned and the Usage record is offered as a Capability assignment")
        val unassignedresult = ComponentDashboardContentOverview.create(dashboard, Vector.empty)
        val usageresult = ComponentDashboardContentOverview.create(
          dashboard,
          Vector(_assignment("source-usage", Capabilities))
        )

        Then("wording does not infer a family, while a non-Content source returns the typed failure without an Overview")
        unassignedresult.toOption.toVector.flatMap(_.groups.flatMap(_.items)) shouldBe Vector.empty
        unassignedresult.toOption.toVector.flatMap(_.unassignedContentRecords.map(_.sourceRecord)) shouldBe Vector(contentlooking)
        usageresult.isLeft shouldBe true
        usageresult.toOption shouldBe None
        usageresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Content Overview assignment source-record identity 'source-usage' is not an admitted Content record."
        )
      }

      "retain only the exact usable navigation target already admitted by ComponentDashboard" in {
        Given("two assigned Content records with one unimplemented target and one exact implemented usable target")
        val component = _component("textus-order")
        val unavailable = _record(
          "entry-point-unavailable",
          component,
          "source-entry-point-unavailable",
          "unavailable entry point",
          targetid = Some("entry-point-alpha"),
          navigation = Some(_navigation("entry-point-alpha", implementedandusable = false))
        )
        val usable = _record(
          "entry-point-usable",
          component,
          "source-entry-point-usable",
          "usable entry point",
          targetid = Some("entry-point-zulu"),
          navigation = Some(_navigation("entry-point-zulu", implementedandusable = true))
        )
        val dashboard = _dashboard(component, Vector(usable, unavailable))

        When("the admitted Content entry points are assigned to their explicit Overview family")
        val result = ComponentDashboardContentOverview.create(
          dashboard,
          Vector(
            _assignment("source-entry-point-unavailable", RepresentativeAnalysisModelEntryPoints),
            _assignment("source-entry-point-usable", RepresentativeAnalysisModelEntryPoints)
          )
        )

        Then("no navigation is rebuilt for the unimplemented target and the usable exact target preserves its Dashboard provenance")
        result.toOption.toVector.flatMap { overview =>
          overview.groups
            .find(_.family == RepresentativeAnalysisModelEntryPoints)
            .toVector
            .flatMap(_.items)
            .map(_.dashboardRecord.navigationTarget)
        } shouldBe Vector(None, Some(_navigation("entry-point-zulu", implementedandusable = true)))
        result.toOption.toVector.flatMap { overview =>
          overview.groups
            .find(_.family == RepresentativeAnalysisModelEntryPoints)
            .toVector
            .flatMap(_.items)
            .flatMap(_.dashboardRecord.navigationTarget)
            .map(target => (target.targetIdentity, target.targetComponent, target.targetAttribution, target.targetCondition))
        } shouldBe Vector(
          (
            ComponentDashboardSemanticTargetIdentity("entry-point-zulu"),
            component,
            ComponentDashboardSourceAttribution("target-source", "target-authority", "target-locator"),
            _condition()
          )
        )
      }

      "return typed failures without a partial Overview for blank unknown and duplicate same-family assignments" in {
        Given("one admitted Content record plus blank, unknown, and duplicate source-record-to-family assignments")
        val component = _component("textus-order")
        val capability = _record("capability", component, "source-capability", "capability", targetid = Some("capability-target"))
        val dashboard = _dashboard(component, Vector(capability))

        When("the invalid explicit assignment identities are projected")
        val result = ComponentDashboardContentOverview.create(
          dashboard,
          Vector(
            _assignment(" ", Purpose),
            _assignment("source-unknown", Responsibility),
            _assignment("source-capability", Capabilities),
            _assignment("source-capability", Capabilities)
          )
        )

        Then("all invalid boundaries return the typed failure and do not expose a partial Overview")
        result.isLeft shouldBe true
        result.toOption shouldBe None
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Content Overview assignment source-record identity must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Content Overview assignment source-record identity 'source-unknown' is not admitted by the supplied Dashboard."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Content Overview assignment for source-record identity 'source-capability' and family 'Capabilities'."
        )
      }
    }
  }

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _assignment(
    sourceid: String,
    family: ComponentDashboardContentOverviewFamily
  ): ComponentDashboardContentOverviewAssignment =
    ComponentDashboardContentOverviewAssignment(sourceid, family)

  private def _dashboard(
    component: ComponentDashboardComponentIdentity,
    records: Vector[ComponentDashboardInventoryRecord]
  ): ComponentDashboard =
    ComponentDashboard.create(component, records).fold(
      failure => throw new IllegalStateException(failure.violations.mkString("; ")),
      identity
    )

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

  private def _navigation(
    targetid: String,
    implementedandusable: Boolean,
    targetcomponent: ComponentDashboardComponentIdentity = ComponentDashboardComponentIdentity("textus-order"),
    targetattribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "target-source",
      "target-authority",
      "target-locator"
    ),
    targetcondition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      targetcomponent,
      targetattribution,
      targetcondition,
      implementedandusable
    )

  private def _record(
    recordid: String,
    component: ComponentDashboardComponentIdentity,
    sourceid: String,
    statement: String,
    concern: Option[ComponentDashboardConcern] = Some(Content),
    targetid: Option[String] = None,
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardInventoryRecord =
    ComponentDashboardInventoryRecord(
      recordid,
      component,
      sourceid,
      ComponentDashboardSourceAttribution(sourceid, "source-authority", s"source:$sourceid"),
      statement,
      condition,
      concern,
      targetid.map(ComponentDashboardSemanticTargetIdentity),
      tiekey,
      navigation
    )
}
