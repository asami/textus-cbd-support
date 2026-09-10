package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentDashboardWebEntrySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentDashboardWebEntry" should {
    "P9-22B pure authorized exact-Component Dashboard Web-entry delivery value" which {
      "retain the exact supplied Dashboard and its existing admitted navigation for an affirmative matching decision" in {
        Given("an affirmative direct Component decision and one already-created matching Dashboard with an admitted target")
        val component = _component("textus-order")
        val navigation = _navigation("entity-order", component)
        val dashboard = _dashboard(component, Vector(_record("content", component, navigation = Some(navigation))))

        When("the authorized exact-Component Dashboard entry is created")
        val result = ComponentDashboardWebEntry.create(
          ComponentDashboardWebEntryAuthorizationAffirmative(component),
          dashboard
        )

        Then("the entry keeps the exact Dashboard instance and its existing navigation unchanged")
        result shouldBe Right(ComponentDashboardWebEntry(dashboard))
        result.toOption.map(_.dashboard eq dashboard) shouldBe Some(true)
        result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).flatMap(_.navigationTarget)) shouldBe Vector(navigation)
      }

      "deny delivery for every missing denied expired or ambiguous decision" in {
        Given("one exact Dashboard and all four non-affirmative authorization decisions")
        val component = _component("textus-order")
        val dashboard = _dashboard(component)
        val decisions: Vector[ComponentDashboardWebEntryAuthorizationDecision] = Vector(
          ComponentDashboardWebEntryAuthorizationMissing,
          ComponentDashboardWebEntryAuthorizationDenied,
          ComponentDashboardWebEntryAuthorizationExpired,
          ComponentDashboardWebEntryAuthorizationAmbiguous
        )

        When("each non-affirmative decision is used to create a Dashboard entry")
        val results = decisions.map(ComponentDashboardWebEntry.create(_, dashboard))

        Then("each result is the typed no-entry failure without a partial Dashboard")
        results.map(_.isLeft) shouldBe Vector(true, true, true, true)
        results.flatMap(_.left.toOption.toVector.flatMap(_.violations)) shouldBe Vector.fill(4)(
          "Component Dashboard Web entry authorization decision must affirm one direct Component identity."
        )
      }

      "deny blank direct identity and blank Dashboard Component identity" in {
        Given("an affirmative decision with a blank direct identity and a Dashboard with a blank Component identity")
        val component = _component("textus-order")
        val dashboard = _dashboard(component)
        val blankdashboard = ComponentDashboard(_component(" "), Vector.empty, Vector.empty)

        When("each blank identity boundary is used to create a Dashboard entry")
        val blankdirectresult = ComponentDashboardWebEntry.create(
          ComponentDashboardWebEntryAuthorizationAffirmative(_component(" ")),
          dashboard
        )
        val blankdashboardresult = ComponentDashboardWebEntry.create(
          ComponentDashboardWebEntryAuthorizationAffirmative(component),
          blankdashboard
        )

        Then("both boundaries return their typed failures without delivery")
        blankdirectresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Component Dashboard Web entry direct Component identity must not be blank."
        )
        blankdashboardresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Component Dashboard Web entry Dashboard Component identity must not be blank."
        )
      }

      "reject a mismatched identity without selecting filtering substituting or creating a near Dashboard" in {
        Given("an affirmative direct Component identity and an already-created Dashboard for a different Component")
        val directcomponent = _component("textus-order")
        val neardashboard = _dashboard(_component("textus-payment"))

        When("the mismatched tuple is used to create a Dashboard entry")
        val result = ComponentDashboardWebEntry.create(
          ComponentDashboardWebEntryAuthorizationAffirmative(directcomponent),
          neardashboard
        )

        Then("the typed failure creates no near or filtered Dashboard entry")
        result.isLeft shouldBe true
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Component Dashboard Web entry direct Component 'textus-order' does not match Dashboard Component 'textus-payment'."
        )
      }

      "preserve attributed records and every independent condition without deriving a target or conclusion" in {
        Given("a matching Dashboard record with complete attribution and independent non-disclosure conditions")
        val component = _component("textus-order")
        val condition = _condition(
          availability = "unavailable",
          authorization = "denied",
          redaction = Some("contract-redaction"),
          explicitabsence = Some("no-published-schema"),
          ambiguity = Some("two-admitted-relations"),
          conflict = Some("attributed-conflict"),
          staleness = Some("freshness-unknown"),
          malformedevidence = Some("source-shape-invalid"),
          limitations = Vector("provider-boundary")
        )
        val record = _record("conditioned-content", component, condition = condition)
        val dashboard = _dashboard(component, Vector(record))

        When("the affirmative exact-Component entry is created")
        val result = ComponentDashboardWebEntry.create(
          ComponentDashboardWebEntryAuthorizationAffirmative(component),
          dashboard
        )

        Then("the immutable pass-through retains the source record attribution and all conditions without a target")
        result.toOption.map(_.dashboard eq dashboard) shouldBe Some(true)
        result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).map(_.sourceRecord)) shouldBe Vector(record)
        result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).map(_.sourceRecord.attribution)) shouldBe
          Vector(record.attribution)
        result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).map(_.sourceRecord.condition)) shouldBe Vector(condition)
        result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).flatMap(_.navigationTarget)) shouldBe Vector.empty
      }
    }
  }

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _dashboard(
    component: ComponentDashboardComponentIdentity,
    records: Vector[ComponentDashboardInventoryRecord] = Vector.empty
  ): ComponentDashboard =
    ComponentDashboard.create(component, records).toOption.get

  private def _record(
    recordid: String,
    component: ComponentDashboardComponentIdentity,
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardInventoryRecord =
    ComponentDashboardInventoryRecord(
      recordid,
      component,
      s"source-$recordid",
      _attribution(s"source-$recordid"),
      "admitted statement",
      condition,
      Some(Content),
      navigation.map(_.targetIdentity),
      None,
      navigation
    )

  private def _attribution(sourceid: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, "source-authority", s"source:$sourceid")

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
    component: ComponentDashboardComponentIdentity
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      ComponentDashboardSourceAttribution("target-source", "target-authority", "target-locator"),
      _condition(),
      implementedAndUsable = true
    )
}
