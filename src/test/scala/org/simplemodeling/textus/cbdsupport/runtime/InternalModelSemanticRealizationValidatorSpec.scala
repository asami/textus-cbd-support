package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
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
final class InternalModelSemanticRealizationValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private final case class FixtureAssociation(
    assertionid: String,
    role: String,
    relatedidentity: String,
    relatedkind: String,
    referenceid: String,
    referenceanchor: String,
    factanchor: String,
    assertioncontent: Option[String] = None
  )

  private val _printer = Printer.noSpacesSortKeys
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"),
    "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"),
    "revision" -> Json.fromString("revision-1"),
    "sha256" -> Json.fromString(_sha256(_model_raw))
  )

  "Internal-model semantic realization validation" should {
    "admit one exact package/snapshot/realization basis and return its canonical bytes" in {
      Given("a project-private package with one model-context snapshot, one realization, both assertion lanes, and an explicit limitation")
      val realization = _realization()

      _with_fixture(realization) { root =>
        When("the selected realization is admitted through the one structural inventory pass")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the immutable candidate retains its exact scope, dependency set, separate assertion lanes, and byte-exact re-encoding")
        result.isSuccess shouldBe true
        result.toOption.map { candidate =>
          (
            candidate.scope,
            candidate.canonicalAssertions.map(_.assertionId),
            candidate.enrichmentAssertions.map(_.assertionId),
            candidate.consumedSnapshotArtifactIds,
            candidate.canonicalBytes,
            InternalModelSemanticRealizationValidator.encode(candidate)
          )
        } shouldBe Some((
          InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
          Vector("a-customer", "a-relationship", "a-usecase"),
          Vector("z-enrichment"),
          Vector("snapshot-model"),
          realization.toVector,
          realization.toVector
        ))
      }
    }

    "ignore an unrelated source-snapshot whose content is not Phase 10.1-valid" in {
      Given("a structurally valid package with one selected model-context snapshot and one unrelated source-snapshot artifact")
      val realization = _realization()
      val unrelated = "{\"unrelated\":true}\n".getBytes(StandardCharsets.UTF_8)

      _with_fixture(realization, unrelatedsnapshot = Some(unrelated)) { root =>
        When("the realization admits only its manifest-declared source-snapshot dependency")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the unrelated snapshot remains structurally inventory-checked but is not semantically parsed or allowed to reject this realization")
        result.isSuccess shouldBe true
      }
    }

    "bind assertion content to its selected Component and projection context witness" in {
      Given("a canonically ordered model-context snapshot with a foreign Component/context fact sharing the local target and anchor")
      val foreign = _fact("element", "e-customer", "anchor-customer", "Foreign Customer content", Vector.empty).mapObject(
        _.add("componentIdentity", Json.fromString("component-before")).add("projectionContextIdentity", Json.fromString("context-before"))
      )

      _with_fixture(_realization(), additionalfacts = Vector(foreign)) { root =>
        When("the canonical and enrichment lanes resolve their source witness in the selected realization scope")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the exact local witness remains authoritative and the foreign content cannot be borrowed")
        result.isSuccess shouldBe true
      }
    }

    "reject foreign witness content independently in canonical and enrichment assertion lanes" in {
      Given("foreign-before-local facts sharing the selected target and anchor across both supported realization profiles")
      val foreigncontent = "Foreign Customer content"
      val foreign = _fact("element", "e-customer", "anchor-customer", foreigncontent, Vector.empty).mapObject(
        _.add("componentIdentity", Json.fromString("component-before")).add("projectionContextIdentity", Json.fromString("context-before"))
      )

      forAll(Gen.oneOf("ccdm-realization-v1", "ccdm-realization-v2")) { profile =>
        _with_fixture(_realization(profilevalue = profile), additionalfacts = Vector(foreign)) { root =>
          When("both assertion lanes retain the selected local witness content")
          val local = InternalModelSemanticRealizationValidator.validate(root)

          Then("the local content admits under the unchanged profile")
          local.isSuccess shouldBe true
        }
        _with_fixture(_realization(profilevalue = profile, customercontent = foreigncontent), additionalfacts = Vector(foreign)) { root =>
          When("only the canonical assertion borrows the foreign content")
          val canonicalborrow = InternalModelSemanticRealizationValidator.validate(root)

          Then("canonical admission rejects the foreign-scope witness")
          canonicalborrow.isSuccess shouldBe false
        }
        _with_fixture(_realization(profilevalue = profile, enrichmentcontent = foreigncontent), additionalfacts = Vector(foreign)) { root =>
          When("only the enrichment assertion borrows the foreign content")
          val enrichmentborrow = InternalModelSemanticRealizationValidator.validate(root)

          Then("enrichment admission rejects the foreign-scope witness")
          enrichmentborrow.isSuccess shouldBe false
        }
      }
    }

    "retain unchanged V1 profile/version bytes and reject a V2-only assertion field" in {
      Given("a V1 realization with its closed V1 assertion shape and the same V1 realization with an added association field")
      val v1 = _realization()
      val v1withassociation = _realization(v1associationfield = true)

      _with_fixture(v1) { acceptedroot =>
        When("the closed V1 candidate is admitted")
        val accepted = InternalModelSemanticRealizationValidator.validate(acceptedroot)

        Then("its preserved profile and schema version re-encode to the original V1 bytes")
        accepted.toOption.map(candidate => (candidate.profile, candidate.schemaVersion, InternalModelSemanticRealizationValidator.encode(candidate))) shouldBe
          Some(("ccdm-realization-v1", "1.0", v1.toVector))
      }
      _with_fixture(v1withassociation) { rejectedroot =>
        When("a V2-only association field is supplied under the V1 profile")
        val rejected = InternalModelSemanticRealizationValidator.validate(rejectedroot)

        Then("the V1 candidate is rejected without a profile fallback or migration")
        rejected.isSuccess shouldBe false
      }
    }

    "admit V2 associations with distinct exact source anchors for element and relationship targets" in {
      Given("two V2 realizations whose relationship association facts are separately source-backed on their asserting relationship")
      val elementassociation = FixtureAssociation(
        "a-association-owner-element", "owner", "e-customer", "element",
        "ref-association-owner-element", "anchor-association-owner-element", "anchor-association-owner-element"
      )
      val relationshipassociation = FixtureAssociation(
        "a-association-owner-relationship", "owner", "r-related", "relationship",
        "ref-association-owner-relationship", "anchor-association-owner-relationship", "anchor-association-owner-relationship"
      )
      val cases = Vector(
        (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(elementassociation), includerolewitness = true), Vector(elementassociation), false),
        (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(relationshipassociation), includerelatedrelationship = true, includerolewitness = true), Vector(relationshipassociation), true)
      )

      forAll(Gen.oneOf(cases)) { case (realization, associations, includerelatedrelationship) =>
        _with_fixture(realization, associations = associations, includerelatedrelationship = includerelatedrelationship, includerolewitness = true) { root =>
          When("the V2 source-backed association is admitted with its own reference and source-owned anchor")
          val result = InternalModelSemanticRealizationValidator.validate(root)

          Then("the candidate preserves the explicit V2 profile and exact typed association rather than inferring it from relationship endpoints")
          result.toOption.map(candidate => (candidate.profile, candidate.schemaVersion, candidate.canonicalAssertions.flatMap(_.association), InternalModelSemanticRealizationValidator.encode(candidate))) shouldBe
            Some(("ccdm-realization-v2", "2.0", associations.map(association => InternalModelSemanticAssociation(association.role, association.relatedidentity, association.relatedkind)), realization.toVector))
        }
      }
    }

    "fail closed for canonical bytes, profile, package dependency, and source-basis mutations" which {
      "reject duplicate/noncanonical bytes, unsupported profile, missing dependency, changed source metadata, and a CML-style assertion path" in {
        Given("otherwise complete candidate bytes with independently bounded V1 mutations")
        val canonical = _realization()
        val duplicate = Array('{'.toByte) ++ "\"canonicalAssertions\":[],".getBytes(StandardCharsets.UTF_8) ++ canonical.drop(1)
        val cases = Vector(
          duplicate -> Vector("snapshot-model"),
          (canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)) -> Vector("snapshot-model"),
          _realization(profilevalue = "unsupported-profile") -> Vector("snapshot-model"),
          _realization(sourcehash = "sha256:" + ("0" * 64)) -> Vector("snapshot-model"),
          _realization(customeranchor = "model/customer.cml") -> Vector("snapshot-model"),
          canonical -> Vector.empty
        )

        forAll(Gen.oneOf(cases)) { case (realization, dependencies) =>
          _with_fixture(realization, dependencies) { root =>
            When("a byte, profile, source-envelope/anchor, or manifest dependency invariant is changed")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("admission rejects the supplied candidate without normalizing, inferring, or repairing it")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject missing or duplicated semantic identity, wrong scope/endpoints/role/direction, source-kind mismatch, lane collision, hidden limitation, and unknown successor link" in {
        Given("canonical candidates with a focused semantic or visibility invariant mutation")
        val cases = Vector(
          _realization(customeridentity = "e-customer-missing"),
          _realization(contextidentity = "other-context"),
          _realization(relationshiptarget = "e-missing"),
          _realization(rolevalue = ""),
          _realization(directionvalue = "diagonal"),
          _realization(customerreferencekind = "relationship"),
          _realization(enrichmentid = "a-customer"),
          _realization(includecondition = false),
          _realization(successor = Some(_successor("element", "e-customer", "element", "e-unknown", "ref-customer"))),
          _realization(
            successor = Some(_successor("element", "e-customer", "element", "e-usecase", "ref-successor")),
            successortargetless = true
          )
        )

        forAll(Gen.oneOf(cases)) { realization =>
          _with_fixture(realization) { root =>
            When("an exact identity, scope, endpoint, role/direction, source target, lane, condition, or successor invariant is invalid")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("the structural/source-basis boundary fails closed")
            result.isSuccess shouldBe false
          }
        }
      }
    }

    "fail closed for malformed or non-unique V2 association claims" which {
      "reject unknown and cross-scope related identities, invalid role/kind combinations, and enrichment promotion" in {
        Given("V2 association claims that are not an in-scope typed canonical relationship assertion")
        val unknown = FixtureAssociation(
          "a-association-unknown", "owner", "e-unknown", "element",
          "ref-association-unknown", "anchor-association-unknown", "anchor-association-unknown"
        )
        val crossscope = FixtureAssociation(
          "a-association-cross-scope", "owner", "e-other-context", "element",
          "ref-association-cross-scope", "anchor-association-cross-scope", "anchor-association-cross-scope"
        )
        val invalidrole = FixtureAssociation(
          "a-association-invalid-role", "unknown-role", "e-customer", "element",
          "ref-association-invalid-role", "anchor-association-invalid-role", "anchor-association-invalid-role"
        )
        val invalidkind = FixtureAssociation(
          "a-association-invalid-kind", "subject", "r-uses", "relationship",
          "ref-association-invalid-kind", "anchor-association-invalid-kind", "anchor-association-invalid-kind"
        )
        val unknownkind = FixtureAssociation(
          "a-association-unknown-kind", "owner", "e-customer", "unknown-kind",
          "ref-association-unknown-kind", "anchor-association-unknown-kind", "anchor-association-unknown-kind"
        )
        val enrichment = FixtureAssociation(
          "z-enrichment", "owner", "e-customer", "element",
          "ref-customer", "anchor-customer", "anchor-customer"
        )
        val cases = Vector(
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(unknown)), Vector(unknown), false),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(crossscope)), Vector(crossscope), false),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(invalidrole)), Vector(invalidrole), false),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(invalidkind)), Vector(invalidkind), false),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(unknownkind)), Vector(unknownkind), false),
          (_realization(profilevalue = "ccdm-realization-v2", enrichmentassociation = Some(enrichment)), Vector(enrichment), false)
        )

        forAll(Gen.oneOf(cases)) { case (realization, associations, includerelatedrelationship) =>
          _with_fixture(realization, associations = associations, includerelatedrelationship = includerelatedrelationship) { root =>
            When("the candidate supplies an unresolved, incompatible, or enrichment association")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("admission rejects it without promoting evidence, crossing scope, or choosing a typed target")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject duplicate or conflicting role claims and association content or anchor mismatches" in {
        Given("V2 relationship associations that reuse one role or no longer match their exact selected source witness")
        val owner = FixtureAssociation(
          "a-association-owner", "owner", "e-customer", "element",
          "ref-association-owner", "anchor-association-owner", "anchor-association-owner"
        )
        val conflictingowner = FixtureAssociation(
          "a-association-owner-conflict", "owner", "e-usecase", "element",
          "ref-association-owner-conflict", "anchor-association-owner-conflict", "anchor-association-owner-conflict"
        )
        val duplicateowner = owner.copy(
          assertionid = "a-association-owner-duplicate",
          referenceid = "ref-association-owner-duplicate",
          referenceanchor = "anchor-association-owner-duplicate",
          factanchor = "anchor-association-owner-duplicate"
        )
        val wrongcontent = FixtureAssociation(
          "a-association-wrong-content", "owner", "e-customer", "element",
          "ref-association-wrong-content", "anchor-association-wrong-content", "anchor-association-wrong-content", Some("association:owner:element:forged")
        )
        val wronganchor = FixtureAssociation(
          "a-association-wrong-anchor", "owner", "e-customer", "element",
          "ref-association-wrong-anchor", "anchor-association-not-owned", "anchor-association-owner"
        )
        val cases = Vector(
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(owner, duplicateowner)), Vector(owner, duplicateowner)),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(owner, conflictingowner)), Vector(owner, conflictingowner)),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(wrongcontent)), Vector(wrongcontent)),
          (_realization(profilevalue = "ccdm-realization-v2", associations = Vector(wronganchor)), Vector(wronganchor))
        )

        forAll(Gen.oneOf(cases)) { case (realization, associations) =>
          _with_fixture(realization, associations = associations) { root =>
            When("the same asserting relationship has duplicate/conflicting role claims or a source content/anchor mismatch")
            val result = InternalModelSemanticRealizationValidator.validate(root)

            Then("admission rejects the candidate rather than selecting an association or repairing its witness")
            result.isSuccess shouldBe false
          }
        }
      }
    }

    "preserve opaque semantic identity while allowing presentation variation" which {
      "accept generated V2 label changes but reject generated V2 association identity and anchor changes" in {
        Given("generated nonempty presentation labels, semantic ID suffixes, and source-anchor suffixes for a V2 source-backed association")
        val generated = for {
          label <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          identitysuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
          anchorsuffix <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
        } yield (label, identitysuffix, anchorsuffix)

        forAll(generated) { case (label, identitysuffix, anchorsuffix) =>
          val association = FixtureAssociation(
            "a-association-owner", "owner", "e-customer", "element",
            "ref-association-owner", "anchor-association-owner", "anchor-association-owner"
          )
          _with_fixture(_realization(profilevalue = "ccdm-realization-v2", customerlabel = label, usecaselabel = label.reverse, associations = Vector(association)), associations = Vector(association)) { acceptedroot =>
            When("only explanatory labels vary under the same exact V2 source-backed association")
            val accepted = InternalModelSemanticRealizationValidator.validate(acceptedroot)

            Then("presentation variation does not replace the opaque associated identity")
            accepted.isSuccess shouldBe true
          }
          val changedassociation = association.copy(relatedidentity = "e-customer-" + identitysuffix)
          _with_fixture(_realization(profilevalue = "ccdm-realization-v2", associations = Vector(changedassociation)), associations = Vector(changedassociation)) { identityroot =>
            When("a generated associated identity no longer resolves in the retained realization scope")
            val rejected = InternalModelSemanticRealizationValidator.validate(identityroot)

            Then("the changed association identity is rejected rather than reconstructed from its label")
            rejected.isSuccess shouldBe false
          }
          val changedanchorassociation = association.copy(referenceanchor = "anchor-association-owner-" + anchorsuffix)
          _with_fixture(_realization(profilevalue = "ccdm-realization-v2", associations = Vector(changedanchorassociation)), associations = Vector(changedanchorassociation)) { anchorroot =>
            When("a generated association source anchor is not source-owned by the selected snapshot")
            val rejected = InternalModelSemanticRealizationValidator.validate(anchorroot)

            Then("the changed association anchor is rejected without path or hash inference")
            rejected.isSuccess shouldBe false
          }
        }
      }
    }
  }

  private def _realization(
    profilevalue: String = "ccdm-realization-v1",
    schemaversion: String = "",
    contextidentity: String = "context-order",
    customeridentity: String = "e-customer",
    customeranchor: String = "anchor-customer",
    relationshiptarget: String = "e-customer",
    rolevalue: String = "uses",
    directionvalue: String = "source-to-target",
    customerreferencekind: String = "element",
    enrichmentid: String = "z-enrichment",
    includecondition: Boolean = true,
    sourcehash: String = _sha256(_model_raw),
    customercontent: String = "Customer is a party",
    enrichmentcontent: String = "Customer is a party",
    customerlabel: String = "Customer",
    usecaselabel: String = "Place order",
    successor: Option[Json] = None,
    successortargetless: Boolean = false,
    associations: Vector[FixtureAssociation] = Vector.empty,
    enrichmentassociation: Option[FixtureAssociation] = None,
    includerelatedrelationship: Boolean = false,
    includerolewitness: Boolean = false,
    v1associationfield: Boolean = false
  ): Array[Byte] = {
    val v2 = profilevalue == "ccdm-realization-v2"
    val schema = if schemaversion.nonEmpty then schemaversion else if v2 then "2.0" else "1.0"
    val associationids = associations.map(_.assertionid).sorted
    val initialreferences = Vector(
      _reference("ref-customer", customerreferencekind, customeridentity, customeranchor, sourcehash),
      _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship", sourcehash),
      _reference("ref-usecase", "element", "e-usecase", "anchor-usecase", sourcehash)
    )
    val associationreferences = associations.map(association =>
      _reference(association.referenceid, "relationship", "r-uses", association.referenceanchor, sourcehash)
    )
    val rolewitnessreferences = if includerolewitness then Vector(_reference("ref-role", "relationship", "r-uses", "anchor-role", sourcehash)) else Vector.empty
    val relatedreferences = if includerelatedrelationship then Vector(_reference("ref-related", "relationship", "r-related", "anchor-related", sourcehash)) else Vector.empty
    val successorreferences = if successortargetless then Vector(_targetless_reference("ref-successor", "anchor-customer", sourcehash)) else Vector.empty
    val references = (initialreferences ++ associationreferences ++ rolewitnessreferences ++ relatedreferences ++ successorreferences).sortBy(_.noSpaces)
    val relationshipconditions = if includecondition then Vector("c-limitation") else Vector.empty
    val conditions = if includecondition then Vector(_condition("c-limitation", "limitation", "relationship", "r-uses", "ref-relationship", "source limitation")) else Vector.empty
    val canonical = Vector(
      _assertion("a-customer", "element", customeridentity, "ref-customer", customercontent, Vector.empty, None, v2 || v1associationfield),
      _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", relationshipconditions, None, v2 || v1associationfield),
      _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty, None, v2 || v1associationfield)
    ) ++ associations.map(association =>
      _assertion(
        association.assertionid,
        "relationship",
        "r-uses",
        association.referenceid,
        association.assertioncontent.getOrElse(_association_content(association)),
        Vector.empty,
        Some(_association(association)),
        v2 || v1associationfield
      )
    ) ++ (if includerolewitness then Vector(
      _assertion("a-role", "relationship", "r-uses", "ref-role", "role:uses", Vector.empty, None, v2 || v1associationfield)
    ) else Vector.empty) ++ (if includerelatedrelationship then Vector(
      _assertion("a-related", "relationship", "r-related", "ref-related", "Related relationship", Vector.empty, None, v2 || v1associationfield)
    ) else Vector.empty)
    val enrichment = Vector(_assertion(enrichmentid, "element", customeridentity, "ref-customer", enrichmentcontent, Vector.empty, enrichmentassociation.map(_association), v2 || v1associationfield))
    val elements = Vector(
      _element(customeridentity, "customer", customerlabel, Vector("a-customer"), Vector(enrichmentid), Vector.empty),
      _element("e-usecase", "use-case", usecaselabel, Vector("a-usecase"), Vector.empty, Vector.empty)
    ).sortBy(_.noSpaces)
    val relationships = (Vector(
      _relationship("r-uses", "uses", "e-usecase", relationshiptarget, rolevalue, directionvalue, (Vector("a-relationship") ++ associationids ++ (if includerolewitness then Vector("a-role") else Vector.empty)).sorted, Vector.empty, relationshipconditions)
    ) ++ (if includerelatedrelationship then Vector(
      _relationship("r-related", "related", "e-usecase", customeridentity, "related", "source-to-target", Vector("a-related"), Vector.empty, Vector.empty)
    ) else Vector.empty)).sortBy(_.hcursor.get[String]("identity").getOrElse(""))
    val links = successor.toVector
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(canonical.sortBy(_.noSpaces)),
      "conditions" -> Json.fromValues(conditions),
      "elements" -> Json.fromValues(elements),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString(profilevalue),
      "realizationIdentity" -> Json.fromString("realization-order"),
      "relationships" -> Json.fromValues(relationships),
      "schemaVersion" -> Json.fromString(schema),
      "scope" -> Json.obj(
        "componentIdentity" -> Json.fromString("component-order"),
        "projectionContextIdentity" -> Json.fromString(contextidentity),
        "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")
      ),
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.fromValues(links),
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
    ))
  }

  private def _source_snapshot(
    associations: Vector[FixtureAssociation] = Vector.empty,
    includerelatedrelationship: Boolean = false,
    includerolewitness: Boolean = false,
    additionalfacts: Vector[Json] = Vector.empty
  ): Array[Byte] = {
    val facts = Vector(
      _fact("element", "e-customer", "anchor-customer", "Customer is a party", Vector.empty),
      _fact("element", "e-usecase", "anchor-usecase", "Place an order", Vector.empty),
      _fact("relationship", "r-uses", "anchor-relationship", "Use case uses Customer", Vector("source limitation"))
    ) ++ associations.map(association =>
      _fact("relationship", "r-uses", association.factanchor, _association_content(association), Vector.empty)
    ) ++ (if includerolewitness then Vector(
      _fact("relationship", "r-uses", "anchor-role", "role:uses", Vector.empty)
    ) else Vector.empty) ++ (if includerelatedrelationship then Vector(
      _fact("relationship", "r-related", "anchor-related", "Related relationship", Vector.empty)
    ) else Vector.empty)
    val sortedfacts = (facts ++ additionalfacts).sortBy { fact =>
      val objectvalue = fact.asObject.get
      (
        objectvalue("componentIdentity").flatMap(_.asString).getOrElse(""),
        objectvalue("projectionContextIdentity").flatMap(_.asString).getOrElse(""),
        objectvalue("semanticIdentityKind").flatMap(_.asString).getOrElse(""),
        objectvalue("semanticIdentity").flatMap(_.asString).getOrElse(""),
        objectvalue("sourceAnchor").flatMap(_.asString).getOrElse("")
      )
    }
    _canonical(Json.obj(
      "basis" -> Json.obj(
        "contextIdentity" -> Json.fromString("model-context"),
        "facts" -> Json.fromValues(sortedfacts)
      ),
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source
    ))
  }

  private def _fact(kindvalue: String, identity: String, anchor: String, content: String, limitations: Vector[String]): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-order"),
      "content" -> Json.fromString(content),
      "limitations" -> Json.fromValues(limitations.map(Json.fromString)),
      "projectionContextIdentity" -> Json.fromString("context-order"),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceAnchor" -> Json.fromString(anchor)
    )

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String, sourcehash: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
      "source" -> _source.mapObject(_.add("sha256", Json.fromString(sourcehash))),
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue))
    )

  private def _targetless_reference(referenceid: String, anchor: String, sourcehash: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
      "source" -> _source.mapObject(_.add("sha256", Json.fromString(sourcehash))),
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.Null
    )

  private def _assertion(
    assertionid: String,
    kindvalue: String,
    identity: String,
    referenceid: String,
    content: String,
    conditionids: Vector[String],
    association: Option[Json] = None,
    includeassociation: Boolean = false
  ): Json = {
    val fields = Vector(
      "assertionId" -> Json.fromString(assertionid),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )
    val associationfields = if includeassociation then fields :+ ("association" -> association.getOrElse(Json.Null)) else fields
    Json.obj(associationfields*)
  }

  private def _association(association: FixtureAssociation): Json =
    Json.obj(
      "associationRole" -> Json.fromString(association.role),
      "relatedSemanticIdentity" -> Json.fromString(association.relatedidentity),
      "relatedSemanticIdentityKind" -> Json.fromString(association.relatedkind)
    )

  private def _association_content(association: FixtureAssociation): String =
    s"association:${association.role}:${association.relatedkind}:${association.relatedidentity}"

  private def _condition(conditionid: String, kindvalue: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(conditionid),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _element(identity: String, kindvalue: String, label: String, canonicalids: Vector[String], enrichmentids: Vector[String], conditionids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "kind" -> Json.fromString(kindvalue),
      "label" -> Json.fromString(label)
    )

  private def _relationship(identity: String, label: String, sourceidentity: String, targetidentity: String, rolevalue: String, directionvalue: String, canonicalids: Vector[String], enrichmentids: Vector[String], conditionids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "direction" -> Json.fromString(directionvalue),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentids.map(Json.fromString)),
      "identity" -> Json.fromString(identity),
      "label" -> Json.fromString(label),
      "role" -> Json.fromString(rolevalue),
      "sourceElementIdentity" -> Json.fromString(sourceidentity),
      "targetElementIdentity" -> Json.fromString(targetidentity)
    )

  private def _successor(priorkind: String, prioridentity: String, successorkind: String, successoridentity: String, referenceid: String): Json =
    Json.obj(
      "priorIdentity" -> Json.fromString(prioridentity),
      "priorKind" -> Json.fromString(priorkind),
      "sourceReferenceId" -> Json.fromString(referenceid),
      "successorIdentity" -> Json.fromString(successoridentity),
      "successorKind" -> Json.fromString(successorkind)
    )

  private def _with_fixture(
    realization: Array[Byte],
    dependencies: Vector[String] = Vector("snapshot-model"),
    unrelatedsnapshot: Option[Array[Byte]] = None,
    associations: Vector[FixtureAssociation] = Vector.empty,
    includerelatedrelationship: Boolean = false,
    includerolewitness: Boolean = false,
    additionalfacts: Vector[Json] = Vector.empty
  )(f: Path => Unit): Unit = {
    val snapshot = _source_snapshot(associations, includerelatedrelationship, includerolewitness, additionalfacts)
    val sourceartifact = _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", snapshot, Vector.empty)
    val realizationartifact = _artifact("realization-main", "realizations/main.json", "realization", realization, dependencies)
    val unrelatedartifact = unrelatedsnapshot.map(bytes => _artifact("snapshot-unrelated", "snapshots/unrelated.json", "source-snapshot", bytes, Vector.empty)).toVector
    val artifacts = if dependencies.isEmpty then Vector(realizationartifact, sourceartifact) ++ unrelatedartifact else Vector(sourceartifact, realizationartifact) ++ unrelatedartifact
    val root = Files.createTempDirectory("internal-model-semantic-realization-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: semantic-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      unrelatedsnapshot.foreach(bytes => _write(root.resolve("src/main/internal-model/snapshots/unrelated.json"), bytes))
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      f(root)
    } finally _delete_tree(root)
  }

  private def _artifact(id: String, path: String, rolevalue: String, bytes: Array[Byte], dependencies: Vector[String]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(true),
      "role" -> Json.fromString(rolevalue),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("semantic-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    ))
    val digest = _sha256(_canonical(root.remove("packageDigest").toJson))
    _canonical(root.add("packageDigest", Json.fromString(digest)).toJson)
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
