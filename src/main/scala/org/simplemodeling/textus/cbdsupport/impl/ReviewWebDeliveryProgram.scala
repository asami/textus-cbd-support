package org.simplemodeling.textus.cbdsupport.impl

import cats.syntax.all.*
import org.goldenport.cncf.action.{ActionCall, ActionCallEntityStorePart}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.simplemodeling.textus.cbdsupport.runtime.{CarReviewDeliveryDocument, CarReviewWebDeliveryApplication, CarReviewWebDiagnosis, ReviewReportId}

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[cbdsupport] final class ReviewWebDeliveryProgram(
  val core: ActionCall.Core
) extends ActionCall.Core.Holder with ActionCallEntityStorePart {
  def dashboard(
    reportId: ReviewReportId,
    roles: Set[String]
  ): ExecUowM[CarReviewDeliveryDocument] =
    for {
      report <- new ReviewDiagnosisHistoryProgram(core).loadReport(reportId)
      document <- exec_from(CarReviewWebDeliveryApplication.dashboard(report, roles))
    } yield document

  def diagnosis(
    reportId: ReviewReportId,
    kind: String,
    itemId: String,
    roles: Set[String]
  ): ExecUowM[CarReviewWebDiagnosis] =
    for {
      report <- new ReviewDiagnosisHistoryProgram(core).loadReport(reportId)
      diagnosis <- exec_from(CarReviewWebDeliveryApplication.diagnosis(report, kind, itemId, roles))
    } yield diagnosis
}
