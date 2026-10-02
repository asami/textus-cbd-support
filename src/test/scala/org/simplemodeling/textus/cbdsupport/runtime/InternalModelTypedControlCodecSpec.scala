package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import scala.compiletime.testing.typeCheckErrors
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.Inspectors.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable specification for distinct typed references and strict JSON admission.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  2, 2026
 */
final class InternalModelTypedControlCodecSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  private val _codec = InternalModelTypedControlCodec
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _uuid = Gen.listOfN(32, Gen.oneOf("0123456789abcdef".toVector)).map { digits =>
    val value = digits.mkString
    s"${value.take(8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.drop(20)}"
  }
  private val _token = for {
    first <- Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))
    size <- Gen.choose(0, 40)
    suffix <- Gen.listOfN(size, Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Vector('.', '_', ':', '-')))
  } yield first.toString + suffix.mkString
  private val _logical_id = for {
    first <- Gen.oneOf("記録", "é", "🧭", "a", "引用\"", "改行\n識別")
    suffix <- Gen.listOf(Gen.oneOf(" ", "\t", "\n", "λ", "中", "🧩", "\\", "\"", ":", "a"))
  } yield "  " + first + suffix.mkString + "  "
  private val _revision = Gen.frequency(2 -> Gen.const(1L), 2 -> Gen.const(Long.MaxValue), 6 -> Gen.choose(1L, Long.MaxValue))
  private val _package_reference = for {
    packageid <- _uuid
    namespace <- _token
    projectid <- _token
  } yield _package(packageid, namespace, projectid)
  private val _artifact_reference = for {
    artifactid <- _token
    revision <- _revision
    role <- Gen.oneOf(InternalModelArtifactRole.values.toVector)
  } yield _artifact(artifactid, revision, role)
  private val _record_reference = for {
    recordid <- _logical_id
    revision <- _revision
  } yield _record(recordid, revision)

  private final case class ReferenceShape(
    name: String,
    bytes: Vector[Byte],
    expected: Any,
    decode: Vector[Byte] => Either[String, Any]
  )

  private val _package_value = _package("01234567-89ab-cdef-0123-456789abcdef", "org.example", "Project:one")
  private val _artifact_value = _artifact("source-one", 1L, InternalModelArtifactRole.SourceSnapshot)
  private val _record_value = _record("  記録🧭\nlogical id  ", 1L)
  private val _shapes = Vector(
    ReferenceShape("package", _codec.encodePackageReference(_package_value), _package_value, _codec.decodePackageReference),
    ReferenceShape("artifact", _codec.encodeArtifactReference(_artifact_value), _artifact_value, _codec.decodeArtifactReference),
    ReferenceShape("record", _codec.encodeRecordReference(_record_value), _record_value, _codec.decodeRecordReference)
  )

  "Typed control references" should {
    "complete reference roundtrip" which {
      "preserves independently generated package UUIDs and project tokens" in {
        forAll(_package_reference) { reference =>
          Given("a complete package reference with independently generated validated UUID and project tokens")
          val expected = reference
          When("its declared fields are encoded and decoded")
          val result = _codec.decodePackageReference(_codec.encodePackageReference(reference))
          Then("the same package reference is admitted by semantic value")
          result shouldBe Right(expected)
        }
      }

      "preserves independently generated artifact IDs, positive Long boundaries and closed roles" in {
        forAll(_artifact_reference) { reference =>
          Given("a complete artifact reference with an independent token, positive Long revision and declared role")
          val expected = reference
          When("its declared fields are encoded and decoded")
          val result = _codec.decodeArtifactReference(_codec.encodeArtifactReference(reference))
          Then("the same artifact reference is admitted by semantic value")
          result shouldBe Right(expected)
        }
      }

      "preserves nonblank Unicode logical IDs without normalization and positive Long boundaries" in {
        forAll(_record_reference) { reference =>
          Given("a complete record reference whose Unicode identity includes significant surrounding whitespace")
          val expected = reference
          When("its declared fields are encoded and decoded")
          val result = _codec.decodeRecordReference(_codec.encodeRecordReference(reference))
          Then("the same logical identity and supplied revision survive unchanged")
          result shouldBe Right(expected)
          result.map(_.recordId.value) shouldBe Right(reference.recordId.value)
        }
      }
    }

    "harmless JSON presentation" which {
      "admits every key permutation and varied insignificant whitespace for all three shapes" in {
        forAll(Gen.oneOf("", " ", "\n", "\t", "\r\n"), Gen.oneOf("", "\n", "  \n\t\r\n")) { (spacing, terminal) =>
          Given("each complete reference's fields in every key permutation with varied JSON whitespace")
          val inputs = _shapes.flatMap { shape =>
            _root(shape.bytes).toVector.permutations.map { fields =>
              val content = fields.map { case (key, value) =>
                Printer.noSpaces.print(Json.fromString(key)) + spacing + ":" + spacing + Printer.noSpaces.print(value)
              }.mkString(spacing + "{" + spacing, spacing + "," + spacing, spacing + "}" + terminal)
              shape -> _bytes(content)
            }.toVector
          }
          When("all harmless presentations are decoded")
          val results = inputs.map { case (shape, bytes) => shape.decode(bytes) -> shape.expected }
          Then("key order and whitespace preserve every typed value without byte equality admission")
          results.foreach { case (result, expected) => result shouldBe Right(expected) }
        }
      }

      "writes sorted keys and one LF solely as deterministic presentation" in {
        Given("the three complete typed reference values")
        val expected = Vector(
          "{\"packageId\":\"01234567-89ab-cdef-0123-456789abcdef\",\"projectId\":\"Project:one\",\"projectNamespace\":\"org.example\"}\n",
          "{\"artifactId\":\"source-one\",\"artifactRevision\":1,\"role\":\"source-snapshot\"}\n",
          "{\"recordId\":\"  記録🧭\\nlogical id  \",\"recordRevision\":1}\n"
        )
        When("the writers present their declared fields")
        val written = Vector(_codec.encodePackageReference(_package_value), _codec.encodeArtifactReference(_artifact_value), _codec.encodeRecordReference(_record_value))
        val withoutlf = _shapes.map(shape => shape.decode(shape.bytes.dropRight(1)))
        Then("the writer presentation is deterministic while admission also accepts no terminal LF")
        written.map(_text) shouldBe expected
        withoutlf shouldBe _shapes.map(shape => Right(shape.expected))
      }
    }

    "closed fields, primitive types and duplicate members" which {
      "rejects each missing field and every extra field including legacy control fields" in {
        Given("the exact three reference shapes and unsupported legacy or envelope fields")
        val extras = Vector("unknown", "sha256", "packageDigest", "packageRevision", "canonicalBytes", "path", "schemaVersion", "profile", "legacyReference", "revision", "id")
        val inputs = _shapes.flatMap { shape =>
          val root = _root(shape.bytes)
          root.keys.toVector.map(key => shape -> _json_bytes(root.remove(key).toJson)) ++
            extras.map(key => shape -> _json_bytes(root.add(key, Json.fromString("legacy")).toJson))
        }
        When("references with removed or additional members are decoded")
        val results = inputs.map { case (shape, bytes) => shape.decode(bytes) }
        Then("every missing or extra field rejects without a legacy reader or inferred default")
        all(results.map(_.isLeft)) shouldBe true
      }

      "rejects every wrong field type and non-object root" in {
        Given("all declared fields with each incompatible JSON primitive or container type")
        val candidates = Vector(Json.Null, Json.True, Json.False, Json.fromInt(1), Json.fromString("1"), Json.arr(), Json.obj())
        val inputs = _shapes.flatMap { shape =>
          val root = _root(shape.bytes)
          val fields = root.toVector.flatMap { case (key, value) =>
            candidates.filter(candidate => if value.isString then !candidate.isString else !candidate.isNumber)
              .map(candidate => shape -> _json_bytes(root.add(key, candidate).toJson))
          }
          fields ++ candidates.filterNot(_.isObject).map(value => shape -> _json_bytes(value))
        }
        When("type-invalid fields and roots are decoded")
        val results = inputs.map { case (shape, bytes) => shape.decode(bytes) }
        Then("each incompatible type rejects rather than being coerced")
        all(results.map(_.isLeft)) shouldBe true
      }

      "rejects a duplicate of every declared member including escaped equivalent keys" in {
        Given("each complete reference with one repeated field name")
        val inputs = _shapes.flatMap { shape =>
          val fields = _root(shape.bytes).toVector
          fields.flatMap { case (key, value) =>
            val members = fields.map { case (name, item) => Printer.noSpaces.print(Json.fromString(name)) + ":" + Printer.noSpaces.print(item) }
            val duplicate = Printer.noSpaces.print(Json.fromString(key)) + ":" + Printer.noSpaces.print(value)
            val escaped = "\"\\u" + f"${key.head.toInt}%04x" + key.tail + "\":" + Printer.noSpaces.print(value)
            Vector(duplicate, escaped).map(member => shape -> _bytes((members :+ member).mkString("{", ",", "}")))
          }
        }
        When("duplicate members are parsed")
        val results = inputs.map { case (shape, bytes) => shape.decode(bytes) }
        Then("both ordinary and escaped duplicates reject instead of choosing a member")
        all(results.map(_.isLeft)) shouldBe true
      }
    }

    "identity and role syntax" which {
      "rejects malformed UUIDs, project and artifact tokens, and blank logical record IDs" in {
        Given("invalid spellings for each validated identity domain")
        val uuid = Vector("", "01234567", "01234567-89AB-CDEF-0123-456789ABCDEF", "{01234567-89ab-cdef-0123-456789abcdef}", " 01234567-89ab-cdef-0123-456789abcdef", "g1234567-89ab-cdef-0123-456789abcdef")
        val tokens = Vector("", " ", "-a", "_a", ".a", ":a", "a b", "é", "a/b", "a\n")
        val blanks = Vector("", " ", "\t\r\n", "\u2003")
        val inputs = uuid.map(value => (_shapes(0), "packageId", value)) ++
          Vector("projectNamespace", "projectId").flatMap(key => tokens.map(value => (_shapes(0), key, value))) ++
          tokens.map(value => (_shapes(1), "artifactId", value)) ++ blanks.map(value => (_shapes(2), "recordId", value))
        When("each invalid identity replaces its declared field and is passed to its domain factory")
        val results = inputs.map { case (shape, key, value) => shape.decode(_json_bytes(_root(shape.bytes).add(key, Json.fromString(value)).toJson)) }
        val factories = uuid.map(InternalModelPackageId.from).map(_.isLeft) ++
          tokens.map(InternalModelProjectToken.from).map(_.isLeft) ++ tokens.map(InternalModelArtifactId.from).map(_.isLeft) ++
          blanks.map(InternalModelRecordId.from).map(_.isLeft)
        Then("all malformed identities reject at both the typed and serialized boundaries")
        all(results.map(_.isLeft)) shouldBe true
        all(factories) shouldBe true
      }

      "admits exactly the eight declared roles and rejects unknown spellings without aliases" in {
        Given("the closed role vocabulary and unsupported spelling variants")
        val expected = Vector("resume", "source-snapshot", "decision", "open-issue", "realization", "projection", "approval", "validation")
        val invalid = Vector("", "unknown", "Resume", "sourceSnapshot", "source_snapshot", "openIssue", "approval ", " manifest", "manifest") ++ expected.map(_.toUpperCase)
        When("each declared role roundtrips and every unknown spelling is decoded")
        val accepted = InternalModelArtifactRole.values.toVector.map(role => _codec.decodeArtifactReference(_codec.encodeArtifactReference(_artifact("artifact", 1L, role))))
        val rejected = invalid.map(value => _codec.decodeArtifactReference(_json_bytes(_root(_shapes(1).bytes).add("role", Json.fromString(value)).toJson)))
        val rolefactories = expected.map(InternalModelArtifactRole.fromWire)
        val invalidfactories = invalid.map(InternalModelArtifactRole.fromWire)
        Then("all eight roles retain exact wire meanings and every other spelling rejects")
        InternalModelArtifactRole.values.toVector.map(_.wireValue) shouldBe expected
        accepted.map(_.map(_.role.wireValue)) shouldBe expected.map(Right(_))
        rolefactories.map(_.map(_.wireValue)) shouldBe expected.map(Right(_))
        all(rejected.map(_.isLeft)) shouldBe true
        all(invalidfactories.map(_.isLeft)) shouldBe true
      }
    }

    "positive revision domains" which {
      "rejects nonpositive, overflowing and noncanonical integer forms for both revision fields" in {
        Given("artifact and record revision fields with forbidden JSON spellings")
        val spellings = Vector("0", "-1", "-0", "+1", "01", "00", "1.0", "1.5", "1e0", "1E+0", "9223372036854775808", "-9223372036854775809", "true", "false", "\"1\"", "null")
        val inputs = Vector(_shapes(1) -> "artifactRevision", _shapes(2) -> "recordRevision").flatMap { case (shape, key) =>
          spellings.map(value => shape -> _bytes(_text(shape.bytes).replace(s"\"$key\":1", s"\"$key\":$value")))
        }
        When("each spelling is decoded and nonpositive Longs reach the factories")
        val results = inputs.map { case (shape, bytes) => shape.decode(bytes) }
        val factories = Vector(Long.MinValue, -1L, 0L).flatMap(value => Vector(InternalModelArtifactRevision.from(value).isLeft, InternalModelRecordRevision.from(value).isLeft))
        Then("every invalid revision rejects without normalization or inference")
        all(results.map(_.isLeft)) shouldBe true
        all(factories) shouldBe true
      }

      "admits one and Long.MaxValue in each declared positive revision domain" in {
        Given("the lower and upper positive Long boundaries for both reference types")
        val artifacts = Vector(1L, Long.MaxValue).map(value => _artifact("artifact", value, InternalModelArtifactRole.Validation))
        val records = Vector(1L, Long.MaxValue).map(value => _record("record", value))
        When("the references and their revisions are admitted")
        val artifactresults = artifacts.map(value => _codec.decodeArtifactReference(_codec.encodeArtifactReference(value)))
        val recordresults = records.map(value => _codec.decodeRecordReference(_codec.encodeRecordReference(value)))
        Then("both boundaries retain their exact explicit values")
        artifactresults shouldBe artifacts.map(Right(_))
        recordresults shouldBe records.map(Right(_))
      }
    }

    "UTF-8 and strict JSON" which {
      "rejects invalid UTF-8, BOMs, YAML-only syntax, malformed JSON and trailing non-JSON input" in {
        Given("every reference shape with forbidden byte encodings and serialized syntax")
        val invalidtext = Vector("", "{", "[]", "null", "recordId: record\nrecordRevision: 1\n", "{'recordId':'record','recordRevision':1}", "{recordId:\"record\",recordRevision:1}", "{\"recordId\":\"record\",\"recordRevision\":1,}")
        val inputs = _shapes.flatMap { shape =>
          invalidtext.map(value => shape -> _bytes(value)) ++ Vector(
            shape -> Vector(0xc3.toByte, 0x28.toByte), shape -> Vector(0xff.toByte),
            shape -> (Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ shape.bytes),
            shape -> (_bytes(" \uFEFF") ++ shape.bytes),
            shape -> (shape.bytes ++ Vector(0.toByte)), shape -> (shape.bytes ++ _bytes("trailing")),
            shape -> (shape.bytes ++ _bytes("{}")), shape -> (shape.bytes ++ _bytes("// comment")),
            shape -> (shape.bytes ++ _bytes("\u00a0"))
          )
        }
        When("the strict readers interpret each invalid input")
        val results = inputs.map { case (shape, bytes) => shape.decode(bytes) }
        Then("every encoding or syntax violation rejects without YAML or trailing-input fallback")
        all(results.map(_.isLeft)) shouldBe true
      }
    }

    "compile-time domain distinctions" which {
      "prevents substitution of raw values, distinct identity and revision domains, and reference shapes" in {
        Given("separate opaque domains and immutable package, artifact and record reference types")
        val expectedviolations = 12
        When("source attempts to substitute raw values or another domain or reference")
        val errors = Vector(
          typeCheckErrors("""val value: InternalModelPackageId = "01234567-89ab-cdef-0123-456789abcdef""""),
          typeCheckErrors("""val value: InternalModelProjectToken = "project"""),
          typeCheckErrors("""val value: InternalModelArtifactId = "artifact"""),
          typeCheckErrors("""val value: InternalModelRecordId = "record"""),
          typeCheckErrors("""val value: InternalModelArtifactRevision = 1L"""),
          typeCheckErrors("""val value: InternalModelRecordRevision = 1L"""),
          typeCheckErrors("""val value: InternalModelArtifactId = InternalModelProjectToken.from("project").toOption.get"""),
          typeCheckErrors("""val value: InternalModelRecordId = InternalModelArtifactId.from("artifact").toOption.get"""),
          typeCheckErrors("""val value: InternalModelProjectToken = InternalModelPackageId.from("01234567-89ab-cdef-0123-456789abcdef").toOption.get"""),
          typeCheckErrors("""val value: InternalModelRecordRevision = InternalModelArtifactRevision.from(1L).toOption.get"""),
          typeCheckErrors("""def consume(value: InternalModelPackageReference): Unit = (); def substitute(value: InternalModelArtifactReference): Unit = consume(value)"""),
          typeCheckErrors("""def consume(value: InternalModelArtifactReference): Unit = (); def substitute(value: InternalModelRecordReference): Unit = consume(value)""")
        )
        Then("every attempted substitution has a compile-time type error")
        errors should have size expectedviolations
        all(errors.map(_.nonEmpty)) shouldBe true
      }
    }
  }

  private def _valid[A](result: Either[String, A]): A = result.fold(message => fail(message), value => value)

  private def _package(packageid: String, namespace: String, projectid: String): InternalModelPackageReference =
    InternalModelPackageReference(_valid(InternalModelPackageId.from(packageid)), _valid(InternalModelProjectToken.from(namespace)), _valid(InternalModelProjectToken.from(projectid)))

  private def _artifact(artifactid: String, revision: Long, role: InternalModelArtifactRole): InternalModelArtifactReference =
    InternalModelArtifactReference(_valid(InternalModelArtifactId.from(artifactid)), _valid(InternalModelArtifactRevision.from(revision)), role)

  private def _record(recordid: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(_valid(InternalModelRecordId.from(recordid)), _valid(InternalModelRecordRevision.from(revision)))

  private def _root(bytes: Vector[Byte]): JsonObject = _valid(_parser.parse(_text(bytes)).left.map(_.message)).asObject.get
  private def _json_bytes(json: Json): Vector[Byte] = _bytes(Printer.noSpaces.print(json))
  private def _bytes(content: String): Vector[Byte] = content.getBytes(StandardCharsets.UTF_8).toVector
  private def _text(bytes: Vector[Byte]): String = new String(bytes.toArray, StandardCharsets.UTF_8)
}
