package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityViewModel.*
import CapabilityComponentUsageJsonEncodingPart.{_string, _vector, _option, _model_id, _source, _presence, _traceability, _mapping, _origin}
import CapabilityCatalogJsonEncodingPart.{_component_ref, _coordinate, _package_coordinate, _input, _definition, _target_contract, _capability_reference, _usage_reference, _key, _realization, _unavailable}

/*
 * Explicit wire encodings of original admitted values, without presentation identities.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityReadJsonPart {
  def scope(value: QueryScope): Json = CapabilityCatalogJsonEncodingPart.encodeSelection(CatalogSelection(value.snapshotReference, value.provenance))
  def envelope(operation: String, status: String, selected: Option[QueryScope], value: Json, failure: Json): Json =
    Json.obj("schemaVersion" -> Json.fromInt(1), "operation" -> _string(operation), "status" -> _string(status),
      "scope" -> _option(selected)(scope), "value" -> value, "failure" -> failure)
  def failure(value: QueryFailure): Json = value match {
    case QueryFailure.Source(d) => Json.obj("kind" -> _string("SOURCE"), "diagnostic" -> Json.obj(
      "kind" -> _string(d.kind.toString), "detail" -> _string(d.detail), "location" -> _string(d.location),
      "scopeId" -> _option(d.scopeId)(_string), "component" -> _option(d.component)(_component_ref), "source" -> _option(d.source)(_source)))
    case QueryFailure.UnavailableProjection(v) => Json.obj("kind" -> _string("UNAVAILABLE_PROJECTION"), "value" -> _unavailable(v))
  }
  def overview(value: Overview): Json = Json.obj("contracts" -> _vector(value.contracts)(record),
    "usages" -> _vector(value.usages)(_usage), "useCase" -> _unavailable(value.useCase))
  def detail(value: Detail): Json = {
    val result = value.result.value
    Json.obj("lookup" -> _lookup(result.lookup), "designation" -> _string(result.designation match {
      case LookupDesignation.ExactIdentity => "EXACT_IDENTITY"
      case LookupDesignation.DiscoveryOnly => "DISCOVERY_ONLY"
    }), "records" -> _vector(result.records)(record))
  }
  def providers(value: ProviderSearch): Json = Json.obj("required" -> _capability_reference(value.required),
    "candidates" -> _vector(value.candidates)(record), "matches" -> _vector(value.matches)(_match),
    "incompatible" -> _vector(value.incompatible)(record), "consideredCompatibility" -> _vector(value.consideredCompatibility)(_compatibility))
  def requirements(value: RequirementSearch): Json = Json.obj("component" -> _coordinate(value.component), "records" -> _vector(value.records)(record))
  def usageTrace(value: UsageTrace): Json = Json.obj("result" -> _result(value.result)(v => _vector(v)(_trace)), "diagnosis" -> _result(value.diagnosis)(diagnosis))
  def diagnosis(value: DemandDiagnosis): Json = Json.obj("selection" -> _selection(value.selection),
    "usages" -> _vector(value.usages)(_trace), "required" -> _vector(value.required)(_required), "gaps" -> _vector(value.gaps)(_gap))

  def recordKey(value: DeclarationRecordKey): Json = Json.obj("context" -> _context(value.context), "declaration" -> _key(value.declaration))
  def record(value: CapabilityRecord): Json = Json.obj("key" -> recordKey(value.key),
    "context" -> Json.obj("key" -> _context(value.context.key), "input" -> _input(value.context.input),
      "resource" -> _admitted(CapabilityCatalogJsonEncodingPart._package(
        PackageProjection(value.context.input, Vector.empty, Vector(value.context.resource)), "/context"))
        .hcursor.downField("resources").focus.get.asArray.get.head),
    "declaration" -> _declaration(value.declaration), "definition" -> _definition(value.definition),
    "realizations" -> _vector(value.realizations)(r => Json.obj("owner" -> recordKey(r.owner), "edge" -> _realization(r.edge),
      "target" -> _target_contract(r.target), "observations" -> _observations(r.observations))))

  private def _admitted(value: Either[Diagnostic, Json]): Json = value.fold(
    d => throw new IllegalStateException("Admitted catalog encoding failed at " + d.location + ": " + d.detail), identity)
  private def _result[A](value: QueryResult[A])(encode: A => Json): Json = Json.obj("scope" -> scope(value.scope), "value" -> encode(value.value))
  private def _lookup(value: CapabilityLookup): Json = value match {
    case CapabilityLookup.Exact(r) => Json.obj("kind" -> _string("EXACT"), "reference" -> _capability_reference(r))
    case CapabilityLookup.Search(t) => Json.obj("kind" -> _string("SEARCH"), "text" -> _string(t))
  }
  private def _source_key(value: SourceKey): Json = Json.obj("modelAuthority" -> _string(value.modelAuthority), "path" -> _string(value.path), "sourceRevision" -> _string(value.sourceRevision))
  private def _context(value: ComponentContextKey): Json = Json.obj("scopeId" -> _string(value.scopeId), "component" -> _coordinate(value.component),
    "packageCoordinate" -> _package_coordinate(value.packageCoordinate), "resourcePath" -> _string(value.resourcePath), "source" -> _source_key(value.source))
  private def _usage_key(value: ComponentUsageCapabilityContextKey): Json = Json.obj("scopeId" -> _string(value.scopeId), "component" -> _coordinate(value.component),
    "packageCoordinate" -> _package_coordinate(value.packageCoordinate), "resourcePath" -> _string(value.resourcePath),
    "document" -> _source_key(value.document), "reference" -> _usage_reference(value.reference))
  private def _declaration(value: Declaration): Json = Json.obj("component" -> _component_ref(value.component), "direction" -> _string(value.direction.tag),
    "capability" -> _capability_reference(value.capability), "source" -> _source(value.source),
    "realizations" -> _presence(value.realizations)(v => _vector(v)(_realization)), "traceability" -> _presence(value.traceability)(v => _vector(v)(_traceability)))
  private def _observations(value: RuntimeObservations): Json = {
    def _unknown_(reason: String): Json = Json.obj("kind" -> _string("UNKNOWN"), "reason" -> _string(reason))
    Json.obj("availability" -> (value.availability match { case AvailabilityObservation.Unknown(r) => _unknown_(r) }),
      "authorization" -> (value.authorization match { case AuthorizationObservation.Unknown(r) => _unknown_(r) }),
      "guard" -> (value.guard match { case GuardObservation.Unknown(r) => _unknown_(r) }),
      "executionResult" -> (value.executionResult match { case ExecutionResultObservation.Unknown(r) => _unknown_(r) }))
  }
  private def _compatibility(value: CompatibilityRecord): Json = Json.obj("context" -> _context(value.context), "edge" -> Json.obj(
    "identity" -> _model_id(value.edge.identity), "requiredVersion" -> _string(value.edge.requiredVersion),
    "providedVersion" -> _string(value.edge.providedVersion), "source" -> _source(value.edge.source), "origin" -> _origin(value.edge.origin)))
  private def _match(value: ProviderMatch): Json = Json.obj("provider" -> record(value.provider), "basis" -> (value.basis match {
    case MatchBasis.SameVersion => Json.obj("kind" -> _string("SAME_VERSION"))
    case MatchBasis.ExplicitCompatibility(records) => Json.obj("kind" -> _string("EXPLICIT_COMPATIBILITY"), "records" -> _vector(records)(_compatibility))
  }))
  private def _usage(value: ComponentUsageCapabilityRecord): Json = Json.obj("key" -> _usage_key(value.key), "input" -> _input(value.input),
    "documentSource" -> _source(value.documentSource), "definition" -> CapabilityComponentUsageJsonEncodingPart._definition(value.definition))
  private def _path(value: ComponentUsageRequirementPath): Json = Json.obj("owner" -> _usage_key(value.owner), "mapping" -> _mapping(value.mapping),
    "requiredEndpoint" -> (value.requiredEndpoint match {
      case RequiredEndpoint.Resolved(records) => Json.obj("kind" -> _string("RESOLVED"), "records" -> _vector(records)(recordKey))
      case RequiredEndpoint.Unresolved(ref, boundary) => Json.obj("kind" -> _string("UNRESOLVED"), "reference" -> _capability_reference(ref), "boundary" -> _string(boundary.tag))
    }))
  private def _required(value: RequiredProviderTrace): Json = Json.obj("required" -> record(value.required), "providers" -> providers(value.providers))
  private def _trace(value: ComponentUsageTrace): Json = Json.obj("usage" -> _usage(value.usage), "paths" -> _vector(value.paths)(p =>
    Json.obj("path" -> _path(p.path), "required" -> _vector(p.required)(_required))))
  private def _selection(value: DemandSelection): Json = value match {
    case DemandSelection.ComponentUsage(r) => Json.obj("kind" -> _string("COMPONENT_USAGE"), "reference" -> _usage_reference(r))
    case DemandSelection.Required(k) => Json.obj("kind" -> _string("REQUIRED"), "key" -> recordKey(k))
    case DemandSelection.UseCase(r) => Json.obj("kind" -> _string("USE_CASE"), "reference" -> _model_id(r))
  }
  private def _demand_path(value: DemandPath): Json = Json.obj("usage" -> _option(value.usage)(_usage), "mapping" -> _option(value.mapping)(_path),
    "required" -> record(value.required), "provider" -> _option(value.provider)(_match))
  private def _owner(value: TraceabilityOwner): Json = value match {
    case TraceabilityOwner.CapabilityDefinition(context, reference) => Json.obj("kind" -> _string("CAPABILITY_DEFINITION"), "context" -> _context(context), "reference" -> _capability_reference(reference))
    case TraceabilityOwner.Declaration(key) => Json.obj("kind" -> _string("DECLARATION"), "key" -> recordKey(key))
    case TraceabilityOwner.ComponentUsage(key) => Json.obj("kind" -> _string("COMPONENT_USAGE"), "key" -> _usage_key(key))
  }
  private def _attributed(value: AttributedTraceability): Json = Json.obj("owner" -> _owner(value.owner), "edge" -> _traceability(value.edge))
  private def _coverage(value: RoleCoverage): Json = Json.obj("boundary" -> _string(value.boundary match {
    case CoverageBoundary.RequiredPathOnly => "REQUIRED_PATH_ONLY"
    case CoverageBoundary.ProviderPath => "PROVIDER_PATH"
  }), "owners" -> _vector(value.owners)(o => Json.obj("owner" -> _owner(o.owner), "traceability" -> _presence(o.traceability)(v => _vector(v)(_traceability)))),
    "specification" -> _vector(value.specification)(_attributed), "evidence" -> _vector(value.evidence)(_attributed))
  private def _gap(value: CapabilityGap): Json = {
    def _base_(tag: String, selected: QueryScope, fields: (String, Json)*): Json =
      Json.obj((Vector("kind" -> _string(tag), "scope" -> scope(selected)) ++ fields)* )
    value match {
      case CapabilityGap.MissingComponentUsageCapabilityProjection(s, r, reason, families) => _base_("MISSING_COMPONENT_USAGE_CAPABILITY_PROJECTION", s,
        "reference" -> _usage_reference(r), "reason" -> _string(reason.tag), "families" -> _vector(families)(f => _admitted(CapabilityCatalogJsonEncodingPart._usage_package(f, "/families"))))
      case CapabilityGap.MissingRequiredComponentCapability(s, u, p) => _base_("MISSING_REQUIRED_COMPONENT_CAPABILITY", s, "usage" -> _usage(u), "path" -> _path(p))
      case CapabilityGap.NoProvider(s, p, search) => _base_("NO_PROVIDER", s, "path" -> _demand_path(p), "search" -> providers(search))
      case CapabilityGap.IncompatibleProviderVersion(s, p, search) => _base_("INCOMPATIBLE_PROVIDER_VERSION", s, "path" -> _demand_path(p), "search" -> providers(search))
      case CapabilityGap.MissingRealization(s, p, original) => _base_("MISSING_REALIZATION", s, "path" -> _demand_path(p), "original" -> _presence(original)(v => _vector(v)(_realization)))
      case CapabilityGap.MissingSpecification(s, p, coverage) => _base_("MISSING_SPECIFICATION", s, "path" -> _demand_path(p), "coverage" -> _coverage(coverage))
      case CapabilityGap.MissingEvidence(s, p, coverage) => _base_("MISSING_EVIDENCE", s, "path" -> _demand_path(p), "coverage" -> _coverage(coverage))
    }
  }
}
