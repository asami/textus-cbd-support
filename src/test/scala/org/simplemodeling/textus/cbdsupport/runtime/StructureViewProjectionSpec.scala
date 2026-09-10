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
final class StructureViewProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "StructureViewProjection" should {
    "P9-41B pure caller-admitted Structure projection" which {
      "retain every admitted relation and independent assertion role with exact endpoints provenance locator and condition" in {
        Given("one exact Component with all admitted static relationship and independent assertion roles")
        val component = _component()
        val composition = _relation(
          "relation-composition",
          component,
          "relationship-composition",
          "endpoint-order",
          "endpoint-line",
          StructureComposition,
          Vector(_assertion("assertion-independent", component, "relationship-composition", Some("endpoint-line"), IndependentExistence))
        )
        val aggregation = _relation(
          "relation-aggregation",
          component,
          "relationship-aggregation",
          "endpoint-order",
          "endpoint-items",
          StructureAggregation,
          Vector(_assertion("assertion-reassignment", component, "relationship-aggregation", Some("endpoint-items"), Reassignment))
        )
        val association = _relation(
          "relation-association",
          component,
          "relationship-association",
          "endpoint-order",
          "endpoint-customer",
          StructureAssociation,
          Vector(_assertion("assertion-deletion", component, "relationship-association", None, DeletionLifecycle))
        )
        val containment = _relation(
          "relation-containment",
          component,
          "relationship-containment",
          "endpoint-catalog",
          "endpoint-category",
          StructureContainment,
          Vector(_assertion("assertion-cardinality", component, "relationship-containment", Some("endpoint-category"), Cardinality))
        )
        val ownershipcondition = _condition(conflict = Some("retained-ownership-conflict"))
        val ownership = _relation(
          "relation-ownership",
          component,
          "relationship-ownership",
          "endpoint-order",
          "endpoint-owner",
          StructureOwnership,
          Vector(_assertion("assertion-navigability", component, "relationship-ownership", Some("endpoint-owner"), Navigability)),
          condition = ownershipcondition
        )

        When("the caller supplies the exact already-admitted Structure ledger")
        val result = StructureViewProjection.create(
          _context(),
          component,
          Vector(ownership, containment, association, aggregation, composition),
          Vector.empty
        )

        Then("all supplied identities roles attribution locators and independent conditions remain distinct")
        val projected = result.toOption.get
        projected.relations.map(_.sourceRelation.role).toSet shouldBe Set(
          StructureComposition,
          StructureAggregation,
          StructureAssociation,
          StructureContainment,
          StructureOwnership
        )
        projected.relations.flatMap(_.assertions.map(_.sourceAssertion.role)).toSet shouldBe Set(
          IndependentExistence,
          Reassignment,
          DeletionLifecycle,
          Cardinality,
          Navigability
        )
        projected.relations.find(_.sourceRelation.id == "relation-ownership").map(_.sourceRelation) shouldBe Some(ownership)
        projected.relations.flatMap(_.assertions.map(_.sourceAssertion.attribution)).toSet should contain(
          ComponentDashboardSourceAttribution("assertion-assertion-independent", "assertion-authority", "assertion:assertion-independent")
        )
        projected.relations.find(_.sourceRelation.id == "relation-ownership").map(_.sourceRelation.condition) shouldBe Some(ownershipcondition)
      }

      "retain an attributable bounded gap without inventing an endpoint value proxy or navigation" in {
        Given("one admitted relation and a bounded Cozy field gap with no known relation or endpoint")
        val component = _component()
        val relation = _relation(
          "relation-order-customer",
          component,
          "relationship-order-customer",
          "endpoint-order",
          "endpoint-customer",
          StructureAssociation,
          Vector.empty
        )
        val gapcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-provider-no-cardinality-field"),
          limitations = Vector("cozy-field-boundary")
        )
        val gap = _gap(
          "gap-cardinality-field",
          component,
          Cardinality,
          "cozy:order-customer/cardinality",
          None,
          None,
          condition = gapcondition,
          reason = "The admitted Cozy field does not supply the requested cardinality assertion."
        )

        When("the unavailable bounded field is projected")
        val result = StructureViewProjection.create(_context(), component, Vector(relation), Vector(gap))

        Then("the limitation remains explicit without an inferred relation endpoint or navigation target")
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.map(_.gaps.head.sourceGap.affectedSemanticRelationshipId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.affectedEndpointId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.condition) shouldBe Some(gapcondition)
      }

      "present full relation permutations and reversed assertions and gaps by exact identities only" in {
        Given("five reverse-ordered relation roles with reverse assertion and gap vectors")
        val component = _component()
        val assertionalpha = _assertion("assertion-alpha", component, "relationship-association", Some("endpoint-customer"), Cardinality)
        val assertionzulu = _assertion("assertion-zulu", component, "relationship-association", Some("endpoint-order"), Navigability)
        val relations = Vector(
          _relation("relation-zulu", component, "relationship-ownership", "endpoint-order", "endpoint-owner", StructureOwnership, Vector.empty),
          _relation("relation-middle", component, "relationship-containment", "endpoint-catalog", "endpoint-category", StructureContainment, Vector.empty),
          _relation("relation-alpha", component, "relationship-association", "endpoint-order", "endpoint-customer", StructureAssociation, Vector(assertionzulu, assertionalpha)),
          _relation("relation-aggregation", component, "relationship-aggregation", "endpoint-order", "endpoint-items", StructureAggregation, Vector.empty),
          _relation("relation-composition", component, "relationship-composition", "endpoint-order", "endpoint-line", StructureComposition, Vector.empty)
        )
        val gaps = Vector(
          _gap("gap-zulu", component, Cardinality, "cozy:zulu", None, None),
          _gap("gap-alpha", component, Navigability, "cozy:alpha", None, None)
        )
        val expectedrelations = relations.sortBy { relation =>
          (
            relation.semanticRelationshipId.value,
            relation.sourceEndpointId.value,
            relation.targetEndpointId.value,
            relation.id,
            relation.stableTieKey.getOrElse("")
          )
        }.map(_.semanticRelationshipId.value)
        val expectedassertions = Vector(assertionzulu, assertionalpha).sortBy { assertion =>
          (
            assertion.semanticRelationshipId.value,
            assertion.id,
            assertion.affectedEndpointId.map(_.value).getOrElse(""),
            assertion.stableTieKey.getOrElse("")
          )
        }.map(_.id)
        val expectedgaps = gaps.sortBy(gap => (gap.id, gap.stableTieKey.getOrElse(""))).map(_.id)

        forAll(Gen.pick(5, relations), Gen.oneOf(true, false)) { (permutedrelations, reversevalues) =>
          val callerrelations = permutedrelations.toVector.map { relation =>
            if (relation.id == "relation-alpha" && reversevalues)
              relation.copy(assertions = relation.assertions.reverse)
            else
              relation
          }
          val callergaps = if (reversevalues) gaps.reverse else gaps

          When("a property-generated full relation permutation and reversed dependent vectors are projected")
          val result = StructureViewProjection.create(_context(), component, callerrelations, callergaps)

          Then("only exact identities and admitted tie keys determine presentation")
          result.toOption.toVector.flatMap(_.relations.map(_.sourceRelation.semanticRelationshipId.value)) shouldBe expectedrelations
          result.toOption.toVector.flatMap(_.relations.find(_.sourceRelation.id == "relation-alpha").toVector.flatMap(_.assertions.map(_.sourceAssertion.id))) shouldBe
            expectedassertions
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe expectedgaps
        }
      }

      "admit only exact usable target-endpoint navigation while retaining conditioned records" in {
        Given("a relation assertion and gap whose exact targets differ by independent navigation gates")
        val component = _component()
        val relation = _relation(
          "relation-order-customer",
          component,
          "relationship-order-customer",
          "endpoint-order",
          "endpoint-customer",
          StructureAssociation,
          Vector(
            _assertion(
              "assertion-cardinality",
              component,
              "relationship-order-customer",
              Some("endpoint-customer"),
              Cardinality,
              condition = _condition(authorization = "denied"),
              navigation = Some(_navigation("endpoint-customer", component))
            )
          ),
          navigation = Some(_navigation("endpoint-customer", component))
        )
        val gap = _gap(
          "gap-navigability",
          component,
          Navigability,
          "cozy:order-customer/navigability",
          Some("relationship-order-customer"),
          Some("endpoint-customer"),
          navigation = Some(_navigation("endpoint-customer", component, implementedandusable = false))
        )

        When("the exact retained target endpoints are projected")
        val result = StructureViewProjection.create(_context(), component, Vector(relation), Vector(gap))

        Then("only the exact implemented usable relation target remains navigable and all records remain retained")
        result.toOption.toVector.flatMap(_.relations.flatMap(_.navigationTarget)) shouldBe
          Vector(_navigation("endpoint-customer", component))
        result.toOption.toVector.flatMap(_.relations.flatMap(_.assertions).flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.relations.flatMap(_.assertions).map(_.sourceAssertion)) shouldBe relation.assertions
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
      }

      "return a typed no-partial failure for invalid role placement scope identity locator endpoint and ordering input" in {
        Given("otherwise admitted records with invalid role placement duplicate identities a cross-Component gap and malformed boundaries")
        val component = _component()
        val malformedrelation = _relation(
          "relation-order-customer",
          component,
          "relationship-order-customer",
          "endpoint-order",
          "endpoint-customer",
          IndependentExistence,
          Vector(
            _assertion("assertion-order", component, "relationship-wrong", Some("endpoint-unknown"), StructureAssociation),
            _assertion("assertion-order", component, "relationship-order-customer", Some("endpoint-customer"), Cardinality)
          )
        )
        val duplicateidentity = _relation(
          "relation-duplicate",
          component,
          "relationship-order-customer",
          "endpoint-order",
          "endpoint-payment",
          StructureAssociation,
          Vector.empty,
          attribution = ComponentDashboardSourceAttribution("relation-source", "relation-authority", " ")
        )
        val blanktiekey = _gap(
          "gap-duplicate",
          component,
          Cardinality,
          "cozy:order/cardinality",
          Some("relationship-order-customer"),
          Some("endpoint-customer"),
          tiekey = Some(" ")
        )
        val duplicategap = _gap("gap-duplicate", component, Navigability, "cozy:order/navigation", None, None)
        val outofscope = _gap("gap-scope", _component("textus-payment"), Cardinality, "cozy:payment/cardinality", None, None)

        When("the immutable factory validates all supplied boundary violations")
        val result = StructureViewProjection.create(
          _context(),
          component,
          Vector(malformedrelation, duplicateidentity),
          Vector(blanktiekey, duplicategap, outofscope)
        )

        Then("it returns StructureViewProjectionFailure with no partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View relation 'relation-order-customer' has assertion role IndependentExistence."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View assertion 'assertion-order' must retain relation 'relation-order-customer' exact semantic relationship identity."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View assertion 'assertion-order' affected endpoint must be retained by relation 'relation-order-customer'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View assertion 'assertion-order' has relation role StructureAssociation."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Structure View assertion identity 'assertion-order'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Structure View semantic relationship identity 'relationship-order-customer'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View relation 'relation-duplicate' source locator must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Structure View gap identity 'gap-duplicate'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View gap 'gap-duplicate' stable non-semantic tie key must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Structure View gap 'gap-scope' is outside Projection Component 'textus-structure'."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-structure-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-structure"): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _attribution(
    sourceid: String,
    authorityscope: String,
    sourcelocator: String
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
      _attribution("navigation-source", "navigation-authority", "navigation:locator"),
      condition,
      implementedandusable
    )

  private def _assertion(
    assertionid: String,
    component: ComponentDashboardComponentIdentity,
    relationshipid: String,
    endpointid: Option[String],
    role: StructureViewProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): StructureViewAssertion =
    StructureViewAssertion(
      assertionid,
      component,
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      endpointid.map(ComponentDashboardSemanticTargetIdentity),
      role,
      _attribution(s"assertion-$assertionid", "assertion-authority", s"assertion:$assertionid"),
      condition,
      None,
      navigation
    )

  private def _relation(
    relationid: String,
    component: ComponentDashboardComponentIdentity,
    relationshipid: String,
    sourceendpointid: String,
    targetendpointid: String,
    role: StructureViewProjectionRole,
    assertions: Vector[StructureViewAssertion],
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "relation-source",
      "relation-authority",
      "relation:locator"
    )
  ): StructureViewRelation =
    StructureViewRelation(
      relationid,
      component,
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      ComponentDashboardSemanticTargetIdentity(sourceendpointid),
      ComponentDashboardSemanticTargetIdentity(targetendpointid),
      role,
      attribution,
      condition,
      assertions,
      None,
      navigation
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: StructureViewProjectionRole,
    boundedfieldorscope: String,
    relationshipid: Option[String],
    endpointid: Option[String],
    condition: ComponentDashboardCondition = _condition(),
    reason: String = "The admitted bounded material cannot support this field.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): StructureViewGap =
    StructureViewGap(
      gapid,
      component,
      requestedrole,
      boundedfieldorscope,
      relationshipid.map(ComponentDashboardSemanticTargetIdentity),
      endpointid.map(ComponentDashboardSemanticTargetIdentity),
      _attribution(s"gap-$gapid", "gap-authority", s"gap:$gapid"),
      condition,
      reason,
      tiekey,
      navigation
    )
}
