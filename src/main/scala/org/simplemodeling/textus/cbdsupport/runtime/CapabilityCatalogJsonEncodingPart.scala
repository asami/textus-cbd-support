package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityProjection.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonEncodingPart.*
import CapabilityComponentUsageJsonPart.{_malformed, _nonnull, _sequence}

/*
 * The carrier contains semantic references only; native values have no encoder.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogJsonEncodingPart {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private def _embedded(value: Either[Diagnostic, String], path: String): Either[Diagnostic, Json] =
    value.left.map(d => d.copy(location = path + d.location)).flatMap(s => _parser.parse(s).left.map(_ => _malformed(path, "Invalid embedded projection JSON.")))
  def encode(projection: PortableProjection): Either[Diagnostic, Json] = for {
    p <- _nonnull(projection, "/")
    packages <- _nonnull(p.packages, "/packages")
    encoded <- _sequence(packages.zipWithIndex.map((pkg, i) => _package(pkg, s"/packages/$i")))
    usage <- _nonnull(p.usage, "/usage")
    encodedusage <- _sequence(usage.zipWithIndex.map((pkg, i) => _usage_package(pkg, s"/usage/$i")))
  } yield Json.obj("schemaVersion" -> Json.fromInt(p.schemaVersion), "snapshotReference" -> _snapshot_reference(p.snapshotReference),
    "provenance" -> _provenance(p.provenance), "packages" -> Json.fromValues(encoded), "usage" -> Json.fromValues(encodedusage), "useCase" -> _unavailable(p.useCase))
  private def _snapshot_reference(value: SnapshotReference): Json = _nullable(value)(x => _nullable(x.value)(r => Json.obj(
    "recordId" -> _nullable(r.recordId)(id => _string(id.value)), "recordRevision" -> _nullable(r.recordRevision)(v => Json.fromLong(v.value)))))
  private def _component_ref(value: ComponentId): Json = _nullable(value)(x => Json.obj("namespace" -> _string(x.namespace.value()), "id" -> _string(x.localId.value())))
  private def _coordinate(value: ComponentCoordinate): Json = _nullable(value)(x => _nullable(x.componentId)(c => Json.obj(
    "namespace" -> _string(c.namespace.value()), "id" -> _string(c.localId.value()), "version" -> _string(x.version))))
  private def _package_coordinate(value: PackageCoordinate): Json = _nullable(value)(x => Json.obj("coordinate" -> _string(x.coordinate), "version" -> _string(x.version)))
  private def _location(value: Location): Json = _nullable(value) {
    case Location.Directory(root) => Json.obj("kind" -> Json.fromString("DIRECTORY"), "path" -> _nullable(root)(p => Json.fromString(p.toString)))
    case Location.Car(path) => Json.obj("kind" -> Json.fromString("CAR"), "path" -> _nullable(path)(p => Json.fromString(p.toString)))
  }
  private def _input(value: PackageInput.Selected): Json = _nullable(value)(x => Json.obj("scopeId" -> _string(x.scopeId),
    "location" -> _location(x.location), "component" -> _coordinate(x.component), "package" -> _package_coordinate(x.packageCoordinate)))
  private def _definition(value: CapabilityDefinition): Json = _nullable(value)(x => Json.obj("kind" -> Json.fromString("COMPONENT"),
    "identity" -> _model_id(x.identity), "contractVersion" -> _string(x.contractVersion), "name" -> _string(x.name),
    "summary" -> _presence(x.summary)(_string), "description" -> _presence(x.description)(_string), "source" -> _source(x.source),
    "discoveryReferences" -> _presence(x.discoveryReferences)(xs => _vector(xs)(_model_link)), "grounding" -> _presence(x.grounding)(xs => _vector(xs)(_grounding)),
    "traceability" -> _presence(x.traceability)(xs => _vector(xs)(_traceability))))
  private def _target_contract(value: TargetContract): Json = _nullable(value)(x => Json.obj("targetKind" -> _nullable(x.targetKind)(k => _string(k.tag)),
    "identity" -> _model_id(x.identity), "component" -> _component_ref(x.component), "contractVersion" -> _string(x.contractVersion),
    "generatedAbiVersion" -> _string(x.generatedAbiVersion), "source" -> _source(x.source)))
  private def _capability_reference(value: CapabilityReference): Json = _nullable(value)(x => Json.obj("kind" -> Json.fromString("COMPONENT"),
    "identity" -> _model_id(x.identity), "contractVersion" -> _string(x.contractVersion)))
  private def _usage_reference(value: ComponentUsageCapabilityReference): Json = _nullable(value)(x => Json.obj("kind" -> Json.fromString("COMPONENT_USAGE"),
    "identity" -> _model_id(x.identity), "contractVersion" -> _string(x.contractVersion)))
  private def _key(value: DeclarationKey): Json = _nullable(value)(x => Json.obj("component" -> _component_ref(x.component),
    "direction" -> _nullable(x.direction)(d => _string(d.tag)), "capability" -> _capability_reference(x.capability)))
  private def _realization(value: Realization): Json = _nullable(value)(x => Json.obj("declarationKey" -> _key(x.declarationKey),
    "targetKind" -> _nullable(x.targetKind)(k => _string(k.tag)), "target" -> _model_reference(x.target), "targetComponent" -> _component_ref(x.targetComponent),
    "targetContractVersion" -> _string(x.targetContractVersion), "generatedAbiVersion" -> _string(x.generatedAbiVersion), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  private def _package(value: PackageProjection, path: String): Either[Diagnostic, Json] = for {
    pkg <- _nonnull(value, path)
    resources <- _nonnull(pkg.resources, path + "/resources")
    encoded <- _sequence(resources.zipWithIndex.map { (value, i) => for {
      r <- _nonnull(value, s"$path/resources/$i")
      projection <- _embedded(ComponentCapabilityProjectionCodec.encode(r.projection), s"$path/resources/$i/projection")
    } yield Json.obj("resourcePath" -> _string(r.resourcePath), "projection" -> projection, "resolvedDefinitions" -> _vector(r.resolvedDefinitions)(_definition),
      "realizations" -> _vector(r.realizations)(x => Json.obj("edge" -> _realization(x.edge), "target" -> _target_contract(x.target)))) })
  } yield Json.obj("input" -> _input(pkg.input), "targetContracts" -> _vector(pkg.targetContracts)(_target_contract), "resources" -> Json.fromValues(encoded))
  private def _scope(value: Scope): Json = _nullable(value)(x => Json.obj("scopeId" -> _string(x.scopeId), "source" -> _source(x.source),
    "modelFacts" -> _vector(x.modelFacts)(f => Json.obj("identity" -> _model_id(f.identity), "kind" -> _nullable(f.kind)(k => _string(k.tag)), "source" -> _source(f.source))),
    "termFacts" -> _vector(x.termFacts)(f => Json.obj("identity" -> _term(f.identity), "source" -> _source(f.source))),
    "relationFacts" -> _vector(x.relationFacts)(f => Json.obj("identity" -> _relation(f.identity), "source" -> _source(f.source))),
    "profileFacts" -> _vector(x.profileFacts)(f => Json.obj("identity" -> _profile(f.identity), "source" -> _source(f.source))),
    "capabilities" -> _vector(x.capabilities)(_definition), "targetContracts" -> _vector(x.targetContracts)(_target_contract)))
  private def _provenance(value: Provenance): Json = _nullable(value)(x => Json.obj("scope" -> _scope(x.scope),
    "packages" -> _vector(x.packages)(p => Json.obj("input" -> _input(p.input), "resources" -> _vector(p.resources)(_resource_expectation), "usage" -> _option(p.usage)(_usage_expectation))),
    "useCase" -> _unavailable(x.useCase)))
  private def _resource_expectation(value: ResourceExpectation): Json = _nullable(value)(x => Json.obj("resourcePath" -> _string(x.resourcePath),
    "producer" -> _producer(x.producer), "schemaVersion" -> Json.fromInt(x.schemaVersion), "generatedAbiVersion" -> _string(x.generatedAbiVersion), "source" -> _source(x.source)))
  private def _usage_expectation(value: ComponentUsageExpectation): Json = _nullable(value)(x => Json.obj("producer" -> _producer(x.producer),
    "schemaVersion" -> Json.fromInt(x.schemaVersion), "generatedAbiVersion" -> _string(x.generatedAbiVersion), "documents" -> _vector(x.documents)(_source)))
  private def _usage_package(value: ComponentUsagePackage, path: String): Either[Diagnostic, Json] = for {
    pkg <- _nonnull(value, path)
    family <- _usage(pkg.family, path + "/family")
  } yield Json.obj("input" -> _input(pkg.input), "resourcePath" -> _string(pkg.resourcePath), "family" -> family)
  private def _usage(value: ComponentUsageFamily, path: String): Either[Diagnostic, Json] = value match {
    case ComponentUsageFamily.Available(p, external) => _embedded(CapabilityComponentUsageProjectionCodec.encode(p), path + "/projection").map(j =>
      Json.obj("status" -> Json.fromString("AVAILABLE"), "projection" -> j, "unresolvedExternal" -> _vector(external)(_external_reference)))
    case ComponentUsageFamily.Unavailable(v) => Right(Json.obj("status" -> Json.fromString("UNAVAILABLE"), "value" -> _unavailable(v)))
    case null => Left(_malformed(path, "An explicit Component Usage family is required."))
  }
  private def _unavailable(value: UnavailableUpstreamProjection): Json = _nullable(value)(x => Json.obj(
    "family" -> _nullable(x.family)(f => _string(if (f == ProjectionFamily.ComponentUsage) "COMPONENT_USAGE" else "USE_CASE")), "reason" -> _string(x.reason), "detail" -> _string(x.detail)))
  private def _external_reference(value: ExternalReference): Json = _nullable(value)(x => Json.obj("location" -> _string(x.location), "target" -> _external_target(x.target), "source" -> _source(x.source), "origin" -> _origin(x.origin)))
  private def _external_target(value: ExternalTarget): Json = _nullable(value) {
    case ExternalTarget.Model(i) => Json.obj("kind" -> Json.fromString("MODEL"), "identity" -> _model_id(i))
    case ExternalTarget.Term(i) => Json.obj("kind" -> Json.fromString("TERM"), "identity" -> _term(i))
    case ExternalTarget.Relation(i) => Json.obj("kind" -> Json.fromString("RELATION"), "identity" -> _relation(i))
    case ExternalTarget.Profile(i) => Json.obj("kind" -> Json.fromString("PROFILE"), "identity" -> _profile(i))
    case ExternalTarget.ComponentCapability(r) => Json.obj("kind" -> Json.fromString("COMPONENT_CAPABILITY"), "reference" -> _capability_reference(r))
    case ExternalTarget.ComponentUsageCapability(r) => Json.obj("kind" -> Json.fromString("COMPONENT_USAGE_CAPABILITY"), "reference" -> _usage_reference(r))
  }
}
