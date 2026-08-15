package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.{ActionId, JobControlCommand, JobControlPolicy, JobControlRequest, JobEngine, JobId, JobPersistencePolicy, JobQueryPolicy, JobQueryReadModel, JobRunMode, JobSubmitOption, JobTask, TaskOutcome, TaskSucceeded}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record

/*
 * @since   Jul. 16, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewJobGateway(
  jobengine: JobEngine
) extends CarReviewJobGateway {
  def submit(
    request: ReviewStartRequest
  )(using ctx: ExecutionContext): Consequence[ReviewJobId] = {
    val actionid = ActionId.create("cbd.review", ctx.clock.instant(), ctx.idGeneration)
    val task = ReviewPipelineTask(actionid, request)
    val option = JobSubmitOption(
      persistence = JobPersistencePolicy.Persistent,
      runMode = JobRunMode.Async,
      requestSummary = Some(s"CBD CAR Review ${request.reviewId.value}"),
      parameters = Map(
        "reviewId" -> request.reviewId.value,
        "targetDigest" -> request.target.digest.value,
        "profile" -> request.profile.value
      )
    )
    jobengine.submit(List(task), ctx, option).map(jobid => ReviewJobId(jobid.value))
  }

  def read(
    jobid: ReviewJobId
  )(using ctx: ExecutionContext): Consequence[Option[ReviewRunJobUpdate]] =
    JobId.parse(jobid.value).flatMap { parsed =>
      jobengine.queryVisible(parsed, ReviewRunQueryPolicy).map(_.map(_update))
    }

  def cancel(
    jobid: ReviewJobId
  )(using ctx: ExecutionContext): Consequence[Unit] =
    JobId.parse(jobid.value).flatMap { parsed =>
      jobengine.control(
        parsed,
        JobControlRequest(JobControlCommand.Cancel),
        ReviewRunControlPolicy
      ).map(_ => ())
    }

  private def _update(model: JobQueryReadModel): ReviewRunJobUpdate =
    ReviewRunJobUpdate(
      model.status,
      ReviewInstant(model.updatedAt.toString),
      completion = model.result.flatMap(_completion)
    )

  private def _completion(response: OperationResponse): Option[ReviewRunCompletion] = response match {
    case OperationResponse.RecordResponse(record) =>
      for {
        reportid <- record.getString("reportId").map(ReviewReportId.apply)
        reportdigest <- record.getString("reportDigest").map(ReviewDigest.apply)
      } yield ReviewRunCompletion(reportid, reportdigest)
    case _ => None
  }
}

private object ReviewRunQueryPolicy extends JobQueryPolicy {
  def authorizeRead(
    model: JobQueryReadModel
  )(using ctx: ExecutionContext): Consequence[Unit] = {
    val _ = model
    CarReviewAuthorization.authorize("review.read-run", CarReviewAuthorization.roles(ctx))
  }
}

private object ReviewRunControlPolicy extends JobControlPolicy {
  def authorize(
    jobid: JobId,
    request: JobControlRequest
  )(using ctx: ExecutionContext): Consequence[Unit] = {
    val _ = jobid
    val _ = request
    CarReviewAuthorization.authorize("review.cancel", CarReviewAuthorization.roles(ctx))
  }
}

private final case class ReviewPipelineTask(
  actionId: ActionId,
  request: ReviewStartRequest
) extends JobTask {
  override def componentName: Option[String] = Some("CbdSupport")
  override def serviceName: Option[String] = Some("CbdReviewAdmin")
  override def operationName: Option[String] = Some("executeReview")

  def run(ctx: ExecutionContext): TaskOutcome = {
    val _ = ctx
    TaskSucceeded(OperationResponse(Record.dataAuto(
      "reviewId" -> request.reviewId.value,
      "status" -> "provider-pipeline-pending"
    )))
  }
}
