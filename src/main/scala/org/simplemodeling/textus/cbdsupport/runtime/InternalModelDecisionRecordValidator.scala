package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 28, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits a captured decision ledger against its one captured semantic realization. */
private[runtime] object InternalModelDecisionRecordValidator {
  def validate(projectRoot: Path): Consequence[InternalModelDecisionAdmission] =
    try {
      InternalModelPackageValidator.verifiedDecisionRecords(projectRoot).flatMap(validateVerified)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model decision record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def validateVerified(handoff: InternalModelVerifiedDecisionPackage): Consequence[InternalModelDecisionAdmission] =
    try {
      _captured_paths(handoff).fold(Consequence.operationInvalid, _ => InternalModelSemanticRealizationValidator.validateVerified(handoff.realizationpackage).flatMap { realization =>
        (for {
          ledger <- InternalModelDecisionRecordCodec.decode(handoff.decision)
          admission <- _admission(handoff, realization, ledger)
        } yield admission).fold(Consequence.operationInvalid, Consequence.success)
      })
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model decision record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _captured_paths(handoff: InternalModelVerifiedDecisionPackage): Either[String, Unit] =
    for {
      _ <- Either.cond(handoff != null && handoff.decision != null && handoff.realizationpackage != null, (), "decision capture, decision and realization package must be present")
      selected = handoff.realizationpackage
      _ <- Either.cond(selected.realization != null && selected.sourcesnapshots != null, (), "captured realization and source-snapshot collection must be present")
      _ <- Either.cond(selected.realization.path != null && !selected.realization.path.isBlank, (), "captured realization path must be present")
      _ <- selected.sourcesnapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        result.flatMap(_ => Either.cond(snapshot != null && snapshot.path != null && !snapshot.path.isBlank, (), "captured source-snapshot and path must be present"))
      }
    } yield ()

  private def _admission(
    handoff: InternalModelVerifiedDecisionPackage,
    realization: InternalModelSemanticRealization,
    ledger: InternalModelDecisionLedger
  ): Either[String, InternalModelDecisionAdmission] = {
    for {
      _ <- Either.cond(handoff.decision.dependencies.contains(handoff.realizationpackage.realization.reference), (), "selected decision must depend on the exact selected realization artifact reference")
      _ <- Either.cond(ledger.scope == realization.scope, (), "decision ledger scope does not equal selected realization scope")
      _ <- ledger.records.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          _ <- _record_admission(record, handoff.realizationpackage.realization, realization, ledger.scope)
        } yield ()
      }
      _ <- _chain(ledger.records)
    } yield InternalModelDecisionAdmission(ledger, realization)
  }

  private def _record_admission(
    record: InternalModelDecisionRecord,
    selected: InternalModelVerifiedRealization,
    realization: InternalModelSemanticRealization,
    ledgerscope: InternalModelSemanticScope
  ): Either[String, Unit] = {
    val current =
      record.basis.realizationArtifactReference == selected.reference &&
        record.basis.realizationReference == realization.realizationReference &&
        record.basis.scope == realization.scope
    for {
      _ <- Either.cond(record.basis.scope == ledgerscope, (), s"decision record ${record.decisionReference.recordId.value} basis scope does not equal ledger scope")
      _ <- Either.cond(
        (current && record.basis.status == InternalModelDecisionBasisStatus.Current) || (!current && record.basis.status == InternalModelDecisionBasisStatus.HistoricalUnverified),
        (),
        s"decision record ${record.decisionReference.recordId.value} basis status does not match its exact current basis"
      )
      _ <- Either.cond(!current || record.state == InternalModelDecisionState.Superseded || record.state == InternalModelDecisionState.Accepted, (), s"decision record ${record.decisionReference.recordId.value} current state is invalid")
      _ <- Either.cond(current || record.state == InternalModelDecisionState.Superseded, (), s"decision record ${record.decisionReference.recordId.value} historical basis must be superseded")
      _ <- if current then _current_record(record, realization) else Right(())
    } yield ()
  }

  private def _current_record(
    record: InternalModelDecisionRecord,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    val targets = record.affectedTargets.toSet
    val conditionids = realization.conditions.map(_.conditionId).toSet
    val requiredtargetconditions = realization.conditions.collect {
      case condition if targets.contains(InternalModelSemanticTarget(condition.affectedKind, condition.affectedIdentity)) => condition.conditionId
    }.toSet
    val evidenceconditions = record.consideredEvidence.flatMap(_.conditionIds).toSet
    for {
      _ <- record.affectedTargets.foldLeft[Either[String, Unit]](Right(())) { (result, target) =>
        for {
          _ <- result
          _ <- _target_exists(target, realization)
        } yield ()
      }
      _ <- Either.cond(record.realizationConditionIds.forall(conditionids.contains), (), s"decision record ${record.decisionReference.recordId.value} names an unknown current realization condition")
      _ <- Either.cond(requiredtargetconditions.subsetOf(record.realizationConditionIds.toSet), (), s"decision record ${record.decisionReference.recordId.value} hides an affected-target condition")
      _ <- Either.cond(evidenceconditions.subsetOf(record.realizationConditionIds.toSet), (), s"decision record ${record.decisionReference.recordId.value} omits an evidence condition from its realization condition set")
      _ <- record.consideredEvidence.foldLeft[Either[String, Unit]](Right(())) { (result, evidence) =>
        for {
          _ <- result
          _ <- _evidence(evidence, record.decisionReference.recordId.value, realization)
        } yield ()
      }
    } yield ()
  }

  private def _target_exists(target: InternalModelSemanticTarget, realization: InternalModelSemanticRealization): Either[String, Unit] =
    target.semanticIdentityKind match {
      case "element" => Either.cond(realization.elements.exists(_.identity == target.semanticIdentity), (), s"decision target element ${target.semanticIdentity} is unknown or cross-scope")
      case "relationship" => Either.cond(realization.relationships.exists(_.identity == target.semanticIdentity), (), s"decision target relationship ${target.semanticIdentity} is unknown or cross-scope")
      case _ => Left(s"decision target ${target.semanticIdentity} has an invalid semantic identity kind")
    }

  private def _evidence(
    evidence: InternalModelDecisionEvidence,
    decisionidentity: String,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] =
    evidence.kind match {
      case InternalModelDecisionEvidenceKind.RealizationSource =>
        for {
          referenceid <- evidence.sourceReferenceId.toRight(s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} lacks a realization source reference")
          reference <- realization.sourceReferences.find(_.referenceId == referenceid).toRight(s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} names an unknown current source reference")
          _ <- Either.cond(reference.source == evidence.source, (), s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} source does not equal its admitted realization reference")
          expectedconditions = realization.conditions.filter(_.sourceReferenceId == referenceid).map(_.conditionId)
          _ <- Either.cond(evidence.conditionIds == expectedconditions, (), s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} hides or changes source limitations")
        } yield ()
      case InternalModelDecisionEvidenceKind.ExternalHuman | InternalModelDecisionEvidenceKind.ProviderProposal | InternalModelDecisionEvidenceKind.ExternalOther =>
        Either.cond(evidence.sourceReferenceId.isEmpty && evidence.conditionIds.isEmpty, (), s"decision record $decisionidentity external evidence must not link realization sources or conditions")
    }

  private def _chain(records: Vector[InternalModelDecisionRecord]): Either[String, Unit] = {
    val byreference = records.map(record => record.decisionReference -> record).toMap
    for {
      _ <- records.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          _ <- _predecessor(record, byreference)
          successors = records.filter(_.supersedes.contains(record.decisionReference))
          _ <- record.state match {
            case InternalModelDecisionState.Superseded => Either.cond(successors.size == 1, (), s"superseded decision ${record.decisionReference.recordId.value} must have exactly one successor")
            case InternalModelDecisionState.Accepted => Either.cond(successors.isEmpty, (), s"accepted decision ${record.decisionReference.recordId.value} must not have a successor")
          }
          _ <- Either.cond(!_has_cycle(record, byreference), (), s"decision record ${record.decisionReference.recordId.value} has a supersession cycle")
        } yield ()
      }
      _ <- records.map(_.topicIdentity).distinct.foldLeft[Either[String, Unit]](Right(())) { (result, topic) =>
        for {
          _ <- result
          terminal = records.filter(record => record.topicIdentity == topic && record.state == InternalModelDecisionState.Accepted)
          _ <- Either.cond(terminal.size == 1, (), s"decision topic $topic must have exactly one accepted terminal")
        } yield ()
      }
    } yield ()
  }

  private def _predecessor(
    record: InternalModelDecisionRecord,
    byreference: Map[InternalModelRecordReference, InternalModelDecisionRecord]
  ): Either[String, Unit] =
    record.supersedes match {
      case Some(predecessorid) =>
        for {
          predecessor <- byreference.get(predecessorid).toRight(s"decision record ${record.decisionReference.recordId.value} supersedes an unknown decision")
          _ <- Either.cond(predecessor.decisionReference != record.decisionReference, (), s"decision record ${record.decisionReference.recordId.value} must not supersede itself")
          _ <- Either.cond(predecessor.topicIdentity == record.topicIdentity && predecessor.basis.scope == record.basis.scope, (), s"decision record ${record.decisionReference.recordId.value} supersedes a decision in another topic or scope")
        } yield ()
      case None => Right(())
    }

  private def _has_cycle(
    start: InternalModelDecisionRecord,
    byreference: Map[InternalModelRecordReference, InternalModelDecisionRecord]
  ): Boolean = {
    var current = Option(start)
    var visited = Set.empty[InternalModelRecordReference]
    var cycle = false
    while current.nonEmpty && !cycle do {
      val record = current.get
      if visited.contains(record.decisionReference) then cycle = true
      else {
        visited = visited + record.decisionReference
        current = record.supersedes.flatMap(byreference.get)
      }
    }
    cycle
  }

}
