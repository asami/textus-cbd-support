package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest

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
 * @version Sep. 28, 2026
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
  private val _source_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"),
    "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"),
    "revision" -> Json.fromString("revision-1"),
    "sha256" -> Json.fromString(_sha256(_source_raw))
  )

  "Internal-model projection continuity validation" should {
    "reconstruct all eight substantive Phase 9 values from one V2 manifest handoff" in {
      Given("a canonical package containing a V2 realization, closed binding, source-local role witnesses, associations, and sequence witnesses")
      _with_fixture("display-one") { root =>
        When("the package is admitted through the projection-continuity entry point")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("every constructor has a normal nonempty value with exact nested identities and retained V2 sidecar bytes")
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
            InternalModelProjectionContinuityValidator.encode(value.binding)
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
          "ccdm-realization-v2",
          "ccdm-projection-binding-v2",
          _binding("display-one").toVector
      ))
      }
    }

    "retain exact IdentityMetadata for admitted Value and Aggregate subjects" in {
      Vector("EntityModelValue", "EntityModelAggregate").foreach { entityrole =>
        Given(s"a coherent V2 source snapshot, realization, binding, and manifest for an admitted $entityrole subject with direct IdentityMetadata")
        _with_fixture(s"metadata-$entityrole", entityrole = entityrole) { root =>
          When("the package is admitted through the projection-continuity entry point")
          val result = InternalModelProjectionContinuityValidator.validate(root)

          Then("the exact subject role, identity, metadata owner, source attribution, and canonical binding bytes are retained")
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
                  value.binding.canonicalBytes == InternalModelProjectionContinuityValidator.encode(value.binding)
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

    "preserve opaque identities when labels and canonical-input encounter order vary" in {
      Given("generated nonempty labels and independently reversed pre-canonical fixture input for the same ledger")
      forAll(Gen.oneOf("EntityModelEntity", "EntityModelValue", "EntityModelAggregate"), Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { (entityrole, label) =>
        _with_fixture(label, reverseinputs = true, entityrole = entityrole) { reversedroot =>
          _with_fixture(label, reverseinputs = false, entityrole = entityrole) { orderedroot =>
            When("labels and pre-canonical encounter order differ while the persisted bytes remain canonical")
            val reversed = InternalModelProjectionContinuityValidator.validate(reversedroot)
            val ordered = InternalModelProjectionContinuityValidator.validate(orderedroot)

            Then("the eight projection identities, exact Entity Model metadata owner, nested sequence key, direct V2 source anchor, and suppressed navigation stay exact")
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
              left.binding.canonicalBytes == right.binding.canonicalBytes
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
      Given("an otherwise valid all-eight V2 handoff with selected Mono-Koto or Entity metadata relationships but no matching selected source subject")
      When("projection reconstruction consumes the selected nested relationship records")
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

    "admit the closed V1 pairing without recasting it as V2" in {
      Given("a V1 manifest package and V1 binding with only directly representable Mono-Koto records")
      _with_v1_fixture() { root =>
        When("the V1 realization and binding are read through the same verified package handoff")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("the original V1 profiles and canonical binding bytes remain exact")
        result.toOption.map(value => (value.realization.profile, value.binding.profile, InternalModelProjectionContinuityValidator.encode(value.binding))) shouldBe
          Some(("ccdm-realization-v1", "ccdm-projection-binding-v1", _v1_binding().toVector))
      }
    }

    "fail closed for each separately repinned persisted-boundary mutation" which {
      "reject a V1 binding paired with a V2 realization" in {
        Given("a package whose projection artifact and manifest digest are repinned to a V1 binding")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", profile = "ccdm-projection-binding-v1", schema = "1.0"))
          When("the version pair reaches binding admission")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the pairing rejects without a compatibility fallback")
          result.isSuccess shouldBe false
        }
      }

      "reject a non-dependent projection manifest" in {
        Given("a package whose manifest digest is repinned to omit the realization dependency")
        _with_fixture("display-one") { root =>
          _rewrite_manifest_dependency(root, Vector("snapshot-model"))
          When("the package handoff selects its projection")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the selection rejects before semantic reconstruction")
          result.isSuccess shouldBe false
        }
      }

      "reject noncanonical projection bytes after artifact repinning" in {
        Given("a manifest whose projection digest names noncanonical binding bytes")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one").dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8))
          When("the binding parser reaches its canonical-byte check")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the parser rejects instead of normalizing whitespace")
          result.isSuccess shouldBe false
        }
      }

      "reject an unsupported direct role with coherent package digests" in {
        Given("a canonical binding whose Event relationship role differs from its direct witness")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", roleoverride = Some("r-event-cause" -> "EventModelUnknown")))
          When("the role is checked against the V2 realization")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the adapter rejects without label interpretation or source selection")
          result.isSuccess shouldBe false
        }
      }

      "reject an Event endpoint without exact Event-local evidence" in {
        Given("a coherent realization whose Event relation targets an admitted but non-Event-bound element")
        _with_fixture("display-one", eventtarget = "e-entity") { root =>
          When("the Event adapter builds endpoint-local evidence")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the endpoint rejects rather than borrowing relationship evidence")
          result.isSuccess shouldBe false
        }
      }

      "reject a Workflow owner association to a retained Activity" in {
        Given("a coherent V2 realization whose Workflow flow owner is an Activity rather than a Workflow")
        _with_fixture("display-one", workflowowner = "e-activity") { root =>
          When("the adapter maps the exact owner association")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the association rejects without owner inference")
          result.isSuccess shouldBe false
        }
      }

      "reject an omitted ordered sequence witness with complete target links" in {
        Given("a canonical binding that clears only its Use Case step sequence field")
        _with_fixture("display-one") { root =>
          _replace_projection(root, _binding("display-one", sequencewitness = None))
          When("the ordered Use Case relationship is admitted")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the sequence rejects without using array order")
          result.isSuccess shouldBe false
        }
      }

      "reject multiple conflicting conditions without a winner" in {
        Given("a coherent realization and binding with two conflict conditions on one Workflow relation")
        _with_fixture("display-one", workflowconditions = Vector("conflict-one", "conflict-two")) { root =>
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
          When("the one-condition gap is admitted")
          val accepted = InternalModelProjectionContinuityValidator.validate(root)
          Then("the Entity constructor retains its exact affected identity and requested field")
          accepted.toOption.map(_.entityModel.gaps.map(gap => (gap.sourceGap.affectedSemanticTargetId.map(_.value), gap.sourceGap.boundedFieldOrScope))) shouldBe
            Some(Vector((Some("e-entity"), "entity-gap")))
        }
        Given("the same coherent ledger with a gap field that differs from the sole attributable condition detail")
        _with_fixture("display-one", entityconditions = Vector("entity-gap")) { root =>
          _replace_projection(root, _binding("display-one", entityconditions = Vector("entity-gap"), entitygap = Some("forged-gap")))
          When("the gap requests text other than its exact condition detail")
          val rejected = InternalModelProjectionContinuityValidator.validate(root)
          Then("the gap rejects without paraphrasing or manufacturing bounded scope")
          rejected.isSuccess shouldBe false
        }
      }

      "reject absent and duplicate present projections after manifest repinning" in {
        Given("one package without a present projection and one package with two independently declared present projections")
        val absent = _with_rejected_fixture() { root =>
          _omit_projection(root)
        }
        val duplicate = _with_rejected_fixture() { root =>
          _duplicate_projection(root)
        }
        When("the package admission boundary selects a projection artifact")
        Then("absence and multiplicity both fail closed before reconstruction")
        Vector(absent, duplicate) shouldBe Vector(true, true)
      }

      "reject closed binding member, family, record-order, and scope mutations" in {
        Given("separately repinned canonical bindings with one closed-shape boundary changed at a time")
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
        When("the repinned bindings cross the closed typed-binding admission boundary")
        Then("unknown members, noncanonical tuple order, and nonmatching scope have no permissive interpretation")
        Vector(unknownmember, familyorder, recordorder, wrongscope) shouldBe Vector(true, true, true, true)
      }

      "reject missing, extra, and lane-promoted assertion links" in {
        Given("separately repinned Event records whose exact canonical assertion link is removed, extended, or moved to enrichment")
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
        When("the exact assertion lanes are compared to the V2 realization")
        Then("no missing, additional, or promoted link is accepted")
        Vector(missing, extra, promoted) shouldBe Vector(true, true, true)
      }

      "reject missing, extra, and assertion-lane-promoted condition links" in {
        Given("a coherent Entity condition and three separately repinned binding link mutations")
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
        When("the exact condition lane is checked against its retained Entity record")
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
        When("ordered relation evidence reaches its local target and graph checks")
        Then("a witness cannot be borrowed across relationships and direction cannot be inferred")
        Vector(wrongtargetsequence, wrongdirection) shouldBe Vector(true, true)
      }

      "reject missing, duplicate, and contradicted local association boundaries" in {
        Given("coherent source-ledger fixtures for a missing Structure owner, duplicate Structure owners, a non-dimension Classification owner, and a non-machine State owner")
        val missingstructureowner = _with_rejected_fixture(structureowners = Vector.empty)()
        val duplicatestructureowner = _with_rejected_fixture(structureowners = Vector("r-mono-domain", "r-structure"))()
        val contradictedclassificationowner = _with_rejected_fixture(classificationowners = Vector("e-entity"))()
        val contradictedstatemachineowner = _with_rejected_fixture(statemachineowners = Vector("e-state-a"))()
        When("the Structure, Classification, and State Machine adapters resolve exact local associations")
        Then("missing, ambiguous, and contradicted association ownership is rejected without a fallback owner")
        Vector(missingstructureowner, duplicatestructureowner, contradictedclassificationowner, contradictedstatemachineowner) shouldBe Vector(true, true, true, true)
      }

      "reject a V1 record with two exact source witnesses and no deterministic winner" in {
        Given("a coherent V1 package whose Mono record has canonical and enrichment assertions backed by different source anchors")
        _with_v1_fixture(hiddenwinner = true) { root =>
          When("V1 attribution would need to select one of the two source witnesses")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("the binding rejects rather than selecting a hidden source winner")
          result.isSuccess shouldBe false
        }
      }

      "reject duplicate V2 direct role witnesses sharing one source reference" in {
        Given("a canonical V2 Event record with two direct role assertions that name the same exact source reference")
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
          When("V2 attribution resolves the direct canonical role witness")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("two matching assertions reject even when their source reference is the same")
          result.isSuccess shouldBe false
          result.show should include("must have exactly one direct canonical role witness")
        }
      }

      "reject duplicate V2 direct role witnesses naming distinct source references" in {
        Given("a canonical V2 Event record with two direct role assertions and two source-reference IDs for the same exact source witness")
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
          When("V2 attribution resolves the direct canonical role witness")
          val result = InternalModelProjectionContinuityValidator.validate(root)
          Then("two matching assertions reject without selecting either source reference")
          result.isSuccess shouldBe false
          result.show should include("must have exactly one direct canonical role witness")
        }
      }

      "reject duplicate selected Use Case flow targets" in {
        Given("a canonical binding and realization with two selected Use Case flows targeting the same flow element")
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
        Given("a canonical binding and realization with two selected steps targeting the same step element")
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
        Given("a canonical binding and realization whose only selected step starts outside the selected flow target")
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

    "attribute unasserted Entity gaps only to their sole condition source within each profile's normal-record rule" in {
      Given("closed V1/V2 fixtures whose selected normal Entity role and gap condition use the profile-admitted source matrix")
      Vector("ccdm-projection-binding-v1", "ccdm-projection-binding-v2").foreach { profile =>
        _with_distinct_gap_fixture(profile) { root =>
          When("a codec-supported EntityModelValue gap is reconstructed without a canonical EntityModelValue role assertion")
          val result = InternalModelProjectionContinuityValidator.validate(root)

          Then("the normal subject and representable gap retain their profile-admitted source attribution and exact bounded gap identity")
          val normalsource = if profile == "ccdm-projection-binding-v1" then "anchor-e-entity-gap-condition" else "anchor-e-entity-role-source-a"
          withClue(s"$profile: ${result.show}") {
            result.toOption.map(value => (value.entityModel.subjects.map(_.sourceSubject.attribution.sourceLocator), value.entityModel.gaps.map(gap => (gap.sourceGap.attribution.sourceLocator, gap.sourceGap.affectedSemanticTargetId.map(_.value), gap.sourceGap.boundedFieldOrScope)), value.binding.canonicalBytes == InternalModelProjectionContinuityValidator.encode(value.binding))) shouldBe
              Some((Vector(normalsource), Vector(("anchor-e-entity-gap-condition", Some("e-entity"), "entity-gap")), true))
          }
        }
      }
      Given("a V1 fixture that selects the normal Entity source A while retaining distinct condition source B")
      _with_distinct_gap_fixture("ccdm-projection-binding-v1", v1distinct = true) { root =>
        When("V1 normal-record attribution would aggregate the selected canonical source A and condition source B")
        val result = InternalModelProjectionContinuityValidator.validate(root)

        Then("the existing hidden-source-winner invariant rejects the distinct normal V1 sources")
        result.isSuccess shouldBe false
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
    val assertions = facts.map { case (kind, identity, content) => _assertion(s"a-$identity-${_token(content)}", kind, identity, s"ref-$identity-${_token(content)}", content, _association_from(content)) }.sortBy(_.noSpaces)
    val references = facts.map { case (kind, identity, content) => _reference(s"ref-$identity-${_token(content)}", kind, identity, s"anchor-$identity-${_token(content)}") }.sortBy(_.noSpaces)
    val elementjson = elements.sortBy(_.identity).map { element =>
      val ids = assertions.filter(_.hcursor.get[String]("semanticIdentity").toOption.contains(element.identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted
      _element(element.identity, element.kind, label, ids, if element.identity == "e-entity" then entityconditions.indices.map(index => s"c-e-entity-$index").toVector else Vector.empty)
    }
    val relationshipjson = relationships.sortBy(_.identity).map { relationship =>
      val ids = assertions.filter(_.hcursor.get[String]("semanticIdentity").toOption.contains(relationship.identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted
      _relationship(relationship, ids)
    }
    val conditions = (workflowconditions.zipWithIndex.map { case (detail, index) =>
      _condition(s"c-r-workflow-flow-$index", "conflict", "relationship", "r-workflow-flow", "ref-r-workflow-flow-role-WorkflowProjectionFlowRelation", detail)
    } ++ entityconditions.zipWithIndex.map { case (detail, index) =>
      _condition(s"c-e-entity-$index", "conflict", "element", "e-entity", s"ref-e-entity-kind-${_token(entityrole)}", detail)
    }).sortBy(_.hcursor.get[String]("conditionId").toOption.get)
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions),
      "conditions" -> Json.fromValues(conditions),
      "elements" -> Json.fromValues(elementjson),
      "enrichmentAssertions" -> Json.arr(),
      "profile" -> Json.fromString("ccdm-realization-v2"),
      "realizationIdentity" -> Json.fromString("realization-all-eight"),
      "relationships" -> Json.fromValues(relationshipjson),
      "schemaVersion" -> Json.fromString("2.0"),
      "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
    ))
  }

  private def _binding(
    label: String,
    profile: String = "ccdm-projection-binding-v2",
    schema: String = "2.0",
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
    _canonical(Json.obj(
      "profile" -> Json.fromString(profile),
      "realizationArtifactId" -> Json.fromString("realization-main"),
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
    }).sortBy { case (kind, identity, content) => (kind, identity, content) }.map { case (kind, identity, content) =>
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
    _canonical(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)),
      "schemaVersion" -> Json.fromString("1.0"),
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
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
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
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main"))
    )
    val root = Files.createTempDirectory("internal-model-projection-continuity-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
      f(root)
    } finally _delete_tree(root)
  }

  private def _rewrite_manifest_dependency(root: Path, dependencies: Vector[String]): Unit = {
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val realization = Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))
    val binding = Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, dependencies)
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _replace_projection(root: Path, binding: Array[Byte]): Unit = {
    _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val realization = Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main"))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _with_rejected_fixture(
    entityconditions: Vector[String] = Vector.empty,
    structureowners: Vector[String] = Vector("r-structure"),
    classificationowners: Vector[String] = Vector("e-dimension"),
    statemachineowners: Vector[String] = Vector("e-state-machine")
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
      rejected = !InternalModelProjectionContinuityValidator.validate(root).isSuccess
    }
    rejected
  }

  private def _binding_json(bytes: Array[Byte]): Json =
    parse(new String(bytes, StandardCharsets.UTF_8)).fold(error => throw IllegalArgumentException(error.message), identity)

  private def _binding_bytes(binding: Json): Array[Byte] = _canonical(binding)

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

  private def _realization_bytes(realization: Json): Array[Byte] = _canonical(realization)

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
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val realization = Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model"))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _duplicate_projection(root: Path): Unit = {
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val realization = Files.readAllBytes(root.resolve("src/main/internal-model/realizations/main.json"))
    val binding = Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))
    _write(root.resolve("src/main/internal-model/projections/second.json"), binding)
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main")),
      _artifact("projection-second", "projections/second.json", "projection", binding, Vector("realization-main"))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _replace_realization(root: Path, realization: Array[Byte]): Unit = {
    _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val binding = Files.readAllBytes(root.resolve("src/main/internal-model/projections/main.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main"))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _replace_realization_and_projection(root: Path, realization: Array[Byte], binding: Array[Byte]): Unit = {
    _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
    _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
    val snapshot = Files.readAllBytes(root.resolve("src/main/internal-model/snapshots/model.json"))
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main"))
    )
    _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
  }

  private def _with_distinct_gap_fixture(profile: String, v1distinct: Boolean = false)(f: Path => Unit): Unit = {
    val v2 = profile == "ccdm-projection-binding-v2"
    val normalsourceb = !v2 && !v1distinct
    val realizationprofile = if v2 then "ccdm-realization-v2" else "ccdm-realization-v1"
    val schemaversion = if v2 then "2.0" else "1.0"
    val _assertion_ = (id: String, kind: String, identity: String, reference: String, content: String) =>
      if v2 then _assertion(id, kind, identity, reference, content, None) else _v1_assertion(id, kind, identity, reference, content)
    val snapshot = _canonical(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(Vector(
        _fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"),
        _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"),
        _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain"),
        _fact("element", "e-entity", "anchor-e-entity-role-source-a", "kind:EntityModelEntity"),
        _fact("element", "e-entity", "anchor-e-entity-gap-condition", if normalsourceb then "kind:EntityModelEntity" else "gap condition basis")
      ).sortBy(value => (value.hcursor.get[String]("semanticIdentityKind").toOption.getOrElse(""), value.hcursor.get[String]("semanticIdentity").toOption.getOrElse(""), value.hcursor.get[String]("sourceAnchor").toOption.getOrElse(""))))), "schemaVersion" -> Json.fromString("1.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source
    ))
    val assertions = Vector(
      _assertion_("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono"),
      _assertion_("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case"),
      _assertion_("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain"),
      _assertion_("a-e-entity-role", "element", "e-entity", if normalsourceb then "ref-e-entity-gap-condition" else "ref-e-entity-role-source-a", "kind:EntityModelEntity")
    )
    val references = Vector(
      _reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"),
      _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"),
      _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain"),
      _reference("ref-e-entity-role-source-a", "element", "e-entity", "anchor-e-entity-role-source-a"),
      _reference("ref-e-entity-gap-condition", "element", "e-entity", "anchor-e-entity-gap-condition")
    ).filter(reference => v2 || v1distinct || !reference.hcursor.get[String]("referenceId").toOption.contains("ref-e-entity-role-source-a")).sortBy(_.noSpaces)
    val realization = _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions.sortBy(_.noSpaces)),
      "conditions" -> Json.arr(_condition("c-e-entity-gap", "conflict", "element", "e-entity", "ref-e-entity-gap-condition", "entity-gap")),
      "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono")), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")), _element("e-entity", "EntityModelEntity", "Entity", Vector("a-e-entity-role"), Vector("c-e-entity-gap"))).sortBy(_.noSpaces)),
      "enrichmentAssertions" -> Json.arr(), "profile" -> Json.fromString(realizationprofile), "realizationIdentity" -> Json.fromString("realization-distinct-gap"),
      "relationships" -> Json.arr(_relationship(FixtureRelationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase"), Vector("a-r-mono-domain-role-StructuralDomain"))),
      "schemaVersion" -> Json.fromString(schemaversion), "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "sourceReferences" -> Json.fromValues(references), "successorLinks" -> Json.arr(), "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
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
    val binding = _canonical(Json.obj("profile" -> Json.fromString(profile), "realizationArtifactId" -> Json.fromString("realization-main"), "schemaVersion" -> Json.fromString(schemaversion), "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")), "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })))
    val root = Files.createTempDirectory("internal-model-distinct-gap-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(Vector(_artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty), _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")), _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main")))))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
      f(root)
    } finally _delete_tree(root)
  }

  private def _with_v1_fixture(hiddenwinner: Boolean = false)(f: Path => Unit): Unit = {
    val snapshot = _v1_snapshot(hiddenwinner)
    val realization = _v1_realization(hiddenwinner)
    val binding = _v1_binding(hiddenwinner)
    val artifacts = Vector(
      _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty),
      _artifact("realization-main", "realizations/main.json", "realization", realization, Vector("snapshot-model")),
      _artifact("projection-main", "projections/main.json", "projection", binding, Vector("realization-main"))
    )
    val root = Files.createTempDirectory("internal-model-projection-continuity-v1-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      _write(root.resolve("src/main/internal-model/projections/main.json"), binding)
      f(root)
    } finally _delete_tree(root)
  }

  private def _v1_snapshot(hiddenwinner: Boolean): Array[Byte] = {
    val facts = (Vector(
      _fact("element", "e-mono", "anchor-e-mono-kind-Mono", "kind:Mono"),
      _fact("element", "e-usecase", "anchor-e-usecase-kind-use-case", "kind:use-case"),
      _fact("relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")
    ) ++ (if hiddenwinner then Vector(_fact("element", "e-mono", "anchor-e-mono-kind-Mono-alternate", "kind:Mono")) else Vector.empty)).sortBy { value =>
      (
        value.hcursor.get[String]("semanticIdentityKind").toOption.get,
        value.hcursor.get[String]("semanticIdentity").toOption.get,
        value.hcursor.get[String]("sourceAnchor").toOption.get
      )
    }
    _canonical(Json.obj(
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(facts)),
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source
    ))
  }

  private def _v1_realization(hiddenwinner: Boolean): Array[Byte] = {
    val assertions = Vector(
      _v1_assertion("a-e-mono-kind-Mono", "element", "e-mono", "ref-e-mono-kind-Mono", "kind:Mono"),
      _v1_assertion("a-e-usecase-kind-use-case", "element", "e-usecase", "ref-e-usecase-kind-use-case", "kind:use-case"),
      _v1_assertion("a-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "ref-r-mono-domain-role-StructuralDomain", "role:StructuralDomain")
    )
    val enrichment = if hiddenwinner then Vector(
      _v1_assertion("z-e-mono-kind-Mono-alternate", "element", "e-mono", "ref-e-mono-kind-Mono-alternate", "kind:Mono")
    ) else Vector.empty
    val references = Vector(
      _reference("ref-e-mono-kind-Mono", "element", "e-mono", "anchor-e-mono-kind-Mono"),
      _reference("ref-e-usecase-kind-use-case", "element", "e-usecase", "anchor-e-usecase-kind-use-case"),
      _reference("ref-r-mono-domain-role-StructuralDomain", "relationship", "r-mono-domain", "anchor-r-mono-domain-role-StructuralDomain")
    ) ++ (if hiddenwinner then Vector(_reference("ref-e-mono-kind-Mono-alternate", "element", "e-mono", "anchor-e-mono-kind-Mono-alternate")) else Vector.empty)
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(assertions),
      "conditions" -> Json.arr(),
      "elements" -> Json.fromValues(Vector(_element("e-mono", "Mono", "Mono", Vector("a-e-mono-kind-Mono"), enrichmentids = enrichment.map(_.hcursor.get[String]("assertionId").toOption.get)), _element("e-usecase", "use-case", "Use case", Vector("a-e-usecase-kind-use-case")))),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString("ccdm-realization-v1"),
      "realizationIdentity" -> Json.fromString("realization-v1"),
      "relationships" -> Json.fromValues(Vector(_relationship(FixtureRelationship("r-mono-domain", "StructuralDomain", "e-mono", "e-usecase"), Vector("a-r-mono-domain-role-StructuralDomain")))),
      "schemaVersion" -> Json.fromString("1.0"),
      "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "sourceReferences" -> Json.fromValues(references.sortBy(_.hcursor.get[String]("referenceId").toOption.get)),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
    ))
  }

  private def _v1_binding(hiddenwinner: Boolean = false): Array[Byte] = {
    def _record_(kind: String, identity: String, role: String, assertions: Vector[String], enrichment: Vector[String] = Vector.empty): Json = Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(assertions.map(Json.fromString)),
      "conditionIds" -> Json.arr(),
      "enrichmentAssertionIds" -> Json.fromValues(enrichment.map(Json.fromString)),
      "recordKind" -> Json.fromString(kind),
      "semanticIdentity" -> Json.fromString(identity),
      "sequenceAssertionId" -> Json.Null,
      "viewRole" -> Json.fromString(role)
    )
    val families = Vector(
      "MonoKotoProjection" -> Vector(_record_("element", "e-mono", "Mono", Vector("a-e-mono-kind-Mono"), if hiddenwinner then Vector("z-e-mono-kind-Mono-alternate") else Vector.empty), _record_("relationship", "r-mono-domain", "StructuralDomain", Vector("a-r-mono-domain-role-StructuralDomain"))),
      "UseCaseCommunicationProjection" -> Vector(_record_("element", "e-usecase", "use-case", Vector("a-e-usecase-kind-use-case"))),
      "EntityModelProjection" -> Vector.empty[Json],
      "EventModelProjection" -> Vector.empty[Json],
      "StructureViewProjection" -> Vector.empty[Json],
      "ClassificationViewProjection" -> Vector.empty[Json],
      "WorkflowProjection" -> Vector.empty[Json],
      "StateMachineProjection" -> Vector.empty[Json]
    )
    _canonical(Json.obj(
      "profile" -> Json.fromString("ccdm-projection-binding-v1"),
      "realizationArtifactId" -> Json.fromString("realization-main"),
      "schemaVersion" -> Json.fromString("1.0"),
      "scope" -> Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")),
      "views" -> Json.fromValues(families.map { case (family, records) => Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records)) })
    ))
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

  private def _v1_assertion(id: String, kind: String, identity: String, referenceid: String, content: String): Json =
    Json.obj(
      "assertionId" -> Json.fromString(id),
      "conditionIds" -> Json.arr(),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kind),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _artifact(id: String, path: String, role: String, bytes: Array[Byte], dependencies: Vector[String]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(role),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("continuity-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    ))
    val digest = _sha256(_canonical(root.remove("packageDigest").toJson))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson)
  }

  private def _token(value: String): String =
    value.map {
      case character if character.isLetterOrDigit => character
      case _ => '-'
    }

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
