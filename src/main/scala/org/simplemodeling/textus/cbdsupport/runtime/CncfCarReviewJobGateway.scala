package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ActionEngine}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.{ActionId, JobControlCommand, JobControlPolicy, JobControlRequest, JobEngine, JobId, JobPersistencePolicy, JobQueryPolicy, JobQueryReadModel, JobRunMode, JobSubmitOption}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.cbdsupport.impl.ComponentFactory

/*
 * @since   Jul. 16, 2026
 *  version Aug. 15, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewJobGateway(
  jobengine: JobEngine
) extends CarReviewJobGateway {
  def submit(
    request: ReviewStartRequest
  )(using ctx: ExecutionContext): Consequence[ReviewJobId] = {
    Consequence.operationInvalid("review-execution-plan-required")
  }

  def stageExecution(
    core: ActionCall.Core,
    work: CarReviewJobExecutionWork,
    providerApplication: CarReviewProviderExecutionApplication,
    factory: ComponentFactory,
    actionEngine: ActionEngine
  ): Consequence[ReviewJobId] =
    if core == null || core.executionContext == null || core.action == null ||
        core.component == null || core.component.isEmpty || core.component.contains(null) ||
        work == null || providerApplication == null || factory == null || actionEngine == null || jobengine == null then
      Consequence.operationInvalid("review-job-runtime-incomplete")
    else for {
      _ <- work.validate
      _ <- CarReviewAuthorization.authorize("review.start", CarReviewAuthorization.roles(core.executionContext))
      jobid <- {
        val ctx = core.executionContext
        val actionid = ActionId.create("cbd.review", ctx.clock.instant(), ctx.idGeneration)
        val task = new CarReviewExecutionJobTask(actionid, work, core, providerApplication, factory, actionEngine)
        val option = JobSubmitOption(
          persistence = JobPersistencePolicy.Persistent,
          runMode = JobRunMode.Async,
          requestSummary = Some(s"CBD CAR Review ${work.plan.request.reviewId.value}"),
          parameters = Map(
            "reviewId" -> work.plan.request.reviewId.value,
            "targetDigest" -> work.plan.request.target.digest.value,
            "profile" -> work.plan.request.profile.value,
            "diagnosisId" -> work.owner.diagnosisId,
            "planRevision" -> work.plan.reference.revision.toString
          )
        )
        jobengine.stageSubmit(List(task), ctx.unitOfWork, option)
      }
    } yield ReviewJobId(jobid.value)

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
      jobengine.queryVisible(parsed, ReviewRunQueryPolicy).flatMap {
        case None => Consequence.operationNotFound("review-job-not-found")
        case Some(_) => jobengine.control(
        parsed,
        JobControlRequest(JobControlCommand.Cancel),
        ReviewRunControlPolicy
      ).map(_ => ())
      }
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
    CarReviewAuthorization.authorize("review.read-run", CarReviewAuthorization.roles(ctx)).flatMap { _ =>
      if (JobQueryPolicy.isOwner(model.submitter, ctx)) Consequence.unit
      else Consequence.operationIllegal("review.read-run", "review-job-not-owned")
    }
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
