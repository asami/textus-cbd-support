package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class MonoKotoProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "MonoKotoProjection" should {
    "P9-30B pure read-only Mono-Koto projection" which {
      "order Mono then Koto subjects and their multiple admitted references independently of input iteration" in {
        Given("reverse-ordered Mono and Koto subjects that each aggregate two or more exact semantic identities")
        val component = _component("textus-order")
        val monoalpha = _subject(
          "mono-alpha",
          component,
          "mono-alpha",
          Mono,
          "Order",
          Vector(
            _reference("mono-alpha-value", component, "value-order-status", StructuralDomain),
            _reference("mono-alpha-aggregate", component, "aggregate-order", StructuralDomain)
          )
        )
        val monozulu = _subject(
          "mono-zulu",
          component,
          "mono-zulu",
          Mono,
          "Payment",
          Vector(
            _reference("mono-zulu-entity", component, "entity-payment", StructuralDomain),
            _reference("mono-zulu-value", component, "value-payment-status", StructuralDomain)
          )
        )
        val kotoalpha = _subject(
          "koto-alpha",
          component,
          "koto-alpha",
          Koto,
          "Place order",
          Vector(
            _reference("koto-alpha-event", component, "event-order-placed", BehavioralTemporal),
            _reference("koto-alpha-command", component, "command-place-order", BehavioralTemporal)
          )
        )
        val kotozulu = _subject(
          "koto-zulu",
          component,
          "koto-zulu",
          Koto,
          "Settle payment",
          Vector(
            _reference("koto-zulu-workflow", component, "workflow-settlement", BehavioralTemporal),
            _reference("koto-zulu-event", component, "event-payment-settled", BehavioralTemporal)
          )
        )

        When("the reverse source order is projected")
        val result = MonoKotoProjection.create(_context(), component, Vector(kotozulu, kotoalpha, monozulu, monoalpha))

        Then("the fixed kind sequence and identity keys, not labels or input order, assemble one-to-many Mono and Koto aggregations")
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.id)) shouldBe
          Vector("mono-alpha", "mono-zulu", "koto-alpha", "koto-zulu")
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "mono-alpha").flatMap(_.references.map(_.sourceReference.semanticTargetId.value))) shouldBe
          Vector("aggregate-order", "value-order-status")
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "koto-alpha").flatMap(_.references.map(_.sourceReference.semanticTargetId.value))) shouldBe
          Vector("command-place-order", "event-order-placed")
        result.toOption.toVector.flatMap(_.subjects.map(_.references.size)) shouldBe Vector(2, 2, 2, 2)
      }

      "preserve deterministic Mono-then-Koto identity order for generated source permutations" in {
        Given("a fixed admitted Mono and Koto subject set with identity-distinct references")
        val component = _component("textus-order")
        val subjects = Vector(
          _subject(
            "mono-alpha",
            component,
            "mono-alpha",
            Mono,
            "Order",
            Vector(
              _reference("mono-alpha-value", component, "value-order-status", StructuralDomain),
              _reference("mono-alpha-aggregate", component, "aggregate-order", StructuralDomain)
            )
          ),
          _subject(
            "mono-zulu",
            component,
            "mono-zulu",
            Mono,
            "Payment",
            Vector(
              _reference("mono-zulu-entity", component, "entity-payment", StructuralDomain),
              _reference("mono-zulu-value", component, "value-payment-status", StructuralDomain)
            )
          ),
          _subject(
            "koto-alpha",
            component,
            "koto-alpha",
            Koto,
            "Place order",
            Vector(
              _reference("koto-alpha-event", component, "event-order-placed", BehavioralTemporal),
              _reference("koto-alpha-command", component, "command-place-order", BehavioralTemporal)
            )
          ),
          _subject(
            "koto-zulu",
            component,
            "koto-zulu",
            Koto,
            "Settle payment",
            Vector(
              _reference("koto-zulu-workflow", component, "workflow-settlement", BehavioralTemporal),
              _reference("koto-zulu-event", component, "event-payment-settled", BehavioralTemporal)
            )
          )
        )
        val expectedsubjectids = Vector("mono-alpha", "mono-zulu", "koto-alpha", "koto-zulu")
        val expectedreferenceids = Vector(
          Vector("aggregate-order", "value-order-status"),
          Vector("entity-payment", "value-payment-status"),
          Vector("command-place-order", "event-order-placed"),
          Vector("event-payment-settled", "workflow-settlement")
        )

        val permutedsubjectsgenerator = for {
          monoalphareferences <- Gen.pick(subjects(0).references.size, subjects(0).references)
          monozulureferences <- Gen.pick(subjects(1).references.size, subjects(1).references)
          kotoalphareferences <- Gen.pick(subjects(2).references.size, subjects(2).references)
          kotozulureferences <- Gen.pick(subjects(3).references.size, subjects(3).references)
          rebuiltsubjects = Vector(
            subjects(0).copy(references = monoalphareferences.toVector),
            subjects(1).copy(references = monozulureferences.toVector),
            subjects(2).copy(references = kotoalphareferences.toVector),
            subjects(3).copy(references = kotozulureferences.toVector)
          )
          permutedsubjects <- Gen.pick(rebuiltsubjects.size, rebuiltsubjects)
        } yield permutedsubjects

        forAll(permutedsubjectsgenerator) { permutedsubjects =>
          When("a generated source permutation is projected")
          val result = MonoKotoProjection.create(_context(), component, permutedsubjects.toVector)

          Then("identity-first ordering remains Mono then Koto for subjects and references")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.id)) shouldBe expectedsubjectids
          result.toOption.toVector.flatMap(_.subjects.map(_.references.map(_.sourceReference.semanticTargetId.value))) shouldBe
            expectedreferenceids
        }
      }

      "admit explicit absence and unavailable zero-reference subjects while preserving independent conditions" in {
        Given("one explicitly absent Mono, one unavailable limited zero-reference Koto, and one conditioned Koto reference")
        val component = _component("textus-order")
        val absencecondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          redaction = Some("contract-redaction"),
          explicitabsence = Some("no-admitted-structural-membership"),
          ambiguity = Some("scope-is-bounded"),
          conflict = Some("attributed-conflict"),
          staleness = Some("freshness-unknown"),
          malformedevidence = Some("source-shape-invalid"),
          limitations = Vector("provider-boundary")
        )
        val limitedcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          limitations = Vector("provider-boundary")
        )
        val referencecondition = _condition(authorization = "denied", limitations = Vector("reference-boundary"))
        val absentmono = _subject("mono-absent", component, "mono-absent", Mono, "Order", Vector.empty, condition = absencecondition)
        val limitedkoto = _subject("koto-limited", component, "koto-limited", Koto, "Place order", Vector.empty, condition = limitedcondition)
        val conditionedkoto = _subject(
          "koto-conditioned",
          component,
          "koto-conditioned",
          Koto,
          "Place order",
          Vector(_reference("koto-reference", component, "event-order-placed", BehavioralTemporal, condition = referencecondition)),
          condition = _condition(ambiguity = Some("subject-ambiguity"))
        )

        When("the bounded source is projected")
        val result = MonoKotoProjection.create(_context(), component, Vector(conditionedkoto, limitedkoto, absentmono))

        Then("both zero-membership conditions remain explicit rather than inferred and every source condition remains available in the projected values")
        result.isRight shouldBe true
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "mono-absent").map(_.references)) shouldBe Vector(Vector.empty)
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "mono-absent").map(_.sourceSubject.condition)) shouldBe Vector(absencecondition)
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "koto-limited").map(_.references)) shouldBe Vector(Vector.empty)
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "koto-limited").map(_.sourceSubject.condition)) shouldBe Vector(limitedcondition)
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "koto-conditioned").map(_.sourceSubject.condition)) shouldBe
          Vector(_condition(ambiguity = Some("subject-ambiguity")))
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "koto-conditioned").flatMap(_.references.map(_.sourceReference.condition))) shouldBe
          Vector(referencecondition)
      }

      "retain navigation only for exact implemented and usable targets while preserving every source value" in {
        Given("a subject and references with usable, unimplemented, source-disqualified, and target-disqualified exact navigation targets")
        val component = _component("textus-order")
        val usabletarget = _navigation("structural-order", implementedandusable = true)
        val unimplementedtarget = _navigation("structural-payment", implementedandusable = false)
        val targetconditiontarget = _navigation(
          "structural-customer",
          implementedandusable = true,
          targetcondition = _condition(redaction = Some("target-redaction"))
        )
        val subjecttarget = _navigation("mono-order", implementedandusable = true)
        val subject = _subject(
          "mono-order",
          component,
          "mono-order",
          Mono,
          "Order",
          Vector(
            _reference("usable", component, "structural-order", StructuralDomain, navigation = Some(usabletarget)),
            _reference("unimplemented", component, "structural-payment", StructuralDomain, navigation = Some(unimplementedtarget)),
            _reference(
              "source-disqualified",
              component,
              "structural-invoice",
              StructuralDomain,
              navigation = Some(_navigation("structural-invoice", implementedandusable = true)),
              condition = _condition(authorization = "denied")
            ),
            _reference("target-disqualified", component, "structural-customer", StructuralDomain, navigation = Some(targetconditiontarget))
          ),
          navigation = Some(subjecttarget)
        )

        When("the exact Component subject is projected")
        val result = MonoKotoProjection.create(_context(), component, Vector(subject))

        Then("only usable implemented targets are retained and the unavailable navigation does not remove any reference source value")
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.navigationTarget)) shouldBe Vector(subjecttarget)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.references.filter(_.sourceReference.id == "usable").flatMap(_.navigationTarget))) shouldBe
          Vector(usabletarget)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.references.filter(_.sourceReference.id != "usable").flatMap(_.navigationTarget))) shouldBe
          Vector.empty
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.references.map(_.sourceReference.id))) should contain allOf (
          "usable",
          "unimplemented",
          "source-disqualified",
          "target-disqualified"
        )
      }

      "return typed failures without a partial projection for blank duplicate out-of-scope and incompatible tuples" in {
        Given("invalid Component, identity, scope, and role boundaries")
        val component = _component("textus-order")
        val validmono = _subject(
          "mono-duplicate",
          component,
          "mono-duplicate",
          Mono,
          "Order",
          Vector(_reference("structural-order", component, "aggregate-order", StructuralDomain))
        )
        val blanksubject = _subject(
          " ",
          component,
          "mono-blank",
          Mono,
          "Order",
          Vector(_reference("structural-blank", component, "aggregate-blank", StructuralDomain))
        )
        val outofscope = _subject(
          "mono-out-of-scope",
          component,
          "mono-out-of-scope",
          Mono,
          "Order",
          Vector(_reference("structural-out-of-scope", _component("textus-payment"), "aggregate-payment", StructuralDomain))
        )
        val incompatible = _subject(
          "mono-incompatible",
          component,
          "mono-incompatible",
          Mono,
          "Order",
          Vector(_reference("behavioral-incompatible", component, "event-order-placed", BehavioralTemporal))
        )

        When("each malformed boundary tuple is admitted")
        val blankcomponentresult = MonoKotoProjection.create(_context(), _component(" "), Vector(validmono.copy(component = _component(" "))))
        val blankresult = MonoKotoProjection.create(_context(), component, Vector(blanksubject))
        val duplicateresult = MonoKotoProjection.create(_context(), component, Vector(validmono, validmono))
        val outofscoperesult = MonoKotoProjection.create(_context(), component, Vector(outofscope))
        val incompatibleresult = MonoKotoProjection.create(_context(), component, Vector(incompatible))

        Then("every malformed tuple returns MonoKotoProjectionFailure instead of any partial subject collection")
        Vector(blankcomponentresult, blankresult, duplicateresult, outofscoperesult, incompatibleresult).map(_.isLeft) shouldBe
          Vector(true, true, true, true, true)
        blankresult.left.toOption.toVector.flatMap(_.violations) should contain("Mono-Koto subject identity must not be blank.")
        duplicateresult.left.toOption.toVector.flatMap(_.violations) should contain("Duplicate Mono-Koto subject identity 'mono-duplicate'.")
        outofscoperesult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto reference 'structural-out-of-scope' is outside subject 'mono-out-of-scope' exact Component scope."
        )
        incompatibleresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto subject 'mono-incompatible' Mono reference 'behavioral-incompatible' has incompatible kind BehavioralTemporal."
        )
      }

      "pass through exact identity attribution and locator values without reconstructing a loose target" in {
        Given("source and target values with distinct exact identities and locators, plus a target that only resembles a source by label")
        val component = _component("textus-order")
        val subjectattribution = ComponentDashboardSourceAttribution("subject-source", "subject-authority", "subject:locator")
        val referenceattribution = ComponentDashboardSourceAttribution("reference-source", "reference-authority", "reference:locator")
        val targetattribution = ComponentDashboardSourceAttribution("target-source", "target-authority", "target:locator")
        val exacttarget = ComponentDashboardNavigationTarget(
          ComponentDashboardSemanticTargetIdentity("aggregate-order-id"),
          component,
          targetattribution,
          _condition(),
          implementedAndUsable = true
        )
        val subject = _subject(
          "mono-order",
          component,
          "mono-order-id",
          Mono,
          "Order",
          Vector(
            _reference(
              "aggregate-order",
              component,
              "aggregate-order-id",
              StructuralDomain,
              attribution = referenceattribution,
              navigation = Some(exacttarget)
            )
          ),
          attribution = subjectattribution
        )
        val looseidentity = _reference(
          "aggregate-order-label",
          component,
          "aggregate-order-id",
          StructuralDomain,
          navigation = Some(_navigation("aggregate-order-label", implementedandusable = true))
        )

        When("the exact target and the look-alike target are admitted")
        val result = MonoKotoProjection.create(_context(), component, Vector(subject))
        val looseidentityresult = MonoKotoProjection.create(_context(), component, Vector(subject.copy(references = Vector(looseidentity))))

        Then("the projection retains supplied source and target values verbatim and rejects an identity reconstructed from a label-like value")
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.attribution)) shouldBe Vector(subjectattribution)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.references.map(_.sourceReference.attribution))) shouldBe Vector(referenceattribution)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.references.flatMap(_.navigationTarget).map { target =>
          (target.targetIdentity, target.targetComponent, target.targetAttribution, target.targetCondition)
        })) shouldBe Vector((ComponentDashboardSemanticTargetIdentity("aggregate-order-id"), component, targetattribution, _condition()))
        looseidentityresult.isLeft shouldBe true
        looseidentityresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto reference 'aggregate-order-label' navigation target must retain its exact semantic target identity."
        )
      }

      "retain one explicit bounded CCDM context and reject blank context without a partial projection" in {
        Given("one admitted Mono subject with a valid bounded context and a manually blank context")
        val component = _component("textus-order")
        val context = _context("textus-order-ccdm")
        val subject = _subject(
          "mono-order",
          component,
          "mono-order",
          Mono,
          "Order",
          Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain))
        )

        When("the same admitted values are projected with the valid and blank contexts")
        val validresult = MonoKotoProjection.create(context, component, Vector(subject))
        val blankresult = MonoKotoProjection.create(_context(" "), component, Vector(subject))

        Then("the exact valid context is retained and blank context returns only a typed failure")
        validresult.toOption.map(_.context) shouldBe Some(context)
        blankresult.isLeft shouldBe true
        blankresult.toOption shouldBe empty
        blankresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Projection bounded CCDM context identity must not be blank."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-order-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

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
      "target:locator"
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

  private def _reference(
    referenceid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    referencekind: MonoKotoProjectionReferenceKind,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "reference-source",
      "reference-authority",
      "reference:locator"
    ),
    condition: ComponentDashboardCondition = _condition(),
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): MonoKotoProjectionReference =
    MonoKotoProjectionReference(
      referenceid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      referencekind,
      attribution,
      condition,
      tiekey,
      navigation
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    projectionkind: MonoKotoProjectionKind,
    label: String,
    references: Vector[MonoKotoProjectionReference],
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "subject-source",
      "subject-authority",
      "subject:locator"
    ),
    condition: ComponentDashboardCondition = _condition(),
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): MonoKotoProjectionSubject =
    MonoKotoProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      projectionkind,
      label,
      attribution,
      condition,
      references,
      tiekey,
      navigation
    )
}
