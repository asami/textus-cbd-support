package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.finding.FindingDispositionV1 as Disposition
import Disposition.{Coverage, Declaration, Inspection, Key, Kind, Report, Status}

/*
 * @since   Oct. 31, 2026
 * @version Nov.  5, 2026
 */
/** CBD working state; CNCF owns disposition meaning and source owners apply it. */
object FindingResolution {
  enum Availability {
    case Unavailable, Unsupported, Failed
  }
  final case class CoverageGap(coverage: Coverage, availability: Availability, detail: String)
  final case class Remediation(key: Key, pattern: String)
  final case class Group(rule: String, pattern: String, keys: Vector[Key])
  final case class Grouping(groups: Vector[Group], unassigned: Vector[Key])
  final case class Proposal(reference: InternalModelRecordReference, imported: InternalModelRecordReference,
    keys: Vector[Key], kind: Kind, reason: String, provenance: String)
  enum Choice {
    case Admit, Reject
  }
  final case class Decision(reference: InternalModelRecordReference, imported: InternalModelRecordReference,
    proposal: InternalModelRecordReference, keys: Vector[Key], choice: Choice, reason: String,
    principalId: String, direction: InternalModelRecordReference)

  /** Supplied by the authenticated host, never decoded from client proposal/decision JSON. */
  private[runtime] final case class DeveloperAuthority(principalid: String,
    direction: InternalModelRecordReference, imported: InternalModelRecordReference,
    proposal: InternalModelRecordReference, keys: Set[Key], kinds: Set[Kind])

  final class Working private[FindingResolution] (
    val reference: InternalModelRecordReference,
    val inspection: Inspection,
    val declarations: Vector[Declaration],
    val gaps: Vector[CoverageGap],
    val report: Report,
    val proposals: Vector[Proposal],
    val decisions: Vector[Decision]
  ) {
    def rawFindingCount: Int = inspection.findings.size
    def actionableCount: Int = report.actionableBacklog.size

    def groupC(assignments: Vector[Remediation]): Consequence[Grouping] = {
      val keys = inspection.findings.map(_.key)
      for {
        _ <- _require(assignments.map(_.key).distinct.size == assignments.size && assignments.forall(value =>
          keys.contains(value.key) && _text(value.pattern)), "Grouping requires distinct known keys and explicit remediation patterns")
      } yield Grouping(assignments.groupBy(value => (value.key.rule, value.pattern)).toVector
        .sortBy(_._1).map { case ((rule, pattern), members) => Group(rule, pattern, _ordered(members.map(_.key))) },
        _ordered(keys.filterNot(assignments.map(_.key).toSet)))
    }

    def proposeC(proposalReference: InternalModelRecordReference, keys: Vector[Key], kind: Kind,
      reason: String, provenance: String): Consequence[Working] = for {
      _ <- _require(!proposals.exists(_.reference == proposalReference) && proposalReference != reference,
        "A proposal requires its own explicit unused record reference")
      _ <- _require(keys.nonEmpty && keys.distinct.size == keys.size &&
        keys.forall(report.results.filter(_.status == Status.Unresolved).map(_.finding.key).contains),
        "Proposal scope must explicitly select distinct unresolved finding keys")
      _ <- _validate_declarations(keys.map(key => Declaration(key, kind, reason, provenance)))
    } yield new Working(reference, inspection, declarations, gaps, report,
      proposals :+ Proposal(proposalReference, reference, _ordered(keys), kind, reason, provenance), decisions)

    private[runtime] def decideC(decisionReference: InternalModelRecordReference,
      proposalReference: InternalModelRecordReference, keys: Vector[Key], choice: Choice,
      reason: String, authority: DeveloperAuthority): Consequence[Working] = for {
      proposal <- proposals.find(_.reference == proposalReference)
        .map(Consequence.success).getOrElse(Consequence.argumentInvalid("Select an existing exact proposal reference"))
      _ <- _require(!decisions.exists(_.reference == decisionReference) && decisionReference != reference &&
        !proposals.exists(_.reference == decisionReference), "A decision requires its own explicit unused record reference")
      _ <- _require(!decisions.exists(_.proposal == proposalReference) &&
        (choice != Choice.Admit || !decisions.exists(value => value.choice == Choice.Admit && value.keys.exists(keys.contains))),
        "Select an undecided proposal without overlapping already admitted work")
      _ <- _require(choice != null && _text(reason) && _text(authority.principalid) &&
        authority.imported == reference && authority.proposal == proposal.reference && authority.kinds(proposal.kind),
        "Developer authority must bind the exact import, proposal and disposition kind")
      _ <- _require(keys.nonEmpty && keys.distinct.size == keys.size &&
        keys.forall(key => proposal.keys.contains(key) && authority.keys(key)),
        "Developer admission requires explicit keys within both proposal and host authority")
    } yield new Working(reference, inspection, declarations, gaps, report, proposals,
      decisions :+ Decision(decisionReference, reference, proposal.reference, _ordered(keys), choice, reason,
        authority.principalid, authority.direction))

    /** Candidate declarations are available only for this retained admitted decision. */
    def admittedDeclarationsC(decisionReference: InternalModelRecordReference): Consequence[Vector[Declaration]] =
      decisions.find(_.reference == decisionReference) match {
        case Some(decision) if decision.choice == Choice.Admit =>
          val proposal = proposals.find(_.reference == decision.proposal).get
          Consequence.success(decision.keys.map(key => Declaration(key, proposal.kind, proposal.reason, proposal.provenance)))
        case _ => Consequence.argumentInvalid("Select a retained admitted decision; a proposal or rejection grants no work")
      }
  }

  def importC(reference: InternalModelRecordReference, inspection: Inspection,
    declarations: Vector[Declaration], gaps: Vector[CoverageGap]): Consequence[Working] = for {
    report <- Disposition.interpretC(inspection, inspection, declarations)
    _ <- _require(gaps.map(_.coverage).distinct.size == gaps.size && gaps.forall(value =>
      !inspection.coverage(value.coverage) && value.availability != null && _text(value.detail)),
      "Incomplete check scopes must be distinct, explicit and outside completed coverage")
    _ <- gaps.foldLeft(Consequence.unit) { (prior, gap) => prior.flatMap { _ =>
      Disposition.canonicalRuleC(gap.coverage.rule).flatMap(_ =>
        _require(_text(gap.coverage.target.scope) && _text(gap.coverage.target.identity),
          "Incomplete coverage requires an exact semantic target"))
    } }
  } yield new Working(reference, inspection, declarations, gaps, report, Vector.empty, Vector.empty)

  private def _require(condition: Boolean, message: String): Consequence[Unit] =
    if (condition) Consequence.unit else Consequence.argumentInvalid(message)

  private def _text(value: String): Boolean =
    value != null && value.nonEmpty && value == value.trim && !value.exists(_.isControl)

  private def _ordered(keys: Vector[Key]): Vector[Key] =
    keys.sortBy(key => (key.target.scope, key.target.identity, key.rule, key.finding))

  private def _validate_declarations(declarations: Vector[Declaration]): Consequence[Unit] =
    declarations.foldLeft(Consequence.unit)((prior, declaration) => prior.flatMap(_ => Disposition.validateC(declaration).map(_ => ())))
}
