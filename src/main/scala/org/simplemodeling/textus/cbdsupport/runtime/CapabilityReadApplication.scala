package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.config.{ComponentConfigurationAccess, ComponentConfigurationKey}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import CapabilityQueryModel.*
import CapabilityReadSelectorPart.Selector
import CapabilityComponentUsageJsonPart.{_diagnostic, _malformed}

/*
 * The sole public bridge from configured admitted queries to generated surfaces.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
object CapabilityReadApplication {
  final case class Response(status: String, resultJson: String, html: String)

  def read(operation: String, selector: String, access: ComponentConfigurationAccess)(using ctx: ExecutionContext): Consequence[Response] =
    if (CarReviewAuthorization.roles(ctx).intersect(CarReviewAuthorization.readRoles).isEmpty)
      Consequence.securityPermissionDenied("capability.read requires a read role")
    else if (!CapabilityReadSelectorPart.operations.contains(operation))
      Consequence.operationInvalid("Unknown Capability read operation: " + operation)
    else Consequence.success {
      val prepared = for {
        selected <- CapabilityReadSelectorPart.decode(operation, selector)
        projectiontext <- _configuration(access, "textus.cbd.capability.projection")
        selectiontext <- _configuration(access, "textus.cbd.capability.selection")
        selectionjson <- CapabilityReadSelectorPart.parse(selectiontext)
        selection <- CapabilityCatalogJsonPart.decodeSelection(selectionjson)
        projectionjson <- CapabilityReadSelectorPart.parse(projectiontext)
        projection <- CapabilityCatalogJsonPart.decode(projectionjson)
        session <- CapabilityQueries.open(Right(projection), selection)
      } yield (session, selected)
      prepared match {
        case Left(diagnostic) => _failure(operation, None, QueryFailure.Source(diagnostic))
        case Right((session, selected)) => _read(operation, session, selected)
      }
    }

  private def _configuration(access: ComponentConfigurationAccess, key: String): Either[Diagnostic, String] =
    access.resolve(ComponentConfigurationKey.optionalString(key)) match {
      case Consequence.Success(resolved) =>
        resolved.value.toRight(_diagnostic(DiagnosticKind.Unavailable, "Capability server input is unavailable: " + key, "/" + key))
      case Consequence.Failure(_) =>
        Left(_malformed("/" + key, "Capability server input must be a JSON string."))
    }

  private def _read(operation: String, session: CapabilityQueries.Session, selector: Selector): Response = {
    def _project_[A](queried: Either[QueryFailure, A])(encode: A => Json, render: A => String): Response = queried match {
      case Left(failure) => _failure(operation, Some(session.scope), failure)
      case Right(value) => _success(operation, session.scope, encode(value), render(value))
    }
    selector match {
      case Selector.Overview =>
        val value = CapabilityViews.overview(session)
        _success(operation, session.scope, CapabilityReadJsonPart.overview(value), CapabilityWebView.overview(value))
      case Selector.Get(lookup) => _project_(CapabilityViews.detail(session, lookup))(CapabilityReadJsonPart.detail, CapabilityWebView.detail)
      case Selector.Providers(reference) => _project_(session.findProviders(reference))(
        r => CapabilityReadJsonPart.providers(r.value), CapabilityWebView.providers)
      case Selector.Requirements(component) => _project_(session.findRequirements(component))(
        r => CapabilityReadJsonPart.requirements(r.value), CapabilityWebView.requirements)
      case Selector.Usage(reference) => _project_(CapabilityViews.usageTrace(session, reference))(CapabilityReadJsonPart.usageTrace, CapabilityWebView.usageTrace)
      case Selector.UseCase(reference) => _project_(CapabilityViews.useCaseTrace(session, reference))(
        r => r.value, r => r.value)
      case Selector.Diagnose(selection) => _project_(session.diagnoseCapabilityDemand(selection))(
        r => CapabilityReadJsonPart.diagnosis(r.value), CapabilityWebView.diagnosis)
    }
  }
  private def _success(operation: String, scope: QueryScope, value: Json, html: String): Response =
    Response("OK", CapabilityReadJsonPart.envelope(operation, "OK", Some(scope), value, Json.Null).noSpaces, html)
  private def _failure(operation: String, scope: Option[QueryScope], failure: QueryFailure): Response = {
    val status = failure match {
      case _: QueryFailure.Source => "SOURCE_FAILURE"
      case _: QueryFailure.UnavailableProjection => "UNAVAILABLE_PROJECTION"
    }
    Response(status, CapabilityReadJsonPart.envelope(operation, status, scope, Json.Null, CapabilityReadJsonPart.failure(failure)).noSpaces,
      CapabilityWebView.failure(failure))
  }
}
