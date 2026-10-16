package org.simplemodeling.textus.cbdsupport.runtime

import scala.util.control.NonFatal
import cats.syntax.all.*
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.cncf.Program
import org.goldenport.cncf.action.{ActionCall, ActionEngine, FunctionalActionCall}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.{ActionId, JobTask, TaskFailed, TaskOutcome, TaskSucceeded}
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWorkOp, UnitOfWorkTermination}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.impl.ComponentFactory

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[cbdsupport] final class CarReviewExecutionJobTask(
  val actionId: ActionId,
  work: CarReviewJobExecutionWork,
  submittingcore: ActionCall.Core,
  application: CarReviewProviderExecutionApplication,
  factory: ComponentFactory,
  actionengine: ActionEngine
) extends JobTask {
  override def taskKind: String = "operation"
  override def targetKind: Option[String] = Some("operation")
  override def relation: Option[String] = Some("root")
  override def transactionRole: Option[String] = Some("own")
  override def transactionScope: Option[String] = Some("per-task")
  override def componentName: Option[String] = Some("CbdSupport")
  override def serviceName: Option[String] = Some("CbdReviewAdmin")
  override def operationName: Option[String] = Some("executeReview")

  def run(ctx: ExecutionContext): TaskOutcome = {
    val result: Consequence[OperationResponse] = try {
      if ctx == null || ctx.jobContext.jobId.isEmpty || ctx.jobContext.taskId.isEmpty ||
          ctx.jobContext.cancellationScope.isEmpty then
        Consequence.operationInvalid("review-job-context-incomplete")
      else ExecutionContext.withFreshUnitOfWorkContext(ctx).flatMap { rebound =>
        val unitofwork = rebound.unitOfWork
        if (rebound eq ctx) || (unitofwork eq ctx.unitOfWork) ||
            !(unitofwork.executionContext eq rebound) then
          Consequence.operationInvalid("review-job-context-not-rebound")
        else {
          val core = ActionCall.Core(submittingcore.action, rebound, submittingcore.component, submittingcore.correlationId)
          actionengine.execute(new ExecutionCall(core)).flatMap { response =>
            unitofwork.lastCommitTermination match {
              case Some(UnitOfWorkTermination.Committed) =>
                unitofwork.lastCommitResult match {
                  case Some(Consequence.Success(_)) => Consequence.success(response)
                  case _ => Consequence.operationInvalid("review-job-commit-failed")
                }
              case _ => Consequence.operationInvalid("review-job-commit-required")
            }
          }
        }
      }
    } catch {
      case NonFatal(_) => Consequence.operationInvalid("review-job-execution-failed")
    }
    result match {
      case Consequence.Success(response) => TaskSucceeded(response)
      case Consequence.Failure(conclusion) => TaskFailed(conclusion)
    }
  }

  private final class ExecutionCall(val core: ActionCall.Core) extends FunctionalActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- _from(work.validate)
        _ <- _from(CarReviewAuthorization.authorize("review.start", CarReviewAuthorization.roles(executionContext)))
        _ <- _from(CarReviewAuthorization.authorize("review.submit-bundle", CarReviewAuthorization.roles(executionContext)))
        _ <- _from(_not_cancelled)
        inputs <- _from(_execute_providers)
        _ <- _from(_not_cancelled)
        canonical <- _from {
          val completedat = ReviewInstant(executionContext.clock.instant().toString)
          val template = work.template.copy(
            createdAt = completedat,
            execution = work.template.execution.copy(startedAt = work.plan.request.startedAt, completedAt = completedat)
          )
          new CarReviewCanonicalResponseApplication().build(template, inputs, CarReviewAuthorization.roles(executionContext))
        }
        _ <- _from(_not_cancelled)
        completed <- factory._complete_review_execution(core, work.owner, work.plan, canonical)
        response <- _from {
          completed match {
            case CarReviewDiagnosisAdmission.Reused(diagnosisid, reviewid, reportid, digest)
                if diagnosisid == work.owner.diagnosisId && reviewid == work.owner.reviewId &&
                  reportid == canonical.report.reportId && digest == canonical.report.reportDigest =>
              Consequence.success(OperationResponse.RecordResponse(Record.dataAuto(
                "reviewId" -> reviewid.value,
                "reportId" -> reportid.value,
                "reportDigest" -> digest.value
              )))
            case _ => Consequence.operationInvalid("review-job-completion-binding-invalid")
          }
        }
      } yield response

    private def _from[A](result: Consequence[A]): ExecUowM[A] =
      ConsequenceT.fromConsequence[[X] =>> Program[UnitOfWorkOp, X], A](result)

    private def _not_cancelled: Consequence[Unit] =
      if executionContext.jobContext.cancellationScope.exists(_.isCancelled) then
        Consequence.operationInvalid("review-job-cancelled")
      else Consequence.unit

    private def _execute_providers: Consequence[Vector[AdmittedProviderBundleInput]] =
      work.providers.sortBy(call => (call.selection.provider.id.value, call.selection.provider.version.value))
        .foldLeft(Consequence.success(Vector.empty[AdmittedProviderBundleInput])) { (result, call) =>
          result.flatMap { inputs =>
            _not_cancelled.flatMap { _ =>
              val scope = executionContext.jobContext.cancellationScope.get
              val request = call.request.copy(
                startedAtMillis = executionContext.clock.instant().toEpochMilli,
                cancellationRequested = scope.isCancelled
              )
              val registration = scope.register(application.cancelRegistered(core, request))
              try {
                _not_cancelled.flatMap { _ =>
                  val outcome = application.executeForCanonicalReport(core, request)
                  _not_cancelled.flatMap { _ =>
                    outcome match {
                      case CanonicalProviderBundleExecutionOutcome.Admitted(input, _) => Consequence.success(inputs :+ input)
                      case CanonicalProviderBundleExecutionOutcome.Refused(value) =>
                        Consequence.operationInvalid("review.execute-provider", value.limitation.code)
                    }
                  }
                }
              } finally registration.close()
            }
          }
        }
  }
}
