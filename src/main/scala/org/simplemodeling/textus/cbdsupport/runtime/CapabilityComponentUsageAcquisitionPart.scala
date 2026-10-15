package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.ComponentFactory
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import CapabilityIngestionModel.*
import CapabilityComponentUsageJsonPart.*

/*
 * CNCF supplies the acquired transport; CBD observes only Usage semantics.
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityComponentUsageAcquisitionPart {
  val resourcePath: String = "meta/component-usage-capability/usage.json"
  def acquire(expected: PackageExpectation): Either[Diagnostic, ComponentUsagePackage] =
    ComponentFactory.acquireUsageCapabilities(expected.input).flatMap { acquired =>
      val result = acquired match {
        case Some(value) => for {
          projection <- CapabilityComponentUsageProjectionCodec.decode(value.content)
          _ <- observe(expected, projection)
        } yield ComponentUsagePackage(value.input, value.resourcePath, ComponentUsageFamily.Available(projection, Vector.empty))
        case None => expected.usage match {
          case Some(_) => Left(_diagnostic(DiagnosticKind.Unavailable, "Selected Component Usage projection is unavailable.", "/usage"))
          case None => Right(ComponentUsagePackage(expected.input, resourcePath, ComponentUsageFamily.Unavailable(
            UnavailableUpstreamProjection(ProjectionFamily.ComponentUsage, "not-supplied", "Optional Component Usage projection is unavailable."))))
        }
      }
      _context(result, None, Some(expected.input.scopeId), Some(expected.input.component.componentId))
    }
  def observe(expected: PackageExpectation, projection: ComponentUsageProjection): Either[Diagnostic, Unit] = for {
    _ <- Either.cond(projection.component == expected.input.component, (),
      _diagnostic(DiagnosticKind.Incompatible, "Usage envelope differs from the selected Component.", "/usage/component"))
    _ <- Either.cond(projection.packageCoordinate == expected.input.packageCoordinate, (),
      _diagnostic(DiagnosticKind.Incompatible, "Usage envelope differs from the selected package.", "/usage/package"))
    header <- expected.usage.toRight(_diagnostic(DiagnosticKind.Incompatible, "Present Usage metadata was selected as absent.", "/usage"))
    _ <- CapabilityIngestionSelectionPart._usage_observed(header, projection)
  } yield ()
}
