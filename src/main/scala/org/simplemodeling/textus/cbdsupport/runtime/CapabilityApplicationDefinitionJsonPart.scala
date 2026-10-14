package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.CapabilityReference
import CapabilityIngestionModel.*
import CapabilityApplicationJsonPart.*
import CapabilityApplicationReferenceJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationDefinitionJsonPart {
  private val _abi = "cozy.application-capability.v1"
  def _decode(json: Json): Either[Diagnostic, ApplicationProjection] = for {
    // Header dispatch precedes closed v1 document decoding.
    header <- json.asObject.toRight(_malformed("/", "A projection object is required."))
    schema <- _read(header, "schemaVersion", "/")(_integer)
    _ <- Either.cond(schema == 1, (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported Application schema.", "/schemaVersion"))
    abi <- _read(header, "generatedAbiVersion", "/")(_string)
    _ <- Either.cond(abi == _abi, (), _diagnostic(DiagnosticKind.Unsupported, "Unsupported Application ABI.", "/generatedAbiVersion"))
    obj <- _object(json, Vector("schemaVersion", "generatedAbiVersion", "producer", "documents"), "/")
    producer <- _read(obj, "producer", "/")(_producer)
    documents <- _read(obj, "documents", "/")((j, p) => _vector(j, p)(_document))
    _ <- _unique(documents.zipWithIndex.map((x, i) => ((x.source.modelAuthority, x.source.path, x.source.sourceRevision), x.source, s"/documents/$i/source")))
    _ <- _unique(documents.zipWithIndex.flatMap { (doc, i) => doc.applicationCapabilities.zipWithIndex.map { (x, k) =>
      ((x.identity, x.contractVersion), x.source, s"/documents/$i/applicationCapabilities/$k")
    } })
  } yield ApplicationProjection(schema, abi, producer, documents)
  private def _unique[K](values: Vector[(K, Source, String)]): Either[Diagnostic, Unit] =
    values.indices.find(i => values.take(i).exists(_._1 == values(i)._1)) match {
      case Some(i) => Left(_diagnostic(DiagnosticKind.Ambiguous, "Repeated exact owning key.", values(i)._3).copy(source = Some(values(i)._2)))
      case None => Right(())
    }
  private def _document(json: Json, path: String): Either[Diagnostic, ApplicationDocument] = for {
    obj <- _object(json, Vector("source", "applicationCapabilities"), path)
    source <- _read(obj, "source", path)(_source)
    definitions <- _context(_read(obj, "applicationCapabilities", path)((j, p) => _vector(j, p)((x, q) => _definition(x, source, q))), Some(source))
  } yield ApplicationDocument(source, definitions)
  private def _definition(json: Json, header: Source, path: String): Either[Diagnostic, ApplicationDefinition] = for {
    obj <- _object(json, Vector("kind", "identity", "contractVersion", "name", "summary", "description", "source", "demandReferences", "discoveryReferences", "componentMappings", "dependencies", "grounding", "traceability"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      _ <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector("APPLICATION" -> ())))
      identity <- _read(obj, "identity", path)(_model_id)
      version <- _read(obj, "contractVersion", path)(_string)
      name <- _read(obj, "name", path)(_string)
      summary <- _read(obj, "summary", path)((j, p) => _presence(j, p)(_string))
      description <- _read(obj, "description", path)((j, p) => _presence(j, p)(_string))
      _ <- Either.cond(source.modelAuthority == header.modelAuthority && source.path == header.path, (),
        _diagnostic(DiagnosticKind.Incompatible, "Definition source does not belong to the document.", _child(path, "source")))
      _ <- _authority(source, identity.modelId, _child(path, "source"))
      demand <- _read(obj, "demandReferences", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_model_link)))
      discovery <- _read(obj, "discoveryReferences", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_model_link)))
      mappings <- _read(obj, "componentMappings", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_mapping)))
      dependencies <- _read(obj, "dependencies", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_dependency)))
      grounding <- _read(obj, "grounding", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_grounding)))
      traceability <- _read(obj, "traceability", path)((j, p) => _presence(j, p)((x, q) => _vector(x, q)(_traceability)))
      _ <- _values(_items(demand), _child(_child(path, "demandReferences"), "value")) { (edge, p) =>
        Either.cond(Set(ModelRole.Goal, ModelRole.UseCase, ModelRole.Scenario, ModelRole.UseCaseSlice).contains(edge.role), (),
          _diagnostic(DiagnosticKind.Incompatible, "Application demand requires a demand model role.", _child(p, "role")))
      }
      _ <- _owned(_items(demand).map(_.source), identity.modelId, path + "/demandReferences/value")
      _ <- _owned(_items(discovery).map(_.source), identity.modelId, path + "/discoveryReferences/value")
      _ <- _owned(_items(mappings).map(_.source), identity.modelId, path + "/componentMappings/value")
      _ <- _owned(_items(dependencies).map(_.source), identity.modelId, path + "/dependencies/value")
      _ <- _owned(_items(grounding).map(_.source), identity.modelId, path + "/grounding/value")
      _ <- _owned(_items(traceability).map(_.source), identity.modelId, path + "/traceability/value")
    } yield ApplicationDefinition(identity, version, name, summary, description, source, demand, discovery, mappings, dependencies, grounding, traceability), Some(source))
  } yield result
  private def _owned(sources: Vector[Source], authority: String, path: String): Either[Diagnostic, Unit] =
    _values(sources, path)((x, p) => _authority(x, authority, _child(p, "source")))
  private def _reference(json: Json, kind: String, path: String): Either[Diagnostic, (ModelElementId, String)] = for {
    obj <- _object(json, Vector("kind", "identity", "contractVersion"), path)
    _ <- _read(obj, "kind", path)((j, p) => _tag(j, p, Vector(kind -> ())))
    identity <- _read(obj, "identity", path)(_model_id)
    version <- _read(obj, "contractVersion", path)(_string)
  } yield (identity, version)
  def _mapping(json: Json, path: String): Either[Diagnostic, ComponentMapping] = for {
    obj <- _object(json, Vector("target", "boundary", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      target <- _read(obj, "target", path)((j, p) => _reference(j, "COMPONENT", p))
      boundary <- _read(obj, "boundary", path)(_boundary)
      origin <- _read(obj, "origin", path)(_origin)
    } yield ComponentMapping(CapabilityReference(target._1, target._2), boundary, source, origin), Some(source))
  } yield result
  def _dependency(json: Json, path: String): Either[Diagnostic, ApplicationDependency] = for {
    obj <- _object(json, Vector("target", "boundary", "source", "origin"), path)
    source <- _read(obj, "source", path)(_source)
    result <- _context(for {
      target <- _read(obj, "target", path)((j, p) => _reference(j, "APPLICATION", p))
      boundary <- _read(obj, "boundary", path)(_boundary)
      origin <- _read(obj, "origin", path)(_origin)
    } yield ApplicationDependency(ApplicationCapabilityReference(target._1, target._2), boundary, source, origin), Some(source))
  } yield result
}
