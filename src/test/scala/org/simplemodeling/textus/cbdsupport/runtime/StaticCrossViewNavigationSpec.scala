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
final class StaticCrossViewNavigationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "StaticCrossViewNavigation" should {
    "P9-43B typed immutable static cross-view navigation" which {
      "retain every caller-admitted static category and role without reconstructing records or mappings" in {
        Given("one exact Component and context with independently admitted Mono-Koto, Entity, Structure, and Classification records")
        val component = _component()
        val context = _context()
        val records = Vector(
          _record("mono-subject", context, component, "order", StaticMonoKoto, StaticMonoKotoSubject),
          _record("mono-reference", context, component, "order", StaticMonoKoto, StaticMonoKotoReference),
          _record("entity-subject", context, component, "order", StaticEntity, StaticEntitySubject),
          _record("entity-metadata", context, component, "order", StaticEntity, StaticEntityMetadata),
          _record("structure-relation", context, component, "order", StaticStructure, StaticStructureRelation),
          _record("structure-assertion", context, component, "order", StaticStructure, StaticStructureAssertion),
          _record("structure-endpoint", context, component, "order", StaticStructure, StaticStructureEndpoint),
          _record("classification-relation", context, component, "order", StaticClassification, StaticClassificationRelation),
          _record("classification-subject", context, component, "order", StaticClassification, StaticClassificationSubject),
          _record("classification-endpoint", context, component, "order", StaticClassification, StaticClassificationEndpoint),
          _record("classification-dimension", context, component, "order", StaticClassification, StaticClassificationDimension),
          _record("classification-assertion", context, component, "order", StaticClassification, StaticClassificationAssertion)
        )
        val mappings = Vector(
          _mapping("mapping-mono-entity", context, component, records(0), records(2)),
          _mapping("mapping-entity-structure", context, component, records(3), records(4)),
          _mapping("mapping-structure-classification", context, component, records(5), records(7))
        )
        val gapcondition = _condition(availability = "unavailable", authorization = "denied", limitations = Vector("receiving-contract-not-implemented"))
        val gaps = Vector(
          _gap(
            "gap-mono-classification",
            context,
            component,
            records(1),
            StaticClassification,
            StaticClassificationDimension,
            Some(ComponentDashboardSemanticTargetIdentity("order")),
            gapcondition
          )
        )

        When("the caller-admitted identity index is assembled")
        val result = StaticCrossViewNavigation.create(context, component, records.reverse, mappings.reverse, gaps)

        Then("the exact records, roles, attribution locators, conditions, mappings, and bounded gap remain independently retained")
        val projectedrecords = result.toOption.toVector.flatMap(_.records)
        projectedrecords.map(record => record.category -> record.role).toSet shouldBe Set(
          StaticMonoKoto -> StaticMonoKotoSubject,
          StaticMonoKoto -> StaticMonoKotoReference,
          StaticEntity -> StaticEntitySubject,
          StaticEntity -> StaticEntityMetadata,
          StaticStructure -> StaticStructureRelation,
          StaticStructure -> StaticStructureAssertion,
          StaticStructure -> StaticStructureEndpoint,
          StaticClassification -> StaticClassificationRelation,
          StaticClassification -> StaticClassificationSubject,
          StaticClassification -> StaticClassificationEndpoint,
          StaticClassification -> StaticClassificationDimension,
          StaticClassification -> StaticClassificationAssertion
        )
        projectedrecords.map(_.id) shouldBe Vector(
          "mono-subject",
          "mono-reference",
          "entity-subject",
          "entity-metadata",
          "structure-relation",
          "structure-assertion",
          "structure-endpoint",
          "classification-relation",
          "classification-subject",
          "classification-endpoint",
          "classification-dimension",
          "classification-assertion"
        )
        projectedrecords.find(_.id == "entity-metadata").map(_.attribution.sourceLocator) shouldBe Some("source:entity-metadata")
        projectedrecords.find(_.id == "structure-assertion").map(_.condition) shouldBe Some(_condition())
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-mono-entity", "mapping-entity-structure", "mapping-structure-classification")
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceRecord.id)) shouldBe
          Vector("mono-subject", "entity-metadata", "structure-assertion")
        result.toOption.toVector.flatMap(_.mappings.map(_.counterpartRecord.id)) shouldBe
          Vector("entity-subject", "structure-relation", "classification-relation")
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap)) shouldBe gaps
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.knownCounterpartSemanticTargetId)) shouldBe
          Vector(Some(ComponentDashboardSemanticTargetIdentity("order")))
      }

      "retain attributable bounded gaps without inferring a counterpart, proxy, lookup, or navigation target" in {
        Given("one exact retained Mono-Koto source and an unavailable Classification counterpart gap")
        val component = _component()
        val context = _context()
        val source = _record("mono-subject", context, component, "order", StaticMonoKoto, StaticMonoKotoSubject)
        val condition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-source-does-not-state-a-classification-dimension"),
          limitations = Vector("classification-receiver-unimplemented")
        )
        val gap = _gap(
          "gap-classification-dimension",
          context,
          component,
          source,
          StaticClassification,
          StaticClassificationDimension,
          None,
          condition
        )

        When("the source and bounded unavailable gap are assembled")
        val result = StaticCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(gap))

        Then("the gap retains only its source and limitation information")
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap)) shouldBe Vector(gap)
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.knownCounterpartSemanticTargetId)) shouldBe Vector(None)
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "present records mappings and gaps by identity category role record identity and admitted tie key independently of caller permutation" in {
        Given("two independently admitted shared identities with records mappings and gaps in arbitrary caller order")
        val component = _component()
        val context = _context()
        val monoalpha = _record("mono-alpha", context, component, "alpha", StaticMonoKoto, StaticMonoKotoSubject, stabletiekey = Some("z"))
        val entityalpha = _record("entity-alpha", context, component, "alpha", StaticEntity, StaticEntitySubject, stabletiekey = Some("a"))
        val structurebeta = _record("structure-beta", context, component, "beta", StaticStructure, StaticStructureRelation)
        val classificationbeta = _record("classification-beta", context, component, "beta", StaticClassification, StaticClassificationRelation)
        val records = Vector(monoalpha, entityalpha, structurebeta, classificationbeta)
        val mappings = Vector(
          _mapping("mapping-beta", context, component, structurebeta, classificationbeta, stabletiekey = Some("b")),
          _mapping("mapping-alpha", context, component, monoalpha, entityalpha, stabletiekey = Some("a"))
        )
        val gaps = Vector(
          _gap("gap-beta", context, component, structurebeta, StaticEntity, StaticEntityMetadata, None, _condition(), stabletiekey = Some("b")),
          _gap("gap-alpha", context, component, monoalpha, StaticClassification, StaticClassificationDimension, None, _condition(), stabletiekey = Some("a"))
        )

        When("ScalaCheck generates independent record mapping and gap permutations")
        forAll(Gen.pick(records.size, records), Gen.pick(mappings.size, mappings), Gen.pick(gaps.size, gaps)) { (recordpermutation, mappingpermutation, gappermutation) =>
          val result = StaticCrossViewNavigation.create(
            context,
            component,
            recordpermutation.toVector,
            mappingpermutation.toVector,
            gappermutation.toVector
          )

          Then("only exact identities categories roles record identities and admitted tie keys choose every retained presentation order")
          result.toOption.toVector.flatMap(_.records.map(_.id)) shouldBe
            Vector("mono-alpha", "entity-alpha", "structure-beta", "classification-beta")
          result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
            Vector("mapping-alpha", "mapping-beta")
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe
            Vector("gap-alpha", "gap-beta")
        }
      }

      "gate forward and reverse navigation independently while retaining valid mappings when receiving implementation is unavailable" in {
        Given("one exact Mono-Koto to Entity mapping with independent forward and reverse implementation gates")
        val component = _component()
        val context = _context()
        val source = _record("mono-order", context, component, "order", StaticMonoKoto, StaticMonoKotoSubject)
        val counterpart = _record("entity-order", context, component, "order", StaticEntity, StaticEntitySubject)
        val forwardonly = _mapping(
          "mapping-forward-suppressed",
          context,
          component,
          source,
          counterpart,
          forwardimplemented = false,
          reverseimplemented = true
        )
        val reverseonly = _mapping(
          "mapping-reverse-suppressed",
          context,
          component,
          source,
          counterpart,
          forwardimplemented = true,
          reverseimplemented = false
        )

        When("the mappings have unusable receiving contracts in one direction only")
        val result = StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(forwardonly, reverseonly), Vector.empty)

        Then("only the relevant forward or reverse affordance is suppressed and both mappings remain retained")
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-forward-suppressed", "mapping-reverse-suppressed")
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-forward-suppressed").flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-forward-suppressed").flatMap(_.reverseNavigationTarget)) should have size 1
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-reverse-suppressed").flatMap(_.forwardNavigationTarget)) should have size 1
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-reverse-suppressed").flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "suppress affordances for source counterpart or mapping conditions without deleting retained identity records or mappings" in {
        Given("otherwise exact mappings whose source counterpart or mapping condition is not usable")
        val component = _component()
        val context = _context()
        val usablemono = _record("mono-order", context, component, "order", StaticMonoKoto, StaticMonoKotoSubject)
        val usableentity = _record("entity-order", context, component, "order", StaticEntity, StaticEntitySubject)
        val conditionedsource = usablemono.copy(id = "mono-denied", condition = _condition(authorization = "denied"))
        val conditionedcounterpart = usableentity.copy(id = "entity-limited", condition = _condition(limitations = Vector("counterpart-limited")))
        val mappings = Vector(
          _mapping("mapping-source-condition", context, component, conditionedsource, usableentity),
          _mapping("mapping-counterpart-condition", context, component, usablemono, conditionedcounterpart),
          _mapping("mapping-condition", context, component, usablemono, usableentity, condition = _condition(availability = "unavailable"))
        )
        val records = Vector(conditionedsource, usableentity, usablemono, conditionedcounterpart)

        When("the exact condition gates are evaluated after all records and mappings are retained")
        val result = StaticCrossViewNavigation.create(context, component, records, mappings, Vector.empty)

        Then("each condition suppresses navigation only and preserves the caller-admitted mappings")
        result.toOption.toVector.flatMap(_.records.map(_.id)).toSet shouldBe records.map(_.id).toSet
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-source-condition", "mapping-counterpart-condition", "mapping-condition")
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "reject invalid scope identity locator tie mapping gap and navigation input as one typed no-partial failure" in {
        Given("otherwise caller-admitted records plus isolated invalid admission variants")
        val component = _component()
        val context = _context()
        val source = _record("mono-order", context, component, "order", StaticMonoKoto, StaticMonoKotoSubject)
        val counterpart = _record("entity-order", context, component, "order", StaticEntity, StaticEntitySubject)
        val mapping = _mapping("mapping-order", context, component, source, counterpart)
        val samemono = _record("mono-reference", context, component, "order", StaticMonoKoto, StaticMonoKotoReference)
        val otheridentity = counterpart.copy(id = "entity-payment", semanticTargetId = ComponentDashboardSemanticTargetIdentity("payment"))
        val malformedidentity = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetIdentity = ComponentDashboardSemanticTargetIdentity("other")))
        )
        val malformedcomponent = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetComponent = ComponentDashboardComponentIdentity("other-component")))
        )
        val malformedattribution = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetAttribution = ComponentDashboardSourceAttribution("other", "other", "other:locator")))
        )
        val malformedcondition = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetCondition = _condition(limitations = Vector("different"))))
        )
        val invalidresults = Vector(
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(id = " ")), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(MonoKotoProjectionContextIdentity(" "), component, Vector(source), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(context = MonoKotoProjectionContextIdentity("other-context"))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(context = MonoKotoProjectionContextIdentity(" "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(component = ComponentDashboardComponentIdentity("other-component"))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(component = ComponentDashboardComponentIdentity(" "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(semanticTargetId = ComponentDashboardSemanticTargetIdentity(" "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(attribution = source.attribution.copy(sourceLocator = " "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(condition = _condition(availability = " "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(stableTieKey = Some(" "))), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, source), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping, mapping), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(id = source.id)), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-duplicate", context, component, source, StaticEntity, StaticEntitySubject, None, _condition()), _gap("gap-duplicate", context, component, source, StaticEntity, StaticEntitySubject, None, _condition()))),
          StaticCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-locator", context, component, source, StaticEntity, StaticEntitySubject, None, _condition()).copy(attribution = ComponentDashboardSourceAttribution("gap", "authority", " ")))),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(sourceRecordId = "missing")), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(context = MonoKotoProjectionContextIdentity("other-context"))), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(_mapping("mapping-self", context, component, source, source)), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, samemono), Vector(_mapping("mapping-same-category", context, component, source, samemono)), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, otheridentity), Vector(_mapping("mapping-different-identity", context, component, source, otheridentity)), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source.copy(category = StaticEntity)), Vector.empty, Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-same-category", context, component, source, StaticMonoKoto, StaticMonoKotoReference, None, _condition()))),
          StaticCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-different-identity", context, component, source, StaticEntity, StaticEntitySubject, Some(ComponentDashboardSemanticTargetIdentity("other")), _condition()))),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedidentity), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedcomponent), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedattribution), Vector.empty),
          StaticCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedcondition), Vector.empty)
        )

        When("each malformed input is admitted")
        val failures = invalidresults.flatMap(_.left.toOption)

        Then("every invalid admission returns a typed failure and none returns a partially projected index")
        failures should have size invalidresults.size
        failures.flatMap(_.violations) should not be empty
        invalidresults.flatMap(_.toOption) shouldBe Vector.empty
      }
    }
  }

  private def _component(): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity("textus-static-navigation")

  private def _context(): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity("ccdm-static-navigation")

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

  private def _record(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    category: StaticCrossViewProjectionCategory,
    role: StaticCrossViewRecordRole,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source", "authority", "source:record"),
    condition: ComponentDashboardCondition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty),
    stabletiekey: Option[String] = None
  ): StaticCrossViewIdentityRecord =
    StaticCrossViewIdentityRecord(
      id,
      context,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      category,
      role,
      attribution.copy(sourceId = s"source-$id", sourceLocator = s"source:$id"),
      condition,
      stabletiekey
    )

  private def _navigation(
    record: StaticCrossViewIdentityRecord,
    implemented: Boolean = true
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      record.semanticTargetId,
      record.component,
      record.attribution,
      record.condition,
      implemented
    )

  private def _mapping(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    source: StaticCrossViewIdentityRecord,
    counterpart: StaticCrossViewIdentityRecord,
    condition: ComponentDashboardCondition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty),
    stabletiekey: Option[String] = None,
    forwardimplemented: Boolean = true,
    reverseimplemented: Boolean = true
  ): StaticCrossViewMapping =
    StaticCrossViewMapping(
      id,
      context,
      component,
      source.id,
      counterpart.id,
      ComponentDashboardSourceAttribution(s"mapping-$id", "mapping-authority", s"mapping:$id"),
      condition,
      stabletiekey,
      Some(_navigation(counterpart, forwardimplemented)),
      Some(_navigation(source, reverseimplemented))
    )

  private def _gap(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    source: StaticCrossViewIdentityRecord,
    requestedcategory: StaticCrossViewProjectionCategory,
    requestedrole: StaticCrossViewRecordRole,
    knownidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition,
    stabletiekey: Option[String] = None
  ): StaticCrossViewGap =
    StaticCrossViewGap(
      id,
      context,
      component,
      source.id,
      requestedcategory,
      requestedrole,
      knownidentity,
      ComponentDashboardSourceAttribution(s"gap-$id", "gap-authority", s"gap:$id"),
      condition,
      "The bounded counterpart receiving contract is not implemented.",
      stabletiekey
    )
}
