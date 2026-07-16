package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Explicit MCP exposure table; unknown Review actions are private by default. */
object CarReviewMcpExposurePolicy {
  val ReadOnlyOperations: Set[String] = Set(
    "getReviewRun",
    "getReviewSummary",
    "getReviewReport",
    "listReviewFindings",
    "listReviewAssurances"
  )

  val PrivateOperations: Set[String] = Set(
    "startReview",
    "cancelReview",
    "deleteReviewReport",
    "purgeReviewRetention",
    "configureReviewFilesystem",
    "enableExternalReviewProvider",
    "enableAiReview"
  )

  def isMcpReadable(operation: String): Boolean = ReadOnlyOperations.contains(operation)
  def isMcpPrivate(operation: String): Boolean = !isMcpReadable(operation)
}
