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
 * @version Oct.  1, 2026
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
  private val _source_fields = Set("authority", "identity", "locator", "revision")
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
      _capture(handoff).fold(Consequence.operationInvalid, _ =>
        InternalModelProjectionContinuityValidator.validateVerified(handoff.continuitypackage).flatMap { continuity =>
          _admission(handoff, continuity).fold(Consequence.operationInvalid, Consequence.success)
        })
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
    } yield InternalModelCandidateCmlAdmission(
      projection = candidate,
      continuity = continuity,
      packageContext = handoff.packagecontext,
      candidateArtifactReference = handoff.candidate.reference,
      candidatePackageRelativePath = handoff.candidate.path,
      targetBytes = bytes
    )

  private def _candidate_binding(
    candidate: InternalModelCandidateCmlProjection,
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage,
    continuity: InternalModelProjectionContinuity
  ): Either[String, Unit] = {
    val realizationreference = handoff.continuitypackage.realizationpackage.realization.reference
    val continuityreference = handoff.continuitypackage.projection.reference
    val expecteddependencies = (Vector(realizationreference, continuityreference) ++ candidate.targets.map(_.baselineArtifactReference)).sortBy(_.artifactId.value)
    for {
      _ <- Either.cond(candidate.realizationArtifactReference == realizationreference, (), "candidate realizationArtifactReference is not the exact selected realization artifact")
      _ <- Either.cond(candidate.continuityArtifactReference == continuityreference, (), "candidate continuityArtifactReference is not the exact selected continuity artifact")
      _ <- Either.cond(candidate.scope == continuity.realization.scope, (), "candidate scope does not equal selected realization scope")
      _ <- Either.cond(handoff.candidate.dependencies == expecteddependencies, (), "candidate artifact dependencies do not equal realization, continuity, and selected baseline artifact references")
    } yield ()
  }

  private def _target_bytes(
    target: InternalModelCandidateCmlTarget,
    handoff: InternalModelVerifiedCandidateCmlProjectionPackage
  ): Either[String, InternalModelCandidateCmlTargetBytes] =
    for {
      snapshot <- handoff.continuitypackage.realizationpackage.sourcesnapshots.filter(_.reference == target.baselineArtifactReference) match {
        case Vector(value) => Right(value)
        case Vector() => Left(s"candidate target ${target.targetId} baselineArtifactId is not a retained source snapshot")
        case _ => Left(s"candidate target ${target.targetId} baselineArtifactId is ambiguous")
      }
      snapshotbytes <- snapshot.bytes.toRight(s"candidate target ${target.targetId} baseline source snapshot is absent")
      kind <- InternalModelSourceSnapshotFreshness.validatedSnapshotKind(snapshotbytes.toArray).left.map(reason => s"candidate target ${target.targetId} baseline source snapshot is invalid: $reason")
      _ <- Either.cond(kind == "cml-baseline", (), s"candidate target ${target.targetId} baseline source snapshot is not cml-baseline")
      baseline <- _cml_baseline(snapshotbytes.toArray, target.targetId)
      _ <- Either.cond(target.source == baseline.source, (), s"candidate target ${target.targetId} source does not equal its baseline source envelope")
      _ <- Either.cond(target.projectRelativePath == baseline.projectrelativepath, (), s"candidate target ${target.targetId} projectRelativePath does not equal its baseline")
      proposed <- _base64(target.proposedContent.rawBytesBase64, s"candidate target ${target.targetId} proposedContent")
    } yield InternalModelCandidateCmlTargetBytes(
      targetId = target.targetId,
      baseline = InternalModelCandidateCmlBaseline(
        reference = target.baselineArtifactReference,
        projectRelativePath = baseline.projectrelativepath,
        source = baseline.source,
        rawBytes = baseline.rawbytes
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
    } yield InternalModelSemanticSource(authority, identity, locator, revision)

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap(value => Either.cond(!value.isBlank && StandardCharsets.UTF_8.newEncoder().canEncode(value), value, s"$label $key must be nonblank valid Unicode"))

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key) match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => _nonempty_string(objectvalue, key, label).map(Some(_))
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

  private def _capture(handoff: InternalModelVerifiedCandidateCmlProjectionPackage): Either[String, Unit] =
    for {
      _ <- Either.cond(handoff != null && handoff.packagecontext != null && handoff.continuitypackage != null && handoff.candidate != null, (), "candidate capture, package context, continuity and candidate must be present")
      context = handoff.packagecontext
      _ <- Either.cond(context.reference != null && context.artifacts != null, (), "captured package reference and inventory must be present")
      _ <- InternalModelPackageId.from(context.reference.packageId.value)
      _ <- InternalModelProjectToken.from(context.reference.projectNamespace.value)
      _ <- InternalModelProjectToken.from(context.reference.projectId.value)
      _ <- Either.cond(context.schemaversion == "2.0" && context.revision > 0, (), "captured package schema and revision must be current and valid")
      _ <- InternalModelProjectToken.from(context.lifecyclestate)
      _ <- context.artifacts.foldLeft[Either[String, Unit]](Right(())) { (result, entry) =>
        for {
          _ <- result
          _ <- Either.cond(entry != null, (), "captured inventory entry must be present")
          _ <- InternalModelCandidateCmlProjectionCodec.capturedMetadata(entry.reference, entry.path, entry.dependencies)
          _ <- Either.cond(!entry.required || entry.present, (), "required captured inventory entry is absent")
        } yield ()
      }
      _ <- Either.cond(context.artifacts.map(_.reference.artifactId).distinct.size == context.artifacts.size && context.artifacts.map(_.path).distinct.size == context.artifacts.size, (), "captured inventory IDs and paths must be unique")
      _ <- _inventory(context.artifacts)
      continuity = handoff.continuitypackage
      _ <- Either.cond(continuity.projection != null && continuity.realizationpackage != null && continuity.realizationpackage.realization != null && continuity.realizationpackage.sourcesnapshots != null, (), "captured continuity, realization and source collection must be present")
      _ <- _projection_capture(handoff.candidate, context)
      _ <- _projection_capture(continuity.projection, context)
      realization = continuity.realizationpackage.realization
      _ <- InternalModelCandidateCmlProjectionCodec.capturedMetadata(realization.reference, realization.path, realization.dependencies)
      _ <- Either.cond(realization.reference.role == InternalModelArtifactRole.Realization && realization.bytes != null, (), "captured realization role and bytes must be valid")
      _ <- _context_entry(context, realization.reference, realization.path, realization.required, realization.dependencies, true)
      snapshots = continuity.realizationpackage.sourcesnapshots
      _ <- snapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        for {
          _ <- result
          _ <- Either.cond(snapshot != null, (), "captured source entry must be present")
          _ <- InternalModelCandidateCmlProjectionCodec.capturedMetadata(snapshot.reference, snapshot.path, snapshot.dependencies)
          _ <- Either.cond(snapshot.reference.role == InternalModelArtifactRole.SourceSnapshot && snapshot.bytes != null && snapshot.bytes.forall(_ != null), (), "captured source role and bytes Option must be valid")
          _ <- _context_entry(context, snapshot.reference, snapshot.path, snapshot.required, snapshot.dependencies, snapshot.bytes.isDefined)
        } yield ()
      }
      _ <- Either.cond(snapshots.map(_.reference.artifactId).distinct.size == snapshots.size, (), "captured source IDs must be unique")
    } yield ()

  private def _projection_capture(projection: InternalModelVerifiedProjection, context: InternalModelVerifiedPackageContext): Either[String, Unit] =
    for {
      _ <- InternalModelCandidateCmlProjectionCodec.capturedMetadata(projection.reference, projection.path, projection.dependencies)
      _ <- Either.cond(projection.reference.role == InternalModelArtifactRole.Projection && projection.bytes != null, (), "captured projection role and bytes must be valid")
      _ <- _context_entry(context, projection.reference, projection.path, projection.required, projection.dependencies, true)
    } yield ()

  private def _context_entry(context: InternalModelVerifiedPackageContext, reference: InternalModelArtifactReference, path: String, required: Boolean, dependencies: Vector[InternalModelArtifactReference], present: Boolean): Either[String, Unit] =
    context.artifacts.filter(_.reference.artifactId == reference.artifactId) match {
      case Vector(entry) => Either.cond(entry.reference == reference && entry.path == path && entry.required == required && entry.dependencies == dependencies && entry.present == present, (), "selected captured artifact does not equal its exact inventory entry")
      case _ => Left("selected captured artifact must resolve to exactly one inventory entry")
    }

  private def _inventory(entries: Vector[InternalModelVerifiedArtifactContext]): Either[String, Unit] = {
    val byid = entries.map(entry => entry.reference.artifactId -> entry).toMap
    entries.foldLeft[Either[String, Vector[InternalModelArtifactReference]]](Right(Vector.empty)) { (result, entry) =>
      for {
        preceding <- result
        _ <- Either.cond(entry.dependencies.forall(reference => byid.get(reference.artifactId).exists(value => value.reference == reference && (!entry.present || value.present)) && preceding.contains(reference)), (), "captured dependencies must resolve exact present inventory references in topological order")
        ready = entries.filter(value => !preceding.contains(value.reference) && value.dependencies.forall(preceding.contains)).sortBy(_.reference.artifactId.value)
        _ <- Either.cond(ready.headOption.exists(_.reference == entry.reference), (), "captured inventory must retain deterministic topological artifact ID order")
      } yield preceding :+ entry.reference
    }.map(_ => ())
  }
}
