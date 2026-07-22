package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
/** Authorized, exact-Report delivery for private Web forms. */
final case class CarReviewWebDiagnosis(
  diagnosis: CarReviewItemDiagnosis,
  nextActions: Vector[String]
)

object CarReviewWebDiagnosisKind {
  val OBSERVATION = "observation"
  val CAPABILITY = "capability"
}

final class CarReviewWebDeliveryApplication(repository: CarReviewRepository) {
  def dashboard(reportid: ReviewReportId, roles: Set[String]): Consequence[CarReviewDeliveryDocument] =
    _report(reportid, roles).map(CarReviewDeliveryProjection.project)

  def diagnosis(
    reportid: ReviewReportId,
    kind: String,
    itemid: String,
    roles: Set[String]
  ): Consequence[CarReviewWebDiagnosis] =
    dashboard(reportid, roles).flatMap { document =>
      _diagnosis(document, kind, itemid)
        .map(value => Consequence.success(CarReviewWebDiagnosis(value, _next_actions(document, value))))
        .getOrElse(Consequence.operationNotFound(s"review delivery item: $kind:$itemid"))
    }

  private def _report(reportid: ReviewReportId, roles: Set[String]): Consequence[CarReviewReport] =
    CarReviewAuthorization.authorize("review.read-run", roles).flatMap { _ =>
      repository.report(reportid)
        .map(Consequence.success)
        .getOrElse(Consequence.operationNotFound(s"review report: ${reportid.value}"))
    }

  private def _diagnosis(
    document: CarReviewDeliveryDocument,
    kind: String,
    itemid: String
  ): Option[CarReviewItemDiagnosis] =
    kind match {
      case CarReviewWebDiagnosisKind.OBSERVATION => document.diagnoseObservation(ReviewObservationId(itemid))
      case CarReviewWebDiagnosisKind.CAPABILITY => document.diagnoseCapability(ReviewCapabilityId(itemid))
      case _ => None
    }

  private def _next_actions(
    document: CarReviewDeliveryDocument,
    diagnosis: CarReviewItemDiagnosis
  ): Vector[String] =
    diagnosis.kind match {
      case CarReviewWebDiagnosisKind.OBSERVATION =>
        document.observations.find(_.id.value == diagnosis.itemId).map { observation =>
          observation.`type`.value match {
            case "finding" => Vector("Inspect the linked rule, evidence, locations, and disposition before choosing remediation.")
            case "unknown" => Vector("Inspect the linked limitation and obtain the missing admitted evidence before changing maturity.")
            case _ => Vector("Retain the linked evidence and re-review when the target or policy changes.")
          }
        }.getOrElse(Vector.empty)
      case CarReviewWebDiagnosisKind.CAPABILITY =>
        Vector("Inspect linked observations, evidence, strengths, gaps, and limitations before changing the capability assessment.")
      case _ => Vector.empty
    }
}
