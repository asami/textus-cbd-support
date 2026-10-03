package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.util.control.NonFatal
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.entity.EntityStore
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter}
import org.goldenport.id.UniversalId
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.model.directive.Update
import org.simplemodeling.model.value.ContentBody
import org.simplemodeling.textus.cbdsupport.entity.{InternalModelHistoryEntry as HistoryEntity}
import org.simplemodeling.textus.cbdsupport.entity.create.{InternalModelHistoryEntry as HistoryCreate}
import org.simplemodeling.textus.cbdsupport.entity.update.{InternalModelHistoryEntry as HistoryUpdate}
import org.simplemodeling.textus.cbdsupport.value.{InternalModelHistoryDocument as HistoryValue}

/**
 * Exact-selection retained storage, owned by the generated Entity and standard UnitOfWork.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 */
private[runtime] enum InternalModelHistoryResult {
  case Unavailable, Missing
  case Retained(selection: InternalModelHistorySelection, record: InternalModelRetainedHistoryRecord, retainedAt: Instant)
  case ExpiryRequired(reference: InternalModelRecordReference, packageReference: InternalModelPackageReference,
    scope: InternalModelSemanticScope, kind: InternalModelRetainedHistoryKind, expiresAt: Instant)
  case Removed(tombstone: InternalModelRetainedHistoryTombstone)
}

private[runtime] final case class InternalModelHistorySupersessionEndpoints(
  previous: InternalModelHistorySelection,
  successor: InternalModelHistorySelection
)

/** Production callers reach this boundary only through the authenticated adapter. */
private[runtime] abstract class InternalModelRetainedHistoryStore {
  def read(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult]
  def record(document: InternalModelHistoryDocument,
    endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[InternalModelHistoryResult]
  def expire(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult]
  def delete(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult]
}

private[runtime] object InternalModelRetainedHistoryStore {
  def entityBacked(core: ActionCall.Core): InternalModelRetainedHistoryStore = new EntityBacked(core)

  def entityId(selection: InternalModelHistorySelection): Consequence[EntityId] = {
    InternalModelRetainedHistoryCodec.validateSelection(selection).flatMap { admitted =>
      val collection = HistoryEntity.collectionId
      EntityId.bridgeFromParts(collection.major, collection.minor, collection,
        UniversalId.StableTimestamp, admitted.storageId.value.replace("-", ""))
    }
  }

  private final class EntityBacked(val core: ActionCall.Core)
      extends InternalModelRetainedHistoryStore with ActionCall.Core.Holder with ActionCallEntityStorePart {
    def read(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] = {
      for {
        id <- entityId(selection)
        loaded <- _run(entity_load_option_internal[HistoryEntity](id))
        result <- loaded match {
          case None => Consequence.success(InternalModelHistoryResult.Missing)
          case Some(entity) => _decode(selection, id, entity).flatMap(_result)
        }
      } yield result
    }

    def record(document: InternalModelHistoryDocument,
      endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[InternalModelHistoryResult] = {
      for {
        admitted <- InternalModelRetainedHistoryCodec.validate(document)
        record <- admitted.content match {
          case InternalModelHistoryDocumentState.Retained(value, _) => Consequence.success(value)
          case _ => _invalid("record-state")
        }
        _ <- _supersession(record, endpoints)
        id <- entityId(admitted.selection)
        now <- _now()
        stored = admitted.copy(content = InternalModelHistoryDocumentState.Retained(record, now))
        bytes <- InternalModelRetainedHistoryCodec.encode(stored)
        candidate <- HistoryCreate.Builder().withId(Some(id)).withHistory_document(_value(bytes)).buildC()
        claimed <- _run(entity_claim_or_load_internal[HistoryCreate, HistoryEntity](candidate))
        result <- claimed match {
          case _: EntityStore.EntityClaimResult.Claimed[?] => _result(stored)
          case _: EntityStore.EntityClaimResult.Loaded[?] => _invalid("occupied-selection")
        }
      } yield result
    }

    def expire(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] =
      _remove(selection, InternalModelHistoryRetentionAction.Expired)

    def delete(selection: InternalModelHistorySelection): Consequence[InternalModelHistoryResult] =
      _remove(selection, InternalModelHistoryRetentionAction.Deleted)

    private def _remove(selection: InternalModelHistorySelection,
      action: InternalModelHistoryRetentionAction): Consequence[InternalModelHistoryResult] = {
      for {
        id <- entityId(selection)
        loaded <- _run(entity_load_option_internal[HistoryEntity](id))
        result <- loaded match {
          case None => Consequence.success(InternalModelHistoryResult.Missing)
          case Some(_) => for {
            snapshot <- _run(entity_load_snapshot_internal[HistoryEntity](id))
            document <- _decode(selection, id, snapshot.entity)
            result <- document.content match {
              case InternalModelHistoryDocumentState.Removed(tombstone) =>
                Consequence.success(InternalModelHistoryResult.Removed(tombstone))
              case InternalModelHistoryDocumentState.Retained(record, retainedat) => for {
                now <- _now()
                expiry <- InternalModelRetainedHistoryCodec.expiresAt(retainedat)
                _ <- if (action == InternalModelHistoryRetentionAction.Deleted || !now.isBefore(expiry)) Consequence.unit
                  else _invalid("expiry-not-due")
                tombstone = InternalModelRetainedHistoryTombstone(selection.reference, selection.packageReference,
                  selection.scope, record.payload.kind, action, now)
                bytes <- InternalModelRetainedHistoryCodec.encode(
                  InternalModelHistoryDocument(selection, InternalModelHistoryDocumentState.Removed(tombstone)))
                patch <- HistoryUpdate.Builder().withHistory_document(Update.set(_value(bytes))).buildC()
                _ <- _run(entity_update_internal(id, patch, snapshot.revision))
              } yield InternalModelHistoryResult.Removed(tombstone)
            }
          } yield result
        }
      } yield result
    }

    private def _supersession(record: InternalModelRetainedHistoryRecord,
      endpoints: Option[InternalModelHistorySupersessionEndpoints]): Consequence[Unit] = {
      if (endpoints == null || endpoints.exists(_ == null)) _invalid("supersession-endpoints")
      else (record.payload, endpoints) match {
        case (_: InternalModelRetainedHistoryPayload.Supersession, Some(pair)) => for {
          _ <- InternalModelRetainedHistoryCodec.validateSelection(pair.previous)
            .zip(InternalModelRetainedHistoryCodec.validateSelection(pair.successor))
          previous <- _live(pair.previous)
          successor <- _live(pair.successor)
          _ <- InternalModelRetainedHistoryValidator.validateSupersession(record, previous, successor)
        } yield ()
        case (_: InternalModelRetainedHistoryPayload.Supersession, None) => _invalid("supersession-endpoints")
        case (_, Some(_)) => _invalid("unexpected-supersession-endpoints")
        case (_, None) => Consequence.unit
      }
    }

    private def _live(selection: InternalModelHistorySelection): Consequence[InternalModelRetainedHistoryRecord] =
      read(selection).flatMap {
        case InternalModelHistoryResult.Retained(_, record, _) => Consequence.success(record)
        case _ => _invalid("supersession-live-endpoint")
      }

    private def _decode(selection: InternalModelHistorySelection, id: EntityId,
      entity: HistoryEntity): Consequence[InternalModelHistoryDocument] = {
      if (entity == null || entity.id != id || entity.history_document == null || entity.history_document.value == null)
        _invalid("stored-entity")
      else {
        val text = entity.history_document.value.value
        if (text == null) _invalid("stored-document")
        else InternalModelRetainedHistoryCodec.decodeText(text).flatMap { document =>
          if (document.selection == selection) Consequence.success(document) else _invalid("stored-selection")
        }
      }
    }

    private def _result(document: InternalModelHistoryDocument): Consequence[InternalModelHistoryResult] = document.content match {
      case InternalModelHistoryDocumentState.Removed(tombstone) => Consequence.success(InternalModelHistoryResult.Removed(tombstone))
      case InternalModelHistoryDocumentState.Retained(record, retainedat) => for {
        expiry <- InternalModelRetainedHistoryCodec.expiresAt(retainedat)
        now <- _now()
      } yield {
        if (now.isBefore(expiry)) InternalModelHistoryResult.Retained(document.selection, record, retainedat)
        else InternalModelHistoryResult.ExpiryRequired(record.reference, record.subject.packageReference,
          record.subject.scope, record.payload.kind, expiry)
      }
    }

    private def _value(bytes: Vector[Byte]): HistoryValue =
      HistoryValue(ContentBody(new String(bytes.toArray, StandardCharsets.UTF_8)))

    private def _now(): Consequence[Instant] = {
      try Consequence.success(core.executionContext.clock.instant())
      catch { case NonFatal(_) => _invalid("server-clock") }
    }

    private def _run[A](program: => ExecUowM[A]): Consequence[A] = {
      try new UnitOfWorkInterpreter(new UnitOfWork(core.executionContext)).run(program)
        .recoverWith(_ => Consequence.dataStoreUnavailable("internal-model history storage unavailable"))
      catch { case NonFatal(_) => Consequence.dataStoreUnavailable("internal-model history storage unavailable") }
    }
  }

  private def _invalid[A](dimension: String): Consequence[A] =
    Consequence.operationInvalid("internal-model history invalid: " + dimension)
}
