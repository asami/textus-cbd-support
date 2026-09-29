package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Base64

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits one captured candidate CML projection without reopening package or source paths. */
private[runtime] object InternalModelCandidateCmlProjectionValidator {
  private final case class SnapshotBaseline(
    source: InternalModelSemanticSource,
    projectrelativepath: String,
    rawbytes: Vector[Byte]
  )

  private val _snapshot_fields = Set("basis", "schemaVersion", "snapshotKind", "source")
  private val _source_fields = Set("authority", "identity", "locator", "revision", "sha256")
  private val _cml_basis_fields = Set("byteLength", "projectRelativePath", "rawBytesBase64")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)

  def validate(projectRoot: java.nio.file.Path): Consequence[InternalModelCandidateCmlAdmission] =
    try {
      InternalModelPackageValidator.verifiedCandidateCmlProjection(projectRoot).flatMap(validateVerified)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate CML projection validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def validateVerified(
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage
  ): Consequence[InternalModelCandidateCmlAdmission] =
    try {
      InternalModelProjectionContinuityValidator.validateVerified(handoff.continuityPackage).flatMap { continuity =>
        _admission(handoff, continuity).fold(Consequence.operationInvalid, Consequence.success)
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model candidate CML projection validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private def _admission(
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage,
    continuity: InternalModelProjectionContinuity
  ): Either[String, InternalModelCandidateCmlAdmission] =
    for {
      candidate <- InternalModelCandidateCmlProjectionCodec.decode(handoff.candidate)
      _ <- _candidate_binding(candidate, handoff, continuity)
      bytes <- candidate.targets.foldLeft[Either[String, Vector[InternalModelCandidateCmlTargetBytes]]](Right(Vector.empty)) { (result, target) =>
        for {
          collected <- result
          targetbytes <- _target_bytes(target, handoff)
          _ <- _mappings(target, continuity.realization)
          _ <- _effects(target, continuity.realization)
        } yield collected :+ targetbytes
      }
      artifact <- handoff.packageContext.artifacts.find(artifact => artifact.artifactId == handoff.candidate.artifactId && artifact.present).toRight("selected candidate artifact is absent from captured package context")
    } yield InternalModelCandidateCmlAdmission(
      projection = candidate,
      continuity = continuity,
      packageContext = handoff.packageContext,
      candidateArtifactId = artifact.artifactId,
      candidateArtifactSha256 = artifact.sha256,
      candidatePackageRelativePath = artifact.packageRelativePath,
      targetBytes = bytes
    )

  private def _candidate_binding(
    candidate: InternalModelCandidateCmlProjection,
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage,
    continuity: InternalModelProjectionContinuity
  ): Either[String, Unit] = {
    val realizationid = handoff.continuityPackage.realizationPackage.realization.artifactId
    val continuityid = handoff.continuityPackage.projection.artifactId
    val expecteddependencies = (Vector(realizationid, continuityid) ++ candidate.targets.map(_.baselineArtifactId)).toSet
    for {
      _ <- Either.cond(candidate.realizationArtifactId == realizationid, (), "candidate realizationArtifactId is not the selected realization artifact")
      _ <- Either.cond(candidate.continuityArtifactId == continuityid, (), "candidate continuityArtifactId is not the selected continuity artifact")
      _ <- Either.cond(candidate.scope == continuity.realization.scope, (), "candidate scope does not equal selected realization scope")
      _ <- Either.cond(handoff.candidate.dependencies.toSet == expecteddependencies, (), "candidate artifact dependencies do not equal realization, continuity, and selected baseline artifacts")
    } yield ()
  }

  private def _target_bytes(
    target: InternalModelCandidateCmlTarget,
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage
  ): Either[String, InternalModelCandidateCmlTargetBytes] =
    for {
      snapshot <- handoff.continuityPackage.realizationPackage.sourceSnapshots.filter(_.artifactId == target.baselineArtifactId) match {
        case Vector(value) => Right(value)
        case Vector() => Left(s"candidate target ${target.targetId} baselineArtifactId is not a retained source snapshot")
        case _ => Left(s"candidate target ${target.targetId} baselineArtifactId is ambiguous")
      }
      snapshotbytes <- snapshot.bytes.toRight(s"candidate target ${target.targetId} baseline source snapshot is absent")
      kind <- InternalModelSourceSnapshotFreshness.validatedSnapshotKind(snapshotbytes.toArray).left.map(reason => s"candidate target ${target.targetId} baseline source snapshot is invalid: $reason")
      _ <- Either.cond(kind == "cml-baseline", (), s"candidate target ${target.targetId} baseline source snapshot is not cml-baseline")
      artifact <- handoff.packageContext.artifacts.find(artifact => artifact.artifactId == target.baselineArtifactId && artifact.role == "source-snapshot" && artifact.present).toRight(s"candidate target ${target.targetId} baseline artifact has no present captured inventory context")
      baseline <- _cml_baseline(snapshotbytes.toArray, target.targetId)
      _ <- Either.cond(target.source == baseline.source, (), s"candidate target ${target.targetId} source does not equal its baseline source envelope")
      _ <- Either.cond(target.projectRelativePath == baseline.projectrelativepath, (), s"candidate target ${target.targetId} projectRelativePath does not equal its baseline")
      proposed <- _base64(target.proposedContent.rawBytesBase64, s"candidate target ${target.targetId} proposedContent")
    } yield InternalModelCandidateCmlTargetBytes(
      targetId = target.targetId,
      baseline = InternalModelCandidateCmlBaseline(
        artifactId = target.baselineArtifactId,
        projectRelativePath = baseline.projectrelativepath,
        source = baseline.source,
        rawBytes = baseline.rawbytes,
        snapshotSha256 = artifact.sha256
      ),
      proposedRawBytes = proposed.toVector
    )

  private def _mappings(
    target: InternalModelCandidateCmlTarget,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] =
    target.mappings.foldLeft[Either[String, Unit]](Right(())) { (result, mapping) =>
      for {
        _ <- result
        semantic <- mapping.semanticIdentityKind match {
          case "element" => realization.elements.filter(_.identity == mapping.semanticIdentity) match {
            case Vector(value) => Right((value.canonicalAssertionIds, value.enrichmentAssertionIds, value.conditionIds))
            case Vector() => Left(s"candidate mapping ${mapping.mappingId} names an unknown or cross-scope element")
            case _ => Left(s"candidate mapping ${mapping.mappingId} names an ambiguous element")
          }
          case "relationship" => realization.relationships.filter(_.identity == mapping.semanticIdentity) match {
            case Vector(value) => Right((value.canonicalAssertionIds, value.enrichmentAssertionIds, value.conditionIds))
            case Vector() => Left(s"candidate mapping ${mapping.mappingId} names an unknown or cross-scope relationship")
            case _ => Left(s"candidate mapping ${mapping.mappingId} names an ambiguous relationship")
          }
          case _ => Left(s"candidate mapping ${mapping.mappingId} semanticIdentityKind is invalid")
        }
        _ <- Either.cond(mapping.canonicalAssertionIds == semantic._1, (), s"candidate mapping ${mapping.mappingId} canonical assertion links are incomplete or inconsistent")
        _ <- Either.cond(mapping.enrichmentAssertionIds == semantic._2, (), s"candidate mapping ${mapping.mappingId} enrichment assertion links are incomplete or inconsistent")
        _ <- Either.cond(mapping.conditionIds == semantic._3, (), s"candidate mapping ${mapping.mappingId} condition links are incomplete or inconsistent")
      } yield ()
    }

  private def _effects(
    target: InternalModelCandidateCmlTarget,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    val mappingids = target.mappings.map(_.mappingId).toSet
    target.effects.foldLeft[Either[String, Unit]](Right(())) { (result, effect) =>
      for {
        _ <- result
        _ <- Either.cond(effect.mappingIds.forall(mappingids.contains), (), s"candidate effect ${effect.effectId} names a mapping outside target ${target.targetId}")
        _ <- realization.sourceReferences.filter(_.referenceId == effect.sourceReferenceId) match {
          case Vector(_) => Right(())
          case Vector() => Left(s"candidate effect ${effect.effectId} names an unknown realization source reference")
          case _ => Left(s"candidate effect ${effect.effectId} names an ambiguous realization source reference")
        }
      } yield ()
    }
  }

  private def _cml_baseline(bytes: Array[Byte], targetid: String): Either[String, SnapshotBaseline] =
    for {
      content <- _decode_utf8(bytes, s"candidate target $targetid baseline snapshot")
      json <- _json_parser.parse(content).left.map(_ => s"candidate target $targetid baseline snapshot cannot be parsed after validation")
      root <- json.asObject.toRight(s"candidate target $targetid baseline snapshot root must be an object")
      _ <- _closed_fields(root, _snapshot_fields, s"candidate target $targetid baseline snapshot root")
      sourcevalue <- root("source").toRight(s"candidate target $targetid baseline snapshot source is missing")
      source <- _source(sourcevalue, s"candidate target $targetid baseline snapshot source")
      basisvalue <- root("basis").toRight(s"candidate target $targetid baseline snapshot basis is missing")
      basis <- _object(basisvalue, s"candidate target $targetid baseline snapshot basis")
      _ <- _closed_fields(basis, _cml_basis_fields, s"candidate target $targetid baseline snapshot basis")
      path <- _string(basis, "projectRelativePath", s"candidate target $targetid baseline snapshot basis")
      _ <- Either.cond(InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(path), (), s"candidate target $targetid baseline snapshot projectRelativePath is unsafe")
      encoded <- _string(basis, "rawBytesBase64", s"candidate target $targetid baseline snapshot basis")
      rawbytes <- _base64(encoded, s"candidate target $targetid baseline snapshot basis")
    } yield SnapshotBaseline(source, path, rawbytes.toVector)

  private def _source(value: Json, label: String): Either[String, InternalModelSemanticSource] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _source_fields, label)
      authority <- _nonempty_string(objectvalue, "authority", label)
      identity <- _nonempty_string(objectvalue, "identity", label)
      locator <- _nullable_nonempty_string(objectvalue, "locator", label)
      revision <- _nullable_nonempty_string(objectvalue, "revision", label)
      sha256 <- _string(objectvalue, "sha256", label)
    } yield InternalModelSemanticSource(authority, identity, locator, revision, sha256)

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap(value => Either.cond(value.nonEmpty, value, s"$label $key must be nonempty"))

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key) match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => value.asString.filter(_.nonEmpty).map(Some(_)).toRight(s"$label $key must be null or a nonempty string")
      case None => Left(s"$label $key is missing")
    }

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")

  private def _base64(value: String, label: String): Either[String, Array[Byte]] =
    try Right(Base64.getDecoder.decode(value))
    catch {
      case NonFatal(_) => Left(s"$label rawBytesBase64 is invalid")
    }

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }
}
