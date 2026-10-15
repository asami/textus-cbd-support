package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Paths
import io.circe.Json
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonPart._items
import CapabilityIngestionFixtures.{right, snapshot}

/*
 * Explicitly synthetic pure semantic carriers. These never construct a Snapshot
 * or stand in for normal producer/native interoperability.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogFixtures {
  val definition: CapabilityDefinition = {
    val original = CapabilityIngestionFixtures.capability
    val app = CapabilityComponentUsageProjectionFixtures.usageDefinition(CapabilityComponentUsageProjectionFixtures.projection)
    original.copy(
      discoveryReferences = Present(_items(app.discoveryReferences).map(e => e.copy(source = e.source.copy(modelAuthority = original.identity.modelId)))),
      grounding = Present(_items(app.grounding).map(e => e.copy(source = e.source.copy(modelAuthority = original.identity.modelId)))),
      traceability = Present(_items(app.traceability).map(e => e.copy(source = e.source.copy(modelAuthority = original.identity.modelId)))))
  }
  val scope: Scope = CapabilityComponentUsageProjectionFixtures.scope.copy(capabilities = Vector(definition))
  val usage: ComponentUsageProjection = CapabilityComponentUsageProjectionFixtures.projection
  val usecase: UnavailableUpstreamProjection = CapabilityIngestionFixtures.useCase
  val unavailable: UnavailableUpstreamProjection = UnavailableUpstreamProjection(ProjectionFamily.ComponentUsage, "not-selected", "Synthetic semantic ComponentUsage is unavailable.")
  def reference(value: CapabilityDefinition): CapabilityReference = CapabilityReference(value.identity, value.contractVersion)
  def sourcekey(value: Source): SourceKey = SourceKey(value.modelAuthority, value.path, value.sourceRevision)
  def contextkey(pkg: PackageProjection, resource: ResourceProjection): ComponentContextKey = ComponentContextKey(pkg.input.scopeId,
    resource.projection.component, resource.projection.packageCoordinate, resource.resourcePath, sourcekey(resource.projection.source))
  def declarationkey(pkg: PackageProjection, resource: ResourceProjection, value: Declaration): DeclarationRecordKey =
    DeclarationRecordKey(contextkey(pkg, resource), DeclarationKey(value.component, value.direction, value.capability))
  def packageprojection(id: String, suffix: String, selectedscope: Scope = scope, selecteddefinition: CapabilityDefinition = definition,
    targetcount: Int = 3, bothdirections: Boolean = true): PackageProjection = {
    val component = ComponentCoordinate(ComponentId("org.example." + id), "3.7.0-SNAPSHOT")
    val pkg = PackageCoordinate("package:" + id, "package-v21")
    val authority = selecteddefinition.identity.modelId
    val header = Source(authority, "models/" + suffix + ".cml", "header-r2", None)
    val key = DeclarationKey(component.componentId, Direction.Provided, reference(selecteddefinition))
    val contracts = TargetKind.values.toVector.take(targetcount).map { kind =>
      TargetContract(kind, ModelElementId("model:native", suffix + ":" + kind.tag), component.componentId,
        "target:" + kind.tag + "/v3", "abi:" + kind.tag + "/v6", Source("model:native", "models/targets.cml", "native:" + suffix, Some(11)))
    }
    val edges = contracts.map(t => Realization(key, t.targetKind, ModelReference(t.identity, ReferenceBoundary.Local), t.component,
      t.contractVersion, t.generatedAbiVersion, Source(authority, "models/edges.cml", "edge:" + suffix, Some(20)),
      Derived("derive:" + suffix, Vector(Source("foreign:input", "inputs/É.cml", "input-r55", None)))))
    val traces = _items(selecteddefinition.traceability)
    val provided = Declaration(component.componentId, Direction.Provided, key.capability,
      Source(authority, "models/declaration.cml", "provided:" + suffix, Some(9)), Present(edges), Present(traces))
    val required = Declaration(component.componentId, Direction.Required, key.capability,
      Source(authority, "models/required.cml", "required:" + suffix, Some(14)), Absent(AbsenceReason.NotRepresented, "No required realization representation."), Present(Vector.empty))
    val projection = Projection(1, "cncf.component-capability.v1", Producer("synthetic-semantic", "producer-v8"), component, pkg, header,
      Vector.empty, if (bothdirections) Vector(provided, required) else Vector(provided), Present(Vector.empty))
    val resource = ResourceProjection("meta/component-capability/" + id + ".json", projection, selectedscope.capabilities,
      edges.zip(contracts).map((e, t) => ResolvedRealizationFact(e, t)))
    PackageProjection(PackageInput.Selected(selectedscope.scopeId, Location.Directory(Paths.get("/declared/catalog/" + suffix)), component, pkg), contracts, Vector(resource))
  }
  def ownedUsage(value: ComponentUsageProjection, input: PackageInput.Selected): ComponentUsageProjection =
    value.copy(component = input.component, packageCoordinate = input.packageCoordinate, documents = value.documents.map(doc =>
      doc.copy(componentUsageCapabilities = doc.componentUsageCapabilities.map(_.copy(component = input.component.componentId)))))
  def assemble(packages: Vector[PackageProjection], selectedScope: Scope = scope, usage: Option[ComponentUsageProjection] = Some(this.usage)): PortableProjection = {
    val projections = packages.zipWithIndex.flatMap { (pkg, i) => usage.map { value =>
      ownedUsage(if (i == 0) value else value.copy(documents = Vector.empty), pkg.input)
    } }
    val families = packages.zipWithIndex.map { (pkg, i) =>
      val family = usage match {
        case Some(_) => right(CapabilityComponentUsageProjectionCodec.admit(projections(i), selectedScope,
          packages.flatMap(_.resources.flatMap(_.projection.capabilities)), projections))
        case None => ComponentUsageFamily.Unavailable(unavailable)
      }
      ComponentUsagePackage(pkg.input, CapabilityComponentUsageAcquisitionPart.resourcePath, family)
    }
    val expectations = packages.zip(families).map { (p, family) =>
      val expected = family.family match {
        case ComponentUsageFamily.Available(value, _) => Some(ComponentUsageExpectation(value.producer, value.schemaVersion, value.generatedAbiVersion, value.documents.map(_.source)))
        case _: ComponentUsageFamily.Unavailable => None
      }
      PackageExpectation(p.input, p.resources.map(r => ResourceExpectation(r.resourcePath,
        r.projection.producer, r.projection.schemaVersion, r.projection.generatedAbiVersion, r.projection.source)), expected)
    }
    PortableProjection(1, snapshot("catalog:semantic|É", 77), Provenance(selectedScope, expectations, usecase), packages, families, usecase)
  }
  def withUsagePackages(value: PortableProjection, projections: Vector[Option[ComponentUsageProjection]]): PortableProjection = {
    val owned = value.packages.zip(projections).map((pkg, projection) => projection.map(ownedUsage(_, pkg.input)))
    val selected = owned.flatten
    val families = value.packages.zip(owned).map { (pkg, projection) =>
      val family = projection match {
        case Some(p) => right(CapabilityComponentUsageProjectionCodec.admit(p, value.provenance.scope,
          value.packages.flatMap(_.resources.flatMap(_.projection.capabilities)), selected))
        case None => ComponentUsageFamily.Unavailable(unavailable)
      }
      ComponentUsagePackage(pkg.input, CapabilityComponentUsageAcquisitionPart.resourcePath, family)
    }
    val expected = value.provenance.packages.zip(owned).map { (pkg, projection) => pkg.copy(usage = projection.map(p =>
      ComponentUsageExpectation(p.producer, p.schemaVersion, p.generatedAbiVersion, p.documents.map(_.source)))) }
    value.copy(provenance = value.provenance.copy(packages = expected), usage = families)
  }
  def complete: PortableProjection = assemble(Vector(packageprojection("Payments", "first")))
  def selection(value: PortableProjection): CatalogSelection = CatalogSelection(value.snapshotReference, value.provenance)
  def catalog(value: PortableProjection): Catalog = right(CapabilityCatalogProjectionCodec.validate(value, selection(value)))
  def withresource(value: PortableProjection, change: ResourceProjection => ResourceProjection): PortableProjection =
    value.copy(packages = value.packages.updated(0, value.packages.head.copy(resources = value.packages.head.resources.updated(0, change(value.packages.head.resources.head)))))
  def withfact(value: PortableProjection, change: ResolvedRealizationFact => ResolvedRealizationFact): PortableProjection =
    withresource(value, r => r.copy(realizations = r.realizations.updated(0, change(r.realizations.head))))
  def withdeclarededge(value: PortableProjection, change: Realization => Realization): PortableProjection = withresource(value, r => {
    val original = r.projection.declarations.head
    r.copy(projection = r.projection.copy(declarations = r.projection.declarations.updated(0,
      original.copy(realizations = Present(_items(original.realizations).updated(0, change(_items(original.realizations).head)))))))
  })
  def withusage(value: PortableProjection, projection: ComponentUsageProjection): PortableProjection = {
    val pkg = value.usage.head
    val available = pkg.family.asInstanceOf[ComponentUsageFamily.Available]
    value.copy(usage = value.usage.updated(0, pkg.copy(family = ComponentUsageFamily.Available(projection, available.unresolvedExternal))))
  }
  def multiplepaths: PortableProjection = {
    val extra = definition.copy(identity = ModelElementId("model:component", "no-required"), contractVersion = "no-required:v13")
    val selectedscope = scope.copy(capabilities = Vector(definition, extra))
    val original = CapabilityComponentUsageProjectionFixtures.usageDefinition(usage)
    val mappings = _items(original.componentMappings)
    val local = mappings.head
    val other = original.copy(identity = original.identity.copy(elementId = "second-app"), contractVersion = "app:v29", dependencies = Present(Vector.empty),
      componentMappings = Present(Vector(local.copy(source = local.source.copy(sourceRevision = "mapping:second")))))
    val first = original.copy(componentMappings = Present(Vector(local, local, local.copy(target = reference(extra)), mappings(1))))
    val app = usage.copy(documents = usage.documents.updated(0, usage.documents.head.copy(componentUsageCapabilities = Vector(first, other))))
    assemble(Vector(packageprojection("Payments", "first", selectedscope), packageprojection("Orders", "second", selectedscope)), selectedscope, Some(app))
  }
  def compatible: PortableProjection = {
    val earlier = definition.copy(contractVersion = "contract:opaque-earlier", source = definition.source.copy(sourceRevision = "definition:earlier"))
    val selectedscope = scope.copy(capabilities = Vector(definition, earlier))
    val pkg = packageprojection("Payments", "first", selectedscope)
    val edge = Compatibility(definition.identity, earlier.contractVersion, definition.contractVersion,
      Source(definition.identity.modelId, "edges/compatibility.cml", "compatibility:r4", Some(19)), Derived("compatibility:declared", Vector(definition.source)))
    val updated = pkg.copy(resources = pkg.resources.map(r => r.copy(projection = r.projection.copy(compatibility = Present(Vector(edge))))))
    assemble(Vector(updated), selectedscope)
  }
  final case class Sample(token: String, contract: String, revision: Int, componentcount: Int, usagecount: Int, targetcount: Int, present: Boolean, repeated: Boolean)
  def generated(sample: Sample): PortableProjection = {
    val authority = "Model:" + sample.token
    val selecteddefinition = definition.copy(identity = ModelElementId(authority, "Capability:" + sample.token), contractVersion = sample.contract,
      source = Source(authority, "definitions/É|É.cml", "definition:" + sample.revision, Some(5)),
      summary = if (sample.present) Present("Summary:" + sample.token) else Absent(AbsenceReason.NotDeclared, "Absent:" + sample.token),
      discoveryReferences = Present(_items(definition.discoveryReferences).map(e => e.copy(source = e.source.copy(modelAuthority = authority)))),
      grounding = Present(_items(definition.grounding).map(e => e.copy(source = e.source.copy(modelAuthority = authority), origin = Derived("ground:" + sample.token,
        Vector(Source("Foreign:" + sample.token, "inputs/source.cml", "input:" + sample.revision, None)))))),
      traceability = Present(_items(definition.traceability).map(e => e.copy(source = e.source.copy(modelAuthority = authority)))))
    val selectedscope = scope.copy(scopeId = "scope:" + sample.token, source = scope.source.copy(sourceRevision = "scope:" + sample.revision), capabilities = Vector(selecteddefinition))
    val packages = Vector.tabulate(sample.componentcount) { i =>
      val pkg = packageprojection("Catalog" + (i + 1), "component" + (i + 1), selectedscope, selecteddefinition, sample.targetcount)
      val coordinate = pkg.input.component.copy(version = if (sample.present) "4.6.0" else "3.7.0-SNAPSHOT")
      val pkgcoordinate = pkg.input.packageCoordinate.copy(version = "package:" + sample.token + ":" + sample.revision)
      val contracts = pkg.targetContracts.map(t => t.copy(contractVersion = "target:" + t.targetKind.tag + ":" + (sample.revision + 13),
        generatedAbiVersion = "native-abi:" + t.targetKind.tag + ":" + sample.token, source = t.source.copy(sourceRevision = "native-source:" + sample.revision)))
      pkg.copy(input = pkg.input.copy(component = coordinate, packageCoordinate = pkgcoordinate), targetContracts = contracts,
        resources = pkg.resources.map(r => r.copy(projection = r.projection.copy(component = coordinate, packageCoordinate = pkgcoordinate,
          producer = Producer("semantic:" + sample.token, "producer:" + sample.revision), source = r.projection.source.copy(sourceRevision = "header:" + sample.revision)),
          realizations = r.realizations.zip(contracts).map((f, t) => ResolvedRealizationFact(f.edge.copy(targetContractVersion = t.contractVersion,
            generatedAbiVersion = t.generatedAbiVersion, source = f.edge.source.copy(sourceRevision = "edge-source:" + sample.revision),
            origin = Derived("edge:" + sample.token, Vector(selecteddefinition.source))), t))))) match {
        case updated => updated.copy(resources = updated.resources.map(r => r.copy(projection = r.projection.copy(declarations = r.projection.declarations.map { d =>
          if (d.direction == Direction.Provided) d.copy(realizations = Present(r.realizations.map(_.edge))) else d
        }))))
      }
    }
    val original = CapabilityComponentUsageProjectionFixtures.usageDefinition(usage)
    val apps = Vector.tabulate(sample.usagecount) { i =>
      val appauthority = "ComponentUsage:" + sample.token + ":" + i
      val source = Source(appauthority, "usages/app" + i + ".cml", "app-definition:" + sample.revision, Some(i + 1))
      val local = ComponentUsageCapabilityMapping(reference(selecteddefinition), ReferenceBoundary.Local, source.copy(path = "edges/mapping.cml", sourceRevision = "mapping:" + sample.revision),
        Derived("mapping:" + sample.token, Vector(selecteddefinition.source)))
      val external = local.copy(target = CapabilityReference(ModelElementId("External:" + sample.token, "foreign-capability"), "external:" + sample.contract), boundary = ReferenceBoundary.External)
      val mappings = (if (sample.repeated) Vector(local, local) else Vector(local)) :+ external
      val app = original.copy(identity = ModelElementId(appauthority, "ComponentUsage:" + sample.token), contractVersion = "app:" + sample.contract, source = source,
        summary = if (sample.present) Present("App summary:" + sample.token) else Absent(AbsenceReason.NotRepresented, "App summary absent."),
        demandReferences = Present(_items(original.demandReferences).map(e => e.copy(source = e.source.copy(modelAuthority = appauthority)))),
        discoveryReferences = if (sample.present) Present(Vector.empty) else Absent(AbsenceReason.NotDeclared, "Discovery absent."),
        componentMappings = Present(mappings), dependencies = Present(Vector.empty), grounding = Present(Vector.empty),
        traceability = Present(_items(original.traceability).map(e => e.copy(source = e.source.copy(modelAuthority = appauthority)))))
      ComponentUsageDocument(source.copy(sourceRevision = "app-header:" + sample.revision, line = None), Vector(app))
    }
    val app = usage.copy(producer = Producer("semantic-app:" + sample.token, "app-producer:" + sample.revision), documents = apps)
    val projection = assemble(packages, selectedscope, Some(app))
    projection.copy(snapshotReference = snapshot("catalog:" + sample.token, sample.revision.toLong))
  }
  def update(json: Json, path: Vector[String], value: Json): Json = CapabilityComponentUsageProjectionFixtures.update(json, path)(_ => value)
}
