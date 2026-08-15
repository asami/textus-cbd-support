package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.job.{JobQueryReadModel, JobStatus}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CarReviewProductionJobProjection {
  def discover(
    model: JobQueryReadModel,
    binding: CarReviewProductionJobBinding
  ): Consequence[CarReviewDiscoveredProductionJob] =
    model.status match {
      case JobStatus.Succeeded =>
        _decode_response(model, binding).flatMap { response =>
          _project(model, binding, response).fold(
            code => Consequence.operationInvalid(code),
            discovered => Consequence.success(discovered)
          )
        }
      case _ if model.result.nonEmpty => Consequence.operationInvalid("review-job-result-status-mismatch")
      case _ =>
        _project(model, binding, None).fold(
          code => Consequence.operationInvalid(code),
          discovered => Consequence.success(discovered)
        )
    }

  private[cbdsupport] def decodeResult(
    record: Record,
    binding: CarReviewProductionJobBinding
  ): Consequence[CarReviewCanonicalResponse] = {
    val keys = Set(
      "schemaVersion", "documentType", "status", "diagnosisId", "reviewId",
      "reuseKeyDefinition", "reuseKeyDigest", "reportId", "reportDigest", "reportDocument"
    )
    val result = for {
      _ <- Either.cond(record.keySet == keys, (), "review-job-result-fields-invalid")
      schema <- record.getString("schemaVersion").toRight("review-job-result-fields-invalid")
      documenttype <- record.getString("documentType").toRight("review-job-result-fields-invalid")
      status <- record.getString("status").toRight("review-job-result-fields-invalid")
      diagnosisid <- record.getString("diagnosisId").toRight("review-job-result-fields-invalid")
      reviewid <- record.getString("reviewId").toRight("review-job-result-fields-invalid")
      definition <- record.getString("reuseKeyDefinition").toRight("review-job-result-fields-invalid")
      digest <- record.getString("reuseKeyDigest").toRight("review-job-result-fields-invalid")
      reportid <- record.getString("reportId").toRight("review-job-result-fields-invalid")
      reportdigest <- record.getString("reportDigest").toRight("review-job-result-fields-invalid")
      reportdocument <- record.getString("reportDocument").toRight("review-job-result-fields-invalid")
      _ <- Either.cond(
        schema == "textus.cbd.review-job-result.v1" && documenttype == "review-job-result" && status == "completed" &&
          diagnosisid == binding.diagnosisId && reviewid == binding.reviewId.value &&
          definition == binding.reuseKeyDefinition && digest == binding.reuseKeyDigest.value,
        (),
        "review-job-result-binding-mismatch"
      )
      report <- CarReviewReportCodec.decode(reportdocument).left.map(_.code)
      canonical <- CarReviewReportCodec.encode(report).left.map(_.code)
      _ <- Either.cond(
        canonical == reportdocument && report.reportId.value == reportid && report.reportDigest.value == reportdigest &&
          report.reviewId == binding.reviewId && report.target == binding.target && report.profile == binding.profile &&
          report.createdAt == report.execution.completedAt,
        (),
        "review-job-result-report-mismatch"
      )
      _ <- Either.cond(report.execution.startedAt == binding.startedAt, (), "review-job-result-started-at-mismatch")
      _ <- _not_before(report.execution.completedAt, binding.startedAt, "review-job-result-report-mismatch")
      attestation <- CarReviewAttestationCodec.fromReport(report).left.map(_.code)
    } yield CarReviewCanonicalResponse(report, report.gate, attestation)
    result.fold(Consequence.operationInvalid, Consequence.success)
  }

  private def _decode_response(
    model: JobQueryReadModel,
    binding: CarReviewProductionJobBinding
  ): Consequence[Option[CarReviewCanonicalResponse]] =
    model.result match {
      case Some(OperationResponse.RecordResponse(record)) => decodeResult(record, binding).map(Some.apply)
      case _ => Consequence.operationInvalid("review-job-result-missing")
    }

  private def _project(
    model: JobQueryReadModel,
    binding: CarReviewProductionJobBinding,
    response: Option[CarReviewCanonicalResponse]
  ): Either[String, CarReviewDiscoveredProductionJob] = {
    val jobid = ReviewJobId(model.jobId.value)
    val update = jobUpdate(model)
    for {
      admitted <- CarReviewRunLifecycle.admitted(
        binding.reviewId,
        binding.target,
        binding.profile,
        binding.startedAt
      ).left.map(_.code)
      queued <- _project_job(admitted, ReviewRunJobUpdate(JobStatus.Submitted, binding.startedAt))
      result <- model.status match {
        case JobStatus.Submitted =>
          _project_job(queued, update).map(run => (run, Option.empty[CarReviewProductionTerminalLease]))
        case JobStatus.Running | JobStatus.Suspended =>
          _project_job(queued, update).map(run => (run, Option.empty[CarReviewProductionTerminalLease]))
        case JobStatus.Succeeded => _project_succeeded(jobid, binding, queued, response)
        case JobStatus.Failed => _project_failed(jobid, binding, queued, update, response)
        case JobStatus.Cancelled => _project_cancelled(jobid, binding, queued, update, response)
      }
    } yield CarReviewDiscoveredProductionJob(
      jobid,
      binding,
      update,
      response,
      result._1,
      result._2
    )
  }

  private def _project_succeeded(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    queued: CarReviewRun,
    response: Option[CarReviewCanonicalResponse]
  ): Either[String, (CarReviewRun, Option[CarReviewProductionTerminalLease])] =
    for {
      canonical <- response.toRight("review-job-result-missing")
      report = canonical.report
      _ <- Either.cond(report.createdAt == report.execution.completedAt && report.execution.startedAt == binding.startedAt, (), "review-job-result-report-mismatch")
      _ <- _not_before(report.execution.completedAt, binding.startedAt, "review-job-result-report-mismatch")
      running <- _project_job(queued, ReviewRunJobUpdate(JobStatus.Running, binding.startedAt))
      completed <- _project_job(running, ReviewRunJobUpdate(
        JobStatus.Succeeded,
        report.execution.completedAt,
        completion = Some(ReviewRunCompletion(report.reportId, report.reportDigest)),
        providers = Some(report.execution.providers),
        limitations = report.limitations
      ))
      _ <- Either.cond(
        completed.state == ReviewRunState("completed") &&
          completed.startedAt == binding.startedAt &&
          completed.completedAt.contains(report.execution.completedAt) &&
          completed.reportId.contains(report.reportId) &&
          completed.reportDigest.contains(report.reportDigest) &&
          completed.providers == report.execution.providers &&
          completed.limitations == report.limitations,
        (),
        "review-job-result-run-mismatch"
      )
      lease = CarReviewProductionTerminalLease.completed(jobid, binding, completed, canonical)
    } yield completed -> Some(lease)

  private def _project_failed(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    queued: CarReviewRun,
    update: ReviewRunJobUpdate,
    response: Option[CarReviewCanonicalResponse]
  ): Either[String, (CarReviewRun, Option[CarReviewProductionTerminalLease])] =
    for {
      _ <- Either.cond(response.isEmpty && update.completion.isEmpty, (), "review-job-result-unexpected")
      failure <- update.failureCode.toRight("review-job-failure-code-missing")
      _ <- Either.cond(_failure_code_valid(failure), (), "review-job-failure-code-invalid")
      failed <- _project_job(queued, update)
      _ <- Either.cond(
        failed.state == ReviewRunState("failed") &&
          failed.completedAt.contains(update.updatedAt) &&
          failed.failureCode.contains(failure),
        (),
        "review-job-failed-run-mismatch"
      )
      lease = CarReviewProductionTerminalLease.failed(jobid, binding, failed)
    } yield failed -> Some(lease)

  private def _project_cancelled(
    jobid: ReviewJobId,
    binding: CarReviewProductionJobBinding,
    queued: CarReviewRun,
    update: ReviewRunJobUpdate,
    response: Option[CarReviewCanonicalResponse]
  ): Either[String, (CarReviewRun, Option[CarReviewProductionTerminalLease])] =
    for {
      _ <- Either.cond(response.isEmpty && update.completion.isEmpty && update.failureCode.isEmpty, (), "review-job-result-unexpected")
      cancelled <- _project_job(queued, update)
      _ <- Either.cond(
        cancelled.state == ReviewRunState("cancelled") && cancelled.completedAt.contains(update.updatedAt), (), "review-job-cancelled-run-mismatch")
      lease = CarReviewProductionTerminalLease.cancelled(jobid, binding, cancelled)
    } yield cancelled -> Some(lease)

  private def _project_job(run: CarReviewRun, update: ReviewRunJobUpdate): Either[String, CarReviewRun] =
    CarReviewRunLifecycle.projectJob(run, update).left.map(_.code)

  def jobUpdate(model: JobQueryReadModel): ReviewRunJobUpdate =
    ReviewRunJobUpdate(
      model.status,
      ReviewInstant(model.updatedAt.toString),
      completion = model.result.flatMap(_completion),
      failureCode = if model.status == JobStatus.Failed then _failure_code(model.resultSummary.message) else None
    )

  private def _completion(response: OperationResponse): Option[ReviewRunCompletion] = response match {
    case OperationResponse.RecordResponse(record) =>
      for {
        reportid <- record.getString("reportId").map(ReviewReportId.apply)
        reportdigest <- record.getString("reportDigest").map(ReviewDigest.apply)
      } yield ReviewRunCompletion(reportid, reportdigest)
    case _ => None
  }

  private def _failure_code(message: Option[String]): Option[ReviewFailureCode] =
    message.flatMap { value =>
      val prefix = "textus.cbd.review.failure.v1:"
      Option(value).map(_.trim).filter(_.startsWith(prefix)).flatMap { text =>
        val code = text.drop(prefix.length)
        Option.when(code.matches("[a-z][a-z0-9.-]{0,127}"))(ReviewFailureCode(code))
      }
    }.orElse(Some(ReviewFailureCode("cncf-job-failed")))

  private def _not_before(later: ReviewInstant, earlier: ReviewInstant, code: String): Either[String, Unit] =
    for {
      parsedlater <- _instant(later).toRight(code)
      parsedearlier <- _instant(earlier).toRight(code)
      _ <- Either.cond(!parsedlater.isBefore(parsedearlier), (), code)
    } yield ()

  private def _instant(value: ReviewInstant): Option[java.time.Instant] =
    scala.util.Try(java.time.Instant.parse(value.value)).toOption

  private def _failure_code_valid(value: ReviewFailureCode): Boolean =
    value.value.matches("[a-z][a-z0-9.-]{0,127}")
}
