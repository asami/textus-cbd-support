package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Private HTTP and local CLI adapters for the one CBD submission application. */
final class CarReviewSubmissionHttpAdapter(
  wireApplication: CarReviewSubmissionWireApplication
) {
  import CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES

  def postJson(
    contentType: String,
    body: String,
    actorroles: Set[String]
  ): Consequence[String] =
    for {
      _ <- _content_type(contentType)
      _ <- _bounded(body)
      response <- wireApplication.submit(body, actorroles)
    } yield response

  private def _content_type(value: String): Consequence[Unit] =
    if value.equalsIgnoreCase("application/json") then Consequence.unit
    else Consequence.operationInvalid("review-submission-content-type-invalid")

  private def _bounded(value: String): Consequence[Unit] =
    if value.getBytes(StandardCharsets.UTF_8).length <= MAX_REQUEST_BYTES then Consequence.unit
    else Consequence.operationInvalid("review-submission-request-too-large")
}

final class CarReviewSubmissionCliAdapter(
  wireApplication: CarReviewSubmissionWireApplication
) {
  import CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES

  def submitStdin(
    stdin: String,
    actorroles: Set[String]
  ): Consequence[String] =
    if stdin.getBytes(StandardCharsets.UTF_8).length <= MAX_REQUEST_BYTES then
      wireApplication.submit(stdin, actorroles)
    else
      Consequence.operationInvalid("review-submission-request-too-large")
}

object CarReviewSubmissionTransportAdapters {
  val MAX_REQUEST_BYTES = 128 * 1024 * 1024
}
