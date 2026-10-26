package org.simplemodeling.textus.cbdsupport

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.spi.SpiResolver
import org.goldenport.cncf.spi.ai.runner.{AiRunnerApplicationPurpose, AiRunnerApplicationPurposePolicy, AiRunnerApplicationPurposeRegistrationSocketSet, AiRunnerSocketSet}
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.protocol.{Property, Protocol, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}

/*
 * @since   Oct. 17, 2026
 * @version Oct. 26, 2026
 * @author  ASAMI, Tomoharu
 */
final class ScopedQualityReviewOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import ScopedQualityReviewSpecSupport.*

  "Generated scoped quality operation (docs/spec/scoped-quality-review.md)" should {
    "O1 connect the normal generated Operation, factory, handwritten ActionCall and provider UoW" in {
      Given("R1,R3,R5: normal typed input and a CBD component with its optional input installed by SPI bootstrap")
      val input = request()
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      val operation = CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation
      val action = valueOf(operation.createOperationRequest(_request(input.toRecord())))
      val core = ActionCall.Core(action, fixture.context, Some(fixture.component), None)
      When("the normal generated action resolves its factory hook and executes")
      val call = action.createCall(core)
      val output = valueOf(call.execute())
      val result = output match {
        case OperationResponse.RecordResponse(record) => valueOf(cbdvalue.ScopedQualityReviewResult.createC(record))
        case _ => fail("Expected generated typed result record")
      }
      Then("the handwritten call returns a typed covered target with actual provider provenance")
      call shouldBe a[impl.ScopedQualityReviewActionCall]
      result.status shouldBe "completed"
      result.reviewId shouldBe input.reviewId
      result.revision shouldBe input.revision
      result.subjectId shouldBe input.subjectId
      result.subjectRevision shouldBe input.subjectRevision
      result.providerId shouldBe "recording-provider"
      result.findings shouldBe Vector.empty
      runner.requests.size shouldBe 1
      runner.contexts.head should be theSameInstanceAs fixture.context
      fixture.trace should include("review-scoped-quality")
    }

    "O2 reject missing normal required input before AI" in {
      Given("R1,R5: ordinary operation envelopes missing required review or subject references")
      val input = request()
      val runner = new RecordingAiRunner(Consequence.success(response(input)))
      val fixture = runtime(Some(runner))
      val records = Vector(Record.dataAuto("revision" -> 1L), Record.dataAuto("reviewId" -> "review-1", "revision" -> 1L))
      When("normal generated input decoding and ActionCall execution receive the incomplete requests")
      val results = records.map { record =>
        CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation.createOperationRequest(_request(record)).flatMap { action =>
          action.createCall(ActionCall.Core(action, fixture.context, Some(fixture.component), None)).execute()
        }
      }
      Then("each request fails through the ordinary input contract before the provider calls AI")
      results.foreach(_.isFaillure shouldBe true)
      runner.requests shouldBe Vector.empty
      fixture.trace should not include "review-scoped-quality"
    }

    "O3 register the semantic purpose through SPI while keeping the QUERY private and managed effects independent of AI" in {
      Given("R3,R5: factory-created CBD instances without AI and an ordinary application-purpose registration consumer")
      val fixture = runtime(None)
      val other = runtime(None)
      val component = fixture.component
      given ExecutionContext = fixture.context
      val registrationset = new AiRunnerApplicationPurposeRegistrationSocketSet {}
      val consumer = new Component() {}.withPort(Component.Port.input(registrationset))
      val identity = ComponentId("org.simplemodeling.textus.scopedquality.SpecPurposeConsumer")
      consumer.initialize(ComponentInit(subsystem = DefaultSubsystemFactory.default(Some("server")),
        core = Component.Core.create(identity.name, identity, ComponentInstanceId.default(identity), Protocol.empty),
        origin = ComponentOrigin.Builtin))
      val input = cbdvalue.ManagedEffectReviewRequest("managed-review", 1L,
        Vector(cbdvalue.ManagedEffectReviewSubject("resource", "src/Example.scala", "resource-read", None, None)))
      val envelope = Request.of(component = "CbdSupport", service = "CbdReviewAdmin", operation = "reviewManagedEffects",
        properties = input.toRecord().fields.map(field => Property(field.key, field.value.single, None)).toList)
      When("normal SPI collects purpose registrations, projections inspect the component and the managed-effects query executes")
      val resolved = SpiResolver.resolve(Vector(component, consumer))
      val definition = component.operationDefinitions.find(_.name == "reviewScopedQuality")
      val tools = McpToolCatalog.toolsForComponent(component)
      val output = CbdSupportComponent.CbdReviewAdminService.ReviewManagedEffectsOperation.createOperationRequest(envelope).flatMap { action =>
        action.createCall(ActionCall.Core(action, fixture.context, Some(component), None)).execute()
      }
      Then("exactly one semantic purpose has its standard purpose and default policy, while the QUERY stays private and managed effects run")
      resolved.isSuccess shouldBe true
      registrationset.registrations.size shouldBe 1
      registrationset.registrations.flatMap(_.purposes) shouldBe Vector(
        AiRunnerApplicationPurpose(
          name = "car-review.semantic-consistency",
          defaultStandardPurpose = "software-analysis",
          defaultPolicy = AiRunnerApplicationPurposePolicy()
        )
      )
      definition.map(_.kind) shouldBe Some("QUERY")
      definition.map(_.inputType) shouldBe Some("ScopedQualityReviewRequest")
      definition.map(_.outputType) shouldBe Some("ScopedQualityReviewResult")
      component.isMcpReady("CbdReviewAdmin", "reviewScopedQuality") shouldBe false
      tools.exists(_.name.contains(".CbdReviewAdmin.")) shouldBe false
      component.port.get[AiRunnerSocketSet].get should not be theSameInstanceAs(other.component.port.get[AiRunnerSocketSet].get)
      component.port.get[AiRunnerSocketSet].get.spiRequired shouldBe false
      output.isSuccess shouldBe true
    }
    "O4 preserve applicable findings when the caller selects affected checks for a new review revision" in {
      Given("R1,R4,R5: a full review and a nonempty affected subset with explicit current references and an applicable finding")
      val selections = Gen.someOf(request().checks).suchThat(_.nonEmpty)
      forAll(selections, Gen.choose(1L, 100L)) { (selected, revision) =>
        val checks = selected.toVector
        val original = request().copy(revision = revision)
        val affected = original.copy(revision = revision + 1, subjectRevision = "subject-revision-4", checks = checks)
        val finding = cbdvalue.ScopedQualityReviewFinding("source.contract.issue", checks.head, "blocker",
          Some(original.paths.head), None, "The selected source still violates the supplied contract.")
        val originalrunner = new RecordingAiRunner(Consequence.success(response(original, findings = Vector(finding))))
        val affectedrunner = new RecordingAiRunner(Consequence.success(response(affected,
          subjectRevision = affected.subjectRevision, findings = Vector(finding))))
        val originalfixture = runtime(Some(originalrunner))
        val affectedfixture = runtime(Some(affectedrunner))
        When("both requests pass through the normal generated Operation and provider UoW")
        val first = _review(original, originalfixture)
        val result = _review(affected, affectedfixture)
        Then("only selected checks are returned, current target references apply, and the applicable finding retains its identity and scope")
        first.status shouldBe "completed"
        result.status shouldBe "completed"
        result.checks.map(_.checkId) shouldBe checks
        result.checks.foreach(_.status shouldBe "covered")
        result.reviewId shouldBe affected.reviewId
        result.revision shouldBe affected.revision
        result.subjectRevision shouldBe affected.subjectRevision
        result.findings shouldBe first.findings
        result.findings shouldBe Vector(finding)
        originalrunner.requests.size shouldBe 1
        affectedrunner.requests.size shouldBe 1
      }
    }
  }

  "Generated model used by the scoped review component" should {
    "G1 roundtrip nested Entity framework fields through the actual declared-store binding" in {
      import cats.data.NonEmptyVector
      import org.goldenport.cncf.entity.{EntityPersistent, EntityStoreRowMapping}
      import org.goldenport.datatype.{Identifier, I18nLabel, I18nTitle, Name, Slug}
      import org.goldenport.record.{Field, Record}
      import org.simplemodeling.model.datatype.{EntityId, EntityRevision}
      import org.simplemodeling.model.value.*
      import org.simplemodeling.model.statemachine.{ActivationStatus, Aliveness, PostStatus}
      import org.simplemodeling.textus.cbdsupport.entity.ReviewTargetSnapshot
      import org.simplemodeling.textus.cbdsupport.entity.aggregate.ReviewDiagnosis

      val slugs = Gen.choose(1, 64).flatMap(size => Gen.listOfN(size, Gen.alphaLowerChar).map(_.mkString))
      forAll(slugs) { slug =>
        Given("a generated nested Entity with all NameAttributes fields and populated framework content and media values")
        val collection = ReviewTargetSnapshot.collectionId
        val targetid = valueOf(EntityId.parse(s"major-minor-entity-ec1_5_major_5_minor_${collection.name.length}_${collection.name}-0-fixture"))
        val names = org.goldenport.value.NameAttributes(
          Name("Snapshot"), Some(I18nLabel("Snapshot label")), Some(I18nTitle("Snapshot title")),
          Some(Identifier("SnapshotCode")), Some(NonEmptyVector.of(I18nLabel("Alias one"), I18nLabel("Alias two"))),
          Some(valueOf(Slug.create(slug))))
        val content = ContentAttributes(
          content = Some(org.goldenport.value.ContentBody("Framework body")),
          mimeType = Some(org.goldenport.datatype.MimeType.TEXT_PLAIN),
          charset = Some(java.nio.charset.StandardCharsets.UTF_8),
          markup = Some(org.goldenport.value.ContentMarkup.MarkdownGfm),
          references = Vector(org.goldenport.value.ContentReferenceOccurrence(originalRef = Some("asset.png"), occurrenceIndex = 2)))
        val lifecycle = LifecycleAttributes(java.time.Instant.EPOCH, java.time.Instant.parse("2026-10-26T01:02:03Z"),
          Identifier("Creator"), Identifier("Updater"), PostStatus.Published, Aliveness.Suspended)
        val publication = PublicationAttributes(Some(java.time.ZonedDateTime.parse("2026-10-26T01:02:03Z")), None, None, None, None)
        val security = SecurityAttributes.publicOwnedBy("Owner")
        val resources = ResourceAttributes(activationStatus = ActivationStatus.Active)
        val leafmedia = MediaAttributes(Some(new java.net.URL("https://example.invalid/snapshot")), Vector.empty, Vector.empty, Vector.empty, Vector.empty)
        val leaf = SimpleObjectContent(names, DescriptiveAttributes.empty, content, lifecycle, publication,
          security, resources, AuditAttributes(), leafmedia, ContextualAttributes())
        val media = leafmedia.copy(
          images = Vector(Image(org.goldenport.datatype.ObjectId(Identifier("ImageOne")), leaf)),
          audios = Vector(Audio(org.goldenport.datatype.ObjectId(Identifier("AudioOne")), leaf)),
          videos = Vector(Video(org.goldenport.datatype.ObjectId(Identifier("VideoOne")), leaf)),
          atathments = Vector(Attachment(org.goldenport.datatype.ObjectId(Identifier("AttachmentOne")), leaf)))
        val snapshot = ReviewTargetSnapshot(targetid, EntityRevision.INITIAL, names, DescriptiveAttributes.empty,
          content, lifecycle, publication, security, resources, AuditAttributes(), media, ContextualAttributes(),
          targetid, cbdvalue.ReviewTargetKind("component"), Some(cbdvalue.ComponentOrganization("org.example")),
          cbdvalue.ComponentName("Example"), Some(cbdvalue.ReviewVersion("1.0")), cbdvalue.ReviewDigest("digest"),
          cbdvalue.ReviewInstant("2026-10-26T01:02:03Z"))
        val targets = summon[EntityPersistent[ReviewDiagnosis]].storeRowMapping.get.fields.find(_.logicalName == "targets").get
        val mapping = EntityStoreRowMapping(Vector(targets))
        val logical = Record(Vector(Field("targets", Field.Value.Single(Vector(snapshot)))))

        When("the generated parent field's captured application codec encodes and decodes the declared nested Entity vector")
        val physical = valueOf(mapping.encode(ReviewDiagnosis.collectionId, logical))
        val json = io.circe.parser.parse(physical.getString("targets").get).toOption.get
        val restored = valueOf(mapping.decode(ReviewDiagnosis.collectionId, physical))
        val decoded = restored.getAny("targets").get.asInstanceOf[Vector[ReviewTargetSnapshot]].head
        val invalidjsons = Vector(
          json.hcursor.downArray.downField("nameAttributes").downField("slug")
            .withFocus(_ => io.circe.Json.fromString("")).top.get,
          json.hcursor.downArray.downField("contentAttributes").downField("charset")
            .withFocus(_ => io.circe.Json.fromString("not-a-supported-charset")).top.get,
          json.hcursor.downArray.downField("mediaAttributes").downField("url")
            .withFocus(_ => io.circe.Json.fromString("not-a-url")).top.get)
        val invalidresults = invalidjsons.map { payload =>
          mapping.decode(ReviewDiagnosis.collectionId, Record(Vector(Field("targets", Field.Value.Single(payload.noSpaces)))))
        }

        Then("all framework and ordinary fields survive, including the nested media Entity content, and malformed Slug, Charset and URL JSON fails")
        decoded.id shouldBe snapshot.id
        decoded.revision shouldBe snapshot.revision
        decoded.descriptiveAttributes shouldBe snapshot.descriptiveAttributes
        decoded.auditAttributes shouldBe snapshot.auditAttributes
        decoded.contextualAttribute shouldBe snapshot.contextualAttribute
        decoded.nameAttributes shouldBe names
        decoded.nameAttributes.slug.map(_.value) shouldBe Some(slug)
        decoded.contentAttributes shouldBe content
        decoded.lifecycleAttributes shouldBe lifecycle
        decoded.publicationAttributes shouldBe publication
        decoded.securityAttributes shouldBe security
        decoded.resourceAttributes shouldBe resources
        decoded.mediaAttributes.url.map(_.toExternalForm) shouldBe leafmedia.url.map(_.toExternalForm)
        val actualmedia = Vector(
          decoded.mediaAttributes.images.map(value => value.id -> value.simpleobject),
          decoded.mediaAttributes.audios.map(value => value.id -> value.simpleobject),
          decoded.mediaAttributes.videos.map(value => value.id -> value.simpleobject),
          decoded.mediaAttributes.atathments.map(value => value.id -> value.simpleobject))
        val expectedmedia = Vector(
          media.images.map(value => value.id -> value.simpleobject),
          media.audios.map(value => value.id -> value.simpleobject),
          media.videos.map(value => value.id -> value.simpleobject),
          media.atathments.map(value => value.id -> value.simpleobject))
        actualmedia.zip(expectedmedia).foreach { case (actual, expected) =>
          actual.map(_._1) shouldBe expected.map(_._1)
          actual.map(_._2).zip(expected.map(_._2)).foreach { case (decodedcontent, originalcontent) =>
            decodedcontent.mediaAttributes.url.map(_.toExternalForm) shouldBe
              originalcontent.mediaAttributes.url.map(_.toExternalForm)
            decodedcontent.copy(mediaAttributes = decodedcontent.mediaAttributes.copy(url = None)) shouldBe
              originalcontent.copy(mediaAttributes = originalcontent.mediaAttributes.copy(url = None))
          }
        }
        decoded.diagnosis_id shouldBe snapshot.diagnosis_id
        decoded.target_kind shouldBe snapshot.target_kind
        decoded.created_at shouldBe snapshot.created_at
        decoded.component_name shouldBe snapshot.component_name
        decoded.organization shouldBe snapshot.organization
        decoded.component_version shouldBe snapshot.component_version
        decoded.target_digest shouldBe snapshot.target_digest
        json.hcursor.downArray.downField("nameAttributes").downField("slug").as[String] shouldBe Right(slug)
        invalidresults.foreach(_.isFaillure shouldBe true)
      }
    }
  }

  private def _review(input: cbdvalue.ScopedQualityReviewRequest, fixture: RuntimeFixture): cbdvalue.ScopedQualityReviewResult = {
    val operation = CbdSupportComponent.CbdReviewAdminService.ReviewScopedQualityOperation
    val action = valueOf(operation.createOperationRequest(_request(input.toRecord())))
    val result = valueOf(action.createCall(ActionCall.Core(action, fixture.context, Some(fixture.component), None)).execute())
    result match {
      case OperationResponse.RecordResponse(record) => valueOf(cbdvalue.ScopedQualityReviewResult.createC(record))
      case _ => fail("Expected generated typed result record")
    }
  }

  private def _request(record: Record): Request = Request.of(component = "CbdSupport", service = "CbdReviewAdmin",
    operation = "reviewScopedQuality", properties = record.fields.map(field => Property(field.key, field.value.single, None)).toList)
}
