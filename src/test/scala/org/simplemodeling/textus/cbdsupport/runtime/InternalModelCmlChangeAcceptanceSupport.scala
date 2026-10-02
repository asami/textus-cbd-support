package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.nio.ByteBuffer
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}
import java.util.Base64
import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/**
 * Test-only ordinary payload, explicit owner graph and external-observation fixtures.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  3, 2026
 */
private[runtime] object InternalModelCmlChangeAcceptanceSupport {
  import InternalModelCmlChangePostValidation.*
  import InternalModelCandidateHumanApprovalValidatorSpec.{FixtureOptions, recordReference, reference, admit}
  private val _core = InternalModelContinuationFixture
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  val realizationArtifact: InternalModelArtifactReference = reference("realization-main", 503L, InternalModelArtifactRole.Realization)
  val realizationRecord: InternalModelRecordReference = recordReference("realization-order", 509L)
  val continuityArtifact: InternalModelArtifactReference = reference("projection-main", 521L, InternalModelArtifactRole.Projection)
  val continuityRecord: InternalModelRecordReference = recordReference("binding-order", 523L)
  val modelSnapshot: InternalModelArtifactReference = reference("snapshot-model", 557L, InternalModelArtifactRole.SourceSnapshot)
  val alphaSnapshot: InternalModelArtifactReference = reference("snapshot-cml-alpha", 541L, InternalModelArtifactRole.SourceSnapshot)
  val betaSnapshot: InternalModelArtifactReference = reference("snapshot-cml-beta", 547L, InternalModelArtifactRole.SourceSnapshot)
  val families: Vector[String] = Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection",
    "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
  final case class Refreshed(owner: OwnerEvidence, resumebytes: Vector[Byte])

  def proposedBytes(targetId: String, mode: String): Vector[Byte] = {
    require(Set("success", "lint-failure").contains(mode), "probe mode must be explicit")
    val negative = targetId == "target-alpha" && mode == "lint-failure"
    val heading = if (negative) Vector("# ENTITY", "## AlphaEntity") else
      Vector("# VALUE", if (targetId == "target-alpha") "## AlphaValue" else "## BetaValue")
    (heading ++ Vector("### Attribute", "| name | type | multiplicity |", "|------+--------+--------------|",
      if (negative) "| name | string | 1 |" else "| value | string | 1 |")).mkString("", "\n", "\n").getBytes(StandardCharsets.UTF_8).toVector
  }

  /** Replace only ordinary candidate payload before fresh real approval/review admission. */
  def withFixture(mode: String = "success", options: FixtureOptions = InternalModelDurableHandoffFixture.approvedOptions)(
    body: InternalModelCmlChangeFixture.Fixture => Unit
  ): Unit = InternalModelCmlChangeFixture.withFixture(options) { fixture =>
    val admission = fixture.request.currentreview.semanticDiffAdmission.candidateAdmission
    val candidate = admission.projection.copy(targets = admission.projection.targets.map { target =>
      val bytes = proposedBytes(target.targetId, mode)
      target.copy(proposedContent = target.proposedContent.copy(byteLength = bytes.size.toLong,
        rawBytesBase64 = Base64.getEncoder.encodeToString(bytes.toArray)))
    })
    _core.replaceArtifact(fixture.root, "candidate-main", InternalModelCandidateCmlProjectionCodec.encode(candidate).toVector)
    body(_readmit(fixture, fixture.root))
  }

  private def _readmit(fixture: InternalModelCmlChangeFixture.Fixture, root: Path): InternalModelCmlChangeFixture.Fixture = {
    val normalizedroot = root.toAbsolutePath.normalize
    val evidence = admit(normalizedroot, fixture.data)
    val handoff = InternalModelPackageValidator.verifiedContinuation(normalizedroot).toOption.get
    val authority = fixture.request.mutationauthority.get.copy(projectroot = normalizedroot.toString)
    fixture.copy(root = normalizedroot, handoff = handoff, request = fixture.request.copy(originalapproval = evidence.approval,
      originalsnapshots = evidence.snapshots, currentreview = evidence.review, mutationauthority = Some(authority)))
  }

  /** New exact probe root is retained, while the inherited temporary unit fixture cleans itself. */
  def copyForProbe(fixture: InternalModelCmlChangeFixture.Fixture, workDir: Path): InternalModelCmlChangeFixture.Fixture = {
    val parent = Path.of("target/internal-model-cml-change-acceptance").toAbsolutePath.normalize
    require(workDir.isAbsolute && workDir.normalize == workDir && workDir.getParent == parent, "probe workdir must be one exact declared acceptance run")
    require(!Files.exists(workDir, LinkOption.NOFOLLOW_LINKS), "probe workdir must be fresh")
    val root = workDir.resolve("root")
    Files.createDirectories(workDir)
    InternalModelDurableHandoffFixture.copyPackage(fixture.root, root, workDir)
    Vector("cml/alpha.cml", "cml/beta.cml").foreach { path =>
      val source = fixture.root.resolve(path)
      require(Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS), "declared baseline target must be an actual regular file")
      val destination = root.resolve(path)
      Files.createDirectories(destination.getParent)
      Files.copy(source, destination)
    }
    _readmit(fixture, root)
  }

  /** Explicit test source declarations are independent of stdout and CML interpretation. */
  def refreshOwner(fixture: InternalModelCmlChangeFixture.Fixture,
    application: InternalModelCmlChangeApplication.ApplicationReport, label: String = "refresh 日本語",
    carrierRevision: Long = 563L, realizationRevision: Long = 503L, realizationRecordRevision: Long = 509L,
    bindingRevision: Long = 521L, bindingRecordRevision: Long = 523L, modelRevision: Long = 557L,
    alphaRevision: Long = 541L, betaRevision: Long = 547L, modelSourceRevision: String = "model-owner-C1",
    includeOptionalSnapshot: Boolean = false): Refreshed = {
    require(application.disposition == InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation, "owner refresh requires actual complete application")
    val root = fixture.root
    val packagepath = _core.packageRoot(root)
    val resumebytes = Files.readAllBytes(packagepath.resolve("resume.yaml")).toVector
    val plan = application.gate.plan.get
    val scope = plan.scope
    val realizationartifact = reference("realization-main", realizationRevision, InternalModelArtifactRole.Realization)
    val realizationrecord = recordReference("realization-order", realizationRecordRevision)
    val continuityartifact = reference("projection-main", bindingRevision, InternalModelArtifactRole.Projection)
    val continuityrecord = recordReference("binding-order", bindingRecordRevision)
    val modelsnapshot = reference("snapshot-model", modelRevision, InternalModelArtifactRole.SourceSnapshot)
    val targetbaselines = Vector(TargetBaseline("target-alpha", reference("snapshot-cml-alpha", alphaRevision, InternalModelArtifactRole.SourceSnapshot)),
      TargetBaseline("target-beta", reference("snapshot-cml-beta", betaRevision, InternalModelArtifactRole.SourceSnapshot)))
    val oldmodel = application.gate.continuation.state.continuity.realization.sourceReferences.find(_.snapshotReference.artifactId.value == "snapshot-model").get.source
    val modelsource = oldmodel.copy(revision = Some(modelSourceRevision))
    val scopejson = Json.obj("componentIdentity" -> Json.fromString(scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity))
    def _adapt_(value: Json): Json = value.arrayOrObject(value, values => Json.fromValues(values.map(_adapt_)), fields => {
      val nested = fields.mapValues(_adapt_)
      if (nested("artifactId").flatMap(_.asString).contains("snapshot-model")) _reference_json(modelsnapshot)
      else if (nested("authority").isDefined && nested("identity").isDefined && nested("locator").isDefined) _source_json(modelsource)
      else Json.fromJsonObject(nested)
    })
    val richrealization = _adapt_(_read(packagepath.resolve("realizations/main.json")))
    val richmodel = _adapt_(_read(packagepath.resolve("snapshots/model.json")))
    val richbinding = _read(packagepath.resolve("projections/continuity.json"))
    var mergedrealization = richrealization
    var mergedmodel = richmodel
    var mergedbinding = richbinding
    InternalModelContinuationFixture.withFixture(label) { coreroot =>
      val corepath = _core.packageRoot(coreroot)
      val corerealization = _prefix(_read(corepath.resolve("realizations/main.json")))
      val coremodel = _prefix(_read(corepath.resolve("snapshots/model.json")))
      val corebinding = _prefix(_read(corepath.resolve("projections/main.json")))
      val selectedassertion = "post-a-e-usecase-kind-use-case"
      val selectedreference = "post-ref-e-usecase-kind-use-case"
      val arrays = Vector("canonicalAssertions" -> "assertionId", "enrichmentAssertions" -> "assertionId", "conditions" -> "conditionId",
        "elements" -> "identity", "relationships" -> "identity", "sourceReferences" -> "referenceId", "successorLinks" -> "priorIdentity")
      arrays.foreach { case (field, key) =>
        val extra = _values(corerealization, field).filterNot { value =>
          (field == "elements" && _text(value, "identity") == "e-usecase") ||
            (field == "canonicalAssertions" && _text(value, key) == selectedassertion) ||
            (field == "sourceReferences" && _text(value, key) == selectedreference)
        }.map(_adapt_)
        mergedrealization = _set(mergedrealization, field, Json.fromValues((_values(richrealization, field) ++ extra).sortBy(value => _text(value, key))))
      }
      val richfacts = richmodel.hcursor.downField("basis").get[Vector[Json]]("facts").toOption.get
      val corefacts = coremodel.hcursor.downField("basis").get[Vector[Json]]("facts").toOption.get
        .filterNot(value => _text(value, "semanticIdentityKind") == "element" && _text(value, "semanticIdentity") == "e-usecase")
        .map(value => _set(_set(value, "componentIdentity", Json.fromString(scope.componentIdentity)), "projectionContextIdentity", Json.fromString(scope.projectionContextIdentity)))
      val facts = (richfacts ++ corefacts).sortBy(value => (_text(value, "semanticIdentityKind"), _text(value, "semanticIdentity"), _text(value, "sourceAnchor")))
      mergedmodel = richmodel.mapObject(_.add("basis", richmodel.hcursor.get[Json]("basis").toOption.get.mapObject(_.add("facts", Json.fromValues(facts)))))
      val views = _values(richbinding, "views").map { view =>
        val family = _text(view, "family")
        val extra = _values(corebinding, "views").find(value => _text(value, "family") == family).toVector.flatMap(_values(_, "records"))
          .filterNot(value => _text(value, "recordKind") == "element" && _text(value, "semanticIdentity") == "e-usecase")
        val records = (_values(view, "records") ++ extra).sortBy(value => (_text(value, "recordKind"), _text(value, "semanticIdentity"), _text(value, "viewRole")))
        _set(view, "records", Json.fromValues(records))
      }
      mergedbinding = _set(richbinding, "views", Json.fromValues(views))
    }
    mergedrealization = _set(_set(mergedrealization, "realizationReference", _record_json(realizationrecord)), "scope", scopejson)
    mergedbinding = _set(_set(_set(mergedbinding, "bindingReference", _record_json(continuityrecord)),
      "realizationArtifactReference", _reference_json(realizationartifact)), "scope", scopejson)
    _write(packagepath.resolve("snapshots/model.json"), mergedmodel)
    _write(packagepath.resolve("realizations/main.json"), mergedrealization)
    _write(packagepath.resolve("projections/continuity.json"), mergedbinding)
    targetbaselines.foreach { baseline =>
      val target = plan.targets.find(_.target.targetId == baseline.targetid).get
      val source = target.target.source.copy(revision = Some(target.nextsourcerevision))
      val path = if (baseline.targetid == "target-alpha") "snapshots/cml-alpha.json" else "snapshots/cml-beta.json"
      val currentbytes = Files.readAllBytes(root.resolve(target.target.projectRelativePath)).toVector
      val snapshot = Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"),
        "source" -> _source_json(source), "basis" -> Json.obj("projectRelativePath" -> Json.fromString(target.target.projectRelativePath),
          "byteLength" -> Json.fromLong(currentbytes.size.toLong), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(currentbytes.toArray))))
      _write(packagepath.resolve(path), snapshot)
    }
    val snapshotreferences = fixture.request.originalsnapshots.map { snapshot =>
      val selected = if (snapshot.reference.artifactId.value == "snapshot-model") modelsnapshot else
        targetbaselines.find(_.snapshotreference.artifactId == snapshot.reference.artifactId).map(_.snapshotreference).getOrElse(snapshot.reference)
      snapshot.reference -> selected
    }.toMap
    val snapshots = fixture.request.originalsnapshots.map { snapshot =>
      val selected = snapshotreferences(snapshot.reference)
      _core.artifact(selected.artifactId.value, selected.artifactRevision.value, snapshot.path, "source-snapshot", required = snapshot.required)
    }
    val referenceupdates = snapshotreferences ++ Map(plan.realizationartifactreference -> realizationartifact,
      plan.continuityartifactreference -> continuityartifact)
    val selectedids = snapshotreferences.values.map(_.artifactId).toSet ++ Set(realizationartifact.artifactId, continuityartifact.artifactId)
    val inherited = fixture.handoff.artifacts.map(_.context).filterNot(context => selectedids.contains(context.reference.artifactId)).map { context =>
      _core.artifact(context.reference.artifactId.value, context.reference.artifactRevision.value, context.path, context.reference.role.wireValue,
        context.dependencies.map(dependency => referenceupdates.getOrElse(dependency, dependency)), context.required)
    }
    val optional = if (includeOptionalSnapshot && !fixture.handoff.artifacts.exists(_.context.reference.artifactId.value == "snapshot-optional"))
      Vector(_core.artifact("snapshot-optional", 71L, "snapshots/optional.json", "source-snapshot", required = false))
      else Vector.empty
    val entries = snapshots ++ Vector(
      _core.artifact(realizationartifact.artifactId.value, realizationartifact.artifactRevision.value, "realizations/main.json", "realization", Vector(modelsnapshot)),
      _core.artifact(continuityartifact.artifactId.value, continuityartifact.artifactRevision.value, "projections/continuity.json", "projection", Vector(realizationartifact))) ++ inherited ++ optional
    _core.replaceInventory(root, entries, plan.packagereference.packageId, plan.packagereference.projectId.value, carrierRevision)
    val retained = fixture.request.livesources.filter { case (ref, _) => !Set("snapshot-model", "snapshot-cml-alpha", "snapshot-cml-beta").contains(ref.artifactId.value) }
    val cmlinputs = targetbaselines.map { baseline =>
      val target = plan.targets.find(_.target.targetId == baseline.targetid).get
      baseline.snapshotreference -> InternalModelPackageFreshnessInput.CmlObserved(target.target.source.authority,
        target.target.source.identity, Some(target.nextsourcerevision), target.target.projectRelativePath)
    }.toMap
    val modelinput = InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(
      modelsource.authority, modelsource.identity, modelsource.revision, Vector.empty, None))
    Refreshed(OwnerEvidence(plan.packagereference, scope, realizationartifact, realizationrecord, continuityartifact,
      continuityrecord, targetbaselines, retained ++ cmlinputs + (modelsnapshot -> modelinput)), resumebytes)
  }

  /** Prefix only declared core identity/anchor links; sequence-key values remain source-owned. */
  private def _prefix(value: Json): Json = {
    def _id_(text: String): String = if (text == "e-usecase") text else "post-" + text
    def _walk_(item: Json, key: String): Json = item.arrayOrObject(item.asString.fold(item) { text =>
      val identityfields = Set("identity", "semanticIdentity", "relatedSemanticIdentity", "sourceElementIdentity", "targetElementIdentity", "affectedIdentity")
      val linkfields = Set("assertionId", "referenceId", "sourceReferenceId", "sourceAnchor", "conditionId", "sequenceAssertionId",
        "canonicalAssertionIds", "enrichmentAssertionIds", "conditionIds")
      if (identityfields.contains(key) || linkfields.contains(key)) Json.fromString(_id_(text))
      else if (key == "content" && text.startsWith("association:")) {
        val pieces = text.split(":", -1).toVector
        Json.fromString((pieces.take(3) :+ _id_(pieces(3))).mkString(":"))
      } else item
    }, values => Json.fromValues(values.map(value => _walk_(value, key))), fields =>
      Json.fromJsonObject(fields.mapValues(identity).toIterable.foldLeft(io.circe.JsonObject.empty) { case (result, (name, nested)) => result.add(name, _walk_(nested, name)) }))
    // Source identity is replaced explicitly by _adapt_, never by semantic identity prefixing.
    _walk_(value, "")
  }

  /** Synthetic terminal observations exercise admission only and are never CLI integration evidence. */
  def syntheticCommands(root: Path, application: InternalModelCmlChangeApplication.ApplicationReport): Vector[CommandObservation] =
    application.gate.plan.get.targets.zip(Vector(recordReference("cozy-alpha", 613L), recordReference("cozy-beta", 617L))).map { case (target, command) =>
      CommandObservation(command, application.applicationreference, root.toString, target.target.targetId, target.target.projectRelativePath,
        target.target.source.authority, target.target.source.identity, target.nextsourcerevision, "0.3.3-SNAPSHOT",
        Vector("--runtime", "0.3.3-SNAPSHOT", "lint", "cml", root.resolve(target.target.projectRelativePath).normalize.toString, "--format", "json"),
        CommandOutcome.Terminal(0, "{\"findings\":[]}", "synthetic unit observation"))
    }

  def projectionCounts(continuity: InternalModelProjectionContinuity): Vector[Int] = Vector(
    continuity.monoKoto.subjects.size, continuity.useCaseCommunication.subjects.size, continuity.entityModel.subjects.size,
    continuity.eventModel.subjects.size + continuity.eventModel.assertions.size, continuity.structureView.relations.size,
    continuity.classificationView.relationships.size + continuity.classificationView.dimensions.size,
    continuity.workflow.subjects.size + continuity.workflow.flows.size, continuity.stateMachine.subjects.size + continuity.stateMachine.transitions.size)

  def requiredUnchanged(report: Report): Boolean = report.freshness.exists { freshness =>
    val required = report.application.gate.continuation.state.packageContext.artifacts.filter(entry => entry.required && entry.reference.role == InternalModelArtifactRole.SourceSnapshot)
    required.forall { old => freshness.entries.find(_.reference.artifactId == old.reference.artifactId).exists { entry => entry.result match {
      case InternalModelPackageFreshnessResult.Compared(value) => value.status == InternalModelSnapshotFreshnessStatus.Unchanged && value.missingDimensionNames.isEmpty
      case _ => false
    }}}
  }

  /** Closed test transport; this is not a production serializer or command receipt. */
  def readObservation(path: Path): Either[String, CommandObservation] = {
    val bytes = Files.readAllBytes(path)
    val decoded = scala.util.Try(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString).toEither.left.map(_ => "observation must be UTF-8")
    for {
      text <- decoded
      value <- _json_parser.parse(text).left.map(_ => "observation JSON must have unique members")
      fields <- value.asObject.toRight("observation must be an object")
      _ <- Either.cond(fields.keys.toSet == Set("schema", "commandReference", "applicationReference", "projectRoot", "targetId", "projectRelativePath",
        "sourceAuthority", "sourceIdentity", "sourceRevision", "runtimeVersion", "argv", "outcome"), (), "observation fields must be closed")
      schema <- _string(fields, "schema")
      _ <- Either.cond(schema == "textus.cml-change-command-observation.v1", (), "observation schema must match")
      command <- _decode_record(fields, "commandReference")
      application <- _decode_record(fields, "applicationReference")
      root <- _string(fields, "projectRoot")
      target <- _string(fields, "targetId")
      path <- _string(fields, "projectRelativePath")
      authority <- _string(fields, "sourceAuthority")
      identity <- _string(fields, "sourceIdentity")
      revision <- _string(fields, "sourceRevision")
      runtime <- _string(fields, "runtimeVersion")
      argv <- fields("argv").toRight("argv missing").flatMap(_.as[Vector[String]].left.map(_ => "argv must be strings"))
      outcomevalue <- fields("outcome").flatMap(_.asObject).toRight("outcome must be an object")
      kind <- _string(outcomevalue, "kind")
      outcome <- kind match {
        case "terminal" => for {
          _ <- Either.cond(outcomevalue.keys.toSet == Set("kind", "exitCode", "stdout", "stderr"), (), "terminal outcome must be closed")
          exitcode <- outcomevalue("exitCode").flatMap(_.asNumber).flatMap(_.toInt).toRight("exitCode must be an Int")
          stdout <- _string(outcomevalue, "stdout")
          stderr <- _string(outcomevalue, "stderr")
        } yield CommandOutcome.Terminal(exitcode, stdout, stderr)
        case "missing" | "indeterminate" => for {
          _ <- Either.cond(outcomevalue.keys.toSet == Set("kind", "reason"), (), "nonterminal outcome must be closed")
          reason <- _string(outcomevalue, "reason")
        } yield if (kind == "missing") CommandOutcome.Missing(reason) else CommandOutcome.Indeterminate(reason)
        case _ => Left("outcome kind is unsupported")
      }
    } yield CommandObservation(command, application, root, target, path, authority, identity, revision, runtime, argv, outcome)
  }

  def writeOnce(path: Path, value: Json): Unit = {
    Files.write(path, _core.canonical(value).toArray, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    ()
  }
  def recordJson(value: InternalModelRecordReference): Json = _record_json(value)
  private def _decode_record(fields: JsonObject, key: String): Either[String, InternalModelRecordReference] =
    fields(key).toRight(s"$key missing").flatMap(value => InternalModelTypedControlCodec.decodeRecordReference(_core.canonical(value)))
  private def _string(fields: JsonObject, key: String): Either[String, String] = fields(key).flatMap(_.asString).toRight(s"$key must be a string")
  private def _record_json(value: InternalModelRecordReference): Json = _core.json(InternalModelTypedControlCodec.encodeRecordReference(value))
  private def _reference_json(value: InternalModelArtifactReference): Json = _core.referenceJson(value)
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority),
    "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null),
    "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null))
  private def _read(path: Path): Json = _core.json(Files.readAllBytes(path).toVector)
  private def _write(path: Path, value: Json): Unit = _core.write(path, _core.canonical(value))
  private def _values(value: Json, key: String): Vector[Json] = value.hcursor.get[Vector[Json]](key).toOption.get
  private def _text(value: Json, key: String): String = value.hcursor.get[String](key).toOption.get
  private def _set(value: Json, key: String, replacement: Json): Json = value.mapObject(_.add(key, replacement))
}
