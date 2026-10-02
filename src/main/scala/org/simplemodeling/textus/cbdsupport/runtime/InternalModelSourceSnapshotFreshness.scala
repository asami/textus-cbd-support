package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.{Arrays, Base64}

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser

enum InternalModelLiveSourceObservation {
  /** rawBytes is ordinary caller payload, never a comparison/control token. */
  case Observed(
    authority: String,
    identity: String,
    revision: Option[String],
    rawBytes: Vector[Byte],
    projectRelativePath: Option[String]
  )
  case Unavailable(reason: String)
  case Unauthorized(reason: String)
  case Malformed(reason: String)
  case AmbiguousOrConflicting(ambiguity: String)
}

enum InternalModelSnapshotFreshnessStatus {
  case Unchanged
  case Changed
  case Incomplete
  case Unavailable
  case Unauthorized
  case Malformed
  case AmbiguousOrConflicting
}

final case class InternalModelSnapshotFreshnessReport(
  status: InternalModelSnapshotFreshnessStatus,
  snapshotKind: Option[String],
  baselineSourceAuthority: Option[String],
  observedSourceAuthority: Option[String],
  baselineSourceIdentity: Option[String],
  observedSourceIdentity: Option[String],
  baselineSourceRevision: Option[String],
  observedSourceRevision: Option[String],
  changedDimensionNames: Vector[String],
  missingDimensionNames: Vector[String],
  baselineCmlProjectRelativePath: Option[String],
  currentCmlProjectRelativePath: Option[String],
  reason: Option[String]
)

/*
 * @since   Sep. 27, 2026
 * @version Oct. 1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Compares declared source references from one strict V2 snapshot and supplied observation. */
object InternalModelSourceSnapshotFreshness {
  private enum SnapshotKind {
    case Scenario
    case ModelContext
    case GlossaryBok
    case CmlBaseline
  }

  private final case class SourceMetadata(
    authority: String,
    identity: String,
    revision: Option[String]
  )

  private final case class ParsedSnapshot(
    kind: SnapshotKind,
    source: SourceMetadata,
    cmlpath: Option[String]
  )

  private final case class CurrentObservation(
    source: SourceMetadata,
    cmlpath: Option[String]
  )

  private final case class TraceLink(
    scenarioanchor: String,
    componentidentity: String,
    projectioncontextidentity: String,
    semanticidentitykind: String,
    semanticidentity: String
  )

  private final case class ModelFact(
    componentidentity: String,
    projectioncontextidentity: String,
    semanticidentitykind: String,
    semanticidentity: String,
    sourceanchor: String
  )

  private final case class GlossaryEntry(termidentity: String, sourceanchor: String)

  private val _top_level_fields = Set("basis", "schemaVersion", "snapshotKind", "source")
  private val _source_fields = Set("authority", "identity", "locator", "revision")
  private val _scenario_basis_fields = Set("content", "scenarioId", "traceLinks")
  private val _trace_link_fields = Set("componentIdentity", "projectionContextIdentity", "scenarioAnchor", "semanticIdentity", "semanticIdentityKind")
  private val _model_context_basis_fields = Set("contextIdentity", "facts")
  private val _model_fact_fields = Set("componentIdentity", "content", "limitations", "projectionContextIdentity", "semanticIdentity", "semanticIdentityKind", "sourceAnchor")
  private val _glossary_basis_fields = Set("entries")
  private val _glossary_entry_fields = Set("definition", "limitations", "sourceAnchor", "termIdentity", "termLabel")
  private val _cml_basis_fields = Set("byteLength", "projectRelativePath", "rawBytesBase64")
  private val _semantic_identity_kinds = Set("element", "relationship")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)

  def compare(
    snapshotBytes: Array[Byte],
    observation: InternalModelLiveSourceObservation
  ): InternalModelSnapshotFreshnessReport =
    _snapshot(snapshotBytes).fold(
      _malformed_report,
      snapshot => _compare(snapshot, observation)
    )

  private[runtime] def validatedSnapshotKind(snapshotBytes: Array[Byte]): Either[String, String] =
    _snapshot(snapshotBytes).map(snapshot => _kind_name(snapshot.kind))

  private[runtime] def isSafeCmlProjectRelativePath(value: String): Boolean =
    _is_safe_path(value)

  private def _compare(
    snapshot: ParsedSnapshot,
    observation: InternalModelLiveSourceObservation
  ): InternalModelSnapshotFreshnessReport =
    if observation == null then
      _report(
        snapshot,
        InternalModelSnapshotFreshnessStatus.Malformed,
        None,
        Vector.empty,
        Vector.empty,
        Some("live source observation is missing")
      )
    else
      observation match {
        case InternalModelLiveSourceObservation.Observed(authority, identity, revision, rawbytes, projectrelativepath) =>
          _observed(snapshot.kind, authority, identity, revision, rawbytes, projectrelativepath).fold(
            reason => _report(snapshot, InternalModelSnapshotFreshnessStatus.Malformed, None, Vector.empty, Vector.empty, Some(reason)),
            current => _compare_observed(snapshot, current)
          )
        case InternalModelLiveSourceObservation.Unavailable(reason) =>
          _observation_report(snapshot, InternalModelSnapshotFreshnessStatus.Unavailable, reason)
        case InternalModelLiveSourceObservation.Unauthorized(reason) =>
          _observation_report(snapshot, InternalModelSnapshotFreshnessStatus.Unauthorized, reason)
        case InternalModelLiveSourceObservation.Malformed(reason) =>
          _observation_report(snapshot, InternalModelSnapshotFreshnessStatus.Malformed, reason)
        case InternalModelLiveSourceObservation.AmbiguousOrConflicting(ambiguity) =>
          _observation_report(snapshot, InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting, ambiguity)
      }

  private def _snapshot(snapshotbytes: Array[Byte]): Either[String, ParsedSnapshot] =
    if snapshotbytes == null then Left("snapshot bytes are missing")
    else
      for {
        _ <- Either.cond(!_has_bom(snapshotbytes), (), "snapshot bytes must not contain a UTF-8 byte-order mark")
        content <- _decode_utf8(snapshotbytes, "snapshot bytes")
        json <- _json_parser.parse(content).left.map(_ => "snapshot bytes must be valid JSON without duplicate members")
        root <- json.asObject.toRight("snapshot root must be an object")
        _ <- _closed_fields(root, _top_level_fields, "snapshot root")
        schema <- _string(root, "schemaVersion", "snapshot root")
        _ <- Either.cond(schema == "2.0", (), "snapshot schemaVersion must be 2.0")
        kind <- _snapshot_kind(root)
        source <- _source(root)
        cmlpath <- _basis(kind, root)
      } yield ParsedSnapshot(kind, source, cmlpath)

  private def _snapshot_kind(root: JsonObject): Either[String, SnapshotKind] =
    _string(root, "snapshotKind", "snapshot root").flatMap {
      case "scenario" => Right(SnapshotKind.Scenario)
      case "model-context" => Right(SnapshotKind.ModelContext)
      case "glossary-bok" => Right(SnapshotKind.GlossaryBok)
      case "cml-baseline" => Right(SnapshotKind.CmlBaseline)
      case _ => Left("snapshotKind is not a supported V2 kind")
    }

  private def _source(root: JsonObject): Either[String, SourceMetadata] =
    for {
      value <- root("source").toRight("snapshot source is missing")
      objectvalue <- _object(value, "snapshot source")
      _ <- _closed_fields(objectvalue, _source_fields, "snapshot source")
      authority <- _nonempty_string(objectvalue, "authority", "snapshot source")
      identity <- _nonempty_string(objectvalue, "identity", "snapshot source")
      _ <- _nullable_nonempty_string(objectvalue, "locator", "snapshot source").map(_ => ())
      revision <- _nullable_nonempty_string(objectvalue, "revision", "snapshot source")
    } yield SourceMetadata(authority, identity, revision)

  private def _basis(
    kind: SnapshotKind,
    root: JsonObject
  ): Either[String, Option[String]] =
    root("basis").toRight("snapshot basis is missing").flatMap { value =>
      kind match {
        case SnapshotKind.Scenario => _scenario_basis(value).map(_ => None)
        case SnapshotKind.ModelContext => _model_context_basis(value).map(_ => None)
        case SnapshotKind.GlossaryBok => _glossary_basis(value).map(_ => None)
        case SnapshotKind.CmlBaseline => _cml_basis(value)
      }
    }

  private def _scenario_basis(value: Json): Either[String, Unit] =
    for {
      objectvalue <- _object(value, "scenario basis")
      _ <- _closed_fields(objectvalue, _scenario_basis_fields, "scenario basis")
      _ <- _nonempty_string(objectvalue, "scenarioId", "scenario basis")
      _ <- _normalized_text(objectvalue, "content", "scenario basis")
      values <- _array(objectvalue, "traceLinks", "scenario basis")
      links <- values.zipWithIndex.foldLeft[Either[String, Vector[TraceLink]]](Right(Vector.empty)) { case (result, (entry, index)) =>
        for {
          collected <- result
          link <- _trace_link(entry, index)
        } yield collected :+ link
      }
      _ <- _strictly_sorted(
        links.map(link => Vector(link.scenarioanchor, link.componentidentity, link.projectioncontextidentity, link.semanticidentitykind, link.semanticidentity)),
        "scenario traceLinks"
      )
    } yield ()

  private def _trace_link(value: Json, index: Int): Either[String, TraceLink] =
    for {
      objectvalue <- _object(value, s"scenario trace link $index")
      _ <- _closed_fields(objectvalue, _trace_link_fields, s"scenario trace link $index")
      scenarioanchor <- _nonempty_string(objectvalue, "scenarioAnchor", s"scenario trace link $index")
      componentidentity <- _nonempty_string(objectvalue, "componentIdentity", s"scenario trace link $index")
      projectioncontextidentity <- _nonempty_string(objectvalue, "projectionContextIdentity", s"scenario trace link $index")
      semanticidentitykind <- _string(objectvalue, "semanticIdentityKind", s"scenario trace link $index")
      _ <- Either.cond(_semantic_identity_kinds.contains(semanticidentitykind), (), s"scenario trace link $index semanticIdentityKind is invalid")
      semanticidentity <- _nonempty_string(objectvalue, "semanticIdentity", s"scenario trace link $index")
    } yield TraceLink(scenarioanchor, componentidentity, projectioncontextidentity, semanticidentitykind, semanticidentity)

  private def _model_context_basis(value: Json): Either[String, Unit] =
    for {
      objectvalue <- _object(value, "model-context basis")
      _ <- _closed_fields(objectvalue, _model_context_basis_fields, "model-context basis")
      _ <- _nonempty_string(objectvalue, "contextIdentity", "model-context basis")
      values <- _array(objectvalue, "facts", "model-context basis")
      _ <- Either.cond(values.nonEmpty, (), "model-context facts must be nonempty")
      facts <- values.zipWithIndex.foldLeft[Either[String, Vector[ModelFact]]](Right(Vector.empty)) { case (result, (entry, index)) =>
        for {
          collected <- result
          fact <- _model_fact(entry, index)
        } yield collected :+ fact
      }
      _ <- _strictly_sorted(
        facts.map(fact => Vector(fact.componentidentity, fact.projectioncontextidentity, fact.semanticidentitykind, fact.semanticidentity, fact.sourceanchor)),
        "model-context facts"
      )
    } yield ()

  private def _model_fact(value: Json, index: Int): Either[String, ModelFact] =
    for {
      objectvalue <- _object(value, s"model-context fact $index")
      _ <- _closed_fields(objectvalue, _model_fact_fields, s"model-context fact $index")
      componentidentity <- _nonempty_string(objectvalue, "componentIdentity", s"model-context fact $index")
      _ <- _normalized_text(objectvalue, "content", s"model-context fact $index")
      _ <- _limitations(objectvalue, s"model-context fact $index")
      projectioncontextidentity <- _nonempty_string(objectvalue, "projectionContextIdentity", s"model-context fact $index")
      semanticidentity <- _nonempty_string(objectvalue, "semanticIdentity", s"model-context fact $index")
      semanticidentitykind <- _string(objectvalue, "semanticIdentityKind", s"model-context fact $index")
      _ <- Either.cond(_semantic_identity_kinds.contains(semanticidentitykind), (), s"model-context fact $index semanticIdentityKind is invalid")
      sourceanchor <- _nonempty_string(objectvalue, "sourceAnchor", s"model-context fact $index")
    } yield ModelFact(componentidentity, projectioncontextidentity, semanticidentitykind, semanticidentity, sourceanchor)

  private def _glossary_basis(value: Json): Either[String, Unit] =
    for {
      objectvalue <- _object(value, "glossary-bok basis")
      _ <- _closed_fields(objectvalue, _glossary_basis_fields, "glossary-bok basis")
      values <- _array(objectvalue, "entries", "glossary-bok basis")
      _ <- Either.cond(values.nonEmpty, (), "glossary-bok entries must be nonempty")
      entries <- values.zipWithIndex.foldLeft[Either[String, Vector[GlossaryEntry]]](Right(Vector.empty)) { case (result, (entry, index)) =>
        for {
          collected <- result
          glossaryentry <- _glossary_entry(entry, index)
        } yield collected :+ glossaryentry
      }
      _ <- _strictly_sorted(entries.map(entry => Vector(entry.termidentity, entry.sourceanchor)), "glossary-bok entries")
    } yield ()

  private def _glossary_entry(value: Json, index: Int): Either[String, GlossaryEntry] =
    for {
      objectvalue <- _object(value, s"glossary-bok entry $index")
      _ <- _closed_fields(objectvalue, _glossary_entry_fields, s"glossary-bok entry $index")
      _ <- _normalized_text(objectvalue, "definition", s"glossary-bok entry $index")
      _ <- _limitations(objectvalue, s"glossary-bok entry $index")
      sourceanchor <- _nonempty_string(objectvalue, "sourceAnchor", s"glossary-bok entry $index")
      termidentity <- _nonempty_string(objectvalue, "termIdentity", s"glossary-bok entry $index")
      _ <- _nonempty_string(objectvalue, "termLabel", s"glossary-bok entry $index")
    } yield GlossaryEntry(termidentity, sourceanchor)

  private def _cml_basis(value: Json): Either[String, Option[String]] =
    for {
      objectvalue <- _object(value, "cml-baseline basis")
      _ <- _closed_fields(objectvalue, _cml_basis_fields, "cml-baseline basis")
      bytelength <- _nonnegative_integer(objectvalue, "byteLength", "cml-baseline basis")
      path <- _string(objectvalue, "projectRelativePath", "cml-baseline basis")
      _ <- Either.cond(_is_safe_path(path), (), "cml-baseline projectRelativePath is unsafe")
      encoded <- _string(objectvalue, "rawBytesBase64", "cml-baseline basis")
      rawbytes <- _decode_base64(encoded)
      _ <- Either.cond(_canonical_base64(rawbytes) == encoded, (), "cml-baseline rawBytesBase64 is not canonical RFC 4648 padded Base64")
      _ <- Either.cond(bytelength == rawbytes.length.toLong, (), "cml-baseline byteLength does not match raw bytes")
    } yield Some(path)

  private def _limitations(objectvalue: JsonObject, label: String): Either[String, Unit] =
    for {
      values <- _array(objectvalue, "limitations", label)
      limitations <- values.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          limitation <- _nonempty_string_value(value, s"$label limitation $index")
        } yield collected :+ limitation
      }
      _ <- _strictly_sorted(limitations.map(value => Vector(value)), s"$label limitations")
    } yield ()

  private def _observed(
    kind: SnapshotKind,
    authority: String,
    identity: String,
    revision: Option[String],
    rawbytes: Vector[Byte],
    projectrelativepath: Option[String]
  ): Either[String, CurrentObservation] =
    if revision == null || rawbytes == null || projectrelativepath == null then Left("observed source contains a missing value")
    else
      for {
        sourceauthority <- _nonempty_value(authority, "observed source authority")
        sourceidentity <- _nonempty_value(identity, "observed source identity")
        sourcerevision <- _optional_nonempty_value(revision, "observed source revision")
        cmlpath <- _observed_cml_path(kind, projectrelativepath)
      } yield CurrentObservation(
        SourceMetadata(sourceauthority, sourceidentity, sourcerevision),
        cmlpath
      )

  private def _observed_cml_path(kind: SnapshotKind, path: Option[String]): Either[String, Option[String]] =
    kind match {
      case SnapshotKind.CmlBaseline =>
        path match {
          case Some(value) if _is_safe_path(value) => Right(Some(value))
          case _ => Left("CML observation requires one safe project-relative path")
        }
      case _ =>
        Either.cond(path.isEmpty, Option.empty[String], "non-CML observation must not claim a CML target path")
    }

  private def _compare_observed(snapshot: ParsedSnapshot, current: CurrentObservation): InternalModelSnapshotFreshnessReport = {
    val sourcechanges = Vector(
      Option.when(snapshot.source.authority != current.source.authority)("source.authority"),
      Option.when(snapshot.source.identity != current.source.identity)("source.identity"),
      Option.when(snapshot.source.revision != current.source.revision)("source.revision")
    ).flatten
    val cmlchanges = snapshot.kind match {
      case SnapshotKind.CmlBaseline =>
        Vector(Option.when(snapshot.cmlpath != current.cmlpath)("basis.projectRelativePath")).flatten
      case _ => Vector.empty
    }
    val changes = (sourcechanges ++ cmlchanges).distinct.sorted
    val missing = Vector(
      Option.when(snapshot.source.revision.isEmpty)("baseline.source.revision"),
      Option.when(current.source.revision.isEmpty)("observed.source.revision")
    ).flatten.distinct.sorted
    val status = if (missing.nonEmpty) {
      InternalModelSnapshotFreshnessStatus.Incomplete
    } else if (changes.nonEmpty) {
      InternalModelSnapshotFreshnessStatus.Changed
    } else {
      InternalModelSnapshotFreshnessStatus.Unchanged
    }
    val reason = if (missing.nonEmpty) {
      Some("missing source-owned revision: " + missing.mkString(", "))
    } else {
      Option.when(changes.nonEmpty)("declared source references differ: " + changes.mkString(", "))
    }
    _report(snapshot, status, Some(current), changes, missing, reason)
  }

  private def _observation_report(
    snapshot: ParsedSnapshot,
    status: InternalModelSnapshotFreshnessStatus,
    evidence: String
  ): InternalModelSnapshotFreshnessReport =
    _nonempty_value(evidence, "live observation evidence").fold(
      reason => _report(snapshot, InternalModelSnapshotFreshnessStatus.Malformed, None, Vector.empty, Vector.empty, Some(reason)),
      reason => _report(snapshot, status, None, Vector.empty, Vector.empty, Some(reason))
    )

  private def _report(
    snapshot: ParsedSnapshot,
    status: InternalModelSnapshotFreshnessStatus,
    current: Option[CurrentObservation],
    changes: Vector[String],
    missing: Vector[String],
    evidence: Option[String]
  ): InternalModelSnapshotFreshnessReport =
    InternalModelSnapshotFreshnessReport(
      status = status,
      snapshotKind = Some(_kind_name(snapshot.kind)),
      baselineSourceAuthority = Some(snapshot.source.authority),
      observedSourceAuthority = current.map(_.source.authority),
      baselineSourceIdentity = Some(snapshot.source.identity),
      observedSourceIdentity = current.map(_.source.identity),
      baselineSourceRevision = snapshot.source.revision,
      observedSourceRevision = current.flatMap(_.source.revision),
      changedDimensionNames = changes.distinct.sorted,
      missingDimensionNames = missing.distinct.sorted,
      baselineCmlProjectRelativePath = snapshot.cmlpath,
      currentCmlProjectRelativePath = current.flatMap(_.cmlpath),
      reason = evidence
    )

  private def _malformed_report(reason: String): InternalModelSnapshotFreshnessReport =
    InternalModelSnapshotFreshnessReport(
      status = InternalModelSnapshotFreshnessStatus.Malformed,
      snapshotKind = None,
      baselineSourceAuthority = None,
      observedSourceAuthority = None,
      baselineSourceIdentity = None,
      observedSourceIdentity = None,
      baselineSourceRevision = None,
      observedSourceRevision = None,
      changedDimensionNames = Vector.empty,
      missingDimensionNames = Vector.empty,
      baselineCmlProjectRelativePath = None,
      currentCmlProjectRelativePath = None,
      reason = Some(reason)
    )

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap(value => _nonempty_value(value, s"$label $key"))

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key) match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => _nonempty_string_value(value, s"$label $key").map(Some(_))
      case None => Left(s"$label $key is missing")
    }

  private def _nonempty_string_value(value: Json, label: String): Either[String, String] =
    value.asString.toRight(s"$label must be a JSON string").flatMap(stringvalue => _nonempty_value(stringvalue, label))

  private def _nonempty_value(value: String, label: String): Either[String, String] =
    Option(value).filter(_.nonEmpty).toRight(s"$label must be nonempty")

  private def _optional_nonempty_value(value: Option[String], label: String): Either[String, Option[String]] =
    value match {
      case None => Right(None)
      case Some(entry) => _nonempty_value(entry, label).map(Some(_))
    }

  private def _normalized_text(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _nonempty_string(objectvalue, key, label).flatMap { value =>
      Either.cond(!value.startsWith("\ufeff") && !value.contains('\r'), value, s"$label $key is not normalized text")
    }

  private def _nonnegative_integer(objectvalue: JsonObject, key: String, label: String): Either[String, Long] =
    objectvalue(key).flatMap(_.asNumber).toRight(s"$label $key must be a JSON integer").flatMap { number =>
      number.toLong.filter(value => value >= 0 && number.toString == value.toString).toRight(s"$label $key must be a nonnegative canonical JSON integer")
    }

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not the closed V2 schema")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] = {
    val ordered = values.zip(values.drop(1)).forall { case (left, right) => _compare_tuple(left, right) < 0 }
    Either.cond(ordered, (), s"$label must be unique and sorted by ascending UTF-8-byte order")
  }

  private def _compare_tuple(left: Vector[String], right: Vector[String]): Int =
    left.zip(right).iterator.map { case (leftvalue, rightvalue) => _compare_text(leftvalue, rightvalue) }.find(_ != 0).getOrElse(left.length.compare(right.length))

  private def _compare_text(left: String, right: String): Int =
    Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))

  private def _decode_base64(value: String): Either[String, Array[Byte]] =
    try Right(Base64.getDecoder.decode(value))
    catch {
      case NonFatal(_) => Left("cml-baseline rawBytesBase64 is invalid")
    }

  private def _canonical_base64(bytes: Array[Byte]): String =
    Base64.getEncoder.encodeToString(bytes)

  private def _is_safe_path(value: String): Boolean =
    Option(value).exists { path =>
      path.nonEmpty && !path.startsWith("/") && !path.contains('\\') &&
        path.split("/", -1).forall { segment =>
          segment.nonEmpty && segment != "." && segment != ".." &&
            !segment.exists(character => Character.isWhitespace(character) || Character.isSpaceChar(character) || Character.isISOControl(character))
        }
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

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _kind_name(kind: SnapshotKind): String =
    kind match {
      case SnapshotKind.Scenario => "scenario"
      case SnapshotKind.ModelContext => "model-context"
      case SnapshotKind.GlossaryBok => "glossary-bok"
      case SnapshotKind.CmlBaseline => "cml-baseline"
    }
}
