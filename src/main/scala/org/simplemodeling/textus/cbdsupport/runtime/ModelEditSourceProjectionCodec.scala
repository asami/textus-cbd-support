package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.jawn.JawnParser
import ModelEditSourceProjection.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Closed, producer-versioned JSON. Optional producer fields may be omitted. */
object ModelEditSourceProjectionCodec {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private def _closed[A](fields: String*)(using decoder: Decoder[A], encoder: Encoder.AsObject[A]): Codec.AsObject[A] =
    Codec.AsObject.from(Decoder.instance { cursor =>
      if (cursor.value.asObject.exists(_.keys.toSet.subsetOf(fields.toSet))) decoder(cursor)
      else Left(DecodingFailure("unknown source record fields", cursor.history))
    }, encoder)
  given Codec.AsObject[RecordRef] = _closed[RecordRef]("owner", "identity", "revision")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Source] = _closed[Source]("owner", "identity", "revision")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Producer] = _closed[Producer]("name", "version", "revision", "modified")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Field] = _closed[Field]("name", "datatype", "multiplicity")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Element] = _closed[Element]("ref", "kind", "title", "intent", "fields", "scope", "acceptance", "lifecycle", "source")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Specification] = _closed[Specification]("owner", "identity", "sourceRevision")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Observation] = _closed[Observation]("state", "revision")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Ticket] = _closed[Ticket]("provider", "owner", "externalIdentity", "observation")(using deriveDecoder, deriveEncoder)
  given Codec[Endpoint] = Codec.from(Decoder.instance { cursor =>
    val keys = cursor.value.asObject.map(_.keys.toSet).getOrElse(Set.empty)
    cursor.get[String]("kind").flatMap {
      case "local" if keys == Set("kind", "ref") => cursor.get[RecordRef]("ref").map(Endpoint.Local(_))
      case "external" if keys == Set("kind", "ref") => cursor.get[RecordRef]("ref").map(Endpoint.External(_))
      case "specification" if keys == Set("kind", "specification") => cursor.get[Specification]("specification").map(Endpoint.SpecificationTarget(_))
      case "ticket" if keys == Set("kind", "ticket") => cursor.get[Ticket]("ticket").map(Endpoint.TicketTarget(_))
      case _ => Left(DecodingFailure("unknown or malformed source endpoint", cursor.history))
    }
  }, Encoder.instance {
    case Endpoint.Local(ref) => Json.obj("kind" -> Json.fromString("local"), "ref" -> ref.asJson)
    case Endpoint.External(ref) => Json.obj("kind" -> Json.fromString("external"), "ref" -> ref.asJson)
    case Endpoint.SpecificationTarget(value) => Json.obj("kind" -> Json.fromString("specification"), "specification" -> value.asJson)
    case Endpoint.TicketTarget(value) => Json.obj("kind" -> Json.fromString("ticket"), "ticket" -> value.asJson)
  })
  given Codec.AsObject[Relationship] = _closed[Relationship]("ref", "kind", "from", "to", "source")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Coverage] = _closed[Coverage]("family", "status", "detail")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Document] = _closed[Document]("schemaVersion", "coverage", "elements", "relationships")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Limitation] = _closed[Limitation]("code", "detail")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Publication] = _closed[Publication]("schemaVersion", "source", "producer", "document", "limitations")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Rendering] = _closed[Rendering]("schemaVersion", "source", "producer", "candidateSource", "text", "limitations")(using deriveDecoder, deriveEncoder)
  import ModelEditCandidateCodec.given
  given Codec.AsObject[Binding] = _closed[Binding]("sourceRef", "semanticIdentity", "recordReference", "communicationId")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[OwnerBinding] = _closed[OwnerBinding]("sourceOwner", "owner")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Selection] = _closed[Selection]("publication", "sourceArtifact", "bindings", "owners", "condition")(using deriveDecoder, deriveEncoder)

  def decodePublication(text: String): Either[String, Publication] =
    _parser.parse(text).left.map(_.message).flatMap(_.as[Publication].left.map(_.message)).flatMap(ModelEditSourceProjection.validate)
  def encodePublication(publication: Publication): String = publication.asJson.noSpaces
  def decodeRendering(text: String): Either[String, Rendering] =
    _parser.parse(text).left.map(_.message).flatMap(_.as[Rendering].left.map(_.message)).flatMap { rendering =>
      Either.cond(rendering.schemaVersion == "cozy.cml.model-edit-candidate.v2" && rendering.text.nonEmpty,
        rendering, "unsupported or empty Cozy rendering")
    }
}
