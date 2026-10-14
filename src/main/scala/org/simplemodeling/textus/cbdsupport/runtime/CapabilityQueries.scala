package org.simplemodeling.textus.cbdsupport.runtime

import java.util.Locale
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityQueries {
  def open(input: Either[Diagnostic, PortableProjection], selection: CatalogSelection): Either[Diagnostic, Session] =
    input.flatMap(p => CapabilityCatalogProjectionCodec.validate(p, selection).map(new Session(_)))
  final class Session private[CapabilityQueries] (val catalog: Catalog) {
    val scope: QueryScope = QueryScope(catalog.projection.snapshotReference, catalog.projection.provenance)
    def getCapability(lookup: CapabilityLookup): Either[QueryFailure, QueryResult[CapabilityLookupResult]] = lookup match {
      case CapabilityLookup.Exact(reference) => _checked(CapabilityQueryInputPart.capability(reference, scope)) {
        CapabilityLookupResult(lookup, LookupDesignation.ExactIdentity, catalog.capabilities.filter(_.declaration.capability == reference))
      }
      case CapabilityLookup.Search(text) => _checked(CapabilityQueryInputPart.search(text, scope)) {
        val needle = text.toLowerCase(Locale.ROOT)
        val records = catalog.capabilities.filter { record =>
          val definition = record.definition
          (Vector(definition.name) ++ _text(definition.summary) ++ _text(definition.description)).exists(_.toLowerCase(Locale.ROOT).contains(needle))
        }
        CapabilityLookupResult(lookup, LookupDesignation.DiscoveryOnly, records)
      }
    }
    def findProviders(required: CapabilityReference): Either[QueryFailure, QueryResult[ProviderSearch]] =
      _checked(CapabilityQueryInputPart.capability(required, scope))(CapabilityProviderMatchingPart.find(catalog, required))
    def findRequirements(component: ComponentCoordinate): Either[QueryFailure, QueryResult[RequirementSearch]] =
      _checked(CapabilityQueryInputPart.component(component, scope))(RequirementSearch(component,
        catalog.capabilities.filter(c => c.declaration.direction == Direction.Required && c.key.context.component == component)))
    def traceApplicationCapability(reference: ApplicationCapabilityReference): Either[QueryFailure, QueryResult[Vector[ApplicationTrace]]] =
      CapabilityQueryInputPart.application(reference, scope).left.map(QueryFailure.Source(_))
        .flatMap(_ => CapabilityDemandQueryPart.trace(catalog, reference)).map(QueryResult(scope, _))
    def traceUseCaseDemand(reference: ModelElementId): Either[QueryFailure, QueryResult[Nothing]] =
      CapabilityQueryInputPart.useCase(reference, scope).left.map(QueryFailure.Source(_))
        .flatMap(_ => Left(QueryFailure.UnavailableProjection(catalog.projection.useCase)))
    def diagnoseCapabilityDemand(demand: DemandSelection): Either[QueryFailure, QueryResult[DemandDiagnosis]] = {
      val validation = demand match {
        case DemandSelection.Application(reference) => CapabilityQueryInputPart.application(reference, scope)
        case DemandSelection.Required(key) => CapabilityQueryInputPart.required(key, scope)
        case DemandSelection.UseCase(reference) => CapabilityQueryInputPart.useCase(reference, scope)
      }
      validation.left.map(QueryFailure.Source(_)).flatMap(_ => CapabilityDemandQueryPart.diagnose(catalog, scope, demand)).map(QueryResult(scope, _))
    }
    private def _checked[A](validation: Either[Diagnostic, Unit])(value: => A): Either[QueryFailure, QueryResult[A]] =
      validation.left.map(QueryFailure.Source(_)).map(_ => QueryResult(scope, value))
    private def _text(value: Presence[String]): Vector[String] = value match {
      case Present(text) => Vector(text)
      case _: Absent => Vector.empty
    }
  }
}
