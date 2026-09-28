package org.simplemodeling.textus.cbdsupport.runtime

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 28, 2026
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] final case class InternalModelProjectionBindingScope(
  componentIdentity: String,
  projectionContextIdentity: String,
  selectedUseCaseElementIdentity: String
)

private[runtime] final case class InternalModelProjectionBindingRecord(
  recordKind: String,
  semanticIdentity: String,
  viewRole: String,
  canonicalAssertionIds: Vector[String],
  enrichmentAssertionIds: Vector[String],
  conditionIds: Vector[String],
  sequenceAssertionId: Option[String],
  requestedFieldOrScope: Option[String]
)

private[runtime] final case class InternalModelProjectionBindingView(
  family: String,
  records: Vector[InternalModelProjectionBindingRecord]
)

private[runtime] final case class InternalModelProjectionBinding(
  profile: String,
  schemaVersion: String,
  realizationArtifactId: String,
  scope: InternalModelProjectionBindingScope,
  views: Vector[InternalModelProjectionBindingView],
  canonicalBytes: Vector[Byte]
)

private[runtime] final case class InternalModelProjectionContinuity(
  realization: InternalModelSemanticRealization,
  binding: InternalModelProjectionBinding,
  monoKoto: MonoKotoProjection,
  useCaseCommunication: UseCaseCommunicationProjection,
  entityModel: EntityModelProjection,
  eventModel: EventModelProjection,
  structureView: StructureViewProjection,
  classificationView: ClassificationViewProjection,
  workflow: WorkflowProjection,
  stateMachine: StateMachineProjection
)

/** Reconstructs all eight Phase 9 projections from one verified package and one closed binding. */
private[runtime] object InternalModelProjectionContinuityValidator {
  private sealed trait BindingTarget {
    def identity: String
    def canonicalAssertionIds: Vector[String]
    def enrichmentAssertionIds: Vector[String]
    def conditionIds: Vector[String]
  }

  private final case class ElementTarget(value: InternalModelSemanticElement) extends BindingTarget {
    val identity = value.identity
    val canonicalAssertionIds = value.canonicalAssertionIds
    val enrichmentAssertionIds = value.enrichmentAssertionIds
    val conditionIds = value.conditionIds
  }

  private final case class RelationshipTarget(value: InternalModelSemanticRelationship) extends BindingTarget {
    val identity = value.identity
    val canonicalAssertionIds = value.canonicalAssertionIds
    val enrichmentAssertionIds = value.enrichmentAssertionIds
    val conditionIds = value.conditionIds
  }

  private final case class BoundRecord(
    family: String,
    record: InternalModelProjectionBindingRecord,
    target: BindingTarget,
    attribution: ComponentDashboardSourceAttribution,
    condition: ComponentDashboardCondition,
    sequencekey: Option[String]
  )

  private val _ordered_roles = Set(
    ("UseCaseCommunicationProjection", "UseCaseCommunicationFlowStep"),
    ("WorkflowProjection", "WorkflowProjectionFlowRelation")
  )
  def validate(projectRoot: java.nio.file.Path): Consequence[InternalModelProjectionContinuity] =
    try {
      InternalModelPackageValidator.verifiedProjectionContinuity(projectRoot).flatMap { handoff =>
        InternalModelSemanticRealizationValidator.validateVerified(handoff.realizationPackage).flatMap { realization =>
          _binding(handoff.projection, handoff.realizationPackage.realization.artifactId, realization).flatMap(binding => _construct(realization, binding)).fold(Consequence.operationInvalid, Consequence.success)
        }
      }
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model projection continuity validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  private[runtime] def encode(binding: InternalModelProjectionBinding): Vector[Byte] =
    InternalModelProjectionBindingCodec.encode(binding).toVector

  private def _binding(
    projection: InternalModelVerifiedProjection,
    selectedrealizationartifactid: String,
    realization: InternalModelSemanticRealization
  ): Either[String, InternalModelProjectionBinding] =
    InternalModelProjectionBindingCodec.decode(projection, selectedrealizationartifactid, realization)

  private def _construct(
    realization: InternalModelSemanticRealization,
    binding: InternalModelProjectionBinding
  ): Either[String, InternalModelProjectionContinuity] =
    for {
      records <- binding.views.foldLeft[Either[String, Vector[BoundRecord]]](Right(Vector.empty)) { (result, view) =>
        for {
          collected <- result
          selected <- view.records.foldLeft[Either[String, Vector[BoundRecord]]](Right(Vector.empty)) { (recordsresult, record) =>
            for {
              current <- recordsresult
              bound <- _bound_record(realization, binding, view.family, record)
            } yield current :+ bound
          }
        } yield collected ++ selected
      }
      mono <- _mono_koto(realization, records)
      usecase <- _use_case(realization, records)
      entity <- _entity(realization, records)
      event <- _event(realization, records)
      structure <- _structure(realization, records)
      classification <- _classification(realization, records)
      workflow <- _workflow(realization, records)
      statemachine <- _state_machine(realization, records)
    } yield InternalModelProjectionContinuity(realization, binding, mono, usecase, entity, event, structure, classification, workflow, statemachine)

  private def _bound_record(
    realization: InternalModelSemanticRealization,
    binding: InternalModelProjectionBinding,
    family: String,
    record: InternalModelProjectionBindingRecord
  ): Either[String, BoundRecord] =
    for {
      target <- _target(realization, record)
      _ <- _target_links(target, record, family)
      _ <- _role(realization, target, record, family)
      attribution <- _attribution(realization, target, record, binding.profile)
      condition <- _condition(realization, record.conditionIds)
      sequencekey <- _sequence_key(realization, target, record, family)
    } yield BoundRecord(family, record, target, attribution, condition, sequencekey)

  private def _target(
    realization: InternalModelSemanticRealization,
    record: InternalModelProjectionBindingRecord
  ): Either[String, BindingTarget] =
    record.recordKind match {
      case "element" => realization.elements.filter(_.identity == record.semanticIdentity) match {
        case Vector(element) => Right(ElementTarget(element))
        case Vector() => Left(s"projection binding element ${record.semanticIdentity} is unknown or cross-scope")
        case _ => Left(s"projection binding element ${record.semanticIdentity} is ambiguous")
      }
      case "relationship" => realization.relationships.filter(_.identity == record.semanticIdentity) match {
        case Vector(relationship) => Right(RelationshipTarget(relationship))
        case Vector() => Left(s"projection binding relationship ${record.semanticIdentity} is unknown or cross-scope")
        case _ => Left(s"projection binding relationship ${record.semanticIdentity} is ambiguous")
      }
      case "gap" =>
        for {
          conditionid <- record.conditionIds match {
            case Vector(id) => Right(id)
            case _ => Left(s"projection binding gap ${record.semanticIdentity} must carry exactly one condition ID")
          }
          condition <- realization.conditions.find(_.conditionId == conditionid).toRight(s"projection binding gap ${record.semanticIdentity} names an unknown condition")
          target <- condition.affectedKind match {
            case "element" => realization.elements.find(_.identity == condition.affectedIdentity).map(ElementTarget.apply).toRight(s"projection binding gap ${record.semanticIdentity} condition names an unknown element")
            case "relationship" => realization.relationships.find(_.identity == condition.affectedIdentity).map(RelationshipTarget.apply).toRight(s"projection binding gap ${record.semanticIdentity} condition names an unknown relationship")
            case _ => Left(s"projection binding gap ${record.semanticIdentity} condition has an invalid affected kind")
          }
          _ <- Either.cond(target.identity == record.semanticIdentity, (), s"projection binding gap ${record.semanticIdentity} condition does not affect its exact identity")
          _ <- Either.cond(record.canonicalAssertionIds.isEmpty && record.enrichmentAssertionIds.isEmpty, (), s"projection binding gap ${record.semanticIdentity} must not carry assertions")
          _ <- Either.cond(record.requestedFieldOrScope.contains(condition.detail), (), s"projection binding gap ${record.semanticIdentity} requested field is not its exact condition detail")
        } yield target
      case _ => Left(s"projection binding record ${record.semanticIdentity} has an invalid kind")
    }

  private def _target_links(
    target: BindingTarget,
    record: InternalModelProjectionBindingRecord,
    family: String
  ): Either[String, Unit] =
    if record.recordKind == "gap" then
      Either.cond(record.conditionIds == target.conditionIds, (), s"projection binding $family gap ${record.semanticIdentity} condition links are incomplete or inconsistent")
    else
      for {
        _ <- Either.cond(record.canonicalAssertionIds == target.canonicalAssertionIds, (), s"projection binding $family record ${record.semanticIdentity} canonical links are incomplete or inconsistent")
        _ <- Either.cond(record.enrichmentAssertionIds == target.enrichmentAssertionIds, (), s"projection binding $family record ${record.semanticIdentity} enrichment links are incomplete or inconsistent")
        _ <- Either.cond(record.conditionIds == target.conditionIds, (), s"projection binding $family record ${record.semanticIdentity} condition links are incomplete or inconsistent")
      } yield ()

  private def _role(
    realization: InternalModelSemanticRealization,
    target: BindingTarget,
    record: InternalModelProjectionBindingRecord,
    family: String
  ): Either[String, Unit] = {
    val admitted = InternalModelProjectionBindingCodec.roles(family)
    if record.recordKind == "gap" then
      Either.cond(admitted._1.contains(record.viewRole) || admitted._2.contains(record.viewRole), (), s"projection binding gap ${record.semanticIdentity} has an unsupported requested role")
    else {
      val actual = target match {
        case ElementTarget(element) => element.kind
        case RelationshipTarget(relationship) => relationship.role
      }
      val compatible = target match {
        case _: ElementTarget => admitted._1.contains(record.viewRole)
        case _: RelationshipTarget => admitted._2.contains(record.viewRole)
      }
      val witness = target match {
        case _: ElementTarget => s"kind:${record.viewRole}"
        case _: RelationshipTarget => s"role:${record.viewRole}"
      }
      for {
        _ <- Either.cond(compatible && actual == record.viewRole, (), s"projection binding $family record ${record.semanticIdentity} has an incompatible target kind or role")
        _ <- Either.cond(_canonical_assertions(realization, target).exists(_.content == witness), (), s"projection binding $family record ${record.semanticIdentity} lacks its direct source-witnessed role")
      } yield ()
    }
  }

  private def _attribution(
    realization: InternalModelSemanticRealization,
    target: BindingTarget,
    record: InternalModelProjectionBindingRecord,
    profile: String
  ): Either[String, ComponentDashboardSourceAttribution] = {
    val allassertions = _canonical_assertions(realization, target) ++ _enrichment_assertions(realization, target)
    val conditionreferences = realization.conditions.filter(condition => record.conditionIds.contains(condition.conditionId)).map(_.sourceReferenceId)
    val referenceids = if record.recordKind == "gap" then conditionreferences else if profile == "ccdm-projection-binding-v1" then allassertions.map(_.sourceReferenceId) ++ conditionreferences else {
      val witness = target match {
        case _: ElementTarget => s"kind:${record.viewRole}"
        case _: RelationshipTarget => s"role:${record.viewRole}"
      }
      _canonical_assertions(realization, target).filter(_.content == witness).map(_.sourceReferenceId)
    }
    for {
      _ <- Either.cond(
        if record.recordKind == "gap" then referenceids.size == 1 else if profile == "ccdm-projection-binding-v1" then referenceids.nonEmpty && referenceids.distinct.size == 1 else referenceids.size == 1,
        (),
        if record.recordKind == "gap" then s"projection binding gap ${record.semanticIdentity} must have exactly one condition source"
        else if profile == "ccdm-projection-binding-v1" then s"projection binding record ${record.semanticIdentity} would select a hidden source winner"
        else s"projection binding record ${record.semanticIdentity} must have exactly one direct canonical role witness"
      )
      reference <- realization.sourceReferences.find(_.referenceId == referenceids.head).toRight(s"projection binding record ${record.semanticIdentity} names an unknown source reference")
      expected = target match {
        case _: ElementTarget => InternalModelSemanticTarget("element", target.identity)
        case _: RelationshipTarget => InternalModelSemanticTarget("relationship", target.identity)
      }
      _ <- Either.cond(reference.target.contains(expected), (), s"projection binding record ${record.semanticIdentity} source reference target is not exact")
    } yield ComponentDashboardSourceAttribution(reference.source.identity, reference.source.authority, reference.sourceAnchor)
  }

  private def _condition(
    realization: InternalModelSemanticRealization,
    conditionids: Vector[String]
  ): Either[String, ComponentDashboardCondition] =
    conditionids.foldLeft[Either[String, ComponentDashboardCondition]](Right(ComponentDashboardCondition("unverified", "unverified", None, None, None, None, None, None, Vector.empty))) { (result, id) =>
      for {
        current <- result
        source <- realization.conditions.find(_.conditionId == id).toRight(s"projection binding condition $id is unknown")
        next <- source.kind match {
          case "absence" if current.explicitAbsence.isEmpty => Right(current.copy(explicitAbsence = Some(source.detail)))
          case "ambiguity" if current.ambiguity.isEmpty => Right(current.copy(ambiguity = Some(source.detail)))
          case "conflict" if current.conflict.isEmpty => Right(current.copy(conflict = Some(source.detail)))
          case "malformed" if current.malformedEvidence.isEmpty => Right(current.copy(malformedEvidence = Some(source.detail)))
          case "limitation" => Right(current.copy(limitations = current.limitations :+ source.detail))
          case "authorization-redaction" | "availability-staleness" => Right(current.copy(limitations = current.limitations :+ s"${source.kind}:${source.detail}"))
          case _ => Left(s"projection binding condition $id cannot be compressed without choosing a winner")
        }
      } yield next
    }

  private def _sequence_key(
    realization: InternalModelSemanticRealization,
    target: BindingTarget,
    record: InternalModelProjectionBindingRecord,
    family: String
  ): Either[String, Option[String]] = {
    val ordered = _ordered_roles.contains(family -> record.viewRole)
    if !ordered then Either.cond(record.sequenceAssertionId.isEmpty, None, s"projection binding record ${record.semanticIdentity} has an unexpected sequence witness")
    else target match {
      case RelationshipTarget(_) =>
        for {
          assertionid <- record.sequenceAssertionId.toRight(s"projection binding ordered record ${record.semanticIdentity} lacks a sequence witness")
          assertion <- _canonical_assertions(realization, target).find(_.assertionId == assertionid).toRight(s"projection binding ordered record ${record.semanticIdentity} sequence witness is not canonical and local")
          _ <- Either.cond(assertion.content.startsWith("sequence-key:") && assertion.content.drop("sequence-key:".length).nonEmpty, (), s"projection binding ordered record ${record.semanticIdentity} sequence witness is malformed")
          reference <- realization.sourceReferences.find(_.referenceId == assertion.sourceReferenceId).toRight(s"projection binding ordered record ${record.semanticIdentity} sequence witness source is unknown")
          _ <- Either.cond(reference.target.contains(InternalModelSemanticTarget("relationship", target.identity)), (), s"projection binding ordered record ${record.semanticIdentity} sequence witness source target is not exact")
        } yield Some(assertion.content.drop("sequence-key:".length))
      case _ => Left(s"projection binding ordered record ${record.semanticIdentity} is not a relationship")
    }
  }

  private def _canonical_assertions(realization: InternalModelSemanticRealization, target: BindingTarget): Vector[InternalModelSemanticAssertion] =
    realization.canonicalAssertions.filter(assertion => _matches(assertion.semanticIdentityKind, assertion.semanticIdentity, target))

  private def _enrichment_assertions(realization: InternalModelSemanticRealization, target: BindingTarget): Vector[InternalModelSemanticAssertion] =
    realization.enrichmentAssertions.filter(assertion => _matches(assertion.semanticIdentityKind, assertion.semanticIdentity, target))

  private def _matches(kind: String, identity: String, target: BindingTarget): Boolean =
    (target match {
      case _: ElementTarget => kind == "element"
      case _: RelationshipTarget => kind == "relationship"
    }) && identity == target.identity

  private def _records(records: Vector[BoundRecord], family: String): Vector[BoundRecord] = records.filter(_.family == family)

  private def _elements(records: Vector[BoundRecord], family: String): Vector[BoundRecord] =
    _records(records, family).collect { case record @ BoundRecord(_, InternalModelProjectionBindingRecord("element", _, _, _, _, _, _, _), _, _, _, _) => record }

  private def _relationships(records: Vector[BoundRecord], family: String): Vector[BoundRecord] =
    _records(records, family).collect { case record @ BoundRecord(_, InternalModelProjectionBindingRecord("relationship", _, _, _, _, _, _, _), _, _, _, _) => record }

  private def _gaps(records: Vector[BoundRecord], family: String): Vector[BoundRecord] =
    _records(records, family).collect { case record @ BoundRecord(_, InternalModelProjectionBindingRecord("gap", _, _, _, _, _, _, _), _, _, _, _) => record }

  private def _element_record(records: Vector[BoundRecord], family: String, identity: String): Either[String, BoundRecord] =
    _elements(records, family).filter(_.target.identity == identity) match {
      case Vector(record) => Right(record)
      case Vector() => Left(s"$family endpoint $identity has no exact retained local evidence")
      case _ => Left(s"$family endpoint $identity has ambiguous retained local evidence")
    }

  private def _association(
    realization: InternalModelSemanticRealization,
    record: BoundRecord,
    role: String,
    kind: String
  ): Either[String, InternalModelSemanticAssociation] =
    _canonical_assertions(realization, record.target).flatMap(_.association).filter(association => association.associationRole == role && association.relatedSemanticIdentityKind == kind) match {
      case Vector(association) => Right(association)
      case Vector() => Left(s"projection binding record ${record.target.identity} lacks exact $role association")
      case _ => Left(s"projection binding record ${record.target.identity} has ambiguous $role association")
    }

  private def _component(realization: InternalModelSemanticRealization): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(realization.scope.componentIdentity)

  private def _context(realization: InternalModelSemanticRealization): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(realization.scope.projectionContextIdentity)

  private def _id(value: String): ComponentDashboardSemanticTargetIdentity = ComponentDashboardSemanticTargetIdentity(value)

  private def _mono_koto_owners(records: Vector[BoundRecord], family: String): Either[String, Unit] =
    _relationships(records, family).foldLeft[Either[String, Unit]](Right(())) { (result, relation) =>
      for {
        _ <- result
        relationship <- relation.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Mono-Koto reference must be a relationship") }
        owner <- _elements(records, family).filter(_.target.identity == relationship.sourceElementIdentity) match {
          case Vector(value) => Right(value)
          case Vector() => Left(s"Mono-Koto reference ${relationship.identity} has no retained selected owner")
          case _ => Left(s"Mono-Koto reference ${relationship.identity} has ambiguous retained selected owners")
        }
        expectedrole <- relation.record.viewRole match {
          case "StructuralDomain" => Right("Mono")
          case "BehavioralTemporal" => Right("Koto")
          case _ => Left(s"Mono-Koto reference ${relationship.identity} has an unsupported role")
        }
        _ <- Either.cond(owner.record.viewRole == expectedrole, (), s"Mono-Koto reference ${relationship.identity} has an incompatible retained selected owner")
      } yield ()
    }

  private def _entity_metadata_owners(records: Vector[BoundRecord], family: String): Either[String, Unit] =
    _relationships(records, family).foldLeft[Either[String, Unit]](Right(())) { (result, relation) =>
      for {
        _ <- result
        relationship <- relation.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Entity Model metadata must be a relationship") }
        _ <- _elements(records, family).filter(record => record.target.identity == relationship.sourceElementIdentity && Set("EntityModelEntity", "EntityModelValue", "EntityModelAggregate").contains(record.record.viewRole)) match {
          case Vector(_) => Right(())
          case Vector() => Left(s"Entity Model metadata ${relationship.identity} has no retained selected Entity Model owner")
          case _ => Left(s"Entity Model metadata ${relationship.identity} has ambiguous retained selected Entity Model owners")
        }
      } yield ()
    }

  private def _mono_koto(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, MonoKotoProjection] = {
    val family = "MonoKotoProjection"
    for {
      _ <- Either.cond(_gaps(records, family).isEmpty, (), "Mono-Koto projection cannot represent a gap without a new DTO contract")
      _ <- _mono_koto_owners(records, family)
      subjects <- _elements(records, family).foldLeft[Either[String, Vector[MonoKotoProjectionSubject]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          element <- record.target match { case ElementTarget(value) => Right(value); case _ => Left("Mono-Koto subject must be an element") }
          kind <- record.record.viewRole match { case "Mono" => Right(Mono); case "Koto" => Right(Koto); case _ => Left(s"Mono-Koto subject ${record.target.identity} has an unsupported role") }
          references <- _relationships(records, family).filter(reference => reference.record.viewRole == (if kind == Mono then "StructuralDomain" else "BehavioralTemporal")).filter(_.target.asInstanceOf[RelationshipTarget].value.sourceElementIdentity == element.identity).foldLeft[Either[String, Vector[MonoKotoProjectionReference]]](Right(Vector.empty)) { (referencesresult, relation) =>
            for {
              present <- referencesresult
              relationship <- relation.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Mono-Koto reference must be a relationship") }
              referencekind <- relation.record.viewRole match { case "StructuralDomain" => Right(StructuralDomain); case "BehavioralTemporal" => Right(BehavioralTemporal); case _ => Left("Mono-Koto reference role is unsupported") }
            } yield present :+ MonoKotoProjectionReference(relationship.identity, _component(realization), _id(relationship.targetElementIdentity), referencekind, relation.attribution, relation.condition, None, None)
          }
        } yield collected :+ MonoKotoProjectionSubject(element.identity, _component(realization), _id(element.identity), kind, element.label, record.attribution, record.condition, references, None, None)
      }
      projection <- MonoKotoProjection.create(_context(realization), _component(realization), subjects).left.map(failure => s"Mono-Koto projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _use_case(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, UseCaseCommunicationProjection] = {
    val family = "UseCaseCommunicationProjection"
    val subjectrecords = _elements(records, family).filter(_.record.viewRole == "use-case")
    val flowrecords = _relationships(records, family).filter(_.record.viewRole == "UseCaseCommunicationFlow")
    val steprecords = _relationships(records, family).filter(_.record.viewRole == "UseCaseCommunicationFlowStep")
    for {
      _ <- Either.cond(_gaps(records, family).isEmpty, (), "Use Case projection cannot represent a gap without a new DTO contract")
      subjectrecord <- subjectrecords match { case Vector(record) => Right(record); case Vector() => Left("Use Case projection has no selected use-case subject"); case _ => Left("Use Case projection has multiple selected use-case subjects") }
      subject <- subjectrecord.target match { case ElementTarget(value) => Right(value); case _ => Left("Use Case subject must be an element") }
      _ <- Either.cond(subject.identity == realization.scope.selectedUseCaseElementIdentity, (), "Use Case projection subject is not the selected Use Case")
      flows <- flowrecords.foldLeft[Either[String, Vector[(BoundRecord, InternalModelSemanticRelationship, InternalModelSemanticElement)]]](Right(Vector.empty)) { (result, flowrecord) =>
        for {
          collected <- result
          flow <- flowrecord.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Use Case flow must be a relationship") }
          _ <- Either.cond(flow.sourceElementIdentity == subject.identity && flow.direction == "source-to-target", (), s"Use Case flow ${flow.identity} has an invalid selected Use Case graph source")
          target <- realization.elements.find(_.identity == flow.targetElementIdentity).toRight(s"Use Case flow ${flow.identity} target is unknown")
          _ <- Either.cond(target.identity != subject.identity && _canonical_assertions(realization, ElementTarget(target)).exists(_.content == "kind:use-case-flow"), (), s"Use Case flow ${flow.identity} target lacks direct flow witness")
        } yield collected :+ (flowrecord, flow, target)
      }
      _ <- Either.cond(flows.map(_._2.targetElementIdentity).distinct.size == flows.size, (), "Use Case projection has duplicate selected flow targets")
      steps <- steprecords.foldLeft[Either[String, Vector[(BoundRecord, InternalModelSemanticRelationship, InternalModelSemanticElement)]]](Right(Vector.empty)) { (result, steprecord) =>
        for {
          collected <- result
          step <- steprecord.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Use Case flow step must be a relationship") }
          _ <- Either.cond(step.direction == "source-to-target", (), s"Use Case flow step ${step.identity} has an invalid direction")
          target <- realization.elements.find(_.identity == step.targetElementIdentity).toRight(s"Use Case flow step ${step.identity} target is unknown")
          _ <- Either.cond(target.identity != step.sourceElementIdentity && _canonical_assertions(realization, ElementTarget(target)).exists(_.content == "kind:use-case-flow-step"), (), s"Use Case flow step ${step.identity} target lacks direct step witness")
          _ <- Either.cond(flows.count(_._2.targetElementIdentity == step.sourceElementIdentity) == 1, (), s"Use Case flow step ${step.identity} has no exact selected flow")
        } yield collected :+ (steprecord, step, target)
      }
      _ <- Either.cond(steps.map(_._2.targetElementIdentity).distinct.size == steps.size, (), "Use Case projection has duplicate selected step targets")
      references <- _relationships(records, family).filter(record => !Set("UseCaseCommunicationFlow", "UseCaseCommunicationFlowStep").contains(record.record.viewRole)).foldLeft[Either[String, Vector[UseCaseCommunicationReference]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Use Case reference must be a relationship") }
          _ <- Either.cond(relationship.sourceElementIdentity == subject.identity && relationship.direction == "source-to-target", (), s"Use Case reference ${relationship.identity} does not retain the selected subject as its source")
          kind <- record.record.viewRole match {
            case "Actor" => Right(Actor); case "Goal" => Right(Goal); case "Trigger" => Right(Trigger); case "Postcondition" => Right(Postcondition)
            case "DomainElement" => Right(DomainElement); case "Collaborator" => Right(Collaborator); case "RealizingWorkflow" => Right(RealizingWorkflow); case "MonoKoto" => Right(MonoKoto)
            case _ => Left(s"Use Case reference ${relationship.identity} has an unsupported role")
          }
        } yield collected :+ UseCaseCommunicationReference(relationship.identity, _component(realization), _id(relationship.targetElementIdentity), _id(relationship.identity), kind, record.attribution, record.condition, None, None)
      }
      projectionflows <- flows.foldLeft[Either[String, Vector[UseCaseCommunicationFlow]]](Right(Vector.empty)) { (result, entry) =>
        for {
          collected <- result
          flowrecord = entry._1
          flow = entry._2
          flowsteps <- steps.filter(_._2.sourceElementIdentity == flow.targetElementIdentity).foldLeft[Either[String, Vector[UseCaseCommunicationFlowStep]]](Right(Vector.empty)) { (stepsresult, entry) =>
            for {
              present <- stepsresult
              steprecord = entry._1
              step = entry._2
              sequence <- steprecord.sequencekey.toRight(s"Use Case flow step ${step.identity} lacks source-owned sequence")
            } yield present :+ UseCaseCommunicationFlowStep(step.identity, _component(realization), _id(step.targetElementIdentity), _id(step.identity), sequence, steprecord.attribution, steprecord.condition, None, None)
          }
          _ <- Either.cond(flowsteps.map(_.sequenceKey).distinct.size == flowsteps.size, (), s"Use Case flow ${flow.identity} has duplicate source-owned step sequence keys")
        } yield collected :+ UseCaseCommunicationFlow(flow.identity, _component(realization), _id(flow.targetElementIdentity), _id(flow.identity), flowrecord.attribution, flowrecord.condition, flowsteps.sortBy(_.sequenceKey), None, None)
      }
      projection <- UseCaseCommunicationProjection.create(_context(realization), _component(realization), Vector(UseCaseCommunicationProjectionSubject(subject.identity, _component(realization), _id(subject.identity), subjectrecord.attribution, subjectrecord.condition, references, projectionflows, None, None))).left.map(failure => s"Use Case projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _entity(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, EntityModelProjection] = {
    val family = "EntityModelProjection"
    for {
      _ <- _entity_metadata_owners(records, family)
      subjects <- _elements(records, family).foldLeft[Either[String, Vector[EntityModelProjectionSubject]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          element <- record.target match { case ElementTarget(value) => Right(value); case _ => Left("Entity Model subject must be an element") }
          role <- _entity_role(record.record.viewRole)
          metadata <- _relationships(records, family).filter(_.target.asInstanceOf[RelationshipTarget].value.sourceElementIdentity == element.identity).foldLeft[Either[String, Vector[EntityModelProjectionMetadata]]](Right(Vector.empty)) { (metadataresult, metarecord) =>
            for {
              present <- metadataresult
              relationship <- metarecord.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Entity Model metadata must be a relationship") }
              metarole <- _entity_role(metarecord.record.viewRole)
            } yield present :+ EntityModelProjectionMetadata(relationship.identity, _component(realization), _id(element.identity), _id(relationship.targetElementIdentity), _id(relationship.identity), metarole, metarecord.attribution, metarecord.condition, None, None)
          }
        } yield collected :+ EntityModelProjectionSubject(element.identity, _component(realization), _id(element.identity), role, record.attribution, record.condition, metadata, None, None)
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[EntityModelProjectionGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _entity_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"Entity Model gap ${record.target.identity} lacks a bounded field")
        } yield collected :+ EntityModelProjectionGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
      }
      projection <- EntityModelProjection.create(_context(realization), _component(realization), subjects, gaps).left.map(failure => s"Entity Model projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _event(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, EventModelProjection] = {
    val family = "EventModelProjection"
    for {
      subjects <- _elements(records, family).foldLeft[Either[String, Vector[EventModelProjectionSubject]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _event_role(record.record.viewRole)
        } yield collected :+ EventModelProjectionSubject(record.target.identity, _component(realization), _id(record.target.identity), role, record.attribution, record.condition, None, None)
      }
      assertions <- _relationships(records, family).foldLeft[Either[String, Vector[EventModelProjectionAssertion]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Event Model assertion must be a relationship") }
          role <- _event_role(record.record.viewRole)
          source <- _element_record(records, family, relationship.sourceElementIdentity)
          target <- _element_record(records, family, relationship.targetElementIdentity)
        } yield collected :+ EventModelProjectionAssertion(
          relationship.identity,
          _component(realization),
          _id(relationship.identity),
          role,
          EventModelAssertionDirection(relationship.direction),
          EventModelAssertionEndpoint(s"${relationship.identity}:source", _component(realization), _id(relationship.sourceElementIdentity), EventModelAssertionEndpointRole("source"), source.attribution, source.condition, None, None),
          EventModelAssertionEndpoint(s"${relationship.identity}:target", _component(realization), _id(relationship.targetElementIdentity), EventModelAssertionEndpointRole("target"), target.attribution, target.condition, None, None),
          record.attribution,
          record.condition,
          None
        )
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[EventModelProjectionGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _event_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"Event Model gap ${record.target.identity} lacks a bounded field")
        } yield collected :+ EventModelProjectionGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
      }
      projection <- EventModelProjection.create(_context(realization), _component(realization), subjects, assertions, gaps).left.map(failure => s"Event Model projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _structure(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, StructureViewProjection] = {
    val family = "StructureViewProjection"
    val relationrecords = _relationships(records, family).filter(record => Set("StructureComposition", "StructureAggregation", "StructureAssociation", "StructureContainment", "StructureOwnership").contains(record.record.viewRole))
    val assertionrecords = _relationships(records, family).filter(record => Set("IndependentExistence", "Reassignment", "DeletionLifecycle", "Cardinality", "Navigability").contains(record.record.viewRole))
    for {
      _ <- assertionrecords.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          owner <- _association(realization, record, "owner", "relationship")
          _ <- Either.cond(relationrecords.exists(_.target.identity == owner.relatedSemanticIdentity), (), s"Structure assertion ${record.target.identity} owner is not a retained Structure relation")
        } yield ()
      }
      relations <- relationrecords.foldLeft[Either[String, Vector[StructureViewRelation]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Structure relation must be a relationship") }
          role <- _structure_role(record.record.viewRole)
          assertions <- assertionrecords.filter { candidate =>
            _association(realization, candidate, "owner", "relationship").toOption.exists(_.relatedSemanticIdentity == relationship.identity)
          }.foldLeft[Either[String, Vector[StructureViewAssertion]]](Right(Vector.empty)) { (assertionsresult, assertionrecord) =>
            for {
              present <- assertionsresult
              assertionrelationship <- assertionrecord.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Structure assertion must be a relationship") }
              assertionrole <- _structure_role(assertionrecord.record.viewRole)
              affected <- _optional_association(realization, assertionrecord, "affected-endpoint", "element")
              _ <- Either.cond(affected.forall(identity => Set(relationship.sourceElementIdentity, relationship.targetElementIdentity).contains(identity)), (), s"Structure assertion ${assertionrelationship.identity} affects an endpoint outside its owner relation")
            } yield present :+ StructureViewAssertion(assertionrelationship.identity, _component(realization), _id(relationship.identity), affected.map(_id), assertionrole, assertionrecord.attribution, assertionrecord.condition, None, None)
          }
        } yield collected :+ StructureViewRelation(relationship.identity, _component(realization), _id(relationship.identity), _id(relationship.sourceElementIdentity), _id(relationship.targetElementIdentity), role, record.attribution, record.condition, assertions, None, None)
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[StructureViewGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _structure_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"Structure gap ${record.target.identity} lacks a bounded field")
        } yield record.target match {
          case _: RelationshipTarget => collected :+ StructureViewGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), None, record.attribution, record.condition, field, None, None)
          case _: ElementTarget => collected :+ StructureViewGap(record.target.identity, _component(realization), role, field, None, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
        }
      }
      projection <- StructureViewProjection.create(_context(realization), _component(realization), relations, gaps).left.map(failure => s"Structure projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _classification(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, ClassificationViewProjection] = {
    val family = "ClassificationViewProjection"
    val relationrecords = _relationships(records, family).filter(record => Set("ClassificationGeneralization", "ClassificationSpecialization", "ClassificationTrait", "ClassificationCategory").contains(record.record.viewRole))
    val dimensionrecords = _elements(records, family).filter(_.record.viewRole == "ClassificationPowertypeDimension")
    val assertionrecords = _relationships(records, family).filter(record => Set("ClassificationDimensionValue", "ClassificationDimensionQualifier", "ClassificationDimensionExclusivity", "ClassificationDimensionCoverage", "ClassificationDimensionMembership").contains(record.record.viewRole))
    for {
      _ <- assertionrecords.foldLeft[Either[String, Unit]](Right(())) { (result, record) =>
        for {
          _ <- result
          owner <- _association(realization, record, "owner", "element")
          _ <- _association(realization, record, "subject", "element")
          _ <- Either.cond(dimensionrecords.exists(_.target.identity == owner.relatedSemanticIdentity), (), s"Classification assertion ${record.target.identity} owner is not a retained dimension")
        } yield ()
      }
      relationships <- relationrecords.foldLeft[Either[String, Vector[ClassificationViewRelationship]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Classification relation must be a relationship") }
          role <- _classification_role(record.record.viewRole)
          subjectidentity <- _association(realization, record, "subject", "element").map(_.relatedSemanticIdentity)
          subject <- _element_record(records, family, subjectidentity)
          source <- _element_record(records, family, relationship.sourceElementIdentity)
          target <- _element_record(records, family, relationship.targetElementIdentity)
        } yield collected :+ ClassificationViewRelationship(
          relationship.identity,
          _component(realization),
          _id(relationship.identity),
          ClassificationViewSubject(_id(subjectidentity), _component(realization), subject.attribution, subject.condition),
          ClassificationViewEndpoint(_id(relationship.sourceElementIdentity), _component(realization), "source", source.attribution, source.condition),
          ClassificationViewEndpoint(_id(relationship.targetElementIdentity), _component(realization), "target", target.attribution, target.condition),
          role,
          record.attribution,
          record.condition,
          None,
          None,
          None
        )
      }
      dimensions <- dimensionrecords.foldLeft[Either[String, Vector[ClassificationViewDimension]]](Right(Vector.empty)) { (result, dimensionrecord) =>
        for {
          collected <- result
          dimension <- dimensionrecord.target match { case ElementTarget(value) => Right(value); case _ => Left("Classification dimension must be an element") }
          assertions <- assertionrecords.filter { record =>
            _association(realization, record, "owner", "element").toOption.exists(_.relatedSemanticIdentity == dimension.identity)
          }.foldLeft[Either[String, Vector[ClassificationViewDimensionAssertion]]](Right(Vector.empty)) { (assertionsresult, assertionrecord) =>
            for {
              present <- assertionsresult
              relationship <- assertionrecord.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Classification dimension assertion must be a relationship") }
              role <- _classification_role(assertionrecord.record.viewRole)
              affectedrelationship <- _optional_association(realization, assertionrecord, "affected-relationship", "relationship")
              affectedsubject <- _optional_association(realization, assertionrecord, "affected-subject", "element")
              affectedendpoint <- _optional_association(realization, assertionrecord, "affected-endpoint", "element")
            } yield present :+ ClassificationViewDimensionAssertion(relationship.identity, _component(realization), _id(dimension.identity), _id(relationship.identity), affectedrelationship.map(_id), affectedsubject.map(_id), affectedendpoint.map(_id), role, assertionrecord.attribution, assertionrecord.condition, None, None)
          }
          subjectids <- assertions.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { (subjectresult, assertion) =>
            for {
              present <- subjectresult
              record <- _relationships(records, family).find(_.target.identity == assertion.semanticAssertionId.value).toRight(s"Classification dimension assertion ${assertion.semanticAssertionId.value} is not retained")
              subject <- _association(realization, record, "subject", "element").map(_.relatedSemanticIdentity)
            } yield present :+ subject
          }
          _ <- Either.cond(subjectids.nonEmpty && subjectids.distinct.size == 1, (), s"Classification dimension ${dimension.identity} does not have one unique source-backed bounded subject")
          subjectrecord <- _element_record(records, family, subjectids.head)
          role <- _classification_role(dimensionrecord.record.viewRole)
        } yield collected :+ ClassificationViewDimension(dimension.identity, _component(realization), _id(dimension.identity), ClassificationViewSubject(_id(subjectids.head), _component(realization), subjectrecord.attribution, subjectrecord.condition), role, dimensionrecord.attribution, dimensionrecord.condition, assertions, None, None)
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[ClassificationViewGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _classification_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"Classification gap ${record.target.identity} lacks a bounded field")
        } yield record.target match {
          case _: RelationshipTarget => collected :+ ClassificationViewGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), None, None, None, record.attribution, record.condition, field, None, None)
          case _: ElementTarget if record.record.viewRole == "ClassificationPowertypeDimension" => collected :+ ClassificationViewGap(record.target.identity, _component(realization), role, field, None, None, None, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
          case _: ElementTarget => collected :+ ClassificationViewGap(record.target.identity, _component(realization), role, field, None, Some(_id(record.target.identity)), None, None, record.attribution, record.condition, field, None, None)
        }
      }
      projection <- ClassificationViewProjection.create(_context(realization), _component(realization), relationships, dimensions, gaps).left.map(failure => s"Classification projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _workflow(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, WorkflowProjection] = {
    val family = "WorkflowProjection"
    for {
      subjects <- _elements(records, family).foldLeft[Either[String, Vector[WorkflowProjectionSubject]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _workflow_role(record.record.viewRole)
        } yield collected :+ WorkflowProjectionSubject(record.target.identity, _component(realization), _id(record.target.identity), role, record.attribution, record.condition, None, None)
      }
      flows <- _relationships(records, family).foldLeft[Either[String, Vector[WorkflowProjectionFlow]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("Workflow flow must be a relationship") }
          workflowidentity <- _association(realization, record, "owner", "element").map(_.relatedSemanticIdentity)
          owner <- _element_record(records, family, workflowidentity)
          _ <- Either.cond(owner.record.viewRole == "WorkflowProjectionWorkflow", (), s"Workflow flow ${relationship.identity} owner is not a retained Workflow subject")
          source <- _element_record(records, family, relationship.sourceElementIdentity)
          target <- _element_record(records, family, relationship.targetElementIdentity)
          sequence <- record.sequencekey.toRight(s"Workflow flow ${relationship.identity} lacks source-owned sequence")
        } yield collected :+ WorkflowProjectionFlow(
          relationship.identity,
          _component(realization),
          _id(relationship.identity),
          _id(workflowidentity),
          WorkflowProjectionFlowRelation,
          WorkflowProjectionFlowDirection(relationship.direction),
          WorkflowProjectionSourceOwnedSequenceKey(sequence),
          WorkflowProjectionFlowEndpoint(s"${relationship.identity}:source", _component(realization), _id(relationship.sourceElementIdentity), WorkflowProjectionFlowEndpointRole("source"), source.attribution, source.condition, None, None),
          WorkflowProjectionFlowEndpoint(s"${relationship.identity}:target", _component(realization), _id(relationship.targetElementIdentity), WorkflowProjectionFlowEndpointRole("target"), target.attribution, target.condition, None, None),
          record.attribution,
          record.condition,
          None
        )
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[WorkflowProjectionGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _workflow_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"Workflow gap ${record.target.identity} lacks a bounded field")
        } yield collected :+ WorkflowProjectionGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
      }
      projection <- WorkflowProjection.create(_context(realization), _component(realization), subjects, flows, gaps).left.map(failure => s"Workflow projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _state_machine(realization: InternalModelSemanticRealization, records: Vector[BoundRecord]): Either[String, StateMachineProjection] = {
    val family = "StateMachineProjection"
    for {
      subjects <- _elements(records, family).foldLeft[Either[String, Vector[StateMachineProjectionSubject]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _state_machine_role(record.record.viewRole)
        } yield collected :+ StateMachineProjectionSubject(record.target.identity, _component(realization), _id(record.target.identity), role, record.attribution, record.condition, None, None)
      }
      transitions <- _relationships(records, family).filter(_.record.viewRole == "StateMachineProjectionTransitionRelation").foldLeft[Either[String, Vector[StateMachineProjectionTransition]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("StateMachine transition must be a relationship") }
          statemachineidentity <- _association(realization, record, "owner", "element").map(_.relatedSemanticIdentity)
          owner <- _element_record(records, family, statemachineidentity)
          _ <- Either.cond(owner.record.viewRole == "StateMachineProjectionStateMachine", (), s"StateMachine transition ${relationship.identity} owner is not a retained StateMachine subject")
          source <- _element_record(records, family, relationship.sourceElementIdentity)
          target <- _element_record(records, family, relationship.targetElementIdentity)
          _ <- Either.cond(source.record.viewRole == "StateMachineProjectionState" && target.record.viewRole == "StateMachineProjectionState", (), s"StateMachine transition ${relationship.identity} endpoints are not retained State subjects")
        } yield collected :+ StateMachineProjectionTransition(
          relationship.identity,
          _component(realization),
          _id(relationship.identity),
          _id(statemachineidentity),
          StateMachineProjectionTransitionRelation,
          StateMachineProjectionTransitionDirection(relationship.direction),
          StateMachineProjectionTransitionEndpoint(s"${relationship.identity}:source", _component(realization), _id(relationship.sourceElementIdentity), StateMachineProjectionTransitionEndpointRole("source"), source.attribution, source.condition, None, None),
          StateMachineProjectionTransitionEndpoint(s"${relationship.identity}:target", _component(realization), _id(relationship.targetElementIdentity), StateMachineProjectionTransitionEndpointRole("target"), target.attribution, target.condition, None, None),
          record.attribution,
          record.condition,
          None
        )
      }
      adjuncts <- _relationships(records, family).filter(_.record.viewRole == "StateMachineProjectionTransitionAdjunctRelation").foldLeft[Either[String, Vector[StateMachineProjectionTransitionAdjunct]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          relationship <- record.target match { case RelationshipTarget(value) => Right(value); case _ => Left("StateMachine adjunct must be a relationship") }
          transitionidentity <- _association(realization, record, "owner", "relationship").map(_.relatedSemanticIdentity)
          transition <- transitions.find(_.semanticTransitionId.value == transitionidentity).toRight(s"StateMachine adjunct ${relationship.identity} owner is not a retained transition")
          endpoints = Vector(relationship.sourceElementIdentity, relationship.targetElementIdentity).flatMap(identity => _element_record(records, family, identity).toOption)
          eligible = endpoints.filter(endpoint => Set("StateMachineProjectionTrigger", "StateMachineProjectionGuard", "StateMachineProjectionAction", "StateMachineProjectionActivity", "StateMachineProjectionOperation", "StateMachineProjectionEvent", "StateMachineProjectionRule").contains(endpoint.record.viewRole))
          endpoint <- eligible match {
            case Vector(value) => Right(value)
            case Vector() => Left(s"StateMachine adjunct ${relationship.identity} has no eligible retained adjunct endpoint")
            case _ => Left(s"StateMachine adjunct ${relationship.identity} has ambiguous retained adjunct endpoints")
          }
        } yield collected :+ StateMachineProjectionTransitionAdjunct(
          relationship.identity,
          _component(realization),
          _id(relationship.identity),
          transition.semanticTransitionId,
          StateMachineProjectionTransitionAdjunctRelation,
          StateMachineProjectionTransitionAdjunctDirection(relationship.direction),
          StateMachineProjectionAdjunctEndpoint(s"${relationship.identity}:adjunct", _component(realization), _id(endpoint.target.identity), StateMachineProjectionAdjunctEndpointRole(endpoint.record.viewRole), endpoint.attribution, endpoint.condition, None, None),
          record.attribution,
          record.condition,
          None
        )
      }
      gaps <- _gaps(records, family).foldLeft[Either[String, Vector[StateMachineProjectionGap]]](Right(Vector.empty)) { (result, record) =>
        for {
          collected <- result
          role <- _state_machine_role(record.record.viewRole)
          field <- record.record.requestedFieldOrScope.toRight(s"StateMachine gap ${record.target.identity} lacks a bounded field")
        } yield collected :+ StateMachineProjectionGap(record.target.identity, _component(realization), role, field, Some(_id(record.target.identity)), record.attribution, record.condition, field, None, None)
      }
      projection <- StateMachineProjection.create(_context(realization), _component(realization), subjects, transitions, adjuncts, gaps).left.map(failure => s"StateMachine projection construction failed: ${failure.violations.mkString(", ")}")
    } yield projection
  }

  private def _optional_association(
    realization: InternalModelSemanticRealization,
    record: BoundRecord,
    role: String,
    kind: String
  ): Either[String, Option[String]] = {
    val associations = _canonical_assertions(realization, record.target).flatMap(_.association).filter(association => association.associationRole == role && association.relatedSemanticIdentityKind == kind)
    associations match {
      case Vector() => Right(None)
      case Vector(association) => Right(Some(association.relatedSemanticIdentity))
      case _ => Left(s"projection binding record ${record.target.identity} has ambiguous optional $role association")
    }
  }

  private def _entity_role(value: String): Either[String, EntityModelProjectionRole] = value match {
    case "EntityModelEntity" => Right(EntityModelEntity)
    case "EntityModelValue" => Right(EntityModelValue)
    case "EntityModelAggregate" => Right(EntityModelAggregate)
    case "IdentityMetadata" => Right(IdentityMetadata)
    case "OwnershipMetadata" => Right(OwnershipMetadata)
    case "LifecycleMetadata" => Right(LifecycleMetadata)
    case "AggregateBoundaryMetadata" => Right(AggregateBoundaryMetadata)
    case _ => Left(s"unsupported Entity Model role $value")
  }

  private def _event_role(value: String): Either[String, EventModelProjectionRole] = value match {
    case "EventModelCommand" => Right(EventModelCommand)
    case "EventModelEvent" => Right(EventModelEvent)
    case "EventModelCausalAssertion" => Right(EventModelCausalAssertion)
    case "EventModelConsequenceAssertion" => Right(EventModelConsequenceAssertion)
    case "EventModelAffectedDomainElementAssertion" => Right(EventModelAffectedDomainElementAssertion)
    case "EventModelGeneratedStateEffectAssertion" => Right(EventModelGeneratedStateEffectAssertion)
    case _ => Left(s"unsupported Event Model role $value")
  }

  private def _structure_role(value: String): Either[String, StructureViewProjectionRole] = value match {
    case "StructureComposition" => Right(StructureComposition)
    case "StructureAggregation" => Right(StructureAggregation)
    case "StructureAssociation" => Right(StructureAssociation)
    case "StructureContainment" => Right(StructureContainment)
    case "StructureOwnership" => Right(StructureOwnership)
    case "IndependentExistence" => Right(IndependentExistence)
    case "Reassignment" => Right(Reassignment)
    case "DeletionLifecycle" => Right(DeletionLifecycle)
    case "Cardinality" => Right(Cardinality)
    case "Navigability" => Right(Navigability)
    case _ => Left(s"unsupported Structure role $value")
  }

  private def _classification_role(value: String): Either[String, ClassificationViewProjectionRole] = value match {
    case "ClassificationGeneralization" => Right(ClassificationGeneralization)
    case "ClassificationSpecialization" => Right(ClassificationSpecialization)
    case "ClassificationTrait" => Right(ClassificationTrait)
    case "ClassificationCategory" => Right(ClassificationCategory)
    case "ClassificationPowertypeDimension" => Right(ClassificationPowertypeDimension)
    case "ClassificationDimensionValue" => Right(ClassificationDimensionValue)
    case "ClassificationDimensionQualifier" => Right(ClassificationDimensionQualifier)
    case "ClassificationDimensionExclusivity" => Right(ClassificationDimensionExclusivity)
    case "ClassificationDimensionCoverage" => Right(ClassificationDimensionCoverage)
    case "ClassificationDimensionMembership" => Right(ClassificationDimensionMembership)
    case _ => Left(s"unsupported Classification role $value")
  }

  private def _workflow_role(value: String): Either[String, WorkflowProjectionRole] = value match {
    case "WorkflowProjectionWorkflow" => Right(WorkflowProjectionWorkflow)
    case "WorkflowProjectionActivity" => Right(WorkflowProjectionActivity)
    case "WorkflowProjectionParticipant" => Right(WorkflowProjectionParticipant)
    case "WorkflowProjectionDomainElement" => Right(WorkflowProjectionDomainElement)
    case "WorkflowProjectionOperation" => Right(WorkflowProjectionOperation)
    case "WorkflowProjectionEvent" => Right(WorkflowProjectionEvent)
    case "WorkflowProjectionPublishedStateEffect" => Right(WorkflowProjectionPublishedStateEffect)
    case "WorkflowProjectionFlowRelation" => Right(WorkflowProjectionFlowRelation)
    case _ => Left(s"unsupported Workflow role $value")
  }

  private def _state_machine_role(value: String): Either[String, StateMachineProjectionRole] = value match {
    case "StateMachineProjectionStateMachine" => Right(StateMachineProjectionStateMachine)
    case "StateMachineProjectionState" => Right(StateMachineProjectionState)
    case "StateMachineProjectionTrigger" => Right(StateMachineProjectionTrigger)
    case "StateMachineProjectionGuard" => Right(StateMachineProjectionGuard)
    case "StateMachineProjectionAction" => Right(StateMachineProjectionAction)
    case "StateMachineProjectionActivity" => Right(StateMachineProjectionActivity)
    case "StateMachineProjectionOperation" => Right(StateMachineProjectionOperation)
    case "StateMachineProjectionEvent" => Right(StateMachineProjectionEvent)
    case "StateMachineProjectionRule" => Right(StateMachineProjectionRule)
    case "StateMachineProjectionTransitionRelation" => Right(StateMachineProjectionTransitionRelation)
    case "StateMachineProjectionTransitionAdjunctRelation" => Right(StateMachineProjectionTransitionAdjunctRelation)
    case _ => Left(s"unsupported StateMachine role $value")
  }

}
