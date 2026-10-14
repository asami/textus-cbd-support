package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.CapabilityDefinition
import CapabilityIngestionModel.*
import CapabilityApplicationJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityApplicationProjectionCodec {
  private val _strict_parser = JawnParser(allowDuplicateKeys = false)
  def decode(json: String): Either[Diagnostic, ApplicationProjection] = for {
    text <- _nonnull(json, "/")
    parsed <- _strict_parser.parse(text).left.map(_ => _malformed("/", "Invalid JSON or duplicate object key."))
    projection <- CapabilityApplicationDefinitionJsonPart._decode(parsed)
  } yield projection
  def encode(projection: ApplicationProjection): Either[Diagnostic, String] = {
    val representation = CapabilityApplicationJsonEncodingPart._encode(projection)
    CapabilityApplicationDefinitionJsonPart._decode(representation).map(_ => representation.noSpaces)
  }
  def admit(projection: ApplicationProjection, scope: Scope, components: Vector[CapabilityDefinition]): Either[Diagnostic, ApplicationFamily.Available] = for {
    _ <- encode(projection)
    _ <- CapabilityIngestionSelectionPart._scope(scope, "/scope")
    _ <- _values(components, "/components")(CapabilityIngestionSelectionPart._definition)
    external <- CapabilityApplicationBindingPart._bind(projection, scope, components)
  } yield ApplicationFamily.Available(projection, external)
}
