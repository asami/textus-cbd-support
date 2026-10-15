package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityComponentUsageJsonPart._items

/*
 * Synthetic pure semantic inputs, admitted through the original validator.
 * Normal producer interoperability belongs to the separate producer suite.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityQueryFixtures {
  val reference: CapabilityReference = CapabilityCatalogFixtures.reference(CapabilityCatalogFixtures.definition)
  val usageReference: ComponentUsageCapabilityReference = ComponentUsageCapabilityReference(ModelElementId("model:app", "query:ComponentUsage|É"), "app:opaque/v7")
  val roles: Vector[TraceabilityRole] = Vector(TraceabilityRole.Specification, TraceabilityRole.Evidence)
  val absentRoles: Presence[Vector[Traceability]] = Absent(AbsenceReason.NotRepresented, "No role projection supplied for this owner.")
  def traces(authority: String, selected: Vector[TraceabilityRole]): Vector[Traceability] = selected.map { role =>
    Traceability(role, ModelReference(ModelElementId("external:roles", role.tag + "|É"), ReferenceBoundary.External),
      Source(authority, "edges/traceability.cml", "trace:" + role.tag, Some(31)),
      Derived("trace:declared|É", Vector(Source("foreign:trace", "inputs/source.cml", "input:v17", None))))
  }
  def definition(ref: CapabilityReference, selected: Vector[TraceabilityRole] = Vector.empty): CapabilityDefinition =
    CapabilityCatalogFixtures.definition.copy(identity = ref.identity, contractVersion = ref.contractVersion,
      source = Source(ref.identity.modelId, "definitions/query.cml", "definition:" + ref.contractVersion, Some(4)),
      name = "Same capability É", summary = Present("Summary literal | Alpha"), description = Present("Description has  padded  literal text"),
      discoveryReferences = Present(Vector.empty), grounding = Present(Vector.empty), traceability = Present(traces(ref.identity.modelId, selected)))
  def scope(definitions: Vector[CapabilityDefinition]): Scope = CapabilityCatalogFixtures.scope.copy(capabilities = definitions)
  def packageFor(id: String, suffix: String, selectedscope: Scope, ref: CapabilityReference,
    directions: Vector[Direction], targetcount: Int = 3, selectedroles: Vector[TraceabilityRole] = roles,
    realizationAbsent: Boolean = false): PackageProjection = {
    val selecteddefinition = selectedscope.capabilities.find(d => CapabilityReference(d.identity, d.contractVersion) == ref).get
    val pkg = CapabilityCatalogFixtures.packageprojection(id, suffix, selectedscope, selecteddefinition, targetcount)
    val resource = pkg.resources.head
    val declarations = resource.projection.declarations.filter(d => directions.contains(d.direction)).map { d =>
      d.copy(traceability = Present(traces(ref.identity.modelId, selectedroles)), realizations =
        if (d.direction == Direction.Provided && realizationAbsent) Absent(AbsenceReason.NotDeclared, "This provider declared no realization.") else d.realizations)
    }
    val facts = if (directions.contains(Direction.Provided) && !realizationAbsent) resource.realizations else Vector.empty
    pkg.copy(resources = Vector(resource.copy(projection = resource.projection.copy(declarations = declarations), realizations = facts)))
  }
  def compatibility(ref: CapabilityReference, providedVersion: String, revision: String): Compatibility =
    Compatibility(ref.identity, ref.contractVersion, providedVersion,
      Source(ref.identity.modelId, "edges/compatibility.cml", revision, Some(19)),
      Derived("compatibility:explicit", Vector(Source("foreign:compatibility", "inputs/compatibility.cml", "input:" + revision, None))))
  def withCompatibility(pkg: PackageProjection, edges: Vector[Compatibility]): PackageProjection =
    pkg.copy(resources = pkg.resources.map(r => r.copy(projection = r.projection.copy(compatibility = Present(edges)))))
  def mapping(ref: CapabilityReference = reference, boundary: ReferenceBoundary = ReferenceBoundary.Local): ComponentUsageCapabilityMapping =
    ComponentUsageCapabilityMapping(ref, boundary, Source("model:app", "edges/mapping.cml", "mapping:opaque|É", Some(12)),
      Derived("mapping:declared", Vector(Source("foreign:map", "inputs/map.cml", "map-input:v31", None))))
  def usages(mappings: Vector[ComponentUsageCapabilityMapping], contexts: Int = 1, selectedroles: Vector[TraceabilityRole] = roles,
    otherVersion: Boolean = false): ComponentUsageProjection = {
    val original = CapabilityComponentUsageProjectionFixtures.usageDefinition(CapabilityCatalogFixtures.usage)
    val app = original.copy(identity = usageReference.identity, contractVersion = usageReference.contractVersion,
      componentMappings = Present(mappings), dependencies = Present(Vector.empty), grounding = Present(Vector.empty),
      discoveryReferences = Present(Vector.empty), traceability = Present(traces("model:app", selectedroles)))
    val documents = Vector.tabulate(contexts) { i =>
      val source = Source("model:app", "usages/context" + i + ".cml", "document:v" + i, None)
      val identity = if (i == 0) usageReference.identity else
        usageReference.identity.copy(elementId = usageReference.identity.elementId + ":fixture-usage-" + i)
      ComponentUsageDocument(source, Vector(app.copy(identity = identity, source = app.source.copy(path = source.path))))
    }
    val extra = if (otherVersion) {
      val source = Source("model:app", "usages/other.cml", "other:v1", None)
      Vector(ComponentUsageDocument(source,
        Vector(app.copy(contractVersion = "app:other-version", source = app.source.copy(path = source.path)))))
    } else Vector.empty
    CapabilityCatalogFixtures.usage.copy(documents = documents ++ extra)
  }
  def assemble(packages: Vector[PackageProjection], selectedscope: Scope, app: Option[ComponentUsageProjection]): PortableProjection =
    CapabilityCatalogFixtures.assemble(packages, selectedscope, app)
  def selection(input: PortableProjection): CatalogSelection = CapabilityCatalogFixtures.selection(input)
  def session(input: PortableProjection): CapabilityQueries.Session =
    CapabilityIngestionFixtures.right(CapabilityQueries.open(Right(input), selection(input)))
  def value[A](result: Either[QueryFailure, QueryResult[A]]): A =
    result.fold(error => throw new IllegalArgumentException(error.toString), _.value)
  def failure[A](result: Either[QueryFailure, QueryResult[A]]): QueryFailure =
    result.fold(identity, _ => throw new IllegalArgumentException("Expected a query failure."))
  def keys(input: PortableProjection, direction: Direction): Vector[DeclarationRecordKey] =
    input.packages.flatMap(p => p.resources.flatMap(r => r.projection.declarations.filter(_.direction == direction)
      .map(CapabilityCatalogFixtures.declarationkey(p, r, _))))
  def sourceDiagnostic(failure: QueryFailure): Diagnostic = failure match {
    case QueryFailure.Source(diagnostic) => diagnostic
    case _ => throw new IllegalArgumentException("Expected original Source diagnostic.")
  }
  final case class ProviderInput(ref: CapabilityReference, targets: Int = 3, selectedroles: Vector[TraceabilityRole] = roles, absent: Boolean = false)
  def scenario(providers: Vector[ProviderInput] = Vector(ProviderInput(reference)), requiredcount: Int = 1,
    mappings: Vector[ComponentUsageCapabilityMapping] = Vector(mapping()), contexts: Int = 1,
    requiredroles: Vector[TraceabilityRole] = roles, usageroles: Vector[TraceabilityRole] = roles,
    definitionroles: Vector[TraceabilityRole] = Vector.empty, witnesses: Vector[Compatibility] = Vector.empty,
    extraDefinitions: Vector[CapabilityReference] = Vector.empty, usageAvailable: Boolean = true,
    otherUsageVersion: Boolean = false, requiredReference: CapabilityReference = reference): PortableProjection = {
    val refs = (Vector(requiredReference) ++ providers.map(_.ref) ++ extraDefinitions).distinct
    val selectedscope = scope(refs.map(definition(_, definitionroles)))
    val required = Vector.tabulate(requiredcount)(i => packageFor("Required" + i, "required" + i, selectedscope,
      requiredReference, Vector(Direction.Required), 0, requiredroles))
    val provided = providers.zipWithIndex.map { (p, i) => packageFor("Provider" + i, "provider" + i, selectedscope,
      p.ref, Vector(Direction.Provided), p.targets, p.selectedroles, p.absent) }
    val packages = required ++ provided
    val witnessed = if (witnesses.nonEmpty) packages.updated(0, withCompatibility(packages.head, witnesses)) else packages
    assemble(witnessed, selectedscope, if (usageAvailable) Some(usages(mappings, contexts, usageroles, otherUsageVersion)) else None)
  }
  enum SampleStatus { case Exact, Directed, WrongVersion, OtherIdentity }
  final case class Sample(token: String, status: SampleStatus, providercount: Int, targets: Vector[Int], mappingcount: Int)
  def generated(sample: Sample): PortableProjection = {
    val required = CapabilityReference(ModelElementId("Model:" + sample.token, "Capability:" + sample.token), "contract:" + sample.token)
    val provided = sample.status match {
      case SampleStatus.Exact => required
      case SampleStatus.Directed => required.copy(contractVersion = "provided:directed|" + sample.token)
      case SampleStatus.WrongVersion => required.copy(contractVersion = "provided:wrong|" + sample.token)
      case SampleStatus.OtherIdentity => required.copy(identity = required.identity.copy(modelId = "Other:" + sample.token))
    }
    val witnesses = if (sample.status == SampleStatus.Directed) Vector(compatibility(required, provided.contractVersion, "witness:generated")) else Vector.empty
    scenario(Vector.tabulate(sample.providercount)(i => ProviderInput(provided, sample.targets(i))),
      mappings = Vector.fill(sample.mappingcount)(mapping(required)), witnesses = witnesses,
      extraDefinitions = Vector(provided), requiredReference = required)
  }
}
