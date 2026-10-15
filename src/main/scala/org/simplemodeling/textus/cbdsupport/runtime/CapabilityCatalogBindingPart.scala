package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonPart.*
import CapabilityIngestionSelectionPart.{_provenance, _compare, _source_compare, _duplicates}

/*
 * Bind only declared finite semantic links; admission and native lifetime stay
 * with ingestion and CNCF. No provider search or runtime observation is inferred.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogBindingPart {
  private val _unknown = RuntimeObservations(AvailabilityObservation.Unknown("No runtime observation supplied."),
    AuthorizationObservation.Unknown("No runtime observation supplied."), GuardObservation.Unknown("No runtime observation supplied."),
    ExecutionResultObservation.Unknown("No runtime observation supplied."))
  private def _equal[A](expected: A, observed: A, path: String): Either[Diagnostic, Unit] =
    Either.cond(expected == observed, (), _diagnostic(DiagnosticKind.Incompatible, "Declared semantic values differ.", path))
  private def _coverage(expected: Int, observed: Int, path: String): Either[Diagnostic, Unit] =
    Either.cond(expected == observed, (), _malformed(path, "Complete ordered semantic coverage is required."))
  private def _source_key(source: Source): SourceKey = SourceKey(source.modelAuthority, source.path, source.sourceRevision)
  private def _context_key(pkg: PackageProjection, resource: ResourceProjection): ComponentContextKey = {
    val p = resource.projection
    ComponentContextKey(pkg.input.scopeId, p.component, p.packageCoordinate, resource.resourcePath, _source_key(p.source))
  }
  private def _declaration_key(value: Declaration): DeclarationKey = DeclarationKey(value.component, value.direction, value.capability)
  private def _upstream[A](result: Either[Diagnostic, A], path: String): Either[Diagnostic, A] =
    result.left.map(d => d.copy(location = path + d.location))
  private def _unique[A, K](values: Vector[A], path: String)(key: A => K, source: A => Source): Either[Diagnostic, Unit] =
    values.indices.find(i => values.take(i).exists(x => key(x) == key(values(i)))) match {
      case Some(i) => Left(_diagnostic(DiagnosticKind.Ambiguous, "Repeated exact semantic candidate/key.", s"$path/$i").copy(source = Some(source(values(i)))))
      case None => Right(())
    }
  def bind(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, Catalog] = {
    val result = for {
    p <- _nonnull(projection, "/")
    _ <- Either.cond(p.schemaVersion == 1, (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported internal catalog schema.", "/schemaVersion"))
    s <- _nonnull(selection, "/selection")
    ref <- _nonnull(p.snapshotReference, "/snapshotReference")
    _ <- SnapshotReference.from(ref.value)
    selected <- _nonnull(s.snapshotReference, "/selection/snapshotReference")
    _ <- SnapshotReference.from(selected.value)
    _ <- Either.cond(selected == ref, (), _diagnostic(DiagnosticKind.Stale, "Selected snapshot reference differs.", "/snapshotReference"))
    _ <- _provenance(p.provenance)
    _ <- _compare(s.provenance, p.provenance)
    // The one closed structural decoder also checks caller-created carriers.
    json <- CapabilityCatalogJsonEncodingPart.encode(p)
    _ <- CapabilityCatalogJsonPart.decode(json)
    _ <- _owning_keys(p)
    _ <- _coverage(p.provenance.packages.size, p.packages.size, "/packages")
    resources <- _sequence(p.packages.zip(p.provenance.packages).zipWithIndex.map { case ((pkg, expected), i) =>
      _package(pkg, expected, p.provenance.scope, s"/packages/$i")
    }).map(_.flatten)
    _ <- _usage(p, resources)
    _ <- _equal(p.provenance.useCase, p.useCase, "/useCase")
    } yield _catalog(p, resources)
    _context(result, None, Option(projection).flatMap(p => Option(p.provenance)).flatMap(p => Option(p.scope)).map(_.scopeId))
  }
  private def _owning_keys(p: PortableProjection): Either[Diagnostic, Unit] = {
    val entries = p.packages.flatMap(pkg => pkg.resources.map(r => (pkg, r)))
    val contexts = entries.map((pkg, r) => _context_key(pkg, r))
    val originalkeys = entries.map { (pkg, r) => (r.projection.component.componentId, _source_key(r.projection.source)) }
    def _unique_(keys: Vector[Any]): Either[Diagnostic, Unit] = keys.indices.find(i => keys.take(i).contains(keys(i))) match {
      case Some(i) =>
        val (pkg, r) = entries(i)
        Left(_diagnostic(DiagnosticKind.Ambiguous, "Repeated exact owning resource context.", "/" + r.resourcePath + "/source")
          .copy(scopeId = Some(pkg.input.scopeId), component = Some(r.projection.component.componentId), source = Some(r.projection.source)))
      case None => Right(())
    }
    for { _ <- _unique_(contexts); _ <- _unique_(originalkeys) } yield ()
  }
  private def _package(pkg: PackageProjection, expected: PackageExpectation, scope: Scope, path: String): Either[Diagnostic, Vector[ComponentResourceRecord]] =
    _context(for {
      _ <- _equal(expected.input, pkg.input, path + "/input")
      _ <- _unique(pkg.targetContracts, path + "/targetContracts")(t => (t.component, t.identity, t.targetKind, t.contractVersion, t.generatedAbiVersion), _.source)
      _ <- if (expected.resources.isEmpty) _equal(Vector.empty[TargetContract], pkg.targetContracts, path + "/targetContracts") else Right(())
      _ <- _coverage(expected.resources.size, pkg.resources.size, path + "/resources")
      _ <- _equal(expected.resources.map(_.resourcePath), pkg.resources.map(_.resourcePath), path + "/resources")
      // Preserve the original package admission's cross-resource uniqueness.
      _ <- _unique(pkg.resources.flatMap(_.projection.capabilities), path + "/resources/capabilities")(d => (d.identity, d.contractVersion), _.source)
      _ <- _unique(pkg.resources.flatMap(_.projection.declarations), path + "/resources/declarations")(_declaration_key, _.source)
      resources <- _sequence(pkg.resources.zip(expected.resources).zipWithIndex.map { case ((r, e), i) => _resource(pkg, r, e, scope, s"$path/resources/$i") })
    } yield resources, None, Some(scope.scopeId), Some(pkg.input.component.componentId))
  private def _resource(pkg: PackageProjection, r: ResourceProjection, expected: ResourceExpectation, scope: Scope, path: String): Either[Diagnostic, ComponentResourceRecord] = {
    val p = r.projection
    _context(for {
      _ <- _equal((pkg.input.component, pkg.input.packageCoordinate), (p.component, p.packageCoordinate), path + "/projection/component")
      _ <- _equal((expected.producer, expected.schemaVersion, expected.generatedAbiVersion), (p.producer, p.schemaVersion, p.generatedAbiVersion), path + "/projection/producer")
      _ <- _source_compare(expected.source, p.source, path + "/projection/source")
      admitted <- _upstream(ComponentCapabilityAdmission.admit(p, ScopeInput.Ready(scope.copy(targetContracts = pkg.targetContracts))), path + "/projection")
      _ <- _unique(r.resolvedDefinitions, path + "/resolvedDefinitions")(d => (d.identity, d.contractVersion), _.source)
      _ <- _coverage(admitted.capabilities.size, r.resolvedDefinitions.size, path + "/resolvedDefinitions")
      _ <- _sequence(admitted.capabilities.zip(r.resolvedDefinitions).zipWithIndex.map { case ((a, b), i) => for {
        _ <- _source_compare(a.source, b.source, s"$path/resolvedDefinitions/$i/source")
        _ <- _equal(a, b, s"$path/resolvedDefinitions/$i")
      } yield () })
      facts = admitted.realizations.map(x => ResolvedRealizationFact(x.edge, x.target))
      _ <- _coverage(facts.size, r.realizations.size, path + "/realizations")
      _ <- _sequence(facts.zip(r.realizations).zipWithIndex.map { case ((a, b), i) => _fact(a, b, s"$path/realizations/$i") })
    } yield ComponentResourceRecord(_context_key(pkg, r), pkg.input, r), Some(p.source), Some(scope.scopeId), Some(p.component.componentId))
  }
  private def _fact(expected: ResolvedRealizationFact, observed: ResolvedRealizationFact, path: String): Either[Diagnostic, Unit] =
    _context(for {
      _ <- Either.cond(expected.edge.declarationKey == observed.edge.declarationKey, (), _malformed(path + "/edge/declarationKey", "Fact must retain its enclosing declaration owner."))
      _ <- Either.cond(expected.edge == observed.edge, (), _malformed(path + "/edge", "Fact must retain the exact ordered declared realization edge."))
      _ <- Either.cond(expected.target.identity == observed.target.identity, (), _malformed(path + "/target/identity", "Fact target identity differs from its declared target."))
      _ <- Either.cond(expected.target.component == observed.target.component, (), _malformed(path + "/target/component", "Fact target Component differs from its declared target."))
      _ <- _equal(expected.target.targetKind, observed.target.targetKind, path + "/target/targetKind")
      _ <- _equal(expected.target.contractVersion, observed.target.contractVersion, path + "/target/contractVersion")
      _ <- _equal(expected.target.generatedAbiVersion, observed.target.generatedAbiVersion, path + "/target/generatedAbiVersion")
      _ <- _source_compare(expected.target.source, observed.target.source, path + "/target/source")
      _ <- _equal(expected.target, observed.target, path + "/target")
    } yield (), Some(observed.edge.source), None, Some(observed.edge.declarationKey.component))
  private def _usage_key(pkg: ComponentUsagePackage, document: Source, definition: ComponentUsageDefinition): ComponentUsageCapabilityContextKey =
    ComponentUsageCapabilityContextKey(pkg.input.scopeId, pkg.input.component, pkg.input.packageCoordinate, pkg.resourcePath,
      _source_key(document), ComponentUsageCapabilityReference(definition.identity, definition.contractVersion))
  private def _usage(p: PortableProjection, resources: Vector[ComponentResourceRecord]): Either[Diagnostic, Unit] = {
    val projections = p.usage.flatMap(_.family match {
      case ComponentUsageFamily.Available(projection, _) => Vector(projection)
      case _: ComponentUsageFamily.Unavailable => Vector.empty
    })
    for {
      _ <- _coverage(p.provenance.packages.size, p.usage.size, "/usage")
      _ <- _sequence(p.usage.zip(p.provenance.packages).zipWithIndex.map { case ((pkg, expected), i) => for {
        _ <- _equal(expected.input, pkg.input, s"/usage/$i/input")
        _ <- _equal(CapabilityComponentUsageAcquisitionPart.resourcePath, pkg.resourcePath, s"/usage/$i/resourcePath")
        _ <- pkg.family match {
          case ComponentUsageFamily.Available(projection, external) => for {
            _ <- CapabilityComponentUsageAcquisitionPart.observe(expected, projection)
            admitted <- CapabilityComponentUsageProjectionCodec.admit(projection, p.provenance.scope,
              resources.flatMap(_.resource.projection.capabilities), projections)
            _ <- _equal(admitted.unresolvedExternal, external, s"/usage/$i/unresolvedExternal")
          } yield ()
          case ComponentUsageFamily.Unavailable(value) => for {
            _ <- _equal(None, expected.usage, s"/usage/$i/expected")
            _ <- _equal(ProjectionFamily.ComponentUsage, value.family, s"/usage/$i/value/family")
            _ <- _text(value.reason, s"/usage/$i/value/reason")
            _ <- _text(value.detail, s"/usage/$i/value/detail")
          } yield ()
        }
      } yield () })
      keys = p.usage.flatMap(pkg => pkg.family match {
        case ComponentUsageFamily.Available(projection, _) => projection.documents.flatMap(doc => doc.componentUsageCapabilities.map(a => _usage_key(pkg, doc.source, a)))
        case _: ComponentUsageFamily.Unavailable => Vector.empty
      })
      _ <- _duplicates(keys, "/usage/records")
    } yield ()
  }
  private def _catalog(p: PortableProjection, resources: Vector[ComponentResourceRecord]): Catalog = {
    val capabilities = resources.flatMap { context => context.resource.projection.declarations.map { declaration =>
      val key = DeclarationRecordKey(context.key, _declaration_key(declaration))
      // Public admission has established exactly one matching resource-local definition.
      val Vector(definition) = context.resource.resolvedDefinitions.filter(d => CapabilityReference(d.identity, d.contractVersion) == declaration.capability): @unchecked
      val refs = context.resource.realizations.filter(r => r.edge.declarationKey == key.declaration && _items(declaration.realizations).contains(r.edge))
        .map(r => CapabilityRealizationRef(key, r.edge, r.target, _unknown))
      CapabilityRecord(key, context, declaration, definition, refs)
    } }
    val usages = p.usage.flatMap { pkg => pkg.family match {
      case ComponentUsageFamily.Available(projection, _) => projection.documents.flatMap(doc => doc.componentUsageCapabilities.map(a =>
        ComponentUsageCapabilityRecord(_usage_key(pkg, doc.source, a), pkg.input, doc.source, a)))
      case _: ComponentUsageFamily.Unavailable => Vector.empty
    } }
    val paths = usages.flatMap { app => _items(app.definition.componentMappings).map { mapping =>
      val records = if (mapping.boundary == ReferenceBoundary.Local) capabilities.filter(c => c.declaration.direction == Direction.Required && c.declaration.capability == mapping.target).map(_.key) else Vector.empty
      val endpoint = if (records.nonEmpty) RequiredEndpoint.Resolved(records) else RequiredEndpoint.Unresolved(mapping.target, mapping.boundary)
      ComponentUsageRequirementPath(app.key, mapping, endpoint)
    } }
    val compatibility = resources.flatMap(r => _items(r.resource.projection.compatibility).map(CompatibilityRecord(r.key, _)))
    val traceability = resources.flatMap { r =>
      r.resource.resolvedDefinitions.flatMap(d => _items(d.traceability).map(e => AttributedTraceability(TraceabilityOwner.CapabilityDefinition(r.key, CapabilityReference(d.identity, d.contractVersion)), e))) ++
        r.resource.projection.declarations.flatMap(d => _items(d.traceability).map(e => AttributedTraceability(TraceabilityOwner.Declaration(DeclarationRecordKey(r.key, _declaration_key(d))), e)))
    } ++ usages.flatMap(a => _items(a.definition.traceability).map(e => AttributedTraceability(TraceabilityOwner.ComponentUsage(a.key), e)))
    Catalog(p, resources, capabilities, usages, compatibility, traceability, CapabilityDemandTrace(p.useCase, p.usage, paths))
  }
}
