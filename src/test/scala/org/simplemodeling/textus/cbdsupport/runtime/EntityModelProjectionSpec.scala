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
final class EntityModelProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "EntityModelProjection" should {
    "P9-40B pure caller-admitted Entity Model projection" which {
      "retain every admitted Entity Model role, exact relationship, attribution, locator, and condition" in {
        Given("one exact Component with Entity, Value, Aggregate, and each direct metadata role")
        val component = _component()
        val entitycondition = _condition(limitations = Vector("bounded-entity-scope"))
        val entity = _subject(
          "entity-order",
          component,
          "entity-order",
          EntityModelEntity,
          Vector(
            _metadata("identity-order", component, "entity-order", "entity-order", "relationship-identity-order", IdentityMetadata),
            _metadata("ownership-order-customer", component, "entity-order", "entity-customer", "relationship-order-customer", OwnershipMetadata),
            _metadata("lifecycle-order", component, "entity-order", "entity-order", "relationship-lifecycle-order", LifecycleMetadata)
          ),
          condition = entitycondition
        )
        val value = _subject("value-order-status", component, "value-order-status", EntityModelValue, Vector.empty)
        val aggregate = _subject(
          "aggregate-order",
          component,
          "aggregate-order",
          EntityModelAggregate,
          Vector(
            _metadata(
              "aggregate-boundary-order",
              component,
              "aggregate-order",
              "aggregate-order",
              "relationship-aggregate-boundary-order",
              AggregateBoundaryMetadata,
              condition = _condition(conflict = Some("displayed-boundary-conflict"))
            )
          )
        )
        val customer = _subject("entity-customer", component, "entity-customer", EntityModelEntity, Vector.empty)

        When("the caller supplies the exact already-admitted Entity Model records")
        val result = EntityModelProjection.create(_context(), component, Vector(entity, value, aggregate, customer), Vector.empty)

        Then("the projection retains supplied records without creating a Mono equivalence or Structure relation")
        val projected = result.toOption.get
        projected.subjects.map(_.sourceSubject.role).toSet shouldBe Set(EntityModelEntity, EntityModelValue, EntityModelAggregate)
        projected.subjects.find(_.sourceSubject.id == "entity-order").map(_.sourceSubject) shouldBe Some(entity)
        projected.subjects.find(_.sourceSubject.id == "entity-order").toVector.flatMap(_.metadata.map(_.sourceMetadata.role)).toSet shouldBe
          Set(IdentityMetadata, OwnershipMetadata, LifecycleMetadata)
        projected.subjects.find(_.sourceSubject.id == "aggregate-order").toVector.flatMap(_.metadata.map(_.sourceMetadata.role)) shouldBe
          Vector(AggregateBoundaryMetadata)
        projected.subjects.flatMap(_.metadata).map(_.sourceMetadata.attribution).toSet should contain(
          ComponentDashboardSourceAttribution("metadata-identity-order", "metadata-authority", "metadata:identity-order")
        )
        projected.subjects.find(_.sourceSubject.id == "entity-order").map(_.sourceSubject.condition) shouldBe Some(entitycondition)
      }

      "retain an explicit bounded unsupported-field gap without inferring an identity, endpoint, or absence" in {
        Given("one admitted Entity and an attributable Cozy field gap without an affected semantic identity")
        val component = _component()
        val entity = _subject("entity-order", component, "entity-order", EntityModelEntity, Vector.empty)
        val gapcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-provider-no-ownership-field"),
          limitations = Vector("cozy-field-boundary")
        )
        val gap = _gap(
          "gap-ownership-field",
          component,
          OwnershipMetadata,
          "cozy:entity-order/ownership-field",
          None,
          condition = gapcondition,
          reason = "The admitted Cozy field does not supply the requested ownership assertion."
        )

        When("the bounded unavailable field is projected")
        val result = EntityModelProjection.create(_context(), component, Vector(entity), Vector(gap))

        Then("the supplied limitation remains explicit and no navigation or proxy target is created")
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.map(_.gaps.head.sourceGap.affectedSemanticTargetId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.condition) shouldBe Some(gapcondition)
      }

      "present subjects metadata and gaps by exact identities independently of caller iteration" in {
        Given("reverse-ordered subjects, metadata relationships, and gaps with exact identity keys")
        val component = _component()
        val aggregate = _subject("aggregate-zulu", component, "aggregate-zulu", EntityModelAggregate, Vector.empty)
        val entity = _subject(
          "entity-alpha",
          component,
          "entity-alpha",
          EntityModelEntity,
          Vector(
            _metadata("metadata-zulu", component, "entity-alpha", "aggregate-zulu", "relationship-zulu", OwnershipMetadata),
            _metadata("metadata-alpha", component, "entity-alpha", "entity-alpha", "relationship-alpha", IdentityMetadata)
          )
        )
        val value = _subject("value-middle", component, "value-middle", EntityModelValue, Vector.empty)
        val gaps = Vector(
          _gap("gap-zulu", component, IdentityMetadata, "cozy:zulu", Some(ComponentDashboardSemanticTargetIdentity("aggregate-zulu"))),
          _gap("gap-alpha", component, IdentityMetadata, "cozy:alpha", Some(ComponentDashboardSemanticTargetIdentity("entity-alpha")))
        )
        forAll(Gen.choose(0, 5), Gen.oneOf(true, false)) { (subjectorderindex, reversegaps) =>
          val subjectorders = Vector(
            Vector(aggregate, entity, value),
            Vector(aggregate, value, entity),
            Vector(entity, aggregate, value),
            Vector(entity, value, aggregate),
            Vector(value, aggregate, entity),
            Vector(value, entity, aggregate)
          )
          val permutedsubjects = subjectorders(subjectorderindex)
          val permutedgaps = if (reversegaps) gaps.reverse else gaps
          When("property-generated caller permutations are projected")
          val result = EntityModelProjection.create(_context(), component, permutedsubjects.toVector, permutedgaps.toVector)

          val expectedsubjects = permutedsubjects.toVector
            .sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse("")))
            .map(_.semanticTargetId.value)
          val expectedmetadata = permutedsubjects.toVector
            .find(_.id == "entity-alpha")
            .toVector
            .flatMap(_.metadata)
            .sortBy(metadata => (
              metadata.semanticSubjectId.value,
              metadata.semanticRelationshipId.value,
              metadata.semanticTargetId.value,
              metadata.id,
              metadata.stableTieKey.getOrElse("")
            ))
            .map(_.semanticRelationshipId.value)
          val expectedgaps = permutedgaps.toVector
            .sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse("")))
            .map(_.id)

          Then("only supplied semantic and non-semantic tie identities determine presentation")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe expectedsubjects
          result.toOption.toVector.flatMap(_.subjects.find(_.sourceSubject.id == "entity-alpha").toVector.flatMap(_.metadata.map(_.sourceMetadata.semanticRelationshipId.value))) shouldBe
            expectedmetadata
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe expectedgaps
        }
      }

      "admit only exact implemented usable navigation while retaining conditioned source records" in {
        Given("an Entity, metadata, and gap with exact targets that differ by implementation and independent condition gates")
        val component = _component()
        val usable = _navigation("entity-order", component)
        val metadataunusable = _metadata(
          "ownership-order-customer",
          component,
          "entity-order",
          "entity-customer",
          "relationship-order-customer",
          OwnershipMetadata,
          navigation = Some(_navigation("entity-customer", component)),
          condition = _condition(authorization = "denied")
        )
        val entity = _subject(
          "entity-order",
          component,
          "entity-order",
          EntityModelEntity,
          Vector(metadataunusable),
          navigation = Some(usable)
        )
        val customer = _subject("entity-customer", component, "entity-customer", EntityModelEntity, Vector.empty)
        val gap = _gap(
          "gap-order",
          component,
          IdentityMetadata,
          "cozy:order/key",
          Some(ComponentDashboardSemanticTargetIdentity("entity-order")),
          navigation = Some(_navigation("entity-order", component, implementedandusable = false))
        )

        When("the exact Component records are projected")
        val result = EntityModelProjection.create(_context(), component, Vector(entity, customer), Vector(gap))

        Then("only the usable implemented target remains navigable and all source values remain retained")
        result.toOption.toVector.flatMap(_.subjects.filter(_.sourceSubject.id == "entity-order").flatMap(_.navigationTarget)) shouldBe Vector(usable)
        result.toOption.toVector.flatMap(_.subjects.flatMap(_.metadata).flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.subjects.find(_.sourceSubject.id == "entity-order").toVector.flatMap(_.metadata.map(_.sourceMetadata))) shouldBe
          Vector(metadataunusable)
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
      }

      "return a typed no-partial failure for invalid role scope identity locator and ordering input" in {
        Given("otherwise admitted records with a metadata subject role, cross-Component gap, duplicate relationship, blank locator, and blank tie key")
        val component = _component()
        val aggregate = _subject("aggregate-order", component, "aggregate-order", EntityModelAggregate, Vector.empty)
        val malformedrole = _subject(
          "entity-order",
          component,
          "entity-order",
          IdentityMetadata,
          Vector(
            _metadata("metadata-order", component, "entity-order", "entity-order", "relationship-order", IdentityMetadata)
          )
        )
        val duplicate = _subject(
          "entity-customer",
          component,
          "entity-customer",
          EntityModelEntity,
          Vector(
            _metadata("metadata-customer", component, "entity-customer", "entity-customer", "relationship-order", IdentityMetadata)
          )
        )
        val blanklocator = _subject(
          "value-status",
          component,
          "value-status",
          EntityModelValue,
          Vector.empty,
          attribution = ComponentDashboardSourceAttribution("value-status-source", "subject-authority", " ")
        )
        val blanktiekey = _gap(
          "gap-blank-tie",
          component,
          AggregateBoundaryMetadata,
          "cozy:aggregate/boundary",
          Some(ComponentDashboardSemanticTargetIdentity("aggregate-order")),
          tiekey = Some(" ")
        )
        val outofscope = _gap("gap-scope", _component("textus-payment"), EntityModelEntity, "cozy:entity", None)

        When("the immutable factory validates all supplied boundary violations")
        val result = EntityModelProjection.create(_context(), component, Vector(malformedrole, duplicate, aggregate, blanklocator), Vector(blanktiekey, outofscope))

        Then("it returns EntityModelProjectionFailure with no partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Entity Model subject 'entity-order' has metadata role IdentityMetadata."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Entity Model metadata relationship identity 'relationship-order'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Entity Model subject 'value-status' source locator must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Entity Model gap 'gap-blank-tie' stable non-semantic tie key must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Entity Model gap 'gap-scope' is outside Projection Component 'textus-entity-model'."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-entity-model-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-entity-model"): ComponentDashboardComponentIdentity =
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

  private def _metadata(
    metadataid: String,
    component: ComponentDashboardComponentIdentity,
    subjectid: String,
    targetid: String,
    relationshipid: String,
    role: EntityModelProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): EntityModelProjectionMetadata =
    EntityModelProjectionMetadata(
      metadataid,
      component,
      ComponentDashboardSemanticTargetIdentity(subjectid),
      ComponentDashboardSemanticTargetIdentity(targetid),
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      role,
      _attribution(s"metadata-$metadataid", "metadata-authority", s"metadata:$metadataid"),
      condition,
      None,
      navigation
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    role: EntityModelProjectionRole,
    metadata: Vector[EntityModelProjectionMetadata],
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "subject-source",
      "subject-authority",
      "subject:locator"
    )
  ): EntityModelProjectionSubject =
    EntityModelProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      role,
      attribution,
      condition,
      metadata,
      None,
      navigation
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: EntityModelProjectionRole,
    boundedfieldorscope: String,
    affectedidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition = _condition(),
    reason: String = "The admitted bounded material cannot support this field.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): EntityModelProjectionGap =
    EntityModelProjectionGap(
      gapid,
      component,
      requestedrole,
      boundedfieldorscope,
      affectedidentity,
      _attribution(s"gap-$gapid", "gap-authority", s"gap:$gapid"),
      condition,
      reason,
      tiekey,
      navigation
    )
}
