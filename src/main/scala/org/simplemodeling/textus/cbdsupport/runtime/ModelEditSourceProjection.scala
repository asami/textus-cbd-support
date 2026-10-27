package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Source-owned Cozy records. JSON is a cross-Scala boundary, never an opaque model sidecar. */
object ModelEditSourceProjection {
  final case class RecordRef(owner: String, identity: String, revision: Long)
  final case class Source(owner: String, identity: String, revision: String)
  final case class Producer(name: String, version: String, revision: String, modified: Boolean)
  final case class Field(name: String, datatype: String, multiplicity: String)
  final case class Element(ref: RecordRef, kind: String, title: String, intent: Option[String],
    fields: Vector[Field], scope: Vector[RecordRef], acceptance: Vector[RecordRef],
    lifecycle: Option[String], source: Source)
  final case class Specification(owner: String, identity: String, sourceRevision: String)
  final case class Observation(state: String, revision: Option[String])
  final case class Ticket(provider: String, owner: String, externalIdentity: String, observation: Option[Observation])
  enum Endpoint {
    case Local(ref: RecordRef)
    case External(ref: RecordRef)
    case SpecificationTarget(specification: Specification)
    case TicketTarget(ticket: Ticket)
  }
  final case class Relationship(ref: RecordRef, kind: String, from: Endpoint, to: Endpoint, source: Source)
  final case class Coverage(family: String, status: String, detail: Option[String])
  final case class Document(schemaVersion: String, coverage: Vector[Coverage],
    elements: Vector[Element], relationships: Vector[Relationship])
  final case class Limitation(code: String, detail: String)
  final case class Publication(schemaVersion: String, source: Source, producer: Producer,
    document: Document, limitations: Vector[Limitation])
  final case class Rendering(schemaVersion: String, source: Source, producer: Producer,
    candidateSource: Source, text: String, limitations: Vector[Limitation])
  /** All identities are supplied; a qualified source spelling is not a CBD identifier. */
  final case class Binding(sourceRef: RecordRef, semanticIdentity: ComponentDashboardSemanticTargetIdentity,
    recordReference: InternalModelRecordReference, communicationId: String)
  final case class OwnerBinding(sourceOwner: String, owner: ModelEditSession.RepositoryProjectReference)
  final case class Selection(publication: Publication, sourceArtifact: InternalModelArtifactReference,
    bindings: Vector[Binding], owners: Vector[OwnerBinding], condition: ComponentDashboardCondition) {
    def binding(reference: RecordRef): Either[String, Binding] = bindings.filter(_.sourceRef == reference) match {
      case Vector(value) => Right(value)
      case _ => Left("source reference requires exactly one explicit CBD binding: " + reference)
    }
    def owner(value: String): Either[String, ModelEditSession.RepositoryProjectReference] =
      owners.filter(_.sourceOwner == value) match {
        case Vector(value) => Right(value.owner)
        case _ => Left("external owner requires exactly one repository/project binding: " + value)
      }
    def complete(family: String): Boolean = publication.document.coverage.exists(value =>
      value.family == family && Set("present", "missing")(value.status))
  }
  val elementKinds: Vector[String] = Vector("actor", "goal", "use-case", "slice", "command", "operation",
    "aggregate", "entity", "domain-event", "policy", "component", "query", "view")
  val relationshipKinds: Vector[String] = Vector("actor-goal", "goal-use-case", "use-case-slice",
    "slice-specification", "slice-ticket", "cause", "reaction", "dependency", "context")
  val families: Vector[String] = elementKinds ++ relationshipKinds

  def validate(publication: Publication): Either[String, Publication] = {
    val document = publication.document
    val refs = document.elements.map(_.ref)
    val allrefs = refs ++ document.relationships.map(_.ref)
    val coverages = document.coverage
    def _valid_ref_(reference: RecordRef): Boolean = reference.owner.nonEmpty && reference.identity.nonEmpty && reference.revision > 0
    def _valid_endpoint_(endpoint: Endpoint): Boolean = endpoint match {
      case Endpoint.Local(ref) => refs.contains(ref)
      case Endpoint.External(ref) => _valid_ref_(ref)
      case Endpoint.SpecificationTarget(value) => value.owner.nonEmpty && value.identity.nonEmpty && value.sourceRevision.nonEmpty
      case Endpoint.TicketTarget(value) => value.provider.nonEmpty && value.owner.nonEmpty && value.externalIdentity.nonEmpty
    }
    def _kind_(endpoint: Endpoint, expected: String): Boolean = endpoint match {
      case Endpoint.Local(ref) => document.elements.exists(value => value.ref == ref && value.kind == expected)
      case _ => false
    }
    def _relationship_kind_(relationship: Relationship): Boolean = relationship.kind match {
      case "actor-goal" => _kind_(relationship.from, "actor") && _kind_(relationship.to, "goal")
      case "goal-use-case" => _kind_(relationship.from, "goal") && _kind_(relationship.to, "use-case")
      case "use-case-slice" => _kind_(relationship.from, "use-case") && _kind_(relationship.to, "slice")
      case "slice-specification" => _kind_(relationship.from, "slice") && relationship.to.isInstanceOf[Endpoint.SpecificationTarget]
      case "slice-ticket" => _kind_(relationship.from, "slice") && relationship.to.isInstanceOf[Endpoint.TicketTarget]
      case "cause" => relationship.from.isInstanceOf[Endpoint.Local] && relationship.to.isInstanceOf[Endpoint.Local]
      case "reaction" => _kind_(relationship.from, "domain-event") && _kind_(relationship.to, "policy")
      case "dependency" => relationship.to.isInstanceOf[Endpoint.External] || _kind_(relationship.to, "component")
      case "context" => true
      case _ => false
    }
    def _present_(family: String): Boolean = coverages.exists(value => value.family == family && value.status == "present")
    for {
      _ <- Either.cond(publication.schemaVersion == "cozy.cml.model-edit-production.v1" &&
        document.schemaVersion == "cozy.cml.model-edit.v1", (), "unsupported source publication version")
      _ <- Either.cond(publication.source.owner.nonEmpty && publication.source.identity.nonEmpty && publication.source.revision.nonEmpty &&
        publication.producer.name == "cozy" && publication.producer.version.nonEmpty && publication.producer.revision.nonEmpty,
        (), "source and executing producer identity are required")
      _ <- Either.cond(allrefs.forall(_valid_ref_) && allrefs.map(ref => (ref.owner, ref.identity)).distinct.size == allrefs.size,
        (), "source record references must be valid and uniquely owned")
      _ <- Either.cond(coverages.map(_.family).sorted == families.sorted && coverages.forall(value =>
        Set("present", "missing", "unavailable", "unsupported", "external").contains(value.status) &&
        (value.status == "present" || value.detail.exists(_.trim.nonEmpty))), (), "known unique source coverage is required")
      _ <- Either.cond(document.elements.forall(value => elementKinds.contains(value.kind) && _present_(value.kind) && value.title.trim.nonEmpty &&
        (!Set("goal", "slice")(value.kind) || value.intent.exists(_.trim.nonEmpty)) &&
        (if (value.kind == "slice") value.lifecycle.exists(Set("Proposed", "InDevelopment", "Implemented", "Abandoned")) else value.lifecycle.isEmpty) &&
        value.fields.map(_.name).distinct.size == value.fields.size && value.fields.forall(field =>
          field.name.trim.nonEmpty && field.datatype.trim.nonEmpty && Set("1", "?", "*", "+")(field.multiplicity)) &&
        value.source.owner == publication.source.owner && value.source.identity == publication.source.identity && value.source.revision.nonEmpty && (value.scope ++ value.acceptance).forall(_valid_ref_)), (), "invalid source element")
      _ <- Either.cond(document.relationships.forall(value => relationshipKinds.contains(value.kind) && _present_(value.kind) && _relationship_kind_(value) &&
        value.source.owner == publication.source.owner && value.source.identity == publication.source.identity && value.source.revision.nonEmpty && _valid_endpoint_(value.from) && _valid_endpoint_(value.to)), (), "invalid explicit source relationship")
    } yield publication
  }

  def importCandidate(selection: Selection, reference: InternalModelRecordReference,
    scope: ModelEditSession.Scope): Either[String, ModelEditSession.Candidate] = {
    import ModelEditSession.*
    val publication = selection.publication
    val document = publication.document
    val attribution = ComponentDashboardSourceAttribution(publication.source.identity,
      publication.source.owner, publication.source.identity)
    val condition = selection.condition
    def _collect_[A, B](values: Vector[A])(f: A => Either[String, B]): Either[String, Vector[B]] =
      values.foldLeft[Either[String, Vector[B]]](Right(Vector.empty))((result, value) =>
        for { collected <- result; item <- f(value) } yield collected :+ item)
    def _local_(endpoint: Endpoint): Either[String, Binding] = endpoint match {
      case Endpoint.Local(ref) => selection.binding(ref)
      case _ => Left("development relationship requires an explicit local endpoint")
    }
    def _communication_(binding: Binding, kind: UseCaseCommunicationReferenceKind,
      relationship: Binding): UseCaseCommunicationReference = UseCaseCommunicationReference(binding.communicationId,
      scope.component, binding.semanticIdentity, relationship.semanticIdentity, kind, attribution, condition, None, None)
    for {
      _ <- validate(publication)
      _ <- Either.cond(selection.sourceArtifact.role == InternalModelArtifactRole.SourceSnapshot &&
        selection.bindings.map(_.sourceRef).distinct.size == selection.bindings.size &&
        selection.bindings.map(_.semanticIdentity).distinct.size == selection.bindings.size &&
        selection.bindings.map(_.recordReference.recordId).distinct.size == selection.bindings.size &&
        selection.bindings.map(_.communicationId).distinct.size == selection.bindings.size && selection.bindings.forall(_.communicationId.trim.nonEmpty),
        (), "source selection requires a source snapshot and unique explicit identities")
      _ <- _collect_(document.elements.map(_.ref) ++ document.relationships.map(_.ref))(selection.binding)
      uses <- _collect_(document.elements.filter(_.kind == "use-case")) { element =>
        selection.binding(element.ref).map(binding => UseCaseCommunicationProjectionSubject(binding.communicationId,
          scope.component, binding.semanticIdentity, attribution, condition, Vector.empty, Vector.empty, None, None))
      }
      goals <- _collect_(document.elements.filter(_.kind == "goal")) { element =>
        for {
          binding <- selection.binding(element.ref)
          intent <- element.intent.filter(_.nonEmpty).toRight("Goal intent is not supplied")
        } yield ModelEditSession.ActorGoal(_communication_(binding, Goal, binding), None, intent, attribution)
      }
      slices <- _collect_(document.elements.filter(_.kind == "slice")) { element =>
        for {
          binding <- selection.binding(element.ref)
          edges = document.relationships.filter(value => value.kind == "use-case-slice" && value.to == Endpoint.Local(element.ref))
          edge <- edges match { case Vector(value) => Right(value); case _ => Left("Slice requires one explicit owning Use Case") }
          usecase <- _local_(edge.from)
          intent <- element.intent.filter(_.nonEmpty).toRight("Slice intent is not supplied")
          lifecycle <- SliceLifecycle.values.find(value => element.lifecycle.contains(value.toString)).toRight("Slice lifecycle is not supplied")
          scoperefs <- _collect_(element.scope)(selection.binding)
          acceptance <- _collect_(element.acceptance)(selection.binding)
        } yield UseCaseSlice(binding.recordReference, binding.semanticIdentity, usecase.semanticIdentity, element.title,
          intent, scoperefs.map(_.recordReference), acceptance.map(_.recordReference), lifecycle, attribution,
          element.fields.map(field => SliceField(field.name, field.datatype, field.multiplicity)))
      }
      goallinks <- _collect_(document.relationships.filter(_.kind == "goal-use-case")) { edge =>
        for { binding <- selection.binding(edge.ref); from <- _local_(edge.from); to <- _local_(edge.to) }
        yield GoalUseCaseRelationship(binding.recordReference, from.semanticIdentity, to.semanticIdentity, attribution)
      }
      specifications <- _collect_(document.relationships.filter(_.kind == "slice-specification")) { edge =>
        for {
          binding <- selection.binding(edge.ref); from <- _local_(edge.from)
          target <- edge.to match { case Endpoint.SpecificationTarget(value) => Right(value); case _ => Left("invalid Specification endpoint") }
          owner <- selection.owner(target.owner)
          identity <- InternalModelRecordId.from(target.identity); revision <- InternalModelRecordId.from(target.sourceRevision)
        } yield SliceSpecificationRelationship(binding.recordReference, from.semanticIdentity, SpecificationReference(owner, identity, revision), attribution)
      }
      tickets <- _collect_(document.relationships.filter(_.kind == "slice-ticket")) { edge =>
        for {
          binding <- selection.binding(edge.ref); from <- _local_(edge.from)
          target <- edge.to match { case Endpoint.TicketTarget(value) => Right(value); case _ => Left("invalid Ticket endpoint") }
          owner <- selection.owner(target.owner); provider <- InternalModelRecordId.from(target.provider)
          identity <- InternalModelRecordId.from(target.externalIdentity)
          observation <- target.observation match {
            case None => Right(None)
            case Some(value) => for {
              state <- InternalModelRecordId.from(value.state)
              revision <- value.revision.fold[Either[String, Option[InternalModelRecordId]]](Right(None))(value => InternalModelRecordId.from(value).map(Some(_)))
            } yield Some(TicketObservation(Some(state), revision))
          }
        } yield SliceTicketRelationship(binding.recordReference, from.semanticIdentity, TicketRef(provider, owner, identity), observation, attribution)
      }
      actors <- _collect_(document.elements.filter(_.kind == "actor"))(element => selection.binding(element.ref))
    } yield {
      // Independent source Actors are not inferred communication edges of each Use Case.

      Candidate(reference, scope, uses, slices, specifications, tickets, goallinks, Vector(selection.sourceArtifact), goals, actors.map(value => _communication_(value, Actor, value)))
    }
  }
  /** Explicit legacy canonical identifiers, supplied by the caller rather than allocated from content. */
  private[runtime] final case class CanonicalWitness(target: InternalModelSemanticTarget,
    sourceRef: Option[RecordRef], external: Option[Endpoint], assertionId: String,
    sourceReferenceId: String, sourceAnchor: String)
  private[runtime] final case class CanonicalProduction(snapshot: Vector[Byte], realization: InternalModelSemanticRealization)

  /** Project the qualified source into the existing model-context/realization owners. */
  private[runtime] def projectCanonical(selection: Selection, scope: InternalModelSemanticScope,
    snapshotReference: InternalModelArtifactReference, realizationReference: InternalModelRecordReference,
    witnesses: Vector[CanonicalWitness]): Either[String, CanonicalProduction] = {
    import io.circe.Json
    import java.nio.charset.StandardCharsets
    val publication = selection.publication
    val document = publication.document
    val records = document.elements.map(value => (value.ref, "element", value.kind, value.title)) ++
      document.relationships.map(value => (value.ref, "relationship", value.kind, value.kind))
    def _identity_endpoint_(endpoint: Endpoint): Endpoint = endpoint match {
      case Endpoint.TicketTarget(ticket) => Endpoint.TicketTarget(ticket.copy(observation = None))
      case other => other
    }
    val endpoints = document.relationships.flatMap(value => Vector(value.from, value.to)).map(_identity_endpoint_).filter {
      case Endpoint.Local(_) => false
      case _ => true
    }.distinct
    def _external_label_(endpoint: Endpoint): String = endpoint match {
      case Endpoint.External(ref) => ref.identity
      case Endpoint.SpecificationTarget(value) => value.identity
      case Endpoint.TicketTarget(value) => value.externalIdentity
      case Endpoint.Local(ref) => ref.identity
    }
    def _endpoint_(endpoint: Endpoint): Either[String, String] = endpoint match {
      case Endpoint.Local(ref) => selection.binding(ref).map(_.semanticIdentity.value)
      case other => witnesses.find(_.external.exists(value => _identity_endpoint_(value) == _identity_endpoint_(other))).map(_.target.semanticIdentity).toRight("external endpoint needs an explicit canonical binding")
    }
    val source = InternalModelSemanticSource(publication.source.owner, publication.source.identity,
      Some(publication.source.identity), Some(publication.source.revision))
    for {
      _ <- validate(publication)
      _ <- Either.cond(snapshotReference.role == InternalModelArtifactRole.SourceSnapshot && families.forall(selection.complete) &&
        publication.limitations.isEmpty, (), "canonical source projection requires complete source scope and an explicit snapshot")
      _ <- Either.cond(witnesses.map(_.assertionId).distinct.size == witnesses.size &&
        witnesses.map(_.sourceReferenceId).distinct.size == witnesses.size && witnesses.map(_.target).distinct.size == witnesses.size &&
        witnesses.forall(value => value.assertionId.nonEmpty && value.sourceReferenceId.nonEmpty && value.sourceAnchor.nonEmpty),
        (), "canonical witness identifiers must be explicit, nonempty and unique")
      local <- records.foldLeft[Either[String, Vector[(CanonicalWitness, String, String)]]](Right(Vector.empty)) { case (result, (ref, kind, vocabulary, label)) =>
        for {
          collected <- result
          binding <- selection.binding(ref)
          witness <- witnesses.filter(value => value.sourceRef.contains(ref) && value.external.isEmpty) match {
            case Vector(value) if value.target == InternalModelSemanticTarget(kind, binding.semanticIdentity.value) && value.sourceAnchor == ref.identity => Right(value)
            case _ => Left("canonical witness must select the exact source record, kind and semantic mapping")
          }
        } yield collected :+ (witness, (if (kind == "element") "kind:" else "role:") + vocabulary, label)
      }
      external <- endpoints.foldLeft[Either[String, Vector[(CanonicalWitness, String, String)]]](Right(Vector.empty)) { (result, endpoint) =>
        for {
          collected <- result
          witness <- witnesses.filter(value => value.external.exists(target => _identity_endpoint_(target) == endpoint) && value.sourceRef.isEmpty) match {
            case Vector(value) if value.target.semanticIdentityKind == "element" && value.sourceAnchor == _external_label_(endpoint) => Right(value)
            case _ => Left("external source endpoint requires an explicit canonical element binding")
          }
        } yield collected :+ (witness, "kind:external-reference", _external_label_(endpoint))
      }
      _ <- Either.cond(witnesses.size == local.size + external.size, (), "canonical witness inventory contains unconsumed records")
      relationships <- document.relationships.foldLeft[Either[String, Vector[InternalModelSemanticRelationship]]](Right(Vector.empty)) { (result, edge) =>
        for {
          collected <- result; binding <- selection.binding(edge.ref); from <- _endpoint_(edge.from); to <- _endpoint_(edge.to)
          witness <- witnesses.find(_.sourceRef.contains(edge.ref)).toRight("relationship witness missing")
        } yield collected :+ InternalModelSemanticRelationship(binding.semanticIdentity.value, edge.kind, from, to,
          edge.kind, "source-to-target", Vector(witness.assertionId), Vector.empty, Vector.empty)
      }
      selected <- selection.bindings.find(_.semanticIdentity.value == scope.selectedUseCaseElementIdentity).toRight("selected Use Case mapping missing")
      _ <- Either.cond(document.elements.exists(value => value.ref == selected.sourceRef && value.kind == "use-case"), (), "selected canonical subject is not the source Use Case")
    } yield {
      val facts = (local ++ external).sortBy(value => (value._1.target.semanticIdentityKind, value._1.target.semanticIdentity, value._1.sourceAnchor))
      val assertions = facts.map { case (witness, content, _) => InternalModelSemanticAssertion(witness.assertionId,
        witness.target.semanticIdentityKind, witness.target.semanticIdentity, witness.sourceReferenceId, content, Vector.empty, None) }.sortBy(_.assertionId)
      val references = facts.map { case (witness, _, _) => InternalModelSemanticSourceReference(witness.sourceReferenceId,
        snapshotReference, witness.sourceAnchor, source, Some(witness.target)) }.sortBy(_.referenceId)
      val elements = facts.collect { case (witness, content, label) if witness.target.semanticIdentityKind == "element" =>
        InternalModelSemanticElement(witness.target.semanticIdentity, content.stripPrefix("kind:"), label,
          Vector(witness.assertionId), Vector.empty, Vector.empty)
      }.sortBy(_.identity)
      val factjson = facts.map { case (witness, content, _) => Json.obj("componentIdentity" -> Json.fromString(scope.componentIdentity),
        "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity),
        "semanticIdentityKind" -> Json.fromString(witness.target.semanticIdentityKind), "semanticIdentity" -> Json.fromString(witness.target.semanticIdentity),
        "sourceAnchor" -> Json.fromString(witness.sourceAnchor), "content" -> Json.fromString(content), "limitations" -> Json.arr()) }
      val snapshot = Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("model-context"),
        "source" -> Json.obj("authority" -> Json.fromString(source.authority), "identity" -> Json.fromString(source.identity),
          "locator" -> Json.fromString(source.locator.get), "revision" -> Json.fromString(source.revision.get)),
        "basis" -> Json.obj("contextIdentity" -> Json.fromString(scope.projectionContextIdentity), "facts" -> Json.fromValues(factjson)))
      CanonicalProduction(snapshot.noSpaces.getBytes(StandardCharsets.UTF_8).toVector,
        InternalModelSemanticRealization("ccdm-realization-v3", "3.0", realizationReference, scope, assertions, Vector.empty,
          elements, relationships.sortBy(_.identity), references, Vector.empty, Vector.empty, Vector(snapshotReference)))
    }
  }

}
