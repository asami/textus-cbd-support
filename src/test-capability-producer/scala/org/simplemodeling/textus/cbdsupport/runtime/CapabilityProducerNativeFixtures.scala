package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.operation.CmlOperationDefinition
import org.goldenport.cncf.workflow.*
import org.goldenport.cncf.statemachine.*
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*

/*
 * Independently authored original public native contracts and finite scope.
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityProducerNativeFixtures {
  val storeComponent: ComponentId = ComponentId("org.simplemodeling.textus.Store")
  val scope: Scope = Scope("explicit-production-fixture",
    Source("Model:能力/A", "model/capability-model.cml", "scope:r1", None),
    Vector(
      ModelFact(ModelElementId("Model:能力/A", "usecase:one"), ModelKind.UseCase,
        Source("Model:能力/A", "model/capability-model.cml", "fact:usecase:one", None)),
      ModelFact(ModelElementId("Model:能力/A", "goal:two"), ModelKind.Goal,
        Source("Model:能力/A", "model/capability-model.cml", "fact:goal:two", None)),
      ModelFact(ModelElementId("Model:能力/A", "scenario:one"), ModelKind.Scenario,
        Source("Model:能力/A", "model/capability-model.cml", "fact:scenario:one", None)),
      ModelFact(ModelElementId("Model:能力/A", "concept:Order"), ModelKind.DomainConcept,
        Source("Model:能力/A", "model/capability-model.cml", "fact:concept:Order", None))),
    Vector(TermFact(TermId("BoK:語彙", "term:注文|α"), Source("BoK:語彙", "catalog/terms.cml", "term:r8", None))),
    Vector(RelationFact(RelationId("vocab:关系", "rel:uses|α"), Source("vocab:关系", "catalog/relations.cml", "relation:r3", None))),
    Vector(ProfileFact(ProfileId("profile:scope", "profile:one"), Source("profile:scope", "catalog/profiles.cml", "profile:r7", None))),
    Vector(_historical("contract:一", "history:contract:一"), _historical("contract:三", "history:contract:三")),
    Vector.empty)

  private def _historical(contractversion: String, revision: String): CapabilityDefinition =
    CapabilityDefinition(ModelElementId("Model:能力/A", "Component:Store|α"), contractversion,
      "Historical Store capability", Absent(AbsenceReason.NotRepresented, "Historical summary unavailable."),
      Absent(AbsenceReason.NotRepresented, "Historical description unavailable."),
      Source("Model:能力/A", "model/capability-model.cml", revision, None),
      Absent(AbsenceReason.NotRepresented, "Historical discovery unavailable."),
      Absent(AbsenceReason.NotRepresented, "Historical grounding unavailable."),
      Absent(AbsenceReason.NotRepresented, "Historical traceability unavailable."))

  val operation: CmlOperationDefinition = CmlOperationDefinition(
    name = "SaveOperation", kind = "command", summary = Some("Declared Save metadata"),
    execution = Some("async"), commandKind = Some("command"),
    jobDefinitionRef = Some("PoisonJobDefinition"), implementation = Some("must.never.execute.PoisonSaveOperation"),
    inputType = "SaveInput", outputType = "SaveOutput", inputValueKind = "record")
  val workflow: WorkflowDefinition = WorkflowDefinition(WorkflowDefinitionId("cncf", "save_metadata"), "SaveWorkflow",
    Vector(WorkflowRegistration("SaveRegistration", "SavedEvent", "Orders", "id", "status",
      Vector(WorkflowStatusRule("Open", "Finish")))))
  val stateMachine: CmlStateMachineDefinition = {
    val identity = CmlStateMachineIdentity("StoreMachine")
    val version = CmlStateMachineVersion(7)
    val state = CmlStateMachineStateIdentity(identity, CmlStateMachineStatePath(Vector("Open")))
    val transition = CmlStateMachineTransitionIdentity(identity, 0)
    val triggeridentity = CmlStateMachineTriggerIdentity(identity, "Finish")
    val trigger = CmlStateMachineTrigger(triggeridentity,
      CmlStateMachineTriggerContext(CmlStateMachineTriggerContextIdentity(triggeridentity), version, Vector.empty))
    val guard = CmlStateMachineGuardProgram.Named(CmlStateMachineGuardIdentity(transition, "Allowed"), "PoisonDeclaredGuard")
    val action = CmlStateMachineActionBinding(
      CmlStateMachineActionIdentity(transition, CmlStateMachineActionPhase.Transition, 0), "PoisonFinishAction")
    val edge = CmlStateMachineTransition(transition, state, CmlStateMachineTransitionTarget.Final, trigger, 0, guard,
      Vector(action), Vector.empty, CmlStateMachineSourceLocation(identity, Vector("transitions", "Finish")))
    val normalized = CmlNormalizedStateMachine(identity, version, state,
      Vector(CmlStateMachineStateDefinition(state, CmlStateMachineStateKind.Leaf)), Vector(edge), Vector(transition))
    CmlStateMachineDefinition("StoreMachine", states = Vector("Open"), events = Vector("Finish"), normalized = Some(normalized))
  }

  val operationSnapshot: NativeSnapshot = NativeSnapshot(storeComponent,
    Source("Model:能力/A", "model/capability-model.cml", "target:operation:save", Some(3)),
    Vector(operation), Vector.empty, Vector.empty)
  val workflowSnapshot: NativeSnapshot = NativeSnapshot(storeComponent,
    Source("Model:能力/A", "model/capability-model.cml", "target:workflow:save", Some(3)),
    Vector.empty, Vector(workflow), Vector.empty)
  val stateMachineSnapshot: NativeSnapshot = NativeSnapshot(storeComponent,
    Source("Model:能力/A", "model/capability-model.cml", "target:state:store", Some(3)),
    Vector.empty, Vector.empty, Vector(stateMachine))
  val operationBinding: NativeBinding = NativeBinding(storeComponent, operationSnapshot.source,
    TargetContract(TargetKind.Operation, ModelElementId("Model:能力/A", "operation:save"), storeComponent,
      "operation:v3", "operation.abi:v7", Source("Model:能力/A", "model/capability-model.cml", "target:operation:save", None)),
    "SaveOperation")
  val workflowBinding: NativeBinding = NativeBinding(storeComponent, workflowSnapshot.source,
    TargetContract(TargetKind.Workflow, ModelElementId("Model:能力/A", "workflow:save"), storeComponent,
      "workflow:v4", "workflow.abi:v8", Source("Model:能力/A", "model/capability-model.cml", "target:workflow:save", None)),
    "SaveWorkflow")
  val stateMachineBinding: NativeBinding = NativeBinding(storeComponent, stateMachineSnapshot.source,
    TargetContract(TargetKind.StateMachine, ModelElementId("Model:能力/A", "state:store"), storeComponent,
      "state:v5", "state.abi:v9", Source("Model:能力/A", "model/capability-model.cml", "target:state:store", None)),
    "StoreMachine")
  val snapshots: Vector[NativeSnapshot] = Vector(operationSnapshot, workflowSnapshot, stateMachineSnapshot)
  val bindings: Vector[NativeBinding] = Vector(operationBinding, workflowBinding, stateMachineBinding)
  val storeInput: NativeInput = NativeInput(NativeAcquisition.Acquired(scope.scopeId, snapshots), ScopeInput.Ready(scope), bindings)
  val emptyInput: NativeInput = NativeInput(NativeAcquisition.Acquired(scope.scopeId, Vector.empty), ScopeInput.Ready(scope), Vector.empty)

  final class CountingProvider(val input: NativeInput) extends ComponentCapabilityMetadataProvider {
    private var _reads = 0
    def reads: Int = _reads
    def componentCapabilityMetadata: NativeInput = { _reads += 1; input }
  }
}
