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
final class ClassificationViewProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "ClassificationViewProjection" should {
    "P9-42B pure caller-admitted Classification projection" which {
      "retain independent relationship roles dimensions and dimension assertions without inference" in {
        Given("one exact Component with four independent relationship roles and two independent powertype dimensions")
        val component = _component()
        val generalization = _relationship(
          "relationship-generalization",
          component,
          "taxonomy-generalization",
          "subject-order",
          "endpoint-order",
          "endpoint-purchase-order",
          ClassificationGeneralization
        )
        val specialization = _relationship(
          "relationship-specialization",
          component,
          "taxonomy-specialization",
          "subject-order",
          "endpoint-order",
          "endpoint-online-order",
          ClassificationSpecialization
        )
        val traitrelation = _relationship(
          "relationship-trait",
          component,
          "taxonomy-trait",
          "subject-order",
          "endpoint-order",
          "endpoint-auditable",
          ClassificationTrait,
          condition = _condition(conflict = Some("retained-trait-conflict"))
        )
        val category = _relationship(
          "relationship-category",
          component,
          "taxonomy-category",
          "subject-order",
          "endpoint-order",
          "endpoint-fulfilment-category",
          ClassificationCategory
        )
        val lifecycle = _dimension(
          "dimension-lifecycle",
          component,
          "powertype-lifecycle",
          "subject-order",
          Vector(
            _assertion("assertion-lifecycle-value", component, "powertype-lifecycle", "value-draft", ClassificationDimensionValue, subjectid = Some("subject-order")),
            _assertion("assertion-lifecycle-coverage", component, "powertype-lifecycle", "coverage-lifecycle", ClassificationDimensionCoverage, relationshipid = Some("taxonomy-generalization"))
          )
        )
        val channel = _dimension(
          "dimension-channel",
          component,
          "powertype-channel",
          "subject-order",
          Vector(
            _assertion("assertion-channel-qualifier", component, "powertype-channel", "qualifier-digital", ClassificationDimensionQualifier, endpointid = Some("endpoint-online-order")),
            _assertion("assertion-channel-exclusivity", component, "powertype-channel", "exclusivity-channel", ClassificationDimensionExclusivity, subjectid = Some("subject-order")),
            _assertion("assertion-channel-membership", component, "powertype-channel", "membership-channel", ClassificationDimensionMembership, relationshipid = Some("taxonomy-category"))
          )
        )

        When("the caller supplies the exact already-admitted Classification ledger")
        val result = ClassificationViewProjection.create(
          _context(),
          component,
          Vector(category, traitrelation, specialization, generalization),
          Vector(channel, lifecycle),
          Vector.empty
        )

        Then("every independent identity role dimension assertion provenance locator and condition remains distinct")
        val projected = result.toOption.get
        projected.relationships.map(_.sourceRelationship.role).toSet shouldBe Set(
          ClassificationGeneralization,
          ClassificationSpecialization,
          ClassificationTrait,
          ClassificationCategory
        )
        projected.dimensions.map(_.sourceDimension.semanticDimensionId.value).toSet shouldBe Set("powertype-lifecycle", "powertype-channel")
        projected.dimensions.flatMap(_.assertions.map(_.sourceAssertion.role)).toSet shouldBe Set(
          ClassificationDimensionValue,
          ClassificationDimensionQualifier,
          ClassificationDimensionExclusivity,
          ClassificationDimensionCoverage,
          ClassificationDimensionMembership
        )
        projected.relationships.find(_.sourceRelationship.id == "relationship-trait").map(_.sourceRelationship.condition.conflict) shouldBe
          Some(Some("retained-trait-conflict"))
        projected.dimensions.flatMap(_.assertions.map(_.sourceAssertion.attribution)).toSet should contain(
          ComponentDashboardSourceAttribution("assertion-assertion-lifecycle-value", "assertion-authority", "assertion:assertion-lifecycle-value")
        )
      }

      "retain attributable gaps and every independent condition without generating taxonomy data or navigation" in {
        Given("one admitted taxonomy relationship and gaps carrying each distinct bounded condition")
        val component = _component()
        val relationship = _relationship(
          "relationship-order",
          component,
          "taxonomy-order",
          "subject-order",
          "endpoint-order",
          "endpoint-purchase-order",
          ClassificationGeneralization
        )
        val dimension = _dimension("dimension-status", component, "powertype-status", "subject-order", Vector.empty)
        val conditionedgaps = Vector(
          _gap("gap-unavailable", component, ClassificationDimensionValue, "cozy:status/value", condition = _condition(availability = "unavailable")),
          _gap("gap-unauthorized", component, ClassificationDimensionValue, "cozy:status/value", condition = _condition(authorization = "denied")),
          _gap("gap-redacted", component, ClassificationDimensionQualifier, "cozy:status/qualifier", condition = _condition(redaction = Some("restricted"))),
          _gap("gap-absence", component, ClassificationDimensionExclusivity, "cozy:status/exclusivity", condition = _condition(explicitabsence = Some("bounded-absence"))),
          _gap("gap-ambiguous", component, ClassificationDimensionCoverage, "cozy:status/coverage", condition = _condition(ambiguity = Some("two-admitted-values"))),
          _gap("gap-conflicting", component, ClassificationDimensionMembership, "cozy:status/membership", condition = _condition(conflict = Some("attributed-conflict"))),
          _gap("gap-stale", component, ClassificationPowertypeDimension, "cozy:status/dimension", condition = _condition(staleness = Some("not-current"))),
          _gap("gap-malformed", component, ClassificationTrait, "cozy:taxonomy/trait", condition = _condition(malformedevidence = Some("unparseable"))),
          _gap("gap-limited", component, ClassificationCategory, "cozy:taxonomy/category", condition = _condition(limitations = Vector("provider-boundary")))
        )

        When("the caller projects only already-admitted unavailable or conditioned material")
        val result = ClassificationViewProjection.create(_context(), component, Vector(relationship), Vector(dimension), conditionedgaps)

        Then("each gap and condition is retained without an inferred relation dimension assertion or navigation")
        result.toOption.map(_.gaps.map(_.sourceGap.id).toSet) shouldBe Some(conditionedgaps.map(_.id).toSet)
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.affectedSemanticRelationshipId)) shouldBe Vector.fill(9)(None)
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.condition.availability)) should contain("unavailable")
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.condition.authorization)) should contain("denied")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.redaction)) should contain("restricted")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.explicitAbsence)) should contain("bounded-absence")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.ambiguity)) should contain("two-admitted-values")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.conflict)) should contain("attributed-conflict")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.staleness)) should contain("not-current")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.malformedEvidence)) should contain("unparseable")
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.sourceGap.condition.limitations)) should contain("provider-boundary")
      }

      "present relationship dimension assertion and gap permutations by exact admitted identities only" in {
        Given("reverse-ordered relationships dimensions dependent assertions and gaps")
        val component = _component()
        val relationships = Vector(
          _relationship("relationship-zulu", component, "taxonomy-zulu", "subject-order", "endpoint-order", "endpoint-zulu", ClassificationCategory),
          _relationship("relationship-alpha", component, "taxonomy-alpha", "subject-order", "endpoint-order", "endpoint-alpha", ClassificationGeneralization),
          _relationship("relationship-middle", component, "taxonomy-middle", "subject-order", "endpoint-order", "endpoint-middle", ClassificationTrait),
          _relationship("relationship-beta", component, "taxonomy-beta", "subject-order", "endpoint-order", "endpoint-beta", ClassificationSpecialization)
        )
        val alphaassertion = _assertion("assertion-alpha", component, "powertype-alpha", "assertion-alpha", ClassificationDimensionValue, subjectid = Some("subject-order"))
        val zuluassertion = _assertion("assertion-zulu", component, "powertype-alpha", "assertion-zulu", ClassificationDimensionQualifier, endpointid = Some("endpoint-alpha"))
        val dimensions = Vector(
          _dimension("dimension-zulu", component, "powertype-zulu", "subject-order", Vector.empty),
          _dimension("dimension-alpha", component, "powertype-alpha", "subject-order", Vector(zuluassertion, alphaassertion))
        )
        val gaps = Vector(
          _gap("gap-zulu", component, ClassificationDimensionCoverage, "cozy:zulu"),
          _gap("gap-alpha", component, ClassificationDimensionMembership, "cozy:alpha")
        )
        val expectedrelationships = relationships.sortBy { relationship =>
          (
            relationship.semanticRelationshipId.value,
            relationship.subject.semanticTargetId.value,
            relationship.sourceEndpoint.semanticTargetId.value,
            relationship.targetEndpoint.semanticTargetId.value,
            relationship.id,
            relationship.stableTieKey.getOrElse("")
          )
        }.map(_.semanticRelationshipId.value)
        val expecteddimensions = dimensions.sortBy { dimension =>
          (
            dimension.semanticDimensionId.value,
            dimension.boundedSubject.semanticTargetId.value,
            dimension.id,
            dimension.stableTieKey.getOrElse("")
          )
        }.map(_.semanticDimensionId.value)
        val expectedassertions = Vector(zuluassertion, alphaassertion).sortBy { assertion =>
          (
            assertion.semanticDimensionId.value,
            assertion.semanticAssertionId.value,
            assertion.affectedSemanticRelationshipId.map(_.value).getOrElse(""),
            assertion.affectedSubjectId.map(_.value).getOrElse(""),
            assertion.affectedEndpointId.map(_.value).getOrElse(""),
            assertion.id,
            assertion.stableTieKey.getOrElse("")
          )
        }.map(_.id)
        val expectedgaps = gaps.sortBy { gap =>
          (
            gap.id,
            gap.affectedSemanticRelationshipId.map(_.value).getOrElse(""),
            gap.affectedSubjectId.map(_.value).getOrElse(""),
            gap.affectedEndpointId.map(_.value).getOrElse(""),
            gap.affectedDimensionId.map(_.value).getOrElse(""),
            gap.stableTieKey.getOrElse("")
          )
        }.map(_.id)

        forAll(Gen.pick(4, relationships), Gen.oneOf(true, false)) { (permutedrelationships, reversevalues) =>
          val callerdimensions = if (reversevalues)
            dimensions.reverse.map(dimension => if (dimension.id == "dimension-alpha") dimension.copy(assertions = dimension.assertions.reverse) else dimension)
          else
            dimensions
          val callergaps = if (reversevalues) gaps.reverse else gaps

          When("a property-generated caller permutation and reverse dependent vectors are projected")
          val result = ClassificationViewProjection.create(_context(), component, permutedrelationships.toVector, callerdimensions, callergaps)

          Then("only exact identities and admitted non-semantic tie keys determine presentation")
          result.toOption.toVector.flatMap(_.relationships.map(_.sourceRelationship.semanticRelationshipId.value)) shouldBe expectedrelationships
          result.toOption.toVector.flatMap(_.dimensions.map(_.sourceDimension.semanticDimensionId.value)) shouldBe expecteddimensions
          result.toOption.toVector.flatMap(_.dimensions.find(_.sourceDimension.id == "dimension-alpha").toVector.flatMap(_.assertions.map(_.sourceAssertion.id))) shouldBe
            expectedassertions
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe expectedgaps
        }
      }

      "gate exact forward and reverse navigation without deleting retained records" in {
        Given("relationships whose forward and reverse targets differ by exact target Component receiver and condition gates")
        val component = _component()
        val usable = _relationship(
          "relationship-usable",
          component,
          "taxonomy-usable",
          "subject-order",
          "endpoint-order",
          "endpoint-purchase-order",
          ClassificationGeneralization,
          forwardnavigation = Some(_navigation("endpoint-purchase-order", component)),
          reversenavigation = Some(_navigation("endpoint-order", component))
        )
        val targetmismatch = _relationship(
          "relationship-mismatch",
          component,
          "taxonomy-mismatch",
          "subject-order",
          "endpoint-order",
          "endpoint-online-order",
          ClassificationSpecialization,
          forwardnavigation = Some(_navigation("endpoint-unrelated", component)),
          reversenavigation = Some(_navigation("endpoint-order", _component("textus-other")))
        )
        val receiverunavailable = _relationship(
          "relationship-receiver",
          component,
          "taxonomy-receiver",
          "subject-order",
          "endpoint-order",
          "endpoint-auditable",
          ClassificationTrait,
          forwardnavigation = Some(_navigation("endpoint-auditable", component, implementedandusable = false)),
          reversenavigation = Some(_navigation("endpoint-order", component, condition = _condition(ambiguity = Some("no-target-discriminator"))))
        )

        When("the exact retained relationships are projected")
        val result = ClassificationViewProjection.create(_context(), component, Vector(targetmismatch, receiverunavailable, usable), Vector.empty, Vector.empty)

        Then("only exact same-Component implemented and condition-usable forward or reverse targets are exposed")
        result.toOption.toVector.flatMap(_.relationships.find(_.sourceRelationship.id == "relationship-usable").toVector.flatMap(_.forwardNavigationTarget)) shouldBe
          Vector(_navigation("endpoint-purchase-order", component))
        result.toOption.toVector.flatMap(_.relationships.find(_.sourceRelationship.id == "relationship-usable").toVector.flatMap(_.reverseNavigationTarget)) shouldBe
          Vector(_navigation("endpoint-order", component))
        result.toOption.toVector.flatMap(_.relationships.filterNot(_.sourceRelationship.id == "relationship-usable").flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.relationships.filterNot(_.sourceRelationship.id == "relationship-usable").flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
        result.toOption.map(_.relationships.map(_.sourceRelationship).toSet) shouldBe Some(Set(targetmismatch, receiverunavailable, usable))
      }

      "return a typed no-partial failure for scope identity role dimension reference affected endpoint attribution locator condition tie limitation and malformed navigation" in {
        Given("otherwise caller-admitted records with independent boundary violations")
        val component = _component()
        val malformedrelationship = _relationship(
          "relationship-order",
          component,
          "taxonomy-order",
          "subject-order",
          "endpoint-order",
          "endpoint-purchase-order",
          ClassificationDimensionValue,
          forwardnavigation = Some(_navigation(" ", component))
        )
        val duplicateidentity = _relationship(
          "relationship-duplicate",
          component,
          "taxonomy-order",
          "subject-order",
          "endpoint-order",
          "endpoint-online-order",
          ClassificationGeneralization,
          attribution = ComponentDashboardSourceAttribution("relationship-source", "relationship-authority", " ")
        )
        val invalidassertion = _assertion(
          "assertion-order",
          component,
          "powertype-wrong",
          "assertion-order",
          ClassificationGeneralization,
          endpointid = Some("endpoint-unknown"),
          condition = _condition(redaction = Some(" "))
        )
        val duplicateassertion = _assertion(
          "assertion-order",
          component,
          "powertype-status",
          "assertion-order",
          ClassificationDimensionValue
        )
        val malformeddimension = _dimension(
          "dimension-status",
          component,
          "powertype-status",
          "subject-order",
          Vector(invalidassertion, duplicateassertion),
          role = ClassificationTrait
        )
        val duplicatedimension = _dimension("dimension-duplicate", component, "powertype-status", "subject-order", Vector.empty)
        val blanktie = _gap(
          "gap-duplicate",
          component,
          ClassificationDimensionCoverage,
          "cozy:status/coverage",
          limitation = " ",
          tiekey = Some(" ")
        )
        val duplicategap = _gap("gap-duplicate", component, ClassificationDimensionMembership, "cozy:status/membership")
        val outofscope = _gap("gap-scope", _component("textus-other"), ClassificationCategory, "cozy:category")

        When("the immutable factory validates all supplied violations before projecting")
        val result = ClassificationViewProjection.create(
          _context(" "),
          component,
          Vector(malformedrelationship, duplicateidentity),
          Vector(malformeddimension, duplicatedimension),
          Vector(blanktie, duplicategap, outofscope)
        )

        Then("it returns ClassificationViewProjectionFailure with no partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification View Projection bounded CCDM context identity must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Classification semantic relationship identity 'taxonomy-order'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification relationship 'relationship-order' has non-relationship role ClassificationDimensionValue."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification relationship 'relationship-duplicate' source locator must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Classification semantic dimension identity 'powertype-status'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification dimension 'dimension-status' has non-dimension role ClassificationTrait."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification assertion 'assertion-order' must retain dimension 'dimension-status' exact semantic dimension identity."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification assertion 'assertion-order' affected endpoint identity 'endpoint-unknown' is not retained in this Projection."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification assertion 'assertion-order' has non-assertion role ClassificationGeneralization."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Classification dimension assertion identity 'assertion-order'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification assertion 'assertion-order' condition redaction must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Classification gap identity 'gap-duplicate'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification gap 'gap-duplicate' limitation reason must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification gap 'gap-duplicate' stable non-semantic tie key must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification gap 'gap-scope' is outside Projection Component 'textus-classification'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Classification relationship 'relationship-order' forward navigation target identity must not be blank."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-classification-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-classification"): ComponentDashboardComponentIdentity =
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

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    condition: ComponentDashboardCondition = _condition()
  ): ClassificationViewSubject =
    ClassificationViewSubject(
      ComponentDashboardSemanticTargetIdentity(subjectid),
      component,
      _attribution(s"subject-$subjectid", "subject-authority", s"subject:$subjectid"),
      condition
    )

  private def _endpoint(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    endpointrole: String,
    condition: ComponentDashboardCondition = _condition()
  ): ClassificationViewEndpoint =
    ClassificationViewEndpoint(
      ComponentDashboardSemanticTargetIdentity(endpointid),
      component,
      endpointrole,
      _attribution(s"endpoint-$endpointid", "endpoint-authority", s"endpoint:$endpointid"),
      condition
    )

  private def _relationship(
    relationshipid: String,
    component: ComponentDashboardComponentIdentity,
    semanticrelationshipid: String,
    subjectid: String,
    sourceendpointid: String,
    targetendpointid: String,
    role: ClassificationViewProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    forwardnavigation: Option[ComponentDashboardNavigationTarget] = None,
    reversenavigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "relationship-source",
      "relationship-authority",
      "relationship:locator"
    )
  ): ClassificationViewRelationship =
    ClassificationViewRelationship(
      relationshipid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticrelationshipid),
      _subject(subjectid, component),
      _endpoint(sourceendpointid, component, "source"),
      _endpoint(targetendpointid, component, "target"),
      role,
      attribution,
      condition,
      None,
      forwardnavigation,
      reversenavigation
    )

  private def _assertion(
    assertionid: String,
    component: ComponentDashboardComponentIdentity,
    dimensionid: String,
    semanticassertionid: String,
    role: ClassificationViewProjectionRole,
    relationshipid: Option[String] = None,
    subjectid: Option[String] = None,
    endpointid: Option[String] = None,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): ClassificationViewDimensionAssertion =
    ClassificationViewDimensionAssertion(
      assertionid,
      component,
      ComponentDashboardSemanticTargetIdentity(dimensionid),
      ComponentDashboardSemanticTargetIdentity(semanticassertionid),
      relationshipid.map(ComponentDashboardSemanticTargetIdentity),
      subjectid.map(ComponentDashboardSemanticTargetIdentity),
      endpointid.map(ComponentDashboardSemanticTargetIdentity),
      role,
      _attribution(s"assertion-$assertionid", "assertion-authority", s"assertion:$assertionid"),
      condition,
      None,
      navigation
    )

  private def _dimension(
    dimensionid: String,
    component: ComponentDashboardComponentIdentity,
    semanticdimensionid: String,
    subjectid: String,
    assertions: Vector[ClassificationViewDimensionAssertion],
    role: ClassificationViewProjectionRole = ClassificationPowertypeDimension,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): ClassificationViewDimension =
    ClassificationViewDimension(
      dimensionid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticdimensionid),
      _subject(subjectid, component),
      role,
      _attribution(s"dimension-$dimensionid", "dimension-authority", s"dimension:$dimensionid"),
      condition,
      assertions,
      None,
      navigation
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: ClassificationViewProjectionRole,
    boundedfieldorscope: String,
    relationshipid: Option[String] = None,
    subjectid: Option[String] = None,
    endpointid: Option[String] = None,
    dimensionid: Option[String] = None,
    condition: ComponentDashboardCondition = _condition(),
    limitation: String = "The admitted bounded material cannot support this taxonomy field.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): ClassificationViewGap =
    ClassificationViewGap(
      gapid,
      component,
      requestedrole,
      boundedfieldorscope,
      relationshipid.map(ComponentDashboardSemanticTargetIdentity),
      subjectid.map(ComponentDashboardSemanticTargetIdentity),
      endpointid.map(ComponentDashboardSemanticTargetIdentity),
      dimensionid.map(ComponentDashboardSemanticTargetIdentity),
      _attribution(s"gap-$gapid", "gap-authority", s"gap:$gapid"),
      condition,
      limitation,
      tiekey,
      navigation
    )
}
