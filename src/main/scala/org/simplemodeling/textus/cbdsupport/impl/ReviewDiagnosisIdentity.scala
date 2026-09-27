package org.simplemodeling.textus.cbdsupport.impl

import java.nio.charset.StandardCharsets
import java.util.UUID

import org.goldenport.Consequence
import org.goldenport.id.UniversalId
import org.simplemodeling.textus.cbdsupport.entity.ReviewDiagnosis
import org.simplemodeling.textus.cbdsupport.entity.create.ReviewRetentionEvent
import org.simplemodeling.textus.cbdsupport.runtime.{CarReviewExecutionPlan, ReviewInstant}

/*
 * @since   Sep. 27, 2026
 * @version Sep. 27, 2026
 * @author  ASAMI, Tomoharu
 */
private[impl] object ReviewDiagnosisIdentity {
  private[impl] def _diagnosis_id(plan: CarReviewExecutionPlan): Consequence[org.simplemodeling.model.datatype.EntityId] = {
    val seed = s"${plan.reuseKey.definitionId}:${plan.reuseKey.digest.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection = ReviewDiagnosis.collectionId
    org.simplemodeling.model.datatype.EntityId.bridgeFromParts(
      collection.major,
      collection.minor,
      collection,
      UniversalId.StableTimestamp,
      key
    )
  }

  private[impl] def _snapshot_id(kind: String, diagnosis: org.simplemodeling.model.datatype.EntityId, identity: String, collection: org.simplemodeling.model.datatype.EntityCollectionId): Consequence[org.simplemodeling.model.datatype.EntityId] = {
    val seed = s"${diagnosis.value}:$kind:$identity"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId.bridgeFromParts(
      collection.major,
      collection.minor,
      collection,
      UniversalId.StableTimestamp,
      key
    )
  }

  private[impl] def _retention_event_id(
    kind: String,
    diagnosis: org.simplemodeling.model.datatype.EntityId,
    recordid: String,
    effectiveat: ReviewInstant
  ): Consequence[org.simplemodeling.model.datatype.EntityId] = {
    val seed = s"${diagnosis.value}:$kind:$recordid:${effectiveat.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    val collection = ReviewRetentionEvent.collectionId
    org.simplemodeling.model.datatype.EntityId.bridgeFromParts(
      collection.major,
      collection.minor,
      collection,
      UniversalId.StableTimestamp,
      key
    )
  }

  private[impl] def _history_snapshot_id(
    kind: String,
    diagnosis: org.simplemodeling.model.datatype.EntityId,
    identity: String,
    collection: org.simplemodeling.model.datatype.EntityCollectionId
  ): Consequence[org.simplemodeling.model.datatype.EntityId] = {
    val seed = s"${diagnosis.value}:$kind:$identity"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId.bridgeFromParts(
      collection.major,
      collection.minor,
      collection,
      UniversalId.StableTimestamp,
      key
    )
  }
}
