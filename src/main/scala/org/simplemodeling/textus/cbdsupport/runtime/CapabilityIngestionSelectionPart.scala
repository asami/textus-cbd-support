package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.identity.ComponentReleaseCoordinate
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityApplicationJsonPart.*
import CapabilityApplicationReferenceJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityIngestionSelectionPart {
  private val _encoding = CapabilityApplicationJsonEncodingPart
  private def _equal[A](expected: A, observed: A, path: String): Either[Diagnostic, Unit] =
    Either.cond(expected == observed, (), _diagnostic(DiagnosticKind.Incompatible, "Declared and observed values differ.", path))
  def _source_compare(expected: Source, observed: Source, path: String): Either[Diagnostic, Unit] = _context(for {
    _ <- _check_source(expected, path)
    _ <- _check_source(observed, path)
    _ <- _equal((expected.modelAuthority, expected.path, expected.line), (observed.modelAuthority, observed.path, observed.line), path)
    _ <- Either.cond(expected.sourceRevision == observed.sourceRevision, (), _diagnostic(DiagnosticKind.Stale, "Declared opaque source revision differs.", _child(path, "sourceRevision")))
  } yield (), Option(observed))
  private def _header(producer: Producer, schema: Int, abi: String, application: Boolean, path: String): Either[Diagnostic, Unit] = for {
    p <- _nonnull(producer, _child(path, "producer"))
    _ <- _text(p.id, path + "/producer/id")
    _ <- _text(p.version, path + "/producer/version")
    _ <- _text(abi, _child(path, "generatedAbiVersion"))
    _ <- Either.cond(schema == 1, (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported expected schema.", _child(path, "schemaVersion")))
    _ <- Either.cond(abi == (if (application) "cozy.application-capability.v1" else "cncf.component-capability.v1"), (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported expected ABI.", _child(path, "generatedAbiVersion")))
  } yield ()
  private def _component(value: ComponentId, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path)
    parsed <- ComponentId.parseC(x.name).toOption.toRight(_malformed(path, "A canonical Component identity is required."))
    _ <- Either.cond(x == parsed, (), _malformed(path, "Component identity must be canonical."))
  } yield ()
  def _definition(value: CapabilityDefinition, path: String): Either[Diagnostic, Unit] = _context(for {
    x <- _nonnull(value, path)
    _ <- _check_model_id(x.identity, _child(path, "identity"))
    _ <- _text(x.contractVersion, _child(path, "contractVersion"))
    _ <- _text(x.name, _child(path, "name"))
    _ <- _presence(_encoding._presence(x.summary)(_encoding._string), _child(path, "summary"))(_string)
    _ <- _presence(_encoding._presence(x.description)(_encoding._string), _child(path, "description"))(_string)
    _ <- _check_source(x.source, _child(path, "source"))
    _ <- _authority(x.source, x.identity.modelId, _child(path, "source"))
    discovery <- _presence(_encoding._presence(x.discoveryReferences)(xs => _encoding._vector(xs)(_encoding._model_link)), _child(path, "discoveryReferences"))((j, p) => _vector(j, p)(_model_link))
    grounding <- _presence(_encoding._presence(x.grounding)(xs => _encoding._vector(xs)(_encoding._grounding)), _child(path, "grounding"))((j, p) => _vector(j, p)(_grounding))
    trace <- _presence(_encoding._presence(x.traceability)(xs => _encoding._vector(xs)(_encoding._traceability)), _child(path, "traceability"))((j, p) => _vector(j, p)(_traceability))
    _ <- _values(_items(discovery), path + "/discoveryReferences/value")((e, p) => _authority(e.source, x.identity.modelId, p + "/source"))
    _ <- _values(_items(grounding), path + "/grounding/value")((e, p) => _authority(e.source, x.identity.modelId, p + "/source"))
    _ <- _values(_items(trace), path + "/traceability/value")((e, p) => _authority(e.source, x.identity.modelId, p + "/source"))
  } yield (), Option(value).flatMap(x => Option(x.source)))
  private def _fact_source(identity: String, source: Source, path: String): Either[Diagnostic, Unit] = for {
    _ <- _check_source(source, _child(path, "source"))
    _ <- _authority(source, identity, _child(path, "source"))
  } yield ()
  def _scope(value: Scope, path: String): Either[Diagnostic, Unit] = _context(for {
    x <- _nonnull(value, path)
    _ <- _text(x.scopeId, _child(path, "scopeId"))
    _ <- _check_source(x.source, _child(path, "source"))
    _ <- _values(x.modelFacts, _child(path, "modelFacts")) { (v, p) => for {
      f <- _nonnull(v, p); _ <- _check_model_id(f.identity, p + "/identity"); _ <- _nonnull(f.kind, p + "/kind")
      _ <- _fact_source(f.identity.modelId, f.source, p)
    } yield () }
    _ <- _values(x.termFacts, _child(path, "termFacts")) { (v, p) => for {
      f <- _nonnull(v, p); _ <- _term(_encoding._term(f.identity), p + "/identity"); _ <- _fact_source(f.identity.vocabularyId, f.source, p)
    } yield () }
    _ <- _values(x.relationFacts, _child(path, "relationFacts")) { (v, p) => for {
      f <- _nonnull(v, p); _ <- _relation(_encoding._relation(f.identity), p + "/identity"); _ <- _fact_source(f.identity.vocabularyId, f.source, p)
    } yield () }
    _ <- _values(x.profileFacts, _child(path, "profileFacts")) { (v, p) => for {
      f <- _nonnull(v, p); _ <- _profile(_encoding._profile(f.identity), p + "/identity"); _ <- _fact_source(f.identity.vocabularyId, f.source, p)
    } yield () }
    _ <- _values(x.capabilities, _child(path, "capabilities"))(_definition)
    contracts <- _nonnull(x.targetContracts, _child(path, "targetContracts"))
    _ <- Either.cond(contracts.isEmpty, (), _diagnostic(DiagnosticKind.Incompatible, "Ready scope must precede native target adaptation.", _child(path, "targetContracts")))
  } yield (), Option(value).flatMap(x => Option(x.source)), Option(value).map(_.scopeId))
  private def _availability(value: UnavailableUpstreamProjection, family: ProjectionFamily, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path)
    _ <- _nonnull(x.family, path + "/family")
    _ <- _text(x.reason, path + "/reason")
    _ <- _text(x.detail, path + "/detail")
    _ <- _equal(family, x.family, path + "/family")
    _ <- if (family == ProjectionFamily.UseCase) _equal("not-yet-admitted", x.reason, path + "/reason") else Right(())
  } yield ()
  def _application_expectation(value: ApplicationExpectation, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path)
    _ <- _header(x.producer, x.schemaVersion, x.generatedAbiVersion, true, path)
    _ <- _values(x.documents, path + "/documents")(_check_source)
    _ <- _duplicates(x.documents.map(s => (s.modelAuthority, s.path, s.sourceRevision)), path + "/documents")
  } yield ()
  private def _resource(value: ResourceExpectation, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path)
    _ <- _path(x.resourcePath, path + "/resourcePath")
    filename = x.resourcePath.stripPrefix(metadataDirectory + "/")
    _ <- Either.cond(x.resourcePath.startsWith(metadataDirectory + "/") && filename.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.json") && filename != "index.json", (),
      _malformed(path + "/resourcePath", "A canonical sibling projection resource is required."))
    _ <- _header(x.producer, x.schemaVersion, x.generatedAbiVersion, false, path)
    _ <- _check_source(x.source, path + "/source")
  } yield ()
  private def _package(value: PackageExpectation, scopeId: String, path: String): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, path)
    input <- _nonnull(x.input, path + "/input")
    _ <- _text(input.scopeId, path + "/input/scopeId")
    _ <- _equal(scopeId, input.scopeId, path + "/input/scopeId")
    location <- _nonnull(input.location, path + "/input/location")
    _ <- location match {
      case Location.Directory(root) => _nonnull(root, path + "/input/location/root").map(_ => ())
      case Location.Car(car) => _nonnull(car, path + "/input/location/car").map(_ => ())
    }
    component <- _nonnull(input.component, path + "/input/component")
    _ <- _component(component.componentId, path + "/input/component/componentId")
    _ <- _text(component.version, path + "/input/component/version")
    _ <- Either.cond(ComponentReleaseCoordinate.create(component.componentId.sharedIdentity, component.version).isSuccess(), (), _malformed(path + "/input/component/version", "A canonical release is required."))
    pkg <- _nonnull(input.packageCoordinate, path + "/input/packageCoordinate")
    _ <- _text(pkg.coordinate, path + "/input/packageCoordinate/coordinate")
    _ <- _text(pkg.version, path + "/input/packageCoordinate/version")
    _ <- _values(x.resources, path + "/resources")(_resource)
    _ <- _duplicates(x.resources.map(_.resourcePath), path + "/resources")
  } yield ()
  def _duplicates[A](values: Vector[A], path: String): Either[Diagnostic, Unit] =
    values.indices.find(i => values.take(i).contains(values(i))).map(i => Left(_diagnostic(DiagnosticKind.Ambiguous, "Repeated exact selected context/key.", _child(path, i.toString)))).getOrElse(Right(()))
  def _selection(value: Selection): Either[Diagnostic, Provenance] = for {
    x <- _nonnull(value, "/selection")
    _ <- _scope(x.scope, "/scope")
    _ <- _values(x.packages, "/packages") { (v, p) => for {
      pkg <- _nonnull(v, p); _ <- _package(pkg.expected, x.scope.scopeId, p + "/expected"); _ <- _nonnull(pkg.provider, p + "/provider")
    } yield () }
    _ <- _duplicates(x.packages.map(_.expected.input), "/packages")
    application <- x.application match {
      case ApplicationInput.Acquired(json, expected) => for { _ <- _nonnull(json, "/application/json"); _ <- _application_expectation(expected, "/application/expected") } yield ApplicationProvenance.Acquired(expected)
      case ApplicationInput.Unavailable(v) => _availability(v, ProjectionFamily.Application, "/application/unavailable").map(_ => ApplicationProvenance.Unavailable(v))
      case ApplicationInput.Rejected(d) => _nonnull(d, "/application/diagnostic").map(ApplicationProvenance.Rejected(_))
      case null => Left(_malformed("/application", "An explicit acquisition outcome is required."))
    }
    _ <- _availability(x.useCase, ProjectionFamily.UseCase, "/useCase")
  } yield Provenance(x.scope, x.packages.map(_.expected), application, x.useCase)
  private def _coverage[A](expected: Vector[A], observed: Vector[A], path: String): Either[Diagnostic, Unit] = for {
    _ <- Either.cond(expected.size == observed.size && expected.toSet == observed.toSet, (), _malformed(path, "Complete expected coverage is required."))
    _ <- _equal(expected, observed, path)
  } yield ()
  private def _paired[A](expected: Vector[A], observed: Vector[A], path: String)(compare: (A, A, String) => Either[Diagnostic, Unit]): Either[Diagnostic, Unit] = for {
    _ <- Either.cond(expected.size == observed.size, (), _malformed(path, "Complete expected fact coverage is required."))
    _ <- _sequence(expected.zip(observed).zipWithIndex.map { case ((a, b), i) => compare(a, b, _child(path, i.toString)) })
  } yield ()
  def _scope_compare(expected: Scope, observed: Scope): Either[Diagnostic, Unit] = _context(for {
    _ <- _scope(expected, "/scope")
    _ <- _scope(observed, "/scope")
    _ <- _equal(expected.scopeId, observed.scopeId, "/scope/scopeId")
    _ <- _source_compare(expected.source, observed.source, "/scope/source")
    _ <- _paired(expected.modelFacts, observed.modelFacts, "/scope/modelFacts")((a, b, p) => for { _ <- _equal((a.identity, a.kind), (b.identity, b.kind), p); _ <- _source_compare(a.source, b.source, p + "/source") } yield ())
    _ <- _paired(expected.termFacts, observed.termFacts, "/scope/termFacts")((a, b, p) => for { _ <- _equal(a.identity, b.identity, p); _ <- _source_compare(a.source, b.source, p + "/source") } yield ())
    _ <- _paired(expected.relationFacts, observed.relationFacts, "/scope/relationFacts")((a, b, p) => for { _ <- _equal(a.identity, b.identity, p); _ <- _source_compare(a.source, b.source, p + "/source") } yield ())
    _ <- _paired(expected.profileFacts, observed.profileFacts, "/scope/profileFacts")((a, b, p) => for { _ <- _equal(a.identity, b.identity, p); _ <- _source_compare(a.source, b.source, p + "/source") } yield ())
    _ <- _paired(expected.capabilities, observed.capabilities, "/scope/capabilities")(_definition_compare)
  } yield (), Option(observed).flatMap(x => Option(x.source)), Option(expected).map(_.scopeId))
  private def _definition_compare(a: CapabilityDefinition, b: CapabilityDefinition, path: String): Either[Diagnostic, Unit] = for {
    _ <- _source_compare(a.source, b.source, path + "/source")
    _ <- _paired(_items(a.discoveryReferences), _items(b.discoveryReferences), path + "/discoveryReferences/value")((x, y, p) => _edge_compare(x.source, y.source, x.origin, y.origin, p))
    _ <- _paired(_items(a.grounding), _items(b.grounding), path + "/grounding/value")((x, y, p) => _edge_compare(x.source, y.source, x.origin, y.origin, p))
    _ <- _paired(_items(a.traceability), _items(b.traceability), path + "/traceability/value")((x, y, p) => _edge_compare(x.source, y.source, x.origin, y.origin, p))
    _ <- _equal(a, b, path)
  } yield ()
  private def _edge_compare(a: Source, b: Source, x: Origin, y: Origin, path: String): Either[Diagnostic, Unit] = for {
    _ <- _source_compare(a, b, path + "/source")
    _ <- (x, y) match {
      case (Derived(ruleA, inputsA), Derived(ruleB, inputsB)) => for {
        _ <- _equal(ruleA, ruleB, path + "/origin/ruleId")
        _ <- _paired(inputsA, inputsB, path + "/origin/inputs")(_source_compare)
      } yield ()
      case _ => _equal(x, y, path + "/origin")
    }
  } yield ()
  def _observed(expected: PackageExpectation, discovered: ComponentCapabilityDiscovery.Discovered, scope: Scope): Either[Diagnostic, Unit] = _context(for {
    _ <- _equal(expected.input, discovered.acquired.input, "/package/input")
    _ <- discovered.nativeInput.scopeInput match {
      case ScopeInput.Ready(observed) => _scope_compare(scope, observed)
      case ScopeInput.Rejected(diagnostic) => Left(diagnostic)
      case null => Left(_malformed("/native/scope", "An explicit Ready scope is required."))
    }
    _ <- _coverage(expected.resources.map(_.resourcePath), discovered.entries.map(_.loaded.resourcePath), "/package/resources")
    _ <- _sequence(expected.resources.zip(discovered.entries).map { (r, entry) =>
      val projection = entry.loaded.projection
      for {
        _ <- _equal((r.producer, r.schemaVersion, r.generatedAbiVersion), (projection.producer, projection.schemaVersion, projection.generatedAbiVersion), "/" + r.resourcePath + "/producer")
        _ <- _source_compare(r.source, projection.source, "/" + r.resourcePath + "/source")
      } yield ()
    })
  } yield (), None, Some(scope.scopeId), Some(expected.input.component.componentId))
  def _application_observed(expected: ApplicationExpectation, projection: ApplicationProjection): Either[Diagnostic, Unit] = for {
    _ <- _application_expectation(expected, "/application/expected")
    _ <- _equal((expected.producer, expected.schemaVersion, expected.generatedAbiVersion), (projection.producer, projection.schemaVersion, projection.generatedAbiVersion), "/application/producer")
    _ <- Either.cond(expected.documents.size == projection.documents.size, (), _malformed("/application/documents", "Every acquired document header must be selected."))
    _ <- _sequence(expected.documents.zip(projection.documents).zipWithIndex.map { case ((a, b), i) => _source_compare(a, b.source, s"/application/documents/$i/source") })
  } yield ()
  def _snapshot_uniqueness(components: Vector[ComponentCapabilityDiscovery.Discovered]): Either[Diagnostic, Unit] = {
    val entries = components.flatMap(_.entries)
    val keys = entries.map { e => val p = e.loaded.projection; (p.component.componentId, p.source.modelAuthority, p.source.path, p.source.sourceRevision) }
    _duplicates(keys, "/components").left.map { d =>
      val i = keys.indices.find(i => keys.take(i).contains(keys(i))).get
      val p = entries(i).loaded.projection
      d.copy(scopeId = Some(entries(i).loaded.input.scopeId), component = Some(p.component.componentId), source = Some(p.source), location = "/" + entries(i).loaded.resourcePath + "/source")
    }
  }
  def _provenance(value: Provenance): Either[Diagnostic, Unit] = for {
    x <- _nonnull(value, "/provenance")
    _ <- _scope(x.scope, "/scope")
    _ <- _values(x.packages, "/packages")((p, q) => _package(p, x.scope.scopeId, q))
    _ <- _duplicates(x.packages.map(_.input), "/packages")
    _ <- x.application match {
      case ApplicationProvenance.Acquired(e) => _application_expectation(e, "/application/expected")
      case ApplicationProvenance.Unavailable(v) => _availability(v, ProjectionFamily.Application, "/application/unavailable")
      case ApplicationProvenance.Rejected(d) => _nonnull(d, "/application/diagnostic").map(_ => ())
      case null => Left(_malformed("/application", "Application provenance is required."))
    }
    _ <- _availability(x.useCase, ProjectionFamily.UseCase, "/useCase")
  } yield ()
  def _compare(expected: Provenance, observed: Provenance): Either[Diagnostic, Unit] = for {
    _ <- _provenance(expected)
    _ <- _provenance(observed)
    _ <- _scope_compare(expected.scope, observed.scope)
    _ <- _paired(expected.packages, observed.packages, "/packages") { (a, b, p) => for {
      _ <- _equal(a.input, b.input, p + "/input")
      _ <- _coverage(a.resources.map(_.resourcePath), b.resources.map(_.resourcePath), p + "/resources")
      _ <- _paired(a.resources, b.resources, p + "/resources")((x, y, q) => for {
        _ <- _equal((x.producer, x.schemaVersion, x.generatedAbiVersion), (y.producer, y.schemaVersion, y.generatedAbiVersion), q + "/producer")
        _ <- _source_compare(x.source, y.source, q + "/source")
      } yield ())
    } yield () }
    _ <- (expected.application, observed.application) match {
      case (ApplicationProvenance.Acquired(a), ApplicationProvenance.Acquired(b)) => for {
        _ <- _equal((a.producer, a.schemaVersion, a.generatedAbiVersion), (b.producer, b.schemaVersion, b.generatedAbiVersion), "/application/producer")
        _ <- _paired(a.documents, b.documents, "/application/documents")(_source_compare)
      } yield ()
      case (a, b) => _equal(a, b, "/application")
    }
    _ <- _equal(expected.useCase, observed.useCase, "/useCase")
  } yield ()
}
