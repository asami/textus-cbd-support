package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path}
import scala.jdk.CollectionConverters.*
import io.circe.{Json, Printer}
import io.circe.parser.parse

/**
 * Test-only source witnesses adapted from InternalModelProjectionContinuityValidatorSpec.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] object InternalModelContinuationFixture {
  private final case class Element(id: String, kind: String, family: Option[String])
  private final case class Relationship(
    id: String, role: String, source: String, target: String, family: String,
    associations: Vector[(String, String, String)] = Vector.empty, sequence: Option[String] = None
  )
  private val _families = Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection",
    "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
  private val _source = Json.obj(
    "authority" -> Json.fromString("model-authority"), "identity" -> Json.fromString("model-source"),
    "locator" -> Json.fromString("catalog/model-source"), "revision" -> Json.fromString("source-version-nine")
  )
  val packageId: InternalModelPackageId = InternalModelPackageId.from("01234567-89ab-cdef-0123-456789abcdef").toOption.get
  val carrierRevision: Long = 31L
  val snapshotReference: InternalModelArtifactReference = reference("snapshot-model", 11L, InternalModelArtifactRole.SourceSnapshot)
  val realizationReference: InternalModelArtifactReference = reference("realization-main", 17L, InternalModelArtifactRole.Realization)
  val projectionReference: InternalModelArtifactReference = reference("projection-main", 23L, InternalModelArtifactRole.Projection)

  def withFixture(label: String = "display-one", reverseInputs: Boolean = false)(body: Path => Unit): Unit = {
    val work = Files.createDirectories(Path.of("target/internal-model-continuation/work").toAbsolutePath)
    val root = Files.createTempDirectory(work, "package-")
    try {
      write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: continuity-sample\n".getBytes(StandardCharsets.UTF_8).toVector)
      write(root.resolve("src/main/cml/main.cml"), "unchanged canonical CML\n".getBytes(StandardCharsets.UTF_8).toVector)
      val (snapshot, realization, binding) = _semantic_bytes(label, reverseInputs)
      val extras = Vector(
        artifact("optional-source", 37L, "snapshots/optional.json", "source-snapshot", required = false),
        artifact("raw-approval", 41L, "approvals/raw.json", "approval", required = false),
        artifact("raw-decision", 43L, "decisions/raw.json", "decision", required = false)
      )
      val core = Vector(
        artifact("snapshot-model", 11L, "snapshots/model.json", "source-snapshot"),
        artifact("realization-main", 17L, "realizations/main.json", "realization", Vector(snapshotReference)),
        artifact("projection-main", 23L, "projections/main.json", "projection", Vector(realizationReference))
      )
      val cursor = cursorFor(core)
      val resume = InternalModelResumeCursorCodec.encode(cursor)
      val entries = core ++ extras :+ artifact("resume-main", 29L, "resume.yaml", "resume")
      write(packageRoot(root).resolve("manifest.yaml"), manifest(entries))
      Vector("snapshots/model.json" -> snapshot, "realizations/main.json" -> realization, "projections/main.json" -> binding,
        "resume.yaml" -> resume, "approvals/raw.json" -> "raw unadmitted approval evidence\n".getBytes(StandardCharsets.UTF_8).toVector)
        .foreach { case (path, bytes) => write(packageRoot(root).resolve(path), bytes) }
      body(root)
    } finally removeTree(root)
  }

  def cursorFor(entries: Vector[Json]): InternalModelResumeCursor =
    InternalModelResumeCursor("2.0", "ccdm-resume-v2", packageId, carrierRevision, entries.map(entryReference)
      .sortBy(_.artifactId.value), "recorded-stage", None, Some("future-gate-action"), Vector.empty,
      Vector(InternalModelResumeCondition("source-recorded", "Recorded basis only", Some("snapshot-model"))), Vector.empty, Vector.empty)

  def packageRoot(root: Path): Path = root.resolve("src/main/internal-model")
  def json(bytes: Vector[Byte]): Json = parse(new String(bytes.toArray, StandardCharsets.UTF_8)).toOption.get
  def canonical(value: Json): Vector[Byte] = (Printer.noSpacesSortKeys.print(value) + "\n").getBytes(StandardCharsets.UTF_8).toVector
  def reference(artifactId: String, artifactRevision: Long, role: InternalModelArtifactRole): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(artifactId).toOption.get, InternalModelArtifactRevision.from(artifactRevision).toOption.get, role)

  def referenceJson(value: InternalModelArtifactReference): Json = json(InternalModelTypedControlCodec.encodeArtifactReference(value))

  def entryReference(value: Json): InternalModelArtifactReference = reference(
    value.hcursor.get[String]("artifactId").toOption.get, value.hcursor.get[Long]("artifactRevision").toOption.get,
    InternalModelArtifactRole.fromWire(value.hcursor.get[String]("role").toOption.get).toOption.get)

  /** Explicit metadata update only: no reconstructed manifest or encoded-content control. */
  def withCapturedInventory(handoff: InternalModelVerifiedContinuationPackage, artifacts: Vector[InternalModelVerifiedContinuationArtifact]): InternalModelVerifiedContinuationPackage =
    handoff.copy(packageContext = handoff.packageContext.copy(artifacts = artifacts.map(_.context)), artifacts = artifacts)

  def artifact(artifactId: String, artifactRevision: Long, path: String, role: String, dependencies: Vector[InternalModelArtifactReference] = Vector.empty, required: Boolean = true): Json =
    Json.obj("artifactId" -> Json.fromString(artifactId), "path" -> Json.fromString(path), "role" -> Json.fromString(role),
      "artifactRevision" -> Json.fromLong(artifactRevision), "dependsOn" -> Json.fromValues(dependencies.sortBy(_.artifactId.value).map(referenceJson)), "required" -> Json.fromBoolean(required))

  def manifest(entries: Vector[Json], identity: InternalModelPackageId = packageId, projectId: String = "continuity-sample", revision: Long = carrierRevision): Vector[Byte] = {
    def _order_(pending: Vector[Json], done: Vector[Json]): Vector[Json] =
      if pending.isEmpty then done else {
        val ids = done.map(_.hcursor.get[String]("artifactId").toOption.get).toSet
        val next = pending.filter(_.hcursor.get[Vector[Json]]("dependsOn").toOption.get.forall(value => ids.contains(value.hcursor.get[String]("artifactId").toOption.get)))
          .sortBy(_.hcursor.get[String]("artifactId").toOption.get).head
        _order_(pending.filterNot(_ == next), done :+ next)
      }
    val value = Json.obj("schemaVersion" -> Json.fromString("2.0"), "packageId" -> Json.fromString(identity.value),
      "projectNamespace" -> Json.fromString("org.example"), "projectId" -> Json.fromString(projectId),
      "revision" -> Json.fromLong(revision), "lifecycleState" -> Json.fromString("draft"), "artifacts" -> Json.fromValues(_order_(entries, Vector.empty)))
    canonical(value)
  }

  def replaceArtifact(root: Path, id: String, bytes: Vector[Byte]): Unit = {
    val value = json(Files.readAllBytes(packageRoot(root).resolve("manifest.yaml")).toVector)
    val entries = value.hcursor.get[Vector[Json]]("artifacts").toOption.get
    val selected = entries.find(_.hcursor.get[String]("artifactId").toOption.contains(id)).get
    write(packageRoot(root).resolve(selected.hcursor.get[String]("path").toOption.get), bytes)
  }

  def replaceInventory(root: Path, entries: Vector[Json], identity: InternalModelPackageId = packageId, projectId: String = "continuity-sample", revision: Long = carrierRevision): Unit =
    write(packageRoot(root).resolve("manifest.yaml"), manifest(entries, identity, projectId, revision))

  def inventory(root: Path): Vector[Json] = json(Files.readAllBytes(packageRoot(root).resolve("manifest.yaml")).toVector).hcursor.get[Vector[Json]]("artifacts").toOption.get
  def cursor(root: Path): InternalModelResumeCursor = InternalModelResumeCursorCodec.decode(Files.readAllBytes(packageRoot(root).resolve("resume.yaml")).toVector).toOption.get
  def replaceCursor(root: Path, value: InternalModelResumeCursor): Unit = replaceArtifact(root, "resume-main", InternalModelResumeCursorCodec.encode(value))

  def write(path: Path, bytes: Vector[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes.toArray)
  }

  def treeBytes(root: Path): Map[String, Vector[Byte]] = {
    val stream = Files.walk(root)
    try stream.iterator.asScala.filter(Files.isRegularFile(_, LinkOption.NOFOLLOW_LINKS)).map(path => root.relativize(path).toString -> Files.readAllBytes(path).toVector).toMap
    finally stream.close()
  }

  def removeTree(root: Path): Unit = if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
    val stream = Files.walk(root)
    try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
    finally stream.close()
  }

  private def _semantic_bytes(label: String, reverseinputs: Boolean): (Vector[Byte], Vector[Byte], Vector[Byte]) = {
    val allelements = Vector(
      Element("e-mono", "Mono", Some("MonoKotoProjection")), Element("e-koto", "Koto", Some("MonoKotoProjection")),
      Element("e-usecase", "use-case", Some("UseCaseCommunicationProjection")), Element("e-flow", "use-case-flow", None), Element("e-step", "use-case-flow-step", None),
      Element("e-entity", "EntityModelEntity", Some("EntityModelProjection")), Element("e-command", "EventModelCommand", Some("EventModelProjection")), Element("e-event", "EventModelEvent", Some("EventModelProjection")),
      Element("e-dimension", "ClassificationPowertypeDimension", Some("ClassificationViewProjection")),
      Element("e-workflow", "WorkflowProjectionWorkflow", Some("WorkflowProjection")), Element("e-activity", "WorkflowProjectionActivity", Some("WorkflowProjection")),
      Element("e-state-machine", "StateMachineProjectionStateMachine", Some("StateMachineProjection")), Element("e-state-a", "StateMachineProjectionState", Some("StateMachineProjection")),
      Element("e-state-b", "StateMachineProjectionState", Some("StateMachineProjection")), Element("e-trigger", "StateMachineProjectionTrigger", Some("StateMachineProjection"))
    )
    val allrelationships = Vector(
      Relationship("r-mono-domain", "StructuralDomain", "e-mono", "e-entity", "MonoKotoProjection"),
      Relationship("r-koto-temporal", "BehavioralTemporal", "e-koto", "e-command", "MonoKotoProjection"),
      Relationship("r-usecase-actor", "Actor", "e-usecase", "e-entity", "UseCaseCommunicationProjection"),
      Relationship("r-usecase-flow", "UseCaseCommunicationFlow", "e-usecase", "e-flow", "UseCaseCommunicationProjection"),
      Relationship("r-usecase-step", "UseCaseCommunicationFlowStep", "e-flow", "e-step", "UseCaseCommunicationProjection", sequence = Some("step-1")),
      Relationship("r-entity-metadata", "IdentityMetadata", "e-entity", "e-entity", "EntityModelProjection"),
      Relationship("r-event-cause", "EventModelCausalAssertion", "e-command", "e-event", "EventModelProjection"),
      Relationship("r-structure", "StructureAssociation", "e-entity", "e-mono", "StructureViewProjection"),
      Relationship("r-structure-assertion", "Cardinality", "e-entity", "e-mono", "StructureViewProjection", Vector(("owner", "relationship", "r-structure"))),
      Relationship("r-classification", "ClassificationGeneralization", "e-dimension", "e-dimension", "ClassificationViewProjection", Vector(("subject", "element", "e-dimension"))),
      Relationship("r-classification-assertion", "ClassificationDimensionValue", "e-dimension", "e-dimension", "ClassificationViewProjection", Vector(("owner", "element", "e-dimension"), ("subject", "element", "e-dimension"))),
      Relationship("r-workflow-flow", "WorkflowProjectionFlowRelation", "e-workflow", "e-activity", "WorkflowProjection", Vector(("owner", "element", "e-workflow")), Some("flow-1")),
      Relationship("r-state-transition", "StateMachineProjectionTransitionRelation", "e-state-a", "e-state-b", "StateMachineProjection", Vector(("owner", "element", "e-state-machine"))),
      Relationship("r-state-adjunct", "StateMachineProjectionTransitionAdjunctRelation", "e-state-b", "e-trigger", "StateMachineProjection", Vector(("owner", "relationship", "r-state-transition")))
    )
    val elements = if reverseinputs then allelements.reverse else allelements
    val relationships = if reverseinputs then allrelationships.reverse else allrelationships
    val facts = (elements.map(element => ("element", element.id, s"kind:${element.kind}")) ++ relationships.flatMap { relationship =>
      Vector(("relationship", relationship.id, s"role:${relationship.role}")) ++
        relationship.associations.map { case (role, kind, identity) => ("relationship", relationship.id, s"association:$role:$kind:$identity") } ++
        relationship.sequence.toVector.map(sequence => ("relationship", relationship.id, s"sequence-key:$sequence"))
    }).sortBy { case (kind, identity, content) => (kind, identity, _token(content)) }
    val enrichmentfacts = Vector(("element", "e-entity", "display-note:enrichment-only"))
    val allfacts = (facts ++ enrichmentfacts).sortBy { case (kind, identity, content) => (kind, identity, _token(content)) }
    def _id_(prefix: String, identity: String, content: String): String = s"$prefix-$identity-${_token(content)}"
    def _target_(kind: String, identity: String): Json = Json.obj("semanticIdentityKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity))
    def _assertions_(selected: Vector[(String, String, String)], prefix: String): Vector[Json] = selected.map { case (kind, identity, content) =>
      val base = Json.obj("assertionId" -> Json.fromString(_id_(prefix, identity, content)), "semanticIdentityKind" -> Json.fromString(kind),
        "semanticIdentity" -> Json.fromString(identity), "content" -> Json.fromString(content), "conditionIds" -> Json.arr(),
        "sourceReferenceId" -> Json.fromString(_id_("ref", identity, content)))
      base.mapObject(_.add("association", content.split(":", -1).toVector match {
        case Vector("association", role, relatedkind, relatedid) => Json.obj("associationRole" -> Json.fromString(role), "relatedSemanticIdentityKind" -> Json.fromString(relatedkind), "relatedSemanticIdentity" -> Json.fromString(relatedid))
        case _ => Json.Null
      }))
    }.sortBy(_.hcursor.get[String]("assertionId").toOption.get)
    val canonicalassertions = _assertions_(facts, "a")
    val enrichmentassertions = _assertions_(enrichmentfacts, "z")
    val conditions = Vector(Json.obj(
      "conditionId" -> Json.fromString("c-workflow-conflict"), "kind" -> Json.fromString("conflict"), "affectedKind" -> Json.fromString("relationship"),
      "affectedIdentity" -> Json.fromString("r-workflow-flow"), "sourceReferenceId" -> Json.fromString("ref-r-workflow-flow-role-WorkflowProjectionFlowRelation"), "detail" -> Json.fromString("recorded conflict")
    ))
    def _links_(identity: String): Vector[(String, Json)] = Vector(
      "canonicalAssertionIds" -> Json.fromValues(canonicalassertions.filter(_.hcursor.get[String]("semanticIdentity").toOption.contains(identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(enrichmentassertions.filter(_.hcursor.get[String]("semanticIdentity").toOption.contains(identity)).map(_.hcursor.get[String]("assertionId").toOption.get).sorted.map(Json.fromString)),
      "conditionIds" -> (if identity == "r-workflow-flow" then Json.arr(Json.fromString("c-workflow-conflict")) else Json.arr())
    )
    val scope = Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"), "selectedUseCaseElementIdentity" -> Json.fromString("e-usecase"))
    val snapshot = canonical(Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("model-context"), "source" -> _source,
      "basis" -> Json.obj("contextIdentity" -> Json.fromString("model-context"), "facts" -> Json.fromValues(allfacts.map { case (kind, identity, content) =>
        Json.obj("componentIdentity" -> Json.fromString("component-all-eight"), "projectionContextIdentity" -> Json.fromString("context-all-eight"),
          "semanticIdentityKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "content" -> Json.fromString(content),
          "sourceAnchor" -> Json.fromString(_id_("anchor", identity, content)), "limitations" -> Json.arr())
      }))))
    val realization = canonical(Json.obj(
      "profile" -> Json.fromString("ccdm-realization-v3"), "schemaVersion" -> Json.fromString("3.0"),
      "realizationReference" -> Json.obj("recordId" -> Json.fromString("realization-all-eight"), "recordRevision" -> Json.fromLong(5L)), "scope" -> scope, "canonicalAssertions" -> Json.fromValues(canonicalassertions),
      "enrichmentAssertions" -> Json.fromValues(enrichmentassertions), "conditions" -> Json.fromValues(conditions), "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(referenceJson(snapshotReference))),
      "elements" -> Json.fromValues(elements.sortBy(_.id).map(element => Json.obj((Vector("identity" -> Json.fromString(element.id), "kind" -> Json.fromString(element.kind), "label" -> Json.fromString(label)) ++ _links_(element.id))*))),
      "relationships" -> Json.fromValues(relationships.sortBy(_.id).map(relationship => Json.obj((Vector("identity" -> Json.fromString(relationship.id), "label" -> Json.fromString(relationship.id),
        "sourceElementIdentity" -> Json.fromString(relationship.source), "targetElementIdentity" -> Json.fromString(relationship.target), "role" -> Json.fromString(relationship.role), "direction" -> Json.fromString("source-to-target")) ++ _links_(relationship.id))*))),
      "sourceReferences" -> Json.fromValues(allfacts.map { case (kind, identity, content) => Json.obj("referenceId" -> Json.fromString(_id_("ref", identity, content)),
        "snapshotReference" -> referenceJson(snapshotReference), "source" -> _source, "sourceAnchor" -> Json.fromString(_id_("anchor", identity, content)), "target" -> _target_(kind, identity))
      }.sortBy(_.hcursor.get[String]("referenceId").toOption.get))
    ))
    def _record_(kind: String, identity: String, role: String, sequence: Option[String]): Json = Json.obj((Vector(
      "recordKind" -> Json.fromString(kind), "semanticIdentity" -> Json.fromString(identity), "viewRole" -> Json.fromString(role),
      "sequenceAssertionId" -> sequence.map(value => Json.fromString(_id_("a", identity, s"sequence-key:$value"))).getOrElse(Json.Null)
    ) ++ _links_(identity))*)
    val binding = canonical(Json.obj("profile" -> Json.fromString("ccdm-projection-binding-v3"),
      "bindingReference" -> Json.obj("recordId" -> Json.fromString("binding-all-eight"), "recordRevision" -> Json.fromLong(7L)),
      "schemaVersion" -> Json.fromString("3.0"), "realizationArtifactReference" -> referenceJson(realizationReference), "scope" -> scope,
      "views" -> Json.fromValues(_families.map { family =>
        val records = elements.filter(_.family.contains(family)).map(element => _record_("element", element.id, element.kind, None)) ++
          relationships.filter(_.family == family).map(relationship => _record_("relationship", relationship.id, relationship.role, relationship.sequence))
        Json.obj("family" -> Json.fromString(family), "records" -> Json.fromValues(records.sortBy(value => (value.hcursor.get[String]("recordKind").toOption.get, value.hcursor.get[String]("semanticIdentity").toOption.get, value.hcursor.get[String]("viewRole").toOption.get))))
      })))
    (snapshot, realization, binding)
  }

  private def _token(value: String): String = value.map(character => if character.isLetterOrDigit then character else '-')
}
