package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityProviderMatchingPart {
  def find(catalog: Catalog, required: CapabilityReference): ProviderSearch = {
    val candidates = catalog.capabilities.filter(c => c.declaration.direction == Direction.Provided && c.declaration.capability.identity == required.identity)
    val compatibility = catalog.compatibility.filter(_.edge.identity == required.identity)
    val assessed = candidates.map { candidate =>
      val provided = candidate.declaration.capability
      val witnesses = compatibility.filter(c => c.edge.requiredVersion == required.contractVersion && c.edge.providedVersion == provided.contractVersion)
      val basis = if (provided.contractVersion == required.contractVersion) Some(MatchBasis.SameVersion)
        else if (witnesses.nonEmpty) Some(MatchBasis.ExplicitCompatibility(witnesses)) else None
      (candidate, basis)
    }
    ProviderSearch(required, candidates, assessed.flatMap((c, b) => b.map(ProviderMatch(c, _))),
      assessed.collect { case (c, None) => c }, compatibility)
  }
}
