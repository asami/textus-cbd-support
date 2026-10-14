package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityQueryModel {
  final case class QueryScope(snapshotReference: SnapshotReference, provenance: Provenance)
  final case class QueryResult[+A](scope: QueryScope, value: A)
  enum QueryFailure {
    case Source(diagnostic: Diagnostic)
    case UnavailableProjection(value: UnavailableUpstreamProjection)
  }
  enum CapabilityLookup {
    case Exact(reference: CapabilityReference)
    case Search(text: String)
  }
  enum LookupDesignation { case ExactIdentity, DiscoveryOnly }
  final case class CapabilityLookupResult(lookup: CapabilityLookup, designation: LookupDesignation, records: Vector[CapabilityRecord])
  enum MatchBasis {
    case SameVersion
    case ExplicitCompatibility(records: Vector[CompatibilityRecord])
  }
  final case class ProviderMatch(provider: CapabilityRecord, basis: MatchBasis)
  final case class ProviderSearch(required: CapabilityReference, candidates: Vector[CapabilityRecord], matches: Vector[ProviderMatch],
    incompatible: Vector[CapabilityRecord], consideredCompatibility: Vector[CompatibilityRecord])
  final case class RequirementSearch(component: ComponentCoordinate, records: Vector[CapabilityRecord])
  final case class RequiredProviderTrace(required: CapabilityRecord, providers: ProviderSearch)
  final case class RequirementPathTrace(path: ApplicationRequirementPath, required: Vector[RequiredProviderTrace])
  final case class ApplicationTrace(application: ApplicationRecord, paths: Vector[RequirementPathTrace])
  enum DemandSelection {
    case Application(reference: ApplicationCapabilityReference)
    case Required(key: DeclarationRecordKey)
    case UseCase(reference: ModelElementId)
  }
  final case class DemandPath(application: Option[ApplicationRecord], mapping: Option[ApplicationRequirementPath],
    required: CapabilityRecord, provider: Option[ProviderMatch])
  final case class OwnerTraceability(owner: TraceabilityOwner, traceability: Presence[Vector[Traceability]])
  enum CoverageBoundary { case RequiredPathOnly, ProviderPath }
  final case class RoleCoverage(boundary: CoverageBoundary, owners: Vector[OwnerTraceability],
    specification: Vector[AttributedTraceability], evidence: Vector[AttributedTraceability])
  enum MissingApplicationReason(val tag: String) {
    case AbsentSelectedReference extends MissingApplicationReason("absent-selected-reference")
    case ProjectionNotSupplied extends MissingApplicationReason("projection-not-supplied")
  }
  enum CapabilityGap {
    case MissingApplicationCapabilityProjection(scope: QueryScope, reference: ApplicationCapabilityReference, reason: MissingApplicationReason, family: ApplicationFamily)
    case MissingRequiredComponentCapability(scope: QueryScope, application: ApplicationRecord, path: ApplicationRequirementPath)
    case NoProvider(scope: QueryScope, path: DemandPath, search: ProviderSearch)
    case IncompatibleProviderVersion(scope: QueryScope, path: DemandPath, search: ProviderSearch)
    case MissingRealization(scope: QueryScope, path: DemandPath, original: Presence[Vector[Realization]])
    case MissingSpecification(scope: QueryScope, path: DemandPath, coverage: RoleCoverage)
    case MissingEvidence(scope: QueryScope, path: DemandPath, coverage: RoleCoverage)
  }
  final case class DemandDiagnosis(selection: DemandSelection, applications: Vector[ApplicationTrace],
    required: Vector[RequiredProviderTrace], gaps: Vector[CapabilityGap])
}
