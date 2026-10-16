package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.Consequence
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.cbdsupport.entity.ReviewDiagnosis
import org.simplemodeling.textus.cbdsupport.runtime.CarReviewExecutionPlan

/*
 * @since   Sep. 27, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[impl] object ReviewDiagnosisIdentity {
  private[impl] def _diagnosis_id(plan: CarReviewExecutionPlan): Consequence[EntityId] =
    if (plan.reference == null || plan.reference.diagnosisId == null || plan.reference.revision <= 0 ||
        plan.reference.diagnosisId.collection != ReviewDiagnosis.collectionId)
      Consequence.operationInvalid("review-plan-reference-invalid")
    else
      Consequence.success(plan.reference.diagnosisId)
}
