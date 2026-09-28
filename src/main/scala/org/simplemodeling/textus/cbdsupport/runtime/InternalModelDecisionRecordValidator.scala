package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import java.security.MessageDigest

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
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
      InternalModelSemanticRealizationValidator.validateVerified(handoff.realizationPackage).flatMap { realization =>
        (for {
          ledger <- InternalModelDecisionRecordCodec.decode(handoff.decision)
          admission <- _admission(handoff, realization, ledger)
        } yield admission).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model decision record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _admission(
    handoff: InternalModelVerifiedDecisionPackage,
    realization: InternalModelSemanticRealization,
    ledger: InternalModelDecisionLedger
  ): Either[String, InternalModelDecisionAdmission] = {
    val currentdigest = _sha256(handoff.realizationPackage.realization.bytes.toArray)
    for {
      _ <- Either.cond(ledger.scope == realization.scope, (), "decision ledger scope does not equal selected realization scope")
      _ <- ledger.records.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          _ <- _record_admission(record, handoff.realizationPackage.realization, realization, currentdigest, ledger.scope)
        } yield ()
      }
      _ <- _chain(ledger.records)
    } yield InternalModelDecisionAdmission(ledger, realization)
  }

  private def _record_admission(
    record: InternalModelDecisionRecord,
    selected: InternalModelVerifiedRealization,
    realization: InternalModelSemanticRealization,
    currentdigest: String,
    ledgerscope: InternalModelSemanticScope
  ): Either[String, Unit] = {
    val current =
      record.basis.realizationArtifactId == selected.artifactId &&
        record.basis.realizationIdentity == realization.realizationIdentity &&
        record.basis.sha256 == currentdigest &&
        record.basis.scope == realization.scope
    for {
      _ <- Either.cond(record.basis.scope == ledgerscope, (), s"decision record ${record.decisionIdentity} basis scope does not equal ledger scope")
      _ <- Either.cond(
        (current && record.basis.status == "current") || (!current && record.basis.status == "historical-unverified"),
        (),
        s"decision record ${record.decisionIdentity} basis status does not match its exact current basis"
      )
      _ <- Either.cond(!current || record.state == "superseded" || record.state == "accepted", (), s"decision record ${record.decisionIdentity} current state is invalid")
      _ <- Either.cond(current || record.state == "superseded", (), s"decision record ${record.decisionIdentity} historical basis must be superseded")
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
      _ <- Either.cond(record.realizationConditionIds.forall(conditionids.contains), (), s"decision record ${record.decisionIdentity} names an unknown current realization condition")
      _ <- Either.cond(requiredtargetconditions.subsetOf(record.realizationConditionIds.toSet), (), s"decision record ${record.decisionIdentity} hides an affected-target condition")
      _ <- Either.cond(evidenceconditions.subsetOf(record.realizationConditionIds.toSet), (), s"decision record ${record.decisionIdentity} omits an evidence condition from its realization condition set")
      _ <- record.consideredEvidence.foldLeft[Either[String, Unit]](Right(())) { (result, evidence) =>
        for {
          _ <- result
          _ <- _evidence(evidence, record.decisionIdentity, realization)
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
      case "realization-source" =>
        for {
          referenceid <- evidence.sourceReferenceId.toRight(s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} lacks a realization source reference")
          reference <- realization.sourceReferences.find(_.referenceId == referenceid).toRight(s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} names an unknown current source reference")
          _ <- Either.cond(reference.source == evidence.source, (), s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} source does not equal its admitted realization reference")
          expectedconditions = realization.conditions.filter(_.sourceReferenceId == referenceid).map(_.conditionId)
          _ <- Either.cond(evidence.conditionIds == expectedconditions, (), s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} hides or changes source limitations")
        } yield ()
      case "external-human" | "provider-proposal" | "external-other" =>
        Either.cond(evidence.sourceReferenceId.isEmpty && evidence.conditionIds.isEmpty, (), s"decision record $decisionidentity external evidence must not link realization sources or conditions")
      case _ => Left(s"decision record $decisionidentity evidence ${evidence.evidenceIdentity} has an invalid kind")
    }

  private def _chain(records: Vector[InternalModelDecisionRecord]): Either[String, Unit] = {
    val byid = records.map(record => record.decisionIdentity -> record).toMap
    for {
      _ <- records.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          _ <- _predecessor(record, byid)
          successors = records.filter(_.supersedes.contains(record.decisionIdentity))
          _ <- record.state match {
            case "superseded" => Either.cond(successors.size == 1, (), s"superseded decision ${record.decisionIdentity} must have exactly one successor")
            case "accepted" => Either.cond(successors.isEmpty, (), s"accepted decision ${record.decisionIdentity} must not have a successor")
            case _ => Left(s"decision record ${record.decisionIdentity} has an invalid state")
          }
          _ <- Either.cond(!_has_cycle(record, byid), (), s"decision record ${record.decisionIdentity} has a supersession cycle")
        } yield ()
      }
      _ <- records.map(_.topicIdentity).distinct.foldLeft[Either[String, Unit]](Right(())) { (result, topic) =>
        for {
          _ <- result
          terminal = records.filter(record => record.topicIdentity == topic && record.state == "accepted")
          _ <- Either.cond(terminal.size == 1, (), s"decision topic $topic must have exactly one accepted terminal")
        } yield ()
      }
    } yield ()
  }

  private def _predecessor(
    record: InternalModelDecisionRecord,
    byid: Map[String, InternalModelDecisionRecord]
  ): Either[String, Unit] =
    record.supersedes match {
      case Some(predecessorid) =>
        for {
          predecessor <- byid.get(predecessorid).toRight(s"decision record ${record.decisionIdentity} supersedes an unknown decision")
          _ <- Either.cond(predecessor.decisionIdentity != record.decisionIdentity, (), s"decision record ${record.decisionIdentity} must not supersede itself")
          _ <- Either.cond(predecessor.topicIdentity == record.topicIdentity, (), s"decision record ${record.decisionIdentity} supersedes a decision in another topic")
        } yield ()
      case None => Right(())
    }

  private def _has_cycle(
    start: InternalModelDecisionRecord,
    byid: Map[String, InternalModelDecisionRecord]
  ): Boolean = {
    var current = Option(start)
    var visited = Set.empty[String]
    var cycle = false
    while current.nonEmpty && !cycle do {
      val record = current.get
      if visited.contains(record.decisionIdentity) then cycle = true
      else {
        visited = visited + record.decisionIdentity
        current = record.supersedes.flatMap(byid.get)
      }
    }
    cycle
  }

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
}
