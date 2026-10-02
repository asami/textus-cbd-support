package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import io.circe.{Json, JsonObject, Printer}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.Inspectors.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable specification for typed resume cursor admission.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
final class InternalModelResumeCursorCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private val _fixture = InternalModelContinuationFixture
  private val _reference = _fixture.reference("source-one", 1L, InternalModelArtifactRole.SourceSnapshot)
  private val _cursor = InternalModelResumeCursor("2.0", "ccdm-resume-v2", _fixture.packageId, 1L, Vector(_reference),
    "recorded-stage", None, Some("future-action"), Vector.empty,
    Vector(InternalModelResumeCondition("basis", "explanatory text", Some("source-one"))), Vector.empty, Vector.empty)
  private val _token = for {
    first <- Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))
    suffix <- Gen.listOf(Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Vector('.', '_', ':', '-')))
  } yield first.toString + suffix.mkString
  private val _uuid = Gen.listOfN(32, Gen.oneOf("0123456789abcdef".toVector)).map { digits =>
    val value = digits.mkString
    InternalModelPackageId.from(s"${value.take(8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.drop(20)}").toOption.get
  }
  private val _detail = Gen.listOf(Gen.choose(0, 0x10ffff).suchThat(value => value < 0xd800 || value > 0xdfff))
    .map(_.flatMap(value => Character.toChars(value).toVector).mkString)
  private val _condition_fields = Vector("blockers", "preconditions", "invalidationChecks", "acceptanceCriteria")

  "Typed resume cursor admission" should {
    "preserve declared fields" which {
      "C1 round-trip generated UUIDs, tokens, independent positive Long revisions and Unicode scalar details" in {
        forAll(_uuid, _token, Gen.choose(1L, Long.MaxValue), Gen.choose(1L, Long.MaxValue), _detail) {
          (identity, token, carrierrevision, artifactrevision, detail) =>
            Given("independently declared carrier and artifact revisions and arbitrary Unicode scalar detail")
            val reference = _reference.copy(artifactRevision = InternalModelArtifactRevision.from(artifactrevision).toOption.get)
            val cursor = _cursor.copy(packageId = identity, packageRevision = carrierrevision, selectedArtifacts = Vector(reference),
              currentStage = token, lastCompletedAction = Some(token), nextPermittedAction = Some(token),
              preconditions = Vector(InternalModelResumeCondition(token, detail, Some(reference.artifactId.value))))
            val bytes = InternalModelResumeCursorCodec.encode(cursor)
            When("the typed fields are decoded")
            val result = InternalModelResumeCursorCodec.decode(bytes)
            Then("every declared field survives without content-derived control state")
            result shouldBe Right(cursor)
            result.map(_.selectedArtifacts.head.artifactRevision.value) shouldBe Right(artifactrevision)
        }
      }

      "C1 admit both Long endpoints independently for carrier and artifact revisions" in {
        Given("every independent Long endpoint pair and blank, whitespace, quoted Unicode, supplementary and long details")
        val cursors = for {
          carrierrevision <- Vector(1L, Long.MaxValue)
          artifactrevision <- Vector(1L, Long.MaxValue)
          detail <- Vector("", "  ", "説明\nwith quoted \"text\"", "😀", "a" * 4096)
        } yield _cursor.copy(packageRevision = carrierrevision, selectedArtifacts = Vector(
          _reference.copy(artifactRevision = InternalModelArtifactRevision.from(artifactrevision).toOption.get)),
          preconditions = _cursor.preconditions.map(_.copy(detail = detail)))
        When("the endpoint pairs are encoded and decoded")
        val results = cursors.map(cursor => InternalModelResumeCursorCodec.decode(InternalModelResumeCursorCodec.encode(cursor)))
        Then("all four endpoint pairs preserve both revisions and every unrestricted detail spelling")
        results shouldBe cursors.map(Right(_))
      }

      "C2 retain omitted condition references and null actions in exactly twelve serialized fields" in {
        Given("null actions and a condition without an artifact reference")
        val cursor = _cursor.copy(nextPermittedAction = None, blockers = Vector(InternalModelResumeCondition("blocked", "", None)))
        When("the cursor is encoded and decoded")
        val bytes = InternalModelResumeCursorCodec.encode(cursor)
        val result = InternalModelResumeCursorCodec.decode(bytes)
        Then("the fields survive and no byte cache is serialized or retained in the cursor")
        result shouldBe Right(cursor)
        _fixture.json(bytes).asObject.get.size shouldBe 12
        _fixture.json(bytes).hcursor.downField("blockers").downArray.downField("artifactId").focus shouldBe None
        _fixture.json(bytes).hcursor.downField("nextPermittedAction").focus shouldBe Some(Json.Null)
        cursor.productElementNames.toVector should not contain "canonicalBytes"
      }

      "C3 retain changed detail without changing condition identity or artifact reference" in {
        forAll(_detail) { detail =>
          Given("a stable code and exact reference with independently varied explanatory text")
          val cursor = _cursor.copy(preconditions = Vector(InternalModelResumeCondition("basis", detail, Some("source-one"))))
          When("the changed detail is admitted")
          val result = InternalModelResumeCursorCodec.decode(InternalModelResumeCursorCodec.encode(cursor))
          Then("the text survives while the declared identity and selection stay exact")
          result.map(_.selectedArtifacts) shouldBe Right(Vector(_reference))
          result.map(_.preconditions) shouldBe Right(cursor.preconditions)
        }
      }

      "C12 admit syntactic stage/action tokens and empty arrays without establishing permission" in {
        Given("an unknown future stage/action spelling and no selected artifacts or recorded conditions")
        val cursor = _cursor.copy(selectedArtifacts = Vector.empty, currentStage = "future-stage",
          lastCompletedAction = Some("future-last"), nextPermittedAction = Some("future-next"),
          blockers = Vector.empty, preconditions = Vector.empty, invalidationChecks = Vector.empty, acceptanceCriteria = Vector.empty)
        When("only the serialized cursor is admitted")
        val result = InternalModelResumeCursorCodec.decode(InternalModelResumeCursorCodec.encode(cursor))
        Then("syntax preserves recorded fields without deriving an eligibility result")
        result shouldBe Right(cursor)
      }
    }

    "enforce closed syntax" which {
      "C4 reject every missing root field, extra field and wrong root type" in {
        Given("the exact twelve-field root")
        val root = _fixture.json(InternalModelResumeCursorCodec.encode(_cursor)).asObject.get
        val mutations = root.keys.toVector.flatMap(key => Vector(root.remove(key).toJson, root.add(key, Json.True).toJson)) ++
          Vector(root.add("unknown", Json.Null).toJson, Json.arr(), Json.Null, Json.fromString("root"))
        When("the closed root violations are decoded")
        val results = mutations.map(value => InternalModelResumeCursorCodec.decode(_fixture.canonical(value)))
        Then("every absent, extra or type-invalid root fails closed")
        all(results.map(_.isLeft)) shouldBe true
      }

      "C5 reject old formats, unsupported schema/profile, UUIDs, tokens and legacy hash controls" in {
        Given("unsupported versions, invalid UUID/token spellings and obsolete control fields")
        val fields = Vector(
          "schemaVersion" -> Json.fromString("1.0"), "schemaVersion" -> Json.fromString("3.0"),
          "profile" -> Json.fromString("ccdm-resume-v1"), "profile" -> Json.fromString("unknown"),
          "packageId" -> Json.fromString(_fixture.packageId.value.toUpperCase),
          "packageId" -> Json.fromString("01234567"), "packageId" -> Json.fromString("{" + _fixture.packageId.value + "}")
        ) ++ Vector("currentStage", "lastCompletedAction", "nextPermittedAction").flatMap(key =>
          Vector("", "a b", "é", "-a").map(value => key -> Json.fromString(value))) ++ Vector("currentStage" -> Json.Null)
        val root = _fixture.json(InternalModelResumeCursorCodec.encode(_cursor))
        val oldcontrols = Vector("packageDigest", "sha256", "canonicalBytes").map(key => root.mapObject(_.add(key, Json.fromString("obsolete"))))
        When("unsupported fields or spellings are decoded")
        val results = fields.map { case (key, value) => _decode_root(key, value) } ++
          oldcontrols.map(value => InternalModelResumeCursorCodec.decode(_fixture.canonical(value)))
        Then("no compatibility reader, fallback or control alias admits them")
        all(results.map(_.isLeft)) shouldBe true
      }

      "C6 reject every carrier and artifact revision lexical/range mutation without normalization" in {
        Given("independent positive lexical integers in the carrier and artifact reference")
        val content = new String(InternalModelResumeCursorCodec.encode(_cursor).toArray, StandardCharsets.UTF_8)
        val spellings = Vector("true", "false", "null", "\"1\"", "0", "-1", "+1", "01", "1.0", "1e0", "1E+0", "9223372036854775808")
        val mutations = Vector("packageRevision", "artifactRevision").flatMap(key =>
          spellings.map(value => content.replace(s""""$key":1""", s""""$key":$value""")))
        When("the actually changed numeric inputs are decoded")
        val results = mutations.map(value => InternalModelResumeCursorCodec.decode(_bytes(value)))
        Then("each lexical violation changes input and rejects instead of becoming a revision")
        all(mutations.map(_ != content)) shouldBe true
        all(results.map(_.isLeft)) shouldBe true
      }

      "C7 reject malformed encoding and Unicode while accepting equivalent JSON presentation" in {
        Given("valid fields with alternative layouts, scalar escapes and invalid encodings")
        val bytes = InternalModelResumeCursorCodec.encode(_cursor)
        val content = new String(bytes.toArray, StandardCharsets.UTF_8)
        val root = _fixture.json(bytes).asObject.get
        val valid = Vector(bytes.dropRight(1), bytes ++ _bytes(" \n\r\t"), _bytes(" " + content),
          _bytes(content.replace(":", ": ")), _bytes(Printer.noSpaces.print(Json.fromJsonObject(JsonObject.fromIterable(root.toVector.reverse)))),
          _bytes(content.replace("source-one", "source\\u002done")), _bytes(content.replace("explanatory text", "\\ud83d\\ude00")))
        val invalid = Vector(Vector(0xc3.toByte, 0x28.toByte), Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ bytes,
          bytes ++ _bytes("{}"), bytes ++ Vector(0.toByte), _bytes("profile: ccdm-resume-v2\n"),
          _bytes(content.replace("explanatory text", "\\ud800")), _bytes(content.replace("source-one", "source\\udfff")),
          _bytes(content.replace("\"role\"", "\"ro\\ud800le\"")), _bytes(content.replace("basis", "\\ud800\\u0041")))
        When("each representation is decoded")
        val accepted = valid.map(InternalModelResumeCursorCodec.decode)
        val rejected = invalid.map(InternalModelResumeCursorCodec.decode)
        Then("harmless presentation succeeds and invalid UTF-8/scalars, BOM, YAML or trailing JSON reject")
        all(accepted.map(_.isRight)) shouldBe true
        all(rejected.map(_.isLeft)) shouldBe true
        accepted.dropRight(1).foreach(result => result shouldBe Right(_cursor))
        accepted.last.map(_.preconditions.head.detail) shouldBe Right("😀")
      }

      "C8 reject duplicate root, reference and condition members" in {
        Given("duplicate members at every defined object depth")
        val content = new String(InternalModelResumeCursorCodec.encode(_cursor).toArray, StandardCharsets.UTF_8)
        val duplicates = Vector(content.replace("\"profile\":", "\"profile\":\"ccdm-resume-v2\",\"profile\":"),
          content.replace("\"role\":", "\"role\":\"source-snapshot\",\"role\":"),
          content.replace("\"detail\":", "\"detail\":\"duplicate\",\"detail\":"))
        When("the ambiguous JSON objects are decoded")
        val results = duplicates.map(value => InternalModelResumeCursorCodec.decode(_bytes(value)))
        Then("no first-member or last-member interpretation is selected")
        all(results.map(_.isLeft)) shouldBe true
      }

      "C9 reject every reference field/type, invalid identity/role and Resume selection" in {
        Given("the exact three-field typed artifact reference")
        val item = _fixture.referenceJson(_reference).asObject.get
        val invalid = item.keys.toVector.flatMap(key => Vector(item.remove(key).toJson, item.add(key, Json.Null).toJson,
          item.add(key, Json.True).toJson)) ++ Vector(item.add("path", Json.fromString("resume.yaml")).toJson,
          item.add("sha256", Json.fromString("obsolete")).toJson, item.add("artifactId", Json.fromString("bad id")).toJson,
          item.add("role", Json.fromString("resume")).toJson, item.add("role", Json.fromString("manifest")).toJson,
          item.add("role", Json.fromString("unknown")).toJson, Json.arr(), Json.fromString("reference"))
        When("the selected references are decoded")
        val results = invalid.map(value => _decode_root("selectedArtifacts", Json.arr(value)))
        Then("all malformed references and Resume selections reject")
        all(results.map(_.isLeft)) shouldBe true
      }

      "C10 reject duplicate or unordered selected IDs and codes in every condition array" in {
        Given("two declared selections and two condition codes")
        val other = _reference.copy(artifactId = InternalModelArtifactId.from("source-two").toOption.get)
        val first = InternalModelResumeCondition("a", "first", None)
        val second = InternalModelResumeCondition("b", "second", None)
        val invalid = Vector(_cursor.copy(selectedArtifacts = Vector(other, _reference)),
          _cursor.copy(selectedArtifacts = Vector(_reference, _reference.copy(artifactRevision = InternalModelArtifactRevision.from(2L).toOption.get)))) ++
          Vector(Vector(second, first), Vector(first, first)).flatMap(values => Vector(_cursor.copy(blockers = values),
            _cursor.copy(preconditions = values), _cursor.copy(invalidationChecks = values), _cursor.copy(acceptanceCriteria = values)))
        When("the unsorted or ambiguous arrays are decoded")
        val results = invalid.map(value => InternalModelResumeCursorCodec.decode(InternalModelResumeCursorCodec.encode(value)))
        Then("array order and unique ID/code resolution are required without normalization")
        all(results.map(_.isLeft)) shouldBe true
      }

      "C11 reject condition missing/extra/null/wrong-type fields and unresolved IDs in every array" in {
        Given("a closed condition with an exact selected artifact ID")
        val item = _fixture.json(InternalModelResumeCursorCodec.encode(_cursor)).hcursor.downField("preconditions").downArray.focus.get.asObject.get
        val invalid = Vector("code", "detail").flatMap(key => Vector(item.remove(key).toJson, item.add(key, Json.Null).toJson,
          item.add(key, Json.fromInt(1)).toJson)) ++ Vector(item.add("unknown", Json.Null).toJson,
          item.add("artifactId", Json.Null).toJson, item.add("artifactId", Json.True).toJson,
          item.add("artifactId", Json.fromString("not-selected")).toJson, item.add("artifactId", Json.fromString("bad id")).toJson,
          item.add("code", Json.fromString("")).toJson, item.add("code", Json.fromString("bad code")).toJson, Json.Null, Json.arr())
        When("each malformed condition is supplied in each defined condition list")
        val results = _condition_fields.flatMap(key => invalid.map(value => _decode_root(key, Json.arr(value))))
        Then("the closed condition grammar and exact selected-ID lookup reject every violation")
        all(results.map(_.isLeft)) shouldBe true
      }
    }
  }

  private def _decode_root(key: String, value: Json): Either[String, InternalModelResumeCursor] = {
    val root = _fixture.json(InternalModelResumeCursorCodec.encode(_cursor))
    InternalModelResumeCursorCodec.decode(_fixture.canonical(root.mapObject(_.add(key, value))))
  }

  private def _bytes(value: String): Vector[Byte] = value.getBytes(StandardCharsets.UTF_8).toVector
}
