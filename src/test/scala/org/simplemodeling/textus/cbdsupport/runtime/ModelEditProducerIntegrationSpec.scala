package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit
import io.circe.parser.parse
import ModelEditCmlProjection.given
import ModelEditSourceProjection.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Effectful native producer check: separate JVM, actual CML, real read/render/re-production. */
final class ModelEditProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ModelEditProjectionSpecFixture.*
  "Qualified Cozy model-edit consumption" should {
    "produce, read, apply five creation commands, render and re-produce a candidate through the shipped JVM entry" in {
      Given("the explicitly selected independent Cozy export and owned creation fixtures")
      val producerroot = Paths.get(sys.props.getOrElse("textus.cbd.modelEditProducerRoot",
        fail("producer-focused check requires explicit textus.cbd.modelEditProducerRoot")))
      val expectedversion = sys.props.getOrElse("textus.cbd.modelEditProducerVersion", fail("qualified producer version is required"))
      val expectedrevision = sys.props.getOrElse("textus.cbd.modelEditProducerRevision", fail("qualified producer revision is required"))
      val resources = Paths.get("src/test/resources/model-edit-producer").toAbsolutePath
      When("the actual producer and reader run from their exported classpath")
      val classpath = Files.readString(producerroot.resolve("target/cozy.d/runtime-classpath.txt")).trim
      val entries = classpath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator), -1).toVector
      val entriespresent = entries.nonEmpty && entries.forall(value => value.nonEmpty && Files.exists(Paths.get(value)))
      val workroot = Paths.get("target/model-edit-producer-integration").toAbsolutePath
      Files.createDirectories(workroot)
      val work = Files.createTempDirectory(workroot, "actual-")
      val input = resources.resolve("model/model-edit-source.cml")
      val original = Files.readAllBytes(input).toVector
      val publicationtext = _run(classpath, work, "produce", Vector("produce", input.toString, source.owner, source.identity, source.revision))
      val basis = ModelEditSourceProjectionCodec.decodePublication(publicationtext).toOption.get
      val publishedpath = work.resolve("publication.json")
      Files.writeString(publishedpath, publicationtext, StandardCharsets.UTF_8)
      val readtext = _run(classpath, work, "read", Vector("read", publishedpath.toString))
      val reread = ModelEditSourceProjectionCodec.decodePublication(readtext)
      val fixture = parse(Files.readString(resources.resolve("requests/model-edit-production.json"))).toOption.get.as[ModelEditCmlProjection.Request].toOption.get
      val proposed = basis.copy(source = fixture.source, document = basis.document.copy(coverage = fixture.coverage,
        elements = basis.document.elements ++ fixture.addedElements, relationships = basis.document.relationships ++ fixture.addedRelationships))
      val edited = editedSession(basis, proposed)
      val plan = ModelEditCmlProjection.prepare(selection(basis), edited, fixture.source, proposed.document, selection(proposed).bindings).toOption.get
      val requestpath = work.resolve("candidate-v2.json")
      Files.writeString(requestpath, ModelEditCmlProjection.encodeRequest(plan), StandardCharsets.UTF_8)
      val renderingtext = _run(classpath, work, "render", Vector("render-candidate-v2", input.toString,
        source.owner, source.identity, source.revision, requestpath.toString))
      val rendering = ModelEditSourceProjectionCodec.decodeRendering(renderingtext).toOption.get
      val proposedpath = work.resolve("candidate.cml")
      Files.writeString(proposedpath, rendering.text, StandardCharsets.UTF_8)
      val reproducedtext = _run(classpath, work, "reproduce", Vector("produce", proposedpath.toString,
        fixture.source.owner, fixture.source.identity, fixture.source.revision))
      val reproduced = ModelEditSourceProjectionCodec.decodePublication(reproducedtext).toOption.get
      val confirmed = ModelEditCmlProjection.confirm(plan, rendering, reproduced)
      val view = confirmed.flatMap(value => ModelEditViewProjection.projectConfirmed(edited, value, Map.empty))
      val phase10 = confirmed.flatMap(value => ModelEditPhase10SpecFixture.prepare(value, edited, original))
      val canonicalafter = Files.readAllBytes(input).toVector
      Then("actual executing producer identity and typed meaning qualify the CBD connection without canonical changes")
      entriespresent shouldBe true
      basis.producer.name shouldBe "cozy"
      basis.producer.version shouldBe expectedversion
      basis.producer.revision shouldBe expectedrevision
      basis.producer.modified shouldBe false
      reread shouldBe Right(basis)
      edited.history.size shouldBe 5
      reproduced.document.elements.size shouldBe 13
      reproduced.document.relationships.size shouldBe 9
      confirmed.isRight shouldBe true
      withClue(phase10.left.toOption) { phase10.isRight shouldBe true }
      phase10.toOption.get.mapping.producer.revision shouldBe expectedrevision
      phase10.toOption.get.subject.map(_.artifactId.value) should contain("model-edit-source-mapping")
      view.toOption.get.confirmation.canonicalSource shouldBe source
      reproduced.document.elements.find(_.kind == "slice").get.fields shouldBe fixture.addedElements.find(_.kind == "slice").get.fields
      view.toOption.get.traces.head.specificationLinks.size shouldBe 1
      view.toOption.get.traces.head.ticketLinks.size shouldBe 1
      view.toOption.get.actorGoals.candidate shouldBe view.toOption.get.eventStorming.candidate
      rendering.text should include("Creation must preserve this authored note.")
      canonicalafter shouldBe original
    }
  }
  private def _run(classpath: String, work: Path, label: String, arguments: Vector[String]): String = {
    val java = Paths.get(sys.props("java.home"), "bin", "java").toString
    val output = work.resolve(label + ".json")
    val errors = work.resolve(label + ".stderr")
    val command = Vector(java, "-Xmx1024m", "-cp", classpath, "cozy.modeler.CmlModelEditCli") ++ arguments
    val process = new ProcessBuilder(command*).directory(work.toFile).redirectOutput(output.toFile).redirectError(errors.toFile).start()
    try {
      if (!process.waitFor(120, TimeUnit.SECONDS)) fail("Cozy model-edit operation timed out; evidence: " + work)
      if (process.exitValue() != 0) fail("Cozy model-edit operation failed; diagnostics: " + errors)
      Files.readString(output, StandardCharsets.UTF_8)
    } finally {
      if (process.isAlive) {
        process.destroyForcibly()
        process.waitFor(10, TimeUnit.SECONDS)
      }
    }
  }
}
