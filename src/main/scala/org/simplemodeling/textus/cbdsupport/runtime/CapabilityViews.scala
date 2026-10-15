package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityReference.ModelElementId
import CapabilityIngestionModel.ComponentUsageCapabilityReference
import CapabilityQueryModel.*
import CapabilityViewModel.*

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityViews {
  def overview(session: CapabilityQueries.Session): Overview =
    Overview(session.scope, session.catalog.capabilities, session.catalog.usages, session.catalog.projection.useCase)

  def detail(session: CapabilityQueries.Session, lookup: CapabilityLookup): Either[QueryFailure, Detail] =
    session.getCapability(lookup).map(Detail(_))

  def usageTrace(session: CapabilityQueries.Session, reference: ComponentUsageCapabilityReference): Either[QueryFailure, UsageTrace] =
    for {
      result <- session.traceComponentUsageCapability(reference)
      diagnosis <- session.diagnoseCapabilityDemand(DemandSelection.ComponentUsage(reference))
    } yield UsageTrace(result, diagnosis)

  def useCaseTrace(session: CapabilityQueries.Session, reference: ModelElementId): Either[QueryFailure, QueryResult[Nothing]] =
    session.traceUseCaseDemand(reference)
}
