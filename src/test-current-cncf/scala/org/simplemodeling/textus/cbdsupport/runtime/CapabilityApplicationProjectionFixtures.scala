package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import CapabilityIngestionModel.*

/*
 * Literal hand-authored full wire conformance; normal Cozy proof remains owed.
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationProjectionFixtures {
  val literal: String = """{
    "schemaVersion":1,"generatedAbiVersion":"cozy.application-capability.v1",
    "producer":{"id":"cozy","version":"producer-v31"},
    "documents":[
      {"source":{"modelAuthority":"model:app","path":"models/app.cml","sourceRevision":"header-A","line":null},
       "applicationCapabilities":[{
        "kind":"APPLICATION","identity":{"modelId":"model:app","elementId":"É|É::支払/1"},
        "contractVersion":"app-v17","name":"支払 \"É\" \\ É | #",
        "summary":{"state":"ABSENT","reason":"NotDeclared","detail":"Summary undeclared."},
        "description":{"state":"ABSENT","reason":"NotRepresented","detail":"Description not represented."},
        "source":{"modelAuthority":"model:app","path":"models/app.cml","sourceRevision":"definition-A","line":4},
        "demandReferences":{"state":"PRESENT","value":[
          {"role":"GOAL","target":{"identity":{"modelId":"model:facts","elementId":"goal"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/demand.cml","sourceRevision":"demand-r1","line":9},"origin":{"kind":"DECLARED"}},
          {"role":"USE_CASE","target":{"identity":{"modelId":"model:facts","elementId":"usecase"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/demand.cml","sourceRevision":"demand-r2","line":10},"origin":{"kind":"DERIVED","ruleId":"derive:É|É","inputs":[{"modelAuthority":"foreign:input","path":"foreign/input.cml","sourceRevision":"input-v3","line":null}]}},
          {"role":"SCENARIO","target":{"identity":{"modelId":"model:facts","elementId":"scenario"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/demand.cml","sourceRevision":"demand-r3","line":11},"origin":{"kind":"DECLARED"}},
          {"role":"USE_CASE_SLICE","target":{"identity":{"modelId":"model:facts","elementId":"slice"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/demand.cml","sourceRevision":"demand-r4","line":12},"origin":{"kind":"DECLARED"}}
        ]},
        "discoveryReferences":{"state":"PRESENT","value":[
          {"role":"GOAL","target":{"identity":{"modelId":"model:facts","elementId":"goal"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-1","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"USE_CASE","target":{"identity":{"modelId":"model:facts","elementId":"usecase"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-2","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"SCENARIO","target":{"identity":{"modelId":"model:facts","elementId":"scenario"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-3","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"USE_CASE_SLICE","target":{"identity":{"modelId":"model:facts","elementId":"slice"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-4","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"COMPONENT","target":{"identity":{"modelId":"model:facts","elementId":"component"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-5","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"SPECIFICATION","target":{"identity":{"modelId":"model:facts","elementId":"spec"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-6","line":null},"origin":{"kind":"DECLARED"}},
          {"role":"EVIDENCE","target":{"identity":{"modelId":"model:facts","elementId":"evidence"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/discovery.cml","sourceRevision":"discovery-7","line":null},"origin":{"kind":"DECLARED"}}
        ]},
        "componentMappings":{"state":"PRESENT","value":[
          {"target":{"kind":"COMPONENT","identity":{"modelId":"model:component","elementId":"cap"},"contractVersion":"cap-v7"},"boundary":"LOCAL","source":{"modelAuthority":"model:app","path":"edges/mapping.cml","sourceRevision":"mapping-r41","line":20},"origin":{"kind":"DECLARED"}},
          {"target":{"kind":"COMPONENT","identity":{"modelId":"external:cap","elementId":"É::cap"},"contractVersion":"external-v6"},"boundary":"EXTERNAL","source":{"modelAuthority":"model:app","path":"edges/mapping.cml","sourceRevision":"mapping-r42","line":21},"origin":{"kind":"DERIVED","ruleId":"external-rule","inputs":[{"modelAuthority":"foreign:input","path":"foreign/input.cml","sourceRevision":"input-v8","line":7}]}}
        ]},
        "dependencies":{"state":"PRESENT","value":[
          {"target":{"kind":"APPLICATION","identity":{"modelId":"model:aux","elementId":"aux"},"contractVersion":"aux-v2"},"boundary":"LOCAL","source":{"modelAuthority":"model:app","path":"edges/dependency.cml","sourceRevision":"dependency-r51","line":null},"origin":{"kind":"DECLARED"}},
          {"target":{"kind":"APPLICATION","identity":{"modelId":"external:app","elementId":"aux"},"contractVersion":"external-v9"},"boundary":"EXTERNAL","source":{"modelAuthority":"model:app","path":"edges/dependency.cml","sourceRevision":"dependency-r52","line":null},"origin":{"kind":"DECLARED"}}
        ]},
        "grounding":{"state":"PRESENT","value":[
          {"target":{"kind":"MODEL","identity":{"modelId":"model:facts","elementId":"concept"}},"boundary":"LOCAL",
           "relation":{"state":"ABSENT","reason":"Unsupported","detail":"Relation unsupported."},
           "profile":{"state":"ABSENT","reason":"NotApplicable","detail":"Profile inapplicable."},
           "context":{"state":"PRESENT","value":"É / É | \"context\" \\"},
           "preferredLabel":{"state":"PRESENT","value":{"text":"支払 É","language":null}},
           "source":{"modelAuthority":"model:app","path":"edges/grounding.cml","sourceRevision":"ground-r61","line":30},"origin":{"kind":"DECLARED"}},
          {"target":{"kind":"TERM","identity":{"vocabularyId":"vocab:語彙","termId":"term|É"}},"boundary":"LOCAL",
           "relation":{"state":"PRESENT","value":{"vocabularyId":"vocab:relation","relationId":"relation::1"}},
           "profile":{"state":"PRESENT","value":{"vocabularyId":"vocab:profile","profileId":"profile/1"}},
           "context":{"state":"ABSENT","reason":"NotDeclared","detail":"Context undeclared."},
           "preferredLabel":{"state":"PRESENT","value":{"text":"支払 É","language":"ja"}},
           "source":{"modelAuthority":"model:app","path":"edges/grounding.cml","sourceRevision":"ground-r62","line":31},"origin":{"kind":"DERIVED","ruleId":"ground-rule","inputs":[{"modelAuthority":"foreign:ground","path":"inputs/source.cml","sourceRevision":"input-r69","line":null}]}}
        ]},
        "traceability":{"state":"PRESENT","value":[
          {"role":"SPECIFICATION","target":{"identity":{"modelId":"model:facts","elementId":"spec"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/trace.cml","sourceRevision":"trace-r71","line":40},"origin":{"kind":"DECLARED"}},
          {"role":"EVIDENCE","target":{"identity":{"modelId":"model:facts","elementId":"evidence"},"boundary":"LOCAL"},"source":{"modelAuthority":"model:app","path":"edges/trace.cml","sourceRevision":"trace-r72","line":41},"origin":{"kind":"DECLARED"}}
        ]}
       }]},
      {"source":{"modelAuthority":"model:empty","path":"models/empty.cml","sourceRevision":"header-empty","line":13},"applicationCapabilities":[]},
      {"source":{"modelAuthority":"model:aux","path":"models/aux.cml","sourceRevision":"header-aux","line":null},"applicationCapabilities":[{
        "kind":"APPLICATION","identity":{"modelId":"model:aux","elementId":"aux"},"contractVersion":"aux-v2","name":"Auxiliary",
        "summary":{"state":"PRESENT","value":"Aux summary."},"description":{"state":"PRESENT","value":"Aux description."},
        "source":{"modelAuthority":"model:aux","path":"models/aux.cml","sourceRevision":"definition-aux","line":7},
        "demandReferences":{"state":"PRESENT","value":[]},"discoveryReferences":{"state":"PRESENT","value":[]},
        "componentMappings":{"state":"PRESENT","value":[]},"dependencies":{"state":"PRESENT","value":[]},
        "grounding":{"state":"PRESENT","value":[]},"traceability":{"state":"PRESENT","value":[]}
      }]}
    ]}"""
  val json: Json = JawnParser(allowDuplicateKeys = false).parse(literal).fold(e => throw new IllegalArgumentException(e.toString), identity)
  def projection: ApplicationProjection = CapabilityIngestionFixtures.right(CapabilityApplicationProjectionCodec.decode(literal))
  val appPath: Vector[String] = Vector("documents", "0", "applicationCapabilities", "0")
  def update(value: Json, path: Vector[String])(change: Json => Json): Json = if (path.isEmpty) change(value) else {
    val head = path.head
    value.asObject match {
      case Some(obj) => Json.fromJsonObject(obj.add(head, update(obj(head).get, path.tail)(change)))
      case None => Json.fromValues(value.asArray.get.zipWithIndex.map((x, i) => if (i == head.toInt) update(x, path.tail)(change) else x))
    }
  }
  def changed(path: Vector[String], value: Json): String = update(json, path)(_ => value).noSpaces
  def app(value: ApplicationProjection): ApplicationDefinition = value.documents.head.applicationCapabilities.head
  def withApp(value: ApplicationProjection, definition: ApplicationDefinition): ApplicationProjection =
    value.copy(documents = value.documents.updated(0, value.documents.head.copy(applicationCapabilities = Vector(definition))))
  val scope: Scope = {
    val facts = Vector("goal" -> ModelKind.Goal, "usecase" -> ModelKind.UseCase, "scenario" -> ModelKind.Scenario, "slice" -> ModelKind.UseCaseSlice,
      "component" -> ModelKind.Component, "spec" -> ModelKind.Specification, "evidence" -> ModelKind.Evidence, "concept" -> ModelKind.DomainConcept)
    def source(authority: String): Source = Source(authority, "facts/selected.cml", "fact-r88", None)
    CapabilityIngestionFixtures.scope.copy(
      modelFacts = facts.map((id, kind) => ModelFact(ModelElementId("model:facts", id), kind, source("model:facts"))),
      termFacts = Vector(TermFact(TermId("vocab:語彙", "term|É"), source("vocab:語彙"))),
      relationFacts = Vector(RelationFact(RelationId("vocab:relation", "relation::1"), source("vocab:relation"))),
      profileFacts = Vector(ProfileFact(ProfileId("vocab:profile", "profile/1"), source("vocab:profile"))))
  }
  def expectation: ApplicationExpectation = {
    val p = projection
    ApplicationExpectation(p.producer, p.schemaVersion, p.generatedAbiVersion, p.documents.map(_.source))
  }
  def acquired: ApplicationInput = ApplicationInput.Acquired(literal, expectation)
}
