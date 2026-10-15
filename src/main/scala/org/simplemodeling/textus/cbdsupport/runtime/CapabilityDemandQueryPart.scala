package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityDemandQueryPart {
  def trace(catalog: Catalog, reference: ComponentUsageCapabilityReference): Either[QueryFailure, Vector[ComponentUsageTrace]] = {
    val available = catalog.projection.usage.exists(_.family.isInstanceOf[ComponentUsageFamily.Available])
    if (!available) {
      val unavailable = catalog.projection.usage.collectFirst { case ComponentUsagePackage(_, _, ComponentUsageFamily.Unavailable(value)) => value }
        .getOrElse(UnavailableUpstreamProjection(ProjectionFamily.ComponentUsage, "not-supplied", "No selected Component Usage projection is available."))
      Left(QueryFailure.UnavailableProjection(unavailable))
    } else Right(catalog.usages.filter(_.key.reference == reference).map { usage =>
      ComponentUsageTrace(usage, catalog.demandTrace.usagePaths.filter(_.owner == usage.key).map { path =>
        val required = path.requiredEndpoint match {
          case RequiredEndpoint.Resolved(keys) => keys.map { key =>
            val record = catalog.capabilities.find(_.key == key).get
            RequiredProviderTrace(record, CapabilityProviderMatchingPart.find(catalog, record.declaration.capability))
          }
          case _: RequiredEndpoint.Unresolved => Vector.empty
        }
        RequirementPathTrace(path, required)
      })
    })
  }
  def diagnose(catalog: Catalog, scope: QueryScope, demand: DemandSelection): Either[QueryFailure, DemandDiagnosis] = demand match {
    case DemandSelection.UseCase(_) => Left(QueryFailure.UnavailableProjection(catalog.projection.useCase))
    case DemandSelection.Required(key) => catalog.capabilities.find(_.key == key) match {
      case None => Left(QueryFailure.Source(Diagnostic(DiagnosticKind.Unavailable, "Selected admitted Required declaration is unavailable.",
        Some(scope.provenance.scope.scopeId), Some(key.context.component.componentId), Some(scope.provenance.scope.source), "/query/required")))
      case Some(record) =>
        val required = RequiredProviderTrace(record, CapabilityProviderMatchingPart.find(catalog, record.declaration.capability))
        Right(DemandDiagnosis(demand, Vector.empty, Vector(required), _gaps(catalog, scope, None, None, required)))
    }
    case DemandSelection.ComponentUsage(reference) =>
      val families = catalog.projection.usage
      val unavailable = families.exists(_.family.isInstanceOf[ComponentUsageFamily.Unavailable]) || families.isEmpty
      val available = families.exists(_.family.isInstanceOf[ComponentUsageFamily.Available])
      if (!available) Right(DemandDiagnosis(demand, Vector.empty, Vector.empty,
        Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(scope, reference, MissingComponentUsageReason.ProjectionNotSupplied, families))))
      else trace(catalog, reference).map { usages =>
        val gaps = if (usages.isEmpty) Vector(CapabilityGap.MissingComponentUsageCapabilityProjection(scope, reference,
          if (unavailable) MissingComponentUsageReason.ProjectionNotSupplied else MissingComponentUsageReason.AbsentSelectedReference, families))
        else usages.flatMap { usage => usage.paths.flatMap { path => path.path.requiredEndpoint match {
          case RequiredEndpoint.Unresolved(_, ReferenceBoundary.Local) => Vector(CapabilityGap.MissingRequiredComponentCapability(scope, usage.usage, path.path))
          case RequiredEndpoint.Unresolved(_, ReferenceBoundary.External) => Vector.empty
          case _: RequiredEndpoint.Resolved => path.required.flatMap(r => _gaps(catalog, scope, Some(usage.usage), Some(path.path), r))
        } } }
        DemandDiagnosis(demand, usages, Vector.empty, gaps)
      }
  }
  private def _gaps(catalog: Catalog, scope: QueryScope, usage: Option[ComponentUsageCapabilityRecord], mapping: Option[ComponentUsageRequirementPath],
    required: RequiredProviderTrace): Vector[CapabilityGap] = {
    val search = required.providers
    if (search.matches.isEmpty) {
      val path = DemandPath(usage, mapping, required.required, None)
      val first = if (search.candidates.isEmpty) CapabilityGap.NoProvider(scope, path, search)
        else CapabilityGap.IncompatibleProviderVersion(scope, path, search)
      Vector(first) ++ _roles(catalog, scope, path)
    } else search.matches.flatMap { provider =>
      val path = DemandPath(usage, mapping, required.required, Some(provider))
      val realization = if (provider.provider.realizations.isEmpty)
        Vector(CapabilityGap.MissingRealization(scope, path, provider.provider.declaration.realizations)) else Vector.empty
      realization ++ _roles(catalog, scope, path)
    }
  }
  private def _roles(catalog: Catalog, scope: QueryScope, path: DemandPath): Vector[CapabilityGap] = {
    val coverage = CapabilityTraceabilityCoveragePart.assess(catalog, path)
    (if (coverage.specification.isEmpty) Vector(CapabilityGap.MissingSpecification(scope, path, coverage)) else Vector.empty) ++
      (if (coverage.evidence.isEmpty) Vector(CapabilityGap.MissingEvidence(scope, path, coverage)) else Vector.empty)
  }
}
