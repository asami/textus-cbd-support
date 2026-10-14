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
  def trace(catalog: Catalog, reference: ApplicationCapabilityReference): Either[QueryFailure, Vector[ApplicationTrace]] =
    catalog.projection.application match {
      case ApplicationFamily.Unavailable(value) => Left(QueryFailure.UnavailableProjection(value))
      case _: ApplicationFamily.Available => Right(catalog.applications.filter(_.key.reference == reference).map { application =>
        ApplicationTrace(application, catalog.demandTrace.applicationPaths.filter(_.owner == application.key).map { path =>
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
    case DemandSelection.Application(reference) => catalog.projection.application match {
      case family: ApplicationFamily.Unavailable => Right(DemandDiagnosis(demand, Vector.empty, Vector.empty,
        Vector(CapabilityGap.MissingApplicationCapabilityProjection(scope, reference, MissingApplicationReason.ProjectionNotSupplied, family))))
      case family: ApplicationFamily.Available => trace(catalog, reference).map { applications =>
        val gaps = if (applications.isEmpty) Vector(CapabilityGap.MissingApplicationCapabilityProjection(scope, reference, MissingApplicationReason.AbsentSelectedReference, family))
          else applications.flatMap { app => app.paths.flatMap { path => path.path.requiredEndpoint match {
            case RequiredEndpoint.Unresolved(_, ReferenceBoundary.Local) => Vector(CapabilityGap.MissingRequiredComponentCapability(scope, app.application, path.path))
            case RequiredEndpoint.Unresolved(_, ReferenceBoundary.External) => Vector.empty
            case _: RequiredEndpoint.Resolved => path.required.flatMap(r => _gaps(catalog, scope, Some(app.application), Some(path.path), r))
          } } }
        DemandDiagnosis(demand, applications, Vector.empty, gaps)
      }
    }
  }
  private def _gaps(catalog: Catalog, scope: QueryScope, application: Option[ApplicationRecord], mapping: Option[ApplicationRequirementPath],
    required: RequiredProviderTrace): Vector[CapabilityGap] = {
    val search = required.providers
    if (search.matches.isEmpty) {
      val path = DemandPath(application, mapping, required.required, None)
      val first = if (search.candidates.isEmpty) CapabilityGap.NoProvider(scope, path, search)
        else CapabilityGap.IncompatibleProviderVersion(scope, path, search)
      Vector(first) ++ _roles(catalog, scope, path)
    } else search.matches.flatMap { provider =>
      val path = DemandPath(application, mapping, required.required, Some(provider))
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
