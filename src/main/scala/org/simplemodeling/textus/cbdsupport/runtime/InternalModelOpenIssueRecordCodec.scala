package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 28, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/** Parses and emits the closed open-issue record bytes without admission or I/O. */
private[runtime] object InternalModelOpenIssueRecordCodec {
  private val _root_fields = Set("basis", "issues", "ledgerReference", "profile", "schemaVersion", "scope")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _basis_fields = Set("realizationArtifactReference", "realizationReference")
  private val _issue_fields = Set(
    "affectedTargets", "assumptions", "blocking", "conditions", "consideredEvidence", "decisionRole", "impact",
    "issueReference", "limitations", "options", "ownerIdentity", "question", "realizationConditionIds", "state"
  )
  private val _target_fields = Set("semanticIdentity", "semanticIdentityKind")
  private val _evidence_fields = Set("conditionIds", "conditions", "evidenceIdentity", "kind", "limitations", "source", "sourceReferenceId")
  private val _source_fields = Set("authority", "identity", "locator", "revision")
  private val _option_fields = Set("assumptions", "conditions", "description", "evidenceIds", "limitations", "optionIdentity")
  private val _blocking_fields = Set("application", "cmlProjection", "semanticApproval", "validation")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(openIssue: InternalModelVerifiedOpenIssue): Either[String, InternalModelOpenIssueLedger] = {
    for {
      _ <- _captured_metadata(openIssue)
      bytes = openIssue.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "open-issue record bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "open-issue record bytes")
      json <- _json_parser.parse(content).left.map(_ => "open-issue record bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("open-issue record root must be an object")
      _ <- _closed_fields(root, _root_fields, "open-issue record root")
      profile <- _nonblank_string(root, "profile", "open-issue record root")
      schema <- _nonblank_string(root, "schemaVersion", "open-issue record root")
      _ <- Either.cond(profile == "ccdm-open-issue-records-v2" && schema == "2.0", (), "open-issue record profile and schemaVersion are unsupported")
      ledgerreference <- _record_reference(root, "ledgerReference", "open-issue record root")
      scope <- _scope(root, "open-issue record")
      basisvalue <- root("basis").toRight("open-issue record basis is missing")
      basis <- _basis(basisvalue, "open-issue record basis")
      issues <- _issues(root)
    } yield InternalModelOpenIssueLedger(profile, schema, ledgerreference, scope, basis, issues)
  }

  def encode(ledger: InternalModelOpenIssueLedger): Array[Byte] =
    _json_output(Json.obj(
      "basis" -> _basis_json(ledger.basis),
      "issues" -> Json.fromValues(_sort_issues(ledger.issues).map(_issue_json)),
      "ledgerReference" -> _record_reference_json(ledger.ledgerReference),
      "profile" -> Json.fromString(ledger.profile),
      "schemaVersion" -> Json.fromString(ledger.schemaVersion),
      "scope" -> _scope_json(ledger.scope)
    ))

  private def _issues(root: JsonObject): Either[String, Vector[InternalModelOpenIssueRecord]] = {
    for {
      values <- _array(root, "issues", "open-issue record root")
      issues <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelOpenIssueRecord]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          issue <- _issue(value, index)
        } yield collected :+ issue
      }
      _ <- _strictly_sorted(issues.map(issue => Vector(issue.issueReference.recordId.value)), "open-issue records")
    } yield issues
  }

  private def _issue(value: Json, index: Int): Either[String, InternalModelOpenIssueRecord] = {
    for {
      objectvalue <- _object(value, s"open-issue record $index")
      _ <- _closed_fields(objectvalue, _issue_fields, s"open-issue record $index")
      issuereference <- _record_reference(objectvalue, "issueReference", s"open-issue record $index")
      issueidentity = issuereference.recordId.value
      state <- _state(objectvalue, s"open-issue record $issueidentity")
      question <- _nonblank_string(objectvalue, "question", s"open-issue record $issueidentity")
      decisionrole <- _nonblank_string(objectvalue, "decisionRole", s"open-issue record $issueidentity")
      owneridentity <- _nullable_nonblank_string(objectvalue, "ownerIdentity", s"open-issue record $issueidentity")
      impact <- _nonblank_string(objectvalue, "impact", s"open-issue record $issueidentity")
      targets <- _affected_targets(objectvalue, issueidentity)
      evidence <- _evidence(objectvalue, issueidentity)
      options <- _options(objectvalue, issueidentity)
      assumptions <- _prose(objectvalue, "assumptions", s"open-issue record $issueidentity")
      conditions <- _prose(objectvalue, "conditions", s"open-issue record $issueidentity")
      limitations <- _prose(objectvalue, "limitations", s"open-issue record $issueidentity")
      conditionids <- _sorted_strings(objectvalue, "realizationConditionIds", s"open-issue record $issueidentity")
      blockingvalue <- objectvalue("blocking").toRight(s"open-issue record $issueidentity blocking is missing")
      blocking <- _blocking(blockingvalue, s"open-issue record $issueidentity blocking")
    } yield InternalModelOpenIssueRecord(
      issuereference, state, question, decisionrole, owneridentity, impact, targets, evidence, options,
      assumptions, conditions, limitations, conditionids, blocking
    )
  }

  private def _state(objectvalue: JsonObject, label: String): Either[String, InternalModelOpenIssueState] = {
    _nonblank_string(objectvalue, "state", label).flatMap {
      case "open" => Right(InternalModelOpenIssueState.Open)
      case _ => Left(s"$label state is invalid")
    }
  }

  private def _affected_targets(objectvalue: JsonObject, issueidentity: String): Either[String, Vector[InternalModelSemanticTarget]] = {
    for {
      values <- _array(objectvalue, "affectedTargets", s"open-issue record $issueidentity")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          target <- _target(value, s"open-issue record $issueidentity affected target $index")
        } yield collected :+ target
      }
      _ <- _strictly_sorted(targets.map(target => Vector(target.semanticIdentityKind, target.semanticIdentity)), s"open-issue record $issueidentity affectedTargets")
    } yield targets
  }

  private def _target(value: Json, label: String): Either[String, InternalModelSemanticTarget] = {
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _target_fields, label)
      kind <- _nonblank_string(objectvalue, "semanticIdentityKind", label)
      _ <- Either.cond(Set("element", "relationship").contains(kind), (), s"$label semanticIdentityKind is invalid")
      identity <- _nonblank_string(objectvalue, "semanticIdentity", label)
    } yield InternalModelSemanticTarget(kind, identity)
  }

  private def _evidence(objectvalue: JsonObject, issueidentity: String): Either[String, Vector[InternalModelOpenIssueEvidence]] = {
    for {
      values <- _array(objectvalue, "consideredEvidence", s"open-issue record $issueidentity")
      evidence <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelOpenIssueEvidence]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          entry <- _evidence_entry(value, issueidentity, index)
        } yield collected :+ entry
      }
      _ <- _strictly_sorted(evidence.map(entry => Vector(entry.evidenceIdentity)), s"open-issue record $issueidentity consideredEvidence")
    } yield evidence
  }

  private def _evidence_entry(value: Json, issueidentity: String, index: Int): Either[String, InternalModelOpenIssueEvidence] = {
    for {
      objectvalue <- _object(value, s"open-issue record $issueidentity evidence $index")
      _ <- _closed_fields(objectvalue, _evidence_fields, s"open-issue record $issueidentity evidence $index")
      identity <- _nonblank_string(objectvalue, "evidenceIdentity", s"open-issue record $issueidentity evidence $index")
      kind <- _evidence_kind(objectvalue, s"open-issue record $issueidentity evidence $identity")
      sourcevalue <- objectvalue("source").toRight(s"open-issue record $issueidentity evidence $identity source is missing")
      source <- _source(sourcevalue, s"open-issue record $issueidentity evidence $identity source")
      referenceid <- _nullable_nonblank_string(objectvalue, "sourceReferenceId", s"open-issue record $issueidentity evidence $identity")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"open-issue record $issueidentity evidence $identity")
      _ <- kind match {
        case InternalModelOpenIssueEvidenceKind.RealizationSource => Either.cond(referenceid.nonEmpty, (), s"open-issue record $issueidentity evidence $identity realization-source needs sourceReferenceId")
        case _ => Either.cond(referenceid.isEmpty && conditionids.isEmpty, (), s"open-issue record $issueidentity evidence $identity external evidence must not link realization conditions")
      }
      conditions <- _prose(objectvalue, "conditions", s"open-issue record $issueidentity evidence $identity")
      limitations <- _prose(objectvalue, "limitations", s"open-issue record $issueidentity evidence $identity")
    } yield InternalModelOpenIssueEvidence(identity, kind, source, referenceid, conditionids, conditions, limitations)
  }

  private def _evidence_kind(objectvalue: JsonObject, label: String): Either[String, InternalModelOpenIssueEvidenceKind] = {
    _nonblank_string(objectvalue, "kind", label).flatMap {
      case "realization-source" => Right(InternalModelOpenIssueEvidenceKind.RealizationSource)
      case "external-human" => Right(InternalModelOpenIssueEvidenceKind.ExternalHuman)
      case "provider-proposal" => Right(InternalModelOpenIssueEvidenceKind.ProviderProposal)
      case "external-other" => Right(InternalModelOpenIssueEvidenceKind.ExternalOther)
      case _ => Left(s"$label kind is invalid")
    }
  }

  private def _options(objectvalue: JsonObject, issueidentity: String): Either[String, Vector[InternalModelOpenIssueOption]] = {
    for {
      values <- _array(objectvalue, "options", s"open-issue record $issueidentity")
      options <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelOpenIssueOption]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          option <- _option(value, issueidentity, index)
        } yield collected :+ option
      }
      _ <- _strictly_sorted(options.map(option => Vector(option.optionIdentity)), s"open-issue record $issueidentity options")
    } yield options
  }

  private def _option(value: Json, issueidentity: String, index: Int): Either[String, InternalModelOpenIssueOption] = {
    for {
      objectvalue <- _object(value, s"open-issue record $issueidentity option $index")
      _ <- _closed_fields(objectvalue, _option_fields, s"open-issue record $issueidentity option $index")
      optionidentity <- _nonblank_string(objectvalue, "optionIdentity", s"open-issue record $issueidentity option $index")
      description <- _nonblank_string(objectvalue, "description", s"open-issue record $issueidentity option $optionidentity")
      evidenceids <- _sorted_strings(objectvalue, "evidenceIds", s"open-issue record $issueidentity option $optionidentity")
      assumptions <- _prose(objectvalue, "assumptions", s"open-issue record $issueidentity option $optionidentity")
      conditions <- _prose(objectvalue, "conditions", s"open-issue record $issueidentity option $optionidentity")
      limitations <- _prose(objectvalue, "limitations", s"open-issue record $issueidentity option $optionidentity")
    } yield InternalModelOpenIssueOption(optionidentity, description, evidenceids, assumptions, conditions, limitations)
  }

  private def _scope(objectvalue: JsonObject, label: String): Either[String, InternalModelSemanticScope] = {
    for {
      value <- objectvalue("scope").toRight(s"$label scope is missing")
      scopeobject <- _object(value, s"$label scope")
      _ <- _closed_fields(scopeobject, _scope_fields, s"$label scope")
      componentidentity <- _nonblank_string(scopeobject, "componentIdentity", s"$label scope")
      contextidentity <- _nonblank_string(scopeobject, "projectionContextIdentity", s"$label scope")
      usecaseidentity <- _nonblank_string(scopeobject, "selectedUseCaseElementIdentity", s"$label scope")
    } yield InternalModelSemanticScope(componentidentity, contextidentity, usecaseidentity)
  }

  private def _basis(value: Json, label: String): Either[String, InternalModelOpenIssueBasis] = {
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _basis_fields, label)
      artifactvalue <- objectvalue("realizationArtifactReference").toRight(s"$label realizationArtifactReference is missing")
      artifactreference <- InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(artifactvalue))
      _ <- Either.cond(artifactreference.role == InternalModelArtifactRole.Realization, (), s"$label artifact role must be realization")
      recordreference <- _record_reference(objectvalue, "realizationReference", label)
    } yield InternalModelOpenIssueBasis(artifactreference, recordreference)
  }

  private def _blocking(value: Json, label: String): Either[String, InternalModelOpenIssueBlocking] = {
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _blocking_fields, label)
      semanticapproval <- _boolean(objectvalue, "semanticApproval", label)
      cmlprojection <- _boolean(objectvalue, "cmlProjection", label)
      application <- _boolean(objectvalue, "application", label)
      validation <- _boolean(objectvalue, "validation", label)
    } yield InternalModelOpenIssueBlocking(semanticapproval, cmlprojection, application, validation)
  }

  private def _source(value: Json, label: String): Either[String, InternalModelSemanticSource] = {
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _source_fields, label)
      authority <- _nonblank_string(objectvalue, "authority", label)
      identity <- _nonblank_string(objectvalue, "identity", label)
      locator <- _nullable_nonblank_string(objectvalue, "locator", label)
      revision <- _nullable_nonblank_string(objectvalue, "revision", label)
    } yield InternalModelSemanticSource(authority, identity, locator, revision)
  }

  private def _prose(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] = {
    for {
      values <- _array(objectvalue, key, label)
      prose <- values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          text <- _nonblank_string_value(value, s"$label $key")
        } yield collected :+ text
      }
    } yield prose
  }

  private def _sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] = {
    for {
      values <- _array(objectvalue, key, label)
      strings <- values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          text <- _nonblank_string_value(value, s"$label $key")
        } yield collected :+ text
      }
      _ <- _strictly_sorted(strings.map(Vector(_)), s"$label $key")
    } yield strings
  }

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _boolean(objectvalue: JsonObject, key: String, label: String): Either[String, Boolean] =
    objectvalue(key).flatMap(_.asBoolean).toRight(s"$label $key must be a JSON Boolean")

  private def _nonblank_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).filter(_nonblank).toRight(s"$label $key must be a nonblank Unicode string")

  private def _nullable_nonblank_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] = {
    objectvalue(key).toRight(s"$label $key is missing").flatMap {
      case value if value.isNull => Right(None)
      case value => value.asString.filter(_nonblank).map(Some(_)).toRight(s"$label $key must be null or a nonblank Unicode string")
    }
  }

  private def _nonblank_string_value(value: Json, label: String): Either[String, String] =
    value.asString.filter(_nonblank).toRight(s"$label must contain only nonblank Unicode strings")

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] = {
    val ordered = values.sliding(2).forall {
      case Vector(left, right) => _compare_tuple(left, right) < 0
      case _ => true
    }
    Either.cond(ordered, (), s"$label are not in canonical tuple order or contain a duplicate")
  }

  private def _issue_json(issue: InternalModelOpenIssueRecord): Json =
    Json.obj(
      "affectedTargets" -> Json.fromValues(_sort_targets(issue.affectedTargets).map(_target_json)),
      "assumptions" -> Json.fromValues(issue.assumptions.map(Json.fromString)),
      "blocking" -> _blocking_json(issue.blocking),
      "conditions" -> Json.fromValues(issue.conditions.map(Json.fromString)),
      "consideredEvidence" -> Json.fromValues(_sort_evidence(issue.consideredEvidence).map(_evidence_json)),
      "decisionRole" -> Json.fromString(issue.decisionRole),
      "impact" -> Json.fromString(issue.impact),
      "issueReference" -> _record_reference_json(issue.issueReference),
      "limitations" -> Json.fromValues(issue.limitations.map(Json.fromString)),
      "options" -> Json.fromValues(_sort_options(issue.options).map(_option_json)),
      "ownerIdentity" -> issue.ownerIdentity.map(Json.fromString).getOrElse(Json.Null),
      "question" -> Json.fromString(issue.question),
      "realizationConditionIds" -> Json.fromValues(_sort_strings(issue.realizationConditionIds).map(Json.fromString)),
      "state" -> Json.fromString(issue.state.wireValue)
    )

  private def _basis_json(basis: InternalModelOpenIssueBasis): Json =
    Json.obj(
      "realizationArtifactReference" -> _artifact_reference_json(basis.realizationArtifactReference),
      "realizationReference" -> _record_reference_json(basis.realizationReference)
    )

  private def _scope_json(scope: InternalModelSemanticScope): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString(scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity)
    )

  private def _target_json(target: InternalModelSemanticTarget): Json =
    Json.obj(
      "semanticIdentity" -> Json.fromString(target.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(target.semanticIdentityKind)
    )

  private def _evidence_json(evidence: InternalModelOpenIssueEvidence): Json =
    Json.obj(
      "conditionIds" -> Json.fromValues(_sort_strings(evidence.conditionIds).map(Json.fromString)),
      "conditions" -> Json.fromValues(evidence.conditions.map(Json.fromString)),
      "evidenceIdentity" -> Json.fromString(evidence.evidenceIdentity),
      "kind" -> Json.fromString(evidence.kind.wireValue),
      "limitations" -> Json.fromValues(evidence.limitations.map(Json.fromString)),
      "source" -> _source_json(evidence.source),
      "sourceReferenceId" -> evidence.sourceReferenceId.map(Json.fromString).getOrElse(Json.Null)
    )

  private def _source_json(source: InternalModelSemanticSource): Json =
    Json.obj(
      "authority" -> Json.fromString(source.authority),
      "identity" -> Json.fromString(source.identity),
      "locator" -> source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null)
    )

  private def _option_json(option: InternalModelOpenIssueOption): Json =
    Json.obj(
      "assumptions" -> Json.fromValues(option.assumptions.map(Json.fromString)),
      "conditions" -> Json.fromValues(option.conditions.map(Json.fromString)),
      "description" -> Json.fromString(option.description),
      "evidenceIds" -> Json.fromValues(_sort_strings(option.evidenceIds).map(Json.fromString)),
      "limitations" -> Json.fromValues(option.limitations.map(Json.fromString)),
      "optionIdentity" -> Json.fromString(option.optionIdentity)
    )

  private def _blocking_json(blocking: InternalModelOpenIssueBlocking): Json =
    Json.obj(
      "application" -> Json.fromBoolean(blocking.application),
      "cmlProjection" -> Json.fromBoolean(blocking.cmlProjection),
      "semanticApproval" -> Json.fromBoolean(blocking.semanticApproval),
      "validation" -> Json.fromBoolean(blocking.validation)
    )

  private def _sort_issues(issues: Vector[InternalModelOpenIssueRecord]): Vector[InternalModelOpenIssueRecord] =
    issues.sortWith((left, right) => _compare_text(left.issueReference.recordId.value, right.issueReference.recordId.value) < 0)

  private def _sort_targets(targets: Vector[InternalModelSemanticTarget]): Vector[InternalModelSemanticTarget] =
    targets.sortWith((left, right) => _compare_tuple(Vector(left.semanticIdentityKind, left.semanticIdentity), Vector(right.semanticIdentityKind, right.semanticIdentity)) < 0)

  private def _sort_evidence(evidence: Vector[InternalModelOpenIssueEvidence]): Vector[InternalModelOpenIssueEvidence] =
    evidence.sortWith((left, right) => _compare_text(left.evidenceIdentity, right.evidenceIdentity) < 0)

  private def _sort_options(options: Vector[InternalModelOpenIssueOption]): Vector[InternalModelOpenIssueOption] =
    options.sortWith((left, right) => _compare_text(left.optionIdentity, right.optionIdentity) < 0)

  private def _sort_strings(values: Vector[String]): Vector[String] =
    values.sortWith((left, right) => _compare_text(left, right) < 0)

  private def _compare_tuple(left: Vector[String], right: Vector[String]): Int =
    left.zip(right).iterator.map { case (leftvalue, rightvalue) => _compare_text(leftvalue, rightvalue) }.find(_ != 0).getOrElse(left.length.compare(right.length))

  private def _compare_text(left: String, right: String): Int =
    Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))

  private def _nonblank(value: String): Boolean = {
    var index = 0
    var valid = true
    var nonblank = false
    while index < value.length && valid do {
      val character = value.charAt(index)
      if Character.isHighSurrogate(character) then {
        valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1))
        if valid && !Character.isWhitespace(value.codePointAt(index)) then nonblank = true
        index += 2
      } else if Character.isLowSurrogate(character) then {
        valid = false
        index += 1
      } else {
        if !Character.isWhitespace(character) then nonblank = true
        index += 1
      }
    }
    valid && nonblank
  }

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] = {
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }
  }

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _json_output(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _json_bytes(json: Json): Vector[Byte] =
    json.noSpaces.getBytes(StandardCharsets.UTF_8).toVector

  private def _record_reference(objectvalue: JsonObject, key: String, label: String): Either[String, InternalModelRecordReference] =
    for {
      value <- objectvalue(key).toRight(s"$label $key is missing")
      _ <- Either.cond(value.asObject.flatMap(_("recordId")).flatMap(_.asString).exists(_nonblank), (), s"$label $key recordId must be a nonblank Unicode scalar string")
      reference <- InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(value))
      _ <- Either.cond(_nonblank(reference.recordId.value), (), s"$label $key recordId must be a nonblank Unicode scalar string")
    } yield reference

  private def _record_reference_json(reference: InternalModelRecordReference): Json =
    Json.obj("recordId" -> Json.fromString(reference.recordId.value), "recordRevision" -> Json.fromLong(reference.recordRevision.value))

  private def _artifact_reference_json(reference: InternalModelArtifactReference): Json =
    Json.obj("artifactId" -> Json.fromString(reference.artifactId.value), "artifactRevision" -> Json.fromLong(reference.artifactRevision.value), "role" -> Json.fromString(reference.role.wireValue))

  private def _captured_metadata(openissue: InternalModelVerifiedOpenIssue): Either[String, Unit] =
    for {
      _ <- Either.cond(openissue != null, (), "selected open-issue must be present")
      _ <- _artifact_reference_metadata(openissue.reference)
      _ <- Either.cond(openissue.reference.role == InternalModelArtifactRole.OpenIssue, (), "selected artifact role must be open-issue")
      _ <- Either.cond(openissue.path != null && _nonblank(openissue.path), (), "selected open-issue path must be present")
      _ <- Either.cond(openissue.bytes != null, (), "selected open-issue bytes must be present")
      _ <- Either.cond(openissue.dependencies != null, (), "selected open-issue dependencies must be present")
      _ <- openissue.dependencies.foldLeft[Either[String, Unit]](Right(())) { (result, reference) =>
        result.flatMap(_ => _artifact_reference_metadata(reference))
      }
      _ <- _strictly_sorted(openissue.dependencies.map(reference => Vector(reference.artifactId.value)), "selected open-issue dependencies")
      _ <- Either.cond(!openissue.dependencies.exists(_.artifactId == openissue.reference.artifactId), (), "selected open-issue must not depend on itself")
    } yield ()

  private def _artifact_reference_metadata(reference: InternalModelArtifactReference): Either[String, Unit] =
    for {
      _ <- Either.cond(reference != null && reference.role != null, (), "captured artifact reference and role must be present")
      _ <- InternalModelArtifactId.from(reference.artifactId.value)
      _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
    } yield ()
}
