package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.*
import org.goldenport.cncf.component.ComponentId
import ComponentCapabilityAdmissionScope.{Diagnostic, Scope}
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.{Producer, CapabilityDefinition, CapabilityReference, ComponentCoordinate, PackageCoordinate}
import ComponentCapabilityPackageSource.PackageInput
import CapabilityComponentUsageJsonPart.{_malformed, _nonnull}

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityIngestionModel {
  final case class AttemptReference private (value: InternalModelRecordReference)
  object AttemptReference {
    def from(value: InternalModelRecordReference): Either[Diagnostic, AttemptReference] =
      _reference(value, "/attempt/reference").map(AttemptReference(_))
  }
  final case class SnapshotReference private (value: InternalModelRecordReference)
  object SnapshotReference {
    def from(value: InternalModelRecordReference): Either[Diagnostic, SnapshotReference] =
      _reference(value, "/snapshot/reference").map(SnapshotReference(_))
  }
  private def _reference(value: InternalModelRecordReference, path: String): Either[Diagnostic, InternalModelRecordReference] = for {
    ref <- _nonnull(value, path)
    _ <- InternalModelRecordId.from(ref.recordId.value).left.map(detail => _malformed(path + "/recordId", detail))
    _ <- InternalModelRecordRevision.from(ref.recordRevision.value).left.map(detail => _malformed(path + "/recordRevision", detail))
  } yield ref

  final case class ComponentUsageProjection(schemaVersion: Int, generatedAbiVersion: String, producer: Producer,
    component: ComponentCoordinate, packageCoordinate: PackageCoordinate, documents: Vector[ComponentUsageDocument])
  final case class ComponentUsageDocument(source: Source, componentUsageCapabilities: Vector[ComponentUsageDefinition])
  final case class ComponentUsageDefinition(
    component: ComponentId, identity: ModelElementId, contractVersion: String, name: String,
    summary: Presence[String], description: Presence[String], source: Source,
    demandReferences: Presence[Vector[ModelLink]], discoveryReferences: Presence[Vector[ModelLink]],
    componentMappings: Presence[Vector[ComponentUsageCapabilityMapping]], dependencies: Presence[Vector[ComponentUsageCapabilityDependency]],
    grounding: Presence[Vector[Grounding]], traceability: Presence[Vector[Traceability]]
  ) { val kind: String = "COMPONENT_USAGE" }
  final case class ComponentUsageCapabilityReference(identity: ModelElementId, contractVersion: String) { val kind: String = "COMPONENT_USAGE" }
  final case class ComponentUsageCapabilityMapping(target: CapabilityReference, boundary: ReferenceBoundary, source: Source, origin: Origin)
  final case class ComponentUsageCapabilityDependency(target: ComponentUsageCapabilityReference, boundary: ReferenceBoundary, source: Source, origin: Origin)

  final case class ResourceExpectation(resourcePath: String, producer: Producer, schemaVersion: Int, generatedAbiVersion: String, source: Source)
  final case class PackageExpectation(input: PackageInput.Selected, resources: Vector[ResourceExpectation], usage: Option[ComponentUsageExpectation])
  final case class PackageSelection(expected: PackageExpectation, provider: Option[ComponentCapabilityMetadataProvider])
  final case class ComponentUsageExpectation(producer: Producer, schemaVersion: Int, generatedAbiVersion: String, documents: Vector[Source])
  enum ProjectionFamily { case ComponentUsage, UseCase }
  final case class UnavailableUpstreamProjection(family: ProjectionFamily, reason: String, detail: String)
  final case class Selection(scope: Scope, packages: Vector[PackageSelection], useCase: UnavailableUpstreamProjection)
  final case class Provenance(scope: Scope, packages: Vector[PackageExpectation], useCase: UnavailableUpstreamProjection)
  enum ExternalTarget {
    case Model(identity: ModelElementId)
    case Term(identity: TermId)
    case Relation(identity: RelationId)
    case Profile(identity: ProfileId)
    case ComponentCapability(reference: CapabilityReference)
    case ComponentUsageCapability(reference: ComponentUsageCapabilityReference)
  }
  final case class ExternalReference(location: String, target: ExternalTarget, source: Source, origin: Origin)
  enum ComponentUsageFamily {
    case Available(projection: ComponentUsageProjection, unresolvedExternal: Vector[ExternalReference])
    case Unavailable(value: UnavailableUpstreamProjection)
  }
  final case class ComponentUsagePackage(input: PackageInput.Selected, resourcePath: String, family: ComponentUsageFamily)
  enum ComponentPackage {
    case Native(discovered: ComponentCapabilityDiscovery.Discovered)
    case MetadataOnly(acquired: ComponentCapabilityPackageLoader.Acquired)
  }
  final class Candidate private[CapabilityIngestionModel] (
    val provenance: Provenance, val componentPackages: Vector[ComponentPackage],
    val usage: Vector[ComponentUsagePackage], val useCase: UnavailableUpstreamProjection
  ) {
    val components: Vector[ComponentCapabilityDiscovery.Discovered] = componentPackages.collect { case ComponentPackage.Native(value) => value }
  }
  final class Snapshot private[CapabilityIngestionModel] (val reference: SnapshotReference, val candidate: Candidate) {
    def provenance: Provenance = candidate.provenance
    def components: Vector[ComponentCapabilityDiscovery.Discovered] = candidate.components
    def componentPackages: Vector[ComponentPackage] = candidate.componentPackages
    def usage: Vector[ComponentUsagePackage] = candidate.usage
    def useCase: UnavailableUpstreamProjection = candidate.useCase
  }
  final case class Attempt(reference: AttemptReference, snapshotReference: SnapshotReference, selection: Selection)
  enum Outcome {
    case Accepted(snapshotReference: SnapshotReference)
    case Rejected(diagnostic: Diagnostic)
  }
  final case class AttemptRecord(attempt: Attempt, outcome: Outcome)
  final class State private[CapabilityIngestionModel] (
    val current: Option[SnapshotReference], val snapshots: Vector[Snapshot], val attempts: Vector[AttemptRecord]
  )
  object State { val empty: State = new State(None, Vector.empty, Vector.empty) }
  abstract class Kernel {
    self: CapabilityIngestion.type =>
    protected final def _candidate(provenance: Provenance, componentPackages: Vector[ComponentPackage], usage: Vector[ComponentUsagePackage], useCase: UnavailableUpstreamProjection): Candidate =
      new Candidate(provenance, componentPackages, usage, useCase)
    protected final def _snapshot(reference: SnapshotReference, candidate: Candidate): Snapshot = new Snapshot(reference, candidate)
    protected final def _state(current: Option[SnapshotReference], snapshots: Vector[Snapshot], attempts: Vector[AttemptRecord]): State = new State(current, snapshots, attempts)
  }
  final case class Transition(state: State, outcome: Outcome)
  final case class CurrentSelection(snapshotReference: SnapshotReference, expected: Provenance, observation: Either[Diagnostic, Unit])
}
