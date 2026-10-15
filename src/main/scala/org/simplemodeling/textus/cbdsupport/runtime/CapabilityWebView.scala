package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityViewModel.*

/*
 * Read-only HTML over admitted immutable values. Reference attributes declare
 * original navigation facts without introducing a receiving endpoint.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityWebView {
  def overview(view: Overview): String =
    "<main><h1>Capability overview</h1>" + _scope(view.scope) +
      "<section><h2>Component contract</h2>" + _collection(view.contracts)(_record) + "</section>" +
      "<section><h2>Component Usage observation</h2>" + _collection(view.usages)(_usage) + "</section>" +
      "<section><h2>UseCase unavailable</h2>" + _unavailable(view.useCase) + "</section></main>"

  def detail(view: Detail): String = {
    val result = view.result.value
    val designation = result.designation match {
      case LookupDesignation.ExactIdentity => "ExactIdentity"
      case LookupDesignation.DiscoveryOnly => "DiscoveryOnly: discovery assistance"
    }
    "<main><h1>Component contract detail</h1>" + _scope(view.result.scope) +
      "<dl>" + _fact("Lookup", result.lookup) + _fact("Designation", designation) + "</dl>" +
      _collection(result.records)(_record) + "</main>"
  }

  def usageTrace(view: UsageTrace): String =
    "<main><h1>Component Usage observation trace</h1>" + _scope(view.result.scope) +
      _collection(view.result.value) { usage =>
        "<article class=\"usage-trace\">" + _usage(usage.usage) +
          _collection(usage.paths) { path =>
            "<article class=\"requirement-path\"><h2>Component contract mapping</h2>" + _mapping(path.path) +
              _collection(path.required) { required =>
                "<article class=\"required\"><h3>Required Component contract</h3>" + _record(required.required) +
                  _search(required.providers) + _collection(required.providers.matches) { provider =>
                    "<section class=\"provider\" data-reference=\"" + _escape(provider.provider.key) + "\">" +
                      "<h3>Provider Component contract</h3><dl>" + _fact("Match basis", provider.basis) + "</dl>" +
                      _record(provider.provider) + "</section>"
                  } + "</article>"
              } + "</article>"
          } + "</article>"
      } + "<section><h2>Demand diagnosis</h2>" + _scope(view.diagnosis.scope) +
      "<dl>" + _fact("Selection", view.diagnosis.value.selection) + "</dl>" +
      _collection(view.diagnosis.value.gaps)(_gap) + "</section></main>"

  def failure(failure: QueryFailure): String = failure match {
    case QueryFailure.Source(diagnostic) =>
      "<section class=\"source-failure\"><h1>Source failure</h1><dl>" +
        _fact("Diagnostic kind", diagnostic.kind) + _fact("Detail", diagnostic.detail) +
        _fact("Location", diagnostic.location) + _fact("Scope", diagnostic.scopeId) +
        _fact("Component", diagnostic.component) + _fact("Source", diagnostic.source) + "</dl></section>"
    case QueryFailure.UnavailableProjection(value) =>
      "<section class=\"unavailable-projection\"><h1>UnavailableProjection</h1>" + _unavailable(value) + "</section>"
  }

  private def _record(record: CapabilityRecord): String = {
    val definition = record.definition
    "<article class=\"capability-record\">" + _reference("Component contract", record.key, record.key.context) +
      _context(record.key.context) + "<dl>" + _fact("Direction", record.declaration.direction) +
      _fact("Qualified identity", definition.identity) + _fact("Contract version", definition.contractVersion) +
      _fact("Name", definition.name) + _fact("Summary Presence", definition.summary) +
      _fact("Description Presence", definition.description) + _fact("Definition Source", definition.source) +
      _fact("Declaration Source", record.declaration.source) + _fact("Package input", record.context.input) +
      _fact("Producer", record.context.resource.projection.producer) +
      _fact("Schema version", record.context.resource.projection.schemaVersion) +
      _fact("Generated ABI", record.context.resource.projection.generatedAbiVersion) +
      _fact("Projection Source", record.context.resource.projection.source) +
      _fact("Discovery references Presence", definition.discoveryReferences) + _fact("Grounding Presence", definition.grounding) +
      _fact("Realization Presence", record.declaration.realizations) + "</dl>" +
      _traceability("Definition", TraceabilityOwner.CapabilityDefinition(record.key.context, record.declaration.capability), definition.traceability) +
      _traceability("Declaration", TraceabilityOwner.Declaration(record.key), record.declaration.traceability) +
      "<details><summary>Provider-owned realizations</summary>" + _collection(record.realizations)(_realization) + "</details></article>"
  }

  private def _usage(record: ComponentUsageCapabilityRecord): String = {
    val definition = record.definition
    "<article class=\"usage-record\">" + _reference("Component Usage observation", record.key.reference, record.key) +
      "<dl>" + _fact("Owner context", record.key) + _fact("Scope", record.key.scopeId) +
      _fact("Component", record.key.component) + _fact("Package", record.key.packageCoordinate) +
      _fact("Resource path", record.key.resourcePath) + _fact("Package input", record.input) +
      _fact("Document Source", record.documentSource) + _fact("Qualified identity", definition.identity) +
      _fact("Contract version", definition.contractVersion) + _fact("Name", definition.name) +
      _fact("Summary Presence", definition.summary) + _fact("Description Presence", definition.description) +
      _fact("Definition Source", definition.source) + _fact("Demand references Presence", definition.demandReferences) +
      _fact("Discovery references Presence", definition.discoveryReferences) + _fact("Mappings Presence", definition.componentMappings) +
      _fact("Dependencies Presence", definition.dependencies) + _fact("Grounding Presence", definition.grounding) + "</dl>" +
      _traceability("Component Usage", TraceabilityOwner.ComponentUsage(record.key), definition.traceability) + "</article>"
  }

  private def _realization(value: CapabilityRealizationRef): String = {
    val edge = value.edge
    val observations = value.observations
    "<article class=\"realization\" data-owner=\"" + _escape(value.owner) + "\" data-target=\"" + _escape(value.target) + "\">" +
      "<h4>Realization</h4>" + _reference("Target", edge.target, value.owner) + "<dl>" +
      _fact("Owner", value.owner) + _fact("Declaration key", edge.declarationKey) + _fact("Kind", edge.targetKind) +
      _fact("Target Component", edge.targetComponent) + _fact("Target contract version", edge.targetContractVersion) +
      _fact("Generated ABI", edge.generatedAbiVersion) + _fact("Source", edge.source) + _fact("Origin", edge.origin) +
      _fact("Resolved target contract", value.target) + _fact("Target Source", value.target.source) +
      _fact("Availability", observations.availability match { case AvailabilityObservation.Unknown(reason) => "Unknown: " + reason }) +
      _fact("Authorization", observations.authorization match { case AuthorizationObservation.Unknown(reason) => "Unknown: " + reason }) +
      _fact("Guard", observations.guard match { case GuardObservation.Unknown(reason) => "Unknown: " + reason }) +
      _fact("Execution result", observations.executionResult match { case ExecutionResultObservation.Unknown(reason) => "Unknown: " + reason }) +
      "</dl></article>"
  }

  private def _traceability(label: String, owner: TraceabilityOwner, value: Presence[Vector[Traceability]]): String =
    "<details><summary>" + _escape(label) + " Specification / Evidence</summary><p>Attributed facts</p><dl>" +
      _fact("Owner", owner) + _fact("Original Presence", value) + "</dl>" + (value match {
        case Present(edges) => _collection(edges) { edge =>
          "<article class=\"traceability\">" + _reference(edge.role.toString, edge.target, owner) +
            "<dl>" + _fact("Role", edge.role) + _fact("Source", edge.source) + _fact("Origin", edge.origin) + "</dl></article>"
        }
        case _: Absent => ""
      }) + "</details>"

  private def _mapping(path: ComponentUsageRequirementPath): String =
    _reference("Mapping target", path.mapping.target, path.owner) + "<dl>" + _fact("Usage owner", path.owner) +
      _fact("Original mapping", path.mapping) + _fact("Boundary", path.mapping.boundary) +
      _fact("Source", path.mapping.source) + _fact("Origin", path.mapping.origin) +
      _fact("Required endpoint", path.requiredEndpoint) + "</dl>"

  private def _search(search: ProviderSearch): String =
    "<details><summary>Provider search witnesses</summary><dl>" + _fact("Required reference", search.required) +
      "</dl><h4>Candidates</h4>" + _collection(search.candidates)(r => _reference("Candidate", r.key, r.key.context)) +
      "<h4>Incompatible providers</h4>" + _collection(search.incompatible)(r => _reference("Incompatible provider", r.key, r.key.context)) +
      "<h4>Considered compatibility</h4>" + _collection(search.consideredCompatibility) { record =>
        "<dl>" + _fact("Context", record.context) + _fact("Directed compatibility", record.edge) + "</dl>"
      } + "</details>"

  private def _gap(gap: CapabilityGap): String = {
    val body = gap match {
      case CapabilityGap.MissingComponentUsageCapabilityProjection(scope, reference, reason, families) =>
        _scope(scope) + _reference("Missing Component Usage observation", reference, scope) +
          "<dl>" + _fact("Reason", reason) + "</dl>" + _collection(families) { family =>
            "<article><dl>" + _fact("Package input", family.input) + _fact("Resource path", family.resourcePath) + "</dl>" +
              (family.family match {
                case ComponentUsageFamily.Unavailable(value) => _unavailable(value)
                case ComponentUsageFamily.Available(projection, external) =>
                  "<dl>" + _fact("Available Component Usage projection", projection) + _fact("Unresolved external references", external) + "</dl>"
              }) + "</article>"
          }
      case CapabilityGap.MissingRequiredComponentCapability(scope, usage, path) =>
        _scope(scope) + _reference("Usage owner", usage.key.reference, usage.key) + _mapping(path)
      case CapabilityGap.NoProvider(scope, path, search) => _scope(scope) + _demand_path(path) + _search(search)
      case CapabilityGap.IncompatibleProviderVersion(scope, path, search) => _scope(scope) + _demand_path(path) + _search(search)
      case CapabilityGap.MissingRealization(scope, path, original) =>
        _scope(scope) + _demand_path(path) + "<dl>" + _fact("Original Presence", original) + "</dl>"
      case CapabilityGap.MissingSpecification(scope, path, coverage) => _scope(scope) + _demand_path(path) + _coverage(coverage)
      case CapabilityGap.MissingEvidence(scope, path, coverage) => _scope(scope) + _demand_path(path) + _coverage(coverage)
    }
    "<article class=\"gap\" data-gap=\"" + _escape(gap.productPrefix) + "\"><h3>" + _escape(gap.productPrefix) + "</h3>" + body + "</article>"
  }

  private def _demand_path(path: DemandPath): String =
    path.usage.map(u => _reference("Usage owner", u.key.reference, u.key)).getOrElse("") +
      path.mapping.map(_mapping).getOrElse("") + _reference("Required owner", path.required.key, path.required.key.context) +
      path.provider.map(p => _reference("Provider owner", p.provider.key, p.provider.key.context) +
        "<dl>" + _fact("Match basis", p.basis) + "</dl>").getOrElse("")

  private def _coverage(coverage: RoleCoverage): String =
    "<details><summary>Role coverage</summary><dl>" + _fact("Boundary", coverage.boundary) + "</dl>" +
      _collection(coverage.owners)(o => _traceability("Coverage owner", o.owner, o.traceability)) +
      "<dl>" + _fact("Specification attribution", coverage.specification) + _fact("Evidence attribution", coverage.evidence) + "</dl></details>"

  private def _context(context: ComponentContextKey): String =
    "<dl>" + _fact("Scope", context.scopeId) + _fact("Component", context.component) +
      _fact("Package", context.packageCoordinate) + _fact("Resource path", context.resourcePath) + _fact("Source", context.source) + "</dl>"

  private def _scope(scope: QueryScope): String =
    "<details><summary>Selected query scope</summary><dl>" + _fact("Snapshot reference", scope.snapshotReference) +
      _fact("Scope", scope.provenance.scope.scopeId) + _fact("Source", scope.provenance.scope.source) +
      _fact("Original provenance", scope.provenance) + "</dl></details>"

  private def _unavailable(value: UnavailableUpstreamProjection): String =
    "<dl>" + _fact("Unavailable family", value.family) + _fact("Reason", value.reason) + _fact("Detail", value.detail) + "</dl>"

  private def _reference(label: String, value: Any, context: Any): String =
    "<span class=\"reference\" data-reference=\"" + _escape(value) + "\" data-context=\"" + _escape(context) + "\">" +
      _escape(label) + ": " + _escape(value) + "</span>"

  private def _collection[A](values: Vector[A])(render: A => String): String =
    if (values.isEmpty) "<p class=\"empty\">Successful empty collection in selected scope.</p>"
    else "<ul>" + values.map(value => "<li>" + render(value) + "</li>").mkString + "</ul>"

  private def _fact(label: String, value: Any): String = "<dt>" + _escape(label) + "</dt><dd>" + _escape(value) + "</dd>"

  private def _escape(value: Any): String = value.toString.flatMap {
    case '&' => "&amp;"
    case '<' => "&lt;"
    case '>' => "&gt;"
    case '"' => "&quot;"
    case '\'' => "&#39;"
    case character => character.toString
  }
}
