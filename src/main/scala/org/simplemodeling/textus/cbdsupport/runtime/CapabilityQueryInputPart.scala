package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.*
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityQueryModel.*
import CapabilityApplicationJsonPart.{_check_model_id, _text, _path, _malformed}

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityQueryInputPart {
  def capability(reference: CapabilityReference, scope: QueryScope): Either[Diagnostic, Unit] = _context(for {
    _ <- _check_model_id(reference.identity, "/query/capability/identity")
    _ <- _text(reference.contractVersion, "/query/capability/contractVersion")
  } yield (), scope)
  def component(coordinate: ComponentCoordinate, scope: QueryScope): Either[Diagnostic, Unit] =
    _context(_text(coordinate.version, "/query/component/version"), scope, Some(coordinate.componentId))
  def application(reference: ApplicationCapabilityReference, scope: QueryScope): Either[Diagnostic, Unit] = _context(for {
    _ <- _check_model_id(reference.identity, "/query/application/identity")
    _ <- _text(reference.contractVersion, "/query/application/contractVersion")
  } yield (), scope)
  def useCase(reference: ModelElementId, scope: QueryScope): Either[Diagnostic, Unit] =
    _context(_check_model_id(reference, "/query/useCase"), scope)
  def search(text: String, scope: QueryScope): Either[Diagnostic, Unit] =
    _context(Either.cond(text.nonEmpty, (), _malformed("/query/search", "A nonempty literal search text is required.")), scope)
  def required(key: DeclarationRecordKey, scope: QueryScope): Either[Diagnostic, Unit] = _context(for {
    _ <- Either.cond(key.declaration.direction == Direction.Required, (), _malformed("/query/required/direction", "A Required declaration key is required."))
    _ <- _check_model_id(key.declaration.capability.identity, "/query/required/capability/identity")
    _ <- _text(key.declaration.capability.contractVersion, "/query/required/capability/contractVersion")
    _ <- _text(key.context.scopeId, "/query/required/context/scopeId")
    _ <- _text(key.context.component.version, "/query/required/context/component/version")
    _ <- _text(key.context.packageCoordinate.coordinate, "/query/required/context/packageCoordinate/coordinate")
    _ <- _text(key.context.packageCoordinate.version, "/query/required/context/packageCoordinate/version")
    _ <- _path(key.context.resourcePath, "/query/required/context/resourcePath")
    _ <- _text(key.context.source.modelAuthority, "/query/required/context/source/modelAuthority")
    _ <- _path(key.context.source.path, "/query/required/context/source/path")
    _ <- _text(key.context.source.sourceRevision, "/query/required/context/source/sourceRevision")
    _ <- Either.cond(key.declaration.component == key.context.component.componentId, (),
      _malformed("/query/required/component", "Declaration Component must equal its owning context Component."))
  } yield (), scope, Some(key.context.component.componentId))
  private def _context(result: Either[Diagnostic, Unit], scope: QueryScope, component: Option[ComponentId] = None): Either[Diagnostic, Unit] =
    result.left.map(_.copy(scopeId = Some(scope.provenance.scope.scopeId), source = Some(scope.provenance.scope.source), component = component))
}
