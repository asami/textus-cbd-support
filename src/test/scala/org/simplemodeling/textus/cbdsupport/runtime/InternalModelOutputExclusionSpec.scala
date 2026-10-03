package org.simplemodeling.textus.cbdsupport.runtime

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import io.circe.Json
import io.circe.parser.parse
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Consuming-project proof over actual default output owners; authored, unvalidated.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
final class InternalModelOutputExclusionSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelOutputExclusionFixture.*
  private lazy val _evidence = load()
  private val _public_types = Set("PublicOutputApi", "PublicRequest", "PublicResponse")
  private val _private_marker = "P106_PRIVATE_MODEL_SENTINEL"
  private val _public_marker = "P106_PUBLIC_OUTPUT_CONTROL"
  private val _resource_marker = "P106_PUBLIC_RESOURCE_CONTROL"

  "Default consuming-project output exclusion" should {
    "actual generation and packaging owners" which {
      "X1 retain public generation with the private manifest and all four payloads present" in {
        Given("the normal Test prerequisite's exact fixture inputs and actual generation locators")
        val evidence = _evidence
        val source = evidence.projectroot.resolve("src/main")
        val privatepaths = Vector("public-control.cml", "InternalPrivateApi.scala",
          "private-description.md", "private-evidence.json")
        When("public inputs, the strict private manifest, its payloads and complete generated trees are interpreted")
        val publiccml = text(source.resolve("cozy/public-control.cml"))
        val publicresource = text(source.resolve("resources/public-control.txt"))
        val manifest = _json(source.resolve("internal-model/manifest.yaml"))
        val payloads = privatepaths.map(path => text(source.resolve("internal-model").resolve(path)))
        val generated = scan(evidence.generatedroot)
        val metadata = scan(evidence.metadataroot)
        Then("real public API, operation and values are generated while present private inputs stay excluded")
        publiccml should include(_public_marker)
        publicresource should include(_resource_marker)
        payloads.foreach(_ should include(_private_marker))
        _string(manifest, "schemaVersion") shouldBe "2.0"
        _string(manifest, "packageId") shouldBe "8ab18442-5b2b-4c76-a5d6-d906020ac2de"
        _string(manifest, "projectNamespace") shouldBe "org.simplemodeling.textus.fixture"
        _string(manifest, "projectId") shouldBe "InternalModelOutputControl"
        _string(manifest, "lifecycleState") shouldBe "draft"
        manifest.hcursor.get[Int]("revision").toOption shouldBe Some(1)
        val artifacts = manifest.hcursor.get[Vector[Json]]("artifacts").toOption.get
        artifacts.map(_string(_, "artifactId")) shouldBe Vector("a-cml", "b-scala", "c-description", "d-evidence")
        artifacts.map(_string(_, "path")) shouldBe privatepaths
        artifacts.foreach { artifact =>
          _string(artifact, "role") shouldBe "source-snapshot"
          artifact.hcursor.get[Int]("artifactRevision").toOption shouldBe Some(1)
          artifact.hcursor.get[Boolean]("required").toOption shouldBe Some(true)
          artifact.hcursor.get[Vector[Json]]("dependsOn").toOption shouldBe Some(Vector.empty)
        }
        generated.entries.exists(_.path.endsWith(".scala")) shouldBe true
        _public_types.foreach(token => generated.containsToken(token) shouldBe true)
        generated.containsToken("echo") shouldBe true
        generated.containsToken("value") shouldBe true
        metadata.entries.exists(_.path.endsWith("model-metadata.json")) shouldBe true
        _public_types.foreach(token => metadata.containsToken(token) shouldBe true)
        metadata.containsToken("echo") shouldBe true
        metadata.containsToken("value") shouldBe true
        generated.leaks shouldBe empty
        metadata.leaks shouldBe empty
      }

      "X2 package real public resources and generated classes without private names paths or content" in {
        Given("the actual normal Compile packageBin JAR locator")
        val jar = _evidence.mainjar
        When("every JAR member name and complete uncompressed content is scanned")
        val result = scan(jar)
        Then("the ordinary resource content and generated public classes are positive controls and private data is absent")
        result.entries.exists(entry => entry.path.endsWith("!/public-control.txt") &&
          entry.tokens.contains(_resource_marker)) shouldBe true
        _public_types.foreach { token =>
          result.entries.exists(_.path.endsWith(s"/$token.class")) shouldBe true
        }
        result.leaks shouldBe empty
      }

      "X3 package the descriptor's nonempty provided public types in the actual API JAR" in {
        Given("the generated API descriptor and normal cozyComponentApiJar result")
        val evidence = _evidence
        When("the real descriptor and every actual API JAR entry are interpreted")
        val descriptor = _json(evidence.metadataroot.resolve("component-api-descriptor.json"))
        val result = scan(evidence.apijar)
        val provided = descriptor.hcursor.get[Vector[Json]]("provided").toOption.get
        val types = provided.flatMap(_.hcursor.get[Vector[Json]]("publicTypes").toOption.get)
        Then("provided publicTypes name the API and both records with their real class companion and TASTy artifacts")
        _string(descriptor, "schemaVersion") shouldBe "cncf.component-api.v2"
        provided should not be empty
        types should not be empty
        types.map(_string(_, "className").split('.').last).toSet should contain allElementsOf _public_types
        _public_types.foreach { token =>
          result.entries.exists(_.path.endsWith(s"/$token.class")) shouldBe true
          result.entries.exists(_.path.endsWith(s"/$token.tasty")) shouldBe true
        }
        Vector("PublicRequest", "PublicResponse").foreach { token =>
          result.entries.exists(_.path.endsWith(s"/$token$$.class")) shouldBe true
        }
        result.leaks shouldBe empty
      }

      "X4 retain actual Scaladoc symbols and local publication metadata and pages in the disabled source-manifest profile" in {
        Given("actual Compile doc staging and cozyPublishProject local output locators")
        val evidence = _evidence
        When("complete documentation and publication trees and the actual final bundle's metadata entries are interpreted")
        val documentation = scan(evidence.scaladocroot)
        val publication = scan(evidence.publicationroot)
        val bundle = _json(evidence.publicationroot.resolve("fixture-internal-model-output-control.json"))
        val entries = bundle.hcursor.get[Vector[Json]]("entries").fold(error => fail(error.message), identity)
        val projectentries = entries.filter(_string(_, "key") == "projects/fixture-internal-model-output-control/metadata")
        val pageentries = entries.filter(_string(_, "key") == "publication-pages/fixture-internal-model-output-control")
        val project = projectentries.headOption.flatMap(_.hcursor.get[Json]("metadata").toOption)
          .getOrElse(fail("Actual project metadata entry is required"))
        val pages = pageentries.headOption.flatMap(_.hcursor.get[Json]("metadata").toOption)
          .getOrElse(fail("Actual publication-pages metadata entry is required"))
        val pagevalues = pages.hcursor.get[Vector[Json]]("pages").fold(error => fail(error.message), identity)
        Then("index search and public symbols plus real public page metadata remain while private material and source manifests are absent")
        documentation.entries.exists(_.path.endsWith("index.html")) shouldBe true
        documentation.entries.exists(entry => Vector("search", "inkuire", "symbol")
          .exists(entry.path.toLowerCase(java.util.Locale.ROOT).contains)) shouldBe true
        documentation.containsToken("PublicOutputApi") shouldBe true
        _string(bundle, "schema") shouldBe "cozy.publish-project.v1"
        _string(bundle, "type") shouldBe "publication-bundle"
        bundle.hcursor.downField("publication").get[String]("name").toOption shouldBe
          Some("fixture-internal-model-output-control")
        entries should not be empty
        projectentries.size shouldBe 1
        pageentries.size shouldBe 1
        _string(projectentries.head, "path") shouldBe "metadata/projects/fixture-internal-model-output-control/metadata.json"
        _string(pageentries.head, "path") shouldBe "metadata/publication-pages/fixture-internal-model-output-control.json"
        _string(project, "schema") shouldBe "cozy.publish-project.v1"
        _string(project, "type") shouldBe "project-metadata"
        project.noSpaces should include(_public_marker)
        _string(pages, "schema") shouldBe "cozy.publish-project.v1"
        _string(pages, "type") shouldBe "publication-pages"
        pagevalues should not be empty
        pagevalues.map(_string(_, "path")) should contain("public-control")
        pages.noSpaces should include(_public_marker)
        entries.foreach { entry =>
          _string(entry, "path") should not include "source-manifest"
          entry.hcursor.downField("metadata").get[String]("type")
            .fold(error => fail(error.message), identity) should not include "source-manifest"
        }
        project.hcursor.downField("publication").downField("sourceManifest").succeeded shouldBe false
        publication.entries.exists(_.path.contains("source-manifest")) shouldBe false
        documentation.leaks shouldBe empty
        publication.leaks shouldBe empty
      }

      "X5 retain actual CAR runtime and API partitions and actual SAR public CML without private material" in {
        Given("the actual cozyBuildCar and cozyBuildSar task-returned archives")
        val evidence = _evidence
        When("every CAR and SAR member name and payload including every nested archive is scanned")
        val car = scan(evidence.car)
        val sar = scan(evidence.sar)
        Then("the CAR descriptors runtime resource and SPI public classes and SAR public CML are non-vacuous controls")
        Vector("component-descriptor.json", "abi-manifest.json", "component-api-descriptor.json")
          .foreach(name => car.entries.exists(_.path.endsWith(s"!/$name")) shouldBe true)
        car.entries.exists(entry => entry.path.contains("!/component/main.jar!/") &&
          entry.tokens.contains(_resource_marker)) shouldBe true
        _public_types.foreach { token =>
          car.entries.exists(entry => entry.path.contains("!/spi/") &&
            entry.path.endsWith(s"/$token.class")) shouldBe true
        }
        sar.entries.exists(entry => entry.path.endsWith("!/public-control.cml") &&
          entry.tokens.contains(_public_marker) && entry.tokens.contains("echo") &&
          entry.tokens.contains("PublicRequest") && entry.tokens.contains("PublicResponse")) shouldBe true
        car.leaks shouldBe empty
        sar.leaks shouldBe empty
      }
    }

    "the secondary bounded detector" which {
      "X6 report exact nested marker and name paths across varied archive depths and content offsets" in {
        forAll(Gen.choose(0, 5), Gen.choose(0, 16384)) { (depth, offset) =>
          Given("small valid nested ZIPs with independently varied nesting and sentinel offset")
          val suffix = (0 until depth).reverse.map(level => s"!/level-$level.jar").mkString
          val dirty = nested(depth, _bytes("x" * offset + _private_marker))
          val clean = nested(depth, _bytes("x" * offset + _public_marker))
          val named = zip("InternalPrivateApi.class", _bytes(_public_marker))
          When("the same detector interprets private content private names and clean public controls")
          val dirtyresult = withArchive(dirty) { path => (path.getFileName.toString, scan(path)) }
          val cleanresult = withArchive(clean)(scan(_))
          val namedresult = withArchive(named) { path => (path.getFileName.toString, scan(path)) }
          Then("each private leak has its exact nested path and the clean public archive is not rejected")
          dirtyresult._2.leaks should contain(Leak(dirtyresult._1 + suffix + "!/payload.txt", _private_marker))
          namedresult._2.leaks should contain(Leak(namedresult._1 + "!/InternalPrivateApi.class", "InternalPrivateApi"))
          cleanresult.leaks shouldBe empty
          cleanresult.containsToken(_public_marker) shouldBe true
        }
      }

      "X7 fail closed on missing or malformed receipt evidence corrupt archives and exceeded bounds" in {
        Given("the actual locator fields and small invalid detector-only archive variants")
        val fieldsvalue = fields(_evidence)
        val receipt = fieldsvalue.toVector.sortBy(_._1).map { case (key, value) => s"$key=$value" }.mkString("\n")
        val valid = zip("payload.txt", _bytes(_public_marker))
        val corrupt = zip("broken.jar", _bytes("not a ZIP"))
        When("receipt admission and bounded archive interpretation encounter unavailable or invalid evidence")
        val missingfield = intercept[IllegalArgumentException](decode(fieldsvalue - "mainJar"))
        val extrafield = intercept[IllegalArgumentException](decode(fieldsvalue + ("extra" -> "value")))
        val schema = intercept[IllegalArgumentException](decode(fieldsvalue.updated("schema", "unknown")))
        val missingoutput = intercept[IllegalArgumentException](decode(fieldsvalue.updated("mainJar",
          _evidence.projectroot.resolve("target/no-such-owner-output.jar").toString)))
        val invalidlocator = intercept[IllegalArgumentException](decode(fieldsvalue.updated("mainJar", "relative.jar")))
        val duplicate = intercept[IllegalArgumentException](readReceipt(new ByteArrayInputStream(
          _bytes(receipt + "\nmainJar=" + fieldsvalue("mainJar")))))
        val corruptarchive = withArchive(corrupt)(path => intercept[IllegalArgumentException](scan(path)))
        val truncated = withArchive(valid.take(10))(path => intercept[IllegalArgumentException](scan(path)))
        val depthlimit = withArchive(nested(1, _bytes(_public_marker)))(path =>
          intercept[IllegalArgumentException](scan(path, ScanLimits(depth = 0))))
        val entrylimit = withArchive(valid)(path =>
          intercept[IllegalArgumentException](scan(path, ScanLimits(entrybytes = 4))))
        val totallimit = withArchive(valid)(path =>
          intercept[IllegalArgumentException](scan(path, ScanLimits(totalbytes = 4))))
        Then("each rejected input produces a failed proof instead of an empty successful exclusion result")
        Vector(missingfield, extrafield, schema, missingoutput, invalidlocator, duplicate,
          corruptarchive, truncated, depthlimit, entrylimit, totallimit)
          .foreach(_.getMessage should not be empty)
      }
    }
  }

  private def _bytes(value: String): Array[Byte] = value.getBytes(StandardCharsets.UTF_8)
  private def _json(path: Path): Json = parse(text(path)).fold(error => fail(error.message), identity)
  private def _string(json: Json, key: String): String =
    json.hcursor.get[String](key).fold(error => fail(error.message), identity)
}
