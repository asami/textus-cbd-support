package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import io.circe.Json

/**
 * Explicit rich/all-eight test-owner inputs, composed through the existing owners.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
private[runtime] object InternalModelEndToEndFixture {
  import InternalModelCandidateHumanApprovalValidatorSpec.*
  private val _core = InternalModelContinuationFixture
  private val _acceptance = InternalModelCmlChangeAcceptanceSupport
  private val _arrays = Vector("canonicalAssertions" -> "assertionId", "enrichmentAssertions" -> "assertionId",
    "conditions" -> "conditionId", "elements" -> "identity", "relationships" -> "identity",
    "sourceReferences" -> "referenceId", "successorLinks" -> "priorIdentity")

  /** Factories run only in a child cwd below the explicitly selected E2E run. */
  def produce(workRoot: Path, label: String, reverseInputs: Boolean, carrierRevision: Long, cmlMode: String): Json = {
    val root = workRoot.resolve("project")
    require(!Files.exists(root), "producer project must be fresh")
    var output = Json.Null
    _acceptance.withFixture(cmlMode, options = InternalModelDurableHandoffFixture.approvedOptions.copy(
      carrierrevision = carrierRevision, reviewstate = "approved",
      extraartifact = Option.when(carrierRevision != 43L)((reference("control-only", 157L, InternalModelArtifactRole.Validation),
        "validation/control-only.txt", "explicit control-only evidence\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), Vector.empty)))) { rich =>
      _merge_core(rich, label, reverseInputs)
      val paths = InternalModelDurableHandoffFixture.copyPackage(rich.root, root, workRoot) ++
        Vector("cml/alpha.cml", "cml/beta.cml", "src/main/cml/original.cml").map { relative =>
          val destination = root.resolve(relative)
          Files.createDirectories(destination.getParent)
          Files.copy(rich.root.resolve(relative), destination)
          relative
        }
      val evidence = admit(root, rich.data)
      val authority = rich.request.mutationauthority.get.copy(projectroot = root.toString)
      val current = rich.request.currentrequest
      InternalModelEndToEndSupport.writeInput(workRoot.resolve("caller-input.json"), current,
        rich.request.livesources, authority, label)
      val action = InternalModelContinuationActionGate.evaluate(root, current).toOption.get
      require(action.eligibility == InternalModelContinuationEligibility.Eligible, "producer semantic graph must be eligible")
      output = Json.obj("projectRoot" -> Json.fromString(root.toString),
        "inputPath" -> Json.fromString(workRoot.resolve("caller-input.json").toString),
        "paths" -> Json.fromValues(paths.sorted.map(Json.fromString)),
        "before" -> InternalModelFreshProcessProbe.actionReport(action),
        "selection" -> selection(action),
        "phase9" -> phase9Evidence(evidence.review),
        "counts" -> Json.fromValues(_acceptance.projectionCounts(action.state.continuity).map(Json.fromInt)),
        "sidecars" -> Json.fromBoolean(retained(action.state.continuity)))
    }
    output
  }

  def selection(action: InternalModelContinuationReport): Json = {
    val realization = action.state.continuity.realization
    Json.obj("scope" -> InternalModelEndToEndSupport.evidence(realization.scope),
      "elements" -> Json.fromValues(realization.elements.map(element => Json.obj("identity" -> Json.fromString(element.identity),
        "kind" -> Json.fromString(element.kind), "canonical" -> InternalModelEndToEndSupport.evidence(element.canonicalAssertionIds),
        "enrichment" -> InternalModelEndToEndSupport.evidence(element.enrichmentAssertionIds),
        "conditions" -> InternalModelEndToEndSupport.evidence(element.conditionIds)))),
      "relationships" -> InternalModelEndToEndSupport.evidence(realization.relationships),
      "canonical" -> InternalModelEndToEndSupport.evidence(realization.canonicalAssertions),
      "enrichment" -> InternalModelEndToEndSupport.evidence(realization.enrichmentAssertions),
      "conditions" -> InternalModelEndToEndSupport.evidence(realization.conditions),
      "sourceReferences" -> InternalModelEndToEndSupport.evidence(realization.sourceReferences),
      "candidate" -> InternalModelEndToEndSupport.evidence(action.candidate.get.projection),
      "human" -> InternalModelEndToEndSupport.evidence(action.approval.get.record.approval),
      "decisions" -> InternalModelEndToEndSupport.evidence(action.decisions.map(_.admission.ledger)))
  }

  /** Current human input is separate; original approval also uses independent input. */
  def load(root: Path, input: InternalModelEndToEndSupport.CallerInput): InternalModelCmlChangeFixture.Fixture = {
    val original = input.original
    val basis = original.executionbasis.get
    val approval = InternalModelCandidateHumanApprovalValidator.validate(root, original.approvalartifact.get,
      original.reviewartifact.get, basis, original.humandecision.get).toOption.get
    val review = InternalModelCandidateReviewBindingValidator.validate(root, input.current.reviewartifact.get,
      input.current.executionbasis.get).toOption.get
    val snapshots = InternalModelPackageValidator.verifiedSourceSnapshots(root).toOption.get
    val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
    val data = FixtureData(original.humandecision.get, basis, approval.reviewAdmission.binding,
      InternalModelDurableHandoffFixture.approvedOptions)
    InternalModelCmlChangeFixture.Fixture(root, data, handoff,
      InternalModelCmlChangeRequest(input.current, approval, snapshots, review, input.live, None, input.authority))
  }

  /** Reuse the accepted independently declared owner refresh, retaining one exact core declaration. */
  def refresh(fixture: InternalModelCmlChangeFixture.Fixture,
    application: InternalModelCmlChangeApplication.ApplicationReport, label: String): InternalModelCmlChangeAcceptanceSupport.Refreshed = {
    val refreshed = _acceptance.refreshOwner(fixture, application, label)
    val path = _core.packageRoot(fixture.root)
    val realization = _read(path.resolve("realizations/main.json"))
    val normalized = _arrays.foldLeft(realization) { case (value, (field, key)) =>
      _set(value, field, Json.fromValues(_unique(_values(value, field), item => _text(item, key))))
    }
    _write(path.resolve("realizations/main.json"), normalized)
    val model = _read(path.resolve("snapshots/model.json"))
    val basis = model.hcursor.get[Json]("basis").toOption.get
    val facts = _unique(_values(basis, "facts"), item =>
      Vector("semanticIdentityKind", "semanticIdentity", "sourceAnchor").map(_text(item, _)).mkString("/"))
    _write(path.resolve("snapshots/model.json"), _set(model, "basis", _set(basis, "facts", Json.fromValues(facts))))
    val binding = _read(path.resolve("projections/continuity.json"))
    val views = _values(binding, "views").map { view =>
      _set(view, "records", Json.fromValues(_unique(_values(view, "records"), item =>
        Vector("recordKind", "semanticIdentity", "viewRole").map(_text(item, _)).mkString("/"))))
    }
    _write(path.resolve("projections/continuity.json"), _set(binding, "views", Json.fromValues(views)))
    refreshed
  }

  /** Repeated fixture declarations must agree semantically; no bytes become permission. */
  private def _unique(values: Vector[Json], key: Json => String): Vector[Json] = {
    values.groupBy(key).foreach { case (_, declarations) =>
      require(declarations.distinct.size == 1, "independent repeated fixture declarations must agree")
    }
    values.distinct
  }

  private def _merge_core(rich: InternalModelCmlChangeFixture.Fixture, label: String, reverseinputs: Boolean): Unit = {
    val packagepath = _core.packageRoot(rich.root)
    val realization = _read(packagepath.resolve("realizations/main.json"))
    val model = _read(packagepath.resolve("snapshots/model.json"))
    val binding = _read(packagepath.resolve("projections/continuity.json"))
    val scope = rich.request.currentrequest.scope
    val source = model.hcursor.get[Json]("source").toOption.get
    val snapshot = _core.referenceJson(artifactReference("snapshot-model", rich.data.options))
    def _adapt_(value: Json): Json = value.arrayOrObject(value, values => Json.fromValues(values.map(_adapt_)), fields => {
      val nested = fields.mapValues(_adapt_)
      if (nested("artifactId").flatMap(_.asString).contains("snapshot-model")) snapshot
      else if (nested("authority").isDefined && nested("identity").isDefined && nested("locator").isDefined) source
      else Json.fromJsonObject(nested)
    })
    _core.withFixture(label, reverseinputs) { coreroot =>
      val corepath = _core.packageRoot(coreroot)
      val extra = _adapt_(_prefix(_read(corepath.resolve("realizations/main.json"))))
      val merged = _arrays.foldLeft(realization) { case (value, (field, key)) =>
        val declarations = _values(extra, field).filterNot(item =>
          (field == "elements" && _text(item, key) == "e-usecase") ||
          (field == "canonicalAssertions" && _text(item, key) == "post-a-e-usecase-kind-use-case") ||
          (field == "sourceReferences" && _text(item, key) == "post-ref-e-usecase-kind-use-case"))
        _set(value, field, Json.fromValues((_values(value, field) ++ declarations).sortBy(_text(_, key))))
      }
      val coremodel = _prefix(_read(corepath.resolve("snapshots/model.json")))
      val basis = model.hcursor.get[Json]("basis").toOption.get
      val extrafacts = _values(coremodel.hcursor.get[Json]("basis").toOption.get, "facts")
        .filterNot(item => _text(item, "semanticIdentityKind") == "element" && _text(item, "semanticIdentity") == "e-usecase")
        .map(item => _set(_set(item, "componentIdentity", Json.fromString(scope.componentIdentity)),
          "projectionContextIdentity", Json.fromString(scope.projectionContextIdentity)))
      val facts = (_values(basis, "facts") ++ extrafacts).sortBy(item =>
        (_text(item, "semanticIdentityKind"), _text(item, "semanticIdentity"), _text(item, "sourceAnchor")))
      val corebinding = _prefix(_read(corepath.resolve("projections/main.json")))
      val views = _values(binding, "views").map { view =>
        val family = _text(view, "family")
        val records = _values(corebinding, "views").find(item => _text(item, "family") == family).toVector
          .flatMap(_values(_, "records")).filterNot(item =>
            _text(item, "recordKind") == "element" && _text(item, "semanticIdentity") == "e-usecase")
        _set(view, "records", Json.fromValues((_values(view, "records") ++ records).sortBy(item =>
          (_text(item, "recordKind"), _text(item, "semanticIdentity"), _text(item, "viewRole")))))
      }
      _write(packagepath.resolve("realizations/main.json"), merged)
      _write(packagepath.resolve("snapshots/model.json"), _set(model, "basis", _set(basis, "facts", Json.fromValues(facts))))
      _write(packagepath.resolve("projections/continuity.json"), _set(binding, "views", Json.fromValues(views)))
    }
  }

  /** Prefix declared identity and anchor lanes; never prefix source identities or sequence keys. */
  private def _prefix(value: Json): Json = {
    def _id_(text: String): String = if (text == "e-usecase") text else "post-" + text
    def _walk_(item: Json, key: String): Json = item.arrayOrObject(item.asString.fold(item) { text =>
      val identities = Set("identity", "semanticIdentity", "relatedSemanticIdentity", "sourceElementIdentity", "targetElementIdentity", "affectedIdentity")
      val links = Set("assertionId", "referenceId", "sourceReferenceId", "sourceAnchor", "conditionId", "sequenceAssertionId",
        "canonicalAssertionIds", "enrichmentAssertionIds", "conditionIds")
      if (identities.contains(key) || links.contains(key)) Json.fromString(_id_(text))
      else if (key == "content" && text.startsWith("association:")) {
        val pieces = text.split(":", -1).toVector
        Json.fromString((pieces.take(3) :+ _id_(pieces(3))).mkString(":"))
      } else item
    }, values => Json.fromValues(values.map(value => _walk_(value, key))), fields =>
      Json.fromFields(fields.toVector.map { case (name, nested) => name -> _walk_(nested, name) }))
    _walk_(value, "")
  }

  /** Declared four-family direct impacts use actual Phase 9 constructors and admitted candidate/diff facts. */
  def phase9Evidence(review: InternalModelCandidateReviewBindingAdmission): Json = {
    val scope = review.binding.scope
    val context = MonoKotoProjectionContextIdentity(scope.projectionContextIdentity)
    val component = ComponentDashboardComponentIdentity(scope.componentIdentity)
    val attribution = ComponentDashboardSourceAttribution("model-source", "model-authority", "declared-impact:use-case")
    val condition = ComponentDashboardCondition("unverified", "unverified", None, None, None, None, None, None,
      Vector("test-owner declaration"))
    val proposal = AnalysisDesignImpactProposal("e2e-proposal", context, component, ComponentDashboardSemanticTargetIdentity("e-usecase"),
      "e2e-use-case-record", AnalysisDesignUseCaseOrigin, AnalysisDesignUseCaseSubject, attribution, condition,
      "test-owner", "Explicit four-family candidate impact", Vector("test-owner declaration"), None)
    val declared = Vector(("post-e-entity", AnalysisDesignImpactEntity, AnalysisDesignImpactEntitySubject),
      ("post-e-event", AnalysisDesignImpactEvent, AnalysisDesignImpactEventSubject),
      ("post-e-workflow", AnalysisDesignImpactWorkflow, AnalysisDesignImpactWorkflowSubject),
      ("post-e-state-machine", AnalysisDesignImpactStateMachine, AnalysisDesignImpactStateMachineSubject))
    val realization = review.semanticDiffAdmission.candidateAdmission.continuity.realization
    val records = declared.map { case (identity, category, role) =>
      require(realization.elements.exists(_.identity == identity), "explicit impact identity must exist in actual realization")
      AnalysisDesignImpactRecord("impact-" + identity, context, component, Some(ComponentDashboardSemanticTargetIdentity(identity)),
        category, role, attribution, condition, None)
    }
    val links = records.map(record => AnalysisDesignImpactLink("link-" + record.id, context, component, proposal.id, record.id,
      ComponentDashboardSemanticTargetIdentity("relation-" + record.id), attribution, condition, Vector("test-owner declaration"), None))
    val projection = AnalysisDesignImpactProjection.create(context, component, Vector(proposal), records, links, Vector.empty).toOption.get
    val diff = review.semanticDiffAdmission.diff.targets.head
    val snapshot = review.binding.targets.head.reviewSnapshot
    val candidate = CandidateDesignCandidateModelIdentity(review.binding.candidateModelIdentity, context, component,
      diff.patchTrace.id, attribution, condition, None)
    val governance = CandidateDesignGitGovernanceReference("e2e-governance-trace", context, component, diff.patchTrace.id,
      candidate.id, snapshot.id, "test-owner:pending-repository-acceptance", "pending", attribution, condition,
      Vector("traceability only"), None)
    val integration = CandidateDesignSemanticDiffIntegration.create(projection, proposal.id, links.map(_.id), diff.patchTrace,
      candidate, diff.entries.map(_.entry), snapshot, governance)
    require(integration.isInstanceOf[CandidateDesignSemanticDiffProjectedIntegration], "actual Phase 9 integration must admit declared evidence")
    Json.obj("projection" -> InternalModelEndToEndSupport.evidence(projection),
      "integration" -> InternalModelEndToEndSupport.evidence(integration))
  }

  def retained(continuity: InternalModelProjectionContinuity): Boolean = {
    val model = continuity.realization
    model.elements.exists(value => value.identity == "opaque-shared" && value.canonicalAssertionIds == Vector("a-opaque-element-kind-Mono") &&
      value.enrichmentAssertionIds == Vector("z-opaque-element-enrichment") && value.conditionIds == Vector("c-opaque-element")) &&
      model.relationships.exists(value => value.identity == "opaque-shared" && value.canonicalAssertionIds == Vector("a-opaque-relationship-role-StructuralDomain") &&
        value.enrichmentAssertionIds == Vector("z-opaque-relationship-enrichment") && value.conditionIds == Vector("c-opaque-relationship")) &&
      model.conditions.exists(value => value.conditionId == "c-opaque-element" && value.affectedKind == "element" &&
        value.affectedIdentity == "opaque-shared" && value.detail == "element evidence remains bounded") &&
      model.conditions.exists(value => value.conditionId == "c-opaque-relationship" && value.affectedKind == "relationship" &&
        value.affectedIdentity == "opaque-shared" && value.detail == "relationship evidence remains bounded") &&
      model.canonicalAssertions.exists(value => value.assertionId == "post-a-r-usecase-step-sequence-key-step-1" && value.content == "sequence-key:step-1") &&
      model.canonicalAssertions.exists(value => value.assertionId == "post-a-r-workflow-flow-sequence-key-flow-1" && value.content == "sequence-key:flow-1") &&
      model.canonicalAssertions.exists(_.association.exists(value => value.associationRole == "owner" &&
        value.relatedSemanticIdentity == "post-r-structure" && value.relatedSemanticIdentityKind == "relationship")) &&
      continuity.workflow.flows.exists(_.sourceFlow.condition.conflict.contains("recorded conflict")) &&
      continuity.entityModel.subjects.exists(_.navigationTarget.isEmpty)
  }

  private def _read(path: Path): Json = _core.json(Files.readAllBytes(path).toVector)
  private def _write(path: Path, value: Json): Unit = _core.write(path, _core.canonical(value))
  private def _values(value: Json, key: String): Vector[Json] = value.hcursor.get[Vector[Json]](key).toOption.get
  private def _text(value: Json, key: String): String = value.hcursor.get[String](key).toOption.get
  private def _set(value: Json, key: String, replacement: Json): Json = value.mapObject(_.add(key, replacement))
}
