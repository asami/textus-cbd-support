package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelSemanticScope(
  componentIdentity: String,
  projectionContextIdentity: String,
  selectedUseCaseElementIdentity: String
)

private[runtime] final case class InternalModelSemanticTarget(
  semanticIdentityKind: String,
  semanticIdentity: String
)

private[runtime] final case class InternalModelSemanticSource(
  authority: String,
  identity: String,
  locator: Option[String],
  revision: Option[String],
  sha256: String
)

private[runtime] final case class InternalModelSemanticSourceReference(
  referenceId: String,
  snapshotArtifactId: String,
  sourceAnchor: String,
  source: InternalModelSemanticSource,
  target: Option[InternalModelSemanticTarget]
)

private[runtime] final case class InternalModelSemanticAssertion(
  assertionId: String,
  semanticIdentityKind: String,
  semanticIdentity: String,
  sourceReferenceId: String,
  content: String,
  conditionIds: Vector[String]
)

private[runtime] final case class InternalModelSemanticCondition(
  conditionId: String,
  kind: String,
  affectedKind: String,
  affectedIdentity: String,
  sourceReferenceId: String,
  detail: String
)

private[runtime] final case class InternalModelSemanticElement(
  identity: String,
  kind: String,
  label: String,
  canonicalAssertionIds: Vector[String],
  enrichmentAssertionIds: Vector[String],
  conditionIds: Vector[String]
)

private[runtime] final case class InternalModelSemanticRelationship(
  identity: String,
  label: String,
  sourceElementIdentity: String,
  targetElementIdentity: String,
  role: String,
  direction: String,
  canonicalAssertionIds: Vector[String],
  enrichmentAssertionIds: Vector[String],
  conditionIds: Vector[String]
)

private[runtime] final case class InternalModelSemanticSuccessorLink(
  priorKind: String,
  priorIdentity: String,
  successorKind: String,
  successorIdentity: String,
  sourceReferenceId: String
)

private[runtime] final case class InternalModelSemanticRealization(
  realizationIdentity: String,
  scope: InternalModelSemanticScope,
  canonicalAssertions: Vector[InternalModelSemanticAssertion],
  enrichmentAssertions: Vector[InternalModelSemanticAssertion],
  elements: Vector[InternalModelSemanticElement],
  relationships: Vector[InternalModelSemanticRelationship],
  sourceReferences: Vector[InternalModelSemanticSourceReference],
  conditions: Vector[InternalModelSemanticCondition],
  successorLinks: Vector[InternalModelSemanticSuccessorLink],
  consumedSnapshotArtifactIds: Vector[String],
  canonicalBytes: Vector[Byte]
)

/** Validates one selected, manifest-bound P10-20B semantic realization without side effects. */
private[runtime] object InternalModelSemanticRealizationValidator {
  private final case class SnapshotWitness(
    componentIdentity: String,
    projectionContextIdentity: String,
    target: InternalModelSemanticTarget,
    sourceAnchor: String,
    content: String,
    limitations: Vector[String]
  )

  private final case class Snapshot(
    source: InternalModelSemanticSource,
    witnesses: Vector[SnapshotWitness],
    anchors: Set[String]
  )

  private val _root_fields = Set(
    "canonicalAssertions", "conditions", "elements", "enrichmentAssertions", "profile", "realizationIdentity",
    "relationships", "schemaVersion", "scope", "sourceReferences", "successorLinks", "traceability"
  )
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _source_fields = Set("authority", "identity", "locator", "revision", "sha256")
  private val _source_reference_fields = Set("referenceId", "snapshotArtifactId", "source", "sourceAnchor", "target")
  private val _target_fields = Set("semanticIdentity", "semanticIdentityKind")
  private val _assertion_fields = Set("assertionId", "conditionIds", "content", "semanticIdentity", "semanticIdentityKind", "sourceReferenceId")
  private val _condition_fields = Set("affectedIdentity", "affectedKind", "conditionId", "detail", "kind", "sourceReferenceId")
  private val _element_fields = Set("canonicalAssertionIds", "conditionIds", "enrichmentAssertionIds", "identity", "kind", "label")
  private val _relationship_fields = Set(
    "canonicalAssertionIds", "conditionIds", "direction", "enrichmentAssertionIds", "identity", "label", "role",
    "sourceElementIdentity", "targetElementIdentity"
  )
  private val _successor_link_fields = Set("priorIdentity", "priorKind", "sourceReferenceId", "successorIdentity", "successorKind")
  private val _traceability_fields = Set("consumedSnapshotArtifactIds")
  private val _semantic_identity_kinds = Set("element", "relationship")
  private val _condition_kinds = Set(
    "absence", "ambiguity", "conflict", "authorization-redaction", "availability-staleness", "malformed", "limitation"
  )
  private val _directions = Set("source-to-target", "target-to-source", "undirected")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def validate(projectRoot: java.nio.file.Path): Consequence[InternalModelSemanticRealization] =
    try {
      InternalModelPackageValidator.verifiedPresentRealization(projectRoot).flatMap { handoff =>
        _realization(handoff).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model semantic realization validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def encode(realization: InternalModelSemanticRealization): Vector[Byte] =
    _encode_realization(realization).toVector

  private def _realization(handoff: InternalModelVerifiedRealizationPackage): Either[String, InternalModelSemanticRealization] =
    for {
      _ <- Either.cond(handoff.realization.role == "realization", (), "selected artifact role must be realization")
      bytes = handoff.realization.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "realization bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "realization bytes")
      json <- _json_parser.parse(content).left.map(_ => "realization bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("realization root must be an object")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(bytes, canonical), (), "realization bytes are not canonical V1 JSON")
      snapshots <- _snapshots(handoff.sourceSnapshots, handoff.realization.dependencies)
      realization <- _root(root, canonical.toVector, handoff.realization, snapshots)
    } yield realization

  private def _root(
    root: JsonObject,
    canonicalbytes: Vector[Byte],
    manifest: InternalModelVerifiedRealization,
    snapshots: Map[String, Snapshot]
  ): Either[String, InternalModelSemanticRealization] =
    for {
      _ <- _closed_fields(root, _root_fields, "realization root")
      schema <- _string(root, "schemaVersion", "realization root")
      _ <- Either.cond(schema == "1.0", (), "realization schemaVersion must be 1.0")
      profile <- _string(root, "profile", "realization root")
      _ <- Either.cond(profile == "ccdm-realization-v1", (), "realization profile is unsupported")
      realizationidentity <- _nonempty_string(root, "realizationIdentity", "realization root")
      scope <- _scope(root)
      references <- _source_references(root, scope, snapshots)
      canonical <- _assertions(root, "canonicalAssertions")
      enrichment <- _assertions(root, "enrichmentAssertions")
      conditions <- _conditions(root)
      elements <- _elements(root)
      relationships <- _relationships(root)
      successors <- _successor_links(root)
      consumedids <- _traceability(root)
      _ <- _invariants(
        scope,
        manifest,
        snapshots,
        references,
        canonical,
        enrichment,
        conditions,
        elements,
        relationships,
        successors,
        consumedids
      )
      candidate = InternalModelSemanticRealization(
        realizationIdentity = realizationidentity,
        scope = scope,
        canonicalAssertions = canonical,
        enrichmentAssertions = enrichment,
        elements = elements,
        relationships = relationships,
        sourceReferences = references,
        conditions = conditions,
        successorLinks = successors,
        consumedSnapshotArtifactIds = consumedids,
        canonicalBytes = Vector.empty
      )
      encoded = _encode_realization(candidate).toVector
      _ <- Either.cond(encoded == canonicalbytes, (), "typed realization re-encoding does not match supplied canonical bytes")
    } yield candidate.copy(canonicalBytes = encoded)

  private def _encode_realization(realization: InternalModelSemanticRealization): Array[Byte] =
    _canonical_bytes(Json.obj(
      "canonicalAssertions" -> Json.fromValues(realization.canonicalAssertions.map(_assertion_json)),
      "conditions" -> Json.fromValues(realization.conditions.map(_condition_json)),
      "elements" -> Json.fromValues(realization.elements.map(_element_json)),
      "enrichmentAssertions" -> Json.fromValues(realization.enrichmentAssertions.map(_assertion_json)),
      "profile" -> Json.fromString("ccdm-realization-v1"),
      "realizationIdentity" -> Json.fromString(realization.realizationIdentity),
      "relationships" -> Json.fromValues(realization.relationships.map(_relationship_json)),
      "schemaVersion" -> Json.fromString("1.0"),
      "scope" -> Json.obj(
        "componentIdentity" -> Json.fromString(realization.scope.componentIdentity),
        "projectionContextIdentity" -> Json.fromString(realization.scope.projectionContextIdentity),
        "selectedUseCaseElementIdentity" -> Json.fromString(realization.scope.selectedUseCaseElementIdentity)
      ),
      "sourceReferences" -> Json.fromValues(realization.sourceReferences.map(_source_reference_json)),
      "successorLinks" -> Json.fromValues(realization.successorLinks.map(_successor_link_json)),
      "traceability" -> Json.obj(
        "consumedSnapshotArtifactIds" -> Json.fromValues(realization.consumedSnapshotArtifactIds.map(Json.fromString))
      )
    ))

  private def _assertion_json(assertion: InternalModelSemanticAssertion): Json =
    Json.obj(
      "assertionId" -> Json.fromString(assertion.assertionId),
      "conditionIds" -> Json.fromValues(assertion.conditionIds.map(Json.fromString)),
      "content" -> Json.fromString(assertion.content),
      "semanticIdentity" -> Json.fromString(assertion.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(assertion.semanticIdentityKind),
      "sourceReferenceId" -> Json.fromString(assertion.sourceReferenceId)
    )

  private def _condition_json(condition: InternalModelSemanticCondition): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(condition.affectedIdentity),
      "affectedKind" -> Json.fromString(condition.affectedKind),
      "conditionId" -> Json.fromString(condition.conditionId),
      "detail" -> Json.fromString(condition.detail),
      "kind" -> Json.fromString(condition.kind),
      "sourceReferenceId" -> Json.fromString(condition.sourceReferenceId)
    )

  private def _element_json(element: InternalModelSemanticElement): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(element.canonicalAssertionIds.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(element.conditionIds.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(element.enrichmentAssertionIds.map(Json.fromString)),
      "identity" -> Json.fromString(element.identity),
      "kind" -> Json.fromString(element.kind),
      "label" -> Json.fromString(element.label)
    )

  private def _relationship_json(relationship: InternalModelSemanticRelationship): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(relationship.canonicalAssertionIds.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(relationship.conditionIds.map(Json.fromString)),
      "direction" -> Json.fromString(relationship.direction),
      "enrichmentAssertionIds" -> Json.fromValues(relationship.enrichmentAssertionIds.map(Json.fromString)),
      "identity" -> Json.fromString(relationship.identity),
      "label" -> Json.fromString(relationship.label),
      "role" -> Json.fromString(relationship.role),
      "sourceElementIdentity" -> Json.fromString(relationship.sourceElementIdentity),
      "targetElementIdentity" -> Json.fromString(relationship.targetElementIdentity)
    )

  private def _source_reference_json(reference: InternalModelSemanticSourceReference): Json =
    Json.obj(
      "referenceId" -> Json.fromString(reference.referenceId),
      "snapshotArtifactId" -> Json.fromString(reference.snapshotArtifactId),
      "source" -> _source_json(reference.source),
      "sourceAnchor" -> Json.fromString(reference.sourceAnchor),
      "target" -> reference.target.map(_target_json).getOrElse(Json.Null)
    )

  private def _source_json(source: InternalModelSemanticSource): Json =
    Json.obj(
      "authority" -> Json.fromString(source.authority),
      "identity" -> Json.fromString(source.identity),
      "locator" -> source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null),
      "sha256" -> Json.fromString(source.sha256)
    )

  private def _target_json(target: InternalModelSemanticTarget): Json =
    Json.obj(
      "semanticIdentity" -> Json.fromString(target.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(target.semanticIdentityKind)
    )

  private def _successor_link_json(link: InternalModelSemanticSuccessorLink): Json =
    Json.obj(
      "priorIdentity" -> Json.fromString(link.priorIdentity),
      "priorKind" -> Json.fromString(link.priorKind),
      "sourceReferenceId" -> Json.fromString(link.sourceReferenceId),
      "successorIdentity" -> Json.fromString(link.successorIdentity),
      "successorKind" -> Json.fromString(link.successorKind)
    )

  private def _scope(root: JsonObject): Either[String, InternalModelSemanticScope] =
    for {
      value <- root("scope").toRight("realization scope is missing")
      objectvalue <- _object(value, "realization scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "realization scope")
      componentidentity <- _nonempty_string(objectvalue, "componentIdentity", "realization scope")
      contextidentity <- _nonempty_string(objectvalue, "projectionContextIdentity", "realization scope")
      usecaseidentity <- _nonempty_string(objectvalue, "selectedUseCaseElementIdentity", "realization scope")
    } yield InternalModelSemanticScope(componentidentity, contextidentity, usecaseidentity)

  private def _source_references(
    root: JsonObject,
    scope: InternalModelSemanticScope,
    snapshots: Map[String, Snapshot]
  ): Either[String, Vector[InternalModelSemanticSourceReference]] =
    for {
      values <- _array(root, "sourceReferences", "realization root")
      references <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticSourceReference]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          reference <- _source_reference(value, index, scope, snapshots)
        } yield collected :+ reference
      }
      _ <- _strictly_sorted(references.map(reference => Vector(reference.referenceId)), "sourceReferences")
    } yield references

  private def _source_reference(
    value: Json,
    index: Int,
    scope: InternalModelSemanticScope,
    snapshots: Map[String, Snapshot]
  ): Either[String, InternalModelSemanticSourceReference] =
    for {
      objectvalue <- _object(value, s"source reference $index")
      _ <- _closed_fields(objectvalue, _source_reference_fields, s"source reference $index")
      referenceid <- _nonempty_string(objectvalue, "referenceId", s"source reference $index")
      snapshotid <- _nonempty_string(objectvalue, "snapshotArtifactId", s"source reference $index")
      sourceanchor <- _nonempty_string(objectvalue, "sourceAnchor", s"source reference $index")
      sourcevalue <- objectvalue("source").toRight(s"source reference $index source is missing")
      source <- _source(sourcevalue, s"source reference $index source")
      target <- _nullable_target(objectvalue, s"source reference $index")
      snapshot <- snapshots.get(snapshotid).toRight(s"source reference $referenceid does not name a present source-snapshot artifact")
      _ <- Either.cond(source == snapshot.source, (), s"source reference $referenceid source metadata does not match its snapshot")
      _ <- _source_anchor(snapshot, sourceanchor, target, scope, s"source reference $referenceid")
    } yield InternalModelSemanticSourceReference(referenceid, snapshotid, sourceanchor, source, target)

  private def _nullable_target(objectvalue: JsonObject, label: String): Either[String, Option[InternalModelSemanticTarget]] =
    objectvalue("target") match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => _target(value, s"$label target").map(Some(_))
      case None => Left(s"$label target is missing")
    }

  private def _target(value: Json, label: String): Either[String, InternalModelSemanticTarget] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _target_fields, label)
      kind <- _semantic_identity_kind(objectvalue, "semanticIdentityKind", label)
      identity <- _nonempty_string(objectvalue, "semanticIdentity", label)
    } yield InternalModelSemanticTarget(kind, identity)

  private def _source(value: Json, label: String): Either[String, InternalModelSemanticSource] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _source_fields, label)
      authority <- _nonempty_string(objectvalue, "authority", label)
      identity <- _nonempty_string(objectvalue, "identity", label)
      locator <- _nullable_nonempty_string(objectvalue, "locator", label)
      revision <- _nullable_nonempty_string(objectvalue, "revision", label)
      sha256 <- _string(objectvalue, "sha256", label)
      _ <- Either.cond(_digest_pattern.matches(sha256), (), s"$label sha256 is invalid")
    } yield InternalModelSemanticSource(authority, identity, locator, revision, sha256)

  private def _assertions(root: JsonObject, key: String): Either[String, Vector[InternalModelSemanticAssertion]] =
    for {
      values <- _array(root, key, "realization root")
      assertions <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticAssertion]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          assertion <- _assertion(value, key, index)
        } yield collected :+ assertion
      }
      _ <- _strictly_sorted(assertions.map(assertion => Vector(assertion.assertionId)), key)
    } yield assertions

  private def _assertion(value: Json, lane: String, index: Int): Either[String, InternalModelSemanticAssertion] =
    for {
      objectvalue <- _object(value, s"$lane assertion $index")
      _ <- _closed_fields(objectvalue, _assertion_fields, s"$lane assertion $index")
      assertionid <- _nonempty_string(objectvalue, "assertionId", s"$lane assertion $index")
      kind <- _semantic_identity_kind(objectvalue, "semanticIdentityKind", s"$lane assertion $index")
      identity <- _nonempty_string(objectvalue, "semanticIdentity", s"$lane assertion $index")
      referenceid <- _nonempty_string(objectvalue, "sourceReferenceId", s"$lane assertion $index")
      content <- _nonempty_string(objectvalue, "content", s"$lane assertion $index")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"$lane assertion $index")
    } yield InternalModelSemanticAssertion(assertionid, kind, identity, referenceid, content, conditionids)

  private def _conditions(root: JsonObject): Either[String, Vector[InternalModelSemanticCondition]] =
    for {
      values <- _array(root, "conditions", "realization root")
      conditions <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticCondition]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          condition <- _condition(value, index)
        } yield collected :+ condition
      }
      _ <- _strictly_sorted(conditions.map(condition => Vector(condition.conditionId)), "conditions")
    } yield conditions

  private def _condition(value: Json, index: Int): Either[String, InternalModelSemanticCondition] =
    for {
      objectvalue <- _object(value, s"condition $index")
      _ <- _closed_fields(objectvalue, _condition_fields, s"condition $index")
      conditionid <- _nonempty_string(objectvalue, "conditionId", s"condition $index")
      kind <- _string(objectvalue, "kind", s"condition $index")
      _ <- Either.cond(_condition_kinds.contains(kind), (), s"condition $conditionid kind is invalid")
      affectedkind <- _semantic_identity_kind(objectvalue, "affectedKind", s"condition $conditionid")
      affectedidentity <- _nonempty_string(objectvalue, "affectedIdentity", s"condition $conditionid")
      referenceid <- _nonempty_string(objectvalue, "sourceReferenceId", s"condition $conditionid")
      detail <- _nonempty_string(objectvalue, "detail", s"condition $conditionid")
    } yield InternalModelSemanticCondition(conditionid, kind, affectedkind, affectedidentity, referenceid, detail)

  private def _elements(root: JsonObject): Either[String, Vector[InternalModelSemanticElement]] =
    for {
      values <- _array(root, "elements", "realization root")
      elements <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticElement]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          element <- _element(value, index)
        } yield collected :+ element
      }
      _ <- _strictly_sorted(elements.map(element => Vector(element.identity)), "elements")
    } yield elements

  private def _element(value: Json, index: Int): Either[String, InternalModelSemanticElement] =
    for {
      objectvalue <- _object(value, s"element $index")
      _ <- _closed_fields(objectvalue, _element_fields, s"element $index")
      identity <- _nonempty_string(objectvalue, "identity", s"element $index")
      kind <- _nonempty_string(objectvalue, "kind", s"element $identity")
      label <- _nonempty_string(objectvalue, "label", s"element $identity")
      canonicalids <- _sorted_strings(objectvalue, "canonicalAssertionIds", s"element $identity")
      enrichmentids <- _sorted_strings(objectvalue, "enrichmentAssertionIds", s"element $identity")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"element $identity")
    } yield InternalModelSemanticElement(identity, kind, label, canonicalids, enrichmentids, conditionids)

  private def _relationships(root: JsonObject): Either[String, Vector[InternalModelSemanticRelationship]] =
    for {
      values <- _array(root, "relationships", "realization root")
      relationships <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticRelationship]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          relationship <- _relationship(value, index)
        } yield collected :+ relationship
      }
      _ <- _strictly_sorted(relationships.map(relationship => Vector(relationship.identity)), "relationships")
    } yield relationships

  private def _relationship(value: Json, index: Int): Either[String, InternalModelSemanticRelationship] =
    for {
      objectvalue <- _object(value, s"relationship $index")
      _ <- _closed_fields(objectvalue, _relationship_fields, s"relationship $index")
      identity <- _nonempty_string(objectvalue, "identity", s"relationship $index")
      label <- _nonempty_string(objectvalue, "label", s"relationship $identity")
      sourceidentity <- _nonempty_string(objectvalue, "sourceElementIdentity", s"relationship $identity")
      targetidentity <- _nonempty_string(objectvalue, "targetElementIdentity", s"relationship $identity")
      role <- _nonempty_string(objectvalue, "role", s"relationship $identity")
      direction <- _string(objectvalue, "direction", s"relationship $identity")
      _ <- Either.cond(_directions.contains(direction), (), s"relationship $identity direction is invalid")
      canonicalids <- _sorted_strings(objectvalue, "canonicalAssertionIds", s"relationship $identity")
      enrichmentids <- _sorted_strings(objectvalue, "enrichmentAssertionIds", s"relationship $identity")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"relationship $identity")
    } yield InternalModelSemanticRelationship(identity, label, sourceidentity, targetidentity, role, direction, canonicalids, enrichmentids, conditionids)

  private def _successor_links(root: JsonObject): Either[String, Vector[InternalModelSemanticSuccessorLink]] =
    for {
      values <- _array(root, "successorLinks", "realization root")
      links <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticSuccessorLink]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          link <- _successor_link(value, index)
        } yield collected :+ link
      }
      _ <- _strictly_sorted(links.map(link => Vector(link.priorKind, link.priorIdentity, link.successorKind, link.successorIdentity, link.sourceReferenceId)), "successorLinks")
    } yield links

  private def _successor_link(value: Json, index: Int): Either[String, InternalModelSemanticSuccessorLink] =
    for {
      objectvalue <- _object(value, s"successor link $index")
      _ <- _closed_fields(objectvalue, _successor_link_fields, s"successor link $index")
      priorkind <- _semantic_identity_kind(objectvalue, "priorKind", s"successor link $index")
      prioridentity <- _nonempty_string(objectvalue, "priorIdentity", s"successor link $index")
      successorkind <- _semantic_identity_kind(objectvalue, "successorKind", s"successor link $index")
      successoridentity <- _nonempty_string(objectvalue, "successorIdentity", s"successor link $index")
      referenceid <- _nonempty_string(objectvalue, "sourceReferenceId", s"successor link $index")
      _ <- Either.cond(priorkind != successorkind || prioridentity != successoridentity, (), s"successor link $index must not replace an identity with itself")
    } yield InternalModelSemanticSuccessorLink(priorkind, prioridentity, successorkind, successoridentity, referenceid)

  private def _traceability(root: JsonObject): Either[String, Vector[String]] =
    for {
      value <- root("traceability").toRight("realization traceability is missing")
      objectvalue <- _object(value, "realization traceability")
      _ <- _closed_fields(objectvalue, _traceability_fields, "realization traceability")
      consumed <- _sorted_strings(objectvalue, "consumedSnapshotArtifactIds", "realization traceability")
    } yield consumed

  private def _invariants(
    scope: InternalModelSemanticScope,
    manifest: InternalModelVerifiedRealization,
    snapshots: Map[String, Snapshot],
    references: Vector[InternalModelSemanticSourceReference],
    canonical: Vector[InternalModelSemanticAssertion],
    enrichment: Vector[InternalModelSemanticAssertion],
    conditions: Vector[InternalModelSemanticCondition],
    elements: Vector[InternalModelSemanticElement],
    relationships: Vector[InternalModelSemanticRelationship],
    successors: Vector[InternalModelSemanticSuccessorLink],
    consumedids: Vector[String]
  ): Either[String, Unit] = {
    val sourcebyid = references.map(reference => reference.referenceId -> reference).toMap
    val canonicalbyid = canonical.map(assertion => assertion.assertionId -> assertion).toMap
    val enrichmentbyid = enrichment.map(assertion => assertion.assertionId -> assertion).toMap
    val conditionbyid = conditions.map(condition => condition.conditionId -> condition).toMap
    val targetids = elements.map(element => InternalModelSemanticTarget("element", element.identity)) ++
      relationships.map(relationship => InternalModelSemanticTarget("relationship", relationship.identity))
    val targetset = targetids.toSet
    val expecteddependencies = manifest.dependencies.toSet
    val referencedsnapshots = references.map(_.snapshotArtifactId).toSet
    val referencedids = (canonical.map(_.sourceReferenceId) ++ enrichment.map(_.sourceReferenceId) ++ conditions.map(_.sourceReferenceId) ++ successors.map(_.sourceReferenceId)).toSet
    for {
      _ <- Either.cond(manifest.bytes.nonEmpty, (), "selected realization bytes are missing")
      _ <- Either.cond(canonicalbyid.size == canonical.size && enrichmentbyid.size == enrichment.size, (), "assertion IDs must be unique within each lane")
      _ <- Either.cond((canonicalbyid.keySet intersect enrichmentbyid.keySet).isEmpty, (), "canonical and enrichment assertion IDs must not collide")
      _ <- Either.cond(conditionbyid.size == conditions.size, (), "condition IDs must be unique")
      _ <- Either.cond(sourcebyid.size == references.size, (), "source reference IDs must be unique")
      _ <- Either.cond(expecteddependencies == referencedsnapshots, (), "realization source references must equal manifest dependsOn")
      _ <- Either.cond(consumedids.toSet == expecteddependencies && consumedids.size == expecteddependencies.size, (), "traceability consumedSnapshotArtifactIds must equal manifest dependsOn")
      _ <- Either.cond(referencedids == sourcebyid.keySet, (), "every source reference must be linked by an assertion, condition, or successor link")
      _ <- _selected_use_case(scope, elements)
      _ <- _relationship_endpoints(relationships, elements)
      _ <- _source_reference_targets(references, targetset)
      _ <- _assertion_links(canonical, snapshots, sourcebyid, conditionbyid, targetset, "canonical")
      _ <- _assertion_links(enrichment, snapshots, sourcebyid, conditionbyid, targetset, "enrichment")
      _ <- _condition_links(conditions, sourcebyid, targetset)
      _ <- _structural_links(elements, relationships, canonical, enrichment, conditions)
      _ <- _successor_links(successors, sourcebyid, targetset)
      _ <- _snapshot_limitations(scope, snapshots, references, conditions)
    } yield ()
  }

  private def _selected_use_case(scope: InternalModelSemanticScope, elements: Vector[InternalModelSemanticElement]): Either[String, Unit] =
    elements.find(_.identity == scope.selectedUseCaseElementIdentity) match {
      case Some(element) if element.kind == "use-case" => Right(())
      case Some(_) => Left("selected Use Case identity must identify an element of kind use-case")
      case None => Left("selected Use Case identity is not an admitted element")
    }

  private def _relationship_endpoints(
    relationships: Vector[InternalModelSemanticRelationship],
    elements: Vector[InternalModelSemanticElement]
  ): Either[String, Unit] = {
    val elementids = elements.map(_.identity).toSet
    relationships.find(relationship => !elementids.contains(relationship.sourceElementIdentity) || !elementids.contains(relationship.targetElementIdentity)) match {
      case Some(relationship) => Left(s"relationship ${relationship.identity} has an unknown endpoint")
      case None => Right(())
    }
  }

  private def _source_reference_targets(
    references: Vector[InternalModelSemanticSourceReference],
    targets: Set[InternalModelSemanticTarget]
  ): Either[String, Unit] =
    references.find(reference => reference.target.exists(target => !targets.contains(target))) match {
      case Some(reference) => Left(s"source reference ${reference.referenceId} targets an unknown semantic identity")
      case None => Right(())
    }

  private def _assertion_links(
    assertions: Vector[InternalModelSemanticAssertion],
    snapshots: Map[String, Snapshot],
    references: Map[String, InternalModelSemanticSourceReference],
    conditions: Map[String, InternalModelSemanticCondition],
    targets: Set[InternalModelSemanticTarget],
    lane: String
  ): Either[String, Unit] =
    _first_failure(assertions.iterator.map { assertion =>
      val target = InternalModelSemanticTarget(assertion.semanticIdentityKind, assertion.semanticIdentity)
      for {
        _ <- Either.cond(targets.contains(target), (), s"$lane assertion ${assertion.assertionId} targets an unknown semantic identity")
        reference <- references.get(assertion.sourceReferenceId).toRight(s"$lane assertion ${assertion.assertionId} has an unknown source reference")
        assertedtarget <- reference.target.toRight(s"$lane assertion ${assertion.assertionId} source reference must carry its semantic target")
        _ <- Either.cond(assertedtarget == target, (), s"$lane assertion ${assertion.assertionId} source reference target does not match its semantic identity")
        snapshot <- snapshots.get(reference.snapshotArtifactId).toRight(s"$lane assertion ${assertion.assertionId} does not name a source snapshot")
        witness <- snapshot.witnesses.find(witness => witness.target == target && witness.sourceAnchor == reference.sourceAnchor).toRight(s"$lane assertion ${assertion.assertionId} has no exact source witness")
        _ <- Either.cond(witness.content == assertion.content, (), s"$lane assertion ${assertion.assertionId} content does not match its exact source basis")
        _ <- Either.cond(assertion.conditionIds.forall(conditions.contains), (), s"$lane assertion ${assertion.assertionId} has an unknown condition")
        _ <- Either.cond(assertion.conditionIds.forall(id => {
          val condition = conditions(id)
          condition.affectedKind == target.semanticIdentityKind && condition.affectedIdentity == target.semanticIdentity
        }), (), s"$lane assertion ${assertion.assertionId} links a condition for another semantic identity")
      } yield ()
    })

  private def _condition_links(
    conditions: Vector[InternalModelSemanticCondition],
    references: Map[String, InternalModelSemanticSourceReference],
    targets: Set[InternalModelSemanticTarget]
  ): Either[String, Unit] =
    _first_failure(conditions.iterator.map { condition =>
      val target = InternalModelSemanticTarget(condition.affectedKind, condition.affectedIdentity)
      for {
        _ <- Either.cond(targets.contains(target), (), s"condition ${condition.conditionId} affects an unknown semantic identity")
        reference <- references.get(condition.sourceReferenceId).toRight(s"condition ${condition.conditionId} has an unknown source reference")
        _ <- Either.cond(reference.target.forall(_ == target), (), s"condition ${condition.conditionId} source reference target does not match its affected identity")
      } yield ()
    })

  private def _structural_links(
    elements: Vector[InternalModelSemanticElement],
    relationships: Vector[InternalModelSemanticRelationship],
    canonical: Vector[InternalModelSemanticAssertion],
    enrichment: Vector[InternalModelSemanticAssertion],
    conditions: Vector[InternalModelSemanticCondition]
  ): Either[String, Unit] = {
    val entries = elements.map { element =>
      (
        InternalModelSemanticTarget("element", element.identity),
        element.canonicalAssertionIds,
        element.enrichmentAssertionIds,
        element.conditionIds
      )
    } ++ relationships.map { relationship =>
      (
        InternalModelSemanticTarget("relationship", relationship.identity),
        relationship.canonicalAssertionIds,
        relationship.enrichmentAssertionIds,
        relationship.conditionIds
      )
    }
    _first_failure(entries.iterator.map { case (target, canonicalids, enrichmentids, conditionids) =>
      val expectedcanonical = canonical.collect {
        case assertion if assertion.semanticIdentityKind == target.semanticIdentityKind && assertion.semanticIdentity == target.semanticIdentity => assertion.assertionId
      }
      val expectedenrichment = enrichment.collect {
        case assertion if assertion.semanticIdentityKind == target.semanticIdentityKind && assertion.semanticIdentity == target.semanticIdentity => assertion.assertionId
      }
      val expectedconditions = conditions.collect {
        case condition if condition.affectedKind == target.semanticIdentityKind && condition.affectedIdentity == target.semanticIdentity => condition.conditionId
      }
      for {
        _ <- Either.cond(canonicalids.nonEmpty, (), s"${target.semanticIdentityKind} ${target.semanticIdentity} has no canonical assertion witness")
        _ <- Either.cond(canonicalids == expectedcanonical, (), s"${target.semanticIdentityKind} ${target.semanticIdentity} canonical assertion links are incomplete or inconsistent")
        _ <- Either.cond(enrichmentids == expectedenrichment, (), s"${target.semanticIdentityKind} ${target.semanticIdentity} enrichment assertion links are incomplete or inconsistent")
        _ <- Either.cond(conditionids == expectedconditions, (), s"${target.semanticIdentityKind} ${target.semanticIdentity} condition links are incomplete or inconsistent")
      } yield ()
    })
  }

  private def _successor_links(
    successors: Vector[InternalModelSemanticSuccessorLink],
    references: Map[String, InternalModelSemanticSourceReference],
    targets: Set[InternalModelSemanticTarget]
  ): Either[String, Unit] =
    _first_failure(successors.iterator.map { successor =>
      val prior = InternalModelSemanticTarget(successor.priorKind, successor.priorIdentity)
      val next = InternalModelSemanticTarget(successor.successorKind, successor.successorIdentity)
      for {
        _ <- Either.cond(targets.contains(prior) && targets.contains(next), (), s"successor link ${successor.priorIdentity} -> ${successor.successorIdentity} has an unknown semantic identity")
        reference <- references.get(successor.sourceReferenceId).toRight(s"successor link ${successor.priorIdentity} -> ${successor.successorIdentity} has an unknown source reference")
        target <- reference.target.toRight(s"successor link ${successor.priorIdentity} -> ${successor.successorIdentity} source reference must carry an explicit semantic target")
        _ <- Either.cond(target == prior || target == next, (), s"successor link ${successor.priorIdentity} -> ${successor.successorIdentity} source reference names another semantic identity")
      } yield ()
    })

  private def _snapshot_limitations(
    scope: InternalModelSemanticScope,
    snapshots: Map[String, Snapshot],
    references: Vector[InternalModelSemanticSourceReference],
    conditions: Vector[InternalModelSemanticCondition]
  ): Either[String, Unit] =
    _first_failure(references.iterator.map { reference =>
      reference.target match {
        case None => Right(())
        case Some(target) =>
          for {
            snapshot <- snapshots.get(reference.snapshotArtifactId).toRight(s"source reference ${reference.referenceId} does not name a snapshot")
            witness <- snapshot.witnesses.find(witness =>
              witness.componentIdentity == scope.componentIdentity &&
                witness.projectionContextIdentity == scope.projectionContextIdentity &&
                witness.target == target &&
                witness.sourceAnchor == reference.sourceAnchor
            ).toRight(s"source reference ${reference.referenceId} does not have an exact source witness")
            _ <- Either.cond(
              witness.limitations.forall(limitation => conditions.exists(condition =>
                condition.kind == "limitation" &&
                  condition.affectedKind == target.semanticIdentityKind &&
                  condition.affectedIdentity == target.semanticIdentity &&
                  condition.sourceReferenceId == reference.referenceId &&
                  condition.detail == limitation
              )),
              (),
              s"source reference ${reference.referenceId} omits a snapshot-reported limitation"
            )
          } yield ()
      }
    })

  private def _snapshots(
    values: Vector[InternalModelVerifiedSourceSnapshot],
    dependencies: Vector[String]
  ): Either[String, Map[String, Snapshot]] = {
    val selected = values.filter(value => dependencies.contains(value.artifactId))
    for {
      _ <- Either.cond(selected.map(_.artifactId).toSet == dependencies.toSet, (), "realization dependsOn includes a non-source-snapshot artifact")
      snapshots <- selected.foldLeft[Either[String, Vector[(String, Snapshot)]]](Right(Vector.empty)) { (result, value) =>
      value.bytes match {
        case None => Left(s"selected source-snapshot artifact ${value.artifactId} is absent")
        case Some(bytes) =>
          for {
            collected <- result
            snapshot <- _snapshot(value.artifactId, bytes.toArray)
          } yield collected :+ (value.artifactId -> snapshot)
      }
      }
    } yield snapshots.toMap
  }

  private def _snapshot(artifactid: String, bytes: Array[Byte]): Either[String, Snapshot] =
    for {
      kind <- InternalModelSourceSnapshotFreshness.validatedSnapshotKind(bytes).left.map(reason => s"source-snapshot artifact $artifactid is invalid: $reason")
      content <- _decode_utf8(bytes, s"source-snapshot artifact $artifactid")
      json <- _json_parser.parse(content).left.map(_ => s"source-snapshot artifact $artifactid cannot be parsed after validation")
      root <- json.asObject.toRight(s"source-snapshot artifact $artifactid root must be an object")
      sourcevalue <- root("source").toRight(s"source-snapshot artifact $artifactid source is missing")
      source <- _source(sourcevalue, s"source-snapshot artifact $artifactid source")
      witnesses <- _snapshot_witnesses(kind, root, artifactid)
    } yield Snapshot(source, witnesses, witnesses.map(_.sourceAnchor).toSet ++ _snapshot_anchors(kind, root))

  private def _snapshot_witnesses(kind: String, root: JsonObject, artifactid: String): Either[String, Vector[SnapshotWitness]] =
    kind match {
      case "scenario" => _scenario_witnesses(root, artifactid)
      case "model-context" => _model_context_witnesses(root, artifactid)
      case "glossary-bok" => Right(Vector.empty)
      case "cml-baseline" => Right(Vector.empty)
      case _ => Left(s"source-snapshot artifact $artifactid has an unsupported validated kind")
    }

  private def _scenario_witnesses(root: JsonObject, artifactid: String): Either[String, Vector[SnapshotWitness]] =
    for {
      basisvalue <- root("basis").toRight(s"source-snapshot artifact $artifactid scenario basis is missing")
      basis <- _object(basisvalue, s"source-snapshot artifact $artifactid scenario basis")
      content <- _nonempty_string(basis, "content", s"source-snapshot artifact $artifactid scenario basis")
      links <- _array(basis, "traceLinks", s"source-snapshot artifact $artifactid scenario basis")
      witnesses <- links.zipWithIndex.foldLeft[Either[String, Vector[SnapshotWitness]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          link <- _object(value, s"source-snapshot artifact $artifactid scenario trace link $index")
          componentidentity <- _nonempty_string(link, "componentIdentity", s"source-snapshot artifact $artifactid scenario trace link $index")
          contextidentity <- _nonempty_string(link, "projectionContextIdentity", s"source-snapshot artifact $artifactid scenario trace link $index")
          sourceanchor <- _nonempty_string(link, "scenarioAnchor", s"source-snapshot artifact $artifactid scenario trace link $index")
          kind <- _semantic_identity_kind(link, "semanticIdentityKind", s"source-snapshot artifact $artifactid scenario trace link $index")
          identity <- _nonempty_string(link, "semanticIdentity", s"source-snapshot artifact $artifactid scenario trace link $index")
        } yield collected :+ SnapshotWitness(componentidentity, contextidentity, InternalModelSemanticTarget(kind, identity), sourceanchor, content, Vector.empty)
      }
    } yield witnesses

  private def _model_context_witnesses(root: JsonObject, artifactid: String): Either[String, Vector[SnapshotWitness]] =
    for {
      basisvalue <- root("basis").toRight(s"source-snapshot artifact $artifactid model-context basis is missing")
      basis <- _object(basisvalue, s"source-snapshot artifact $artifactid model-context basis")
      facts <- _array(basis, "facts", s"source-snapshot artifact $artifactid model-context basis")
      witnesses <- facts.zipWithIndex.foldLeft[Either[String, Vector[SnapshotWitness]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          fact <- _object(value, s"source-snapshot artifact $artifactid model-context fact $index")
          componentidentity <- _nonempty_string(fact, "componentIdentity", s"source-snapshot artifact $artifactid model-context fact $index")
          contextidentity <- _nonempty_string(fact, "projectionContextIdentity", s"source-snapshot artifact $artifactid model-context fact $index")
          sourceanchor <- _nonempty_string(fact, "sourceAnchor", s"source-snapshot artifact $artifactid model-context fact $index")
          kind <- _semantic_identity_kind(fact, "semanticIdentityKind", s"source-snapshot artifact $artifactid model-context fact $index")
          identity <- _nonempty_string(fact, "semanticIdentity", s"source-snapshot artifact $artifactid model-context fact $index")
          content <- _nonempty_string(fact, "content", s"source-snapshot artifact $artifactid model-context fact $index")
          limitations <- _strings(fact, "limitations", s"source-snapshot artifact $artifactid model-context fact $index")
        } yield collected :+ SnapshotWitness(componentidentity, contextidentity, InternalModelSemanticTarget(kind, identity), sourceanchor, content, limitations)
      }
    } yield witnesses

  private def _snapshot_anchors(kind: String, root: JsonObject): Set[String] =
    kind match {
      case "glossary-bok" =>
        root("basis").flatMap(_.asObject).flatMap(_("entries")).flatMap(_.asArray).toVector.flatten.flatMap { entry =>
          entry.asObject.flatMap(_("sourceAnchor")).flatMap(_.asString)
        }.toSet
      case _ => Set.empty
    }

  private def _source_anchor(
    snapshot: Snapshot,
    sourceanchor: String,
    target: Option[InternalModelSemanticTarget],
    scope: InternalModelSemanticScope,
    label: String
  ): Either[String, Unit] =
    target match {
      case Some(expected) =>
        val witness = snapshot.witnesses.find(witness =>
          witness.componentIdentity == scope.componentIdentity &&
            witness.projectionContextIdentity == scope.projectionContextIdentity &&
            witness.target == expected &&
            witness.sourceAnchor == sourceanchor
        )
        witness match {
          case Some(_) => Right(())
          case None => Left(s"$label does not match a source-owned Scenario trace link or model-context fact")
        }
      case None =>
        Either.cond(snapshot.anchors.contains(sourceanchor), (), s"$label does not name a source-owned anchor")
    }

  private def _sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    for {
      values <- _strings(objectvalue, key, label)
      _ <- _strictly_sorted(values.map(value => Vector(value)), s"$label $key")
    } yield values

  private def _strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    _array(objectvalue, key, label).flatMap { values =>
      values.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          stringvalue <- _nonempty_string_value(value, s"$label $key $index")
        } yield collected :+ stringvalue
      }
    }

  private def _semantic_identity_kind(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap { kind =>
      Either.cond(_semantic_identity_kinds.contains(kind), kind, s"$label $key is invalid")
    }

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap(value => _nonempty_value(value, s"$label $key"))

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key) match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => _nonempty_string_value(value, s"$label $key").map(Some(_))
      case None => Left(s"$label $key is missing")
    }

  private def _nonempty_string_value(value: Json, label: String): Either[String, String] =
    value.asString.toRight(s"$label must be a JSON string").flatMap(stringvalue => _nonempty_value(stringvalue, label))

  private def _nonempty_value(value: String, label: String): Either[String, String] =
    Option(value).filter(_.nonEmpty).toRight(s"$label must be nonempty")

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not the closed V1 schema")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] = {
    val ordered = values.zip(values.drop(1)).forall { case (left, right) => _compare_tuple(left, right) < 0 }
    Either.cond(ordered, (), s"$label must be unique and sorted by ascending UTF-8-byte order")
  }

  private def _compare_tuple(left: Vector[String], right: Vector[String]): Int =
    left.zip(right).iterator.map { case (leftvalue, rightvalue) => _compare_text(leftvalue, rightvalue) }.find(_ != 0).getOrElse(left.length.compare(right.length))

  private def _compare_text(left: String, right: String): Int =
    Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))

  private def _first_failure(values: Iterator[Either[String, Unit]]): Either[String, Unit] =
    values.collectFirst { case Left(reason) => reason } match {
      case Some(reason) => Left(reason)
      case None => Right(())
    }

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
