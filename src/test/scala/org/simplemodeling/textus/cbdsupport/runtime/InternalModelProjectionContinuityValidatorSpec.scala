package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, Paths, StandardOpenOption}

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import io.circe.parser.parse
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 28, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelProjectionContinuityValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private final case class FixtureElement(identity: String, kind: String, role: Option[String])
  private final case class FixtureAssociation(role: String, kind: String, identity: String)
  private final case class FixtureRelationship(
    identity: String,
    role: String,
    source: String,
    target: String,
    associations: Vector[FixtureAssociation] = Vector.empty,
    sequence: Option[String] = None,
    conditionids: Vector[String] = Vector.empty
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"),
    "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"),
    "revision" -> Json.fromString("revision-1")
  )

  private val _snapshot_reference = _artifact_reference("snapshot-model", 17L, "source-snapshot")
  private val _realization_artifact_reference = _artifact_reference("realization-main", 23L, "realization")
  private val _work_root = Paths.get("target/internal-model-projection-continuity/work").toAbsolutePath.normalize()


  "Internal-model projection continuity validation" should {
    "reconstruct all eight substantive Phase 9 values from one package V2 handoff" in {
      Given("a admitted package containing a V3 realization, closed binding, source-local role witnesses, associations, and sequence witnesses")
      _with_fixture("display-one") { root =>
        When("the package is admitted through the projection-continuity entry point")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        val roundtrip = result.toOption.map(value => _roundtrip_binding(value))

        Then("every constructor has a normal nonempty value with exact nested identities and retained V3 sidecar")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map { value =>
          (
            value.monoKoto.subjects.map(_.sourceSubject.semanticTargetId.value),
            value.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.map(_.semanticRelationshipId.value),
            value.entityModel.subjects.head.metadata.map(_.sourceMetadata.semanticRelationshipId.value),
            value.eventModel.assertions.map(_.sourceAssertion.semanticAssertionId.value),
            value.structureView.relations.head.assertions.map(_.sourceAssertion.semanticRelationshipId.value),
            value.classificationView.dimensions.head.assertions.map(_.sourceAssertion.semanticAssertionId.value),
            value.workflow.flows.map(_.sourceFlow.semanticFlowId.value),
            value.stateMachine.transitions.map(_.sourceTransition.semanticTransitionId.value),
            value.stateMachine.transitionAdjuncts.map(_.sourceTransitionAdjunct.semanticAdjunctRelationId.value),
            value.workflow.flows.head.sourceFlow.attribution.sourceLocator,
            value.workflow.flows.head.sourceFlow.sourceEndpoint.attribution.sourceLocator,
            value.realization.profile,
            value.binding.profile,
            roundtrip.contains(Right(value.binding))
          )
        } shouldBe Some((
          Vector("e-mono", "e-koto"),
          Vector("r-usecase-step"),
          Vector("r-entity-metadata"),
          Vector("r-event-cause"),
          Vector("r-structure"),
          Vector("r-classification-assertion"),
          Vector("r-workflow-flow"),
          Vector("r-state-transition"),
          Vector("r-state-adjunct"),
          "anchor-r-workflow-flow-role-WorkflowProjectionFlowRelation",
          "anchor-e-workflow-kind-WorkflowProjectionWorkflow",
          "ccdm-realization-v3",
          "ccdm-projection-binding-v3",
          true
      ))
      }
    }

    "retain exact IdentityMetadata for admitted Value and Aggregate subjects" in {
      Vector("EntityModelValue", "EntityModelAggregate").foreach { entityrole =>
        Given(s"a coherent V2 source snapshot, V3 realization, binding, and manifest for an admitted $entityrole subject with direct IdentityMetadata")
        _with_fixture(s"metadata-$entityrole", entityrole = entityrole) { root =>
          When("the package is admitted through the projection-continuity entry point")
          val result = InternalModelProjectionContinuityValidator.validate(root)

          val roundtrip = result.toOption.map(value => _roundtrip_binding(value))

          Then("the exact subject role, identity, metadata owner, source attribution, and decoded binding values are retained")
          val expectedrole = if entityrole == "EntityModelValue" then EntityModelValue else EntityModelAggregate
          withClue(result.show) {
            result.toOption.map { value =>
              value.entityModel.subjects.map { subject =>
                (
                  subject.sourceSubject.role,
                  subject.sourceSubject.semanticTargetId.value,
                  subject.sourceSubject.attribution.sourceLocator,
                  subject.metadata.map(metadata => (
                    metadata.sourceMetadata.semanticSubjectId.value,
                    metadata.sourceMetadata.semanticTargetId.value,
                    metadata.sourceMetadata.semanticRelationshipId.value,
                    metadata.sourceMetadata.attribution.sourceLocator
                  )),
                  _roundtrip_binding(value) == Right(value.binding)
                )
              }
            } shouldBe Some(Vector((
              expectedrole,
              "e-entity",
              s"anchor-e-entity-kind-$entityrole",
              Vector(("e-entity", "e-entity", "r-entity-metadata", "anchor-r-entity-metadata-role-IdentityMetadata")),
              true
            )))
          }
        }
      }
    }

    "preserve opaque identities when labels and semantic-input encounter order vary" in {
      Given("generated nonempty labels and independently reversed pre-serialization fixture input for the same ledger")
      forAll(Gen.oneOf("EntityModelEntity", "EntityModelValue", "EntityModelAggregate"), Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { (entityrole, label) =>
        _with_fixture(label, reverseinputs = true, entityrole = entityrole) { reversedroot =>
          _with_fixture(label, reverseinputs = false, entityrole = entityrole) { orderedroot =>
            When("labels and pre-serialization encounter order differ while the declared semantic arrays remain sorted")
            val reversed = InternalModelProjectionContinuityValidator.validate(reversedroot)
            val ordered = InternalModelProjectionContinuityValidator.validate(orderedroot)

            Then("the eight projection identities, exact Entity Model metadata owner, nested sequence key, direct V3 source anchor, and suppressed navigation stay exact")
            (for {
              left <- reversed.toOption
              right <- ordered.toOption
            } yield (
              left.monoKoto.subjects.map(_.sourceSubject.semanticTargetId.value).sorted,
              left.entityModel.subjects.map(subject => (
                subject.sourceSubject.role,
                subject.sourceSubject.semanticTargetId.value,
                subject.metadata.map(metadata => (
                  metadata.sourceMetadata.semanticSubjectId.value,
                  metadata.sourceMetadata.semanticTargetId.value,
                  metadata.sourceMetadata.semanticRelationshipId.value,
                  metadata.sourceMetadata.attribution.sourceLocator
                )),
                subject.sourceSubject.attribution.sourceLocator
              )),
              left.entityModel.subjects.map(_.sourceSubject.role) == right.entityModel.subjects.map(_.sourceSubject.role),
              left.useCaseCommunication.subjects.head.sourceSubject.flows.head.steps.head.sequenceKey,
              left.workflow.flows.head.sourceFlow.attribution.sourceLocator,
              left.stateMachine.transitions.head.forwardNavigationTarget,
              left.binding == right.binding
            )) shouldBe Some((
              Vector("e-koto", "e-mono"),
              Vector((
                if entityrole == "EntityModelEntity" then EntityModelEntity else if entityrole == "EntityModelValue" then EntityModelValue else EntityModelAggregate,
                "e-entity",
                Vector(("e-entity", "e-entity", "r-entity-metadata", "anchor-r-entity-metadata-role-IdentityMetadata")),
                s"anchor-e-entity-kind-$entityrole"
              )),
              true,
              "step-1",
              "anchor-r-workflow-flow-role-WorkflowProjectionFlowRelation",
              None,
              true
            ))
          }
        }
      }
    }

    "reject selected nested relationships whose exact retained owner is absent" in {
      Given("an otherwise valid all-eight V3 handoff with selected Mono-Koto or Entity metadata relationships but no matching selected source subject")
      val missingmono = _with_rejected_fixture() { root =>
        _replace_projection(root, _binding_bytes(_update_binding_view(_binding_json(_binding("display-one")), "MonoKotoProjection")(_.filterNot(_.hcursor.get[String]("semanticIdentity").toOption.contains("e-mono")))))
      }
      val missingentity = _with_rejected_fixture() { root =>
        _replace_projection(root, _binding_bytes(_update_binding_view(_binding_json(_binding("display-one")), "EntityModelProjection")(_.filterNot(_.hcursor.get[String]("semanticIdentity").toOption.contains("e-entity")))))
      }
      val emptyowners = _with_rejected_fixture() { root =>
        _replace_projection(root, _binding_bytes(_update_binding_view(_binding_json(_binding("display-one")), "MonoKotoProjection")(_.filterNot(_.hcursor.get[String]("recordKind").toOption.contains("element")))))
      }
      val incompatiblemono = _with_rejected_fixture() { root =>
        _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one")), "MonoKotoProjection", "e-mono")(_.add("viewRole", Json.fromString("Koto")))))
      }
      Then("missing or empty selected owners reject rather than silently dropping nested values")
      Vector(missingmono, missingentity, emptyowners, incompatiblemono) shouldBe Vector(true, true, true, true)
    }

    "apply the closed persisted binding boundaries" which {
      "reject a V1 binding paired with a V3 realization" in {
        Given("a package whose projection artifact and manifest dependency are declared to a V1 binding")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", profile = "ccdm-projection-binding-v1", schema = "1.0"))
          When("the version pair reaches binding admission")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the pairing rejects without a compatibility fallback")
          result.isSuccess shouldBe false
        }
      }

      "reject a non-dependent projection manifest" in {
        Given("a package whose manifest dependency is declared to omit the realization dependency")
        _with_fixture("display-one") { root =>
          _rewrite_manifest_dependency(root, Vector(_snapshot_reference))
          When("the package handoff selects its projection")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the selection rejects before semantic reconstruction")
          result.isSuccess shouldBe false
        }
      }

      "admit equivalent JSON presentation without using bytes as control" in {
        Given("a manifest whose projection entry names nondecoded binding values")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one").dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8))
          When("the strict JSON parser admits its semantic fields")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the same binding semantics remain admitted")
          result.isSuccess shouldBe true
        }
      }

      "reject an unsupported direct role with coherent package references" in {
        Given("a closed binding whose Event relationship role differs from its direct witness")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", roleoverride = Some("r-event-cause" -> "EventModelUnknown")))
          _admitted_realization(root)
          When("the role is checked against the V3 realization")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the adapter rejects without label interpretation or source selection")
          result.isSuccess shouldBe false
        }
      }

      "reject an Event endpoint without exact Event-local evidence" in {
        Given("a coherent realization whose Event relation targets an admitted but non-Event-bound element")
        _with_fixture("display-one", eventtarget = "e-entity") { root =>
          _admitted_realization(root)
          When("the Event adapter builds endpoint-local evidence")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the endpoint rejects rather than borrowing relationship evidence")
          result.isSuccess shouldBe false
        }
      }

      "reject a Workflow owner association to a retained Activity" in {
        Given("a coherent V3 realization whose Workflow flow owner is an Activity rather than a Workflow")
        _with_fixture("display-one", workflowowner = "e-activity") { root =>
          _admitted_realization(root)
          When("the adapter maps the exact owner association")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the association rejects without owner inference")
          result.isSuccess shouldBe false
        }
      }

      "reject an omitted ordered sequence witness with complete target links" in {
        Given("a closed binding that clears only its Use Case step sequence field")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", sequencewitness = None))
          _admitted_realization(root)
          When("the ordered Use Case relationship is admitted")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the sequence rejects without using array order")
          result.isSuccess shouldBe false
        }
      }

      "reject multiple conflicting conditions without a winner" in {
        Given("a coherent realization and binding with two conflict conditions on one Workflow relation")
        _with_fixture("display-one", workflowconditions = Vector("conflict-one", "conflict-two")) { root =>
          _admitted_realization(root)
          When("the relation condition reaches DTO compression")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the condition boundary rejects instead of concatenating or selecting detail")
          result.isSuccess shouldBe false
        }
      }

      "retain an exact single-condition Entity gap and reject a changed bounded field" in {
        Given("a coherent ledger with one attributable Entity conflict condition and its matching bounded gap")
        _with_fixture("display-one", entityconditions = Vector("entity-gap")) { root =>
          _replace_projection(root, _binding("display-one", entityconditions = Vector("entity-gap"), entitygap = Some("entity-gap")))
          _admitted_realization(root)
          When("the one-condition gap is admitted")
          val accepted = InternalModelProjectionContinuityValidator.validate(root)
          Then("the Entity constructor retains its exact affected identity and requested field")
          accepted.toOption.map(_.entityModel.gaps.map(gap => (gap.sourceGap.affectedSemanticTargetId.map(_.value), gap.sourceGap.boundedFieldOrScope))) shouldBe
            Some(Vector((Some("e-entity"), "entity-gap")))
        }
        Given("the same coherent ledger with a gap field that differs from the sole attributable condition detail")
        _with_fixture("display-one", entityconditions = Vector("entity-gap")) { root =>
          _replace_projection(root, _binding("display-one", entityconditions = Vector("entity-gap"), entitygap = Some("forged-gap")))
          _admitted_realization(root)
          When("the gap requests text other than its exact condition detail")
          val rejected = InternalModelProjectionContinuityValidator.validate(root)
          Then("the gap rejects without paraphrasing or manufacturing bounded scope")
          rejected.isSuccess shouldBe false
        }
      }

      "reject absent and duplicate present projections after manifest declaration changes" in {
        Given("one package without a present projection and one package with two independently declared present projections")
        val absent = _with_rejected_fixture() { root =>
          _omit_projection(root)
        }
        val duplicate = _with_rejected_fixture() { root =>
          _duplicate_projection(root)
        }
          Then("absence and multiplicity both fail closed before reconstruction")
        Vector(absent, duplicate) shouldBe Vector(true, true)
      }

      "reject closed binding member, family, record-order, and scope mutations" in {
        Given("separately declared closed bindings with one closed-shape boundary changed at a time")
        val unknownmember = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_binding_json(_binding("display-one")).mapObject(_.add("forgedMember", Json.fromString("forged")))))
        }
        val familyorder = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_views(_binding_json(_binding("display-one")))(_.reverse)))
        }
        val recordorder = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_view(_binding_json(_binding("display-one")), "MonoKotoProjection")(_.reverse)))
        }
        val wrongscope = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_binding_json(_binding("display-one")).mapObject { objectvalue =>
            objectvalue.add("scope", Json.obj(
              "componentIdentity" -> Json.fromString("other-component"),
              "projectionContextIdentity" -> Json.fromString("context-all-eight"),
              "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")
            ))
          }))
        }
          Then("unknown members, noncanonical tuple order, and nonmatching scope have no permissive interpretation")
        Vector(unknownmember, familyorder, recordorder, wrongscope) shouldBe Vector(true, true, true, true)
      }

      "reject missing, extra, and lane-promoted assertion links" in {
        Given("separately declared Event records whose exact canonical assertion link is removed, extended, or moved to enrichment")
        val missing = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one")), "EventModelProjection", "e-event") { objectvalue =>
            objectvalue.add("canonicalAssertionIds", Json.arr())
          }))
        }
        val extra = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one")), "EventModelProjection", "e-event") { objectvalue =>
            objectvalue.add("canonicalAssertionIds", Json.arr(Json.fromString("a-e-event-kind-EventModelEvent"), Json.fromString("a-forged")))
          }))
        }
        val promoted = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one")), "EventModelProjection", "e-event") { objectvalue =>
            objectvalue.add("canonicalAssertionIds", Json.arr()).add("enrichmentAssertionIds", Json.arr(Json.fromString("a-e-event-kind-EventModelEvent")))
          }))
        }
          Then("no missing, additional, or promoted link is accepted")
        Vector(missing, extra, promoted) shouldBe Vector(true, true, true)
      }

      "reject missing, extra, and assertion-lane-promoted condition links" in {
        Given("a coherent Entity condition and three separately declared binding link mutations")
        val missing = _with_rejected_fixture(entityconditions = Vector("entity-condition")) { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one", entityconditions = Vector("entity-condition"))), "EntityModelProjection", "e-entity") { objectvalue =>
            objectvalue.add("conditionIds", Json.arr())
          }))
        }
        val extra = _with_rejected_fixture(entityconditions = Vector("entity-condition")) { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one", entityconditions = Vector("entity-condition"))), "EntityModelProjection", "e-entity") { objectvalue =>
            objectvalue.add("conditionIds", Json.arr(Json.fromString("c-e-entity-0"), Json.fromString("c-forged")))
          }))
        }
        val promoted = _with_rejected_fixture(entityconditions = Vector("entity-condition")) { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one", entityconditions = Vector("entity-condition"))), "EntityModelProjection", "e-entity") { objectvalue =>
            objectvalue.add("canonicalAssertionIds", Json.arr(Json.fromString("a-e-entity-kind-EntityModelEntity"), Json.fromString("c-e-entity-0"))).add("conditionIds", Json.arr())
          }))
        }
          Then("omitted, forged, and assertion-lane-promoted conditions fail closed")
        Vector(missing, extra, promoted) shouldBe Vector(true, true, true)
      }

      "reject a wrong-target sequence witness and a wrong Use Case graph direction" in {
        Given("a binding that names a Workflow sequence for a Use Case step and a realization whose retained flow direction is reversed")
        val wrongtargetsequence = _with_rejected_fixture() { root =>
          _replace_projection(root, _binding_bytes(_update_binding_record(_binding_json(_binding("display-one")), "UseCaseCommunicationProjection", "r-usecase-step") { objectvalue =>
            objectvalue.add("sequenceAssertionId", Json.fromString("a-r-workflow-flow-sequence-key-flow-1"))
          }))
        }
        val wrongdirection = _with_rejected_fixture() { root =>
          _replace_realization(root, _realization_bytes(_update_realization_relationship(_realization_json(_realization("display-one")), "r-usecase-flow") { objectvalue =>
            objectvalue.add("direction", Json.fromString("target-to-source"))
          }))
        }
          Then("a witness cannot be borrowed across relationships and direction cannot be inferred")
        Vector(wrongtargetsequence, wrongdirection) shouldBe Vector(true, true)
      }

      "reject missing, duplicate, and contradicted local association boundaries" in {
        Given("coherent source-ledger fixtures for a missing Structure owner, duplicate Structure owners, a non-dimension Classification owner, and a non-machine State owner")
        val missingstructureowner = _with_rejected_fixture(structureowners = Vector.empty)()
        val duplicatestructureowner = _with_rejected_fixture(structureowners = Vector("r-mono-domain", "r-structure"), admitrealization = false)()
        val contradictedclassificationowner = _with_rejected_fixture(classificationowners = Vector("e-entity"))()
        val contradictedstatemachineowner = _with_rejected_fixture(statemachineowners = Vector("e-state-a"))()
          Then("missing, ambiguous, and contradicted association ownership is rejected without a fallback owner")
        Vector(missingstructureowner, duplicatestructureowner, contradictedclassificationowner, contradictedstatemachineowner) shouldBe Vector(true, true, true, true)
      }

      "reject duplicate V3 direct role witnesses sharing one source reference" in {
        Given("a admitted V3 Event record with two direct role assertions that name the same exact source reference")
        _with_fixture("display-one") { root =>
          val realization = _duplicate_direct_role_witness(
            _realization_json(Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))),
            "z-e-event-kind-EventModelEvent-duplicate",
            "ref-e-event-kind-EventModelEvent"
          )
          val binding = _append_binding_canonical_assertion(
            _binding_json(Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))),
            "EventModelProjection",
            "e-event",
            "z-e-event-kind-EventModelEvent-duplicate"
          )
          _replace_realization_and_projection(root, _realization_bytes(realization), _binding_bytes(binding))
          withClue("the modified realization must remain independently valid before projection continuity attribution") {
            InternalModelSemanticRealizationValidator.validate(root).isSuccess shouldBe true
          }
          When("V3 attribution resolves the direct canonical role witness")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("two matching assertions reject even when their source reference is the same")
          result.isSuccess shouldBe false
          result.show should include("must have exactly one direct canonical role witness")
        }
      }

      "reject duplicate V3 direct role witnesses naming distinct source references" in {
        Given("a admitted V3 Event record with two direct role assertions and two source-reference IDs for the same exact source witness")
        _with_fixture("display-one") { root =>
          val sourceid = "ref-e-event-kind-EventModelEvent-duplicate"
          val realization = _duplicate_direct_role_witness(
            _append_source_reference(
              _realization_json(Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))),
              sourceid
            ),
            "z-e-event-kind-EventModelEvent-second",
            sourceid
          )
          val binding = _append_binding_canonical_assertion(
            _binding_json(Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))),
            "EventModelProjection",
            "e-event",
            "z-e-event-kind-EventModelEvent-second"
          )
          _replace_realization_and_projection(root, _realization_bytes(realization), _binding_bytes(binding))
          withClue("the modified realization must remain independently valid before projection continuity attribution") {
            InternalModelSemanticRealizationValidator.validate(root).isSuccess shouldBe true
          }
          When("V3 attribution resolves the direct canonical role witness")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("two matching assertions reject without selecting either source reference")
          result.isSuccess shouldBe false
          result.show should include("must have exactly one direct canonical role witness")
        }
      }

      "reject duplicate selected Use Case flow targets" in {
        Given("a closed binding and realization with two selected Use Case flows targeting the same flow element")
        _with_fixture("display-one", duplicateflowtarget = true) { root =>
          withClue("the duplicate-flow realization must remain independently valid before projection continuity graph construction") {
            InternalModelSemanticRealizationValidator.validate(root).isSuccess shouldBe true
          }
          When("the Use Case adapter derives its complete selected flow graph")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the duplicate flow target rejects before nested DTO construction")
          result.isSuccess shouldBe false
          result.show should include("duplicate selected flow targets")
        }
      }

      "reject duplicate selected Use Case step targets" in {
        Given("a closed binding and realization with two selected steps targeting the same step element")
        _with_fixture("display-one", duplicatesteptarget = true) { root =>
          withClue("the duplicate-step realization must remain independently valid before projection continuity graph construction") {
            InternalModelSemanticRealizationValidator.validate(root).isSuccess shouldBe true
          }
          When("the Use Case adapter derives its complete selected step graph")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the duplicate step target rejects before nested DTO construction")
          result.isSuccess shouldBe false
          result.show should include("duplicate selected step targets")
        }
      }

      "reject a selected Use Case step without an exact selected flow" in {
        Given("a closed binding and realization whose only selected step starts outside the selected flow target")
        _with_fixture("display-one", stepsource = "e-mono") { root =>
          withClue("the orphan-step realization must remain independently valid before projection continuity graph construction") {
            InternalModelSemanticRealizationValidator.validate(root).isSuccess shouldBe true
          }
          When("the Use Case adapter matches every selected step to its one selected flow")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the orphan step rejects without deriving membership from order or labels")
          result.isSuccess shouldBe false
          result.show should include("has no exact selected flow")
        }
      }
    }

    "attribute an unasserted Entity gap only to its sole condition source" in {
      Given("a V3 fixture whose normal Entity role and sole gap condition have distinct exact source references")
      _with_distinct_gap_fixture() { root =>
        _admitted_realization(root)
        When("an EntityModelValue gap is reconstructed without an asserted EntityModelValue role")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("normal attribution retains the direct role source and gap attribution retains its exact condition source")
        withClue(result.show) {
          result.toOption.map(value => (
            value.entityModel.subjects.map(_.sourceSubject.attribution.sourceLocator),
            value.entityModel.gaps.map(gap => (gap.sourceGap.attribution.sourceLocator, gap.sourceGap.affectedSemanticTargetId.map(_.value), gap.sourceGap.boundedFieldOrScope))
          )) shouldBe Some((Vector("anchor-e-entity-role-source-a"), Vector(("anchor-e-entity-gap-condition", Some("e-entity"), "entity-gap"))))
        }
      }
    }
  }


  "Explicit V3 binding references and captured admission" should {
    "retain independently declared identities and revisions under generated input order and labels" in {
      forAll(
        Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
        Gen.oneOf(1L, 7L, Long.MaxValue),
        Gen.choose(1L, 10000L),
        Gen.oneOf(true, false)
      ) { (suffix, bindingrevision, artifactrevision, reverseinputs) =>
        Given("generated opaque identities, labels, positive binding and realization artifact revisions with independently ordered fixture input")
        _with_fixture(suffix, reverseinputs = reverseinputs) { root =>
          val semanticidentity = s"e-event:$suffix"
          _rename_semantic_identity(root, "e-event", semanticidentity)
          val captured = _capture(root)
          val selected = captured.realizationpackage.realization.reference.copy(
            artifactRevision = InternalModelArtifactRevision.from(artifactrevision).toOption.get
          )
          val binding = _binding_json(captured.projection.bytes.toArray).mapObject(_.add(
            "bindingReference", _record_reference(s"binding:$suffix", bindingrevision)
          ).add("realizationArtifactReference", _artifact_reference("realization-main", artifactrevision, "realization")))
          val changed = captured.copy(
            realizationpackage = captured.realizationpackage.copy(
              realization = captured.realizationpackage.realization.copy(reference = selected)
            ),
            projection = captured.projection.copy(dependencies = Vector(selected), bytes = _binding_bytes(binding).toVector)
          )
          val admitted = InternalModelSemanticRealizationValidator.validateVerified(changed.realizationpackage)
          withClue(admitted.show) { admitted.isSuccess shouldBe true }

          When("the one changed capture is reconstructed with the explicit selected references")
          val result = InternalModelProjectionContinuityValidator.validateVerified(changed)

          val roundtrip = result.toOption.map(value => _decode_binding(changed, value.realization, InternalModelProjectionContinuityValidator.encode(value.binding)))

          Then("logical binding, realization record, artifact and source versions remain independent and semantic identities stay exact")
          withClue(result.show) {
            result.toOption.map(value => (
              value.binding.bindingReference.recordId.value,
              value.binding.bindingReference.recordRevision.value,
              value.binding.realizationArtifactReference,
              value.realization.realizationReference.recordId.value,
              value.realization.realizationReference.recordRevision.value,
              value.realization.consumedSnapshotReferences.map(_.artifactRevision.value),
              value.realization.sourceReferences.map(_.source.revision).distinct,
              value.eventModel.subjects.map(_.sourceSubject.semanticTargetId.value).sorted,
              value.eventModel.assertions.head.sourceAssertion.targetEndpoint.semanticTargetId.value,
              roundtrip.contains(Right(value.binding))
            )) shouldBe Some((
              s"binding:$suffix", bindingrevision, selected, "realization-all-eight", 11L,
              Vector(17L), Vector(Some("revision-1")), Vector("e-command", semanticidentity).sorted,
              semanticidentity, true
            ))
          }
        }
      }
    }

    "admit object-key order, whitespace and equivalent escaping with ordinary writer roundtrip" in {
      Given("one admitted V3 capture and presentation variations of its same binding fields")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val json = _binding_json(captured.projection.bytes.toArray)
        val reordered = Json.fromJsonObject(JsonObject.fromIterable(json.asObject.get.toVector.reverse))
        val variants = Vector(
          reordered.spaces2.getBytes(StandardCharsets.UTF_8).toVector,
          (" \n\t" + reordered.noSpaces + "\n \t").getBytes(StandardCharsets.UTF_8).toVector,
          reordered.noSpaces.replace("binding-all-eight", "binding-\\u0061ll-eight").getBytes(StandardCharsets.UTF_8).toVector
        )
        When("strict JSON binding admission reads every presentation and the ordinary writer output")
        val results = variants.map(bytes => _decode_binding(captured, realization, bytes))
        val original = _decode_binding(captured, realization, captured.projection.bytes)
        val roundtrip = original.flatMap(binding => _decode_binding(captured, realization, InternalModelProjectionContinuityValidator.encode(binding)))

        Then("all presentations and the writer preserve exactly the same decoded binding")
        original.isRight shouldBe true
        results shouldBe Vector.fill(variants.size)(original)
        roundtrip shouldBe original
      }
    }

    "reject raw escaped lone surrogates in the original binding record ID" in {
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        Vector("\\ud800", "\\udfff").foreach { escape =>
          Given("an admitted V3 realization and valid binding JSON with a unique original record ID placeholder")
          val marked = _binding_json(captured.projection.bytes.toArray).mapObject(_.add("bindingReference", _record_reference("unicode-binding-placeholder", 7L)))
          val bytes = marked.noSpaces.replace("\"unicode-binding-placeholder\"", "\"binding-" + escape + "\"").getBytes(StandardCharsets.UTF_8).toVector
          When("the raw ASCII escape reaches binding admission before nested UTF-8 conversion")
          val result = _decode_binding(captured, realization, bytes)
          Then("each lone surrogate is rejected instead of admitting a replacement binding identity")
          result.isLeft shouldBe true
          result.left.toOption.get should include("bindingReference recordId must be nonblank valid Unicode")
        }
      }
    }

    "preserve generated paired supplementary binding IDs and positive revisions exactly" in {
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        forAll(Gen.choose(0x10000, 0x10ffff), Gen.choose(1L, Long.MaxValue)) { (codepoint, revision) =>
          Given("one valid binding whose record ID contains a generated supplementary scalar and independent positive revision")
          val scalar = new String(Character.toChars(codepoint))
          val escaped = Character.toChars(codepoint).map(character => f"\\u${character.toInt}%04x").mkString
          val marked = _binding_json(captured.projection.bytes.toArray).mapObject(_.add("bindingReference", _record_reference("unicode-binding-pair", revision)))
          val bytes = marked.noSpaces.replace("\"unicode-binding-pair\"", "\"binding-" + escaped + "\"").getBytes(StandardCharsets.UTF_8).toVector
          val ordinary = marked.noSpaces.replace("\"unicode-binding-pair\"", "\"binding-" + scalar + "\"").getBytes(StandardCharsets.UTF_8).toVector
          When("paired escaping, ordinary UTF-8 and the ordinary binding writer cross the same selected admission")
          val result = _decode_binding(captured, realization, bytes)
          val expected = _decode_binding(captured, realization, ordinary)
          val roundtrip = result.flatMap(value => _decode_binding(captured, realization, InternalModelProjectionContinuityValidator.encode(value)))
          Then("the exact scalar, revision and all views, evidence links and selected dependencies remain intact")
          result.isRight shouldBe true
          result shouldBe expected
          roundtrip shouldBe result
          result.toOption.get.bindingReference.recordId.value shouldBe "binding-" + scalar
          result.toOption.get.bindingReference.recordRevision.value shouldBe revision
        }
      }
    }

    "reject invalid lexical record and artifact revisions without defaults" in {
      Given("an admitted realization and binding with one explicit reference revision replaced by each invalid lexical form")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val content = new String(captured.projection.bytes.toArray, StandardCharsets.UTF_8)
        val forms = Vector("0", "-1", "+7", "7.0", "7e0", "07", "\"7\"", "9223372036854775808", "null")
        val variants = forms.flatMap { form =>
          Vector(
            content.replace("\"recordRevision\":7", s"\"recordRevision\":$form"),
            content.replace("\"artifactRevision\":23", s"\"artifactRevision\":$form")
          )
        }
        When("each declared revision crosses strict binding reference admission")
        val results = variants.map(value => _decode_binding(captured, realization, value.getBytes(StandardCharsets.UTF_8).toVector))

        Then("zero, signed, fractional, exponent, leading-zero, string, overflow and null versions all reject")
        results.map(_.isLeft) shouldBe Vector.fill(variants.size)(true)
      }
    }

    "reject missing, extra, wrong-type and blank logical reference fields" in {
      Given("an admitted capture with malformed closed logical reference shapes")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val json = _binding_json(captured.projection.bytes.toArray)
        val references = Vector(
          Json.Null, Json.fromString("binding-all-eight"),
          Json.obj("recordId" -> Json.fromString("binding-all-eight")),
          _record_reference("binding-all-eight", 7L).mapObject(_.add("extra", Json.fromBoolean(true))),
          _record_reference("", 7L), _record_reference("   ", 7L)
        )
        val variants = references.map(reference => json.mapObject(_.add("bindingReference", reference))) :+
          json.mapObject(_.remove("bindingReference"))
        When("the closed six-field binding and logical reference shapes are decoded")
        val results = variants.map(value => _decode_binding(captured, realization, _binding_bytes(value).toVector))

        Then("a missing logical record identity or version is never inferred from its carrier")
        results.map(_.isLeft) shouldBe Vector.fill(variants.size)(true)
      }
    }

    "reject the wrong realization revision, role, unknown ID and bare ID" in {
      Given("an admitted capture with an exact selected realization dependency")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val json = _binding_json(captured.projection.bytes.toArray)
        val references = Vector(
          _artifact_reference("realization-main", 24L, "realization"),
          _artifact_reference("realization-main", 23L, "projection"),
          _artifact_reference("realization-unknown", 23L, "realization"),
          Json.fromString("realization-main"),
          _realization_artifact_reference.mapObject(_.remove("artifactRevision")),
          _realization_artifact_reference.mapObject(_.add("extra", Json.fromBoolean(true)))
        )
        When("binding admission resolves each supplied reference against the exact selected capture")
        val results = references.map(reference => _decode_binding(captured, realization, _binding_bytes(json.mapObject(_.add("realizationArtifactReference", reference))).toVector))

        Then("existing artifact ID alone never satisfies reference equality or a missing version")
        results.map(_.isLeft) shouldBe Vector.fill(references.size)(true)
      }
    }

    "reject malformed strict JSON and duplicate members at every reference depth" in {
      Given("an admitted capture and binding bytes with malformed encoding, BOM, trailing data or duplicate members")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val content = new String(captured.projection.bytes.toArray, StandardCharsets.UTF_8)
        val variants = Vector(
          Vector(0xc3.toByte, 0x28.toByte),
          Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ captured.projection.bytes,
          (content + "{}").getBytes(StandardCharsets.UTF_8).toVector,
          content.replace("\"profile\":", "\"profile\":\"ccdm-projection-binding-v3\",\"profile\":").getBytes(StandardCharsets.UTF_8).toVector,
          content.replace("\"recordRevision\":7", "\"recordRevision\":7,\"recordRevision\":7").getBytes(StandardCharsets.UTF_8).toVector,
          content.replace("\"artifactRevision\":23", "\"artifactRevision\":23,\"artifactRevision\":23").getBytes(StandardCharsets.UTF_8).toVector
        )
        When("each malformed serialization crosses strict admission")
        val results = variants.map(value => _decode_binding(captured, realization, value))

        Then("encoding, duplicate or trailing data never becomes an admitted binding")
        results.map(_.isLeft) shouldBe Vector.fill(variants.size)(true)
      }
    }

    "reject all legacy and mismatched profile pairs" in {
      Given("an admitted V3 realization and current binding grammar carrying legacy or mismatched declarations")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = _admitted_realization(root)
        val json = _binding_json(captured.projection.bytes.toArray)
        val pairs = Vector(("ccdm-projection-binding-v1", "1.0"), ("ccdm-projection-binding-v2", "2.0"), ("ccdm-projection-binding-v3", "2.0"), ("ccdm-projection-binding-v2", "3.0"))
        val variants = pairs.map { case (profile, schema) =>
          json.mapObject(_.add("profile", Json.fromString(profile)).add("schemaVersion", Json.fromString(schema)))
        }
        val oldrealizations = Vector(realization.copy(profile = "ccdm-realization-v1", schemaVersion = "1.0"), realization.copy(profile = "ccdm-realization-v2", schemaVersion = "2.0"), realization.copy(schemaVersion = "2.0"))
        When("binding admission receives each legacy declaration and each incompatible selected realization")
        val bindingresults = variants.map(value => _decode_binding(captured, realization, _binding_bytes(value).toVector))
        val realizationresults = oldrealizations.map(value => _decode_binding(captured, value, captured.projection.bytes))
        val legacypairs = variants.take(2).zip(oldrealizations.take(2)).map { case (binding, oldrealization) =>
          _decode_binding(captured, oldrealization, _binding_bytes(binding).toVector)
        }

        Then("none is reinterpreted, migrated or admitted by fallback")
        bindingresults.map(_.isLeft) shouldBe Vector.fill(variants.size)(true)
        realizationresults.map(_.isLeft) shouldBe Vector.fill(oldrealizations.size)(true)
        legacypairs.map(_.isLeft) shouldBe Vector(true, true)
      }
    }

    "reject null and inconsistent supplied capture metadata before returning a partial projection" in {
      Given("one admitted capture with each required metadata boundary independently absent or inconsistent")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        _admitted_realization(root)
        val projection = captured.projection
        val selected = captured.realizationpackage.realization.reference
        val snapshot = captured.realizationpackage.sourcesnapshots.head.reference
        val invalidreference = projection.reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])
        val variants = Vector(
          null,
          captured.copy(projection = null),
          captured.copy(realizationpackage = null),
          captured.copy(realizationpackage = captured.realizationpackage.copy(realization = null)),
          captured.copy(realizationpackage = captured.realizationpackage.copy(sourcesnapshots = null)),
          captured.copy(projection = projection.copy(reference = null)),
          captured.copy(projection = projection.copy(reference = invalidreference)),
          captured.copy(projection = projection.copy(reference = projection.reference.copy(artifactId = null.asInstanceOf[InternalModelArtifactId]))),
          captured.copy(projection = projection.copy(reference = projection.reference.copy(role = null))),
          captured.copy(projection = projection.copy(reference = projection.reference.copy(role = InternalModelArtifactRole.Realization))),
          captured.copy(projection = projection.copy(path = null)),
          captured.copy(projection = projection.copy(bytes = null)),
          captured.copy(projection = projection.copy(dependencies = null)),
          captured.copy(projection = projection.copy(dependencies = Vector(null))),
          captured.copy(projection = projection.copy(dependencies = Vector(selected, selected))),
          captured.copy(projection = projection.copy(dependencies = Vector(snapshot, selected))),
          captured.copy(projection = projection.copy(dependencies = Vector(projection.reference, selected))),
          captured.copy(projection = projection.copy(dependencies = Vector(snapshot))),
          captured.copy(projection = projection.copy(dependencies = Vector(selected.copy(artifactRevision = InternalModelArtifactRevision.from(24L).toOption.get)))),
          captured.copy(projection = projection.copy(dependencies = Vector(selected.copy(role = InternalModelArtifactRole.Projection)))),
          captured.copy(realizationpackage = captured.realizationpackage.copy(realization = captured.realizationpackage.realization.copy(reference = selected.copy(role = InternalModelArtifactRole.Projection))))
        )
        When("the public captured reconstruction boundary receives each malformed handoff")
        val results = variants.map(InternalModelProjectionContinuityValidator.validateVerified)

        Then("every malformed capture yields structured rejection without a partial value")
        results.map(_.isSuccess) shouldBe Vector.fill(variants.size)(false)
      }
    }

    "reconstruct only the original captured package after carrier files change" in {
      Given("one complete admitted inventory capture and then malformed on-disk carrier and artifact payloads")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        val realization = InternalModelSemanticRealizationValidator.validateVerified(captured.realizationpackage)
        realization.isSuccess shouldBe true
        Vector("manifest.yaml", "snapshots/model.json", "realizations/main.json", "projections/main.json").foreach { path =>
          _write(root.resolve("src/main/internal-model").resolve(path), "malformed changed file".getBytes(StandardCharsets.UTF_8))
        }
        When("pure captured projection reconstruction consumes the original handoff")
        val result = InternalModelProjectionContinuityValidator.validateVerified(captured)

        Then("the original realization and exact binding reference remain admitted without any reread")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map(_.realization) shouldBe realization.toOption
        result.toOption.map(_.binding.realizationArtifactReference) shouldBe Some(captured.realizationpackage.realization.reference)
      }
    }

    "retain unknown source-owned versions without replacing them with known record or artifact revisions" in {
      Given("otherwise complete V2 source and V3 realization records with explicit null source revision")
      _with_fixture("display-one") { root =>
        _set_source_revision(root, None)
        _admitted_realization(root)
        When("the all-eight recorded projections are reconstructed")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("every source revision remains unknown beside the independently declared binding and artifact versions")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map(value => (
          value.realization.sourceReferences.map(_.source.revision).distinct,
          value.binding.bindingReference.recordRevision.value,
          value.binding.realizationArtifactReference.artifactRevision.value
        )) shouldBe Some((Vector(None), 7L, 23L))
      }
    }

    "retain element and relationship target kinds when opaque identity strings are equal" in {
      Given("an admitted sole-condition Entity gap and a Mono relation with the same opaque identity string as its affected element")
      _with_distinct_gap_fixture() { root =>
        _rename_semantic_identity(root, "r-mono-domain", "e-entity")
        _admitted_realization(root)
        When("the exact gap condition target and relation role witnesses are reconstructed")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("the element gap retains its condition source while the relationship retains its separate role source")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map(value => (
          value.entityModel.gaps.head.sourceGap.attribution.sourceLocator,
          value.entityModel.gaps.head.sourceGap.affectedSemanticTargetId.map(_.value),
          value.monoKoto.subjects.head.references.head.sourceReference.id,
          value.realization.sourceReferences.filter(_.target.exists(_.semanticIdentity == "e-entity")).flatMap(_.target.map(_.semanticIdentityKind)).distinct.sorted
        )) shouldBe Some(("anchor-e-entity-gap-condition", Some("e-entity"), "e-entity", Vector("element", "relationship")))
      }
    }
  }


  "Retained V3 evidence and exact nested semantics" should {
    "retain complete enrichment, association, sequence and condition sidecars beside direct role attribution" in {
      Given("an all-eight admitted ledger with separate Event enrichment and a Workflow conflict condition")
      _with_fixture("display-one", workflowconditions = Vector("recorded-conflict")) { root =>
        _add_evidence(root, "EventModelProjection", "element", "e-event", "attributed enrichment", enrichment = true)
        val realization = _admitted_realization(root)
        When("the projections consume complete link arrays with direct role attribution")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("all source and evidence records remain complete while normal DTO attribution uses only its one direct role witness")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map(value => (
          value.realization == realization,
          value.realization.enrichmentAssertions.map(_.content),
          value.realization.canonicalAssertions.flatMap(_.association).map(_.associationRole).distinct.sorted,
          value.realization.canonicalAssertions.filter(_.content.startsWith("sequence-key:")).map(_.content).sorted,
          value.binding.views.find(_.family == "EventModelProjection").get.records.find(_.semanticIdentity == "e-event").get.enrichmentAssertionIds,
          value.eventModel.subjects.find(_.sourceSubject.semanticTargetId.value == "e-event").get.sourceSubject.attribution.sourceLocator,
          value.workflow.flows.head.sourceFlow.condition.conflict,
          value.workflow.flows.head.sourceFlow.sourceEndpoint.condition.conflict,
          value.workflow.flows.head.sourceFlow.condition.availability,
          value.workflow.flows.head.sourceFlow.condition.authorization,
          value.eventModel.subjects.map(_.navigationTarget).distinct
        )) shouldBe Some((
          true, Vector("attributed enrichment"), Vector("owner", "subject"),
          Vector("sequence-key:flow-1", "sequence-key:step-1"),
          Vector("z-e-event-attributed-enrichment"),
          "anchor-e-event-kind-EventModelEvent", Some("recorded-conflict"), None,
          "unverified", "unverified", Vector(None)
        ))
      }
    }

    "retain all optional Structure and Classification affected associations without altering parent attribution" in {
      Given("source-backed affected endpoint, relationship and subject claims beside the complete required association graph")
      _with_fixture("display-one") { root =>
        _add_evidence(root, "StructureViewProjection", "relationship", "r-structure-assertion", "association:affected-endpoint:element:e-entity", _association_from("association:affected-endpoint:element:e-entity"))
        _add_evidence(root, "ClassificationViewProjection", "relationship", "r-classification-assertion", "association:affected-relationship:relationship:r-classification", _association_from("association:affected-relationship:relationship:r-classification"))
        _add_evidence(root, "ClassificationViewProjection", "relationship", "r-classification-assertion", "association:affected-subject:element:e-dimension", _association_from("association:affected-subject:element:e-dimension"))
        _add_evidence(root, "ClassificationViewProjection", "relationship", "r-classification-assertion", "association:affected-endpoint:element:e-dimension", _association_from("association:affected-endpoint:element:e-dimension"))
        _admitted_realization(root)
        When("the existing nested Structure and Classification constructors consume exact association targets")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("all optional affected IDs remain exact and each nested DTO retains its direct local role source")
        withClue(result.show) { result.isSuccess shouldBe true }
        result.toOption.map(value => (
          value.structureView.relations.head.assertions.head.sourceAssertion.affectedEndpointId.map(_.value),
          value.structureView.relations.head.assertions.head.sourceAssertion.attribution.sourceLocator,
          value.classificationView.dimensions.head.assertions.head.sourceAssertion.affectedSemanticRelationshipId.map(_.value),
          value.classificationView.dimensions.head.assertions.head.sourceAssertion.affectedSubjectId.map(_.value),
          value.classificationView.dimensions.head.assertions.head.sourceAssertion.affectedEndpointId.map(_.value),
          value.classificationView.dimensions.head.assertions.head.sourceAssertion.attribution.sourceLocator
        )) shouldBe Some((
          Some("e-entity"), "anchor-r-structure-assertion-role-Cardinality",
          Some("r-classification"), Some("e-dimension"), Some("e-dimension"),
          "anchor-r-classification-assertion-role-ClassificationDimensionValue"
        ))
      }
    }

    "reject an optional affected Structure endpoint outside the exact parent relation" in {
      Given("an admitted source-backed affected endpoint claim to an in-scope element outside the owner relation")
      _with_fixture("display-one") { root =>
        _add_evidence(root, "StructureViewProjection", "relationship", "r-structure-assertion", "association:affected-endpoint:element:e-event", _association_from("association:affected-endpoint:element:e-event"))
        _admitted_realization(root)
        When("the Structure adapter maps that exact optional affected endpoint")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("the endpoint rejects without substituting an eligible parent endpoint")
        result.isSuccess shouldBe false
        result.show should include("affects an endpoint outside its owner relation")
      }
    }

    "reject duplicate source-owned sequence keys after admitting distinct flow-step targets" in {
      Given("two source-backed selected Use Case steps with distinct exact targets and the same source-owned sequence key")
      _with_fixture("display-one", duplicatesteptarget = true) { root =>
        _add_element(root, "e-step-distinct", "use-case-flow-step")
        val realizationpath = root.resolve("src/main/internal-model/realizations/main.json")
        val realization = _update_realization_relationship(_realization_json(Files.readAllBytes(realizationpath)), "r-usecase-step-duplicate")(_.add("targetElementIdentity", Json.fromString("e-step-distinct")))
        _replace_realization(root, _realization_bytes(realization))
        _replace_sequence_key(root, "sequence-key:step-2", "sequence-key:step-1")
        _admitted_realization(root)
        When("the exact selected flow's distinct steps are ordered by their admitted sequence keys")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("duplicate source keys reject independently of target membership and array position")
        result.isSuccess shouldBe false
        result.show should include("duplicate source-owned step sequence keys")
      }
    }

    "reject unknown, cross-scope and duplicate semantic selections with a previously admitted realization" in {
      Given("one admitted all-eight realization and bindings with missing or duplicate exact selected identities")
      _with_fixture("display-one") { root =>
        val captured = _capture(root)
        _admitted_realization(root)
        val binding = _binding_json(captured.projection.bytes.toArray)
        val variants = Vector(
          _update_binding_record(binding, "EventModelProjection", "e-event")(_.add("semanticIdentity", Json.fromString("unknown-event"))),
          _update_binding_record(binding, "EventModelProjection", "e-event")(_.add("semanticIdentity", Json.fromString("other-component:event"))),
          _update_binding_view(binding, "EventModelProjection")(records => records :+ records.head),
          _update_binding_record(binding, "EventModelProjection", "e-event")(_.add("recordKind", Json.fromString("relationship"))),
          _update_binding_record(binding, "EventModelProjection", "r-event-cause")(_.add("targetElementIdentity", Json.fromString("e-entity")))
        )
        When("each changed binding is reconstructed against that same capture")
        val results = variants.map(value => InternalModelProjectionContinuityValidator.validateVerified(captured.copy(projection = captured.projection.copy(bytes = _binding_bytes(value).toVector))))

        Then("unknown identities, inferred scope, duplicate selections, wrong kind and forged endpoint fields all reject")
        results.map(_.isSuccess) shouldBe Vector.fill(variants.size)(false)
      }
    }

    "compress every admitted condition kind exactly and retain its complete source ledger" in {
      Vector("absence", "ambiguity", "conflict", "malformed", "limitation", "authorization-redaction", "availability-staleness").foreach { kind =>
        Given(s"one admitted $kind condition on the exact Workflow relation")
        _with_fixture("display-one", workflowconditions = Vector("condition-detail")) { root =>
          val path = root.resolve("src/main/internal-model/realizations/main.json")
          val realization = _realization_json(Files.readAllBytes(path)).mapObject { objectvalue =>
            objectvalue.add("conditions", Json.fromValues(objectvalue("conditions").get.asArray.get.map(_.mapObject(_.add("kind", Json.fromString(kind))))))
          }
          _replace_realization(root, _realization_bytes(realization))
          _admitted_realization(root)
          When("the admitted condition is compressed into the existing DTO")
          val result = InternalModelProjectionContinuityValidator.validate(root)

          Then("the exact optional slot or typed limitation is retained without splitting source conditions or changing adapter sentinels")
          withClue(result.show) { result.isSuccess shouldBe true }
          result.toOption.map { value =>
            val condition = value.workflow.flows.head.sourceFlow.condition
            (
              condition.explicitAbsence, condition.ambiguity, condition.conflict, condition.malformedEvidence,
              condition.limitations, condition.redaction, condition.staleness, condition.availability, condition.authorization,
              value.realization.conditions.head.kind
            )
          } shouldBe Some((
            if kind == "absence" then Some("condition-detail") else None,
            if kind == "ambiguity" then Some("condition-detail") else None,
            if kind == "conflict" then Some("condition-detail") else None,
            if kind == "malformed" then Some("condition-detail") else None,
            if kind == "limitation" then Vector("condition-detail")
            else if Set("authorization-redaction", "availability-staleness").contains(kind) then Vector(s"$kind:condition-detail")
            else Vector.empty,
            None, None, "unverified", "unverified", kind
          ))
        }
      }
    }
  }

  private def _elements(label: String, reverseinputs: Boolean = false, entityrole: String = "EntityModelEntity"): Vector[FixtureElement] = {
    val values = Vector(
    FixtureElement("e-mono", "Mono", Some("Mono")),
    FixtureElement("e-koto", "Koto", Some("Koto")),
    FixtureElement("e-usecase", "use-case", Some("use-case")),
    FixtureElement("e-flow", "use-case-flow", None),
    FixtureElement("e-step", "use-case-flow-step", None),
    FixtureElement("e-entity", entityrole, Some(entityrole)),
    FixtureElement("e-command", "EventModelCommand", Some("EventModelCommand")),
    FixtureElement("e-event", "EventModelEvent", Some("EventModelEvent")),
    FixtureElement("e-dimension", "ClassificationPowertypeDimension", Some("ClassificationPowertypeDimension")),
    FixtureElement("e-workflow", "WorkflowProjectionWorkflow", Some("WorkflowProjectionWorkflow")),
    FixtureElement("e-activity", "WorkflowProjectionActivity", Some("WorkflowProjectionActivity")),
    FixtureElement("e-state-machine", "StateMachineProjectionStateMachine", Some("StateMachineProjectionStateMachine")),
    FixtureElement("e-state-a", "StateMachineProjectionState", Some("StateMachineProjectionState")),
    FixtureElement("e-state-b", "StateMachineProjectionState", Some("StateMachineProjectionState")),
      FixtureElement("e-trigger", "StateMachineProjectionTrigger", Some("StateMachineProjectionTrigger"))
    )
    if reverseinputs then values.reverse else values
  }

  private def _relationships(
    workflowowner: String = "e-workflow",
    eventtarget: String = "e-event",
    workflowconditions: Vector[String] = Vector.empty,
    reverseinputs: Boolean = false,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    duplicateflowtarget: Boolean = false,
    duplicatesteptarget: Boolean = false,
    stepsource: String = "e-flow"
  ): Vector[FixtureRelationship] = {
    val conditionids = workflowconditions.indices.map(index => s"c-r-workflow-flow-$index").toVector
    val values = Vector(
    FixtureRelationship("r-mono-domain", "StructuralDomain", "e-mono", "e-entity"),
    FixtureRelationship("r-koto-temporal", "BehavioralTemporal", "e-koto", "e-command"),
    FixtureRelationship("r-usecase-actor", "Actor", "e-usecase", "e-entity"),
    FixtureRelationship("r-usecase-flow", "UseCaseCommunicationFlow", "e-usecase", "e-flow"),
    FixtureRelationship("r-usecase-step", "UseCaseCommunicationFlowStep", stepsource, "e-step", sequence = Some("step-1")),
    FixtureRelationship("r-entity-metadata", "IdentityMetadata", "e-entity", "e-entity"),
    FixtureRelationship("r-event-cause", "EventModelCausalAssertion", "e-command", eventtarget),
    FixtureRelationship("r-structure", "StructureAssociation", "e-entity", "e-mono"),
    FixtureRelationship("r-structure-assertion", "Cardinality", "e-entity", "e-mono", structureowners.map(owner => FixtureAssociation("owner", "relationship", owner))),
    FixtureRelationship("r-classification", "ClassificationGeneralization", "e-dimension", "e-dimension", Vector(FixtureAssociation("subject", "element", "e-dimension"))),
    FixtureRelationship("r-classification-assertion", "ClassificationDimensionValue", "e-dimension", "e-dimension", classificationowners.map(owner => FixtureAssociation("owner", "element", owner)) :+ FixtureAssociation("subject", "element", "e-dimension")),
    FixtureRelationship("r-workflow-flow", "WorkflowProjectionFlowRelation", "e-workflow", "e-activity", Vector(FixtureAssociation("owner", "element", workflowowner)), Some("flow-1"), conditionids),
    FixtureRelationship("r-state-transition", "StateMachineProjectionTransitionRelation", "e-state-a", "e-state-b", statemachineowners.map(owner => FixtureAssociation("owner", "element", owner))),
      FixtureRelationship("r-state-adjunct", "StateMachineProjectionTransitionAdjunctRelation", "e-state-b", "e-trigger", Vector(FixtureAssociation("owner", "relationship", "r-state-transition")))
    ) ++
      (if duplicateflowtarget then Vector(FixtureRelationship("r-usecase-flow-duplicate", "UseCaseCommunicationFlow", "e-usecase", "e-flow")) else Vector.empty) ++
      (if duplicatesteptarget then Vector(FixtureRelationship("r-usecase-step-duplicate", "UseCaseCommunicationFlowStep", "e-flow", "e-step", sequence = Some("step-2"))) else Vector.empty)
    if reverseinputs then values.reverse else values
  }

  private def _realization(
    label: String,
    workflowowner: String = "e-workflow",
    eventtarget: String = "e-event",
    workflowconditions: Vector[String] = Vector.empty,
    entityconditions: Vector[String] = Vector.empty,
    reverseinputs: Boolean = false,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    duplicateflowtarget: Boolean = false,
    duplicatesteptarget: Boolean = false,
    stepsource: String = "e-flow",
    entityrole: String = "EntityModelEntity"
  ): Array[Byte] = {
    val elements = _elements(label, reverseinputs, entityrole)
    val relationships = _relationships(workflowowner, eventtarget, workflowconditions, reverseinputs, structureowners, classificationowners, statemachineowners, duplicateflowtarget, duplicatesteptarget, stepsource)
    val facts = (elements.map(element => ("element", element.identity, s"kind:${element.kind}")) ++ relationships.flatMap { relationship =>
      Vector(("relationship", relationship.identity, s"role:${relationship.role}")) ++
        relationship.associations.map(association => ("relationship", relationship.identity, s"association:${association.role}:${association.kind}:${association.identity}")) ++
        relationship.sequence.toVector.map(value => ("relationship", relationship.identity, s"sequence-key:$value"))
    }).sortBy { case (kind, identity, content) => (kind, identity, content) }
    val assertions = facts.map { case (kind, identity, content) => _assertion(s"a-$identity-${_token(content)}", kind, identity, s"ref-$identity-${_token(content)}", content, _association_from(content)) }.sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val references = facts.map { case (kind, identity, content) => _reference(s"ref-$identity-${_token(content)}", kind, identity, s"anchor-$identity-${_token(content)}") }.sortBy(_.hcursor.get[String]("referenceId").toOption.get)
    val elementjson = elements.sortBy(_.identity).map { element =>
      val ids = assertions.filter(assertion => assertion.hcursor.get[String]("semanticIdentityKind").toOption.contains("element") && assertion.hcursor.get[String]("semanticIdentity").toOption.contains(element.identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted
      _element(element.identity, element.kind, label, ids, if element.identity == "e-entity" then entityconditions.indices.map(index => s"c-e-entity-$index").toVector else Vector.empty)
    }
    val relationshipjson = relationships.sortBy(_.identity).map { relationship =>
      val ids = assertions.filter(assertion => assertion.hcursor.get[String]("semanticIdentityKind").toOption.contains("relationship") && assertion.hcursor.get[String]("semanticIdentity").toOption.contains(relationship.identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted
      _relationship(relationship, ids)
    }
    val conditions = (workflowconditions.zipWithIndex.map { case (detail, index) =>
      _condition(s"c-r-workflow-flow-$index", "conflict", "relationship", "r-workflow-flow", "ref-r-workflow-flow-role-WorkflowProjectionFlowRelation", detail)
    } ++ entityconditions.zipWithIndex.map { case (detail, index) =>
      _condition(s"c-e-entity-$index", "conflict", "element", "e-entity", s"ref-e-entity-kind-${_token(entityrole)}", detail)
    }).sortBy(_.hcursor.get[String]("conditionId").toOption.get)
    _serialize(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions),
      "conditions" -> Json.fromValues(conditions),
      "elements" -> Json.fromValues(elementjson),
      "enrichmentAssertions" -> Json.arr(),
      "profile" -> Json.fromString("ccdm-realization-v3"),
      "realizationReference" -> _record_reference("realization-all-eight", 11L),
      "relationships" -> Json.fromValues(relationshipjson),
      "schemaVersion" -> Json.fromString("3.0"),
      "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_snapshot_reference))
    ))
  }

  private def _binding(
    label: String,
    profile: String = "ccdm-projection-binding-v3",
    schema: String = "3.0",
    sequencewitness: Option[String] = Some("step-1"),
    roleoverride: Option[(String, String)] = None,
    workflowowner: String = "e-workflow",
    eventtarget: String = "e-event",
    workflowconditions: Vector[String] = Vector.empty,
    entityconditions: Vector[String] = Vector.empty,
    entitygap: Option[String] = None,
    reverseinputs: Boolean = false,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    duplicateflowtarget: Boolean = false,
    duplicatesteptarget: Boolean = false,
    stepsource: String = "e-flow",
    entityrole: String = "EntityModelEntity"
  ): Array[Byte] = {
    val elements = _elements(label, reverseinputs, entityrole)
    val relationships = _relationships(workflowowner, eventtarget, workflowconditions, reverseinputs, structureowners, classificationowners, statemachineowners, duplicateflowtarget, duplicatesteptarget, stepsource)
    def _assertion_ids_(identity: String): Vector[String] = {
      val relation = relationships.find(_.identity == identity)
      relation match {
        case Some(value) => (Vector(s"a-$identity-${_token(s"role:${value.role}")}") ++ value.associations.map(association => s"a-$identity-${_token(s"association:${association.role}:${association.kind}:${association.identity}")}") ++ value.sequence.toVector.map(entry => s"a-$identity-${_token(s"sequence-key:$entry")}")).sorted
        case None => elements.find(_.identity == identity).toVector.map(element => s"a-$identity-${_token(s"kind:${element.kind}")}")
      }
    }
    def _record_(kind: String, identity: String, role: String): Json = {
      val boundrole = roleoverride.collect { case (targetidentity, replacement) if targetidentity == identity => replacement }.getOrElse(role)
      val conditionids = relationships.find(_.identity == identity).map(_.conditionids).getOrElse(if identity == "e-entity" then entityconditions.indices.map(index => s"c-e-entity-$index").toVector else Vector.empty)
      Json.obj(
        "canonicalAssertionIds" -> Json.fromValues(_assertion_ids_(identity).map(Json.fromString)),
        "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
        "enrichmentAssertionIds" -> Json.arr(),
        "recordKind" -> Json.fromString(kind),
        "semanticIdentity" -> Json.fromString(identity),
        "sequenceAssertionId" -> (if role == "UseCaseCommunicationFlowStep" then {
          val sequence = if identity == "r-usecase-step" then sequencewitness else relationships.find(_.identity == identity).flatMap(_.sequence)
          sequence.map(entry => Json.fromString(s"a-$identity-${_token(s"sequence-key:$entry")}")).getOrElse(Json.Null)
        } else if role == "WorkflowProjectionFlowRelation" then Json.fromString(s"a-$identity-${_token("sequence-key:flow-1")}") else Json.Null),
        "viewRole" -> Json.fromString(boundrole)
      )
    }
    def _gap_record_(identity: String, role: String, field: String): Json = Json.obj(
      "canonicalAssertionIds" -> Json.arr(),
      "conditionIds" -> Json.fromValues(entityconditions.indices.map(index => Json.fromString(s"c-e-entity-$index"))),
      "enrichmentAssertionIds" -> Json.arr(),
      "recordKind" -> Json.fromString("gap"),
      "requestedFieldOrScope" -> Json.fromString(field),
      "semanticIdentity" -> Json.fromString(identity),
      "sequenceAssertionId" -> Json.Null,
      "viewRole" -> Json.fromString(role)
    )
    def _view_(family: String, entries: Vector[(String, String, String)]): Json = Json.obj(
      "family" -> Json.fromString(family),
      "records" -> Json.fromValues(entries.map { case (kind, identity, role) => _record_(kind, identity, role) }.sortBy(value => (value.hcursor.get[String]("recordKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("viewRole").toOption.get)))
    )
    _serialize(Json.obj(
      "profile" -> Json.fromString(profile),
      "bindingReference" -> _record_reference("binding-all-eight", 7L),
      "realizationArtifactReference" -> _realization_artifact_reference,
      "schemaVersion" -> Json.fromString(schema),
      "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "views" -> Json.fromValues(Vector(
        _view_("MonoKotoProjection", Vector(("element", "e-mono", "Mono"), ("element", "e-koto", "Koto"), ("relationship", "r-mono-domain", "StructuralDomain"), ("relationship", "r-koto-temporal", "BehavioralTemporal"))),
        _view_("UseCaseCommunicationProjection", Vector(("element", "e-usecase", "use-case"), ("relationship", "r-usecase-actor", "Actor"), ("relationship", "r-usecase-flow", "UseCaseCommunicationFlow"), ("relationship", "r-usecase-step", "UseCaseCommunicationFlowStep")) ++
          (if duplicateflowtarget then Vector(("relationship", "r-usecase-flow-duplicate", "UseCaseCommunicationFlow")) else Vector.empty) ++
          (if duplicatesteptarget then Vector(("relationship", "r-usecase-step-duplicate", "UseCaseCommunicationFlowStep")) else Vector.empty)),
        Json.obj("family" -> Json.fromString("EntityModelProjection"), "records" -> Json.fromValues((Vector(_record_("element", "e-entity", entityrole), _record_("relationship", "r-entity-metadata", "IdentityMetadata")) ++ entitygap.toVector.map(field => _gap_record_("e-entity", "EntityModelEntity", field))).sortBy(value => (value.hcursor.get[String]("recordKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("viewRole").toOption.get)))),
        _view_("EventModelProjection", Vector(("element", "e-command", "EventModelCommand"), ("element", "e-event", "EventModelEvent"), ("relationship", "r-event-cause", "EventModelCausalAssertion"))),
        _view_("StructureViewProjection", Vector(("relationship", "r-structure", "StructureAssociation"), ("relationship", "r-structure-assertion", "Cardinality"))),
        _view_("ClassificationViewProjection", Vector(("element", "e-dimension", "ClassificationPowertypeDimension"), ("relationship", "r-classification", "ClassificationGeneralization"), ("relationship", "r-classification-assertion", "ClassificationDimensionValue"))),
        _view_("WorkflowProjection", Vector(("element", "e-workflow", "WorkflowProjectionWorkflow"), ("element", "e-activity", "WorkflowProjectionActivity"), ("relationship", "r-workflow-flow", "WorkflowProjectionFlowRelation"))),
        _view_("StateMachineProjection", Vector(("element", "e-state-machine", "StateMachineProjectionStateMachine"), ("element", "e-state-a", "StateMachineProjectionState"), ("element", "e-state-b", "StateMachineProjectionState"), ("element", "e-trigger", "StateMachineProjectionTrigger"), ("relationship", "r-state-transition", "StateMachineProjectionTransitionRelation"), ("relationship", "r-state-adjunct", "StateMachineProjectionTransitionAdjunctRelation")))
      ))
    ))
  }

  private def _snapshot(
    label: String,
    workflowowner: String = "e-workflow",
    eventtarget: String = "e-event",
    workflowconditions: Vector[String] = Vector.empty,
    reverseinputs: Boolean = false,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    duplicateflowtarget: Boolean = false,
    duplicatesteptarget: Boolean = false,
    stepsource: String = "e-flow",
    entityrole: String = "EntityModelEntity"
  ): Array[Byte] = {
    val facts = (_elements(label, reverseinputs, entityrole).map(element => ("element", element.identity, s"kind:${element.kind}")) ++ _relationships(workflowowner, eventtarget, workflowconditions, reverseinputs, structureowners, classificationowners, statemachineowners, duplicateflowtarget, duplicatesteptarget, stepsource).flatMap { relationship =>
      Vector(("relationship", relationship.identity, s"role:${relationship.role}")) ++
        relationship.associations.map(association => ("relationship", relationship.identity, s"association:${association.role}:${association.kind}:${association.identity}")) ++
        relationship.sequence.toVector.map(value => ("relationship", relationship.identity, s"sequence-key:$value"))
    }).map { case (kind, identity, content) =>
      Json.obj(
        "componentIdentity" -> Json.fromString("component-all-eight"),
        "content" -> Json.fromString(content),
        "limitations" -> Json.arr(),
        "projectionContextIdentity" -> Json.fromString("context-all-eight"),
        "semanticIdentity" -> Json.fromString(identity),
        "semanticIdentityKind" -> Json.fromString(kind),
        "sourceAnchor" -> Json.fromString(s"anchor-$identity-${_token(content)}")
      )
    }
    _serialize(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts.sortBy(_fact_key))),
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source
    ))
  }

  private def _assertion(id: String, kind: String, identity: String, referenceid: String, content: String, association: Option[Json]): Json =
    Json.obj(
      "assertionId" -> Json.fromString(id),
      "association" -> association.getOrElse(Json.Null),
      "conditionIds" -> Json.arr(),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _association_from(content: String): Option[Json] = content.split(":", -1).toVector match {
    case Vector("association", role, kind, identity) => Some(Json.obj("associationRole" -> Json.fromString(role), "relatedSemanticIdentity" -> Json.fromString(identity), "relatedSemanticIdentityKind" -> Json.fromString(kind)))
    case _ => None
  }

  private def _reference(id: String, kind: String, identity: String, anchor: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(id),
      "snapshotReference" -> _snapshot_reference,
      "source" -> _source,
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kind))
    )

  private def _condition(id: String, kind: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(id),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _element(identity: String, kind: String, label: String, canonicalids: Vector[String], conditionids: Vector[String] = Vector.empty, enrichmentids: Vector[String] = Vector.empty): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "kind" -> Json.fromString(kind),
      "label" -> Json.fromString(label)
    )

  private def _relationship(value: FixtureRelationship, canonicalids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(value.conditionids.map(Json.fromString)),
      "direction" -> Json.fromString("source-to-target"),
      "enrichmentAssertionIds" -> Json.arr(),
      "identity" -> Json.fromString(value.identity),
      "label" -> Json.fromString(value.identity),
      "role" -> Json.fromString(value.role),
      "sourceElementIdentity" -> Json.fromString(value.source),
      "targetElementIdentity" -> Json.fromString(value.target)
    )

  private def _with_fixture(
    label: String,
    workflowowner: String = "e-workflow",
    eventtarget: String = "e-event",
    workflowconditions: Vector[String] = Vector.empty,
    entityconditions: Vector[String] = Vector.empty,
    reverseinputs: Boolean = false,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    duplicateflowtarget: Boolean = false,
    duplicatesteptarget: Boolean = false,
    stepsource: String = "e-flow",
    entityrole: String = "EntityModelEntity"
  )(f: Path => Unit): Unit = {
    val snapshot = _snapshot(label, workflowowner, eventtarget, workflowconditions, reverseinputs, structureowners, classificationowners, statemachineowners, duplicateflowtarget, duplicatesteptarget, stepsource, entityrole)
    val realization = _realization(label, workflowowner, eventtarget, workflowconditions, entityconditions, reverseinputs, structureowners, classificationowners, statemachineowners, duplicateflowtarget, duplicatesteptarget, stepsource, entityrole)
    val binding = _binding(label, workflowowner = workflowowner, eventtarget = eventtarget, workflowconditions = workflowconditions, entityconditions = entityconditions, reverseinputs = reverseinputs, structureowners = structureowners, classificationowners = classificationowners, statemachineowners = statemachineowners, duplicateflowtarget = duplicateflowtarget, duplicatesteptarget = duplicatesteptarget, stepsource = stepsource, entityrole = entityrole)
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", 17L, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", 23L, Vector(_snapshot_reference)),
      _artifact("projection-main", "projections/main.json", "projection", 31L, Vector(_realization_artifact_reference))
    )
    val root = _fixture_root("all-eight-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
      f(root)
    } finally _delete_tree(root)
  }

  private def _rewrite_manifest_dependency(root: Path, dependencies: Vector[Json]): Unit = {
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", 17L, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", 23L, Vector(_snapshot_reference)),
      _artifact("projection-main", "projections/main.json", "projection", 31L, dependencies)
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _replace_projection(root: Path, binding: Array[Byte]): Unit =
    _write(root.resolve("src/main/internal-model/projections/main.json"), binding)

  private def _with_rejected_fixture(
    entityconditions: Vector[String] = Vector.empty,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine"),
    admitrealization: Boolean = true
  )(mutate: Path => Unit = _ => ()): Boolean = {
    var rejected = false
    _with_fixture(
      "display-one",
      entityconditions = entityconditions,
      structureowners = structureowners,
      classificationowners = classificationowners,
      statemachineowners = statemachineowners
    ) { root =>
      mutate(root)
      if admitrealization then _admitted_realization(root)
      When("the selected package and binding are interpreted at the changed boundary")
      rejected = !InternalModelProjectionContinuityValidator.validate(root).isSuccess
    }
    rejected
  }

  private def _binding_json(bytes: Array[Byte]): Json =
    parse(new String(bytes, StandardCharsets.UTF_8)).fold(error => throw IllegalArgumentException(error.message), identity)

  private def _binding_bytes(binding: Json): Array[Byte] = _serialize(binding)

  private def _update_binding_views(binding: Json)(change: Vector[Json] => Vector[Json]): Json = {
    val root = binding.asObject.getOrElse(throw IllegalArgumentException("binding fixture root is not an object"))
    val views = root("views").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("binding fixture views are absent"))
    Json.fromJsonObject(root.add("views", Json.fromValues(change(views))))
  }

  private def _update_binding_view(binding: Json, family: String)(change: Vector[Json] => Vector[Json]): Json =
    _update_binding_views(binding) { views =>
      views.map { view =>
        if view.hcursor.get[String]("family").toOption.contains(family) then {
          val objectvalue = view.asObject.getOrElse(throw IllegalArgumentException(s"binding fixture $family view is not an object"))
          val records = objectvalue("records").flatMap(_.asArray).getOrElse(throw IllegalArgumentException(s"binding fixture $family records are absent"))
          Json.fromJsonObject(objectvalue.add("records", Json.fromValues(change(records))))
        } else view
      }
    }

  private def _update_binding_record(binding: Json, family: String, semanticidentity: String)(change: JsonObject => JsonObject): Json =
    _update_binding_view(binding, family) { records =>
      records.map { record =>
        if record.hcursor.get[String]("semanticIdentity").toOption.contains(semanticidentity) then
          Json.fromJsonObject(change(record.asObject.getOrElse(throw IllegalArgumentException(s"binding fixture $semanticidentity record is not an object"))))
        else record
      }
    }

  private def _realization_json(bytes: Array[Byte]): Json =
    parse(new String(bytes, StandardCharsets.UTF_8)).fold(error => throw IllegalArgumentException(error.message), identity)

  private def _realization_bytes(realization: Json): Array[Byte] = _serialize(realization)

  private def _update_realization_relationship(realization: Json, identity: String)(change: JsonObject => JsonObject): Json = {
    val root = realization.asObject.getOrElse(throw IllegalArgumentException("realization fixture root is not an object"))
    val relationships = root("relationships").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("realization fixture relationships are absent"))
    val updated = relationships.map { relationship =>
      if relationship.hcursor.get[String]("identity").toOption.contains(identity) then
        Json.fromJsonObject(change(relationship.asObject.getOrElse(throw IllegalArgumentException(s"realization fixture $identity relationship is not an object"))))
      else relationship
    }
    Json.fromJsonObject(root.add("relationships", Json.fromValues(updated)))
  }

  private def _duplicate_direct_role_witness(realization: Json, assertionid: String, referenceid: String): Json = {
    val root = realization.asObject.getOrElse(throw IllegalArgumentException("realization fixture root is not an object"))
    val assertions = root("canonicalAssertions").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("realization fixture canonical assertions are absent"))
    val original = assertions.find(_.hcursor.get[String]("assertionId").toOption.contains("a-e-event-kind-EventModelEvent")).flatMap(_.asObject).getOrElse(throw IllegalArgumentException("Event role assertion is absent"))
    val duplicate = Json.fromJsonObject(original.add("assertionId", Json.fromString(assertionid)).add("sourceReferenceId", Json.fromString(referenceid)))
    val elements = root("elements").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("realization fixture elements are absent"))
    val updatedelements = elements.map { element =>
      if element.hcursor.get[String]("identity").toOption.contains("e-event") then {
        val objectvalue = element.asObject.getOrElse(throw IllegalArgumentException("Event element is not an object"))
        val ids = objectvalue("canonicalAssertionIds").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("Event canonical assertion IDs are absent"))
        Json.fromJsonObject(objectvalue.add("canonicalAssertionIds", Json.fromValues((ids :+ Json.fromString(assertionid)).sortBy(_.asString.getOrElse("")))))
      } else element
    }
    Json.fromJsonObject(root.add("canonicalAssertions", Json.fromValues((assertions :+ duplicate).sortBy(_.hcursor.get[String]("assertionId").toOption.getOrElse("")))).add("elements", Json.fromValues(updatedelements)))
  }

  private def _append_source_reference(realization: Json, referenceid: String): Json = {
    val root = realization.asObject.getOrElse(throw IllegalArgumentException("realization fixture root is not an object"))
    val references = root("sourceReferences").flatMap(_.asArray).getOrElse(throw IllegalArgumentException("realization fixture source references are absent"))
    val original = references.find(_.hcursor.get[String]("referenceId").toOption.contains("ref-e-event-kind-EventModelEvent")).flatMap(_.asObject).getOrElse(throw IllegalArgumentException("Event source reference is absent"))
    val duplicate = Json.fromJsonObject(original.add("referenceId", Json.fromString(referenceid)))
    Json.fromJsonObject(root.add("sourceReferences", Json.fromValues((references :+ duplicate).sortBy(_.hcursor.get[String]("referenceId").toOption.getOrElse("")))))
  }

  private def _append_binding_canonical_assertion(binding: Json, family: String, identity: String, assertionid: String): Json =
    _update_binding_record(binding, family, identity) { objectvalue =>
      val ids = objectvalue("canonicalAssertionIds").flatMap(_.asArray).getOrElse(throw IllegalArgumentException(s"binding fixture $identity canonical assertion IDs are absent"))
      objectvalue.add("canonicalAssertionIds", Json.fromValues((ids :+ Json.fromString(assertionid)).sortBy(_.asString.getOrElse(""))))
    }

  private def _omit_projection(root: Path): Unit = {
    Files.delete(root.resolve("src/main/internal-model/projections/main.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", 17L, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", 23L, Vector(_snapshot_reference))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _duplicate_projection(root: Path): Unit = {
            val binding = Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))
    _write(root.resolve("src/main/internal-model/projections/second.json"), binding)
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", 17L, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", 23L, Vector(_snapshot_reference)),
      _artifact("projection-main", "projections/main.json", "projection", 31L, Vector(_realization_artifact_reference)),
      _artifact("projection-second", "projections/second.json", "projection", 31L, Vector(_realization_artifact_reference))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _replace_realization(root: Path, realization: Array[Byte]): Unit =
    _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)

  private def _replace_realization_and_projection(root: Path, realization: Array[Byte], binding: Array[Byte]): Unit = {
    _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
    _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
  }

  private def _with_distinct_gap_fixture()(f: Path => Unit): Unit = {
    val snapshot = _serialize(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(Vector(
        _fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"),
        _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"),
        _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain"),
        _fact("element", "e-entity", "anchor-e-entity-role-source-a", "kind:EntityModelEntity"),
        _fact("element", "e-entity", "anchor-e-entity-gap-condition", "gap condition basis")
      ).sortBy(_fact_key))),
      "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source
    ))
    val assertions = Vector(
      _assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono", None),
      _assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case", None),
      _assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain", None),
      _assertion("a-e-entity-role", "element", "e-entity", "ref-e-entity-role-source-a", "kind:EntityModelEntity", None)
    ).sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val references = Vector(
      _reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"),
      _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"),
      _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain"),
      _reference("ref-e-entity-role-source-a", "element", "e-entity", "anchor-e-entity-role-source-a"),
      _reference("ref-e-entity-gap-condition", "element", "e-entity", "anchor-e-entity-gap-condition")
    ).sortBy(_.hcursor.get[String]("referenceId").toOption.get)
    val realization = _serialize(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions),
      "conditions" -> Json.arr(_condition("c-e-entity-gap", "conflict", "element", "e-entity", "ref-e-entity-gap-condition", "entity-gap")),
      "elements" -> Json.fromValues(Vector(
        _element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")),
        _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")),
        _element("e-entity", "EntityModelEntity", "Entity", Vector("a-e-entity-role"), Vector("c-e-entity-gap"))
      ).sortBy(_.hcursor.get[String]("identity").toOption.get)),
      "enrichmentAssertions" -> Json.arr(), "profile" -> Json.fromString("ccdm-realization-v3"),
      "realizationReference" -> _record_reference("realization-distinct-gap", 11L),
      "relationships" -> Json.arr(_relationship(FixtureRelationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase"), Vector("a-r-mono-domain-role-StructuralDomain"))),
      "schemaVersion" -> Json.fromString("3.0"), "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "sourceReferences" -> Json.fromValues(references), "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_snapshot_reference))
    ))
    def _record_(kind: String, identity: String, role: String, assertions: Vector[String] = Vector.empty, conditions: Vector[String] = Vector.empty, field: Option[String] = None): Json = Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(assertions.map(Json.fromString)), "conditionIds" -> Json.fromValues(conditions.map(Json.fromString)), "enrichmentAssertionIds" -> Json.arr(), "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "sequenceAssertionId" -> Json.Null, "viewRole" -> Json.fromString(role)
    ).mapObject(objectvalue => field.map(value => objectvalue.add("requestedFieldOrScope", Json.fromString(value))).getOrElse(objectvalue))
    val families = Vector(
      "MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono")), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))),
      "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))),
      "EntityModelProjection" -> Vector(_record_("element", "e-entity", "EntityModelEntity", Vector("a-e-entity-role"), Vector("c-e-entity-gap")), _record_("gap", "e-entity", "EntityModelValue", conditions = Vector("c-e-entity-gap"), field = Some("entity-gap"))),
      "EventModelProjection" -> Vector.empty[Json], "StructureViewProjection" -> Vector.empty[Json], "ClassificationViewProjection" -> Vector.empty[Json], "WorkflowProjection" -> Vector.empty[Json], "StateMachineProjection" -> Vector.empty[Json]
    )
    val binding = _serialize(Json.obj(
      "bindingReference" -> _record_reference("binding-distinct-gap", 7L),
      "profile" -> Json.fromString("ccdm-projection-binding-v3"), "realizationArtifactReference" -> _realization_artifact_reference,
      "schemaVersion" -> Json.fromString("3.0"), "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })
    ))
    val root = _fixture_root("distinct-gap-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(Vector(_artifact("snapshot-model", "snapshots/model.json", "source-snapshot", 17L, Vector.empty), _artifact("realization-main", "realizations/main.json", "realization", 23L, Vector(_snapshot_reference)), _artifact("projection-main", "projections/main.json", "projection", 31L, Vector(_realization_artifact_reference)))))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
      f(root)
    } finally _delete_tree(root)
  }

  private def _fact(kind: String, identity: String, anchor: String, content: String): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-all-eight"),
      "content" -> Json.fromString(content),
      "limitations" -> Json.arr(),
      "projectionContextIdentity" -> Json.fromString("context-all-eight"),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceAnchor" -> Json.fromString(anchor)
    )



  private def _add_evidence(
    root: Path,
    family: String,
    kind: String,
    identity: String,
    content: String,
    association: Option[Json] = None,
    enrichment: Boolean = false
  ): Unit = {
    val assertionid = s"z-$identity-${_token(content)}"
    val referenceid = s"ref-extra-$identity-${_token(content)}"
    val anchor = s"anchor-extra-$identity-${_token(content)}"
    val lane = if enrichment then "enrichmentAssertions" else "canonicalAssertions"
    val links = if enrichment then "enrichmentAssertionIds" else "canonicalAssertionIds"
    val targetkey = if kind == "element" then "elements" else "relationships"
    val snapshotpath = root.resolve("src/main/internal-model/snapshots/model.json")
    val realizationpath = root.resolve("src/main/internal-model/realizations/main.json")
    val projectionpath = root.resolve("src/main/internal-model/projections/main.json")
    val snapshot = _binding_json(Files.readAllBytes(snapshotpath)).mapObject { objectvalue =>
      objectvalue.add("basis", objectvalue("basis").get.mapObject { basis =>
        val facts = basis("facts").get.asArray.get :+ _fact(kind, identity, anchor, content)
        basis.add("facts", Json.fromValues(facts.sortBy(_fact_key)))
      })
    }
    val realization = _realization_json(Files.readAllBytes(realizationpath)).mapObject { objectvalue =>
      val assertions = objectvalue(lane).get.asArray.get :+ _assertion(assertionid, kind, identity, referenceid, content, association)
      val references = objectvalue("sourceReferences").get.asArray.get :+ _reference(referenceid, kind, identity, anchor)
      val targets = objectvalue(targetkey).get.asArray.get.map { target =>
        if target.hcursor.get[String]("identity").toOption.contains(identity) then target.mapObject { current =>
          current.add(links, Json.fromValues((current(links).get.asArray.get :+ Json.fromString(assertionid)).sortBy(_.asString.get)))
        } else target
      }
      objectvalue.add(lane, Json.fromValues(assertions.sortBy(_.hcursor.get[String]("assertionId").toOption.get)))
        .add("sourceReferences", Json.fromValues(references.sortBy(_.hcursor.get[String]("referenceId").toOption.get)))
        .add(targetkey, Json.fromValues(targets))
    }
    val binding = _update_binding_record(_binding_json(Files.readAllBytes(projectionpath)), family, identity) { objectvalue =>
      objectvalue.add(links, Json.fromValues((objectvalue(links).get.asArray.get :+ Json.fromString(assertionid)).sortBy(_.asString.get)))
    }
    _write(snapshotpath, _serialize(snapshot))
    _replace_realization_and_projection(root, _serialize(realization), _serialize(binding))
  }

  private def _add_element(root: Path, identity: String, kind: String): Unit = {
    val path = root.resolve("src/main/internal-model/realizations/main.json")
    val realization = _realization_json(Files.readAllBytes(path)).mapObject { objectvalue =>
      val elements = objectvalue("elements").get.asArray.get :+ _element(identity, kind, identity, Vector.empty)
      objectvalue.add("elements", Json.fromValues(elements.sortBy(_.hcursor.get[String]("identity").toOption.get)))
    }
    _replace_realization(root, _realization_bytes(realization))
    _add_evidence(root, "UseCaseCommunicationProjection", "element", identity, s"kind:$kind")
  }

  private def _replace_sequence_key(root: Path, prior: String, successor: String): Unit =
    Vector("snapshots/model.json", "realizations/main.json").foreach { path =>
      val target = root.resolve("src/main/internal-model").resolve(path)
      val changed = _rewrite_strings(_binding_json(Files.readAllBytes(target)), value => if value == prior then successor else value)
      _write(target, _serialize(changed))
    }


  private def _capture(root: Path): InternalModelVerifiedProjectionContinuityPackage = {
    val result = InternalModelPackageValidator.verifiedProjectionContinuity(root)
    withClue(result.show) { result.isSuccess shouldBe true }
    result.toOption.get
  }

  private def _decode_binding(
    captured: InternalModelVerifiedProjectionContinuityPackage,
    realization: InternalModelSemanticRealization,
    bytes: Vector[Byte]
  ): Either[String, InternalModelProjectionBinding] =
    InternalModelProjectionBindingCodec.decode(
      captured.projection.copy(bytes = bytes),
      captured.realizationpackage.realization.reference,
      realization
    )

  private def _rewrite_strings(json: Json, change: String => String): Json =
    json.asString match {
      case Some(value) => Json.fromString(change(value))
      case None => json.arrayOrObject(
        json,
        values => Json.fromValues(values.map(value => _rewrite_strings(value, change))),
        objectvalue => Json.fromJsonObject(JsonObject.fromIterable(objectvalue.toVector.map { case (key, value) => key -> _rewrite_strings(value, change) }))
      )
    }

  private def _rename_semantic_identity(root: Path, prior: String, successor: String): Unit = {
    val paths = Vector("snapshots/model.json", "realizations/main.json", "projections/main.json")
    paths.foreach { path =>
      val target = root.resolve("src/main/internal-model").resolve(path)
      val json = _binding_json(Files.readAllBytes(target))
      val changed = _rewrite_strings(json, value =>
        if value == prior then successor
        else if value.startsWith("association:") && value.endsWith(s":$prior") then value.dropRight(prior.length) + successor
        else value
      )
      val ordered = if path == "snapshots/model.json" then changed.mapObject { objectvalue =>
        objectvalue.add("basis", objectvalue("basis").get.mapObject { basis =>
          basis.add("facts", Json.fromValues(basis("facts").get.asArray.get.sortBy(_fact_key)))
        })
      } else if path == "realizations/main.json" then changed.mapObject { objectvalue =>
        Vector("elements", "relationships").foldLeft(objectvalue) { (current, key) =>
          current.add(key, Json.fromValues(current(key).get.asArray.get.sortBy(_.hcursor.get[String]("identity").toOption.get)))
        }
      } else _update_binding_views(changed) { views =>
        views.map(_.mapObject { objectvalue =>
          objectvalue.add("records", Json.fromValues(objectvalue("records").get.asArray.get.sortBy(value => (
            value.hcursor.get[String]("recordKind").toOption.get,
            value.hcursor.get[String]("semanticIdentity").toOption.get,
            value.hcursor.get[String]("viewRole").toOption.get
          ))))
        })
      }
      _write(target, _serialize(ordered))
    }
  }

  private def _set_source_revision(root: Path, revision: Option[String]): Unit = {
    val snapshotpath = root.resolve("src/main/internal-model/snapshots/model.json")
    val realizationpath = root.resolve("src/main/internal-model/realizations/main.json")
    val value = revision.map(Json.fromString).getOrElse(Json.Null)
    val snapshot = _binding_json(Files.readAllBytes(snapshotpath)).mapObject { objectvalue =>
      objectvalue.add("source", objectvalue("source").get.mapObject(_.add("revision", value)))
    }
    val realization = _realization_json(Files.readAllBytes(realizationpath)).mapObject { objectvalue =>
      objectvalue.add("sourceReferences", Json.fromValues(objectvalue("sourceReferences").get.asArray.get.map(_.mapObject { reference =>
        reference.add("source", reference("source").get.mapObject(_.add("revision", value)))
      })))
    }
    _write(snapshotpath, _serialize(snapshot))
    _replace_realization(root, _serialize(realization))
  }


  private def _record_reference(id: String, revision: Long): Json =
    Json.obj("recordId" -> Json.fromString(id), "recordRevision" -> Json.fromLong(revision))

  private def _artifact_reference(id: String, revision: Long, role: String): Json =
    Json.obj("artifactId" -> Json.fromString(id), "artifactRevision" -> Json.fromLong(revision), "role" -> Json.fromString(role))

  private def _artifact(id: String, path: String, role: String, revision: Long, dependencies: Vector[Json]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "artifactRevision" -> Json.fromLong(revision),
      "dependsOn" -> Json.fromValues(dependencies),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(role)
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] =
    _serialize(Json.obj(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("continuity-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(41),
      "schemaVersion" -> Json.fromString("2.0")
    ))

  private def _fact_key(value: Json): (String, String, String, String, String) =
    (
      value.hcursor.get[String]("componentIdentity").toOption.get,
      value.hcursor.get[String]("projectionContextIdentity").toOption.get,
      value.hcursor.get[String]("semanticIdentityKind").toOption.get,
      value.hcursor.get[String]("semanticIdentity").toOption.get,
      value.hcursor.get[String]("sourceAnchor").toOption.get
    )

  private def _fixture_root(prefix: String): Path = {
    Files.createDirectories(_work_root)
    Files.createTempDirectory(_work_root, prefix)
  }

  private def _admitted_realization(root: Path): InternalModelSemanticRealization = {
    val result = InternalModelSemanticRealizationValidator.validate(root)
    withClue("the earlier V3 realization admission must succeed: " + result.show) {
      result.isSuccess shouldBe true
    }
    result.toOption.get
  }

  private def _roundtrip_binding(value: InternalModelProjectionContinuity): Either[String, InternalModelProjectionBinding] = {
    val projection = InternalModelVerifiedProjection(
      reference = InternalModelArtifactReference(
        InternalModelArtifactId.from("projection-main").toOption.get,
        InternalModelArtifactRevision.from(31L).toOption.get,
        InternalModelArtifactRole.Projection
      ),
      path = "projections/main.json",
      required = true,
      dependencies = Vector(value.binding.realizationArtifactReference),
      bytes = InternalModelProjectionContinuityValidator.encode(value.binding)
    )
    InternalModelProjectionBindingCodec.decode(projection, value.binding.realizationArtifactReference, value.realization)
  }

  private def _token(value: String): String =
    value.map {
      case character if character.isLetterOrDigit => character
      case _ => '-'
    }

  private def _serialize(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit = {
    require(root.toAbsolutePath.normalize().getParent == _work_root, "cleanup is confined to one exact fixture subtree")
    if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
      val entries = Files.walk(root)
      try entries.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally entries.close()
    }
  }
}
