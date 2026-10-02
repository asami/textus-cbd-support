package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}

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
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"),
    "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"),
    "revision" -> Json.fromString("revision-1")
  )
  private val _snapshot_reference = _artifact_reference("snapshot-model", 7L, "source-snapshot")

  "Internal-model semantic realization validation" should {
    "admit one exact versioned package/snapshot/realization basis and encode the same semantic record" in {
      Given("a project-private package with one model-context snapshot, one realization, both assertion lanes, and an explicit limitation")
      val realization = _realization()

      _with_fixture(realization) { root =>
        When("the selected realization is admitted through the one structural inventory pass")
        val result = InternalModelSemanticRealizationValidator.validate(root)

        Then("the immutable candidate retains its exact scope, explicit record version, dependency set and separate assertion lanes")
        result.isSuccess shouldBe true
        result.toOption.map { candidate =>
          (
            candidate.scope,
            candidate.canonicalAssertions.map(_.assertionId),
            candidate.enrichmentAssertions.map(_.assertionId),
            candidate.consumedSnapshotReferences,
            candidate.realizationReference
          )
        } shouldBe Some((
          InternalModelSemanticScope("component-order", "context-order", "e-usecase"),
          Vector("a-customer", "a-relationship", "a-usecase"),
          Vector("z-enrichment"),
          Vector(_typed_snapshot_reference()),
          InternalModelRecordReference(InternalModelRecordId.from("realization-order").toOption.get, InternalModelRecordRevision.from(11L).toOption.get)
        ))
        When("ordinary serialization is interpreted against the same captured source basis")
        val roundtrip = result.toOption.map(candidate => InternalModelSemanticRealizationValidator.validateVerified(
          _capture(InternalModelSemanticRealizationValidator.encode(candidate).toArray)
        ).toOption)
        Then("the decoded semantic record is unchanged")
        roundtrip shouldBe result.toOption.map(Some(_))
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
      Given("foreign-before-local facts sharing the selected target and anchor in a V3 realization")
      val foreigncontent = "Foreign Customer content"
      val foreign = _fact("element", "e-customer", "anchor-customer", foreigncontent, Vector.empty).mapObject(
        _.add("componentIdentity", Json.fromString("component-before")).add("projectionContextIdentity", Json.fromString("context-before"))
      )

        _with_fixture(_realization(), additionalfacts = Vector(foreign)) { root =>
          When("both assertion lanes retain the selected local witness content")
          val local = InternalModelSemanticRealizationValidator.validate(root)

          Then("the local content admits under the V3 profile")
          local.isSuccess shouldBe true
        }
        Given("the canonical lane borrows foreign content while its reference still names the local witness")
        _with_fixture(_realization(customercontent = foreigncontent), additionalfacts = Vector(foreign)) { root =>
          When("only the canonical assertion borrows the foreign content")
          val canonicalborrow = InternalModelSemanticRealizationValidator.validate(root)

          Then("canonical admission rejects the foreign-scope witness")
          canonicalborrow.isSuccess shouldBe false
        }
        Given("the enrichment lane borrows foreign content while its reference still names the local witness")
        _with_fixture(_realization(enrichmentcontent = foreigncontent), additionalfacts = Vector(foreign)) { root =>
          When("only the enrichment assertion borrows the foreign content")
          val enrichmentborrow = InternalModelSemanticRealizationValidator.validate(root)

          Then("enrichment admission rejects the foreign-scope witness")
          enrichmentborrow.isSuccess shouldBe false
        }
    }

    "reject both legacy profile/version pairs and every mismatched current pair" in {
      Given("explicit legacy or mismatched profile/version pairs")
      val cases = Vector(("ccdm-realization-v1", "1.0"), ("ccdm-realization-v2", "2.0"), ("ccdm-realization-v3", "2.0"), ("ccdm-realization-v2", "3.0"))
      forAll(Gen.oneOf(cases)) { case (profile, schema) =>
        _with_fixture(_realization(profilevalue = profile, schemaversion = schema)) { root =>
          When("the supplied profile/version pair is interpreted")
          val result = InternalModelSemanticRealizationValidator.validate(root)
          Then("admission rejects it without compatibility or inferred versions")
          result.isSuccess shouldBe false
        }
      }
    }

    "admit V3 associations with distinct exact source anchors for element and relationship targets" in {
      Given("two V3 realizations whose relationship association facts are separately source-backed on their asserting relationship")
      val elementassociation = FixtureAssociation(
        "a-association-owner-element", "owner", "e-customer", "element",
        "ref-association-owner-element", "anchor-association-owner-element", "anchor-association-owner-element"
      )
      val relationshipassociation = FixtureAssociation(
        "a-association-owner-relationship", "owner", "r-related", "relationship",
        "ref-association-owner-relationship", "anchor-association-owner-relationship", "anchor-association-owner-relationship"
      )
      val cases = Vector(
        (_realization(associations = Vector(elementassociation), includerolewitness = true), Vector(elementassociation), false),
        (_realization(associations = Vector(relationshipassociation), includerelatedrelationship = true, includerolewitness = true), Vector(relationshipassociation), true)
      )

      forAll(Gen.oneOf(cases)) { case (realization, associations, includerelatedrelationship) =>
        _with_fixture(realization, associations = associations, includerelatedrelationship = includerelatedrelationship, includerolewitness = true) { root =>
          When("the V3 source-backed association is admitted with its own reference and source-owned anchor")
          val result = InternalModelSemanticRealizationValidator.validate(root)

          Then("the candidate preserves the explicit V3 profile and exact typed association rather than inferring it from relationship endpoints")
          result.toOption.map(candidate => (candidate.profile, candidate.schemaVersion, candidate.canonicalAssertions.flatMap(_.association))) shouldBe
            Some(("ccdm-realization-v3", "3.0", associations.map(association => InternalModelSemanticAssociation(association.role, association.relatedidentity, association.relatedkind))))
        }
      }
    }

    "fail closed for structure, profile, package dependency, and source-basis mutations" which {
      "reject duplicate members, unsupported profile, missing dependency, changed source metadata, and a CML-style assertion path" in {
        Given("otherwise complete candidate values with independently bounded V3 mutations")
        val canonical = _realization()
        val duplicate = Array('{'.toByte) ++ "\"canonicalAssertions\":[],".getBytes(StandardCharsets.UTF_8) ++ canonical.drop(1)
        val cases = Vector(
          duplicate -> Vector(_snapshot_reference),
          _realization(profilevalue = "unsupported-profile") -> Vector(_snapshot_reference),
          _realization(sourcemetadata = _source.mapObject(_.add("identity", Json.fromString("contradictory-source")))) -> Vector(_snapshot_reference),
          _realization(customeranchor = "model/customer.cml") -> Vector(_snapshot_reference),
          canonical -> Vector.empty
        )

        forAll(Gen.oneOf(cases)) { case (realization, dependencies) =>
          _with_fixture(realization, dependencies) { root =>
            When("a structure, profile, source-envelope/anchor, or manifest dependency invariant is changed")
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

    "fail closed for malformed or non-unique V3 association claims" which {
      "reject unknown and cross-scope related identities, invalid role/kind combinations, and enrichment promotion" in {
        Given("V3 association claims that are not an in-scope typed canonical relationship assertion")
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
          (_realization(associations = Vector(unknown)), Vector(unknown), false),
          (_realization(associations = Vector(crossscope)), Vector(crossscope), false),
          (_realization(associations = Vector(invalidrole)), Vector(invalidrole), false),
          (_realization(associations = Vector(invalidkind)), Vector(invalidkind), false),
          (_realization(associations = Vector(unknownkind)), Vector(unknownkind), false),
          (_realization(enrichmentassociation = Some(enrichment)), Vector(enrichment), false)
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
        Given("V3 relationship associations that reuse one role or no longer match their exact selected source witness")
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
          (_realization(associations = Vector(owner, duplicateowner)), Vector(owner, duplicateowner)),
          (_realization(associations = Vector(owner, conflictingowner)), Vector(owner, conflictingowner)),
          (_realization(associations = Vector(wrongcontent)), Vector(wrongcontent)),
          (_realization(associations = Vector(wronganchor)), Vector(wronganchor))
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
      "accept generated V3 label changes but reject generated V3 association identity and anchor changes" in {
        Given("generated nonempty presentation labels, semantic ID suffixes, and source-anchor suffixes for a V3 source-backed association")
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
          _with_fixture(_realization(customerlabel = label, usecaselabel = label.reverse, associations = Vector(association)), associations = Vector(association)) { acceptedroot =>
            When("only explanatory labels vary under the same exact V3 source-backed association")
            val accepted = InternalModelSemanticRealizationValidator.validate(acceptedroot)

            Then("presentation variation does not replace the opaque associated identity")
            accepted.isSuccess shouldBe true
          }
          Given("the related semantic identity is changed while the retained scope is fixed")
          val changedassociation = association.copy(relatedidentity = "e-customer-" + identitysuffix)
          _with_fixture(_realization(associations = Vector(changedassociation)), associations = Vector(changedassociation)) { identityroot =>
            When("a generated associated identity no longer resolves in the retained realization scope")
            val rejected = InternalModelSemanticRealizationValidator.validate(identityroot)

            Then("the changed association identity is rejected rather than reconstructed from its label")
            rejected.isSuccess shouldBe false
          }
          Given("the source reference uses a generated anchor absent from the selected snapshot")
          val changedanchorassociation = association.copy(referenceanchor = "anchor-association-owner-" + anchorsuffix)
          _with_fixture(_realization(associations = Vector(changedanchorassociation)), associations = Vector(changedanchorassociation)) { anchorroot =>
            When("a generated association source anchor is not source-owned by the selected snapshot")
            val rejected = InternalModelSemanticRealizationValidator.validate(anchorroot)

            Then("the changed association anchor is rejected without inferred traceability")
            rejected.isSuccess shouldBe false
          }
        }
      }
    }
  }

  "V3 typed realization structure" should {
    "ordinary JSON presentation" which {
      "admit equivalent whitespace, reversed object keys and escaped identities" in {
        Given("the same complete V3 value presented with insignificant whitespace, reversed keys and equivalent JSON escapes")
        val original = _realization()
        val json = parse(new String(original, StandardCharsets.UTF_8)).toOption.get
        val reversed = json.mapObject(value => JsonObject.fromIterable(value.toVector.reverse))
        val presentations = Vector(json.spaces2, reversed.noSpaces, reversed.spaces2.replace("realization-order", "\\u0072ealization-order"))
        forAll(Gen.oneOf(presentations)) { presentation =>
          When("the presentation and ordinary writer output are independently decoded")
          val expected = InternalModelSemanticRealizationValidator.validateVerified(_capture(original)).toOption
          val actual = InternalModelSemanticRealizationValidator.validateVerified(_capture(presentation.getBytes(StandardCharsets.UTF_8))).toOption
          Then("their semantic values are equal without a byte admission condition")
          actual shouldBe expected
          actual.isDefined shouldBe true
        }
      }

      "reject malformed UTF-8, BOM, missing fields, extra fields and duplicate nested members" in {
        Given("a V3 realization with independently malformed or nonclosed serialized structure")
        val original = _realization()
        val text = new String(original, StandardCharsets.UTF_8)
        val cases = Vector(
          Array(0xc3.toByte, 0x28.toByte),
          Array(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ original,
          _mutate(original)(_.mapObject(_.remove("realizationReference"))),
          _mutate(original)(_.mapObject(_.add("unexpected", Json.Null))),
          text.replace("\"recordRevision\":11", "\"recordRevision\":11,\"recordRevision\":12").getBytes(StandardCharsets.UTF_8),
          _mutate(original)(_.mapObject(_.add("realizationReference", Json.obj("recordId" -> Json.fromString("realization-order"))))),
          _map_array(original, "canonicalAssertions")(_.mapObject(_.remove("association"))),
          _map_array(original, "sourceReferences")(_.mapObject(value => value.add("source", value("source").get.mapObject(_.add("unexpected", Json.Null))))),
          _map_array(original, "sourceReferences")(_.mapObject(value => value.add("snapshotReference", value("snapshotReference").get.mapObject(_.add("unexpected", Json.Null)))))
        )
        forAll(Gen.oneOf(cases)) { bytes =>
          When("the malformed candidate is interpreted against a valid capture")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("the closed V3 admission fails without a partial candidate")
          result.isSuccess shouldBe false
        }
      }
    }

    "explicit logical record revisions" which {
      "reject raw escaped lone surrogates in the original realization record ID" in {
        Vector("\\ud800", "\\udfff").foreach { escape =>
          Given("valid realization JSON with a unique record ID placeholder serialized before the raw escape is inserted")
          val marked = _mutate(_realization())(_.mapObject(root => root.add("realizationReference", root("realizationReference").get.mapObject(_.add("recordId", Json.fromString("unicode-realization-placeholder"))))))
          val bytes = new String(marked, StandardCharsets.UTF_8).replace("\"unicode-realization-placeholder\"", "\"realization-" + escape + "\"").getBytes(StandardCharsets.UTF_8)
          When("captured V3 realization admission examines the original escaped record ID")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("a lone high or low surrogate fails without a substituted logical identity or partial realization")
          result.isSuccess shouldBe false
          result.toOption shouldBe None
          result.show should include("realizationReference recordId must be nonblank valid Unicode")
        }
      }

      "preserve generated paired supplementary realization IDs and positive revisions exactly" in {
        forAll(Gen.choose(0x10000, 0x10ffff), Gen.choose(1L, Long.MaxValue)) { (codepoint, revision) =>
          Given("a generated supplementary scalar ID and independently declared positive realization revision")
          val scalar = new String(Character.toChars(codepoint))
          val escaped = Character.toChars(codepoint).map(character => f"\\u${character.toInt}%04x").mkString
          val marked = _mutate(_realization(recordrevision = revision))(_.mapObject(root => root.add("realizationReference", root("realizationReference").get.mapObject(_.add("recordId", Json.fromString("unicode-realization-pair"))))))
          val serialized = new String(marked, StandardCharsets.UTF_8)
          val bytes = serialized.replace("\"unicode-realization-pair\"", "\"realization-" + escaped + "\"").getBytes(StandardCharsets.UTF_8)
          val ordinary = serialized.replace("\"unicode-realization-pair\"", "\"realization-" + scalar + "\"").getBytes(StandardCharsets.UTF_8)
          When("paired JSON escaping and ordinary UTF-8 independently cross captured V3 admission")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          val expected = InternalModelSemanticRealizationValidator.validateVerified(_capture(ordinary))
          Then("the exact scalar, revision and complete evidence-bearing realization survive unchanged")
          result.isSuccess shouldBe true
          result.toOption shouldBe expected.toOption
          result.toOption.get.realizationReference.recordId.value shouldBe "realization-" + scalar
          result.toOption.get.realizationReference.recordRevision.value shouldBe revision
        }
      }

      "retain generated positive Long revisions including both boundaries" in {
        Given("producer-declared revisions including one, Long.MaxValue and generated positive Longs")
        val revisions = Gen.frequency(1 -> Gen.const(1L), 1 -> Gen.const(Long.MaxValue), 8 -> Gen.choose(1L, Long.MaxValue))
        forAll(revisions) { revision =>
          val bytes = _realization(recordrevision = revision)
          When("the exact declared record revision is decoded")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("the logical revision is retained independently of artifact and source revisions")
          result.toOption.map(_.realizationReference.recordRevision.value) shouldBe Some(revision)
        }
      }

      "reject nonpositive, fractional, exponent, signed, string and overflowing record revisions" in {
        Given("invalid lexical record revision forms and a blank record ID")
        val original = new String(_realization(), StandardCharsets.UTF_8)
        val lexical = Vector("0", "-1", "1.0", "1e0", "+1", "01", "\"1\"", "9223372036854775808", "null")
        val cases = lexical.map(value => original.replace("\"recordRevision\":11", "\"recordRevision\":" + value).getBytes(StandardCharsets.UTF_8)) :+
          original.replace("realization-order", "   ").getBytes(StandardCharsets.UTF_8)
        forAll(Gen.oneOf(cases)) { bytes =>
          When("the supplied logical record reference is decoded")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("admission rejects rather than allocating a version or repairing syntax")
          result.isSuccess shouldBe false
        }
      }
    }

    "exact source artifact selection" which {
      "reject wrong revisions, wrong roles and unknown references in source selection and traceability" in {
        Given("well-shaped references contradicting the exact captured source dependency")
        val references = Gen.frequency(
          4 -> Gen.choose(8L, Long.MaxValue).map(revision => _artifact_reference("snapshot-model", revision, "source-snapshot")),
          1 -> Gen.const(_artifact_reference("snapshot-model", 7L, "projection")),
          1 -> Gen.const(_artifact_reference("snapshot-unknown", 7L, "source-snapshot"))
        )
        forAll(references) { reference =>
          val sourcechanged = _map_array(_realization(), "sourceReferences")(_.mapObject(_.add("snapshotReference", reference)))
          val tracechanged = _mutate(_realization())(_.mapObject(_.add("traceability", Json.obj("consumedSnapshotReferences" -> Json.arr(reference)))))
          forAll(Gen.oneOf(sourcechanged, tracechanged)) { bytes =>
            When("the realization selects a source version or role absent from its admitted dependency set")
            val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
            Then("the exact reference mismatch rejects without selecting an existing ID or latest revision")
            result.isSuccess shouldBe false
          }
        }
      }

      "reject inconsistent manifest dependencies and absent optional source dependencies" in {
        Given("manifest dependencies with wrong revision, wrong role or unknown artifact ID")
        val cases = Vector(
          Vector(_artifact_reference("snapshot-model", 8L, "source-snapshot")),
          Vector(_artifact_reference("snapshot-model", 7L, "projection")),
          Vector(_artifact_reference("snapshot-unknown", 7L, "source-snapshot"))
        )
        forAll(Gen.oneOf(cases)) { dependencies =>
          _with_fixture(_realization(), dependencies = dependencies) { root =>
            When("the package capture resolves the realization dependencies")
            val result = InternalModelSemanticRealizationValidator.validate(root)
            Then("the contradictory dependency rejects before a candidate is returned")
            result.isSuccess shouldBe false
          }
        }
        Given("the selected dependency is inventoried as optional and absent")
        _with_fixture(_realization(), snapshotpresent = false) { root =>
          When("the package resolves a consumed optional dependency")
          val result = InternalModelSemanticRealizationValidator.validate(root)
          Then("absence cannot satisfy the exact selected source reference")
          result.isSuccess shouldBe false
        }
      }

      "reject duplicate selections, unordered traceability and malformed captured metadata" in {
        Given("a valid one-capture handoff and independently invalid capture or traceability metadata")
        val handoff = _capture(_realization())
        val source = handoff.sourcesnapshots.head
        val badreference = source.reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])
        val cases = Vector(
          null,
          handoff.copy(realization = null),
          handoff.copy(sourcesnapshots = null),
          handoff.copy(sourcesnapshots = Vector(null)),
          handoff.copy(sourcesnapshots = Vector(source, source)),
          handoff.copy(sourcesnapshots = Vector(source, source.copy(reference = source.reference.copy(artifactRevision = InternalModelArtifactRevision.from(8L).toOption.get)))),
          handoff.copy(sourcesnapshots = Vector(source.copy(reference = null))),
          handoff.copy(sourcesnapshots = Vector(source.copy(reference = badreference))),
          handoff.copy(sourcesnapshots = Vector(source.copy(dependencies = null))),
          handoff.copy(sourcesnapshots = Vector(source.copy(bytes = null))),
          handoff.copy(sourcesnapshots = Vector(source.copy(reference = source.reference.copy(role = InternalModelArtifactRole.Projection)))),
          handoff.copy(sourcesnapshots = Vector(source.copy(bytes = None))),
          handoff.copy(realization = handoff.realization.copy(dependencies = null)),
          handoff.copy(realization = handoff.realization.copy(reference = null)),
          handoff.copy(realization = handoff.realization.copy(bytes = null)),
          handoff.copy(realization = handoff.realization.copy(dependencies = Vector(source.reference, source.reference))),
          handoff.copy(realization = handoff.realization.copy(reference = handoff.realization.reference.copy(role = InternalModelArtifactRole.Projection)))
        )
        forAll(Gen.oneOf(cases)) { capture =>
          When("the already captured handoff is interpreted")
          val result = InternalModelSemanticRealizationValidator.validateVerified(capture)
          Then("malformed metadata rejects before any partial candidate can escape")
          result.isSuccess shouldBe false
        }
        Given("duplicate source-reference IDs or duplicate and unordered consumed artifact IDs")
        val duplicate = _mutate(_realization())(_.mapObject(value => value.add("sourceReferences", Json.fromValues(value("sourceReferences").get.asArray.get :+ value("sourceReferences").get.asArray.get.head))))
        val tracecases = Vector(
          Json.arr(_snapshot_reference, _snapshot_reference),
          Json.arr(_artifact_reference("snapshot-z", 7L, "source-snapshot"), _snapshot_reference)
        ).map(trace => _mutate(_realization())(_.mapObject(_.add("traceability", Json.obj("consumedSnapshotReferences" -> trace))))) :+ duplicate
        forAll(Gen.oneOf(tracecases)) { bytes =>
          When("the duplicate or unordered semantic selection is interpreted")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("selection rejects without collapsing a duplicate into a map")
          result.isSuccess shouldBe false
        }
      }
    }

    "source-owned unknown versions" which {
      "retain explicit unknown locator and revision and reject contradictory source attribution" in {
        Given("the selected source and every semantic reference explicitly report unknown source locator and revision")
        val unknown = _source.mapObject(_.add("locator", Json.Null).add("revision", Json.Null))
        _with_fixture(_realization(sourcemetadata = unknown), sourcemetadata = unknown) { root =>
          When("the recorded attributed semantic state is structurally admitted")
          val result = InternalModelSemanticRealizationValidator.validate(root)
          Then("unknown source versions remain unknown despite positive record and artifact revisions")
          result.isSuccess shouldBe true
          result.toOption.map(_.sourceReferences.map(reference => (reference.source.locator, reference.source.revision))) shouldBe Some(Vector.fill(3)((None, None)))
        }
        Given("a source reference supplies a revision or authority contradicting the selected unknown source")
        forAll(Gen.oneOf(_source, unknown.mapObject(_.add("authority", Json.fromString("other-authority"))))) { contradictory =>
          _with_fixture(_realization(sourcemetadata = contradictory), sourcemetadata = unknown) { root =>
            When("the claimed metadata is compared with admitted snapshot attribution")
            val result = InternalModelSemanticRealizationValidator.validate(root)
            Then("metadata contradiction rejects without filling the unknown source version")
            result.isSuccess shouldBe false
          }
        }
      }
    }
  }

  "V3 retained semantic evidence" should {
    "complete semantic links" which {
      "retain a source-attributed successor and every explicit condition class" in {
        Given("a source-backed successor between retained elements and conditions attributed to their affected relationship")
        val kinds = Vector("absence", "ambiguity", "conflict", "authorization-redaction", "availability-staleness", "malformed", "limitation")
        val original = _realization(successor = Some(_successor("element", "e-customer", "element", "e-usecase", "ref-customer")))
        val conditionids = kinds.indices.map(index => s"c-$index").toVector
        val conditions = kinds.zip(conditionids).map { case (kind, id) => _condition(id, kind, "relationship", "r-uses", "ref-relationship", "source limitation") }
        val withconditions = _mutate(original)(_.mapObject(_.add("conditions", Json.fromValues(conditions))))
        val withassertions = _map_array(withconditions, "canonicalAssertions") { value =>
          if value.hcursor.get[String]("assertionId").toOption.contains("a-relationship") then value.mapObject(_.add("conditionIds", Json.fromValues(conditionids.map(Json.fromString)))) else value
        }
        val bytes = _map_array(withassertions, "relationships")(_.mapObject(_.add("conditionIds", Json.fromValues(conditionids.map(Json.fromString)))))
        When("the complete condition and successor ledger is interpreted")
        val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
        Then("all conditions and the explicit replacement attribution remain visible")
        result.toOption.map(_.conditions.map(_.kind)) shouldBe Some(kinds)
        result.toOption.map(_.successorLinks.map(_.sourceReferenceId)) shouldBe Some(Vector("ref-customer"))
      }

      "reject missing structural links, condition mismatch, selected Use Case mismatch and duplicate identities" in {
        Given("complete source witnesses with independently inconsistent structural or condition links")
        val original = _realization()
        val cases = Vector(
          _map_array(original, "relationships")(_.mapObject(_.add("canonicalAssertionIds", Json.arr()))),
          _map_array(original, "elements")(_.mapObject(_.add("enrichmentAssertionIds", Json.arr()))),
          _map_array(original, "conditions")(_.mapObject(_.add("affectedIdentity", Json.fromString("e-usecase")))),
          _map_array(original, "conditions")(_.mapObject(_.add("sourceReferenceId", Json.fromString("ref-unknown")))),
          _mutate(original)(_.mapObject(value => value.add("scope", value("scope").get.mapObject(_.add("selectedUseCaseElementIdentity", Json.fromString("e-customer")))))),
          _mutate(original)(_.mapObject(value => value.add("elements", Json.fromValues(value("elements").get.asArray.get :+ value("elements").get.asArray.get.head))))
        )
        forAll(Gen.oneOf(cases)) { bytes =>
          When("the linked CCDM state is interpreted")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("inconsistent links or identities reject without a hidden semantic repair")
          result.isSuccess shouldBe false
        }
      }

      "validate consumed source V2 before extracting any semantic witnesses" in {
        Given("consumed snapshots with malformed fields, duplicate members, unsupported source schema or an unsupported assertion basis")
        val original = _source_snapshot()
        val cases = Vector(
          _mutate(original)(_.mapObject(_.add("schemaVersion", Json.fromString("1.0")))),
          _mutate(original)(_.mapObject(_.add("unexpected", Json.Null))),
          ("{\"source\":{}," + new String(original.drop(1), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
          _serialize(Json.obj("basis" -> Json.obj("entries" -> Json.arr(Json.obj(
            "definition" -> Json.fromString("Customer is a party"), "limitations" -> Json.arr(),
            "sourceAnchor" -> Json.fromString("anchor-customer"), "termIdentity" -> Json.fromString("e-customer"), "termLabel" -> Json.fromString("Customer")
          ))), "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("glossary-bok"), "source" -> _source)),
          _serialize(Json.obj(
            "basis" -> Json.obj("byteLength" -> Json.fromLong(0L), "projectRelativePath" -> Json.fromString("model/customer.cml"), "rawBytesBase64" -> Json.fromString("")),
            "schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"), "source" -> _source
          ))
        )
        forAll(Gen.oneOf(cases)) { snapshot =>
          _with_fixture(_realization(), snapshotoverride = Some(snapshot)) { root =>
            When("the selected source artifact is validated before assertion extraction")
            val result = InternalModelSemanticRealizationValidator.validate(root)
            Then("invalid V2 evidence, a glossary definition or a CML path cannot supply a CCDM witness")
            result.isSuccess shouldBe false
          }
        }
      }
    }

    "closed source-backed association roles" which {
      "retain every allowed role with an exact distinct source anchor" in {
        Given("source-owned relationship claims for all admitted association roles")
        val roles = Vector("owner", "subject", "affected-relationship", "affected-subject", "affected-endpoint")
        forAll(Gen.oneOf(roles)) { role =>
          val kind = if role == "affected-relationship" then "relationship" else "element"
          val identity = if kind == "relationship" then "r-uses" else "e-customer"
          val association = FixtureAssociation("a-association", role, identity, kind, "ref-association", "anchor-association", "anchor-association")
          _with_fixture(_realization(associations = Vector(association)), associations = Vector(association)) { root =>
            When("the canonical relationship assertion carries its exact association witness")
            val result = InternalModelSemanticRealizationValidator.validate(root)
            Then("the declared role and related kind/identity are retained without endpoint inference")
            result.toOption.map(_.canonicalAssertions.flatMap(_.association)) shouldBe Some(Vector(InternalModelSemanticAssociation(role, identity, kind)))
          }
        }
      }

      "reject association claims on elements and malformed association objects" in {
        Given("ordinary element assertions with a relationship-only association or nonclosed association detail")
        val association = Json.obj("associationRole" -> Json.fromString("owner"), "relatedSemanticIdentity" -> Json.fromString("e-customer"), "relatedSemanticIdentityKind" -> Json.fromString("element"))
        val details = Vector(association, association.mapObject(_.remove("associationRole")), association.mapObject(_.add("unexpected", Json.Null)))
        forAll(Gen.oneOf(details)) { detail =>
          val bytes = _map_array(_realization(), "canonicalAssertions") { value =>
            if value.hcursor.get[String]("assertionId").toOption.contains("a-customer") then value.mapObject(_.add("association", detail)) else value
          }
          When("the element association claim is interpreted")
          val result = InternalModelSemanticRealizationValidator.validateVerified(_capture(bytes))
          Then("a nonrelationship assertion or malformed typed detail rejects")
          result.isSuccess shouldBe false
        }
      }
    }
  }

  private def _realization(
    profilevalue: String = "ccdm-realization-v3",
    schemaversion: String = "3.0",
    contextidentity: String = "context-order",
    customeridentity: String = "e-customer",
    customeranchor: String = "anchor-customer",
    relationshiptarget: String = "e-customer",
    rolevalue: String = "uses",
    directionvalue: String = "source-to-target",
    customerreferencekind: String = "element",
    enrichmentid: String = "z-enrichment",
    includecondition: Boolean = true,
    sourcemetadata: Json = _source,
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
    recordrevision: Long = 11L
  ): Array[Byte] = {
    val associationids = associations.map(_.assertionid).sorted
    val initialreferences = Vector(
      _reference("ref-customer", customerreferencekind, customeridentity, customeranchor, sourcemetadata),
      _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship", sourcemetadata),
      _reference("ref-usecase", "element", "e-usecase", "anchor-usecase", sourcemetadata)
    )
    val associationreferences = associations.map(association =>
      _reference(association.referenceid, "relationship", "r-uses", association.referenceanchor, sourcemetadata)
    )
    val rolewitnessreferences = if includerolewitness then Vector(_reference("ref-role", "relationship", "r-uses", "anchor-role", sourcemetadata)) else Vector.empty
    val relatedreferences = if includerelatedrelationship then Vector(_reference("ref-related", "relationship", "r-related", "anchor-related", sourcemetadata)) else Vector.empty
    val successorreferences = if successortargetless then Vector(_targetless_reference("ref-successor", "anchor-customer", sourcemetadata)) else Vector.empty
    val references = (initialreferences ++ associationreferences ++ rolewitnessreferences ++ relatedreferences ++ successorreferences).sortBy(_.hcursor.get[String]("referenceId").toOption.get)
    val relationshipconditions = if includecondition then Vector("c-limitation") else Vector.empty
    val conditions = if includecondition then Vector(_condition("c-limitation", "limitation", "relationship", "r-uses", "ref-relationship", "source limitation")) else Vector.empty
    val canonical = Vector(
      _assertion("a-customer", "element", customeridentity, "ref-customer", customercontent, Vector.empty),
      _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", relationshipconditions),
      _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty)
    ) ++ associations.map(association =>
      _assertion(
        association.assertionid,
        "relationship",
        "r-uses",
        association.referenceid,
        association.assertioncontent.getOrElse(_association_content(association)),
        Vector.empty,
        Some(_association(association))
      )
    ) ++ (if includerolewitness then Vector(
      _assertion("a-role", "relationship", "r-uses", "ref-role", "role:uses", Vector.empty)
    ) else Vector.empty) ++ (if includerelatedrelationship then Vector(
      _assertion("a-related", "relationship", "r-related", "ref-related", "Related relationship", Vector.empty)
    ) else Vector.empty)
    val enrichment = Vector(_assertion(enrichmentid, "element", customeridentity, "ref-customer", enrichmentcontent, Vector.empty, enrichmentassociation.map(_association)))
    val elements = Vector(
      _element(customeridentity, "customer", customerlabel, Vector("a-customer"), Vector(enrichmentid), Vector.empty),
      _element("e-usecase", "use-case", usecaselabel, Vector("a-usecase"), Vector.empty, Vector.empty)
    ).sortBy(_.hcursor.get[String]("identity").toOption.get)
    val relationships = (Vector(
      _relationship("r-uses", "uses", "e-usecase", relationshiptarget, rolevalue, directionvalue, (Vector("a-relationship") ++ associationids ++ (if includerolewitness then Vector("a-role") else Vector.empty)).sorted, Vector.empty, relationshipconditions)
    ) ++ (if includerelatedrelationship then Vector(
      _relationship("r-related", "related", "e-usecase", customeridentity, "related", "source-to-target", Vector("a-related"), Vector.empty, Vector.empty)
    ) else Vector.empty)).sortBy(_.hcursor.get[String]("identity").getOrElse(""))
    val links = successor.toVector
    _serialize(Json.obj(
      "canonicalAssertions" -> Json.fromValues(canonical.sortBy(_.hcursor.get[String]("assertionId").toOption.get)),
      "conditions" -> Json.fromValues(conditions),
      "elements" -> Json.fromValues(elements),
      "enrichmentAssertions" -> Json.fromValues(enrichment),
      "profile" -> Json.fromString(profilevalue),
      "realizationReference" -> Json.obj("recordId" -> Json.fromString("realization-order"), "recordRevision" -> Json.fromLong(recordrevision)),
      "relationships" -> Json.fromValues(relationships),
      "schemaVersion" -> Json.fromString(schemaversion),
      "scope" -> Json.obj(
        "componentIdentity" -> Json.fromString("component-order"),
        "projectionContextIdentity" -> Json.fromString(contextidentity),
        "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase")
      ),
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.fromValues(links),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_snapshot_reference))
    ))
  }

  private def _source_snapshot(
    associations: Vector[FixtureAssociation] = Vector.empty,
    includerelatedrelationship: Boolean = false,
    includerolewitness: Boolean = false,
    additionalfacts: Vector[Json] = Vector.empty,
    sourcemetadata: Json = _source
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
    _serialize(Json.obj(
      "basis" -> Json.obj(
        "contextIdentity" -> Json.fromString("model-context"),
        "facts" -> Json.fromValues(sortedfacts)
      ),
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> sourcemetadata
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

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String, sourcemetadata: Json): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotReference" -> _snapshot_reference,
      "source" -> sourcemetadata,
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue))
    )

  private def _targetless_reference(referenceid: String, anchor: String, sourcemetadata: Json): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotReference" -> _snapshot_reference,
      "source" -> sourcemetadata,
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
    association: Option[Json] = None
  ): Json =
    Json.obj(
      "association" -> association.getOrElse(Json.Null),
      "assertionId" -> Json.fromString(assertionid),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

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
    dependencies: Vector[Json] = Vector(_snapshot_reference),
    unrelatedsnapshot: Option[Array[Byte]] = None,
    associations: Vector[FixtureAssociation] = Vector.empty,
    includerelatedrelationship: Boolean = false,
    includerolewitness: Boolean = false,
    additionalfacts: Vector[Json] = Vector.empty,
    sourcemetadata: Json = _source,
    snapshotoverride: Option[Array[Byte]] = None,
    snapshotpresent: Boolean = true
  )(f: Path => Unit): Unit = {
    val snapshot = snapshotoverride.getOrElse(_source_snapshot(associations, includerelatedrelationship, includerolewitness, additionalfacts, sourcemetadata))
    val sourceartifact = _artifact("snapshot-model", 7L, "snapshots/model.json", "source-snapshot", Vector.empty, snapshotpresent)
    val realizationartifact = _artifact("realization-main", 13L, "realizations/main.json", "realization", dependencies)
    val unrelatedartifact = unrelatedsnapshot.map(_ => _artifact("snapshot-unrelated", 17L, "snapshots/unrelated.json", "source-snapshot", Vector.empty)).toVector
    val artifacts = if dependencies.isEmpty then Vector(realizationartifact, sourceartifact) ++ unrelatedartifact else Vector(sourceartifact, realizationartifact) ++ unrelatedartifact
    val work = Path.of("target/internal-model-semantic-realization/work")
    Files.createDirectories(work)
    val root = Files.createTempDirectory(work, "fixture-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: semantic-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(artifacts))
      if snapshotpresent then _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      unrelatedsnapshot.foreach(bytes => _write(root.resolve("src/main/internal-model/snapshots/unrelated.json"), bytes))
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      f(root)
    } finally _delete_tree(root)
  }

  private def _artifact(id: String, revision: Long, path: String, rolevalue: String, dependencies: Vector[Json], required: Boolean = true): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "artifactRevision" -> Json.fromLong(revision),
      "dependsOn" -> Json.fromValues(dependencies),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(required),
      "role" -> Json.fromString(rolevalue)
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(artifacts),
      "lifecycleState" -> Json.fromString("draft"),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("semantic-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("2.0")
    ))
    _serialize(root.toJson)
  }

  private def _serialize(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _artifact_reference(id: String, revision: Long, rolevalue: String): Json =
    Json.obj("artifactId" -> Json.fromString(id), "artifactRevision" -> Json.fromLong(revision), "role" -> Json.fromString(rolevalue))

  private def _typed_snapshot_reference(): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from("snapshot-model").toOption.get, InternalModelArtifactRevision.from(7L).toOption.get, InternalModelArtifactRole.SourceSnapshot)

  private def _capture(realization: Array[Byte]): InternalModelVerifiedRealizationPackage = {
    val source = _typed_snapshot_reference()
    val reference = InternalModelArtifactReference(InternalModelArtifactId.from("realization-main").toOption.get, InternalModelArtifactRevision.from(13L).toOption.get, InternalModelArtifactRole.Realization)
    InternalModelVerifiedRealizationPackage(
      InternalModelVerifiedRealization(reference, "realizations/main.json", true, Vector(source), realization.toVector),
      Vector(InternalModelVerifiedSourceSnapshot(source, "snapshots/model.json", true, Vector.empty, Some(_source_snapshot().toVector)))
    )
  }

  private def _mutate(bytes: Array[Byte])(f: Json => Json): Array[Byte] =
    _serialize(f(parse(new String(bytes, StandardCharsets.UTF_8)).toOption.get))

  private def _map_array(bytes: Array[Byte], key: String)(f: Json => Json): Array[Byte] =
    _mutate(bytes)(_.mapObject(value => value.add(key, Json.fromValues(value(key).get.asArray.get.map(f)))))

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally stream.close()
    }
}
