package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import io.circe.{Codec, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import ModelEditSourceProjection.*
import ModelEditSourceProjectionCodec.given

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Explicit candidate-v2 planning and connection to the existing Phase 10 admission. */
object ModelEditCmlProjection {
  final case class ElementReplacement(expected: RecordRef, replacement: Element)
  final case class RelationshipReplacement(expected: RecordRef, replacement: Relationship)
  final case class Request(schemaVersion: String, source: Source, elements: Vector[ElementReplacement],
    relationships: Vector[RelationshipReplacement], removedRelationships: Vector[RecordRef],
    addedElements: Vector[Element], addedRelationships: Vector[Relationship], coverage: Vector[Coverage])
  final case class Plan(selection: Selection, session: InternalModelRecordReference,
    candidate: InternalModelRecordReference, operations: Vector[InternalModelRecordReference],
    request: Request, proposed: Document, proposedBindings: Vector[Binding])
  final case class Confirmed(plan: Plan, rendering: Rendering, reproduced: Publication)
  /** Persisted exact linkage, not an alternative approval or canonical writer. */
  final case class Mapping(profile: String, schemaVersion: String, session: InternalModelRecordReference,
    candidate: InternalModelRecordReference, operations: Vector[InternalModelRecordReference],
    sourceArtifact: InternalModelArtifactReference, editArtifact: InternalModelArtifactReference,
    candidateArtifact: InternalModelArtifactReference, source: Source, producer: Producer, confirmed: Confirmed)
  import ModelEditCandidateCodec.given
  given Codec.AsObject[ElementReplacement] = ModelEditCandidateCodec.closed[ElementReplacement]("expected", "replacement")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[RelationshipReplacement] = ModelEditCandidateCodec.closed[RelationshipReplacement]("expected", "replacement")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Request] = ModelEditCandidateCodec.closed[Request]("schemaVersion", "source", "elements", "relationships", "removedRelationships", "addedElements", "addedRelationships", "coverage")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Plan] = ModelEditCandidateCodec.closed[Plan]("selection", "session", "candidate", "operations", "request", "proposed", "proposedBindings")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Confirmed] = ModelEditCandidateCodec.closed[Confirmed]("plan", "rendering", "reproduced")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Mapping] = ModelEditCandidateCodec.closed[Mapping]("profile", "schemaVersion", "session", "candidate", "operations", "sourceArtifact", "editArtifact", "candidateArtifact", "source", "producer", "confirmed")(using deriveDecoder, deriveEncoder)

  def prepare(selection: Selection, session: ModelEditSession, successor: Source,
    proposed: Document, proposedBindings: Vector[Binding]): Either[String, Plan] = {
    val basis = selection.publication.document
    val proposedselection = selection.copy(publication = selection.publication.copy(source = successor, document = proposed), bindings = proposedBindings)
    def _key_(reference: RecordRef): (String, String) = (reference.owner, reference.identity)
    for {
      _ <- ModelEditSessionCodec.validate(session)
      _ <- Either.cond(session.state == ModelEditSession.SessionState.Active && session.candidate.artifacts.contains(selection.sourceArtifact), (), "active candidate must depend on the selected source artifact")
      _ <- Either.cond(successor.owner == selection.publication.source.owner && successor.identity == selection.publication.source.identity &&
        successor.revision.nonEmpty && successor.revision != selection.publication.source.revision, (), "explicit successor of the same source is required")
      _ <- Either.cond(selection.publication.limitations.isEmpty && families.forall(selection.complete), (), "incomplete or unsupported source mapping cannot promote")
      imported <- ModelEditSourceProjection.importCandidate(proposedselection, session.candidate.reference, session.scope)
      _ <- _candidate_matches(session.candidate, imported, proposedselection)
      _ <- _actor_goal_scope(selection, proposedselection, session.candidate)
      _ <- Either.cond(basis.elements.filterNot(value => Set("goal", "slice")(value.kind)) == proposed.elements.filterNot(value => Set("goal", "slice")(value.kind)), (), "commands cannot change unselected source element families")
      _ <- Either.cond(basis.relationships.filter(value => Set("cause", "reaction", "dependency", "context")(value.kind)) ==
        proposed.relationships.filter(value => Set("cause", "reaction", "dependency", "context")(value.kind)), (), "commands cannot change causal or contextual source evidence")
      _ <- Either.cond(basis.elements.filter(_.kind == "goal").forall(value => proposed.elements.find(next => _key_(next.ref) == _key_(value.ref)).exists(next =>
        next.copy(ref = value.ref, intent = value.intent, source = value.source) == value)), (), "Goal refinement changes intent while retaining other source fields")
      _ <- Either.cond(basis.elements.forall(value => proposed.elements.exists(next => _key_(next.ref) == _key_(value.ref))), (), "element removal is outside the editing command contract")
      replacements = proposed.elements.flatMap { value => basis.elements.find(old => _key_(old.ref) == _key_(value.ref)).filter(_ != value).map(old => ElementReplacement(old.ref, value)) }
      relationships = proposed.relationships.flatMap { value => basis.relationships.find(old => _key_(old.ref) == _key_(value.ref)).filter(_ != value).map(old => RelationshipReplacement(old.ref, value)) }
      added = proposed.elements.filterNot(value => basis.elements.exists(old => _key_(old.ref) == _key_(value.ref)))
      edges = proposed.relationships.filterNot(value => basis.relationships.exists(old => _key_(old.ref) == _key_(value.ref)))
      _ <- Either.cond((replacements.map(value => (value.expected, value.replacement.ref, value.replacement.source)) ++
        relationships.map(value => (value.expected, value.replacement.ref, value.replacement.source))).forall { case (before, after, source) =>
        _key_(before) == _key_(after) && after.revision > before.revision && source == successor
      } && (added.map(_.source) ++ edges.map(_.source)).forall(_ == successor), (), "changed records require supplied successor references and source attribution")
      request = Request("cozy.cml.model-edit-candidate.v2", successor, replacements, relationships,
        basis.relationships.filterNot(value => proposed.relationships.exists(next => _key_(next.ref) == _key_(value.ref))).map(_.ref), added, edges, proposed.coverage)
    } yield Plan(selection, session.reference, session.candidate.reference, session.history.map(_.operation.reference), request, proposed, proposedBindings)
  }

  def encodeRequest(plan: Plan): String = plan.request.asJson.noSpaces

  def confirm(plan: Plan, rendering: Rendering, reproduced: Publication): Either[String, Confirmed] =
    for {
      _ <- ModelEditSourceProjection.validate(reproduced)
      _ <- Either.cond(rendering.schemaVersion == plan.request.schemaVersion && rendering.source == plan.selection.publication.source &&
        rendering.candidateSource == plan.request.source && rendering.limitations.isEmpty && rendering.text.nonEmpty,
        (), "rendering does not bind the exact selected source and candidate request")
      _ <- Either.cond(reproduced.source == plan.request.source && reproduced.producer == rendering.producer &&
        reproduced.document == plan.proposed && reproduced.limitations.isEmpty, (), "actual re-production differs from the proposed typed source document")
    } yield Confirmed(plan, rendering, reproduced)

  private[runtime] def bind(confirmed: Confirmed, session: ModelEditSession,
    editArtifact: InternalModelArtifactReference, candidate: InternalModelCandidateCmlAdmission): Either[String, Mapping] = {
    val plan = confirmed.plan
    val source = plan.selection.publication.source
    for {
      _ <- Either.cond(plan.session == session.reference && plan.candidate == session.candidate.reference &&
        plan.operations == session.history.map(_.operation.reference), (), "confirmation is stale for this edit history")
      _ <- Either.cond(candidate.projection.candidateReference == session.candidate.reference &&
        candidate.projection.scope.componentIdentity == session.scope.component.value &&
        candidate.projection.scope.projectionContextIdentity == session.scope.context.value, (), "Phase 10 candidate scope/reference differs from edit session")
      target <- candidate.targetBytes.filter(value => value.baseline.reference == plan.selection.sourceArtifact &&
        value.baseline.source.authority == source.owner && value.baseline.source.identity == source.identity &&
        value.baseline.source.revision.contains(source.revision)) match {
        case Vector(value) => Right(value)
        case _ => Left("Phase 10 candidate must contain the exact source-owned target")
      }
      _ <- Either.cond(candidate.targetBytes.size == 1 && target.proposedRawBytes == confirmed.rendering.text.getBytes(StandardCharsets.UTF_8).toVector &&
        editArtifact.role == InternalModelArtifactRole.Projection, (), "Phase 10 candidate must retain the confirmed proposed CML payload")
      projected <- candidate.projection.targets.find(_.targetId == target.targetId).toRight("source-owned candidate target is absent")
      _ <- _source_mappings(plan, projected.mappings)
    } yield Mapping("ccdm-model-edit-source-mapping-v1", "1.0", session.reference, session.candidate.reference,
      plan.operations, plan.selection.sourceArtifact, editArtifact, candidate.candidateArtifactReference, source, confirmed.rendering.producer, confirmed)
  }

  def encodeMapping(mapping: Mapping): Vector[Byte] = mapping.asJson.noSpaces.getBytes(StandardCharsets.UTF_8).toVector
  private[runtime] def decodeMapping(projection: InternalModelVerifiedProjection): Either[String, Mapping] =
    for {
      json <- io.circe.jawn.JawnParser(allowDuplicateKeys = false).parse(new String(projection.bytes.toArray, StandardCharsets.UTF_8)).left.map(_.message)
      mapping <- json.as[Mapping].left.map(_.message)
      _ <- Either.cond(mapping.profile == "ccdm-model-edit-source-mapping-v1" && mapping.schemaVersion == "1.0" &&
        projection.reference.role == InternalModelArtifactRole.Projection &&
        projection.dependencies == Vector(mapping.sourceArtifact, mapping.editArtifact, mapping.candidateArtifact).sortBy(_.artifactId.value) &&
        mapping.sourceArtifact.role == InternalModelArtifactRole.SourceSnapshot && mapping.editArtifact.role == InternalModelArtifactRole.Projection &&
        mapping.candidateArtifact.role == InternalModelArtifactRole.Projection &&
        mapping.session == mapping.confirmed.plan.session && mapping.candidate == mapping.confirmed.plan.candidate &&
        mapping.operations == mapping.confirmed.plan.operations && mapping.sourceArtifact == mapping.confirmed.plan.selection.sourceArtifact &&
        mapping.source == mapping.confirmed.plan.selection.publication.source && mapping.producer == mapping.confirmed.rendering.producer, (), "source mapping dependencies or profile are invalid")
    } yield mapping

  private[runtime] def validateMapping(mapping: Mapping, session: ModelEditSession,
    candidate: InternalModelCandidateCmlAdmission): Either[String, Unit] = {
    val confirmed = mapping.confirmed
    val plan = confirmed.plan
    for {
      prepared <- prepare(plan.selection, session, plan.request.source, plan.proposed, plan.proposedBindings)
      _ <- Either.cond(prepared == plan, (), "captured candidate request is not the exact edit projection")
      checked <- confirm(plan, confirmed.rendering, confirmed.reproduced)
      bound <- bind(checked, session, mapping.editArtifact, candidate)
      _ <- Either.cond(bound == mapping, (), "captured source mapping differs from admitted Phase 10 binding")
    } yield ()
  }

  private def _actor_goal_scope(basis: Selection, proposed: Selection, candidate: ModelEditSession.Candidate): Either[String, Unit] = {
    def _pair_(selection: Selection, edge: Relationship): Option[(ComponentDashboardSemanticTargetIdentity, ComponentDashboardSemanticTargetIdentity)] =
      (edge.from, edge.to) match {
        case (Endpoint.Local(from), Endpoint.Local(to)) => for { actor <- selection.binding(from).toOption; goal <- selection.binding(to).toOption } yield (actor.semanticIdentity, goal.semanticIdentity)
        case _ => None
      }
    def _pairs_(selection: Selection): Set[(ComponentDashboardSemanticTargetIdentity, ComponentDashboardSemanticTargetIdentity)] =
      selection.publication.document.relationships.filter(_.kind == "actor-goal").flatMap(value => _pair_(selection, value)).toSet
    val expected = _pairs_(basis) ++ candidate.goals.flatMap(goal => goal.actor.map(actor => (actor.semanticTargetId, goal.reference.semanticTargetId)))
    val original = basis.publication.document.relationships.filter(_.kind == "actor-goal")
    val current = proposed.publication.document.relationships.filter(_.kind == "actor-goal")
    val oldgoals = basis.publication.document.elements.filter(_.kind == "goal").flatMap(value => basis.binding(value.ref).toOption.map(_.semanticIdentity)).toSet
    val additions = candidate.goals.count(goal => !oldgoals(goal.reference.semanticTargetId) && goal.actor.nonEmpty)
    val retained = original.forall(edge => current.exists(value => value.ref.owner == edge.ref.owner && value.ref.identity == edge.ref.identity &&
      _pair_(basis, edge) == _pair_(proposed, value)))
    Either.cond(_pairs_(proposed) == expected && retained && current.size == original.size + additions,
      (), "Actor-Goal changes require the explicit candidate Actor association")
  }

  private def _source_mappings(plan: Plan, mappings: Vector[InternalModelCandidateCmlMapping]): Either[String, Unit] = {
    val basis = plan.selection.publication.document
    val records = (basis.elements ++ plan.proposed.elements).map(value => (value.ref, "element")) ++
      (basis.relationships ++ plan.proposed.relationships).map(value => (value.ref, "relationship"))
    val bindings = plan.proposedBindings ++ plan.selection.bindings
    val associated = records.flatMap { case (ref, kind) => bindings.find(_.sourceRef == ref).map(binding =>
      (ref, kind, binding.semanticIdentity.value)) }
    val changed = (plan.request.elements.map(_.replacement.ref) ++ plan.request.addedElements.map(_.ref) ++
      plan.request.relationships.map(_.replacement.ref) ++ plan.request.addedRelationships.map(_.ref) ++ plan.request.removedRelationships).toSet
    val exact = mappings.forall(mapping => associated.exists { case (ref, kind, identity) =>
      mapping.semanticIdentityKind == kind && mapping.semanticIdentity == identity &&
        mapping.cmlSemanticIdentity == ref.identity && mapping.cmlAnchor == ref.identity
    })
    val complete = associated.filter(value => changed(value._1)).forall { case (_, kind, identity) =>
      mappings.exists(value => value.semanticIdentityKind == kind && value.semanticIdentity == identity)
    }
    Either.cond(exact && complete, (), "Phase 10 mappings must retain every changed source record with its exact kind, declared anchor and explicit CBD identity")
  }

  private def _candidate_matches(expected: ModelEditSession.Candidate, actual: ModelEditSession.Candidate,
    selection: Selection): Either[String, Unit] = {
    val actorlinks = selection.publication.document.relationships.filter(_.kind == "actor-goal")
    val attribution = ComponentDashboardSourceAttribution(selection.publication.source.identity,
      selection.publication.source.owner, selection.publication.source.identity)
    val explicitactors = expected.goals.forall(goal => goal.actor.forall(actor => actorlinks.exists(edge =>
      (edge.from, edge.to) match {
        case (Endpoint.Local(from), Endpoint.Local(to)) => selection.binding(from).exists(_.semanticIdentity == actor.semanticTargetId) &&
          selection.binding(to).exists(_.semanticIdentity == goal.reference.semanticTargetId)
        case _ => false
      })))
    Either.cond(expected.useCases.map(_.semanticTargetId).toSet == actual.useCases.map(_.semanticTargetId).toSet &&
      expected.actors.toSet == actual.actors.toSet &&
      expected.goals.map(value => (value.reference, value.intent)).toSet == actual.goals.map(value => (value.reference, value.intent)).toSet &&
      expected.slices.map(value => value.copy(attribution = attribution)).toSet == actual.slices.toSet &&
      expected.goalLinks.map(value => value.copy(attribution = attribution)).toSet == actual.goalLinks.toSet &&
      expected.specificationLinks.map(value => value.copy(attribution = attribution)).toSet == actual.specificationLinks.toSet &&
      expected.ticketLinks.map(value => value.copy(attribution = attribution)).toSet == actual.ticketLinks.toSet && explicitactors,
      (), "proposed CML does not represent the exact selected editing candidate")
  }
}
