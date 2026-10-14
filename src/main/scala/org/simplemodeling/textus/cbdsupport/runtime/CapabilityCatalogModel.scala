package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityPackageSource.PackageInput
import CapabilityIngestionModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogModel {
  final case class SourceKey(modelAuthority: String, path: String, sourceRevision: String)
  final case class ComponentContextKey(scopeId: String, component: ComponentCoordinate, packageCoordinate: PackageCoordinate, resourcePath: String, source: SourceKey)
  final case class DeclarationRecordKey(context: ComponentContextKey, declaration: DeclarationKey)
  final case class ApplicationContextKey(document: SourceKey, reference: ApplicationCapabilityReference)
  final case class CatalogSelection(snapshotReference: SnapshotReference, provenance: Provenance)
  final case class PortableProjection(schemaVersion: Int, snapshotReference: SnapshotReference, provenance: Provenance,
    packages: Vector[PackageProjection], application: ApplicationFamily, useCase: UnavailableUpstreamProjection)
  final case class PackageProjection(input: PackageInput.Selected, targetContracts: Vector[TargetContract], resources: Vector[ResourceProjection])
  final case class ResourceProjection(resourcePath: String, projection: Projection, resolvedDefinitions: Vector[CapabilityDefinition], realizations: Vector[ResolvedRealizationFact])
  final case class ResolvedRealizationFact(edge: Realization, target: TargetContract)
  final case class ComponentResourceRecord(key: ComponentContextKey, input: PackageInput.Selected, resource: ResourceProjection)
  final case class CapabilityRealizationRef(owner: DeclarationRecordKey, edge: Realization, target: TargetContract, observations: RuntimeObservations)
  final case class CapabilityRecord(key: DeclarationRecordKey, context: ComponentResourceRecord, declaration: Declaration,
    definition: CapabilityDefinition, realizations: Vector[CapabilityRealizationRef])
  final case class CompatibilityRecord(context: ComponentContextKey, edge: Compatibility)
  final case class ApplicationRecord(key: ApplicationContextKey, documentSource: Source, definition: ApplicationDefinition)
  enum RequiredEndpoint {
    case Resolved(records: Vector[DeclarationRecordKey])
    case Unresolved(reference: CapabilityReference, boundary: ReferenceBoundary)
  }
  final case class ApplicationRequirementPath(owner: ApplicationContextKey, mapping: ComponentMapping, requiredEndpoint: RequiredEndpoint)
  final case class CapabilityDemandTrace(useCase: UnavailableUpstreamProjection, application: ApplicationFamily, applicationPaths: Vector[ApplicationRequirementPath])
  enum TraceabilityOwner {
    case CapabilityDefinition(context: ComponentContextKey, reference: CapabilityReference)
    case Declaration(key: DeclarationRecordKey)
    case Application(key: ApplicationContextKey)
  }
  final case class AttributedTraceability(owner: TraceabilityOwner, edge: Traceability)
  enum AvailabilityObservation { case Unknown(reason: String) }
  enum AuthorizationObservation { case Unknown(reason: String) }
  enum GuardObservation { case Unknown(reason: String) }
  enum ExecutionResultObservation { case Unknown(reason: String) }
  final case class RuntimeObservations(availability: AvailabilityObservation, authorization: AuthorizationObservation,
    guard: GuardObservation, executionResult: ExecutionResultObservation)
  final case class Catalog(projection: PortableProjection, resources: Vector[ComponentResourceRecord], capabilities: Vector[CapabilityRecord],
    applications: Vector[ApplicationRecord], compatibility: Vector[CompatibilityRecord], traceability: Vector[AttributedTraceability], demandTrace: CapabilityDemandTrace)
  final case class NativeRealizationObservation(owner: DeclarationRecordKey, resolved: ComponentCapabilityDiscovery.ResolvedTarget)
  final case class LiveCatalog(snapshot: Snapshot, catalog: Catalog, nativeObservations: Vector[NativeRealizationObservation])
}
