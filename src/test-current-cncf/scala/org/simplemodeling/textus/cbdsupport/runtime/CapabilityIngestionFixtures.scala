package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import io.circe.Json
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.operation.CmlOperationDefinition
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*

/*
 * Synthetic public kernel conformance inputs, not normal Cozy producer proof.
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityIngestionFixtures {
  val scope: Scope = Scope("scope:kernel", Source("scope:authority", "scope/facts.cml", "scope-r100", None),
    Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
  val useCase: UnavailableUpstreamProjection = UnavailableUpstreamProjection(ProjectionFamily.UseCase, "not-yet-admitted", "Positive forward UseCase demand path remains unexercised.")
  val applicationUnavailable: ApplicationInput = ApplicationInput.Unavailable(UnavailableUpstreamProjection(ProjectionFamily.Application, "not-selected", "Application projection is explicitly unavailable."))
  val capability: CapabilityDefinition = CapabilityDefinition(ModelElementId("model:component", "cap"), "cap-v7", "支払::Capability",
    Absent(AbsenceReason.NotDeclared, "No summary."), Present("Literal É / E\u0301 | contract."), Source("model:component", "models/definition.cml", "definition-r9", Some(5)),
    Present(Vector.empty), Present(Vector.empty), Present(Vector.empty))
  val component: ComponentCoordinate = ComponentCoordinate(ComponentId("org.example.Payments"), "3.7.0-SNAPSHOT")
  val packageCoordinate: PackageCoordinate = PackageCoordinate("package:kernel", "package-v21")
  def native(componentId: ComponentId = component.componentId, selectedScope: Scope = scope): NativeInput = {
    val source = Source("model:native", "models/targets.cml", "native-r12", Some(3))
    val operation = CmlOperationDefinition(name = "Charge", kind = "command", inputType = "Input", outputType = "Output", inputValueKind = "record")
    val snapshot = NativeSnapshot(componentId, source, Vector(operation), Vector.empty, Vector.empty)
    val target = TargetContract(TargetKind.Operation, ModelElementId("model:native", "explicit:charge"), componentId, "target-v3", "operation:abi-v6", source.copy(line = Some(11)))
    NativeInput(NativeAcquisition.Acquired(selectedScope.scopeId, Vector(snapshot)), ScopeInput.Ready(selectedScope),
      Vector(NativeBinding(componentId, source, target, "Charge")))
  }
  def projection(input: NativeInput = native(), suffix: String = "first", coordinate: ComponentCoordinate = component): Projection = {
    val binding = input.bindings.head
    val key = DeclarationKey(coordinate.componentId, Direction.Provided, CapabilityReference(capability.identity, capability.contractVersion))
    val edge = Realization(key, binding.target.targetKind, ModelReference(binding.target.identity, ReferenceBoundary.Local), binding.target.component,
      binding.target.contractVersion, binding.target.generatedAbiVersion, Source("model:component", "models/edges.cml", "edge-r4", Some(20)), Declared)
    val declaration = Declaration(coordinate.componentId, Direction.Provided, key.capability, Source("model:component", "models/declaration.cml", "declaration-r6", Some(9)), Present(Vector(edge)), Present(Vector.empty))
    Projection(1, "cncf.component-capability.v1", Producer("synthetic-kernel", "producer-v8"), coordinate, packageCoordinate,
      Source("model:component", s"models/$suffix.cml", "header-r2", None), Vector(capability), Vector(declaration), Present(Vector.empty))
  }
  def emptyProjection(suffix: String): Projection = projection(suffix = suffix).copy(capabilities = Vector.empty, declarations = Vector.empty)
  final class CountingProvider(val input: NativeInput) extends ComponentCapabilityMetadataProvider {
    private var _reads = 0
    def reads: Int = _reads
    def componentCapabilityMetadata: NativeInput = { _reads += 1; input }
  }
  def withDirectory[A](body: Path => A): A = {
    val root = Files.createTempDirectory("cbd-capability-kernel-")
    try body(root) finally {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_)) finally stream.close()
    }
  }
  def write(root: Path, resource: String, text: String): Unit = {
    val path = root.resolve(resource)
    Files.createDirectories(path.getParent)
    Files.writeString(path, text, StandardCharsets.UTF_8)
    ()
  }
  def selected(root: Path, values: Vector[(String, Projection)], selectedScope: Scope = scope, input: NativeInput = native()): PackageSelection = {
    val coordinate = values.headOption.map(_._2.component).getOrElse(component)
    val pkg = values.headOption.map(_._2.packageCoordinate).getOrElse(packageCoordinate)
    // Canonical schema-3 descriptor shape follows public PackageLoaderSpec.
    val descriptor = Json.obj("schemaVersion" -> Json.fromInt(3), "component" -> Json.obj(
      "namespace" -> Json.fromString(coordinate.componentId.sharedIdentity.namespace().value()),
      "id" -> Json.fromString(coordinate.componentId.sharedIdentity.localId().value()), "version" -> Json.fromString(coordinate.version)))
    write(root, "meta/component-descriptor.json", descriptor.noSpaces)
    write(root, indexPath, Json.obj("schemaVersion" -> Json.fromInt(1), "projections" -> Json.fromValues(values.map(x => Json.fromString(x._1)))).noSpaces)
    values.foreach { (filename, value) => write(root, metadataDirectory + "/" + filename, ComponentCapabilityProjectionCodec.encode(value).fold(e => throw new IllegalArgumentException(e.toString), identity)) }
    val expected = PackageExpectation(PackageInput.Selected(selectedScope.scopeId, Location.Directory(root), coordinate, pkg),
      values.map { (filename, value) => ResourceExpectation(metadataDirectory + "/" + filename, value.producer, value.schemaVersion, value.generatedAbiVersion, value.source) })
    PackageSelection(expected, Some(new CountingProvider(input)))
  }
  def selection(packages: Vector[PackageSelection], selectedScope: Scope = scope, application: ApplicationInput = applicationUnavailable): Selection =
    Selection(selectedScope, packages, application, useCase)
  def record(id: String, revision: Long): InternalModelRecordReference = InternalModelRecordReference(
    InternalModelRecordId.from(id).fold(e => throw new IllegalArgumentException(e), identity),
    InternalModelRecordRevision.from(revision).fold(e => throw new IllegalArgumentException(e), identity))
  def snapshot(id: String, revision: Long = 1): SnapshotReference = SnapshotReference.from(record(id, revision)).fold(e => throw new IllegalArgumentException(e.toString), identity)
  def attempt(id: String, selection: Selection, snapshotReference: SnapshotReference): Attempt =
    Attempt(AttemptReference.from(record(id, 1)).fold(e => throw new IllegalArgumentException(e.toString), identity), snapshotReference, selection)
  def right[A](value: Either[Diagnostic, A]): A = value.fold(e => throw new IllegalArgumentException(e.toString), identity)
  def left[A](value: Either[Diagnostic, A]): Diagnostic = value.fold(identity, _ => throw new IllegalArgumentException("Expected a typed rejection."))
  def rejected(kind: DiagnosticKind): Diagnostic = Diagnostic(kind, "Original independent acquisition observation.", Some(scope.scopeId), Some(component.componentId), Some(scope.source), "/observed/native")
}
