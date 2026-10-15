package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityTraceabilityCoveragePart {
  def assess(catalog: Catalog, path: DemandPath): RoleCoverage = {
    val usage = path.usage.toVector.map(a => OwnerTraceability(TraceabilityOwner.ComponentUsage(a.key), a.definition.traceability))
    val owners = (usage ++ _owners(path.required) ++ path.provider.toVector.flatMap(p => _owners(p.provider)))
      .foldLeft(Vector.empty[OwnerTraceability])((xs, x) => if (xs.exists(_.owner == x.owner)) xs else xs :+ x)
    val facts = catalog.traceability.filter(t => owners.exists(_.owner == t.owner))
    RoleCoverage(if (path.provider.isDefined) CoverageBoundary.ProviderPath else CoverageBoundary.RequiredPathOnly,
      owners, facts.filter(_.edge.role == TraceabilityRole.Specification), facts.filter(_.edge.role == TraceabilityRole.Evidence))
  }
  private def _owners(record: CapabilityRecord): Vector[OwnerTraceability] = Vector(
    OwnerTraceability(TraceabilityOwner.Declaration(record.key), record.declaration.traceability),
    OwnerTraceability(TraceabilityOwner.CapabilityDefinition(record.key.context, record.declaration.capability), record.definition.traceability))
}
