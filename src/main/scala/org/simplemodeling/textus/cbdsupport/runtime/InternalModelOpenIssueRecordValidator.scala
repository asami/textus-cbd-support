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
/** Admits one captured open-issue ledger against its one captured semantic realization. */
private[runtime] object InternalModelOpenIssueRecordValidator {
  def validate(projectRoot: Path): Consequence[InternalModelOpenIssueAdmission] = {
    try {
      InternalModelPackageValidator.verifiedOpenIssueRecords(projectRoot).flatMap(validateVerified)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model open-issue record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }
  }

  private[runtime] def validateVerified(handoff: InternalModelVerifiedOpenIssuePackage): Consequence[InternalModelOpenIssueAdmission] = {
    try {
      InternalModelSemanticRealizationValidator.validateVerified(handoff.realizationPackage).flatMap { realization =>
        (for {
          ledger <- InternalModelOpenIssueRecordCodec.decode(handoff.openIssue)
          admission <- _admission(handoff, realization, ledger)
        } yield admission).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model open-issue record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }
  }

  private def _admission(
    handoff: InternalModelVerifiedOpenIssuePackage,
    realization: InternalModelSemanticRealization,
    ledger: InternalModelOpenIssueLedger
  ): Either[String, InternalModelOpenIssueAdmission] = {
    val selected = handoff.realizationPackage.realization
    val currentdigest = _sha256(selected.bytes.toArray)
    for {
      _ <- Either.cond(ledger.scope == realization.scope, (), "open-issue ledger scope does not equal selected realization scope")
      _ <- Either.cond(ledger.basis.realizationArtifactId == selected.artifactId, (), "open-issue ledger basis artifact does not equal selected realization artifact")
      _ <- Either.cond(ledger.basis.realizationIdentity == realization.realizationIdentity, (), "open-issue ledger basis identity does not equal selected realization identity")
      _ <- Either.cond(ledger.basis.sha256 == currentdigest, (), "open-issue ledger basis digest does not equal selected realization bytes")
      _ <- _first_failure(ledger.issues.iterator.map(issue => _issue_admission(issue, realization)))
    } yield InternalModelOpenIssueAdmission(ledger, realization)
  }

  private def _issue_admission(
    issue: InternalModelOpenIssueRecord,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    val conditionids = realization.conditions.map(_.conditionId).toSet
    val targets = issue.affectedTargets.toSet
    val targetconditions =
      if issue.affectedTargets.isEmpty then realization.conditions.map(_.conditionId).toSet
      else realization.conditions.collect {
        case condition if targets.contains(InternalModelSemanticTarget(condition.affectedKind, condition.affectedIdentity)) => condition.conditionId
      }.toSet
    val evidenceconditions = issue.consideredEvidence.flatMap(_.conditionIds).toSet
    for {
      _ <- _first_failure(issue.affectedTargets.iterator.map(target => _target_exists(target, realization, issue.issueIdentity)))
      _ <- Either.cond(issue.realizationConditionIds.forall(conditionids.contains), (), s"open-issue record ${issue.issueIdentity} names an unknown current realization condition")
      _ <- Either.cond(targetconditions.subsetOf(issue.realizationConditionIds.toSet), (), s"open-issue record ${issue.issueIdentity} hides a required affected-target or scope-wide condition")
      _ <- Either.cond(evidenceconditions.subsetOf(issue.realizationConditionIds.toSet), (), s"open-issue record ${issue.issueIdentity} omits an evidence condition from its realization condition set")
      _ <- _first_failure(issue.consideredEvidence.iterator.map(evidence => _evidence(evidence, issue.issueIdentity, realization)))
      _ <- _first_failure(issue.options.iterator.map(option => _option(option, issue)))
    } yield ()
  }

  private def _target_exists(
    target: InternalModelSemanticTarget,
    realization: InternalModelSemanticRealization,
    issueidentity: String
  ): Either[String, Unit] = {
    target.semanticIdentityKind match {
      case "element" => Either.cond(realization.elements.exists(_.identity == target.semanticIdentity), (), s"open-issue record $issueidentity targets an unknown or cross-scope element")
      case "relationship" => Either.cond(realization.relationships.exists(_.identity == target.semanticIdentity), (), s"open-issue record $issueidentity targets an unknown or cross-scope relationship")
      case _ => Left(s"open-issue record $issueidentity target has an invalid semantic identity kind")
    }
  }

  private def _evidence(
    evidence: InternalModelOpenIssueEvidence,
    issueidentity: String,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    evidence.kind match {
      case InternalModelOpenIssueEvidenceKind.RealizationSource =>
        for {
          referenceid <- evidence.sourceReferenceId.toRight(s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} lacks a realization source reference")
          reference <- realization.sourceReferences.find(_.referenceId == referenceid).toRight(s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} names an unknown current source reference")
          _ <- Either.cond(reference.source == evidence.source, (), s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} source does not equal its admitted realization reference")
          expectedconditions = realization.conditions.filter(_.sourceReferenceId == referenceid).map(_.conditionId)
          _ <- Either.cond(evidence.conditionIds == expectedconditions, (), s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} hides or changes source-reference conditions")
        } yield ()
      case InternalModelOpenIssueEvidenceKind.ExternalHuman |
          InternalModelOpenIssueEvidenceKind.ProviderProposal |
          InternalModelOpenIssueEvidenceKind.ExternalOther =>
        Either.cond(evidence.sourceReferenceId.isEmpty && evidence.conditionIds.isEmpty, (), s"open-issue record $issueidentity external evidence must not link realization sources or conditions")
    }
  }

  private def _option(
    option: InternalModelOpenIssueOption,
    issue: InternalModelOpenIssueRecord
  ): Either[String, Unit] = {
    val evidenceids = issue.consideredEvidence.map(_.evidenceIdentity).toSet
    Either.cond(
      option.evidenceIds.forall(evidenceids.contains),
      (),
      s"open-issue record ${issue.issueIdentity} option ${option.optionIdentity} links evidence outside its owning issue"
    )
  }

  private def _first_failure(values: Iterator[Either[String, Unit]]): Either[String, Unit] = {
    values.collectFirst { case Left(reason) => reason } match {
      case Some(reason) => Left(reason)
      case None => Right(())
    }
  }

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
}
