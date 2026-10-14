package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import CapabilityApplicationJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationReferenceJsonPart {
  def _term(json: Json, path: String): Either[Diagnostic, TermId] = _qualified(json, "termId", path).map((v, x) => TermId(v, x))
  def _relation(json: Json, path: String): Either[Diagnostic, RelationId] = _qualified(json, "relationId", path).map((v, x) => RelationId(v, x))
  def _profile(json: Json, path: String): Either[Diagnostic, ProfileId] = _qualified(json, "profileId", path).map((v, x) => ProfileId(v, x))
  def _model_reference(json: Json, path: String): Either[Diagnostic, ModelReference] = for {
    obj <- _object(json, Vector("identity", "boundary"), path)
    identity <- _read(obj, "identity", path)(_model_id)
    boundary <- _read(obj, "boundary", path)(_boundary)
  } yield ModelReference(identity, boundary)
  def _semantic_target(json: Json, path: String): Either[Diagnostic, SemanticTarget] = for {
    obj <- _object(json, Vector("kind", "identity"), path)
    model <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("MODEL" -> true, "TERM" -> false)))
    target <- if (model) _read(obj, "identity", path)(_model_id).map(ModelTarget(_)) else _read(obj, "identity", path)(_term).map(TermTarget(_))
  } yield target
  def _model_link(json: Json, path: String): Either[Diagnostic, ModelLink] = for {
    obj <- _object(json, Vector("role", "target", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    value <- _context(for {
      role <- _read(obj, "role", path)((j, p) => _tag(j, p, ModelRole.values.toVector.map(x => x.tag -> x)))
      target <- _read(obj, "target", path)(_model_reference)
      origin <- _read(obj, "origin", path)(_origin)
    } yield ModelLink(role, target, source, origin), Some(source))
  } yield value
  def _traceability(json: Json, path: String): Either[Diagnostic, Traceability] = for {
    obj <- _object(json, Vector("role", "target", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    value <- _context(for {
      role <- _read(obj, "role", path)((j, p) => _tag(j, p, TraceabilityRole.values.toVector.map(x => x.tag -> x)))
      target <- _read(obj, "target", path)(_model_reference)
      origin <- _read(obj, "origin", path)(_origin)
    } yield Traceability(role, target, source, origin), Some(source))
  } yield value
  def _grounding(json: Json, path: String): Either[Diagnostic, Grounding] = for {
    obj <- _object(json, Vector("target", "boundary", "relation", "profile", "context", "preferredLabel", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    value <- _context(for {
      target <- _read(obj, "target", path)(_semantic_target)
      boundary <- _read(obj, "boundary", path)(_boundary)
      relation <- _read(obj, "relation", path)((j, p) => _presence(j, p)(_relation))
      profile <- _read(obj, "profile", path)((j, p) => _presence(j, p)(_profile))
      context <- _read(obj, "context", path)((j, p) => _presence(j, p)(_string))
      label <- _read(obj, "preferredLabel", path)((j, p) => _presence(j, p)(_label))
      origin <- _read(obj, "origin", path)(_origin)
    } yield Grounding(target, boundary, relation, profile, context, label, source, origin), Some(source))
  } yield value
}
