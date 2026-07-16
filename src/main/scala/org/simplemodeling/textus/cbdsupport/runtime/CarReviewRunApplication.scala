package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ReviewStartRequest(
  reviewId: ReviewId,
  target: ReviewTarget,
  profile: ReviewProfile,
  startedAt: ReviewInstant
)

final case class ReviewRunAdmission(
  run: CarReviewRun,
  binding: ReviewRunJobBinding
)

trait CarReviewJobGateway {
  def submit(
    request: ReviewStartRequest
  )(using ExecutionContext): Consequence[ReviewJobId]

  def read(
    jobid: ReviewJobId
  )(using ExecutionContext): Consequence[Option[ReviewRunJobUpdate]]

  def cancel(
    jobid: ReviewJobId
  )(using ExecutionContext): Consequence[Unit]
}

object CarReviewAuthorization {
  val startRoles: Set[String] = Set("reviewer", "operator", "admin")
  val readRoles: Set[String] = Set("viewer", "reviewer", "operator", "admin")
  val cancelRoles: Set[String] = Set("operator", "admin")

  def roles(ctx: ExecutionContext): Set[String] = {
    val attributes = ctx.security.principal.attributes.toVector.collect {
      case (key, value) if Set("role", "roles", "privilege").contains(key.trim.toLowerCase) => value
    }
    val capabilities = ctx.security.capabilities.toVector.map(_.name)
    (attributes ++ capabilities :+ ctx.security.level.value)
      .flatMap(_.split("[,\\s]+"))
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .toSet
  }

  def authorize(
    action: String,
    actorroles: Set[String]
  ): Consequence[Unit] = {
    val allowed = action match {
      case "review.start" => startRoles
      case "review.read-run" => readRoles
      case "review.cancel" => cancelRoles
      case _ => Set.empty[String]
    }
    if (actorroles.map(_.trim.toLowerCase).exists(allowed.contains))
      Consequence.unit
    else
      Consequence.operationIllegal(action, s"required role: ${allowed.toVector.sorted.mkString("|")}")
  }
}

final class CarReviewRunApplication {
  private var _entries = Map.empty[ReviewId, ReviewRunAdmission]

  def start(
    request: ReviewStartRequest,
    actorroles: Set[String],
    gateway: CarReviewJobGateway
  )(using ExecutionContext): Consequence[ReviewRunAdmission] = synchronized {
    for {
      _ <- CarReviewAuthorization.authorize("review.start", actorroles)
      _ <- if (_entries.contains(request.reviewId))
        Consequence.operationInvalid(s"review already exists: ${request.reviewId.value}")
      else
        Consequence.unit
      admitted <- _admitted(request)
      jobid <- gateway.submit(request)
      queued <- _project(admitted, ReviewRunJobUpdate(
        org.goldenport.cncf.job.JobStatus.Submitted,
        request.startedAt
      ))
      admission = ReviewRunAdmission(
        queued,
        ReviewRunJobBinding(request.reviewId, jobid)
      )
      _ = _entries = _entries.updated(request.reviewId, admission)
    } yield admission
  }

  def get(
    reviewid: ReviewId,
    actorroles: Set[String],
    gateway: CarReviewJobGateway
  )(using ExecutionContext): Consequence[ReviewRunAdmission] = synchronized {
    for {
      _ <- CarReviewAuthorization.authorize("review.read-run", actorroles)
      current <- _entry(reviewid)
      update <- gateway.read(current.binding.jobId)
      refreshed <- update match {
        case Some(value) => _project(current.run, value).map(run => current.copy(run = run))
        case None => Consequence.operationNotFound(s"review job: ${current.binding.jobId.value}")
      }
      _ = _entries = _entries.updated(reviewid, refreshed)
    } yield refreshed
  }

  def cancel(
    reviewid: ReviewId,
    actorroles: Set[String],
    updatedat: ReviewInstant,
    gateway: CarReviewJobGateway
  )(using ExecutionContext): Consequence[ReviewRunAdmission] = synchronized {
    for {
      _ <- CarReviewAuthorization.authorize("review.cancel", actorroles)
      current <- _entry(reviewid)
      cancelling <- _cancelling(current.run, updatedat)
      _ <- gateway.cancel(current.binding.jobId)
      changed = current.copy(run = cancelling)
      _ = _entries = _entries.updated(reviewid, changed)
    } yield changed
  }

  private def _entry(reviewid: ReviewId): Consequence[ReviewRunAdmission] =
    _entries.get(reviewid)
      .map(Consequence.success)
      .getOrElse(Consequence.operationNotFound(s"review: ${reviewid.value}"))

  private def _admitted(request: ReviewStartRequest): Consequence[CarReviewRun] =
    CarReviewRunLifecycle.admitted(
      request.reviewId,
      request.target,
      request.profile,
      request.startedAt
    ) match {
      case Right(run) => Consequence.success(run)
      case Left(error) => Consequence.operationInvalid(s"${error.code}: ${error.path}: ${error.message}")
    }

  private def _project(
    run: CarReviewRun,
    update: ReviewRunJobUpdate
  ): Consequence[CarReviewRun] =
    CarReviewRunLifecycle.projectJob(run, update) match {
      case Right(changed) => Consequence.success(changed)
      case Left(error) => Consequence.operationInvalid(s"${error.code}: ${error.message}")
    }

  private def _cancelling(
    run: CarReviewRun,
    updatedat: ReviewInstant
  ): Consequence[CarReviewRun] =
    CarReviewRunLifecycle.requestCancellation(run, updatedat) match {
      case Right(changed) => Consequence.success(changed)
      case Left(error) => Consequence.operationInvalid(s"${error.code}: ${error.message}")
    }
}
