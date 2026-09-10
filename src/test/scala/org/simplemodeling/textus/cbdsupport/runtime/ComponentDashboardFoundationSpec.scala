package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentDashboardFoundationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentDashboard foundation" should {
    "P9-23A cross-boundary Dashboard guarantees" which {
      "retain deterministic concern and Content order with exact attribution and Component identity" in {
        Given("the same reverse-ordered admitted inventory twice, with explicit Content-family assignments for one exact Component")
        val component = _component("textus-order")
        val contentzulu = _record(
          "content-zulu",
          component,
          "source-content-zulu",
          "zulu capability",
          targetid = Some("zulu-content")
        )
        val contentalpha = _record(
          "content-alpha",
          component,
          "source-content-alpha",
          "alpha purpose",
          targetid = Some("alpha-content")
        )
        val usage = _record("usage", component, "source-usage", "usage statement", concern = Some(Usage))
        val operation = _record("operation", component, "source-operation", "operation statement", concern = Some(Operation))
        val quality = _record("quality", component, "source-quality", "quality statement", concern = Some(QualityReview))
        val reverseinventory = Vector(quality, operation, usage, contentzulu, contentalpha)
        val assignments = Vector(
          _assignment("source-content-zulu", Capabilities),
          _assignment("source-content-alpha", Purpose)
        )

        When("both inventories are projected, explicitly grouped as Content Overview, and delivered through one matching affirmative entry")
        val dashboardresults = Vector(
          ComponentDashboard.create(component, reverseinventory),
          ComponentDashboard.create(component, reverseinventory)
        )
        val overviewresults = dashboardresults.headOption.toVector.flatMap(_.toOption).map { dashboard =>
          ComponentDashboardContentOverview.create(dashboard, assignments)
        }
        val entrysameidentity = dashboardresults.headOption.toVector.flatMap(_.toOption).map { dashboard =>
          ComponentDashboardWebEntry.create(
            ComponentDashboardWebEntryAuthorizationAffirmative(component),
            dashboard
          ).toOption.map(_.dashboard eq dashboard)
        }

        Then("fixed concern order, identity-first Content order, attribution, Component scope, and matching delivery identity survive without input-order inference")
        dashboardresults.map(_.toOption.toVector.flatMap(_.concernGroups.map(_.concern))) shouldBe
          Vector.fill(2)(Vector(Content, Usage, Operation, QualityReview))
        dashboardresults.map(_.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups
            .find(_.concern == Content)
            .toVector
            .flatMap(_.records)
            .flatMap(_.sourceRecord.semanticTargetId.map(_.value))
        }) shouldBe Vector.fill(2)(Vector("alpha-content", "zulu-content"))
        overviewresults.toVector.flatMap(_.toOption).flatMap(_.groups.flatMap(_.items).map { item =>
          val record = item.dashboardRecord.sourceRecord
          (record.component, record.sourceRecordId, record.attribution)
        }) shouldBe Vector(
          (component, "source-content-alpha", contentalpha.attribution),
          (component, "source-content-zulu", contentzulu.attribution)
        )
        entrysameidentity shouldBe Vector(Some(true))
      }

      "keep unavailable unauthorized redacted and explicitly absent Content conditions distinct without navigation" in {
        Given("four independently conditioned Content records, each carrying an otherwise exact implemented target")
        val component = _component("textus-order")
        val unavailable = _record(
          "content-unavailable",
          component,
          "source-unavailable",
          "unavailable content",
          targetid = Some("target-unavailable"),
          navigation = Some(_navigation("target-unavailable", component)),
          condition = _condition(availability = "unavailable")
        )
        val unauthorized = _record(
          "content-unauthorized",
          component,
          "source-unauthorized",
          "unauthorized content",
          targetid = Some("target-unauthorized"),
          navigation = Some(_navigation("target-unauthorized", component)),
          condition = _condition(authorization = "denied")
        )
        val redacted = _record(
          "content-redacted",
          component,
          "source-redacted",
          "redacted content",
          targetid = Some("target-redacted"),
          navigation = Some(_navigation("target-redacted", component)),
          condition = _condition(redaction = Some("contract-redaction"))
        )
        val explicitlyabsent = _record(
          "content-explicitly-absent",
          component,
          "source-explicitly-absent",
          "explicitly absent content",
          targetid = Some("target-explicitly-absent"),
          navigation = Some(_navigation("target-explicitly-absent", component)),
          condition = _condition(explicitabsence = Some("no-published-content"))
        )

        When("the distinct condition records are projected as one exact Component Dashboard")
        val result = ComponentDashboard.create(component, Vector(unauthorized, unavailable, redacted, explicitlyabsent))

        Then("their original condition values remain distinct, explicit absence is not reused, and no target is admitted")
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups
            .find(_.concern == Content)
            .toVector
            .flatMap(_.records)
            .map(_.sourceRecord.condition)
        } shouldBe Vector(
          explicitlyabsent.condition,
          redacted.condition,
          unauthorized.condition,
          unavailable.condition
        )
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups
            .find(_.concern == Content)
            .toVector
            .flatMap(_.records)
            .map(_.sourceRecord.condition.explicitAbsence)
        } shouldBe Vector(Some("no-published-content"), None, None, None)
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups
            .find(_.concern == Content)
            .toVector
            .flatMap(_.records)
            .map(_.navigationTarget)
        } shouldBe Vector(None, None, None, None)
      }

      "retain one redacted record and its exact permitted provenance through Content Overview and matching Web delivery" in {
        Given("one redacted Content record with a nonblank admitted source attribution, locator, and explicit family assignment")
        val component = _component("textus-order")
        val attribution = ComponentDashboardSourceAttribution(
          "catalog-record-redacted",
          "catalog-content-contract",
          "catalog:component/textus-order/content/redacted"
        )
        val condition = _condition(redaction = Some("contract-redaction"))
        val record = _record(
          "content-redacted",
          component,
          "source-redacted",
          "withheld statement representation",
          attribution = Some(attribution),
          targetid = Some("content-redacted-target"),
          condition = condition
        )
        val assignments = Vector(_assignment("source-redacted", RelatedKnowledge))

        When("the record is admitted to Content Overview and its matching affirmative Dashboard Web entry")
        val dashboardresult = ComponentDashboard.create(component, Vector(record))
        val overviewresults = dashboardresult.toOption.toVector.map { dashboard =>
          ComponentDashboardContentOverview.create(dashboard, assignments)
        }
        val entryresults = dashboardresult.toOption.toVector.map { dashboard =>
          ComponentDashboardWebEntry.create(
            ComponentDashboardWebEntryAuthorizationAffirmative(component),
            dashboard
          )
        }

        Then("both representations retain the original record, exact attribution and locator, and redaction condition without a substitute")
        overviewresults.toVector.flatMap(_.toOption).flatMap(_.groups.flatMap(_.items).map(_.dashboardRecord.sourceRecord)) shouldBe
          Vector(record)
        overviewresults.toVector.flatMap(_.toOption).flatMap(_.groups.flatMap(_.items).map { item =>
          val source = item.dashboardRecord.sourceRecord
          (source.attribution, source.attribution.sourceLocator, source.condition)
        }) shouldBe Vector((attribution, "catalog:component/textus-order/content/redacted", condition))
        entryresults.toVector.flatMap(_.toOption).flatMap { entry =>
          entry.dashboard.concernGroups.flatMap(_.records).map(_.sourceRecord)
        } shouldBe Vector(record)
        entryresults.toVector.flatMap(_.toOption).flatMap { entry =>
          entry.dashboard.concernGroups.flatMap(_.records).map { projected =>
            val source = projected.sourceRecord
            (source.attribution, source.attribution.sourceLocator, source.condition)
          }
        } shouldBe Vector((attribution, "catalog:component/textus-order/content/redacted", condition))
      }

      "return typed no-entry failures for all insufficient blank and mismatching authorization tuples" in {
        Given("one existing exact Dashboard, every non-affirmative decision, blank identity boundaries, and one mismatching direct identity")
        val component = _component("textus-order")
        val blankcomponent = _component(" ")
        val dashboardresult = ComponentDashboard.create(component, Vector.empty)
        val blankdashboard = ComponentDashboard(blankcomponent, Vector.empty, Vector.empty)
        val decisions: Vector[ComponentDashboardWebEntryAuthorizationDecision] = Vector(
          ComponentDashboardWebEntryAuthorizationMissing,
          ComponentDashboardWebEntryAuthorizationDenied,
          ComponentDashboardWebEntryAuthorizationExpired,
          ComponentDashboardWebEntryAuthorizationAmbiguous
        )

        When("each insufficient tuple is submitted without deriving or substituting a Dashboard")
        val results = dashboardresult.toOption.toVector.flatMap { dashboard =>
          decisions.map(ComponentDashboardWebEntry.create(_, dashboard)) ++ Vector(
            ComponentDashboardWebEntry.create(
              ComponentDashboardWebEntryAuthorizationAffirmative(blankcomponent),
              dashboard
            ),
            ComponentDashboardWebEntry.create(
              ComponentDashboardWebEntryAuthorizationAffirmative(component),
              blankdashboard
            ),
            ComponentDashboardWebEntry.create(
              ComponentDashboardWebEntryAuthorizationAffirmative(_component("textus-payment")),
              dashboard
            )
          )
        }

        Then("every decision returns its typed failure and no entry is exposed")
        results.map(_.isLeft) shouldBe Vector.fill(7)(true)
        results.flatMap(_.toOption) shouldBe Vector.empty
        results.flatMap(_.left.toOption.toVector.flatMap(_.violations)) should contain(
          "Component Dashboard Web entry authorization decision must affirm one direct Component identity."
        )
        results.flatMap(_.left.toOption.toVector.flatMap(_.violations)) should contain(
          "Component Dashboard Web entry direct Component identity must not be blank."
        )
        results.flatMap(_.left.toOption.toVector.flatMap(_.violations)) should contain(
          "Component Dashboard Web entry Dashboard Component identity must not be blank."
        )
        results.flatMap(_.left.toOption.toVector.flatMap(_.violations)) should contain(
          "Component Dashboard Web entry direct Component 'textus-payment' does not match Dashboard Component 'textus-order'."
        )
      }

      "pass through the exact matching Dashboard and usable target while retaining unavailable targets as None" in {
        Given("one exact Dashboard with an implemented usable target, an unimplemented target, and a condition-disqualified target")
        val component = _component("textus-order")
        val usabletarget = _navigation("target-usable", component, implementedandusable = true)
        val unimplementedtarget = _navigation("target-unimplemented", component, implementedandusable = false)
        val disqualifiedtarget = _navigation(
          "target-disqualified",
          component,
          implementedandusable = true,
          targetcondition = _condition(authorization = "denied")
        )
        val usable = _record(
          "content-usable",
          component,
          "source-usable",
          "usable entry point",
          targetid = Some("target-usable"),
          navigation = Some(usabletarget)
        )
        val unimplemented = _record(
          "content-unimplemented",
          component,
          "source-unimplemented",
          "unimplemented entry point",
          targetid = Some("target-unimplemented"),
          navigation = Some(unimplementedtarget)
        )
        val disqualified = _record(
          "content-disqualified",
          component,
          "source-disqualified",
          "condition-disqualified entry point",
          targetid = Some("target-disqualified"),
          navigation = Some(disqualifiedtarget)
        )

        When("one matching affirmative decision delivers the existing Dashboard")
        val dashboardresult = ComponentDashboard.create(component, Vector(usable, unimplemented, disqualified))
        val entryresults = dashboardresult.toOption.toVector.map { dashboard =>
          (
            dashboard,
            ComponentDashboardWebEntry.create(
              ComponentDashboardWebEntryAuthorizationAffirmative(component),
              dashboard
            )
          )
        }

        Then("the entry has the exact Dashboard reference and Component identity, passes the admitted target identity through, and leaves the other targets unavailable")
        entryresults.map { case (dashboard, result) =>
          result.toOption.map(_.dashboard eq dashboard)
        } shouldBe Vector(Some(true))
        entryresults.flatMap { case (_, result) =>
          result.toOption.toVector.map(_.dashboard.component)
        } shouldBe Vector(component)
        entryresults.flatMap { case (_, result) =>
          result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).map(_.navigationTarget))
        } shouldBe Vector(None, None, Some(usabletarget))
        entryresults.flatMap { case (_, result) =>
          result.toOption.toVector.flatMap(_.dashboard.concernGroups.flatMap(_.records).flatMap(_.navigationTarget).map(_.targetIdentity))
        } shouldBe Vector(usabletarget.targetIdentity)
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
    component: ComponentDashboardComponentIdentity,
    implementedandusable: Boolean = true,
    targetattribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "target-source",
      "target-authority",
      "target-locator"
    ),
    targetcondition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      targetattribution,
      targetcondition,
      implementedandusable
    )

  private def _record(
    recordid: String,
    component: ComponentDashboardComponentIdentity,
    sourceid: String,
    statement: String,
    attribution: Option[ComponentDashboardSourceAttribution] = None,
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
      attribution.getOrElse(
        ComponentDashboardSourceAttribution(
          sourceid,
          "source-authority",
          s"source:$sourceid"
        )
      ),
      statement,
      condition,
      concern,
      targetid.map(ComponentDashboardSemanticTargetIdentity.apply),
      tiekey,
      navigation
    )
}
