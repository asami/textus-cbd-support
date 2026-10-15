package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.CapabilityDefinition
import CapabilityIngestionModel.*
import CapabilityComponentUsageJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityComponentUsageProjectionCodec {
  private val _strict_parser = JawnParser(allowDuplicateKeys = false)
  def decode(json: String): Either[Diagnostic, ComponentUsageProjection] = for {
    text <- _nonnull(json, "/")
    parsed <- _strict_parser.parse(text).left.map(_ => _malformed("/", "Invalid JSON or duplicate object key."))
    projection <- CapabilityComponentUsageDefinitionJsonPart._decode(parsed)
  } yield projection
  def encode(projection: ComponentUsageProjection): Either[Diagnostic, String] = {
    val representation = CapabilityComponentUsageJsonEncodingPart._encode(projection)
    CapabilityComponentUsageDefinitionJsonPart._decode(representation).map(_ => representation.noSpaces)
  }
  def admit(projection: ComponentUsageProjection, scope: Scope, components: Vector[CapabilityDefinition], selectedUsage: Vector[ComponentUsageProjection]): Either[Diagnostic, ComponentUsageFamily.Available] = for {
    _ <- encode(projection)
    _ <- CapabilityIngestionSelectionPart._scope(scope, "/scope")
    _ <- _values(components, "/components")(CapabilityIngestionSelectionPart._definition)
    _ <- _values(selectedUsage, "/usage")((p, _) => encode(p).map(_ => ()))
    external <- CapabilityComponentUsageBindingPart._bind(projection, scope, components, selectedUsage)
  } yield ComponentUsageFamily.Available(projection, external)
}
