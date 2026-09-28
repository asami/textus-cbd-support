package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

import io.circe.{Json, Printer}

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
/** Encodes the closed semantic-realization artifact without validation or I/O. */
private[runtime] object InternalModelSemanticRealizationEncoder {
  private val _canonical_printer = Printer.noSpacesSortKeys

  def encode(realization: InternalModelSemanticRealization): Array[Byte] =
    _canonical_bytes(Json.obj(
      "canonicalAssertions" -> Json.fromValues(realization.canonicalAssertions.map(_assertion_json(_, realization.profile))),
      "conditions" -> Json.fromValues(realization.conditions.map(_condition_json)),
      "elements" -> Json.fromValues(realization.elements.map(_element_json)),
      "enrichmentAssertions" -> Json.fromValues(realization.enrichmentAssertions.map(_assertion_json(_, realization.profile))),
      "profile" -> Json.fromString(realization.profile),
      "realizationIdentity" -> Json.fromString(realization.realizationIdentity),
      "relationships" -> Json.fromValues(realization.relationships.map(_relationship_json)),
      "schemaVersion" -> Json.fromString(realization.schemaVersion),
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

  private def _assertion_json(assertion: InternalModelSemanticAssertion, profile: String): Json = {
    val fields = Vector(
      "assertionId" -> Json.fromString(assertion.assertionId),
      "conditionIds" -> Json.fromValues(assertion.conditionIds.map(Json.fromString)),
      "content" -> Json.fromString(assertion.content),
      "semanticIdentity" -> Json.fromString(assertion.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(assertion.semanticIdentityKind),
      "sourceReferenceId" -> Json.fromString(assertion.sourceReferenceId)
    )
    val v2fields = if profile == "ccdm-realization-v2" then fields :+ ("association" -> assertion.association.map(_association_json).getOrElse(Json.Null)) else fields
    Json.obj(v2fields*)
  }

  private def _association_json(association: InternalModelSemanticAssociation): Json =
    Json.obj(
      "associationRole" -> Json.fromString(association.associationRole),
      "relatedSemanticIdentity" -> Json.fromString(association.relatedSemanticIdentity),
      "relatedSemanticIdentityKind" -> Json.fromString(association.relatedSemanticIdentityKind)
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

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
