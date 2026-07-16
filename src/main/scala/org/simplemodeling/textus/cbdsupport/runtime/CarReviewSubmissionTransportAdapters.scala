package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Shared byte-bounded admission used by the private HTTP, CLI, and component adapters. */
final class CarReviewSubmissionBoundedAdapter(
  wireApplication: CarReviewSubmissionWireApplication
) {
  import CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES

  def submit(
    document: String,
    actorroles: Set[String]
  ): Consequence[String] =
    for {
      _ <- _bounded(document)
      response <- wireApplication.submit(document, actorroles)
    } yield response

  private def _bounded(value: String): Consequence[Unit] =
    if value.getBytes(StandardCharsets.UTF_8).length <= MAX_REQUEST_BYTES then Consequence.unit
    else Consequence.operationInvalid("review-submission-request-too-large")
}

/** Private HTTP adapter for the one CBD submission application. */
final class CarReviewSubmissionHttpAdapter(
  wireApplication: CarReviewSubmissionWireApplication
) {
  private val _bounded_adapter = new CarReviewSubmissionBoundedAdapter(wireApplication)

  def postJson(
    contentType: String,
    body: String,
    actorroles: Set[String]
  ): Consequence[String] =
    if contentType.equalsIgnoreCase("application/json") then
      _bounded_adapter.submit(body, actorroles)
    else
      Consequence.operationInvalid("review-submission-content-type-invalid")
}

final class CarReviewSubmissionCliAdapter(
  wireApplication: CarReviewSubmissionWireApplication
) {
  private val _bounded_adapter = new CarReviewSubmissionBoundedAdapter(wireApplication)

  def submitStdin(
    stdin: String,
    actorroles: Set[String]
  ): Consequence[String] = _bounded_adapter.submit(stdin, actorroles)
}

object CarReviewSubmissionTransportAdapters {
  val MAX_REQUEST_BYTES = 128 * 1024 * 1024
}
