package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
/** Parses and emits the closed decision-record V1 bytes without selecting a current decision. */
private[runtime] object InternalModelDecisionRecordCodec {
  private val _root_fields = Set("ledgerIdentity", "profile", "records", "schemaVersion", "scope")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _record_fields = Set(
    "actor", "affectedTargets", "assumptions", "basis", "conditions", "consideredEvidence", "decisionIdentity",
    "limitations", "provenance", "rationale", "realizationConditionIds", "rejectedAlternatives", "selectedChoice",
    "state", "supersedes", "topicIdentity"
  )
  private val _actor_fields = Set("identity", "kind", "role")
  private val _source_fields = Set("authority", "identity", "locator", "revision", "sha256")
  private val _choice_fields = Set("choiceIdentity", "description")
  private val _target_fields = Set("semanticIdentity", "semanticIdentityKind")
  private val _evidence_fields = Set("conditionIds", "conditions", "evidenceIdentity", "kind", "limitations", "source", "sourceReferenceId")
  private val _alternative_fields = Set("alternativeIdentity", "description", "rejectionRationale")
  private val _basis_fields = Set("realizationArtifactId", "realizationIdentity", "scope", "sha256", "status")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(decision: InternalModelVerifiedDecision): Either[String, InternalModelDecisionLedger] =
    for {
      _ <- Either.cond(decision.role == "decision", (), "selected artifact role must be decision")
      bytes = decision.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "decision record bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "decision record bytes")
      json <- _json_parser.parse(content).left.map(_ => "decision record bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("decision record root must be an object")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(bytes, canonical), (), "decision record bytes are not canonical JSON")
      _ <- _closed_fields(root, _root_fields, "decision record root")
      profile <- _nonblank_string(root, "profile", "decision record root")
      schema <- _nonblank_string(root, "schemaVersion", "decision record root")
      _ <- Either.cond(profile == "ccdm-decision-records-v1" && schema == "1.0", (), "decision record profile and schemaVersion are unsupported")
      ledgeridentity <- _nonblank_string(root, "ledgerIdentity", "decision record root")
      scope <- _scope(root, "decision record")
      records <- _records(root)
      candidate = InternalModelDecisionLedger(profile, schema, ledgeridentity, scope, records, Vector.empty)
      encoded = encode(candidate)
      _ <- Either.cond(encoded.toVector == canonical.toVector, (), "typed decision record re-encoding does not match supplied canonical bytes")
    } yield candidate.copy(canonicalBytes = encoded.toVector)

  def encode(ledger: InternalModelDecisionLedger): Array[Byte] =
    _canonical_bytes(Json.obj(
      "ledgerIdentity" -> Json.fromString(ledger.ledgerIdentity),
      "profile" -> Json.fromString(ledger.profile),
      "records" -> Json.fromValues(_sort_records(ledger.records).map(_record_json)),
      "schemaVersion" -> Json.fromString(ledger.schemaVersion),
      "scope" -> _scope_json(ledger.scope)
    ))

  private def _records(root: JsonObject): Either[String, Vector[InternalModelDecisionRecord]] =
    for {
      values <- _array(root, "records", "decision record root")
      records <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelDecisionRecord]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          record <- _record(value, index)
        } yield collected :+ record
      }
      _ <- Either.cond(records.nonEmpty, (), "decision record root records must be nonempty")
      _ <- _strictly_sorted(records.map(record => Vector(record.decisionIdentity)), "decision records")
    } yield records

  private def _record(value: Json, index: Int): Either[String, InternalModelDecisionRecord] =
    for {
      objectvalue <- _object(value, s"decision record $index")
      _ <- _closed_fields(objectvalue, _record_fields, s"decision record $index")
      decisionidentity <- _nonblank_string(objectvalue, "decisionIdentity", s"decision record $index")
      topicidentity <- _nonblank_string(objectvalue, "topicIdentity", s"decision record $decisionidentity")
      state <- _nonblank_string(objectvalue, "state", s"decision record $decisionidentity")
      _ <- Either.cond(Set("accepted", "superseded").contains(state), (), s"decision record $decisionidentity state is invalid")
      actorvalue <- objectvalue("actor").toRight(s"decision record $decisionidentity actor is missing")
      actor <- _actor(actorvalue, s"decision record $decisionidentity actor")
      provenancevalue <- objectvalue("provenance").toRight(s"decision record $decisionidentity provenance is missing")
      provenance <- _source(provenancevalue, s"decision record $decisionidentity provenance", Some("human-decision"))
      choicevalue <- objectvalue("selectedChoice").toRight(s"decision record $decisionidentity selectedChoice is missing")
      choice <- _choice(choicevalue, s"decision record $decisionidentity selectedChoice")
      rationale <- _nonblank_string(objectvalue, "rationale", s"decision record $decisionidentity")
      targets <- _affected_targets(objectvalue, decisionidentity)
      evidence <- _evidence(objectvalue, decisionidentity)
      assumptions <- _prose(objectvalue, "assumptions", s"decision record $decisionidentity")
      conditions <- _prose(objectvalue, "conditions", s"decision record $decisionidentity")
      limitations <- _prose(objectvalue, "limitations", s"decision record $decisionidentity")
      conditionids <- _sorted_strings(objectvalue, "realizationConditionIds", s"decision record $decisionidentity")
      alternatives <- _alternatives(objectvalue, decisionidentity)
      _ <- Either.cond(!alternatives.exists(_.alternativeIdentity == choice.choiceIdentity), (), s"decision record $decisionidentity selected choice identity collides with a rejected alternative")
      basisvalue <- objectvalue("basis").toRight(s"decision record $decisionidentity basis is missing")
      basis <- _basis(basisvalue, s"decision record $decisionidentity basis")
      supersedes <- _nullable_nonblank_string(objectvalue, "supersedes", s"decision record $decisionidentity")
    } yield InternalModelDecisionRecord(
      decisionidentity, topicidentity, state, actor, provenance, choice, rationale, targets, evidence,
      assumptions, conditions, limitations, conditionids, alternatives, basis, supersedes
    )

  private def _actor(value: Json, label: String): Either[String, InternalModelDecisionActor] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _actor_fields, label)
      kind <- _nonblank_string(objectvalue, "kind", label)
      _ <- Either.cond(kind == "human", (), s"$label kind must be human")
      identity <- _nonblank_string(objectvalue, "identity", label)
      role <- _nonblank_string(objectvalue, "role", label)
    } yield InternalModelDecisionActor(kind, identity, role)

  private def _choice(value: Json, label: String): Either[String, InternalModelDecisionChoice] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _choice_fields, label)
      identity <- _nonblank_string(objectvalue, "choiceIdentity", label)
      description <- _nonblank_string(objectvalue, "description", label)
    } yield InternalModelDecisionChoice(identity, description)

  private def _affected_targets(objectvalue: JsonObject, decisionidentity: String): Either[String, Vector[InternalModelSemanticTarget]] =
    for {
      values <- _array(objectvalue, "affectedTargets", s"decision record $decisionidentity")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          target <- _target(value, s"decision record $decisionidentity affected target $index")
        } yield collected :+ target
      }
      _ <- Either.cond(targets.nonEmpty, (), s"decision record $decisionidentity affectedTargets must be nonempty")
      _ <- _strictly_sorted(targets.map(target => Vector(target.semanticIdentityKind, target.semanticIdentity)), s"decision record $decisionidentity affectedTargets")
    } yield targets

  private def _target(value: Json, label: String): Either[String, InternalModelSemanticTarget] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _target_fields, label)
      kind <- _nonblank_string(objectvalue, "semanticIdentityKind", label)
      _ <- Either.cond(Set("element", "relationship").contains(kind), (), s"$label semanticIdentityKind is invalid")
      identity <- _nonblank_string(objectvalue, "semanticIdentity", label)
    } yield InternalModelSemanticTarget(kind, identity)

  private def _evidence(objectvalue: JsonObject, decisionidentity: String): Either[String, Vector[InternalModelDecisionEvidence]] =
    for {
      values <- _array(objectvalue, "consideredEvidence", s"decision record $decisionidentity")
      evidence <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelDecisionEvidence]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          entry <- _evidence_entry(value, decisionidentity, index)
        } yield collected :+ entry
      }
      _ <- _strictly_sorted(evidence.map(entry => Vector(entry.evidenceIdentity)), s"decision record $decisionidentity consideredEvidence")
    } yield evidence

  private def _evidence_entry(value: Json, decisionidentity: String, index: Int): Either[String, InternalModelDecisionEvidence] =
    for {
      objectvalue <- _object(value, s"decision record $decisionidentity evidence $index")
      _ <- _closed_fields(objectvalue, _evidence_fields, s"decision record $decisionidentity evidence $index")
      identity <- _nonblank_string(objectvalue, "evidenceIdentity", s"decision record $decisionidentity evidence $index")
      kind <- _nonblank_string(objectvalue, "kind", s"decision record $decisionidentity evidence $identity")
      _ <- Either.cond(Set("realization-source", "external-human", "provider-proposal", "external-other").contains(kind), (), s"decision record $decisionidentity evidence $identity kind is invalid")
      sourcevalue <- objectvalue("source").toRight(s"decision record $decisionidentity evidence $identity source is missing")
      source <- _source(sourcevalue, s"decision record $decisionidentity evidence $identity source", None)
      referenceid <- _nullable_nonblank_string(objectvalue, "sourceReferenceId", s"decision record $decisionidentity evidence $identity")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"decision record $decisionidentity evidence $identity")
      _ <- kind match {
        case "realization-source" => Either.cond(referenceid.nonEmpty, (), s"decision record $decisionidentity evidence $identity realization-source needs sourceReferenceId")
        case _ => Either.cond(referenceid.isEmpty && conditionids.isEmpty, (), s"decision record $decisionidentity evidence $identity external evidence must not link realization conditions")
      }
      conditions <- _prose(objectvalue, "conditions", s"decision record $decisionidentity evidence $identity")
      limitations <- _prose(objectvalue, "limitations", s"decision record $decisionidentity evidence $identity")
    } yield InternalModelDecisionEvidence(identity, kind, source, referenceid, conditionids, conditions, limitations)

  private def _alternatives(objectvalue: JsonObject, decisionidentity: String): Either[String, Vector[InternalModelDecisionAlternative]] =
    for {
      values <- _array(objectvalue, "rejectedAlternatives", s"decision record $decisionidentity")
      alternatives <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelDecisionAlternative]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          alternative <- _alternative(value, s"decision record $decisionidentity alternative $index")
        } yield collected :+ alternative
      }
      _ <- _strictly_sorted(alternatives.map(alternative => Vector(alternative.alternativeIdentity)), s"decision record $decisionidentity rejectedAlternatives")
    } yield alternatives

  private def _alternative(value: Json, label: String): Either[String, InternalModelDecisionAlternative] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _alternative_fields, label)
      identity <- _nonblank_string(objectvalue, "alternativeIdentity", label)
      description <- _nonblank_string(objectvalue, "description", label)
      rationale <- _nonblank_string(objectvalue, "rejectionRationale", label)
    } yield InternalModelDecisionAlternative(identity, description, rationale)

  private def _basis(value: Json, label: String): Either[String, InternalModelDecisionBasis] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _basis_fields, label)
      artifactid <- _nonblank_string(objectvalue, "realizationArtifactId", label)
      identity <- _nonblank_string(objectvalue, "realizationIdentity", label)
      sha256 <- _nonblank_string(objectvalue, "sha256", label)
      _ <- Either.cond(_digest_pattern.matches(sha256), (), s"$label sha256 is invalid")
      scope <- _scope(objectvalue, label)
      status <- _nonblank_string(objectvalue, "status", label)
      _ <- Either.cond(Set("current", "historical-unverified").contains(status), (), s"$label status is invalid")
    } yield InternalModelDecisionBasis(artifactid, identity, sha256, scope, status)

  private def _scope(objectvalue: JsonObject, label: String): Either[String, InternalModelSemanticScope] =
    for {
      value <- objectvalue("scope").toRight(s"$label scope is missing")
      scope <- _scope_value(value, s"$label scope")
    } yield scope

  private def _scope_value(value: Json, label: String): Either[String, InternalModelSemanticScope] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _scope_fields, label)
      componentidentity <- _nonblank_string(objectvalue, "componentIdentity", label)
      contextidentity <- _nonblank_string(objectvalue, "projectionContextIdentity", label)
      usecaseidentity <- _nonblank_string(objectvalue, "selectedUseCaseElementIdentity", label)
    } yield InternalModelSemanticScope(componentidentity, contextidentity, usecaseidentity)

  private def _source(value: Json, label: String, expectedauthority: Option[String]): Either[String, InternalModelSemanticSource] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _source_fields, label)
      authority <- _nonblank_string(objectvalue, "authority", label)
      _ <- expectedauthority.fold[Either[String, Unit]](Right(()))(expected => Either.cond(authority == expected, (), s"$label authority is invalid"))
      identity <- _nonblank_string(objectvalue, "identity", label)
      locator <- _nullable_nonblank_string(objectvalue, "locator", label)
      revision <- _nullable_nonblank_string(objectvalue, "revision", label)
      sha256 <- _nonblank_string(objectvalue, "sha256", label)
      _ <- Either.cond(_digest_pattern.matches(sha256), (), s"$label sha256 is invalid")
    } yield InternalModelSemanticSource(authority, identity, locator, revision, sha256)

  private def _prose(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    for {
      values <- _array(objectvalue, key, label)
      prose <- values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          text <- value.asString.filter(_nonblank).toRight(s"$label $key must contain only nonblank Unicode strings")
        } yield collected :+ text
      }
    } yield prose

  private def _sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    for {
      values <- _array(objectvalue, key, label)
      strings <- values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          text <- value.asString.filter(_nonblank).toRight(s"$label $key must contain only nonblank Unicode strings")
        } yield collected :+ text
      }
      _ <- _strictly_sorted(strings.map(Vector(_)), s"$label $key")
    } yield strings

  private def _record_json(record: InternalModelDecisionRecord): Json =
    Json.obj(
      "actor" -> _actor_json(record.actor),
      "affectedTargets" -> Json.fromValues(_sort_targets(record.affectedTargets).map(_target_json)),
      "assumptions" -> Json.fromValues(record.assumptions.map(Json.fromString)),
      "basis" -> _basis_json(record.basis),
      "conditions" -> Json.fromValues(record.conditions.map(Json.fromString)),
      "consideredEvidence" -> Json.fromValues(_sort_evidence(record.consideredEvidence).map(_evidence_json)),
      "decisionIdentity" -> Json.fromString(record.decisionIdentity),
      "limitations" -> Json.fromValues(record.limitations.map(Json.fromString)),
      "provenance" -> _source_json(record.provenance),
      "rationale" -> Json.fromString(record.rationale),
      "realizationConditionIds" -> Json.fromValues(_sort_strings(record.realizationConditionIds).map(Json.fromString)),
      "rejectedAlternatives" -> Json.fromValues(_sort_alternatives(record.rejectedAlternatives).map(_alternative_json)),
      "selectedChoice" -> _choice_json(record.selectedChoice),
      "state" -> Json.fromString(record.state),
      "supersedes" -> record.supersedes.map(Json.fromString).getOrElse(Json.Null),
      "topicIdentity" -> Json.fromString(record.topicIdentity)
    )

  private def _actor_json(actor: InternalModelDecisionActor): Json =
    Json.obj(
      "identity" -> Json.fromString(actor.identity),
      "kind" -> Json.fromString(actor.kind),
      "role" -> Json.fromString(actor.role)
    )

  private def _choice_json(choice: InternalModelDecisionChoice): Json =
    Json.obj(
      "choiceIdentity" -> Json.fromString(choice.choiceIdentity),
      "description" -> Json.fromString(choice.description)
    )

  private def _target_json(target: InternalModelSemanticTarget): Json =
    Json.obj(
      "semanticIdentity" -> Json.fromString(target.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(target.semanticIdentityKind)
    )

  private def _evidence_json(evidence: InternalModelDecisionEvidence): Json =
    Json.obj(
      "conditionIds" -> Json.fromValues(_sort_strings(evidence.conditionIds).map(Json.fromString)),
      "conditions" -> Json.fromValues(evidence.conditions.map(Json.fromString)),
      "evidenceIdentity" -> Json.fromString(evidence.evidenceIdentity),
      "kind" -> Json.fromString(evidence.kind),
      "limitations" -> Json.fromValues(evidence.limitations.map(Json.fromString)),
      "source" -> _source_json(evidence.source),
      "sourceReferenceId" -> evidence.sourceReferenceId.map(Json.fromString).getOrElse(Json.Null)
    )

  private def _alternative_json(alternative: InternalModelDecisionAlternative): Json =
    Json.obj(
      "alternativeIdentity" -> Json.fromString(alternative.alternativeIdentity),
      "description" -> Json.fromString(alternative.description),
      "rejectionRationale" -> Json.fromString(alternative.rejectionRationale)
    )

  private def _basis_json(basis: InternalModelDecisionBasis): Json =
    Json.obj(
      "realizationArtifactId" -> Json.fromString(basis.realizationArtifactId),
      "realizationIdentity" -> Json.fromString(basis.realizationIdentity),
      "scope" -> _scope_json(basis.scope),
      "sha256" -> Json.fromString(basis.sha256),
      "status" -> Json.fromString(basis.status)
    )

  private def _scope_json(scope: InternalModelSemanticScope): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString(scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity)
    )

  private def _source_json(source: InternalModelSemanticSource): Json =
    Json.obj(
      "authority" -> Json.fromString(source.authority),
      "identity" -> Json.fromString(source.identity),
      "locator" -> source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null),
      "sha256" -> Json.fromString(source.sha256)
    )

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _nonblank_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).filter(_nonblank).toRight(s"$label $key must be a nonblank Unicode string")

  private def _nullable_nonblank_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key).toRight(s"$label $key is missing").flatMap {
      case value if value.isNull => Right(None)
      case value => value.asString.filter(_nonblank).map(Some(_)).toRight(s"$label $key must be null or a nonblank Unicode string")
    }

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] = {
    val ordered = values.sliding(2).forall {
      case Vector(left, right) => _compare_tuple(left, right) < 0
      case _ => true
    }
    Either.cond(ordered, (), s"$label are not in canonical tuple order or contain a duplicate")
  }

  private def _sort_records(records: Vector[InternalModelDecisionRecord]): Vector[InternalModelDecisionRecord] =
    records.sortWith((left, right) => _compare_text(left.decisionIdentity, right.decisionIdentity) < 0)

  private def _sort_targets(targets: Vector[InternalModelSemanticTarget]): Vector[InternalModelSemanticTarget] =
    targets.sortWith((left, right) => _compare_tuple(Vector(left.semanticIdentityKind, left.semanticIdentity), Vector(right.semanticIdentityKind, right.semanticIdentity)) < 0)

  private def _sort_evidence(evidence: Vector[InternalModelDecisionEvidence]): Vector[InternalModelDecisionEvidence] =
    evidence.sortWith((left, right) => _compare_text(left.evidenceIdentity, right.evidenceIdentity) < 0)

  private def _sort_alternatives(alternatives: Vector[InternalModelDecisionAlternative]): Vector[InternalModelDecisionAlternative] =
    alternatives.sortWith((left, right) => _compare_text(left.alternativeIdentity, right.alternativeIdentity) < 0)

  private def _sort_strings(values: Vector[String]): Vector[String] =
    values.sortWith((left, right) => _compare_text(left, right) < 0)

  private def _compare_tuple(left: Vector[String], right: Vector[String]): Int =
    left.zip(right).iterator.map { case (leftvalue, rightvalue) => _compare_text(leftvalue, rightvalue) }.find(_ != 0).getOrElse(0)

  private def _compare_text(left: String, right: String): Int = {
    val leftbytes = left.getBytes(StandardCharsets.UTF_8)
    val rightbytes = right.getBytes(StandardCharsets.UTF_8)
    val limit = math.min(leftbytes.length, rightbytes.length)
    var index = 0
    while index < limit && leftbytes(index) == rightbytes(index) do index += 1
    if index == limit then Integer.compare(leftbytes.length, rightbytes.length)
    else Integer.compare(leftbytes(index) & 0xff, rightbytes(index) & 0xff)
  }

  private def _nonblank(value: String): Boolean =
    _unicode_scalars(value) && {
      var index = 0
      var nonblank = false
      while index < value.length do {
        val codepoint = value.codePointAt(index)
        if !Character.isWhitespace(codepoint) then nonblank = true
        index += Character.charCount(codepoint)
      }
      nonblank
    }

  private def _unicode_scalars(value: String): Boolean = {
    var index = 0
    var valid = true
    while index < value.length && valid do {
      val character = value.charAt(index)
      if Character.isHighSurrogate(character) then {
        valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1))
        index += 2
      } else if Character.isLowSurrogate(character) then {
        valid = false
        index += 1
      } else index += 1
    }
    valid
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

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
