package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.{Conclusion, Consequence}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.{ActionId, JobControlCommand, JobControlPolicy, JobControlRequest, JobEngine, JobId, JobPersistencePolicy, JobQueryPolicy, JobQueryReadModel, JobRunMode, JobSubmitOption, JobTask, TaskFailed, TaskOutcome, TaskSucceeded}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record

/*
 * @since   Jul. 16, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * Bridges the current CNCF JobEngine to a server-owned Review execution.
 * Lookup is durable only within the configured current JobEngine state; it
 * does not claim process-restart task or result rehydration because the CNCF
 * API exposes only a bounded, non-paginated listJobs operation.
 */
final class CncfCarReviewJobGateway(
  jobengine: JobEngine,
  persistentSearchLimit: Int = 10000
) extends CarReviewJobGateway with CarReviewProductionJobPort {
  require(persistentSearchLimit > 0, "persistentSearchLimit must be positive")

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

  def submit(
    binding: CarReviewProductionJobBinding,
    execution: CarReviewProductionExecution
  )(using ctx: ExecutionContext): Consequence[ReviewJobId] =
    binding.validate(execution) match {
      case Left(code) => Consequence.operationInvalid(code)
      case Right(_) =>
        val actionid = ActionId.create("cbd.review", ctx.clock.instant(), ctx.idGeneration)
        val option = JobSubmitOption(
          persistence = JobPersistencePolicy.Persistent,
          runMode = JobRunMode.Async,
          requestSummary = Some(s"CBD CAR Review ${binding.reviewId.value}"),
          parameters = CarReviewProductionJobBinding.parameters(binding)
        )
        jobengine.submit(List(ProductionReviewPipelineTask(actionid, binding, execution)), ctx, option)
          .map(jobid => ReviewJobId(jobid.value))
    }

  def read(
    jobid: ReviewJobId
  )(using ctx: ExecutionContext): Consequence[Option[ReviewRunJobUpdate]] =
    JobId.parse(jobid.value).flatMap { parsed =>
      jobengine.queryVisible(parsed, ReviewRunQueryPolicy).map(_.map(CarReviewProductionJobProjection.jobUpdate))
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

  def findByReviewId(
    reviewId: ReviewId
  )(using ctx: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] =
    _find(reviewId, None)

  def findByReviewReuse(
    reviewId: ReviewId,
    reuseKeyDefinition: String,
    reuseKeyDigest: ReviewDigest
  )(using ctx: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] =
    _find(reviewId, Some(reuseKeyDefinition -> reuseKeyDigest))

  private def _find(
    reviewid: ReviewId,
    reuse: Option[(String, ReviewDigest)]
  )(using ctx: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] =
    for {
      _ <- CarReviewAuthorization.authorize("review.read-run", CarReviewAuthorization.roles(ctx))
      discovered <- _discover(reviewid, reuse)
    } yield discovered

  private def _discover(
    reviewid: ReviewId,
    reuse: Option[(String, ReviewDigest)]
  )(using ctx: ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]] = {
    val models = jobengine.listJobs(persistentSearchLimit, persistentOnly = true)
    if models.size >= persistentSearchLimit then
      Consequence.operationInvalid("review-job-search-bound-exceeded")
    else {
      val candidates = models.filter(_.debug.parameters.get("reviewId").contains(reviewid.value))
      val parsed = candidates.foldLeft[Either[String, Vector[(JobQueryReadModel, CarReviewProductionJobBinding)]]](Right(Vector.empty)) { (result, model) =>
        for {
          values <- result
          binding <- CarReviewProductionJobBinding.parse(model.debug.parameters)
        } yield values :+ (model -> binding)
      }.map(_.filter { case (_, binding) =>
        reuse.forall { case (definition, digest) =>
          binding.reuseKeyDefinition == definition && binding.reuseKeyDigest == digest
        }
      })
      parsed match {
        case Left(code) => Consequence.operationInvalid(code)
        case Right(Vector()) => Consequence.success(None)
        case Right(Vector((model, binding))) =>
          jobengine.queryVisible(model.jobId, ReviewRunQueryPolicy).flatMap {
            case None => Consequence.operationNotFound(s"review job: ${model.jobId.value}")
            case Some(visible) =>
              CarReviewProductionJobBinding.parse(visible.debug.parameters) match {
                case Left(code) => Consequence.operationInvalid(code)
                case Right(current) if current != binding => Consequence.operationInvalid("review-job-binding-query-mismatch")
                case Right(_) => _discovered(visible, binding).map(Some.apply)
              }
          }
        case Right(_) => Consequence.operationInvalid("review-job-binding-ambiguous")
      }
    }
  }

  private def _discovered(
    model: JobQueryReadModel,
    binding: CarReviewProductionJobBinding
  ): Consequence[CarReviewDiscoveredProductionJob] =
    CarReviewProductionJobProjection.discover(model, binding)

  private[cbdsupport] def _decode_result(
    record: Record,
    binding: CarReviewProductionJobBinding
  ): Consequence[CarReviewCanonicalResponse] =
    CarReviewProductionJobProjection.decodeResult(record, binding)
}

/**
 * The production action boundary's JobEngine capability.  It is a real
 * package-scoped port rather than a test hook: production owns submission,
 * exact reuse discovery, and cancellation through this one authority.
 */
private[cbdsupport] trait CarReviewProductionJobPort {
  def submit(
    binding: CarReviewProductionJobBinding,
    execution: CarReviewProductionExecution
  )(using ExecutionContext): Consequence[ReviewJobId]

  def findByReviewId(
    reviewId: ReviewId
  )(using ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]]

  def findByReviewReuse(
    reviewId: ReviewId,
    reuseKeyDefinition: String,
    reuseKeyDigest: ReviewDigest
  )(using ExecutionContext): Consequence[Option[CarReviewDiscoveredProductionJob]]

  def cancel(
    jobId: ReviewJobId
  )(using ExecutionContext): Consequence[Unit]
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

private[cbdsupport] final case class ProductionReviewPipelineTask(
  actionId: ActionId,
  binding: CarReviewProductionJobBinding,
  execution: CarReviewProductionExecution
) extends JobTask {
  override def componentName: Option[String] = Some("CbdSupport")
  override def serviceName: Option[String] = Some("CbdReviewAdmin")
  override def operationName: Option[String] = Some("executeProductionReview")

  def run(ctx: ExecutionContext): TaskOutcome =
    execution.execute(
      CarReviewAuthorization.roles(ctx),
      ReviewInstant(ctx.clock.instant().toString)
    ) match {
      case Consequence.Success(response) =>
        _response(response) match {
          case Right(value) => TaskSucceeded(value)
          case Left(code) => TaskFailed(_failure_code(code))
        }
      case Consequence.Failure(conclusion) =>
        TaskFailed(_failure_conclusion(conclusion, "review-production-execution-failed"))
    }

  private def _response(response: CarReviewCanonicalResponse): Either[String, OperationResponse] = {
    val report = response.report
    CarReviewReportCodec.encode(report).left.map(error => _codec_code(error.code)).flatMap { document =>
      CarReviewReportCodec.decode(document).left.map(error => _codec_code(error.code)).flatMap { decoded =>
        CarReviewReportCodec.encode(decoded).left.map(error => _codec_code(error.code)).flatMap { canonical =>
          Either.cond(
            canonical == document &&
              decoded.reportId == report.reportId && decoded.reportDigest == report.reportDigest &&
              decoded.reviewId == binding.reviewId && decoded.target == binding.target && decoded.profile == binding.profile,
            OperationResponse(Record.dataAuto(
              "schemaVersion" -> "textus.cbd.review-job-result.v1",
              "documentType" -> "review-job-result",
              "status" -> "completed",
              "diagnosisId" -> binding.diagnosisId,
              "reviewId" -> binding.reviewId.value,
              "reuseKeyDefinition" -> binding.reuseKeyDefinition,
              "reuseKeyDigest" -> binding.reuseKeyDigest.value,
              "reportId" -> decoded.reportId.value,
              "reportDigest" -> decoded.reportDigest.value,
              "reportDocument" -> document
            )),
            "review-job-result-report-mismatch"
          )
        }
      }
    }
  }

  private def _codec_code(code: String): String =
    _validated_code(code).getOrElse("review-job-result-codec-failed")

  private def _failure_code(code: String): Conclusion =
    Conclusion.simple(s"textus.cbd.review.failure.v1:${_validated_code(code).getOrElse("cncf-job-failed")}")

  private def _failure_conclusion(
    conclusion: Conclusion,
    fallback: String
  ): Conclusion = {
    val representations = Vector(
      conclusion.observation.getEffectiveMessage,
      Option(conclusion.toString).filter(_.length <= 256)
    ).flatten
    val code = representations.iterator.flatMap(_failure_code_from_representation).toSeq.headOption.getOrElse(fallback)
    _failure_code(code)
  }

  private def _failure_code_from_representation(value: String): Option[String] =
    FailureSentinel.unapply(value).flatMap(_validated_code)

  private def _validated_code(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.matches("[a-z][a-z0-9.-]{0,127}"))

  private object FailureSentinel {
    private val _pattern = "^textus\\.cbd\\.review\\.failure\\.v1:([a-z][a-z0-9.-]{0,127})$".r

    def unapply(value: String): Option[String] =
      Option(value).flatMap(_pattern.unapplySeq).flatMap(_.headOption)
  }
}
