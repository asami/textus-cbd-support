package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 28, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
/** Parses and encodes the closed projection-binding artifact without projection reconstruction. */
private[runtime] object InternalModelProjectionBindingCodec {
  private val _root_fields = Set("bindingReference", "profile", "realizationArtifactReference", "schemaVersion", "scope", "views")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _view_fields = Set("family", "records")
  private val _record_fields = Set(
    "canonicalAssertionIds", "conditionIds", "enrichmentAssertionIds", "recordKind", "semanticIdentity", "sequenceAssertionId", "viewRole"
  )
  private val _gap_record_fields = _record_fields + "requestedFieldOrScope"
  private val _families = Vector(
    "MonoKotoProjection",
    "UseCaseCommunicationProjection",
    "EntityModelProjection",
    "EventModelProjection",
    "StructureViewProjection",
    "ClassificationViewProjection",
    "WorkflowProjection",
    "StateMachineProjection"
  )
  private[runtime] val roles = Map(
    "MonoKotoProjection" -> (Set("Mono", "Koto"), Set("StructuralDomain", "BehavioralTemporal")),
    "UseCaseCommunicationProjection" -> (Set("use-case"), Set("Actor", "Goal", "Trigger", "Postcondition", "DomainElement", "Collaborator", "RealizingWorkflow", "MonoKoto", "UseCaseCommunicationFlow", "UseCaseCommunicationFlowStep")),
    "EntityModelProjection" -> (Set("EntityModelEntity", "EntityModelValue", "EntityModelAggregate"), Set("IdentityMetadata", "OwnershipMetadata", "LifecycleMetadata", "AggregateBoundaryMetadata")),
    "EventModelProjection" -> (Set("EventModelCommand", "EventModelEvent"), Set("EventModelCausalAssertion", "EventModelConsequenceAssertion", "EventModelAffectedDomainElementAssertion", "EventModelGeneratedStateEffectAssertion")),
    "StructureViewProjection" -> (Set.empty[String], Set("StructureComposition", "StructureAggregation", "StructureAssociation", "StructureContainment", "StructureOwnership", "IndependentExistence", "Reassignment", "DeletionLifecycle", "Cardinality", "Navigability")),
    "ClassificationViewProjection" -> (Set("ClassificationPowertypeDimension"), Set("ClassificationGeneralization", "ClassificationSpecialization", "ClassificationTrait", "ClassificationCategory", "ClassificationDimensionValue", "ClassificationDimensionQualifier", "ClassificationDimensionExclusivity", "ClassificationDimensionCoverage", "ClassificationDimensionMembership")),
    "WorkflowProjection" -> (Set("WorkflowProjectionWorkflow", "WorkflowProjectionActivity", "WorkflowProjectionParticipant", "WorkflowProjectionDomainElement", "WorkflowProjectionOperation", "WorkflowProjectionEvent", "WorkflowProjectionPublishedStateEffect"), Set("WorkflowProjectionFlowRelation")),
    "StateMachineProjection" -> (Set("StateMachineProjectionStateMachine", "StateMachineProjectionState", "StateMachineProjectionTrigger", "StateMachineProjectionGuard", "StateMachineProjectionAction", "StateMachineProjectionActivity", "StateMachineProjectionOperation", "StateMachineProjectionEvent", "StateMachineProjectionRule"), Set("StateMachineProjectionTransitionRelation", "StateMachineProjectionTransitionAdjunctRelation"))
  )
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(
    projection: InternalModelVerifiedProjection,
    selectedRealizationArtifactReference: InternalModelArtifactReference,
    realization: InternalModelSemanticRealization
  ): Either[String, InternalModelProjectionBinding] =
    for {
      _ <- Either.cond(projection != null && realization != null, (), "projection capture and realization must be present")
      _ <- _captured_metadata(projection)
      _ <- _reference_metadata(selectedRealizationArtifactReference)
      _ <- Either.cond(selectedRealizationArtifactReference.role == InternalModelArtifactRole.Realization, (), "selected realization reference role must be realization")
      bytes = projection.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "projection binding bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "projection binding bytes")
      json <- _json_parser.parse(content).left.map(_ => "projection binding bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("projection binding root must be an object")
      _ <- _closed_fields(root, _root_fields, "projection binding root")
      profile <- _nonempty_string(root, "profile", "projection binding root")
      schema <- _nonempty_string(root, "schemaVersion", "projection binding root")
      _ <- Either.cond(
        realization.profile == "ccdm-realization-v3" && realization.schemaVersion == "3.0" &&
          profile == "ccdm-projection-binding-v3" && schema == "3.0",
        (),
        "projection binding profile/schemaVersion must pair exactly with the selected realization version"
      )
      bindingvalue <- root("bindingReference").toRight("projection bindingReference is missing")
      _ <- _record_id(bindingvalue, "bindingReference")
      bindingreference <- InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(bindingvalue))
      realizationvalue <- root("realizationArtifactReference").toRight("projection realizationArtifactReference is missing")
      realizationreference <- InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(realizationvalue))
      _ <- Either.cond(realizationreference == selectedRealizationArtifactReference && projection.dependencies.contains(realizationreference), (), "projection binding realizationArtifactReference is not the exact selected projection dependency")
      scope <- _scope(root)
      _ <- Either.cond(
        scope == InternalModelProjectionBindingScope(realization.scope.componentIdentity, realization.scope.projectionContextIdentity, realization.scope.selectedUseCaseElementIdentity),
        (),
        "projection binding scope does not equal selected realization scope"
      )
      views <- _views(root)
    } yield InternalModelProjectionBinding(bindingreference, profile, schema, realizationreference, scope, views)

  def encode(binding: InternalModelProjectionBinding): Array[Byte] =
    _json_bytes(Json.obj(
      "bindingReference" -> Json.obj(
        "recordId" -> Json.fromString(binding.bindingReference.recordId.value),
        "recordRevision" -> Json.fromLong(binding.bindingReference.recordRevision.value)
      ),
      "profile" -> Json.fromString(binding.profile),
      "realizationArtifactReference" -> Json.obj(
        "artifactId" -> Json.fromString(binding.realizationArtifactReference.artifactId.value),
        "artifactRevision" -> Json.fromLong(binding.realizationArtifactReference.artifactRevision.value),
        "role" -> Json.fromString(binding.realizationArtifactReference.role.wireValue)
      ),
      "schemaVersion" -> Json.fromString(binding.schemaVersion),
      "scope" -> Json.obj(
        "componentIdentity" -> Json.fromString(binding.scope.componentIdentity),
        "projectionContextIdentity" -> Json.fromString(binding.scope.projectionContextIdentity),
        "selectedUseCaseElementIdentity" -> Json.fromString(binding.scope.selectedUseCaseElementIdentity)
      ),
      "views" -> Json.fromValues(binding.views.map(_view_json))
    )).toArray

  private def _captured_metadata(projection: InternalModelVerifiedProjection): Either[String, Unit] =
    for {
      _ <- _reference_metadata(projection.reference)
      _ <- Either.cond(projection.reference.role == InternalModelArtifactRole.Projection, (), "selected artifact role must be projection")
      _ <- Either.cond(projection.path != null && projection.path.nonEmpty && projection.bytes != null && projection.dependencies != null, (), "projection capture metadata, bytes and dependencies must be present")
      _ <- projection.dependencies.foldLeft[Either[String, Unit]](Right(())) { (result, reference) =>
        result.flatMap(_ => _reference_metadata(reference))
      }
      ids = projection.dependencies.map(_.artifactId.value)
      _ <- Either.cond(ids.distinct.size == ids.size && ids == ids.sortWith((left, right) => _compare_text(left, right) < 0), (), "projection dependency IDs must be sorted and unique")
      _ <- Either.cond(!ids.contains(projection.reference.artifactId.value), (), "projection must not depend on itself")
    } yield ()

  private def _reference_metadata(reference: InternalModelArtifactReference): Either[String, Unit] =
    for {
      _ <- Either.cond(reference != null && reference.role != null, (), "artifact reference and role must be present")
      _ <- InternalModelArtifactId.from(reference.artifactId.value)
      _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
    } yield ()

  private def _scope(root: JsonObject): Either[String, InternalModelProjectionBindingScope] =
    for {
      value <- root("scope").toRight("projection binding scope is missing")
      objectvalue <- _object(value, "projection binding scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "projection binding scope")
      componentidentity <- _nonempty_string(objectvalue, "componentIdentity", "projection binding scope")
      contextidentity <- _nonempty_string(objectvalue, "projectionContextIdentity", "projection binding scope")
      usecaseidentity <- _nonempty_string(objectvalue, "selectedUseCaseElementIdentity", "projection binding scope")
    } yield InternalModelProjectionBindingScope(componentidentity, contextidentity, usecaseidentity)

  private def _views(root: JsonObject): Either[String, Vector[InternalModelProjectionBindingView]] =
    for {
      values <- _array(root, "views", "projection binding root")
      views <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelProjectionBindingView]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          view <- _view(value, index)
        } yield collected :+ view
      }
      _ <- Either.cond(views.map(_.family) == _families, (), "projection binding views must contain the exact eight families in canonical order")
    } yield views

  private def _view(value: Json, index: Int): Either[String, InternalModelProjectionBindingView] =
    for {
      objectvalue <- _object(value, s"projection binding view $index")
      _ <- _closed_fields(objectvalue, _view_fields, s"projection binding view $index")
      family <- _nonempty_string(objectvalue, "family", s"projection binding view $index")
      _ <- Either.cond(roles.contains(family), (), s"projection binding view $index has an unsupported family")
      values <- _array(objectvalue, "records", s"projection binding view $family")
      records <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelProjectionBindingRecord]]](Right(Vector.empty)) { case (result, (entry, recordindex)) =>
        for {
          collected <- result
          record <- _record(entry, family, recordindex)
        } yield collected :+ record
      }
      _ <- _strictly_sorted(records.map(record => Vector(record.recordKind, record.semanticIdentity, record.viewRole)), s"projection binding view $family records")
    } yield InternalModelProjectionBindingView(family, records)

  private def _record(value: Json, family: String, index: Int): Either[String, InternalModelProjectionBindingRecord] =
    for {
      objectvalue <- _object(value, s"projection binding $family record $index")
      recordkind <- _nonempty_string(objectvalue, "recordKind", s"projection binding $family record $index")
      _ <- Either.cond(Set("element", "relationship", "gap").contains(recordkind), (), s"projection binding $family record $index has an invalid recordKind")
      _ <- _closed_fields(objectvalue, if recordkind == "gap" then _gap_record_fields else _record_fields, s"projection binding $family record $index")
      semanticidentity <- _nonempty_string(objectvalue, "semanticIdentity", s"projection binding $family record $index")
      viewrole <- _nonempty_string(objectvalue, "viewRole", s"projection binding $family record $index")
      canonicalids <- _sorted_strings(objectvalue, "canonicalAssertionIds", s"projection binding $family record $index")
      enrichmentids <- _sorted_strings(objectvalue, "enrichmentAssertionIds", s"projection binding $family record $index")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"projection binding $family record $index")
      sequenceid <- _nullable_nonempty_string(objectvalue, "sequenceAssertionId", s"projection binding $family record $index")
      requested <- if recordkind == "gap" then _nonempty_string(objectvalue, "requestedFieldOrScope", s"projection binding $family record $index").map(Some(_)) else Right(None)
    } yield InternalModelProjectionBindingRecord(recordkind, semanticidentity, viewrole, canonicalids, enrichmentids, conditionids, sequenceid, requested)

  private def _view_json(view: InternalModelProjectionBindingView): Json =
    Json.obj(
      "family" -> Json.fromString(view.family),
      "records" -> Json.fromValues(view.records.map(_record_json))
    )

  private def _record_json(record: InternalModelProjectionBindingRecord): Json = {
    val fields = Vector(
      "canonicalAssertionIds" -> Json.fromValues(record.canonicalAssertionIds.map(Json.fromString)),
      "conditionIds" -> Json.fromValues(record.conditionIds.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(record.enrichmentAssertionIds.map(Json.fromString)),
      "recordKind" -> Json.fromString(record.recordKind)
    ) ++ record.requestedFieldOrScope.toVector.map("requestedFieldOrScope" -> Json.fromString(_)) ++ Vector(
      "semanticIdentity" -> Json.fromString(record.semanticIdentity),
      "sequenceAssertionId" -> record.sequenceAssertionId.map(Json.fromString).getOrElse(Json.Null),
      "viewRole" -> Json.fromString(record.viewRole)
    )
    Json.obj(fields*)
  }

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _record_id(value: Json, label: String): Either[String, Unit] = {
    val encoder = StandardCharsets.UTF_8.newEncoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
    for {
      objectvalue <- _object(value, label)
      recordid <- objectvalue("recordId").flatMap(_.asString).toRight(s"$label recordId must be a JSON string")
      _ <- Either.cond(!recordid.isBlank && encoder.canEncode(recordid), (), s"$label recordId must be nonblank valid Unicode")
    } yield ()
  }

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).filter(_.nonEmpty).toRight(s"$label $key must be a nonempty string")

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key).toRight(s"$label $key is missing").flatMap {
      case value if value.isNull => Right(None)
      case value => value.asString.filter(_.nonEmpty).map(Some(_)).toRight(s"$label $key must be null or a nonempty string")
    }

  private def _sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    for {
      values <- _array(objectvalue, key, label)
      strings <- values.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (result, value) =>
        for {
          collected <- result
          stringvalue <- value.asString.filter(_.nonEmpty).toRight(s"$label $key must contain only nonempty strings")
        } yield collected :+ stringvalue
      }
      _ <- Either.cond(strings == strings.sortWith((left, right) => _compare_text(left, right) < 0) && strings.distinct.size == strings.size, (), s"$label $key must be sorted and unique")
    } yield strings

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] = {
    val ordered = values.sliding(2).forall {
      case Vector(left, right) => _compare_tuple(left, right) < 0
      case _ => true
    }
    Either.cond(ordered, (), s"$label are not in canonical tuple order or contain a duplicate")
  }

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

  private def _json_bytes(json: Json): Vector[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8).toVector
}
