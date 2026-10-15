package org.simplemodeling.textus.cbdsupport.runtime

import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityViewModel {
  final case class Overview(scope: QueryScope, contracts: Vector[CapabilityRecord],
    usages: Vector[ComponentUsageCapabilityRecord], useCase: UnavailableUpstreamProjection)
  final case class Detail(result: QueryResult[CapabilityLookupResult])
  final case class UsageTrace(result: QueryResult[Vector[ComponentUsageTrace]], diagnosis: QueryResult[DemandDiagnosis])
}
