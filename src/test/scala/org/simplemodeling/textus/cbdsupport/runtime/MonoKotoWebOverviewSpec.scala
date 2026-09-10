package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class MonoKotoWebOverviewSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "MonoKotoWebOverview" should {
    "P9-31B pure immutable stakeholder Web overview" which {
      "assemble reverse-iterated explicit presentations in inherited Mono-then-Koto identity order without engineering classification" in {
        Given("two Mono and two Koto subjects with multiple identity-distinct relationships and reverse presentation iteration")
        val component = _component("textus-order")
        val projection = _projection(
          component,
          Vector(
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
          )
        )
        val presentations = Vector(
          _presentation("koto-zulu", "koto-zulu-workflow", component, "workflow-settlement", "progresses through", "Settlement"),
          _presentation("koto-zulu", "koto-zulu-event", component, "event-payment-settled", "announces", "Payment settled"),
          _presentation("koto-alpha", "koto-alpha-event", component, "event-order-placed", "announces", "Order placed"),
          _presentation("koto-alpha", "koto-alpha-command", component, "command-place-order", "starts with", "Place order"),
          _presentation("mono-zulu", "mono-zulu-value", component, "value-payment-status", "keeps", "Payment status"),
          _presentation("mono-zulu", "mono-zulu-entity", component, "entity-payment", "contains", "Payment"),
          _presentation("mono-alpha", "mono-alpha-value", component, "value-order-status", "keeps", "Order status"),
          _presentation("mono-alpha", "mono-alpha-aggregate", component, "aggregate-order", "contains", "Order")
        )

        When("the existing exact Web entry, projection, and reverse presentations are assembled")
        val result = MonoKotoWebOverview.create(_web_entry(component), projection, presentations)

        Then("the output keeps stakeholder labels and explicit relationship material in inherited identity order without a Mono or Koto kind field")
        result.toOption.toVector.flatMap(_.subjects.map { subject =>
          (
            subject.sourceSubjectId,
            subject.semanticTargetId.value,
            subject.stakeholderLabel,
            subject.relationships.map(reference => (reference.sourceReferenceId, reference.semanticTargetId.value, reference.relationshipLabel, reference.targetLabel))
          )
        }) shouldBe Vector(
          (
            "mono-alpha",
            "mono-alpha",
            "Order",
            Vector(
              ("mono-alpha-aggregate", "aggregate-order", "contains", "Order"),
              ("mono-alpha-value", "value-order-status", "keeps", "Order status")
            )
          ),
          (
            "mono-zulu",
            "mono-zulu",
            "Payment",
            Vector(
              ("mono-zulu-entity", "entity-payment", "contains", "Payment"),
              ("mono-zulu-value", "value-payment-status", "keeps", "Payment status")
            )
          ),
          (
            "koto-alpha",
            "koto-alpha",
            "Place order",
            Vector(
              ("koto-alpha-command", "command-place-order", "starts with", "Place order"),
              ("koto-alpha-event", "event-order-placed", "announces", "Order placed")
            )
          ),
          (
            "koto-zulu",
            "koto-zulu",
            "Settle payment",
            Vector(
              ("koto-zulu-event", "event-payment-settled", "announces", "Payment settled"),
              ("koto-zulu-workflow", "workflow-settlement", "progresses through", "Settlement")
            )
          )
        )
      }

      "reject a manually fabricated noncanonical projection instead of rebuilding a substitute overview" in {
        Given("valid Mono and Koto source subjects manually wrapped in reverse projection subject order")
        val component = _component("textus-order")
        val monosubject = _subject(
          "mono-order",
          component,
          "mono-order",
          Mono,
          "Order",
          Vector(_reference("mono-order-reference", component, "aggregate-order", StructuralDomain))
        )
        val kotosubject = _subject(
          "koto-order",
          component,
          "koto-order",
          Koto,
          "Place order",
          Vector(_reference("koto-order-reference", component, "command-order", BehavioralTemporal))
        )
        val projection = MonoKotoProjection(
          _context(),
          component,
          Vector(
            MonoKotoProjectedSubject(
              kotosubject,
              kotosubject.references.map(reference => MonoKotoProjectedReference(reference, None)),
              None
            ),
            MonoKotoProjectedSubject(
              monosubject,
              monosubject.references.map(reference => MonoKotoProjectedReference(reference, None)),
              None
            )
          )
        )
        val presentations = Vector(
          _presentation("mono-order", "mono-order-reference", component, "aggregate-order", "contains", "Order"),
          _presentation("koto-order", "koto-order-reference", component, "command-order", "starts with", "Place order")
        )

        When("the exact Web entry and manually fabricated projection are submitted")
        val result = MonoKotoWebOverview.create(_web_entry(component), projection, presentations)

        Then("the malformed projection returns a typed failure without a normalized substitute overview")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Projection subjects must retain their admitted Mono-then-Koto identity order."
        )
      }

      "retain deterministic source sequence for generated presentation permutations" in {
        Given("a fixed admitted Mono and Koto projection plus every matching explicit presentation record")
        val component = _component("textus-order")
        val projection = _projection(
          component,
          Vector(
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
          )
        )
        val presentations = Vector(
          _presentation("mono-alpha", "mono-alpha-value", component, "value-order-status", "keeps", "Order status"),
          _presentation("mono-alpha", "mono-alpha-aggregate", component, "aggregate-order", "contains", "Order"),
          _presentation("koto-alpha", "koto-alpha-event", component, "event-order-placed", "announces", "Order placed"),
          _presentation("koto-alpha", "koto-alpha-command", component, "command-place-order", "starts with", "Place order")
        )
        val expectedsequence = Vector(
          ("mono-alpha", Vector("aggregate-order", "value-order-status")),
          ("koto-alpha", Vector("command-place-order", "event-order-placed"))
        )
        val presentationpermutations = Gen.pick(presentations.size, presentations)

        forAll(presentationpermutations) { permutedpresentations =>
          When("a generated presentation iteration order is assembled")
          val result = MonoKotoWebOverview.create(_web_entry(component), projection, permutedpresentations.toVector)

          Then("the overview sequence remains the normalized source sequence rather than the presentation iteration")
          result.toOption.toVector.flatMap(_.subjects.map { subject =>
            subject.sourceSubjectId -> subject.relationships.map(_.semanticTargetId.value)
          }) shouldBe expectedsequence
        }
      }

      "retain independent source and presentation provenance and every condition while suppressing presentation-disqualified navigation" in {
        Given("conditioned source subjects and references with separately attributed and conditioned relationship presentation material")
        val component = _component("textus-order")
        val subjectcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          redaction = Some("subject-redaction"),
          explicitabsence = Some("subject-not-established"),
          ambiguity = Some("subject-ambiguity"),
          conflict = Some("subject-conflict"),
          staleness = Some("subject-staleness"),
          malformedevidence = Some("subject-malformed"),
          limitations = Vector("subject-limitation")
        )
        val sourcecondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          redaction = Some("source-redaction"),
          explicitabsence = Some("source-not-established"),
          ambiguity = Some("source-ambiguity"),
          conflict = Some("source-conflict"),
          staleness = Some("source-staleness"),
          malformedevidence = Some("source-malformed"),
          limitations = Vector("source-limitation")
        )
        val presentationcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          redaction = Some("presentation-redaction"),
          explicitabsence = Some("presentation-not-established"),
          ambiguity = Some("presentation-ambiguity"),
          conflict = Some("presentation-conflict"),
          staleness = Some("presentation-staleness"),
          malformedevidence = Some("presentation-malformed"),
          limitations = Vector("presentation-limitation")
        )
        val sourceattribution = _attribution("source-reference")
        val presentationattribution = _attribution("presentation-reference")
        val sourceconditioned = _reference(
          "source-conditioned",
          component,
          "aggregate-source-conditioned",
          StructuralDomain,
          sourceattribution,
          sourcecondition,
          Some(_navigation("aggregate-source-conditioned", component))
        )
        val presentationconditioned = _reference(
          "presentation-conditioned",
          component,
          "aggregate-presentation-conditioned",
          StructuralDomain,
          sourceattribution,
          _condition(),
          Some(_navigation("aggregate-presentation-conditioned", component))
        )
        val projection = _projection(
          component,
          Vector(
            _subject(
              "mono-order",
              component,
              "mono-order",
              Mono,
              "Order",
              Vector(sourceconditioned, presentationconditioned),
              _attribution("source-subject"),
              subjectcondition
            )
          )
        )
        val presentations = Vector(
          _presentation(
            "mono-order",
            "source-conditioned",
            component,
            "aggregate-source-conditioned",
            "contains",
            "Source conditioned aggregate",
            presentationattribution,
            _condition()
          ),
          _presentation(
            "mono-order",
            "presentation-conditioned",
            component,
            "aggregate-presentation-conditioned",
            "contains",
            "Presentation conditioned aggregate",
            presentationattribution,
            presentationcondition
          )
        )

        When("the overview is assembled from the independently conditioned material")
        val result = MonoKotoWebOverview.create(_web_entry(component), projection, presentations)

        Then("all source and presentation provenance and condition values remain distinct, while neither disqualified target becomes a link")
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceCondition)) shouldBe Vector(subjectcondition)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.relationships).map { reference =>
          (reference.sourceReferenceId, reference.sourceAttribution, reference.sourceCondition, reference.presentationAttribution, reference.presentationCondition, reference.navigationTarget)
        }) shouldBe Vector(
          ("presentation-conditioned", sourceattribution, _condition(), presentationattribution, presentationcondition, None),
          ("source-conditioned", sourceattribution, sourcecondition, presentationattribution, _condition(), None)
        )
      }

      "retain only exact usable navigation and return a typed failure for a mismatched source target" in {
        Given("exact usable, unimplemented, target-conditioned, presentation-conditioned, and mismatched source target cases")
        val component = _component("textus-order")
        val usable = _reference(
          "usable",
          component,
          "aggregate-usable",
          StructuralDomain,
          _attribution("usable-source"),
          _condition(),
          Some(_navigation("aggregate-usable", component))
        )
        val unimplemented = _reference(
          "unimplemented",
          component,
          "aggregate-unimplemented",
          StructuralDomain,
          _attribution("unimplemented-source"),
          _condition(),
          Some(_navigation("aggregate-unimplemented", component, _condition(), implementedandusable = false))
        )
        val targetconditioned = _reference(
          "target-conditioned",
          component,
          "aggregate-target-conditioned",
          StructuralDomain,
          _attribution("target-conditioned-source"),
          _condition(),
          Some(_navigation("aggregate-target-conditioned", component, _condition(redaction = Some("target-redaction"))))
        )
        val presentationconditioned = _reference(
          "presentation-conditioned",
          component,
          "aggregate-presentation-conditioned",
          StructuralDomain,
          _attribution("presentation-conditioned-source"),
          _condition(),
          Some(_navigation("aggregate-presentation-conditioned", component))
        )
        val subject = _subject(
          "mono-order",
          component,
          "mono-order",
          Mono,
          "Order",
          Vector(usable, unimplemented, targetconditioned, presentationconditioned),
          _attribution("subject-source"),
          _condition(),
          Some(_navigation("mono-order", component))
        )
        val projection = _projection(component, Vector(subject))
        val presentations = Vector(
          _presentation("mono-order", "usable", component, "aggregate-usable", "contains", "Usable aggregate"),
          _presentation("mono-order", "unimplemented", component, "aggregate-unimplemented", "contains", "Unimplemented aggregate"),
          _presentation("mono-order", "target-conditioned", component, "aggregate-target-conditioned", "contains", "Target conditioned aggregate"),
          _presentation(
            "mono-order",
            "presentation-conditioned",
            component,
            "aggregate-presentation-conditioned",
            "contains",
            "Presentation conditioned aggregate",
            _attribution("presentation-source"),
            _condition(authorization = "denied")
          )
        )
        val mismatchedreference = usable.copy(
          id = "mismatched",
          semanticTargetId = ComponentDashboardSemanticTargetIdentity("aggregate-mismatched"),
          navigationTarget = Some(_navigation("aggregate-other", component))
        )
        val mismatchedprojection = MonoKotoProjection(
          _context(),
          component,
          Vector(
            MonoKotoProjectedSubject(
              subject.copy(references = Vector(mismatchedreference)),
              Vector(MonoKotoProjectedReference(mismatchedreference, mismatchedreference.navigationTarget)),
              None
            )
          )
        )

        When("the exact target cases and a direct identity mismatch are assembled")
        val result = MonoKotoWebOverview.create(_web_entry(component), projection, presentations)
        val mismatchedresult = MonoKotoWebOverview.create(
          _web_entry(component),
          mismatchedprojection,
          Vector(_presentation("mono-order", "mismatched", component, "aggregate-mismatched", "contains", "Mismatched aggregate"))
        )

        Then("only the exact usable targets remain navigable and a mismatched source target cannot yield a substitute overview")
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.navigationTarget)) shouldBe Vector(_navigation("mono-order", component))
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.relationships).map { reference =>
          reference.sourceReferenceId -> reference.navigationTarget
        }) shouldBe Vector(
          "presentation-conditioned" -> None,
          "target-conditioned" -> None,
          "unimplemented" -> None,
          "usable" -> Some(_navigation("aggregate-usable", component))
        )
        mismatchedresult.isLeft shouldBe true
        mismatchedresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto reference 'mismatched' navigation target must retain its exact semantic target identity."
        )
      }

      "return typed failure without a partial overview for Component and presentation boundary violations" in {
        Given("one valid exact source tuple plus mismatched duplicate missing orphan blank and identity-invalid presentation variants")
        val component = _component("textus-order")
        val othercomponent = _component("textus-payment")
        val reference = _reference("aggregate-order", component, "aggregate-order", StructuralDomain)
        val subject = _subject("mono-order", component, "mono-order", Mono, "Order", Vector(reference))
        val projection = _projection(component, Vector(subject))
        val presentation = _presentation("mono-order", "aggregate-order", component, "aggregate-order", "contains", "Order")
        val subjectcomponentmismatchprojection = MonoKotoProjection(
          _context(),
          component,
          Vector(MonoKotoProjectedSubject(subject.copy(component = othercomponent), Vector.empty, None))
        )
        val referencecomponentmismatchprojection = MonoKotoProjection(
          _context(),
          component,
          Vector(
            MonoKotoProjectedSubject(
              subject.copy(references = Vector(reference.copy(component = othercomponent))),
              Vector.empty,
              None
            )
          )
        )

        When("each malformed exact-boundary tuple is submitted")
        val componentmismatchresult = MonoKotoWebOverview.create(_web_entry(othercomponent), projection, Vector(presentation))
        val duplicateresult = MonoKotoWebOverview.create(_web_entry(component), projection, Vector(presentation, presentation))
        val missingresult = MonoKotoWebOverview.create(_web_entry(component), projection, Vector.empty)
        val orphanresult = MonoKotoWebOverview.create(
          _web_entry(component),
          projection,
          Vector(presentation, _presentation("mono-order", "orphan", component, "aggregate-orphan", "contains", "Orphan"))
        )
        val blankresult = MonoKotoWebOverview.create(_web_entry(component), projection, Vector(presentation.copy(relationshipLabel = " ")))
        val presentationcomponentresult = MonoKotoWebOverview.create(
          _web_entry(component),
          projection,
          Vector(presentation.copy(component = othercomponent))
        )
        val presentationtargetresult = MonoKotoWebOverview.create(
          _web_entry(component),
          projection,
          Vector(presentation.copy(semanticTargetId = ComponentDashboardSemanticTargetIdentity("aggregate-other")))
        )
        val subjectcomponentresult = MonoKotoWebOverview.create(
          _web_entry(component),
          subjectcomponentmismatchprojection,
          Vector(presentation)
        )
        val referencecomponentresult = MonoKotoWebOverview.create(
          _web_entry(component),
          referencecomponentmismatchprojection,
          Vector(presentation)
        )

        Then("all malformed inputs remain typed failures and none admits a partial overview")
        Vector(
          componentmismatchresult,
          duplicateresult,
          missingresult,
          orphanresult,
          blankresult,
          presentationcomponentresult,
          presentationtargetresult,
          subjectcomponentresult,
          referencecomponentresult
        ).map(_.isLeft) shouldBe Vector.fill(9)(true)
        componentmismatchresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview delivered Component 'textus-payment' does not match Projection Component 'textus-order'."
        )
        duplicateresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Mono-Koto Web overview presentation for subject 'mono-order' reference 'aggregate-order'."
        )
        missingresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview projected subject 'mono-order' reference 'aggregate-order' requires exactly one presentation."
        )
        orphanresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview presentation subject 'mono-order' reference 'orphan' does not match a projected source reference."
        )
        blankresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview presentation subject 'mono-order' reference 'aggregate-order' relationship label must not be blank."
        )
        presentationcomponentresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview presentation reference 'aggregate-order' must retain its exact Component identity."
        )
        presentationtargetresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Web overview presentation reference 'aggregate-order' must retain its exact semantic target identity."
        )
        subjectcomponentresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto subject 'mono-order' is outside Projection Component 'textus-order'."
        )
        referencecomponentresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto reference 'aggregate-order' is outside subject 'mono-order' exact Component scope."
        )
      }

      "reject a manually constructed projection with blank bounded CCDM context without a partial overview" in {
        Given("one otherwise valid projected Mono subject whose manually constructed projection carries blank context")
        val component = _component("textus-order")
        val subject = _subject(
          "mono-order",
          component,
          "mono-order",
          Mono,
          "Order",
          Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain))
        )
        val admittedprojection = _projection(component, Vector(subject))
        val malformedprojection = MonoKotoProjection(_context(" "), component, admittedprojection.subjects)
        val presentations = Vector(
          _presentation("mono-order", "aggregate-order", component, "aggregate-order", "contains", "Order")
        )

        When("the exact Web entry receives the manually malformed projection")
        val result = MonoKotoWebOverview.create(_web_entry(component), malformedprojection, presentations)

        Then("the direct integrity gate rejects the blank context without constructing a substitute overview")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto Projection bounded CCDM context identity must not be blank."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-order-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _web_entry(component: ComponentDashboardComponentIdentity): ComponentDashboardWebEntry =
    ComponentDashboardWebEntry.create(
      ComponentDashboardWebEntryAuthorizationAffirmative(component),
      ComponentDashboard(component, Vector.empty, Vector.empty)
    ).toOption.get

  private def _projection(
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[MonoKotoProjectionSubject]
  ): MonoKotoProjection =
    MonoKotoProjection.create(_context(), component, subjects).toOption.get

  private def _attribution(sourceid: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, s"$sourceid-authority", s"$sourceid:locator")

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
    targetcondition: ComponentDashboardCondition = _condition(),
    implementedandusable: Boolean = true
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      _attribution(s"target-$targetid"),
      targetcondition,
      implementedandusable
    )

  private def _reference(
    referenceid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    referencekind: MonoKotoProjectionReferenceKind,
    attribution: ComponentDashboardSourceAttribution = _attribution("source-reference"),
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): MonoKotoProjectionReference =
    MonoKotoProjectionReference(
      referenceid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      referencekind,
      attribution,
      condition,
      None,
      navigation
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    projectionkind: MonoKotoProjectionKind,
    label: String,
    references: Vector[MonoKotoProjectionReference],
    attribution: ComponentDashboardSourceAttribution = _attribution("source-subject"),
    condition: ComponentDashboardCondition = _condition(),
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
      None,
      navigation
    )

  private def _presentation(
    subjectid: String,
    referenceid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    relationshiplabel: String,
    targetlabel: String,
    attribution: ComponentDashboardSourceAttribution = _attribution("presentation-source"),
    condition: ComponentDashboardCondition = _condition()
  ): MonoKotoWebOverviewReferencePresentation =
    MonoKotoWebOverviewReferencePresentation(
      subjectid,
      referenceid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      relationshiplabel,
      targetlabel,
      attribution,
      condition
    )
}
