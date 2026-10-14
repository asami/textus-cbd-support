package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.Producer
import CapabilityIngestionModel.*

/*
 * Nullable supplied branches are represented as JSON null; the single structural
 * decoder owns their rejection, including caller-created invalid carriers.
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationJsonEncodingPart {
  def _nullable[A](value: A)(encode: A => Json): Json = if (value == null) Json.Null else encode(value)
  def _string(value: String): Json = _nullable(value)(Json.fromString)
  def _vector[A](values: Vector[A])(encode: A => Json): Json = _nullable(values)(xs => Json.fromValues(xs.map(x => _nullable(x)(encode))))
  def _option[A](value: Option[A])(encode: A => Json): Json = _nullable(value)(_.map(x => _nullable(x)(encode)).getOrElse(Json.Null))
  def _producer(value: Producer): Json = _nullable(value)(x => Json.obj("id" -> _string(x.id), "version" -> _string(x.version)))
  def _source(value: Source): Json = _nullable(value)(x => if (x.line == null) Json.Null else Json.obj("modelAuthority" -> _string(x.modelAuthority), "path" -> _string(x.path), "sourceRevision" -> _string(x.sourceRevision), "line" -> _option(x.line)(Json.fromInt)))
  def _model_id(value: ModelElementId): Json = _nullable(value)(x => Json.obj("modelId" -> _string(x.modelId), "elementId" -> _string(x.elementId)))
  def _term(value: TermId): Json = _nullable(value)(x => Json.obj("vocabularyId" -> _string(x.vocabularyId), "termId" -> _string(x.termId)))
  def _relation(value: RelationId): Json = _nullable(value)(x => Json.obj("vocabularyId" -> _string(x.vocabularyId), "relationId" -> _string(x.relationId)))
  def _profile(value: ProfileId): Json = _nullable(value)(x => Json.obj("vocabularyId" -> _string(x.vocabularyId), "profileId" -> _string(x.profileId)))
  def _presence[A](value: Presence[A])(encode: A => Json): Json = _nullable(value) {
    case Present(x) => Json.obj("state" -> Json.fromString("PRESENT"), "value" -> _nullable(x)(encode))
    case Absent(reason, detail) => Json.obj("state" -> Json.fromString("ABSENT"), "reason" -> _nullable(reason)(x => Json.fromString(x.toString)), "detail" -> _string(detail))
  }
  def _origin(value: Origin): Json = _nullable(value) {
    case Declared => Json.obj("kind" -> Json.fromString("DECLARED"))
    case Derived(rule, inputs) => Json.obj("kind" -> Json.fromString("DERIVED"), "ruleId" -> _string(rule), "inputs" -> _vector(inputs)(_source))
  }
  def _boundary(value: ReferenceBoundary): Json = _nullable(value)(x => Json.fromString(x.tag))
  def _model_reference(value: ModelReference): Json = _nullable(value)(x => Json.obj("identity" -> _model_id(x.identity), "boundary" -> _boundary(x.boundary)))
  def _semantic_target(value: SemanticTarget): Json = _nullable(value) {
    case ModelTarget(x) => Json.obj("kind" -> Json.fromString("MODEL"), "identity" -> _model_id(x))
    case TermTarget(x) => Json.obj("kind" -> Json.fromString("TERM"), "identity" -> _term(x))
  }
  def _label(value: LocalizedLabel): Json = _nullable(value)(x => if (x.language == null || x.language.exists(_ == null)) Json.Null else Json.obj("text" -> _string(x.text), "language" -> _option(x.language)(_string)))
  def _model_link(value: ModelLink): Json = _nullable(value)(x => Json.obj("role" -> _nullable(x.role)(v => Json.fromString(v.tag)), "target" -> _model_reference(x.target), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  def _traceability(value: Traceability): Json = _nullable(value)(x => Json.obj("role" -> _nullable(x.role)(v => Json.fromString(v.tag)), "target" -> _model_reference(x.target), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  def _grounding(value: Grounding): Json = _nullable(value)(x => Json.obj("target" -> _semantic_target(x.target), "boundary" -> _boundary(x.boundary),
    "relation" -> _presence(x.relation)(_relation), "profile" -> _presence(x.profile)(_profile), "context" -> _presence(x.context)(_string),
    "preferredLabel" -> _presence(x.preferredLabel)(_label), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  def _mapping(value: ComponentMapping): Json = _nullable(value)(x => Json.obj(
    "target" -> _nullable(x.target)(v => Json.obj("kind" -> Json.fromString("COMPONENT"), "identity" -> _model_id(v.identity), "contractVersion" -> _string(v.contractVersion))),
    "boundary" -> _boundary(x.boundary), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  def _dependency(value: ApplicationDependency): Json = _nullable(value)(x => Json.obj(
    "target" -> _nullable(x.target)(v => Json.obj("kind" -> Json.fromString("APPLICATION"), "identity" -> _model_id(v.identity), "contractVersion" -> _string(v.contractVersion))),
    "boundary" -> _boundary(x.boundary), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  def _definition(value: ApplicationDefinition): Json = _nullable(value)(x => Json.obj(
    "kind" -> Json.fromString("APPLICATION"), "identity" -> _model_id(x.identity), "contractVersion" -> _string(x.contractVersion), "name" -> _string(x.name),
    "summary" -> _presence(x.summary)(_string), "description" -> _presence(x.description)(_string), "source" -> _source(x.source),
    "demandReferences" -> _presence(x.demandReferences)(xs => _vector(xs)(_model_link)),
    "discoveryReferences" -> _presence(x.discoveryReferences)(xs => _vector(xs)(_model_link)),
    "componentMappings" -> _presence(x.componentMappings)(xs => _vector(xs)(_mapping)),
    "dependencies" -> _presence(x.dependencies)(xs => _vector(xs)(_dependency)),
    "grounding" -> _presence(x.grounding)(xs => _vector(xs)(_grounding)), "traceability" -> _presence(x.traceability)(xs => _vector(xs)(_traceability))))
  def _encode(value: ApplicationProjection): Json = _nullable(value)(x => Json.obj(
    "schemaVersion" -> Json.fromInt(x.schemaVersion), "generatedAbiVersion" -> _string(x.generatedAbiVersion), "producer" -> _producer(x.producer),
    "documents" -> _vector(x.documents)(doc => Json.obj("source" -> _source(doc.source), "applicationCapabilities" -> _vector(doc.applicationCapabilities)(_definition)))))
}
