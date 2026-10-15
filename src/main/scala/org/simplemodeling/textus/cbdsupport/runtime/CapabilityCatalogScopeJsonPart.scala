package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityComponentUsageJsonPart.*
import CapabilityComponentUsageReferenceJsonPart.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogScopeJsonPart {
  def _definition(json: Json, path: String): Either[Diagnostic, CapabilityDefinition] = for {
    obj <- _object(json, Vector("kind", "identity", "contractVersion", "name", "summary", "description", "source", "discoveryReferences", "grounding", "traceability"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      _ <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("COMPONENT" -> ())))
      identity <- _read(obj, "identity", path)(_model_id)
      version <- _read(obj, "contractVersion", path)(_string)
      name <- _read(obj, "name", path)(_string)
      summary <- _read(obj, "summary", path)((j, p) => _presence(j, p)(_string))
      description <- _read(obj, "description", path)((j, p) => _presence(j, p)(_string))
      discovery <- _read(obj, "discoveryReferences", path)((j, p) => _presence(j, p)((v, q) => _vector(v, q)(_model_link)))
      grounding <- _read(obj, "grounding", path)((j, p) => _presence(j, p)((v, q) => _vector(v, q)(_grounding)))
      trace <- _read(obj, "traceability", path)((j, p) => _presence(j, p)((v, q) => _vector(v, q)(_traceability)))
      value = CapabilityDefinition(identity, version, name, summary, description, source, discovery, grounding, trace)
      _ <- CapabilityIngestionSelectionPart._definition(value, path)
    } yield value, Some(source))
  } yield result
  def _target_contract(json: Json, path: String): Either[Diagnostic, TargetContract] = for {
    obj <- _object(json, Vector("targetKind", "identity", "component", "contractVersion", "generatedAbiVersion", "source"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      kind <- _read(obj, "targetKind", path)(_target_kind)
      identity <- _read(obj, "identity", path)(_model_id)
      component <- _read(obj, "component", path)(CapabilityCatalogComponentJsonPart._component_ref)
      version <- _read(obj, "contractVersion", path)(_string)
      abi <- _read(obj, "generatedAbiVersion", path)(_string)
      _ <- _authority(source, identity.modelId, path + "/source")
    } yield TargetContract(kind, identity, component, version, abi, source), Some(source))
  } yield result
  def _target_kind(json: Json, path: String): Either[Diagnostic, TargetKind] = _tag(json, path, TargetKind.values.toVector.map(x => x.tag -> x))
  def _scope(json: Json, path: String): Either[Diagnostic, Scope] = for {
    obj <- _object(json, Vector("scopeId", "source", "modelFacts", "termFacts", "relationFacts", "profileFacts", "capabilities", "targetContracts"), path)
    id <- _read(obj, "scopeId", path)(_string)
    source <- _read(obj, "source", path)(_source)
    models <- _read(obj, "modelFacts", path)((j, p) => _vector(j, p)(_model_fact))
    terms <- _read(obj, "termFacts", path)((j, p) => _vector(j, p) { (v, q) => for {
      f <- _object(v, Vector("identity", "source"), q); i <- _read(f, "identity", q)(_term); s <- _read(f, "source", q)(_source)
    } yield TermFact(i, s) })
    relations <- _read(obj, "relationFacts", path)((j, p) => _vector(j, p) { (v, q) => for {
      f <- _object(v, Vector("identity", "source"), q); i <- _read(f, "identity", q)(_relation); s <- _read(f, "source", q)(_source)
    } yield RelationFact(i, s) })
    profiles <- _read(obj, "profileFacts", path)((j, p) => _vector(j, p) { (v, q) => for {
      f <- _object(v, Vector("identity", "source"), q); i <- _read(f, "identity", q)(_profile); s <- _read(f, "source", q)(_source)
    } yield ProfileFact(i, s) })
    capabilities <- _read(obj, "capabilities", path)((j, p) => _vector(j, p)(_definition))
    targets <- _read(obj, "targetContracts", path)((j, p) => _vector(j, p)(_target_contract))
  } yield Scope(id, source, models, terms, relations, profiles, capabilities, targets)
  private def _model_fact(json: Json, path: String): Either[Diagnostic, ModelFact] = for {
    obj <- _object(json, Vector("identity", "kind", "source"), path)
    identity <- _read(obj, "identity", path)(_model_id)
    kind <- _read(obj, "kind", path)((j, p) => _tag(j, p, ModelKind.values.toVector.map(x => x.tag -> x)))
    source <- _read(obj, "source", path)(_source)
  } yield ModelFact(identity, kind, source)
}
