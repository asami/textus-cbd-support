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
final class MonoKotoSemanticBridgeSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "MonoKotoSemanticBridge" should {
    "P9-32C pure immutable relation ledger" which {
      "retain structural and behavioral many-to-many categories without a Mono equals Entity or Koto equals Event collapse" in {
        Given("one exact Projection with structural Mono references and behavioral Koto references")
        val component = _component()
        val projection = _projection(
          component,
          Vector(
            _subject("mono-order", component, Mono, Vector(
              _reference("mono-aggregate", component, "aggregate-order", StructuralDomain),
              _reference("mono-entity", component, "entity-order", StructuralDomain),
              _reference("mono-value", component, "value-order-status", StructuralDomain),
              _reference("mono-structure", component, "relation-order-customer", StructuralDomain)
            )),
            _subject("koto-place", component, Koto, Vector(
              _reference("koto-command", component, "command-place-order", BehavioralTemporal),
              _reference("koto-event", component, "event-order-placed", BehavioralTemporal),
              _reference("koto-workflow", component, "workflow-place-order", BehavioralTemporal),
              _reference("koto-effect", component, "state-effect-order-placed", BehavioralTemporal)
            ))
          )
        )
        val admissions = Vector(
          _admission("mono-aggregate", "mono-order", "mono-aggregate", _target("aggregate-order", component, Aggregate)),
          _admission("mono-entity", "mono-order", "mono-entity", _target("entity-order", component, Entity)),
          _admission("mono-value", "mono-order", "mono-value", _target("value-order-status", component, Value)),
          _admission("mono-structure", "mono-order", "mono-structure", _target("relation-order-customer", component, StructuralRelation)),
          _admission("koto-command", "koto-place", "koto-command", _target("command-place-order", component, Command)),
          _admission("koto-event", "koto-place", "koto-event", _target("event-order-placed", component, Event)),
          _admission("koto-workflow", "koto-place", "koto-workflow", _target("workflow-place-order", component, WorkflowActivity)),
          _admission("koto-effect", "koto-place", "koto-effect", _target("state-effect-order-placed", component, StateEffect))
        )

        When("the caller supplies already admitted exact target categories")
        val result = MonoKotoSemanticBridge.create(projection, admissions)

        Then("the ledger retains every supplied compatible category rather than inferring one preferred Entity or Event category")
        result.toOption.toVector.flatMap(_.relations.map(_.target.category)) shouldBe
          Vector(Command, Event, StateEffect, WorkflowActivity, Aggregate, Entity, StructuralRelation, Value)
        result.toOption.toVector.flatMap(_.relations.map(_.sourceSubject.sourceSubject.projectionKind)) shouldBe
          Vector(Koto, Koto, Koto, Koto, Mono, Mono, Mono, Mono)
      }

      "derive forward and reverse groups from the same retained relation values" in {
        Given("two Mono subjects with different exact references to one admitted Aggregate target")
        val component = _component()
        val projection = _projection(
          component,
          Vector(
            _subject("mono-order", component, Mono, Vector(_reference("order-aggregate", component, "aggregate-sales", StructuralDomain))),
            _subject("mono-invoice", component, Mono, Vector(_reference("invoice-aggregate", component, "aggregate-sales", StructuralDomain)))
          )
        )
        val target = _target("aggregate-sales", component, Aggregate)
        val admissions = Vector(
          _admission("relation-order", "mono-order", "order-aggregate", target),
          _admission("relation-invoice", "mono-invoice", "invoice-aggregate", target)
        )

        When("the exact admissions are retained in one bridge ledger")
        val result = MonoKotoSemanticBridge.create(projection, admissions)

        Then("forward and reverse navigation contain only the same relation records, without an independently constructed inverse")
        val bridge = result.toOption.get
        bridge.forwardGroups.flatMap(_.relations) shouldBe bridge.relations
        bridge.reverseGroups should have size 1
        bridge.reverseGroups.head.relations should contain theSameElementsAs bridge.relations
        bridge.reverseGroups.head.relations.map(_.target.identity.value) shouldBe Vector("aggregate-sales", "aggregate-sales")
      }

      "preserve deterministic identity-first forward and reverse order for generated admission permutations" in {
        Given("one fixed Projection and four exact admissions with identities distinct from their input order")
        val component = _component()
        val projection = _projection(
          component,
          Vector(
            _subject("mono-zulu", component, Mono, Vector(_reference("mono-zulu-ref", component, "aggregate-zulu", StructuralDomain))),
            _subject("mono-alpha", component, Mono, Vector(_reference("mono-alpha-ref", component, "aggregate-alpha", StructuralDomain))),
            _subject("koto-zulu", component, Koto, Vector(_reference("koto-zulu-ref", component, "event-zulu", BehavioralTemporal))),
            _subject("koto-alpha", component, Koto, Vector(_reference("koto-alpha-ref", component, "event-alpha", BehavioralTemporal)))
          )
        )
        val admissions = Vector(
          _admission("relation-mono-zulu", "mono-zulu", "mono-zulu-ref", _target("aggregate-zulu", component, Aggregate)),
          _admission("relation-mono-alpha", "mono-alpha", "mono-alpha-ref", _target("aggregate-alpha", component, Aggregate)),
          _admission("relation-koto-zulu", "koto-zulu", "koto-zulu-ref", _target("event-zulu", component, Event)),
          _admission("relation-koto-alpha", "koto-alpha", "koto-alpha-ref", _target("event-alpha", component, Event))
        )
        val expectedforward = Vector(
          "koto-alpha:event-alpha",
          "koto-zulu:event-zulu",
          "mono-alpha:aggregate-alpha",
          "mono-zulu:aggregate-zulu"
        )
        val expectedreversegroups = Vector("aggregate-alpha", "aggregate-zulu", "event-alpha", "event-zulu")

        forAll(Gen.pick(admissions.size, admissions)) { permutedadmissions =>
          When("a property-generated admission order is supplied")
          val result = MonoKotoSemanticBridge.create(projection, permutedadmissions.toVector)

          Then("the retained ledger and inverse groups use exact identities rather than caller iteration order")
          result.toOption.toVector.flatMap(_.relations.map { relation =>
            s"${relation.sourceSubject.sourceSubject.id}:${relation.target.identity.value}"
          }) shouldBe expectedforward
          result.toOption.toVector.flatMap(_.reverseGroups.map(_.target.identity.value)) shouldBe expectedreversegroups
        }
      }

      "suppress ineligible navigation without deleting a relation or its exact provenance and conditions" in {
        Given("an admitted Mono relation whose source, target, relation, and navigation values retain independent limited conditions")
        val component = _component()
        val sourcecondition = _condition(authorization = "denied", redaction = Some("source-redaction"))
        val targetcondition = _condition(ambiguity = Some("target-ambiguity"))
        val relationcondition = _condition(limitations = Vector("bridge-boundary"))
        val sourceattribution = _attribution("source-subject", "source-scope", "source:subject")
        val referenceattribution = _attribution("source-reference", "reference-scope", "source:reference")
        val targetattribution = _attribution("target", "target-scope", "target:locator")
        val relationattribution = _attribution("relation", "relation-scope", "relation:locator")
        val projection = _projection(
          component,
          Vector(_subject(
            "mono-order",
            component,
            Mono,
            Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain, referenceattribution)),
            sourceattribution,
            sourcecondition
          ))
        )
        val target = _target(
          "aggregate-order",
          component,
          Aggregate,
          targetattribution,
          targetcondition,
          Some(_navigation("aggregate-order", component))
        )
        val admission = _admission("order-aggregate", "mono-order", "aggregate-order", target, relationattribution, relationcondition)

        When("the exact relation is admitted while its visibility conditions make navigation ineligible")
        val result = MonoKotoSemanticBridge.create(projection, Vector(admission))

        Then("the relation retains all original scope, attribution, locator, and condition values while exposing no navigation target")
        val relation = result.toOption.get.relations.head
        relation.navigationTarget shouldBe empty
        relation.sourceSubject.sourceSubject.attribution shouldBe sourceattribution
        relation.sourceSubject.sourceSubject.condition shouldBe sourcecondition
        relation.sourceReference.sourceReference.attribution shouldBe referenceattribution
        relation.target.attribution shouldBe targetattribution
        relation.target.condition shouldBe targetcondition
        relation.attribution shouldBe relationattribution
        relation.condition shouldBe relationcondition
      }

      "reject manually fabricated projected navigation without a partial bridge" in {
        Given("one otherwise valid Projection whose retained reference navigation is manually fabricated after admission")
        val component = _component()
        val projection = _projection(
          component,
          Vector(_subject(
            "mono-order",
            component,
            Mono,
            Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain))
          ))
        )
        val fabricatedprojection = projection.copy(
          subjects = projection.subjects.map { subject =>
            subject.copy(
              references = subject.references.map { reference =>
                reference.copy(navigationTarget = Some(_navigation("aggregate-order", component)))
              }
            )
          }
        )
        val admission = _admission(
          "order-aggregate",
          "mono-order",
          "aggregate-order",
          _target("aggregate-order", component, Aggregate)
        )

        When("the bridge factory receives the fabricated projected navigation")
        val result = MonoKotoSemanticBridge.create(fabricatedprojection, Vector(admission))

        Then("it returns MonoKotoSemanticBridgeFailure with no partial bridge and the precise projection-integrity violation")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto semantic bridge source reference 'aggregate-order' projected navigation target must not admit a disqualified target."
        )
      }

      "reject a manually fabricated blank source stakeholder label without a partial bridge" in {
        Given("one otherwise valid Projection whose raw source stakeholder label is manually blanked after admission")
        val component = _component()
        val projection = _projection(
          component,
          Vector(_subject(
            "mono-order",
            component,
            Mono,
            Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain))
          ))
        )
        val fabricatedprojection = projection.copy(
          subjects = projection.subjects.map { subject =>
            subject.copy(sourceSubject = subject.sourceSubject.copy(stakeholderLabel = " "))
          }
        )
        val admission = _admission(
          "order-aggregate",
          "mono-order",
          "aggregate-order",
          _target("aggregate-order", component, Aggregate)
        )

        When("the bridge factory receives the manually malformed raw source")
        val result = MonoKotoSemanticBridge.create(fabricatedprojection, Vector(admission))

        Then("it returns MonoKotoSemanticBridgeFailure with no partial bridge and the precise stakeholder-label integrity violation")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto semantic bridge source subject 'mono-order' stakeholder label must not be blank."
        )
      }

      "return typed no-partial failures for malformed exact admission boundaries" in {
        Given("one valid source Projection and admissions that violate one exact bridge boundary each")
        val component = _component()
        val projection = _projection(
          component,
          Vector(
            _subject("mono-order", component, Mono, Vector(_reference("aggregate-order", component, "aggregate-order", StructuralDomain))),
            _subject("koto-place", component, Koto, Vector(_reference("event-order", component, "event-order", BehavioralTemporal)))
          )
        )
        val missingreference = _admission("missing-reference", "mono-order", "missing", _target("aggregate-order", component, Aggregate))
        val incompatiblecategory = _admission("incompatible-category", "mono-order", "aggregate-order", _target("aggregate-order", component, Command))
        val outofscope = _admission("out-of-scope", "mono-order", "aggregate-order", _target("aggregate-order", _component("textus-other"), Aggregate))
        val malformedtarget = _admission("malformed-target", "mono-order", "aggregate-order", _target(" ", component, Aggregate))
        val duplicateone = _admission("duplicate", "mono-order", "aggregate-order", _target("aggregate-order", component, Aggregate))
        val duplicatetwo = _admission("duplicate", "koto-place", "event-order", _target("event-order", component, Event))
        val conflictingone = _admission("conflict-one", "mono-order", "aggregate-order", _target("aggregate-order", component, Aggregate))
        val conflictingtwo = _admission("conflict-two", "mono-order", "aggregate-order", _target("aggregate-order", component, Entity))

        When("each malformed admission set is supplied to the immutable factory")
        val missingresult = MonoKotoSemanticBridge.create(projection, Vector(missingreference))
        val incompatibleresult = MonoKotoSemanticBridge.create(projection, Vector(incompatiblecategory))
        val outofscoperesult = MonoKotoSemanticBridge.create(projection, Vector(outofscope))
        val malformedresult = MonoKotoSemanticBridge.create(projection, Vector(malformedtarget))
        val duplicateresult = MonoKotoSemanticBridge.create(projection, Vector(duplicateone, duplicatetwo))
        val conflictingresult = MonoKotoSemanticBridge.create(projection, Vector(conflictingone, conflictingtwo))

        Then("all malformed boundaries return only MonoKotoSemanticBridgeFailure rather than a partial ledger")
        Vector(missingresult, incompatibleresult, outofscoperesult, malformedresult, duplicateresult, conflictingresult).map(_.isLeft) shouldBe
          Vector(true, true, true, true, true, true)
        Vector(missingresult, incompatibleresult, outofscoperesult, malformedresult, duplicateresult, conflictingresult).flatMap(_.toOption) shouldBe empty
        missingresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto semantic bridge admission 'missing-reference' does not name an admitted exact source reference."
        )
        incompatibleresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto semantic bridge admission 'incompatible-category' target category is incompatible with source subject 'mono-order'."
        )
        duplicateresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Mono-Koto semantic bridge admission identity 'duplicate'."
        )
        conflictingresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Mono-Koto semantic bridge target identity 'aggregate-order' has contradictory admitted descriptors."
        )
      }
    }
  }

  private def _context(): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity("textus-order-ccdm")

  private def _component(componentid: String = "textus-order"): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _attribution(
    sourceid: String = "source",
    authorityscope: String = "authority",
    sourcelocator: String = "source:locator"
  ): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, authorityscope, sourcelocator)

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
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      _attribution("navigation", "navigation-authority", "navigation:locator"),
      condition,
      implementedandusable
    )

  private def _reference(
    referenceid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    kind: MonoKotoProjectionReferenceKind,
    attribution: ComponentDashboardSourceAttribution = _attribution(),
    condition: ComponentDashboardCondition = _condition()
  ): MonoKotoProjectionReference =
    MonoKotoProjectionReference(
      referenceid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      kind,
      attribution,
      condition,
      None,
      None
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    kind: MonoKotoProjectionKind,
    references: Vector[MonoKotoProjectionReference],
    attribution: ComponentDashboardSourceAttribution = _attribution(),
    condition: ComponentDashboardCondition = _condition()
  ): MonoKotoProjectionSubject =
    MonoKotoProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(subjectid),
      kind,
      subjectid,
      attribution,
      condition,
      references,
      None,
      None
    )

  private def _projection(
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[MonoKotoProjectionSubject]
  ): MonoKotoProjection =
    MonoKotoProjection.create(_context(), component, subjects).toOption.get

  private def _target(
    identity: String,
    component: ComponentDashboardComponentIdentity,
    category: MonoKotoSemanticBridgeTargetCategory,
    attribution: ComponentDashboardSourceAttribution = _attribution("target", "target-authority", "target:locator"),
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): MonoKotoSemanticBridgeTarget =
    MonoKotoSemanticBridgeTarget(
      ComponentDashboardSemanticTargetIdentity(identity),
      category,
      component,
      attribution,
      condition,
      None,
      navigation
    )

  private def _admission(
    admissionid: String,
    subjectid: String,
    referenceid: String,
    target: MonoKotoSemanticBridgeTarget,
    attribution: ComponentDashboardSourceAttribution = _attribution("relation", "relation-authority", "relation:locator"),
    condition: ComponentDashboardCondition = _condition()
  ): MonoKotoSemanticBridgeAdmission =
    MonoKotoSemanticBridgeAdmission(
      admissionid,
      subjectid,
      referenceid,
      target,
      attribution,
      condition,
      None
    )
}
