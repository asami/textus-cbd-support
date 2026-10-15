package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonPart.*
import CapabilityComponentUsageReferenceJsonPart.*
import CapabilityCatalogComponentJsonPart.{_input, _component_ref, _capability_reference}

/*
 * Closed internal envelopes; embedded upstream transports retain their codecs.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogJsonPart {
  def decode(json: Json): Either[Diagnostic, PortableProjection] = for {
    header <- json.asObject.toRight(_malformed("/", "A catalog projection object is required."))
    schema <- _read(header, "schemaVersion", "/")(_integer)
    _ <- Either.cond(schema == 1, (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported internal catalog schema.", "/schemaVersion"))
    obj <- _object(json, Vector("schemaVersion", "snapshotReference", "provenance", "packages", "usage", "useCase"), "/")
    reference <- _read(obj, "snapshotReference", "/")(_snapshot_reference)
    provenance <- _read(obj, "provenance", "/")(_provenance)
    packages <- _read(obj, "packages", "/")((j, p) => _vector(j, p)(CapabilityCatalogComponentJsonPart._package))
    usage <- _read(obj, "usage", "/")((j, p) => _vector(j, p)(_usage_package))
    usecase <- _read(obj, "useCase", "/")(_unavailable)
  } yield PortableProjection(schema, reference, provenance, packages, usage, usecase)
  private def _snapshot_reference(json: Json, path: String): Either[Diagnostic, SnapshotReference] = for {
    obj <- _object(json, Vector("recordId", "recordRevision"), path)
    id <- _read(obj, "recordId", path)((j, p) => _string(j, p).flatMap(x => InternalModelRecordId.from(x).left.map(_malformed(p, _))))
    revision <- _read(obj, "recordRevision", path) { (j, p) =>
      j.asNumber.filter(_.toString.matches("-?(0|[1-9][0-9]*)")).flatMap(_.toLong)
        .toRight(_malformed(p, "An integral positive Long revision is required."))
        .flatMap(x => InternalModelRecordRevision.from(x).left.map(_malformed(p, _)))
    }
    reference <- SnapshotReference.from(InternalModelRecordReference(id, revision)).left.map(d => d.copy(location = path + d.location))
  } yield reference
  private def _provenance(json: Json, path: String): Either[Diagnostic, Provenance] = for {
    obj <- _object(json, Vector("scope", "packages", "useCase"), path)
    scope <- _read(obj, "scope", path)(CapabilityCatalogScopeJsonPart._scope)
    packages <- _read(obj, "packages", path)((j, p) => _vector(j, p)(_package_expectation))
    usecase <- _read(obj, "useCase", path)(_unavailable)
  } yield Provenance(scope, packages, usecase)
  private def _package_expectation(json: Json, path: String): Either[Diagnostic, PackageExpectation] = for {
    obj <- _object(json, Vector("input", "resources", "usage"), path)
    input <- _read(obj, "input", path)(_input)
    resources <- _read(obj, "resources", path)((j, p) => _vector(j, p)(_resource_expectation))
    usage <- _read(obj, "usage", path)((j, p) => if (j.isNull) Right(None) else _usage_expectation(j, p).map(Some(_)))
  } yield PackageExpectation(input, resources, usage)
  private def _resource_expectation(json: Json, path: String): Either[Diagnostic, ResourceExpectation] = for {
    obj <- _object(json, Vector("resourcePath", "producer", "schemaVersion", "generatedAbiVersion", "source"), path)
    resource <- _read(obj, "resourcePath", path)(_string)
    producer <- _read(obj, "producer", path)(_producer)
    schema <- _read(obj, "schemaVersion", path)(_integer)
    abi <- _read(obj, "generatedAbiVersion", path)(_string)
    source <- _read(obj, "source", path)(_source)
  } yield ResourceExpectation(resource, producer, schema, abi, source)
  private def _usage_expectation(json: Json, path: String): Either[Diagnostic, ComponentUsageExpectation] = for {
    obj <- _object(json, Vector("producer", "schemaVersion", "generatedAbiVersion", "documents"), path)
    producer <- _read(obj, "producer", path)(_producer)
    schema <- _read(obj, "schemaVersion", path)(_integer)
    abi <- _read(obj, "generatedAbiVersion", path)(_string)
    documents <- _read(obj, "documents", path)((j, p) => _vector(j, p)(_source))
  } yield ComponentUsageExpectation(producer, schema, abi, documents)
  private def _usage_package(json: Json, path: String): Either[Diagnostic, ComponentUsagePackage] = for {
    obj <- _object(json, Vector("input", "resourcePath", "family"), path)
    input <- _read(obj, "input", path)(_input)
    resource <- _read(obj, "resourcePath", path)(_string)
    family <- _read(obj, "family", path)(_usage_family)
  } yield ComponentUsagePackage(input, resource, family)
  private def _usage_family(json: Json, path: String): Either[Diagnostic, ComponentUsageFamily] = for {
    header <- json.asObject.toRight(_malformed(path, "An explicit ComponentUsage family object is required."))
    available <- _read(header, "status", path)((j, p) => _tag(j, p, Vector("AVAILABLE" -> true, "UNAVAILABLE" -> false)))
    value <- if (available) for {
      obj <- _object(json, Vector("status", "projection", "unresolvedExternal"), path)
      projection <- _read(obj, "projection", path)((j, p) => CapabilityComponentUsageProjectionCodec.decode(j.noSpaces).left.map(d => d.copy(location = p + d.location)))
      external <- _read(obj, "unresolvedExternal", path)((j, p) => _vector(j, p)(_external_reference))
    } yield ComponentUsageFamily.Available(projection, external) else for {
      obj <- _object(json, Vector("status", "value"), path)
      v <- _read(obj, "value", path)(_unavailable)
    } yield ComponentUsageFamily.Unavailable(v)
  } yield value
  private def _unavailable(json: Json, path: String): Either[Diagnostic, UnavailableUpstreamProjection] = for {
    obj <- _object(json, Vector("family", "reason", "detail"), path)
    family <- _read(obj, "family", path)((j, p) => _tag(j, p, Vector("COMPONENT_USAGE" -> ProjectionFamily.ComponentUsage, "USE_CASE" -> ProjectionFamily.UseCase)))
    reason <- _read(obj, "reason", path)(_string)
    detail <- _read(obj, "detail", path)(_string)
  } yield UnavailableUpstreamProjection(family, reason, detail)
  private def _external_reference(json: Json, path: String): Either[Diagnostic, ExternalReference] = for {
    obj <- _object(json, Vector("location", "target", "source", "origin"), path)
    location <- _read(obj, "location", path)(_string)
    target <- _read(obj, "target", path)(_external_target)
    source <- _read(obj, "source", path)(_source)
    origin <- _read(obj, "origin", path)(_origin)
  } yield ExternalReference(location, target, source, origin)
  private def _external_target(json: Json, path: String): Either[Diagnostic, ExternalTarget] = for {
    header <- json.asObject.toRight(_malformed(path, "An External target object is required."))
    kind <- _read(header, "kind", path)((j, p) => _tag(j, p, Vector("MODEL" -> 0, "TERM" -> 1, "RELATION" -> 2, "PROFILE" -> 3, "COMPONENT_CAPABILITY" -> 4, "COMPONENT_USAGE_CAPABILITY" -> 5)))
    obj <- _object(json, Vector("kind", if (kind < 4) "identity" else "reference"), path)
    target <- kind match {
      case 0 => _read(obj, "identity", path)(_model_id).map(ExternalTarget.Model(_))
      case 1 => _read(obj, "identity", path)(_term).map(ExternalTarget.Term(_))
      case 2 => _read(obj, "identity", path)(_relation).map(ExternalTarget.Relation(_))
      case 3 => _read(obj, "identity", path)(_profile).map(ExternalTarget.Profile(_))
      case 4 => _read(obj, "reference", path)(_capability_reference).map(ExternalTarget.ComponentCapability(_))
      case _ => _read(obj, "reference", path)(_usage_reference).map(ExternalTarget.ComponentUsageCapability(_))
    }
  } yield target
  private def _usage_reference(json: Json, path: String): Either[Diagnostic, ComponentUsageCapabilityReference] = for {
    obj <- _object(json, Vector("kind", "identity", "contractVersion"), path)
    _ <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("COMPONENT_USAGE" -> ())))
    identity <- _read(obj, "identity", path)(_model_id)
    version <- _read(obj, "contractVersion", path)(_string)
  } yield ComponentUsageCapabilityReference(identity, version)
}
