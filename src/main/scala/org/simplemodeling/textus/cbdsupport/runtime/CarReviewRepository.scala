package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewRepositoryFailure(code: String, message: String)

final case class CarReviewRetentionPolicy(
  maxAgeDays: Int,
  maxReportsPerTarget: Int,
  maxRunsPerTarget: Int
) {
  def validate: Either[CarReviewRepositoryFailure, Unit] =
    if maxAgeDays <= 0 then Left(CarReviewRepositoryFailure("invalid-retention-policy", "Report retention age must be positive."))
    else if maxReportsPerTarget <= 0 then Left(CarReviewRepositoryFailure("invalid-retention-policy", "Per-target report retention count must be positive."))
    else if maxRunsPerTarget <= 0 then Left(CarReviewRepositoryFailure("invalid-retention-policy", "Per-target Review Run retention count must be positive."))
    else Right(())
}

final case class CarReviewRunReportBinding(
  reviewId: ReviewId,
  target: ReviewTarget,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  gateResult: ReviewGateResult
)

final case class CarReviewGateEvidence(
  reviewId: ReviewId,
  target: ReviewTarget,
  reportId: ReviewReportId,
  reportDigest: ReviewDigest,
  gateResult: ReviewGateResult
)

final case class CarReviewRetentionAudit(
  action: String,
  recordType: String,
  reviewId: ReviewId,
  reportId: Option[ReviewReportId],
  reportDigest: Option[ReviewDigest],
  targetDigest: ReviewDigest,
  effectiveAt: ReviewInstant
)

object CarReviewRetentionAudit {
  val EXPIRED = "expired"
  val DELETED = "deleted"
  val REPORT = "report"
  val RUN = "run"
}

final class CarReviewRepository(
  policy: CarReviewRetentionPolicy = CarReviewRetentionPolicy(30, 20, 20)
) {
  private var _reports = Map.empty[ReviewReportId, CarReviewReport]
  private var _runs = Map.empty[ReviewId, CarReviewRun]
  private var _bindings = Map.empty[ReviewId, CarReviewRunReportBinding]
  private var _audit = Vector.empty[CarReviewRetentionAudit]

  def retain(report: CarReviewReport): Either[CarReviewRepositoryFailure, CarReviewReport] = synchronized {
    for {
      _ <- policy.validate
      _ <- _validate_report(report)
      _ <- _check_report_identity(report)
      _ <- _check_report_capacity(report)
    } yield {
      _reports = _reports.updated(report.reportId, report)
      report
    }
  }

  def retain(run: CarReviewRun): Either[CarReviewRepositoryFailure, CarReviewRun] = synchronized {
    for {
      _ <- policy.validate
      _ <- _validate_terminal_run(run)
      _ <- _check_run_identity(run)
      _ <- _check_run_capacity(run)
    } yield {
      _runs = _runs.updated(run.reviewId, run)
      run
    }
  }

  def retain(
    run: CarReviewRun,
    report: CarReviewReport
  ): Either[CarReviewRepositoryFailure, CarReviewRunReportBinding] = synchronized {
    for {
      _ <- policy.validate
      _ <- _validate_report(report)
      _ <- _validate_completed_run(run)
      binding <- _binding_for(run, report)
      _ <- _check_report_identity(report)
      _ <- _check_run_identity(run)
      _ <- _check_binding_identity(binding)
      _ <- _check_report_capacity(report)
      _ <- _check_run_capacity(run)
    } yield {
      _reports = _reports.updated(report.reportId, report)
      _runs = _runs.updated(run.reviewId, run)
      _bindings = _bindings.updated(run.reviewId, binding)
      binding
    }
  }

  def binding(reviewid: ReviewId): Option[CarReviewRunReportBinding] = synchronized {
    _bindings.get(reviewid)
  }

  def report(reportid: ReviewReportId): Option[CarReviewReport] = synchronized {
    _reports.get(reportid)
  }

  def validateGateEvidence(evidence: CarReviewGateEvidence): Either[CarReviewRepositoryFailure, CarReviewRunReportBinding] = synchronized {
    _bindings.get(evidence.reviewId) match {
      case Some(binding) if _reports.contains(binding.reportId) && _matches(binding, evidence) => Right(binding)
      case _ => Left(CarReviewRepositoryFailure("stale-gate-evidence", "Gate evidence does not exactly match a retained Review Run/report binding."))
    }
  }

  def compare(current: CarReviewReport, baselineid: ReviewReportId): Either[CarReviewRepositoryFailure, ReviewBaseline] = synchronized {
    _reports.get(baselineid).toRight(CarReviewRepositoryFailure("baseline-not-found", "Baseline report is not retained.")).flatMap { baseline =>
      if baseline.target != current.target then Left(CarReviewRepositoryFailure("baseline-target-mismatch", "Baseline target differs from current report target."))
      else if _reports.get(current.reportId).forall(_ != current) then Left(CarReviewRepositoryFailure("current-report-not-retained", "Current report is not retained with its immutable digest."))
      else {
        val previous = baseline.observations.map(_.id).toSet
        val present = current.observations.map(_.id).toSet
        Right(ReviewBaseline(baseline.reportId, baseline.reportDigest, (present -- previous).toVector.sortBy(_.value), (previous -- present).toVector.sortBy(_.value), (present intersect previous).toVector.sortBy(_.value)))
      }
    }
  }

  def delete(reportid: ReviewReportId, effectiveat: ReviewInstant): Either[CarReviewRepositoryFailure, CarReviewRetentionAudit] = synchronized {
    _parse_instant(effectiveat).flatMap { _ =>
      _reports.get(reportid).toRight(CarReviewRepositoryFailure("report-not-found", "Retained report does not exist.")).map { report =>
        val audit = CarReviewRetentionAudit(CarReviewRetentionAudit.DELETED, CarReviewRetentionAudit.REPORT, report.reviewId, Some(report.reportId), Some(report.reportDigest), report.target.digest, effectiveat)
        _reports = _reports.removed(reportid)
        _audit = _audit :+ audit
        audit
      }
    }
  }

  def purgeExpired(effectiveat: ReviewInstant): Either[CarReviewRepositoryFailure, Vector[CarReviewRetentionAudit]] = synchronized {
    for {
      _ <- policy.validate
      instant <- _parse_instant(effectiveat)
    } yield {
      val threshold = instant.minusSeconds(policy.maxAgeDays.toLong * 24L * 60L * 60L)
      val expiredreports = _reports.values.toVector.filter(report => _created_at(report).isBefore(threshold)).sortBy(_.reportId.value)
      val expiredruns = _runs.values.toVector.filter(run => _completed_at(run).isBefore(threshold)).sortBy(_.reviewId.value)
      val reportaudits = expiredreports.map { report =>
        CarReviewRetentionAudit(CarReviewRetentionAudit.EXPIRED, CarReviewRetentionAudit.REPORT, report.reviewId, Some(report.reportId), Some(report.reportDigest), report.target.digest, effectiveat)
      }
      val runaudits = expiredruns.map { run =>
        CarReviewRetentionAudit(CarReviewRetentionAudit.EXPIRED, CarReviewRetentionAudit.RUN, run.reviewId, run.reportId, run.reportDigest, run.target.digest, effectiveat)
      }
      val audits = reportaudits ++ runaudits
      _reports = _reports -- expiredreports.map(_.reportId)
      _runs = _runs -- expiredruns.map(_.reviewId)
      _bindings = _bindings -- expiredruns.map(_.reviewId)
      _audit = _audit ++ audits
      audits
    }
  }

  def retentionAudit: Vector[CarReviewRetentionAudit] = synchronized {
    _audit
  }

  private def _validate_report(report: CarReviewReport): Either[CarReviewRepositoryFailure, Unit] =
    CarReviewReportCodec.encode(report).left.map(error => CarReviewRepositoryFailure(error.code, error.message)).map(_ => ())

  private def _validate_terminal_run(run: CarReviewRun): Either[CarReviewRepositoryFailure, Unit] =
    CarReviewRunCodec.validate(run).left.map(error => CarReviewRepositoryFailure(error.code, error.message)).flatMap { _ =>
      if CarReviewRunVocabulary.TERMINAL_STATES.contains(run.state.value) then Right(())
      else Left(CarReviewRepositoryFailure("nonterminal-run", "Only terminal Review Runs are immutable retention records."))
    }

  private def _validate_completed_run(run: CarReviewRun): Either[CarReviewRepositoryFailure, Unit] =
    _validate_terminal_run(run).flatMap { _ =>
      if run.state.value == "completed" then Right(())
      else Left(CarReviewRepositoryFailure("incomplete-run", "A Review Report can bind only to a completed Review Run."))
    }

  private def _binding_for(run: CarReviewRun, report: CarReviewReport): Either[CarReviewRepositoryFailure, CarReviewRunReportBinding] =
    (run.reportId, run.reportDigest) match {
      case (Some(reportid), Some(reportdigest)) if reportid == report.reportId && reportdigest == report.reportDigest && run.reviewId == report.reviewId && run.target == report.target =>
        Right(CarReviewRunReportBinding(run.reviewId, run.target, report.reportId, report.reportDigest, report.gate.result))
      case _ => Left(CarReviewRepositoryFailure("run-report-attribution-mismatch", "Completed Review Run and Report must share exact review, target, report ID, and report digest attribution."))
    }

  private def _check_report_identity(report: CarReviewReport): Either[CarReviewRepositoryFailure, Unit] =
    _reports.get(report.reportId) match {
      case Some(current) if current != report => Left(CarReviewRepositoryFailure("immutable-report-conflict", "Report ID is already bound to different immutable content."))
      case _ => Right(())
    }

  private def _check_run_identity(run: CarReviewRun): Either[CarReviewRepositoryFailure, Unit] =
    _runs.get(run.reviewId) match {
      case Some(current) if current != run => Left(CarReviewRepositoryFailure("immutable-run-conflict", "Review ID is already bound to different immutable terminal content."))
      case _ => Right(())
    }

  private def _check_binding_identity(binding: CarReviewRunReportBinding): Either[CarReviewRepositoryFailure, Unit] =
    _bindings.get(binding.reviewId) match {
      case Some(current) if current != binding => Left(CarReviewRepositoryFailure("immutable-run-binding-conflict", "Review ID is already bound to a different immutable report."))
      case _ => Right(())
    }

  private def _check_report_capacity(report: CarReviewReport): Either[CarReviewRepositoryFailure, Unit] = {
    val retained = _reports.values.count(_.target == report.target)
    if _reports.contains(report.reportId) || retained < policy.maxReportsPerTarget then Right(())
    else Left(CarReviewRepositoryFailure("retention-capacity-exceeded", "Retained report count for the target reached the configured limit."))
  }

  private def _check_run_capacity(run: CarReviewRun): Either[CarReviewRepositoryFailure, Unit] = {
    val retained = _runs.values.count(_.target == run.target)
    if _runs.contains(run.reviewId) || retained < policy.maxRunsPerTarget then Right(())
    else Left(CarReviewRepositoryFailure("run-retention-capacity-exceeded", "Retained terminal Run count for the target reached the configured limit."))
  }

  private def _matches(binding: CarReviewRunReportBinding, evidence: CarReviewGateEvidence): Boolean =
    binding.reviewId == evidence.reviewId &&
      binding.target == evidence.target &&
      binding.reportId == evidence.reportId &&
      binding.reportDigest == evidence.reportDigest &&
      binding.gateResult == evidence.gateResult

  private def _parse_instant(value: ReviewInstant): Either[CarReviewRepositoryFailure, Instant] =
    scala.util.Try(Instant.parse(value.value)).toEither.left.map(_ => CarReviewRepositoryFailure("invalid-retention-time", "Retention operation time must be an ISO-8601 instant."))

  private def _created_at(report: CarReviewReport): Instant =
    Instant.parse(report.createdAt.value)

  private def _completed_at(run: CarReviewRun): Instant =
    Instant.parse(run.completedAt.fold(throw new IllegalArgumentException("Terminal Review Run is missing completion time."))(_.value))
}
