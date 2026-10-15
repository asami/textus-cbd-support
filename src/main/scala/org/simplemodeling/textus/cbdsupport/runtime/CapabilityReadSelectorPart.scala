package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.ModelElementId
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityIngestionModel.ComponentUsageCapabilityReference
import CapabilityComponentUsageJsonPart.*
import CapabilityCatalogComponentJsonPart.{_capability_reference, _coordinate, _package_coordinate, _key}

/*
 * Closed selectors; authority and source configuration never come from callers.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityReadSelectorPart {
  enum Selector {
    case Overview
    case Get(lookup: CapabilityLookup)
    case Providers(reference: CapabilityReference)
    case Requirements(component: ComponentCoordinate)
    case Usage(reference: ComponentUsageCapabilityReference)
    case UseCase(reference: ModelElementId)
    case Diagnose(selection: DemandSelection)
  }
  val operations: Set[String] = Set("getCapabilityOverview", "getCapability", "findProviders", "findRequirements",
    "traceComponentUsageCapability", "traceUseCaseDemand", "diagnoseCapabilityDemand")
  private val _parser = JawnParser(allowDuplicateKeys = false)

  def parse(text: String): Either[Diagnostic, Json] =
    _nonnull(text, "/").flatMap(s => _parser.parse(s).left.map(_ => _malformed("/", "Invalid JSON or duplicate object key.")))

  def decode(operation: String, text: String): Either[Diagnostic, Selector] = parse(text).flatMap { json =>
    operation match {
      case "getCapabilityOverview" => _object(json, Vector.empty, "/").map(_ => Selector.Overview)
      case "getCapability" => for {
        header <- json.asObject.toRight(_malformed("/", "A selector object is required."))
        kind <- _read(header, "kind", "/")(_string)
        lookup <- kind match {
          case "EXACT" => _reference(json, Vector("kind", "reference"))(_capability_reference).map(CapabilityLookup.Exact(_))
          case "SEARCH" => for {
            obj <- _object(json, Vector("kind", "text"), "/")
            value <- _read(obj, "text", "/")((j, p) => j.asString.filter(_.nonEmpty).toRight(_malformed(p, "Nonempty search text is required.")))
          } yield CapabilityLookup.Search(value)
          case _ => Left(_malformed("/kind", "EXACT or SEARCH is required."))
        }
      } yield Selector.Get(lookup)
      case "findProviders" => _reference(json, Vector("reference"))(_capability_reference).map(Selector.Providers(_))
      case "findRequirements" => for {
        obj <- _object(json, Vector("component"), "/")
        component <- _read(obj, "component", "/")(_coordinate)
      } yield Selector.Requirements(component)
      case "traceComponentUsageCapability" => _reference(json, Vector("reference"))(CapabilityCatalogJsonPart._usage_reference).map(Selector.Usage(_))
      case "traceUseCaseDemand" => _reference(json, Vector("reference"))(_model_id).map(Selector.UseCase(_))
      case "diagnoseCapabilityDemand" => _demand(json).map(Selector.Diagnose(_))
      case _ => Left(_malformed("/operation", "Unknown read operation."))
    }
  }

  private def _reference[A](json: Json, fields: Vector[String])(decode: (Json, String) => Either[Diagnostic, A]): Either[Diagnostic, A] =
    _object(json, fields, "/").flatMap(obj => _read(obj, "reference", "/")(decode))

  private def _demand(json: Json): Either[Diagnostic, DemandSelection] = for {
    header <- json.asObject.toRight(_malformed("/", "A selector object is required."))
    kind <- _read(header, "kind", "/")(_string)
    selection <- kind match {
      case "COMPONENT_USAGE" => _reference(json, Vector("kind", "reference"))(CapabilityCatalogJsonPart._usage_reference).map(DemandSelection.ComponentUsage(_))
      case "USE_CASE" => _reference(json, Vector("kind", "reference"))(_model_id).map(DemandSelection.UseCase(_))
      case "REQUIRED" => for {
        obj <- _object(json, Vector("kind", "key"), "/")
        key <- _read(obj, "key", "/")(_record_key)
      } yield DemandSelection.Required(key)
      case _ => Left(_malformed("/kind", "COMPONENT_USAGE, REQUIRED or USE_CASE is required."))
    }
  } yield selection

  private def _record_key(json: Json, path: String): Either[Diagnostic, DeclarationRecordKey] = for {
    obj <- _object(json, Vector("context", "declaration"), path)
    context <- _read(obj, "context", path)(_context_key)
    declaration <- _read(obj, "declaration", path)(_key)
  } yield DeclarationRecordKey(context, declaration)

  private def _context_key(json: Json, path: String): Either[Diagnostic, ComponentContextKey] = for {
    obj <- _object(json, Vector("scopeId", "component", "packageCoordinate", "resourcePath", "source"), path)
    scope <- _read(obj, "scopeId", path)(_string)
    component <- _read(obj, "component", path)(_coordinate)
    pkg <- _read(obj, "packageCoordinate", path)(_package_coordinate)
    resource <- _read(obj, "resourcePath", path)((j, p) => _string(j, p).flatMap(s => _path(s, p).map(_ => s)))
    source <- _read(obj, "source", path)(_source_key)
  } yield ComponentContextKey(scope, component, pkg, resource, source)

  private def _source_key(json: Json, path: String): Either[Diagnostic, SourceKey] = for {
    obj <- _object(json, Vector("modelAuthority", "path", "sourceRevision"), path)
    authority <- _read(obj, "modelAuthority", path)(_string)
    resource <- _read(obj, "path", path)((j, p) => _string(j, p).flatMap(s => _path(s, p).map(_ => s)))
    revision <- _read(obj, "sourceRevision", path)(_string)
  } yield SourceKey(authority, resource, revision)
}
