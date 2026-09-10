package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentDashboardProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentDashboard" should {
    "P9-20B pure deterministic ComponentDashboard projection foundation" which {
      "retain all four concerns with their independent provenance and condition values" in {
        Given("one exact Component with separately attributed records admitted to each Dashboard concern")
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
        val content = _record("content", component, "source-content", Some(Content), condition = condition)
        val usage = _record("usage", component, "source-usage", Some(Usage))
        val operation = _record("operation", component, "source-operation", Some(Operation))
        val quality = _record("quality", component, "source-quality", Some(QualityReview))

        When("the exact Component inventory is projected")
        val result = ComponentDashboard.create(component, Vector(content, usage, operation, quality))

        Then("the four fixed non-ranking groups retain exactly one matching source record with its attribution and condition")
        result.toOption.toVector.flatMap(_.concernGroups.map(group => (group.concern, group.records.map(_.sourceRecord)))) shouldBe
          Vector(
            (Content, Vector(content)),
            (Usage, Vector(usage)),
            (Operation, Vector(operation)),
            (QualityReview, Vector(quality))
          )
        result.toOption.toVector.flatMap(_.concernGroups.flatMap(_.records).map(_.sourceRecord.attribution)) shouldBe
          Vector(content.attribution, usage.attribution, operation.attribution, quality.attribution)
        result.toOption.toVector.flatMap(_.concernGroups.flatMap(_.records).map(_.sourceRecord.condition)) shouldBe
          Vector(condition, usage.condition, operation.condition, quality.condition)
      }

      "assemble fixed concern and identity-first record order independently of input iteration" in {
        Given("reverse-ordered records with two Content targets admitted for one exact Component")
        val component = _component("textus-order")
        val contentzulu = _record("content-zulu", component, "source-content-zulu", Some(Content), targetid = Some("zulu"))
        val contentalpha = _record("content-alpha", component, "source-content-alpha", Some(Content), targetid = Some("alpha"))
        val usage = _record("usage", component, "source-usage", Some(Usage), targetid = Some("usage"))
        val operation = _record("operation", component, "source-operation", Some(Operation), targetid = Some("operation"))
        val quality = _record("quality", component, "source-quality", Some(QualityReview), targetid = Some("quality"))

        When("the reverse inventory is projected")
        val result = ComponentDashboard.create(component, Vector(quality, operation, usage, contentzulu, contentalpha))

        Then("the fixed concern sequence and exact target identity order do not inherit input order")
        result.toOption.toVector.flatMap(_.concernGroups.map(_.concern)) shouldBe
          Vector(Content, Usage, Operation, QualityReview)
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups.filter(_.concern == Content).flatMap(_.records).flatMap(_.sourceRecord.semanticTargetId.map(_.value))
        } shouldBe Vector("alpha", "zulu")
      }

      "retain a content-looking statement without an admitted concern only as unclassified inventory" in {
        Given("one source statement whose representation resembles Content without an admitted Dashboard concern")
        val component = _component("textus-order")
        val unclassified = _record(
          "unclassified",
          component,
          "source-unclassified",
          None,
          statement = "Content: apparent purpose without Dashboard concern admission"
        )

        When("the source record is projected")
        val result = ComponentDashboard.create(component, Vector(unclassified))

        Then("no concern group infers membership and the exact source record remains unclassified")
        result.toOption.toVector.flatMap(_.concernGroups.flatMap(_.records)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.unclassifiedRecords.map(_.sourceRecord)) shouldBe Vector(unclassified)
      }

      "expose navigation only for an exact target admitted as implemented and usable" in {
        Given("exact targets whose source or receiving conditions may be unusable, plus one admitted implemented and usable target")
        val component = _component("textus-order")
        val unavailabletarget = _record(
          "unusable-target",
          component,
          "source-unusable-target",
          Some(Content),
          targetid = Some("entity-order"),
          navigation = Some(_navigation("entity-order", implementedandusable = false))
        )
        val sourceconditiontarget = _record(
          "source-condition-target",
          component,
          "source-source-condition-target",
          Some(Content),
          targetid = Some("entity-source-condition"),
          navigation = Some(_navigation("entity-source-condition", implementedandusable = true)),
          condition = _condition(availability = "unavailable")
        )
        val targetconditiontarget = _record(
          "target-condition-target",
          component,
          "source-target-condition-target",
          Some(Content),
          targetid = Some("entity-target-condition"),
          navigation = Some(
            _navigation(
              "entity-target-condition",
              implementedandusable = true,
              targetcondition = _condition(authorization = "denied")
            )
          )
        )
        val admittedtarget = _record(
          "admitted-target",
          component,
          "source-admitted-target",
          Some(Content),
          targetid = Some("event-order-created"),
          navigation = Some(_navigation("event-order-created", implementedandusable = true))
        )

        When("the target-gated records are projected")
        val result = ComponentDashboard.create(
          component,
          Vector(unavailabletarget, sourceconditiontarget, targetconditiontarget, admittedtarget)
        )

        Then("unusable source or target conditions and unimplemented targets expose no navigation while the admitted usable target retains exact provenance")
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups.flatMap(_.records).filter(_.sourceRecord.id == unavailabletarget.id).flatMap(_.navigationTarget)
        } shouldBe Vector.empty
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups.flatMap(_.records).filter(_.sourceRecord.id == sourceconditiontarget.id).flatMap(_.navigationTarget)
        } shouldBe Vector.empty
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups.flatMap(_.records).filter(_.sourceRecord.id == targetconditiontarget.id).flatMap(_.navigationTarget)
        } shouldBe Vector.empty
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups.flatMap(_.records).filter(_.sourceRecord.id == admittedtarget.id).flatMap(_.navigationTarget)
        } shouldBe Vector(_navigation("event-order-created", implementedandusable = true))
        result.toOption.toVector.flatMap { dashboard =>
          dashboard.concernGroups
            .flatMap(_.records)
            .filter(_.sourceRecord.id == admittedtarget.id)
            .flatMap(_.navigationTarget)
            .map(target => (target.targetComponent, target.targetAttribution, target.targetCondition))
        } shouldBe Vector(
          (
            component,
            ComponentDashboardSourceAttribution("target-source", "target-authority", "target-locator"),
            _condition()
          )
        )
        result.toOption.toVector.flatMap(_.concernGroups.flatMap(_.records).map(_.sourceRecord)) should contain allOf (
          unavailabletarget,
          sourceconditiontarget,
          targetconditiontarget,
          admittedtarget
        )
      }

      "reject navigation targets outside the exact Component scope or without target provenance" in {
        Given("navigation targets with a mismatched Component, blank attribution, and malformed target condition")
        val component = _component("textus-order")
        val outofscope = _record(
          "out-of-scope-target",
          component,
          "source-out-of-scope-target",
          Some(Content),
          targetid = Some("entity-order"),
          navigation = Some(
            _navigation(
              "entity-order",
              implementedandusable = true,
              targetcomponent = _component("textus-payment")
            )
          )
        )
        val unprovenanced = _record(
          "unprovenanced-target",
          component,
          "source-unprovenanced-target",
          Some(Content),
          targetid = Some("entity-unprovenanced"),
          navigation = Some(
            _navigation(
              "entity-unprovenanced",
              implementedandusable = true,
              targetattribution = _attribution(" ", "target:entity-unprovenanced"),
              targetcondition = _condition(redaction = Some(" "))
            )
          )
        )

        When("the target-gated records are validated")
        val result = ComponentDashboard.create(component, Vector(outofscope, unprovenanced))

        Then("target scope, attribution, and condition violations return a typed failure")
        result.isLeft shouldBe true
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'out-of-scope-target' navigation target must remain within its exact Component scope."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'unprovenanced-target' navigation target source identity must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'unprovenanced-target' navigation target condition redaction must not be blank."
        )
      }

      "return typed failures without a partial Dashboard for blank cross-component duplicate or unresolved-order input" in {
        Given("blank Component identity, cross-Component, duplicate, blank tie-key, statement, and condition inventory inputs")
        val component = _component("textus-order")
        val blankcomponent = _component(" ")
        val crosscomponent = _record("cross-component", _component("textus-payment"), "source-cross", Some(Content))
        val duplicate = _record("duplicate", component, "source-duplicate", Some(Content))
        val unresolvedorder = _record(
          "unresolved-order",
          component,
          "source-unresolved-order",
          Some(Content),
          targetid = Some("entity-order"),
          tiekey = Some(" ")
        )
        val blankstatement = _record(
          "blank-statement",
          component,
          "source-blank-statement",
          Some(Content),
          statement = " "
        )
        val blankcondition = _record(
          "blank-condition",
          component,
          "source-blank-condition",
          Some(Content),
          condition = _condition(redaction = Some(" "))
        )

        When("each invalid exact-inventory boundary is admitted")
        val blankresult = ComponentDashboard.create(blankcomponent, Vector(_record("blank", blankcomponent, "source-blank", Some(Content))))
        val crossresult = ComponentDashboard.create(component, Vector(crosscomponent))
        val duplicateresult = ComponentDashboard.create(component, Vector(duplicate, duplicate))
        val unresolvedresult = ComponentDashboard.create(component, Vector(unresolvedorder))
        val blankstatementresult = ComponentDashboard.create(component, Vector(blankstatement))
        val blankconditionresult = ComponentDashboard.create(component, Vector(blankcondition))

        Then("each boundary returns the typed failure instead of a partially projected Dashboard")
        Vector(blankresult, crossresult, duplicateresult, unresolvedresult, blankstatementresult, blankconditionresult).map(_.isLeft) shouldBe
          Vector(true, true, true, true, true, true)
        blankresult.left.toOption.toVector.flatMap(_.violations) should contain("Dashboard Component identity must not be blank.")
        crossresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'cross-component' is outside Dashboard Component 'textus-order'."
        )
        duplicateresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Dashboard inventory record identity 'duplicate'."
        )
        unresolvedresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'unresolved-order' presentation tie key must not be blank."
        )
        blankstatementresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'blank-statement' statement representation must not be blank."
        )
        blankconditionresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Dashboard inventory record 'blank-condition' condition redaction must not be blank."
        )
      }

      "reject targeted and mixed targeted or untargeted duplicate source-record identities" in {
        Given("unique targeted records, targeted records sharing one source-record identity, and mixed targeted or untargeted records sharing one identity")
        val component = _component("textus-order")
        val uniquetargetedrecords = Vector(
          _record("targeted-alpha", component, "source-targeted-alpha", Some(Content), targetid = Some("entity-alpha")),
          _record("targeted-zulu", component, "source-targeted-zulu", Some(Content), targetid = Some("entity-zulu"))
        )
        val targetedduplicaterecords = Vector(
          _record("targeted-duplicate-alpha", component, "source-targeted-duplicate", Some(Content), targetid = Some("entity-alpha")),
          _record("targeted-duplicate-zulu", component, "source-targeted-duplicate", Some(Content), targetid = Some("entity-zulu"))
        )
        val mixedduplicaterecords = Vector(
          _record("mixed-targeted", component, "source-mixed-duplicate", Some(Content), targetid = Some("entity-mixed")),
          _record("mixed-untargeted", component, "source-mixed-duplicate", Some(Content))
        )

        When("the source-record identity boundary is projected")
        val uniquetargeted = ComponentDashboard.create(component, uniquetargetedrecords)
        val targetedduplicate = ComponentDashboard.create(component, targetedduplicaterecords)
        val mixedduplicate = ComponentDashboard.create(component, mixedduplicaterecords)

        Then("unique targeted identities remain valid while targeted and mixed duplicates return typed failures")
        uniquetargeted.isRight shouldBe true
        targetedduplicate.isLeft shouldBe true
        mixedduplicate.isLeft shouldBe true
        targetedduplicate.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Dashboard source-record identity 'source-targeted-duplicate' is unresolved as an ordering key."
        )
        mixedduplicate.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Dashboard source-record identity 'source-mixed-duplicate' is unresolved as an ordering key."
        )
      }
    }
  }

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _attribution(sourceid: String, locator: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, "source-authority", locator)

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
    concern: Option[ComponentDashboardConcern],
    statement: String = "admitted statement",
    targetid: Option[String] = None,
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardInventoryRecord =
    ComponentDashboardInventoryRecord(
      recordid,
      component,
      sourceid,
      _attribution(sourceid, s"source:$sourceid"),
      statement,
      condition,
      concern,
      targetid.map(ComponentDashboardSemanticTargetIdentity),
      tiekey,
      navigation
    )
}
