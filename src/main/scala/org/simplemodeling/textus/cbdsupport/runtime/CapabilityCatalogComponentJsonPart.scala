package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{InvalidPathException, Paths}
import io.circe.Json
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityProjection.*
import ComponentCapabilityPackageSource.*
import CapabilityCatalogModel.*
import CapabilityComponentUsageJsonPart.*
import CapabilityComponentUsageReferenceJsonPart._model_reference

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogComponentJsonPart {
  private def _component_identity(json: Json, fields: Vector[String], path: String): Either[Diagnostic, ComponentId] = for {
    obj <- _object(json, fields, path)
    namespace <- _read(obj, "namespace", path)(_string)
    id <- _read(obj, "id", path)(_string)
    component <- ComponentId.parseC(namespace + "." + id).toOption.toRight(_malformed(path, "A canonical Component identity is required."))
    _ <- Either.cond(component.namespace.value() == namespace && component.localId.value() == id, (), _malformed(path, "Separate canonical Component fields are required."))
  } yield component
  def _component_ref(json: Json, path: String): Either[Diagnostic, ComponentId] = _component_identity(json, Vector("namespace", "id"), path)
  private def _coordinate(json: Json, path: String): Either[Diagnostic, ComponentCoordinate] = for {
    component <- _component_identity(json, Vector("namespace", "id", "version"), path)
    obj <- _object(json, Vector("namespace", "id", "version"), path)
    version <- _read(obj, "version", path)(_string)
  } yield ComponentCoordinate(component, version)
  private def _package_coordinate(json: Json, path: String): Either[Diagnostic, PackageCoordinate] = for {
    obj <- _object(json, Vector("coordinate", "version"), path)
    coordinate <- _read(obj, "coordinate", path)(_string)
    version <- _read(obj, "version", path)(_string)
  } yield PackageCoordinate(coordinate, version)
  private def _location(json: Json, path: String): Either[Diagnostic, Location] = for {
    obj <- _object(json, Vector("kind", "path"), path)
    directory <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("DIRECTORY" -> true, "CAR" -> false)))
    text <- _read(obj, "path", path) { (j, p) => j.asString.toRight(_malformed(p, "An original location string is required.")) }
    value <- try Right(Paths.get(text)) catch { case _: InvalidPathException => Left(_malformed(path + "/path", "Invalid location path.")) }
  } yield if (directory) Location.Directory(value) else Location.Car(value)
  def _input(json: Json, path: String): Either[Diagnostic, PackageInput.Selected] = for {
    obj <- _object(json, Vector("scopeId", "location", "component", "package"), path)
    scope <- _read(obj, "scopeId", path)(_string)
    location <- _read(obj, "location", path)(_location)
    component <- _read(obj, "component", path)(_coordinate)
    pkg <- _read(obj, "package", path)(_package_coordinate)
  } yield PackageInput.Selected(scope, location, component, pkg)
  def _capability_reference(json: Json, path: String): Either[Diagnostic, CapabilityReference] = for {
    obj <- _object(json, Vector("kind", "identity", "contractVersion"), path)
    _ <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("COMPONENT" -> ())))
    identity <- _read(obj, "identity", path)(_model_id)
    version <- _read(obj, "contractVersion", path)(_string)
  } yield CapabilityReference(identity, version)
  private def _key(json: Json, path: String): Either[Diagnostic, DeclarationKey] = for {
    obj <- _object(json, Vector("component", "direction", "capability"), path)
    component <- _read(obj, "component", path)(_component_ref)
    direction <- _read(obj, "direction", path)((j, p) => _tag(j, p, Direction.values.toVector.map(x => x.tag -> x)))
    capability <- _read(obj, "capability", path)(_capability_reference)
  } yield DeclarationKey(component, direction, capability)
  private def _realization(json: Json, path: String): Either[Diagnostic, Realization] = for {
    obj <- _object(json, Vector("declarationKey", "targetKind", "target", "targetComponent", "targetContractVersion", "generatedAbiVersion", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      key <- _read(obj, "declarationKey", path)(_key)
      kind <- _read(obj, "targetKind", path)(CapabilityCatalogScopeJsonPart._target_kind)
      target <- _read(obj, "target", path)(_model_reference)
      component <- _read(obj, "targetComponent", path)(_component_ref)
      version <- _read(obj, "targetContractVersion", path)(_string)
      abi <- _read(obj, "generatedAbiVersion", path)(_string)
      origin <- _read(obj, "origin", path)(_origin)
    } yield Realization(key, kind, target, component, version, abi, source, origin), Some(source))
  } yield result
  private def _fact(json: Json, path: String): Either[Diagnostic, ResolvedRealizationFact] = for {
    obj <- _object(json, Vector("edge", "target"), path)
    edge <- _read(obj, "edge", path)(_realization)
    target <- _read(obj, "target", path)(CapabilityCatalogScopeJsonPart._target_contract)
  } yield ResolvedRealizationFact(edge, target)
  private def _resource(json: Json, path: String): Either[Diagnostic, ResourceProjection] = for {
    obj <- _object(json, Vector("resourcePath", "projection", "resolvedDefinitions", "realizations"), path)
    resource <- _read(obj, "resourcePath", path)((j, p) => _string(j, p).flatMap(x => _path(x, p).map(_ => x)))
    projection <- _read(obj, "projection", path)((j, p) => ComponentCapabilityProjectionCodec.decode(j.noSpaces).left.map(d => d.copy(location = p + d.location)))
    definitions <- _read(obj, "resolvedDefinitions", path)((j, p) => _vector(j, p)(CapabilityCatalogScopeJsonPart._definition))
    facts <- _read(obj, "realizations", path)((j, p) => _vector(j, p)(_fact))
  } yield ResourceProjection(resource, projection, definitions, facts)
  def _package(json: Json, path: String): Either[Diagnostic, PackageProjection] = for {
    obj <- _object(json, Vector("input", "targetContracts", "resources"), path)
    input <- _read(obj, "input", path)(_input)
    contracts <- _read(obj, "targetContracts", path)((j, p) => _vector(j, p)(CapabilityCatalogScopeJsonPart._target_contract))
    resources <- _read(obj, "resources", path)((j, p) => _vector(j, p)(_resource))
  } yield PackageProjection(input, contracts, resources)
}
