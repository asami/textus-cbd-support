package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalacheck.{Gen, Prop, Test}
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityIngestionFixtures.{right, left, capability}
import CapabilityComponentUsageProjectionFixtures.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityComponentUsageProjectionCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _codec = CapabilityComponentUsageProjectionCodec
  private def _admit(value: ComponentUsageProjection, selected: Scope = scope, definitions: Vector[CapabilityDefinition] = Vector(capability)) =
    _codec.admit(value, selected, definitions, Vector(value))
  private def _kind(value: Either[Diagnostic, ?]): DiagnosticKind = left(value).kind
  private def _mapping(value: ComponentUsageProjection, identity: ModelElementId, version: String): ComponentUsageProjection = {
    val a = usageDefinition(value)
    val xs = CapabilityComponentUsageJsonPart._items(a.componentMappings)
    withUsage(value, a.copy(componentMappings = Present(xs.updated(0, xs.head.copy(target = CapabilityReference(identity, version))))))
  }
  private def _dependency(value: ComponentUsageProjection, identity: ModelElementId, version: String): ComponentUsageProjection = {
    val a = usageDefinition(value)
    val xs = CapabilityComponentUsageJsonPart._items(a.dependencies)
    withUsage(value, a.copy(dependencies = Present(xs.updated(0, xs.head.copy(target = ComponentUsageCapabilityReference(identity, version))))))
  }
  "Complete ComponentUsage transport" should {
    "C11A01 retain every semantic field and authored edge in a complete roundtrip" in {
      Given("C11I-D05/06: literal full synthetic wire, finite facts and independently supplied Component definition")
      val original = right(_codec.decode(literal))
      When("the complete projection is admitted, encoded and decoded")
      val admitted = right(_admit(original))
      val roundtrip = right(_codec.decode(right(_codec.encode(admitted.projection))))
      Then("semantic values, all document/edge order and independent source/origin values survive")
      roundtrip shouldBe original
      roundtrip.documents.map(_.componentUsageCapabilities.size) shouldBe Vector(1, 0, 1)
      admitted.projection should be theSameInstanceAs original
      admitted.unresolvedExternal.map(_.target) shouldBe Vector(
        ExternalTarget.ComponentCapability(CapabilityReference(ModelElementId("external:cap", "É::cap"), "external-v6")),
        ExternalTarget.ComponentUsageCapability(ComponentUsageCapabilityReference(ModelElementId("external:app", "aux"), "external-v9")))
      usageDefinition(roundtrip).source.sourceRevision shouldBe "definition-A"
      CapabilityComponentUsageJsonPart._items(usageDefinition(roundtrip).demandReferences)(1).origin shouldBe
        Derived("derive:É|É", Vector(Source("foreign:input", "foreign/input.cml", "input-v3", None)))
    }
    "C11A02 reject missing, extra, null, wrong-shape and duplicate-key branches" in {
      Given("C11I-D06: independently mutated complete literal objects, including escaped duplicate keys")
      val path = usagePath
      val missing = update(json, path)(j => Json.fromJsonObject(j.asObject.get.remove("name"))).noSpaces
      val extra = update(json, path)(j => j.mapObject(_.add("surprise", Json.fromBoolean(true)))).noSpaces
      val mixed = changed(path ++ Vector("summary"), Json.obj("state" -> Json.fromString("PRESENT"), "value" -> Json.fromString("x"), "reason" -> Json.fromString("NotDeclared")))
      val wrong = Vector(changed(Vector("documents"), Json.obj()), changed(path ++ Vector("grounding", "value"), Json.fromString("not-array")),
        changed(path ++ Vector("contractVersion"), Json.fromInt(3)), changed(path ++ Vector("source"), Json.Null))
      val duplicates = Vector(literal.replace("\"id\":\"cozy\"", "\"id\":\"cozy\",\"id\":\"other\""),
        literal.replace("\"id\":\"cozy\"", "\"id\":\"cozy\",\"\\u0069d\":\"other\""),
        literal.replace("\"reason\":\"NotDeclared\"", "\"reason\":\"NotDeclared\",\"reason\":\"Unsupported\""))
      When("each complete input is strictly decoded before map conversion")
      val errors = (Vector(missing, extra, mixed) ++ wrong ++ duplicates).map(x => left(_codec.decode(x)))
      Then("all malformed branches reject with source-correlated nested pointers and no duplicate winner")
      errors.map(_.kind).distinct shouldBe Vector(DiagnosticKind.Malformed)
      errors.head.location shouldBe "/documents/0/componentUsageCapabilities/0/name"
      errors.head.source shouldBe Some(projection.documents.head.source)
      errors(2).location shouldBe "/documents/0/componentUsageCapabilities/0/summary/reason"
      errors.takeRight(3).map(_.location) shouldBe Vector.fill(3)("/")
    }
    "C11A03 dispatch headers first and preserve literal lexical domains" in {
      Given("C11I-D02/06: invalid scalar/path/line/text branches and unsupported future headers/tags")
      val bad = Vector(changed(Vector("schemaVersion"), Json.fromBoolean(true)), changed(Vector("schemaVersion"), Json.fromBigDecimal(BigDecimal("1.5"))),
        changed(usagePath ++ Vector("name"), Json.fromString("\u00a0padded")), changed(usagePath ++ Vector("name"), Json.fromString("control\u0000")),
        changed(usagePath ++ Vector("source", "path"), Json.fromString("models/../app.cml")), changed(usagePath ++ Vector("source", "line"), Json.fromInt(0)),
        changed(usagePath ++ Vector("name"), Json.Null), changed(usagePath ++ Vector("source", "path"), Json.fromString("C:/app.cml")))
      val future = Vector("""{"schemaVersion":2,"generatedAbiVersion":null,"documents":null}""",
        """{"schemaVersion":1,"generatedAbiVersion":"future.abi","documents":null}""",
        changed(usagePath ++ Vector("demandReferences", "value", "0", "role"), Json.fromString("FUTURE_ROLE")))
      val invalidcarrier = projection.copy(documents = Vector(projection.documents.head.copy(source = projection.documents.head.source.copy(line = null))))
      When("headers and nested lexical values are decoded and an invalid supplied carrier is encoded")
      val malformed = bad.map(x => _kind(_codec.decode(x))) :+ _kind(_codec.encode(invalidcarrier))
      val unsupported = future.map(x => _kind(_codec.decode(x)))
      val valid = right(_codec.decode(literal))
      Then("invalid shape is Malformed, future contracts are Unsupported before nested decoding, and Unicode remains literal")
      malformed.distinct shouldBe Vector(DiagnosticKind.Malformed)
      unsupported shouldBe Vector.fill(3)(DiagnosticKind.Unsupported)
      usageDefinition(valid).identity.elementId shouldBe "É|É::支払/1"
      usageDefinition(valid).name shouldBe "支払 \"É\" \\ É | #"
    }
    "C11A04 enforce source ownership without equating independent revisions" in {
      Given("C11I-D05/06: foreign definition authority/path/identity and owned-edge authority, with valid foreign Derived inputs")
      val invalid = Vector(changed(usagePath ++ Vector("component", "id"), Json.fromString("OtherOwner")), changed(usagePath ++ Vector("source", "modelAuthority"), Json.fromString("foreign")),
        changed(usagePath ++ Vector("source", "path"), Json.fromString("models/other.cml")),
        changed(usagePath ++ Vector("identity", "modelId"), Json.fromString("foreign")),
        changed(usagePath ++ Vector("componentMappings", "value", "0", "source", "modelAuthority"), Json.fromString("foreign")))
      val valid = projection
      When("the foreign ownership variants and complete independently revised projection are decoded/admitted")
      val errors = invalid.map(x => left(_codec.decode(x)))
      val admitted = right(_admit(valid))
      Then("foreign ownership is Incompatible while header/definition/edge/Derived-input versions and lines survive")
      errors.map(_.kind) shouldBe Vector.fill(5)(DiagnosticKind.Incompatible)
      admitted.projection.documents.head.source.sourceRevision shouldBe "header-A"
      usageDefinition(admitted.projection).source shouldBe Source("model:app", "models/app.cml", "definition-A", Some(4))
      CapabilityComponentUsageJsonPart._items(usageDefinition(admitted.projection).componentMappings).map(_.source.sourceRevision) shouldBe Vector("mapping-r41", "mapping-r42")
      errors.last.source.get.modelAuthority shouldBe "foreign"
    }
    "C11A05 reject equal owning keys and retain distinct versions and document headers" in {
      Given("C11I-D06: repeated header differing only in line and repeated ComponentUsage identity/version across genuine headers")
      val p = projection
      val headerduplicate = p.copy(documents = p.documents :+ ComponentUsageDocument(p.documents.head.source.copy(line = Some(99)), Vector.empty))
      val repeated = p.copy(documents = p.documents :+ p.documents.head.copy(source = p.documents.head.source.copy(sourceRevision = "another-header")))
      val distinct = p.copy(documents = p.documents.updated(0, p.documents.head.copy(componentUsageCapabilities = Vector(usageDefinition(p), usageDefinition(p).copy(contractVersion = "app-v18")))))
      When("each complete transport is encoded through the same strict decoder")
      val errors = Vector(headerduplicate, repeated).map(x => left(_codec.encode(x)))
      val preserved = right(_codec.decode(right(_codec.encode(distinct))))
      Then("equal keys are Ambiguous including equal data, but genuine headers and separate versions stay ordered")
      errors.map(_.kind) shouldBe Vector.fill(2)(DiagnosticKind.Ambiguous)
      preserved.documents.head.componentUsageCapabilities.map(_.contractVersion) shouldBe Vector("app-v17", "app-v18")
      preserved.documents.map(_.source) shouldBe distinct.documents.map(_.source)
    }
  }
  "Explicit finite ComponentUsage binding" should {
    "C11A06 resolve Local Component mappings and ComponentUsage dependencies by exact kind and version" in {
      Given("C11I-D06: versioned targets with missing, wrong-kind, wrong-version and repeated exact Component witnesses")
      val p = projection
      val absent = ModelElementId("missing", "cap")
      val aux = p.documents(2).componentUsageCapabilities.head
      val mappings = Vector(_mapping(p, absent, "cap-v7"), _mapping(p, aux.identity, aux.contractVersion), _mapping(p, capability.identity, "cap-v8"))
      val dependencies = Vector(_dependency(p, absent, "aux-v2"), _dependency(p, capability.identity, capability.contractVersion), _dependency(p, aux.identity, "aux-v3"))
      val duplicateusages = p.copy(documents = p.documents :+ p.documents(2).copy(source = p.documents(2).source.copy(sourceRevision = "another-aux-header")))
      When("each target is admitted solely against the selected finite definitions")
      val errors = (mappings ++ dependencies).map(x => left(_admit(x)))
      val ambiguous = left(_admit(p, definitions = Vector(capability, capability)))
      val original = right(_admit(p))
      val usageambiguity = left(_codec.encode(duplicateusages))
      Then("no nearby version or label wins; repeated independent witnesses remain ambiguous for Local binding")
      errors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible,
        DiagnosticKind.Malformed, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible)
      ambiguous.kind shouldBe DiagnosticKind.Ambiguous
      ambiguous.location shouldBe "/documents/0/componentUsageCapabilities/0/componentMappings/value/0/target"
      original.projection shouldBe p
      usageambiguity.kind shouldBe DiagnosticKind.Ambiguous
    }
    "C11A07 bind demand roles and Specification/Evidence separately" in {
      Given("C11I-D06/07: missing, wrong-kind and duplicate model facts, a wrong demand role and independent Evidence target")
      val p = projection
      val variants = Vector(scope.copy(modelFacts = scope.modelFacts.filterNot(_.kind == ModelKind.Goal)),
        scope.copy(modelFacts = scope.modelFacts.map(f => if (f.kind == ModelKind.Goal) f.copy(kind = ModelKind.Component) else f)),
        scope.copy(modelFacts = scope.modelFacts :+ scope.modelFacts.head),
        scope.copy(modelFacts = scope.modelFacts.filterNot(_.kind == ModelKind.Evidence)),
        scope.copy(modelFacts = scope.modelFacts.map(f => if (f.kind == ModelKind.Evidence) f.copy(kind = ModelKind.Specification) else f)))
      val wrongrole = changed(usagePath ++ Vector("demandReferences", "value", "0", "role"), Json.fromString("COMPONENT"))
      val traceonly = withUsage(p, usageDefinition(p).copy(discoveryReferences = Present(Vector.empty)))
      val tracescopes = Vector(scope.copy(modelFacts = scope.modelFacts.filterNot(_.kind == ModelKind.Evidence)),
        scope.copy(modelFacts = scope.modelFacts.map(f => if (f.kind == ModelKind.Evidence) f.copy(kind = ModelKind.Specification) else f)),
        scope.copy(modelFacts = scope.modelFacts :+ scope.modelFacts.find(_.kind == ModelKind.Specification).get))
      val grounding = CapabilityComponentUsageJsonPart._items(usageDefinition(p).grounding)
      val capabilitymodel = withUsage(p, usageDefinition(p).copy(grounding = Present(grounding.updated(0, grounding.head.copy(target = ModelTarget(capability.identity))))))
      val versions = Vector(capability, capability.copy(contractVersion = "cap-v8"))
      val explicitcapabilityfact = ModelFact(capability.identity, ModelKind.ComponentCapability, capability.source)
      When("the full projection is admitted against each explicit role scope")
      val errors = variants.map(s => left(_admit(p, s)))
      val role = left(_codec.decode(wrongrole))
      val traceerrors = tracescopes.map(s => left(_admit(traceonly, s)))
      val versionpresence = right(_admit(capabilitymodel, definitions = versions))
      val independentfact = left(_admit(capabilitymodel, scope.copy(modelFacts = scope.modelFacts :+ explicitcapabilityfact), versions))
      Then("each semantic role has its own exact resolution and demand remains restricted")
      errors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Incompatible, DiagnosticKind.Ambiguous, DiagnosticKind.Malformed, DiagnosticKind.Incompatible)
      errors(3).location shouldBe "/documents/0/componentUsageCapabilities/0/discoveryReferences/value/6/target"
      role.kind shouldBe DiagnosticKind.Incompatible
      traceerrors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Incompatible, DiagnosticKind.Ambiguous)
      traceerrors.take(2).map(_.location) shouldBe Vector.fill(2)("/documents/0/componentUsageCapabilities/0/traceability/value/1/target")
      CapabilityComponentUsageJsonPart._items(usageDefinition(right(_admit(p)).projection).traceability).map(_.role) shouldBe Vector(TraceabilityRole.Specification, TraceabilityRole.Evidence)
      versionpresence.projection shouldBe capabilitymodel
      independentfact.kind shouldBe DiagnosticKind.Ambiguous
    }
    "C11A08 resolve model, term, relation and profile grounding in independent domains" in {
      Given("C11I-D06: independently absent or duplicate term/relation/profile facts and a model grounding lacking its concept")
      val p = projection
      val variants = Vector(scope.copy(termFacts = Vector.empty), scope.copy(relationFacts = Vector.empty), scope.copy(profileFacts = Vector.empty),
        scope.copy(termFacts = scope.termFacts ++ scope.termFacts), scope.copy(relationFacts = scope.relationFacts ++ scope.relationFacts),
        scope.copy(profileFacts = scope.profileFacts ++ scope.profileFacts), scope.copy(modelFacts = scope.modelFacts.filterNot(_.kind == ModelKind.DomainConcept)),
        scope.copy(termFacts = Vector(scope.termFacts.head.copy(identity = scope.termFacts.head.identity.copy(vocabularyId = "model:facts"), source = scope.termFacts.head.source.copy(modelAuthority = "model:facts")))))
      When("the grounding families are admitted independently against those supplied facts")
      val errors = variants.map(s => left(_admit(p, s)))
      val empty = right(_admit(withUsage(p, usageDefinition(p).copy(grounding = Present(Vector.empty)))))
      Then("qualified domains never alias, duplicates are ambiguous, and Present(empty) supplies no invented target")
      errors.map(_.kind) shouldBe Vector(DiagnosticKind.Malformed, DiagnosticKind.Malformed, DiagnosticKind.Malformed, DiagnosticKind.Ambiguous,
        DiagnosticKind.Ambiguous, DiagnosticKind.Ambiguous, DiagnosticKind.Malformed, DiagnosticKind.Malformed)
      errors.take(3).map(_.location) shouldBe Vector("target", "relation/value", "profile/value").map("/documents/0/componentUsageCapabilities/0/grounding/value/1/" + _)
      usageDefinition(empty.projection).grounding shouldBe Present(Vector.empty)
    }
    "C11A09 retain typed unresolved External evidence and authored direction" in {
      Given("C11I-D06/07: every edge explicitly External and no Local facts or definitions")
      val p = projection
      val a = usageDefinition(p)
      val external = withUsage(p, a.copy(
        demandReferences = Present(CapabilityComponentUsageJsonPart._items(a.demandReferences).map(x => x.copy(target = x.target.copy(boundary = ReferenceBoundary.External)))),
        discoveryReferences = Present(CapabilityComponentUsageJsonPart._items(a.discoveryReferences).map(x => x.copy(target = x.target.copy(boundary = ReferenceBoundary.External)))),
        componentMappings = Present(CapabilityComponentUsageJsonPart._items(a.componentMappings).map(_.copy(boundary = ReferenceBoundary.External))),
        dependencies = Present(CapabilityComponentUsageJsonPart._items(a.dependencies).map(_.copy(boundary = ReferenceBoundary.External))),
        grounding = Present(CapabilityComponentUsageJsonPart._items(a.grounding).map(_.copy(boundary = ReferenceBoundary.External))),
        traceability = Present(CapabilityComponentUsageJsonPart._items(a.traceability).map(x => x.copy(target = x.target.copy(boundary = ReferenceBoundary.External))))))
      When("the explicit External projection is admitted in an empty finite scope")
      val admitted = right(_admit(external, CapabilityIngestionFixtures.scope, Vector.empty))
      Then("every external domain retains its pointer, original edge source/origin and enclosing ComponentUsage direction")
      admitted.projection should be theSameInstanceAs external
      admitted.unresolvedExternal.size shouldBe 21
      admitted.unresolvedExternal.map(_.target).collect { case ExternalTarget.Relation(id) => id } shouldBe Vector(RelationId("vocab:relation", "relation::1"))
      admitted.unresolvedExternal.map(_.target).collect { case ExternalTarget.Profile(id) => id } shouldBe Vector(ProfileId("vocab:profile", "profile/1"))
      admitted.unresolvedExternal.head.source shouldBe CapabilityComponentUsageJsonPart._items(a.demandReferences).head.source
      admitted.unresolvedExternal(1).origin shouldBe CapabilityComponentUsageJsonPart._items(a.demandReferences)(1).origin
      admitted.unresolvedExternal.head.location shouldBe "/documents/0/componentUsageCapabilities/0/demandReferences/value/0/target"
    }
    "C11A10 preserve empty/app-only documents and the complete Presence vocabulary" in {
      Given("C11I-D05/07: all four Absent reasons, Present(empty) and explicit optional nulls in ordered complete documents")
      val p = projection
      val variants = AbsenceReason.values.toVector.map(reason => withUsage(p, usageDefinition(p).copy(summary = Absent(reason, "Literal absence: " + reason))))
      When("all absence variants and the original ordered documents roundtrip")
      val roundtrips = variants.map(v => right(_codec.decode(right(_codec.encode(v)))))
      val original = right(_codec.decode(right(_codec.encode(p))))
      Then("absence reason/detail, empty vectors, ordered empty documents and optional language/line survive independently")
      roundtrips.map(v => usageDefinition(v).summary) shouldBe AbsenceReason.values.toVector.map(r => Absent(r, "Literal absence: " + r))
      original.documents(1).componentUsageCapabilities shouldBe Vector.empty
      original.documents(2).componentUsageCapabilities.head.demandReferences shouldBe Present(Vector.empty)
      original.documents.head.source.line shouldBe None
      original.documents(1).source.line shouldBe Some(13)
      CapabilityComponentUsageJsonPart._items(usageDefinition(original).grounding).head.preferredLabel shouldBe Present(LocalizedLabel("支払 É", None))
    }
    "C11A11 actively preserve complete literal carriers over at least one hundred samples" in {
      Given("C11I-D05/06: complete valid carriers varying literal Unicode, escapes, Presence, Derived inputs, edge order and key presentation")
      val samples = for {
        text <- Gen.oneOf("É::支払|# /", "E\u0301::支払|#", "\"quoted\"\\backslash", "Case::CAP/v2")
        reverse <- Gen.oneOf(true, false)
        reason <- Gen.oneOf(AbsenceReason.values.toVector)
      } yield (text, reverse, reason)
      When("ScalaCheck roundtrips the full projection and reorders JSON object keys semantically")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { (sample: (String, Boolean, AbsenceReason)) =>
        val p = projection
        val a = usageDefinition(p)
        val links = CapabilityComponentUsageJsonPart._items(a.demandReferences)
        val ordered = if (sample._2) links.reverse else links
        val updated = withUsage(p, a.copy(name = sample._1, summary = Absent(sample._3, sample._1), demandReferences = Present(ordered.map(x =>
          x.copy(origin = Derived(sample._1, Vector(Source("foreign:" + sample._1, "inputs/literal.cml", sample._1, Some(7)))))))))
        _codec.encode(updated).flatMap { encoded =>
          val parsed = io.circe.jawn.JawnParser(allowDuplicateKeys = false).parse(encoded).toOption.get
          val presented = Json.fromFields(parsed.asObject.get.toVector.reverse).spaces2
          _codec.decode(presented)
        }.contains(updated)
      })
      Then("all complete semantic values and source/origin fields survive with at least 100 successes and no discards")
      checked.passed shouldBe true
      checked.succeeded should be >= 100
      checked.discarded shouldBe 0
    }
  }
}
