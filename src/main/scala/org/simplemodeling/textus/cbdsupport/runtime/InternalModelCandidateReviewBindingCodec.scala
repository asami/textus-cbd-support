package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Strict structural codec for durable, non-approval candidate-review evidence. */
private[runtime] object InternalModelCandidateReviewBindingCodec {
  private val _root_fields = Set("candidateArtifactReference", "candidateReference", "candidateModelIdentity", "continuityArtifactReference", "evidenceArtifacts", "profile", "providers", "realizationArtifactReference", "realizationReference", "reviewReference", "rules", "schemaVersion", "scope", "semanticDiffArtifactReference", "semanticDiffReference", "subject", "targets")
  private val _subject_fields = Set("subjectId", "subjectRevision", "packageReference", "scope", "artifacts")
  private val _rule_fields = Set("ruleId", "ruleVersion")
  private val _provider_fields = Set("providerId", "providerVersion")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _target_fields = Set("evidenceArtifactIds", "reviewSnapshot", "targetId")
  private val _snapshot_fields = Set("attribution", "candidateModelId", "component", "condition", "context", "id", "limitations", "patchId", "stableTieKey", "state")
  private val _attribution_fields = Set("authorityScope", "sourceId", "sourceLocator")
  private val _condition_fields = Set("ambiguity", "authorization", "availability", "conflict", "explicitAbsence", "limitations", "malformedEvidence", "redaction", "staleness")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateReviewBinding] = {
    for {
      _ <- Either.cond(bytes != null, (), "candidate review bytes must be present")
      content <- _decode_utf8(bytes.toArray, "candidate review bytes")
      _ <- Either.cond(!content.startsWith("\uFEFF"), (), "candidate review bytes must not have a BOM")
      json <- _json_parser.parse(content).left.map(_ => "candidate review must be strict JSON without duplicate members or trailing content")
      value <- _decode_json(json)
    } yield value
  }

  /** Structural grammar only; it establishes no capture or provider authenticity. */
  def validateValue(value: InternalModelCandidateReviewBinding): Either[String, Unit] = {
    try _decode_json(_value_json(value)).map(_ => ())
    catch { case NonFatal(_) => Left("candidate review value contains a null or invalid object graph") }
  }

  private def _decode_json(json: Json): Either[String, InternalModelCandidateReviewBinding] = {
    for {
      _ <- _valid_json_unicode(json)
      root <- _object(json, "candidate review root")
      _ <- _closed_fields(root, _root_fields, "candidate review root")
      candidateartifact <- _artifact(root, "candidateArtifactReference", Some(InternalModelArtifactRole.Projection))
      candidatereference <- _record(root, "candidateReference")
      candidatemodelidentity <- _nonblank(root, "candidateModelIdentity", "candidate review")
      continuityartifact <- _artifact(root, "continuityArtifactReference", Some(InternalModelArtifactRole.Projection))
      evidence <- _artifacts(root, "evidenceArtifacts", Some(InternalModelArtifactRole.Validation))
      profile <- _nonblank(root, "profile", "candidate review")
      _ <- Either.cond(profile == "ccdm-candidate-review-binding-v2", (), "candidate review profile is unsupported")
      providers <- _providers(root)
      realizationartifact <- _artifact(root, "realizationArtifactReference", Some(InternalModelArtifactRole.Realization))
      realizationreference <- _record(root, "realizationReference")
      reviewreference <- _record(root, "reviewReference")
      rules <- _rules(root)
      schema <- _nonblank(root, "schemaVersion", "candidate review")
      _ <- Either.cond(schema == "2.0", (), "candidate review schemaVersion is unsupported")
      scope <- _scope(root)
      diffartifact <- _artifact(root, "semanticDiffArtifactReference", Some(InternalModelArtifactRole.Projection))
      diffreference <- _record(root, "semanticDiffReference")
      subjectvalue <- root("subject").toRight("candidate review subject is missing")
      subject <- decodeSubject(subjectvalue)
      targets <- _targets(root, evidence)
    } yield InternalModelCandidateReviewBinding(candidateartifact, candidatereference, candidatemodelidentity, continuityartifact, evidence, profile, providers, realizationartifact, realizationreference, reviewreference, rules, schema, scope, diffartifact, diffreference, subject, targets)
  }

  def encode(value: InternalModelCandidateReviewBinding): Array[Byte] =
    _json_bytes(_value_json(value))

  private def _value_json(value: InternalModelCandidateReviewBinding): Json =
    Json.obj(
      "candidateArtifactReference" -> _artifact_json(value.candidateArtifactReference),
      "candidateReference" -> _record_json(value.candidateReference),
      "candidateModelIdentity" -> Json.fromString(value.candidateModelIdentity),
      "continuityArtifactReference" -> _artifact_json(value.continuityArtifactReference),
      "evidenceArtifacts" -> Json.fromValues(value.evidenceArtifacts.map(_artifact_json)),
      "profile" -> Json.fromString(value.profile),
      "providers" -> Json.fromValues(value.providers.map(_provider_json)),
      "realizationArtifactReference" -> _artifact_json(value.realizationArtifactReference),
      "realizationReference" -> _record_json(value.realizationReference),
      "reviewReference" -> _record_json(value.reviewReference),
      "rules" -> Json.fromValues(value.rules.map(_rule_json)),
      "schemaVersion" -> Json.fromString(value.schemaVersion),
      "scope" -> _scope_json(value.scope),
      "semanticDiffArtifactReference" -> _artifact_json(value.semanticDiffArtifactReference),
      "semanticDiffReference" -> _record_json(value.semanticDiffReference),
      "subject" -> encodeSubject(value.subject),
      "targets" -> Json.fromValues(value.targets.map(_target_json))
    )

  private[runtime] def validateExecutionBasis(basis: InternalModelCandidateReviewExecutionBasis): Either[String, Unit] =
    for {
      _ <- Either.cond(basis != null && basis.rules != null && basis.providers != null, (), "expected candidate review basis and vectors must be present")
      _ <- Either.cond(basis.rules.forall(_ != null) && basis.providers.forall(_ != null), (), "expected candidate review basis entries must be present")
      _ <- _typed_basis_entries(basis.rules.map(value => (value.ruleId, value.ruleVersion)), "rules")
      _ <- _typed_basis_entries(basis.providers.map(value => (value.providerId, value.providerVersion)), "providers")
    } yield ()

  private def _artifact(root: JsonObject, key: String, role: Option[InternalModelArtifactRole]): Either[String, InternalModelArtifactReference] =
    root(key).toRight(s"candidate review $key is missing").flatMap(_artifact_value(_, role))

  private def _artifact_value(value: Json, role: Option[InternalModelArtifactRole]): Either[String, InternalModelArtifactReference] =
    for {
      reference <- InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(value).toVector)
      _ <- Either.cond(role.forall(_ == reference.role), (), "candidate review artifact has wrong role")
      _ <- Either.cond(reference.role != InternalModelArtifactRole.Resume && reference.role != InternalModelArtifactRole.Approval, (), "candidate review subject cannot consume resume or approval")
    } yield reference

  private def _record(root: JsonObject, key: String): Either[String, InternalModelRecordReference] =
    root(key).toRight(s"candidate review $key is missing").flatMap { value =>
      for {
        fields <- _object(value, s"candidate review $key")
        _ <- _nonblank(fields, "recordId", s"candidate review $key")
        reference <- InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(value).toVector)
      } yield reference
    }

  private def _artifacts(root: JsonObject, key: String, role: Option[InternalModelArtifactRole]): Either[String, Vector[InternalModelArtifactReference]] =
    for {
      values <- _array(root, key, "candidate review")
      artifacts <- values.foldLeft[Either[String, Vector[InternalModelArtifactReference]]](Right(Vector.empty)) { (result, value) =>
        for { collected <- result; reference <- _artifact_value(value, role) } yield collected :+ reference
      }
      _ <- Either.cond(artifacts.nonEmpty, (), s"candidate review $key must be nonempty")
      _ <- _strictly_sorted(artifacts.map(_.artifactId.value), s"candidate review $key")
    } yield artifacts

  private[runtime] def decodeSubject(value: Json): Either[String, InternalModelReviewSubject] =
    for {
      _ <- _valid_json_unicode(value)
      subject <- _object(value, "candidate review subject")
      _ <- _closed_fields(subject, _subject_fields, "candidate review subject")
      id <- subject("subjectId").toRight("subjectId is missing")
      _ <- _nonblank(subject, "subjectId", "candidate review subject")
      revision <- subject("subjectRevision").toRight("subjectRevision is missing")
      reference <- InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(Json.obj("recordId" -> id, "recordRevision" -> revision)).toVector)
      packagevalue <- subject("packageReference").toRight("subject packageReference is missing")
      packagereference <- InternalModelTypedControlCodec.decodePackageReference(_json_bytes(packagevalue).toVector)
      scope <- _scope(subject)
      artifacts <- _artifacts(subject, "artifacts", None)
    } yield InternalModelReviewSubject(reference.recordId, reference.recordRevision, packagereference, scope, artifacts)

  private[runtime] def encodeSubject(value: InternalModelReviewSubject): Json = Json.obj(
    "subjectId" -> Json.fromString(value.subjectId.value),
    "subjectRevision" -> Json.fromLong(value.subjectRevision.value),
    "packageReference" -> Json.obj(
      "packageId" -> Json.fromString(value.packageReference.packageId.value),
      "projectNamespace" -> Json.fromString(value.packageReference.projectNamespace.value),
      "projectId" -> Json.fromString(value.packageReference.projectId.value)
    ),
    "scope" -> _scope_json(value.scope),
    "artifacts" -> Json.fromValues(value.artifacts.map(_artifact_json))
  )

  private def _rules(root: JsonObject): Either[String, Vector[InternalModelCandidateReviewRule]] =
    _basis_entries(root, "rules", _rule_fields, "ruleId", "ruleVersion", InternalModelCandidateReviewRule.apply)

  private def _providers(root: JsonObject): Either[String, Vector[InternalModelCandidateReviewProvider]] =
    _basis_entries(root, "providers", _provider_fields, "providerId", "providerVersion", InternalModelCandidateReviewProvider.apply)

  private def _basis_entries[A](root: JsonObject, key: String, fields: Set[String], idkey: String, versionkey: String, create: (String, String) => A): Either[String, Vector[A]] =
    for {
      values <- _array(root, key, "candidate review")
      entries <- values.foldLeft[Either[String, Vector[(String, A)]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          objectvalue <- _object(value, s"candidate review $key")
          _ <- _closed_fields(objectvalue, fields, s"candidate review $key")
          id <- _nonblank(objectvalue, idkey, s"candidate review $key")
          version <- _nonblank(objectvalue, versionkey, s"candidate review $key")
        } yield collected :+ (id -> create(id, version))
      }
      _ <- Either.cond(entries.nonEmpty, (), s"candidate review $key must be nonempty")
      _ <- _strictly_sorted(entries.map(_._1), s"candidate review $key")
    } yield entries.map(_._2)

  private def _typed_basis_entries(values: Vector[(String, String)], label: String): Either[String, Unit] =
    for {
      _ <- Either.cond(values.nonEmpty, (), s"expected candidate review $label must be nonempty")
      _ <- Either.cond(values.forall { case (id, version) => _valid_text(id) && _valid_text(version) }, (), s"expected candidate review $label has malformed ID or version")
      _ <- _strictly_sorted(values.map(_._1), s"expected candidate review $label")
    } yield ()

  private def _scope(root: JsonObject): Either[String, InternalModelSemanticScope] =
    for {
      value <- root("scope").toRight("candidate review scope is missing")
      objectvalue <- _object(value, "candidate review scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "candidate review scope")
      component <- _nonblank(objectvalue, "componentIdentity", "candidate review scope")
      context <- _nonblank(objectvalue, "projectionContextIdentity", "candidate review scope")
      usecase <- _nonblank(objectvalue, "selectedUseCaseElementIdentity", "candidate review scope")
    } yield InternalModelSemanticScope(component, context, usecase)

  private def _targets(root: JsonObject, evidence: Vector[InternalModelArtifactReference]): Either[String, Vector[InternalModelCandidateReviewTarget]] =
    for {
      values <- _array(root, "targets", "candidate review root")
      _ <- Either.cond(values.nonEmpty, (), "candidate review targets must be nonempty")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateReviewTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for { collected <- result; target <- _target(value, index) } yield collected :+ target
      }
      _ <- _strictly_sorted(targets.map(_.targetId), "candidate review targets")
      _ <- _unique(targets.map(_.reviewSnapshot.id), "candidate review snapshot IDs")
      evidenceids = evidence.map(_.artifactId.value).toSet
      targetids = targets.flatMap(_.evidenceArtifactIds)
      _ <- Either.cond(targetids.toSet == evidenceids, (), "candidate review target evidence IDs must cover the complete evidence artifact set")
    } yield targets

  private def _target(value: Json, index: Int): Either[String, InternalModelCandidateReviewTarget] =
    for {
      objectvalue <- _object(value, s"candidate review target $index")
      _ <- _closed_fields(objectvalue, _target_fields, s"candidate review target $index")
      evidence <- _nonempty_sorted_strings(objectvalue, "evidenceArtifactIds", s"candidate review target $index")
      snapshotvalue <- objectvalue("reviewSnapshot").toRight(s"candidate review target $index reviewSnapshot is missing")
      snapshot <- _snapshot(snapshotvalue, index)
      targetid <- _nonblank(objectvalue, "targetId", s"candidate review target $index")
    } yield InternalModelCandidateReviewTarget(evidence, snapshot, targetid)

  private def _snapshot(value: Json, targetindex: Int): Either[String, CandidateDesignReviewSnapshot] =
    for {
      objectvalue <- _object(value, s"candidate review target $targetindex reviewSnapshot")
      _ <- _closed_fields(objectvalue, _snapshot_fields, s"candidate review target $targetindex reviewSnapshot")
      attribution <- _attribution(objectvalue, s"candidate review target $targetindex reviewSnapshot")
      candidate <- _nonblank(objectvalue, "candidateModelId", s"candidate review target $targetindex reviewSnapshot")
      component <- _nonblank(objectvalue, "component", s"candidate review target $targetindex reviewSnapshot")
      condition <- _condition(objectvalue, s"candidate review target $targetindex reviewSnapshot")
      context <- _nonblank(objectvalue, "context", s"candidate review target $targetindex reviewSnapshot")
      id <- _nonblank(objectvalue, "id", s"candidate review target $targetindex reviewSnapshot")
      limitations <- _nonblank_strings(objectvalue, "limitations", s"candidate review target $targetindex reviewSnapshot")
      patch <- _nonblank(objectvalue, "patchId", s"candidate review target $targetindex reviewSnapshot")
      tie <- _nullable_nonblank(objectvalue, "stableTieKey", s"candidate review target $targetindex reviewSnapshot")
      state <- _nonblank(objectvalue, "state", s"candidate review target $targetindex reviewSnapshot")
    } yield CandidateDesignReviewSnapshot(id, MonoKotoProjectionContextIdentity(context), ComponentDashboardComponentIdentity(component), patch, candidate, state, attribution, condition, limitations, tie)

  private def _attribution(root: JsonObject, label: String): Either[String, ComponentDashboardSourceAttribution] =
    for {
      value <- root("attribution").toRight(s"$label attribution is missing")
      objectvalue <- _object(value, s"$label attribution")
      _ <- _closed_fields(objectvalue, _attribution_fields, s"$label attribution")
      authority <- _nonblank(objectvalue, "authorityScope", s"$label attribution")
      source <- _nonblank(objectvalue, "sourceId", s"$label attribution")
      locator <- _nonblank(objectvalue, "sourceLocator", s"$label attribution")
    } yield ComponentDashboardSourceAttribution(source, authority, locator)

  private def _condition(root: JsonObject, label: String): Either[String, ComponentDashboardCondition] =
    for {
      value <- root("condition").toRight(s"$label condition is missing")
      objectvalue <- _object(value, s"$label condition")
      _ <- _closed_fields(objectvalue, _condition_fields, s"$label condition")
      ambiguity <- _nullable_nonblank(objectvalue, "ambiguity", s"$label condition")
      authorization <- _nonblank(objectvalue, "authorization", s"$label condition")
      availability <- _nonblank(objectvalue, "availability", s"$label condition")
      conflict <- _nullable_nonblank(objectvalue, "conflict", s"$label condition")
      absence <- _nullable_nonblank(objectvalue, "explicitAbsence", s"$label condition")
      limitations <- _nonblank_strings(objectvalue, "limitations", s"$label condition")
      malformed <- _nullable_nonblank(objectvalue, "malformedEvidence", s"$label condition")
      redaction <- _nullable_nonblank(objectvalue, "redaction", s"$label condition")
      staleness <- _nullable_nonblank(objectvalue, "staleness", s"$label condition")
    } yield ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)

  private def _artifact_json(value: InternalModelArtifactReference): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId.value), "artifactRevision" -> Json.fromLong(value.artifactRevision.value), "role" -> Json.fromString(value.role.wireValue))
  private def _record_json(value: InternalModelRecordReference): Json = Json.obj("recordId" -> Json.fromString(value.recordId.value), "recordRevision" -> Json.fromLong(value.recordRevision.value))
  private def _rule_json(value: InternalModelCandidateReviewRule): Json = Json.obj("ruleId" -> Json.fromString(value.ruleId), "ruleVersion" -> Json.fromString(value.ruleVersion))
  private def _provider_json(value: InternalModelCandidateReviewProvider): Json = Json.obj("providerId" -> Json.fromString(value.providerId), "providerVersion" -> Json.fromString(value.providerVersion))
  private def _scope_json(value: InternalModelSemanticScope): Json = Json.obj("componentIdentity" -> Json.fromString(value.componentIdentity), "projectionContextIdentity" -> Json.fromString(value.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(value.selectedUseCaseElementIdentity))
  private def _target_json(value: InternalModelCandidateReviewTarget): Json = Json.obj("evidenceArtifactIds" -> Json.fromValues(value.evidenceArtifactIds.map(Json.fromString)), "reviewSnapshot" -> _snapshot_json(value.reviewSnapshot), "targetId" -> Json.fromString(value.targetId))
  private def _snapshot_json(value: CandidateDesignReviewSnapshot): Json = Json.obj("attribution" -> _attribution_json(value.attribution), "candidateModelId" -> Json.fromString(value.candidateModelId), "component" -> Json.fromString(value.component.value), "condition" -> _condition_json(value.condition), "context" -> Json.fromString(value.context.value), "id" -> Json.fromString(value.id), "limitations" -> Json.fromValues(value.limitations.map(Json.fromString)), "patchId" -> Json.fromString(value.patchId), "stableTieKey" -> value.stableTieKey.map(Json.fromString).getOrElse(Json.Null), "state" -> Json.fromString(value.state))
  private def _attribution_json(value: ComponentDashboardSourceAttribution): Json = Json.obj("authorityScope" -> Json.fromString(value.authorityScope), "sourceId" -> Json.fromString(value.sourceId), "sourceLocator" -> Json.fromString(value.sourceLocator))
  private def _condition_json(value: ComponentDashboardCondition): Json = Json.obj("ambiguity" -> _nullable_json(value.ambiguity), "authorization" -> Json.fromString(value.authorization), "availability" -> Json.fromString(value.availability), "conflict" -> _nullable_json(value.conflict), "explicitAbsence" -> _nullable_json(value.explicitAbsence), "limitations" -> Json.fromValues(value.limitations.map(Json.fromString)), "malformedEvidence" -> _nullable_json(value.malformedEvidence), "redaction" -> _nullable_json(value.redaction), "staleness" -> _nullable_json(value.staleness))
  private def _nullable_json(value: Option[String]): Json = value.map(Json.fromString).getOrElse(Json.Null)

  private def _object(value: Json, label: String): Either[String, JsonObject] = value.asObject.toRight(s"$label must be an object")
  private def _array(root: JsonObject, key: String, label: String): Either[String, Vector[Json]] = root(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")
  private def _string(root: JsonObject, key: String, label: String): Either[String, String] = root(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")
  private def _nonblank(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(_valid_text(value), value, s"$label $key must be a nonblank JSON string"))
  private def _nullable_nonblank(root: JsonObject, key: String, label: String): Either[String, Option[String]] = root(key) match { case Some(value) if value.isNull => Right(None); case Some(value) => value.asString.filter(_valid_text).map(Some(_)).toRight(s"$label $key must be null or a nonblank JSON string"); case None => Left(s"$label $key is missing") }
  private def _nonblank_strings(root: JsonObject, key: String, label: String): Either[String, Vector[String]] = _array(root, key, label).flatMap(_.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) => for { collected <- result; stringvalue <- value.asString.filter(_valid_text).toRight(s"$label $key $index must be a nonblank JSON string") } yield collected :+ stringvalue })
  private def _nonempty_sorted_strings(root: JsonObject, key: String, label: String): Either[String, Vector[String]] = _nonblank_strings(root, key, label).flatMap(values => Either.cond(values.nonEmpty, values, s"$label $key must be nonempty").flatMap(values => _strictly_sorted(values, s"$label $key").map(_ => values)))
  private def _closed_fields(root: JsonObject, fields: Set[String], label: String): Either[String, Unit] = Either.cond(root.keys.toSet == fields, (), s"$label fields are not closed")
  private def _unique(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.distinct.size == values.size, (), s"$label must be unique")
  private def _strictly_sorted(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.zip(values.drop(1)).forall { case (left, right) => _compare_text(left, right) < 0 }, (), s"$label must be unique and sorted by ascending UTF-8-byte order")
  private def _compare_text(left: String, right: String): Int = Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))
  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] = try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) } catch { case NonFatal(_) => Left(s"$label is not valid UTF-8") }
  private def _valid_text(value: String): Boolean = value != null && _valid_unicode(value) && value.codePoints().anyMatch(codepoint => codepoint > 0x20 && codepoint != 0x85 && !Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint))
  private def _valid_unicode(value: String): Boolean = {
    var index = 0
    var valid = true
    while (index < value.length && valid) {
      val current = value.charAt(index)
      if (Character.isHighSurrogate(current)) {
        valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1))
        index += 2
      } else {
        valid = !Character.isLowSurrogate(current)
        index += 1
      }
    }
    valid
  }
  private def _valid_json_unicode(value: Json): Either[String, Unit] = {
    val valid = value.fold(
      true, _ => true, _ => true, _valid_unicode,
      values => values.forall(item => _valid_json_unicode(item).isRight),
      fields => fields.toVector.forall { case (key, item) => _valid_unicode(key) && _valid_json_unicode(item).isRight }
    )
    Either.cond(valid, (), "candidate review JSON contains invalid Unicode")
  }
  private def _json_bytes(value: Json): Array[Byte] = {
    _valid_json_unicode(value).fold(message => throw new IllegalArgumentException(message), _ => ())
    (_printer.print(value) + "\n").getBytes(StandardCharsets.UTF_8)
  }
}
