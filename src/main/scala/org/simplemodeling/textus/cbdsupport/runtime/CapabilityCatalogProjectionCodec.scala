package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.jawn.JawnParser
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.Diagnostic
import CapabilityCatalogModel.*
import CapabilityApplicationJsonPart.*

/*
 * @since   Oct. 15, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityCatalogProjectionCodec {
  private val _strict_parser = JawnParser(allowDuplicateKeys = false)
  def decode(json: String, selection: CatalogSelection): Either[Diagnostic, Catalog] = for {
    text <- _nonnull(json, "/")
    parsed <- _strict_parser.parse(text).left.map(_ => _malformed("/", "Invalid JSON or duplicate object key."))
    projection <- CapabilityCatalogJsonPart.decode(parsed)
    catalog <- validate(projection, selection)
  } yield catalog
  def validate(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, Catalog] =
    CapabilityCatalogBindingPart.bind(projection, selection)
  def encode(projection: PortableProjection, selection: CatalogSelection): Either[Diagnostic, String] = for {
    _ <- validate(projection, selection)
    json <- CapabilityCatalogJsonEncodingPart.encode(projection)
  } yield json.noSpaces
}
