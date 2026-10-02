package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/**
 * Closed test-only transport of independently supplied caller evidence.
 * Never obtains caller inputs from stored package review or approval claims.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelDurableHandoffInput {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _fields = Set("packagereference", "carrierrevision", "realizationreference", "scope",
    "candidateartifact", "semanticdiffartifact", "reviewartifact", "approvalartifact",
    "executionbasis", "humandecision", "requireddecisions", "requiredmappings")

  def encode(request: InternalModelContinuationRequest): Vector[Byte] = bytes(Json.obj(
    "packagereference" -> _json(InternalModelTypedControlCodec.encodePackageReference(request.packagereference)),
    "carrierrevision" -> Json.fromLong(request.carrierrevision),
    "realizationreference" -> _record_json(request.realizationreference),
    "scope" -> Json.obj("componentIdentity" -> Json.fromString(request.scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(request.scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(request.scope.selectedUseCaseElementIdentity)),
    "candidateartifact" -> request.candidateartifact.map(_artifact_json).getOrElse(Json.Null),
    "semanticdiffartifact" -> request.semanticdiffartifact.map(_artifact_json).getOrElse(Json.Null),
    "reviewartifact" -> request.reviewartifact.map(_artifact_json).getOrElse(Json.Null),
    "approvalartifact" -> request.approvalartifact.map(_artifact_json).getOrElse(Json.Null),
    "executionbasis" -> request.executionbasis.map(value => Json.obj(
      "rules" -> Json.fromValues(value.rules.map(rule => Json.obj("ruleId" -> Json.fromString(rule.ruleId), "ruleVersion" -> Json.fromString(rule.ruleVersion)))),
      "providers" -> Json.fromValues(value.providers.map(provider => Json.obj("providerId" -> Json.fromString(provider.providerId), "providerVersion" -> Json.fromString(provider.providerVersion))))
    )).getOrElse(Json.Null),
    "humandecision" -> request.humandecision.map(value => _json(InternalModelCandidateHumanApprovalCodec.encode(
      InternalModelCandidateHumanApproval(value, "ccdm-candidate-human-approval-v2", "2.0")).toVector)).getOrElse(Json.Null),
    "requireddecisions" -> request.requireddecisions.map(values => Json.fromValues(values.map(value => Json.obj(
      "artifactreference" -> _artifact_json(value.artifactreference), "recordreference" -> _record_json(value.recordreference),
      "topicidentity" -> Json.fromString(value.topicidentity), "choiceidentity" -> Json.fromString(value.choiceidentity),
      "affectedtargets" -> Json.fromValues(value.affectedtargets.map(_target_json))
    )))).getOrElse(Json.Null),
    "requiredmappings" -> request.requiredmappings.map(values => Json.fromValues(values.map(value => Json.obj(
      "targetid" -> Json.fromString(value.targetid), "mappingid" -> Json.fromString(value.mappingid),
      "semantictarget" -> _target_json(value.semantictarget)
    )))).getOrElse(Json.Null)
  ))

  def bytes(value: Json): Vector[Byte] =
    (Printer.noSpacesSortKeys.print(value) + "\n").getBytes(StandardCharsets.UTF_8).toVector

  def decode(input: Vector[Byte]): Either[String, InternalModelContinuationRequest] = {
    for {
      text <- _utf8(input)
      _ <- Either.cond(!text.startsWith("\uFEFF"), (), "independent input must not have a BOM")
      json <- _parser.parse(text).left.map(_.getMessage)
      _ <- Either.cond(_unicode(json), (), "independent input must contain Unicode scalars")
      root <- _object(json, _fields)
      pkg <- InternalModelTypedControlCodec.decodePackageReference(bytes(root("packagereference").get))
      carrier <- root("carrierrevision").flatMap(_.asNumber).flatMap(number => number.toLong.filter(value => value > 0 && number.toString == value.toString))
        .toRight("carrierrevision must be a positive canonical Long integer")
      realization <- _record(root("realizationreference").get)
      scope <- _scope(root("scope").get)
      candidate <- _optional(root("candidateartifact").get, _artifact)
      diff <- _optional(root("semanticdiffartifact").get, _artifact)
      review <- _optional(root("reviewartifact").get, _artifact)
      approval <- _optional(root("approvalartifact").get, _artifact)
      basis <- _optional(root("executionbasis").get, _basis)
      human <- _optional(root("humandecision").get, value => InternalModelCandidateHumanApprovalCodec.decode(bytes(value)).map(_.approval))
      decisions <- _optional(root("requireddecisions").get, value => _vector(value, _decision))
      mappings <- _optional(root("requiredmappings").get, value => _vector(value, _mapping))
    } yield InternalModelContinuationRequest(pkg, carrier, realization, scope, candidate, diff, review, approval,
      basis, human, decisions, mappings)
  }

  private def _scope(value: Json): Either[String, InternalModelSemanticScope] = for {
    root <- _object(value, Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity"))
    component <- _string(root, "componentIdentity")
    context <- _string(root, "projectionContextIdentity")
    usecase <- _string(root, "selectedUseCaseElementIdentity")
  } yield InternalModelSemanticScope(component, context, usecase)

  private def _basis(value: Json): Either[String, InternalModelCandidateReviewExecutionBasis] = for {
    root <- _object(value, Set("rules", "providers"))
    rules <- _vector(root("rules").get, value => for {
      fields <- _object(value, Set("ruleId", "ruleVersion"))
      id <- _string(fields, "ruleId")
      version <- _string(fields, "ruleVersion")
    } yield InternalModelCandidateReviewRule(id, version))
    providers <- _vector(root("providers").get, value => for {
      fields <- _object(value, Set("providerId", "providerVersion"))
      id <- _string(fields, "providerId")
      version <- _string(fields, "providerVersion")
    } yield InternalModelCandidateReviewProvider(id, version))
  } yield InternalModelCandidateReviewExecutionBasis(rules, providers)

  private def _decision(value: Json): Either[String, InternalModelContinuationDecisionRequirement] = for {
    root <- _object(value, Set("artifactreference", "recordreference", "topicidentity", "choiceidentity", "affectedtargets"))
    artifact <- _artifact(root("artifactreference").get)
    record <- _record(root("recordreference").get)
    topic <- _string(root, "topicidentity")
    choice <- _string(root, "choiceidentity")
    targets <- _vector(root("affectedtargets").get, _target)
  } yield InternalModelContinuationDecisionRequirement(artifact, record, topic, choice, targets)

  private def _mapping(value: Json): Either[String, InternalModelContinuationMappingRequirement] = for {
    root <- _object(value, Set("targetid", "mappingid", "semantictarget"))
    target <- _string(root, "targetid")
    mapping <- _string(root, "mappingid")
    semantic <- _target(root("semantictarget").get)
  } yield InternalModelContinuationMappingRequirement(target, mapping, semantic)

  private def _target(value: Json): Either[String, InternalModelSemanticTarget] = for {
    root <- _object(value, Set("semanticIdentityKind", "semanticIdentity"))
    kind <- _string(root, "semanticIdentityKind")
    identity <- _string(root, "semanticIdentity")
  } yield InternalModelSemanticTarget(kind, identity)

  private def _artifact(value: Json): Either[String, InternalModelArtifactReference] = InternalModelTypedControlCodec.decodeArtifactReference(bytes(value))
  private def _record(value: Json): Either[String, InternalModelRecordReference] = InternalModelTypedControlCodec.decodeRecordReference(bytes(value))
  private def _artifact_json(value: InternalModelArtifactReference): Json = _json(InternalModelTypedControlCodec.encodeArtifactReference(value))
  private def _record_json(value: InternalModelRecordReference): Json = _json(InternalModelTypedControlCodec.encodeRecordReference(value))
  private def _target_json(value: InternalModelSemanticTarget): Json = Json.obj(
    "semanticIdentityKind" -> Json.fromString(value.semanticIdentityKind), "semanticIdentity" -> Json.fromString(value.semanticIdentity))
  private def _json(value: Vector[Byte]): Json = _parser.parse(new String(value.toArray, StandardCharsets.UTF_8)).toOption.get
  private def _string(value: JsonObject, key: String): Either[String, String] = value(key).flatMap(_.asString).toRight(s"$key must be a string")
  private def _object(value: Json, fields: Set[String]): Either[String, JsonObject] =
    value.asObject.filter(_.keys.toSet == fields).toRight("independent input requires the exact object fields")
  private def _optional[A](value: Json, decode: Json => Either[String, A]): Either[String, Option[A]] =
    if (value.isNull) Right(None) else decode(value).map(Some(_))
  private def _vector[A](value: Json, decode: Json => Either[String, A]): Either[String, Vector[A]] =
    value.asArray.toRight("independent input requires an array").flatMap(_.foldLeft[Either[String, Vector[A]]](Right(Vector.empty)) {
      (result, item) => for { retained <- result; next <- decode(item) } yield retained :+ next
    })
  private def _utf8(value: Vector[Byte]): Either[String, String] = try {
    Right(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value.toArray)).toString)
  } catch { case NonFatal(error) => Left(error.getMessage) }
  private def _unicode(value: Json): Boolean = {
    def _string_(text: String): Boolean = {
      var index = 0
      var valid = true
      while (index < text.length && valid) {
        val char = text.charAt(index)
        if (Character.isHighSurrogate(char)) {
          valid = index + 1 < text.length && Character.isLowSurrogate(text.charAt(index + 1))
          index += 2
        } else {
          valid = !Character.isLowSurrogate(char)
          index += 1
        }
      }
      valid
    }
    value.asString.forall(_string_) && value.asArray.forall(_.forall(_unicode)) &&
      value.asObject.forall(_.toVector.forall { case (key, item) => _string_(key) && _unicode(item) })
  }
}
