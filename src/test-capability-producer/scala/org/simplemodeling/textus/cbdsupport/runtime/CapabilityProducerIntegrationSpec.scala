package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.goldenport.cncf.component.ComponentFactory
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityProducerIntegrationFixtures.*
import CapabilityProducerNativeFixtures.{CountingProvider, operation, workflow, stateMachine,
  operationSnapshot, workflowSnapshot, stateMachineSnapshot, operationBinding, workflowBinding, stateMachineBinding}

/*
 * Normal producer -> configured Scala 3 -> public Factory -> CBD behavior.
 * @since   Oct. 14, 2026
 * @version Oct. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityProducerIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Actual normal Capability production and public intake" should {
    "retain separately compiled products and native ownership" which {
      "C11R01 agree across all six compiled accessors and Directory discovery" in {
        withIntake { fixture =>
          Given("four actual single-source normal productions, six configured Scala 3 accessors and independently authored original native metadata")
          fixture.cmlInputs shouldBe Vector.empty
          val selected = for { suffix <- Vector("First", "Second"); id <- Vector("Store", "Archive", "Remote") } yield {
            val nativeprovider = provider(id)
            (id, suffix, nativeprovider, fixture.directory(id, suffix, nativeprovider))
          }
          When("public ComponentFactory discovers each actual bundle and each compiled accessor supplies its semantic projection")
          val results = selected.map { (id, suffix, nativeprovider, pkg) =>
            (id, suffix, nativeprovider, right(ComponentFactory.discoverCapabilities(pkg.expected.input, nativeprovider)), accessor(id, suffix))
          }
          Then("each complete projection agrees while independent domains and all three Store native originals survive without execution")
          results.size shouldBe 6
          results.foreach { (id, suffix, nativeprovider, discovered, compiled) =>
            nativeprovider.reads shouldBe 1
            discovered.nativeInput should be theSameInstanceAs nativeprovider.input
            discovered.entries.map(_.loaded.projection) shouldBe Vector(compiled)
            compiled.producer shouldBe Producer("cozy", fixture.products.cozyVersion)
            compiled.schemaVersion shouldBe 1
            compiled.generatedAbiVersion shouldBe "cncf.component-capability.v1"
            compiled.component shouldBe coordinate(id)
            compiled.packageCoordinate shouldBe packageCoordinate(id)
            compiled.source shouldBe header(id, suffix)
            compiled.capabilities should not be empty
            if (id == "Store") _assert_native(discovered)
            else discovered.entries.flatMap(_.realizations) shouldBe Vector.empty
          }
        }
      }

      "C11R02 ingest actual Store and Remote Directory products with complete primary Application without CML" in {
        withIntake { fixture =>
          Given("source-free normal Store/Remote bundles and the separately acquired complete primary Application with explicit unavailable forward UseCase")
          fixture.cmlInputs shouldBe Vector.empty
          val storeprovider = provider("Store")
          val remoteprovider = provider("Remote")
          val selected = fixture.selection(Vector(fixture.directory("Store", "First", storeprovider),
            fixture.directory("Remote", "First", remoteprovider)), fixture.application("primary", "First"))
          val reference = snapshot("producer-first-snapshot", 77)
          When("CBD ingests that exact complete candidate through its sole public Factory acquisition facade")
          val transition = right(CapabilityIngestion.ingest(State.empty, attempt("producer-first-attempt", 77, selected, reference)))
          val accepted = right(CapabilityIngestion.readCurrent(transition.state,
            CurrentSelection(reference, transition.state.snapshots.head.provenance, Right(()))))
          Then("the full Application, Provided/Required semantics, native originals and external evidence are retained as one accepted snapshot")
          transition.outcome shouldBe Outcome.Accepted(reference)
          Vector(storeprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          accepted.components.map(_.acquired.input.component) shouldBe Vector(coordinate("Store"), coordinate("Remote"))
          accepted.components.head.nativeInput should be theSameInstanceAs storeprovider.input
          _assert_native(accepted.components.head)
          val declarations = accepted.components.head.entries.head.loaded.projection.declarations
          declarations.map(_.direction) shouldBe Vector(Direction.Provided, Direction.Required)
          _present(declarations.head.realizations).map(_.targetKind) shouldBe Vector(TargetKind.Operation, TargetKind.Workflow, TargetKind.StateMachine)
          declarations(1).realizations shouldBe Absent(AbsenceReason.NotRepresented, "No required realization representation.")
          val application = available(accepted.application)
          _assert_primary(application.projection, header("Store", "First"))
          application.unresolvedExternal.map(_.target) should contain allOf(
            ExternalTarget.ComponentCapability(CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二")),
            ExternalTarget.ApplicationCapability(ApplicationCapabilityReference(ModelElementId("Other:能力/B", "Application:Remote"), "app:v1")),
            ExternalTarget.Term(TermId("BoK:語彙", "term:注文|α")))
          accepted.useCase shouldBe useCase
          accepted.useCase.reason shouldBe "not-yet-admitted"
          accepted.provenance.packages shouldBe selected.packages.map(_.expected)
          fixture.cmlInputs shouldBe Vector.empty
        }
      }

      "C11R03 discover and ingest normal Store and Remote CARs with actual compiled carriers after source removal" in {
        withIntake { fixture =>
          Given("normal package-car products built from the real six compiled Scala 3 carriers, selected CNCF JAR and complete original generated projects")
          val archives = fixture.packageCars()
          fixture.cmlInputs shouldBe Vector.empty
          val selected = Vector("Store", "Remote").zip(archives).map { (id, archive) =>
            val nativeprovider = provider(id)
            fixture.selected(id, "First", Some(nativeprovider), Location.Car(archive))
          }
          When("public Factory discovers the source-free CAR and Directory pairs and CBD ingests the two CARs with the complete primary Application")
          val pairs = Vector("Store", "Remote").zip(selected).map { (id, pkg) =>
            val directoryprovider = provider(id)
            val car = right(ComponentFactory.discoverCapabilities(pkg.expected.input, pkg.provider.get))
            val directory = right(ComponentFactory.discoverCapabilities(fixture.directory(id, "First", directoryprovider).expected.input, directoryprovider))
            (id, car, directory)
          }
          val intake = fixture.selection(Vector("Store", "Remote").zip(archives).map { (id, archive) =>
            fixture.selected(id, "First", Some(provider(id)), Location.Car(archive))
          }, fixture.application("primary", "First"))
          val reference = snapshot("producer-car-snapshot", 79)
          val transition = right(CapabilityIngestion.ingest(State.empty, attempt("producer-car-attempt", 79, intake, reference)))
          Then("normal CAR metadata agrees with actual Directory/accessor semantics and keeps original native procedures without Component activation")
          transition.outcome shouldBe Outcome.Accepted(reference)
          pairs.foreach { (id, car, directory) =>
            car.entries.map(_.loaded.projection) shouldBe directory.entries.map(_.loaded.projection)
            car.entries.map(_.loaded.projection) shouldBe Vector(accessor(id, "First"))
            if (id == "Store") _assert_native(car)
          }
          val accepted = transition.state.snapshots.head
          _assert_native(accepted.components.head)
          _assert_primary(available(accepted.application).projection, header("Store", "First"))
          accepted.provenance.packages.map(_.input.location) shouldBe archives.map(Location.Car(_))
          intake.packages.map(_.provider.get.asInstanceOf[CountingProvider].reads) shouldBe Vector(1, 1)
          fixture.cmlInputs shouldBe Vector.empty
        }
      }
    }
  }

  "Complete separately produced Application records" should {
    "preserve authored fields and availability" which {
      "C11R04 roundtrip and store each complete primary and secondary product separately" in {
        withIntake { fixture =>
          for { suffix <- Vector("First", "Second"); family <- Vector("primary", "secondary") } {
            Given("the complete actual " + family + " " + suffix + " Application product and its independent one-document header")
            val selected = fixture.directories(if (family == "primary") Vector("Store", "Remote") else Vector("Remote"),
              suffix, fixture.application(family, suffix))
            val id = family + "-" + suffix
            val reference = snapshot("application-" + id, 80)
            When("the strict codec decodes, encodes, decodes and admits the full product and CBD stores its complete intake")
            val projection = fixture.decodedApplication(family, suffix)
            val roundtrip = right(CapabilityApplicationProjectionCodec.decode(right(CapabilityApplicationProjectionCodec.encode(projection))))
            val transition = right(CapabilityIngestion.ingest(State.empty, attempt("application-attempt-" + id, 80, selected, reference)))
            val stored = available(transition.state.snapshots.head.application)
            val admitted = right(CapabilityApplicationProjectionCodec.admit(projection, CapabilityProducerNativeFixtures.scope,
              transition.state.snapshots.head.components.flatMap(_.entries.flatMap(_.loaded.projection.capabilities))))
            Then("every definition, Presence, qualified identity, ordered origin and independent source remains in that original product")
            transition.outcome shouldBe Outcome.Accepted(reference)
            projection.documents.size shouldBe 1
            projection.producer shouldBe Producer("cozy", fixture.products.cozyVersion)
            projection.schemaVersion shouldBe 1
            projection.generatedAbiVersion shouldBe "cozy.application-capability.v1"
            roundtrip shouldBe projection
            stored shouldBe admitted
            stored.projection shouldBe projection
            if (family == "primary") _assert_primary(stored.projection, header("Store", suffix))
            else _assert_secondary(stored.projection, header("Remote", suffix))
          }
        }
      }

      "C11R05 retain Archive Provided Present(empty), Store Required Absent and unavailable provider distinctly" in {
        withIntake { fixture =>
          Given("actual nonempty Archive/Remote Capability products with honest acquired empty native metadata and a separate Store declaration")
          val archiveprovider = provider("Archive")
          val remoteprovider = provider("Remote")
          val selected = fixture.selection(Vector(fixture.directory("Archive", "First", archiveprovider),
            fixture.directory("Remote", "First", remoteprovider)), fixture.application("primary", "First"))
          val missing = selected.copy(packages = selected.packages.updated(0, selected.packages.head.copy(provider = None)))
          val storeprovider = provider("Store")
          When("CBD acquires the honest empty-native selection, Factory discovers Store and the same selection lacks its Archive provider")
          val candidate = right(CapabilityIngestion.acquire(selected))
          val store = right(ComponentFactory.discoverCapabilities(fixture.directory("Store", "First", storeprovider).expected.input, storeprovider))
          val rejected = left(CapabilityIngestion.acquire(missing))
          Then("Present(empty) and Absent remain authored successful facts while the unavailable provider cannot become scoped empty success")
          Vector(archiveprovider.reads, remoteprovider.reads) shouldBe Vector(1, 1)
          candidate.components.head.nativeInput should be theSameInstanceAs archiveprovider.input
          archiveprovider.input.acquisition shouldBe NativeAcquisition.Acquired(CapabilityProducerNativeFixtures.scope.scopeId, Vector.empty)
          candidate.components.head.entries.head.loaded.projection.capabilities should not be empty
          val archive = candidate.components.head.entries.head.loaded.projection.declarations.head
          archive.direction shouldBe Direction.Provided
          archive.realizations shouldBe Present(Vector.empty)
          candidate.components.head.entries.head.realizations shouldBe Vector.empty
          val required = store.entries.head.loaded.projection.declarations.find(_.direction == Direction.Required).get
          required.realizations shouldBe Absent(AbsenceReason.NotRepresented, "No required realization representation.")
          available(candidate.application).projection.documents.head.applicationCapabilities.size shouldBe 3
          rejected.kind shouldBe DiagnosticKind.Unavailable
          rejected.location shouldBe "/packages/0/provider"
        }
      }
    }
  }

  "Explicit current and historical production selection" should {
    "retain all witnesses and reject the whole changed candidate" which {
      "C11R06 retain Store and Archive witnesses but reject complete primary Local ambiguity without a winner" in {
        withIntake { fixture =>
          Given("an accepted Store/Remote snapshot and three actual separately discoverable Store/Archive/Remote packages witnessing one Local definition twice")
          val baseline = fixture.directories(Vector("Store", "Remote"), "First", fixture.application("primary", "First"))
          val reference = snapshot("ambiguity-prior-snapshot", 81)
          val prior = right(CapabilityIngestion.ingest(State.empty, attempt("ambiguity-prior-attempt", 81, baseline, reference))).state
          val unavailable = fixture.directories(Vector("Store", "Archive", "Remote"), "First", applicationUnavailable)
          val ambiguous = unavailable.copy(application = fixture.application("primary", "First"))
          When("CBD retains all Component witnesses with Application explicitly unavailable and then tries the complete primary Application Local mapping")
          val retained = right(CapabilityIngestion.acquire(unavailable))
          val transition = right(CapabilityIngestion.ingest(prior, attempt("ambiguity-rejected-attempt", 82, ambiguous, snapshot("ambiguity-rejected-snapshot", 82))))
          Then("both exact-definition contexts survive independently and ambiguity appends one failure without changing accepted current or history")
          retained.components.map(_.acquired.input.component) shouldBe Vector(coordinate("Store"), coordinate("Archive"), coordinate("Remote"))
          retained.components.take(2).map(_.entries.head.loaded.projection.capabilities.head.identity) shouldBe
            Vector.fill(2)(ModelElementId("Model:能力/A", "Component:Store|α"))
          retained.components.take(2).map(_.entries.head.loaded.projection.capabilities.head.contractVersion) shouldBe Vector.fill(2)("contract:二")
          retained.application shouldBe ApplicationFamily.Unavailable(applicationUnavailable.asInstanceOf[ApplicationInput.Unavailable].value)
          _assert_native(retained.components.head)
          val diagnostic = _rejected(transition.outcome)
          diagnostic.kind shouldBe DiagnosticKind.Ambiguous
          diagnostic.location should include("componentMappings/value/0/target")
          transition.state.current shouldBe prior.current
          transition.state.snapshots should be theSameInstanceAs prior.snapshots
          transition.state.attempts.size shouldBe 2
          transition.state.attempts.last.attempt.selection should be theSameInstanceAs ambiguous
          right(CapabilityIngestion.readHistorical(transition.state, reference)) should be theSameInstanceAs prior.snapshots.head
        }
      }

      "C11R07 select Second current while retaining exact First historical native and Application objects" in {
        withIntake { fixture =>
          Given("First and Second actual normal products with independently supplied local references 77 and 78 and unchanged authored definitions/native contracts")
          val firstselection = fixture.directories(Vector("Store", "Remote"), "First", fixture.application("primary", "First"))
          val secondselection = fixture.directories(Vector("Store", "Remote"), "Second", fixture.application("primary", "Second"))
          val firstref = snapshot("history-first-snapshot", 77)
          val secondref = snapshot("history-second-snapshot", 78)
          val first = right(CapabilityIngestion.ingest(State.empty, attempt("history-first-attempt", 77, firstselection, firstref)))
          val firstsnapshot = first.state.snapshots.head
          val firstapplication = available(firstsnapshot.application).projection
          When("CBD ingests the independently selected Second production and explicitly reads Second current and First history")
          val second = right(CapabilityIngestion.ingest(first.state, attempt("history-second-attempt", 78, secondselection, secondref)))
          val historical = right(CapabilityIngestion.readHistorical(second.state, firstref))
          val current = right(CapabilityIngestion.readCurrent(second.state,
            CurrentSelection(secondref, second.state.snapshots.last.provenance, Right(()))))
          Then("the original First snapshot remains intact while explicit Second headers alone select current without cross-domain aliases")
          second.outcome shouldBe Outcome.Accepted(secondref)
          second.state.current shouldBe Some(secondref)
          second.state.snapshots.map(_.reference) shouldBe Vector(firstref, secondref)
          second.state.attempts.map(_.attempt.reference.value.recordRevision.value) shouldBe Vector(77L, 78L)
          historical should be theSameInstanceAs firstsnapshot
          historical.candidate should be theSameInstanceAs firstsnapshot.candidate
          available(historical.application).projection should be theSameInstanceAs firstapplication
          historical.components should be theSameInstanceAs firstsnapshot.components
          historical.components.head.nativeInput should be theSameInstanceAs CapabilityProducerNativeFixtures.storeInput
          _assert_native(historical.components.head)
          _assert_native(current.components.head)
          historical.components.map(_.entries.head.loaded.projection.source) shouldBe Vector(header("Store", "First"), header("Remote", "First"))
          current.components.map(_.entries.head.loaded.projection.source) shouldBe Vector(header("Store", "Second"), header("Remote", "Second"))
          _assert_primary(available(current.application).projection, header("Store", "Second"))
          current.components.head.entries.head.loaded.projection.capabilities.head.source.sourceRevision shouldBe "definition-r1"
          current.components.head.entries.head.realizations.map(_.target.source.sourceRevision) shouldBe
            Vector("target:operation:save", "target:workflow:save", "target:state:store")
          current.reference.value.recordRevision.value shouldBe 78L
          current.components.head.entries.head.loaded.projection.component.version shouldBe "1.2.3-SNAPSHOT"
          current.components.head.entries.head.loaded.projection.packageCoordinate.version shouldBe "package:r9"
        }
      }

      "C11R08 retain stale header/current and original Unauthorized or Unavailable native failures without historical fallback" in {
        withIntake { fixture =>
          Given("an accepted First then Second state and independent stale expectations and original rejected native acquisition observations")
          val firstselection = fixture.directories(Vector("Store", "Remote"), "First", fixture.application("primary", "First"))
          val secondselection = fixture.directories(Vector("Store", "Remote"), "Second", fixture.application("primary", "Second"))
          val firstref = snapshot("failure-first-snapshot", 77)
          val secondref = snapshot("failure-second-snapshot", 78)
          val first = right(CapabilityIngestion.ingest(State.empty, attempt("failure-first-attempt", 77, firstselection, firstref))).state
          val prior = right(CapabilityIngestion.ingest(first, attempt("failure-second-attempt", 78, secondselection, secondref))).state
          val originalresource = secondselection.packages.head.expected.resources.head
          val stalepackage = secondselection.packages.head.copy(expected = secondselection.packages.head.expected.copy(
            resources = Vector(originalresource.copy(source = Source("Model:能力/A", "model/capability-model.cml", "header:primary-r42", None)))))
          val stale = secondselection.copy(packages = secondselection.packages.updated(0, stalepackage))
          val currentexpected = prior.snapshots.last.provenance.copy(packages = prior.snapshots.last.provenance.packages.updated(0, stalepackage.expected))
          val diagnostics = Vector(DiagnosticKind.Unauthorized, DiagnosticKind.Unavailable).map { kind =>
            Diagnostic(kind, "Original independent native acquisition refusal.", Some(CapabilityProducerNativeFixtures.scope.scopeId),
              Some(coordinate("Remote").componentId), Some(header("Remote", "Second")), "/observed/native/Remote")
          }
          When("CBD attempts the stale header and both rejected native acquisitions and explicitly checks stale current provenance and historical-as-current")
          val staleattempt = attempt("failure-stale-attempt", 79, stale, snapshot("failure-stale-snapshot", 79))
          val staletransition = right(CapabilityIngestion.ingest(prior, staleattempt))
          val failures = diagnostics.zipWithIndex.foldLeft((staletransition.state, Vector.empty[(Transition, Diagnostic)])) { case ((state, values), (diagnostic, index)) =>
            val rejectedinput = NativeInput(NativeAcquisition.Rejected(diagnostic), ScopeInput.Ready(CapabilityProducerNativeFixtures.scope), Vector.empty)
            val remote = secondselection.packages(1).copy(provider = Some(new CountingProvider(rejectedinput)))
            val selected = secondselection.copy(packages = secondselection.packages.updated(1, remote))
            val revision = 80L + index
            val result = right(CapabilityIngestion.ingest(state,
              attempt("failure-native-attempt-" + index, revision, selected, snapshot("failure-native-snapshot-" + index, revision))))
            (result.state, values :+ (result -> diagnostic))
          }
          val currenterror = left(CapabilityIngestion.readCurrent(failures._1, CurrentSelection(secondref, currentexpected, Right(()))))
          val historicalerror = left(CapabilityIngestion.readCurrent(failures._1, CurrentSelection(firstref, prior.snapshots.head.provenance, Right(()))))
          Then("all failure kinds and original native diagnostics remain failures, append only rejected attempts and preserve the exact accepted state")
          _rejected(staletransition.outcome).kind shouldBe DiagnosticKind.Stale
          staletransition.state.attempts.last.attempt should be theSameInstanceAs staleattempt
          failures._2.foreach { (result, diagnostic) =>
            _rejected(result.outcome) should be theSameInstanceAs diagnostic
            result.state.snapshots should be theSameInstanceAs prior.snapshots
            result.state.current shouldBe Some(secondref)
          }
          failures._1.snapshots.map(_.reference) shouldBe Vector(firstref, secondref)
          failures._1.attempts.size shouldBe 5
          currenterror.kind shouldBe DiagnosticKind.Stale
          historicalerror.kind shouldBe DiagnosticKind.Stale
          right(CapabilityIngestion.readHistorical(failures._1, firstref)) should be theSameInstanceAs prior.snapshots.head
          right(CapabilityIngestion.readCurrent(failures._1, CurrentSelection(secondref, prior.snapshots.last.provenance, Right(())))) should be theSameInstanceAs prior.snapshots.last
        }
      }
    }
  }

  private def _present[A](presence: Presence[A]): A = presence match {
    case Present(value) => value
    case _ => fail("Expected the authored Present branch.")
  }
  private def _rejected(outcome: Outcome): Diagnostic = outcome match {
    case Outcome.Rejected(diagnostic) => diagnostic
    case _ => fail("Expected whole-candidate rejection.")
  }
  private def _assert_native(discovered: ComponentCapabilityDiscovery.Discovered): Unit = {
    val targets = discovered.entries.head.realizations
    targets.map(_.target.targetKind) shouldBe Vector(TargetKind.Operation, TargetKind.Workflow, TargetKind.StateMachine)
    targets.map(_.binding) shouldBe Vector(operationBinding, workflowBinding, stateMachineBinding)
    targets.map(_.snapshot) shouldBe Vector(operationSnapshot, workflowSnapshot, stateMachineSnapshot)
    targets(0).binding should be theSameInstanceAs operationBinding
    targets(1).binding should be theSameInstanceAs workflowBinding
    targets(2).binding should be theSameInstanceAs stateMachineBinding
    targets(0).snapshot should be theSameInstanceAs operationSnapshot
    targets(1).snapshot should be theSameInstanceAs workflowSnapshot
    targets(2).snapshot should be theSameInstanceAs stateMachineSnapshot
    targets(0).contract match {
      case NativeContract.Operation(definition) =>
        definition should be theSameInstanceAs operation
        definition.execution shouldBe Some("async")
        definition.commandKind shouldBe Some("command")
        definition.jobDefinitionRef shouldBe Some("PoisonJobDefinition")
        definition.implementation shouldBe Some("must.never.execute.PoisonSaveOperation")
      case _ => fail("Expected original Operation metadata.")
    }
    targets(1).contract match {
      case NativeContract.Workflow(definition) =>
        definition should be theSameInstanceAs workflow
        definition.registrations should be theSameInstanceAs workflow.registrations
        definition.registrations.head.statusRules.map(rule => (rule.currentStatus, rule.nextAction)) shouldBe Vector("Open" -> "Finish")
      case _ => fail("Expected original Workflow metadata.")
    }
    targets(2).contract match {
      case NativeContract.StateMachine(definition) =>
        definition should be theSameInstanceAs stateMachine
        definition.normalized.get should be theSameInstanceAs stateMachine.normalized.get
        definition.normalized.get.states.map(_.identity.path.render) shouldBe Vector("Open")
        definition.normalized.get.transitions.head should be theSameInstanceAs stateMachine.normalized.get.transitions.head
        definition.normalized.get.transitions.head.sourceLocation.declarationPath shouldBe Vector("transitions", "Finish")
        definition.normalized.get.transitions.head.actions.size shouldBe 1
        definition.normalized.get.version.value shouldBe 7
      case _ => fail("Expected original normalized StateMachine metadata.")
    }
    targets.map(_.snapshot.source.line) shouldBe Vector.fill(3)(Some(3))
    targets.map(_.target.source.line) shouldBe Vector.fill(3)(None)
    targets.map(_.target.contractVersion) shouldBe Vector("operation:v3", "workflow:v4", "state:v5")
    targets.map(_.target.generatedAbiVersion) shouldBe Vector("operation.abi:v7", "workflow.abi:v8", "state.abi:v9")
    val portable = right(ComponentCapabilityProjectionCodec.encode(discovered.entries.head.loaded.projection))
    portable should not include ("PoisonSaveOperation")
    portable should not include ("PoisonJobDefinition")
    portable should not include ("PoisonDeclaredGuard")
    portable should not include ("PoisonFinishAction")
  }

  private def _assert_primary(projection: ApplicationProjection, source: Source): Unit = {
    projection.documents.map(_.source) shouldBe Vector(source)
    val definitions = projection.documents.head.applicationCapabilities
    definitions.map(_.identity) shouldBe Vector("Application:Independent", "Application:One", "Application:Two")
      .map(element => ModelElementId("Model:能力/A", element))
    definitions.map(_.name) shouldBe Vector.fill(3)("Same presentation")
    definitions.map(_.contractVersion) shouldBe Vector.fill(3)("app:v1")
    definitions.map(_.source) shouldBe Vector.fill(3)(Source("Model:能力/A", "model/capability-model.cml", "definition-r1", Some(12)))
    definitions.map(_.summary) shouldBe Vector.fill(3)(Present("Summary = literal"))
    definitions.map(_.description) shouldBe Vector.fill(3)(Absent(AbsenceReason.NotRepresented, "No detailed prose."))
    val independent = definitions.head
    independent.demandReferences shouldBe Present(Vector.empty)
    independent.discoveryReferences shouldBe Absent(AbsenceReason.Unsupported, "Discovery not represented.")
    independent.componentMappings shouldBe Present(Vector.empty)
    independent.dependencies shouldBe Absent(AbsenceReason.NotDeclared, "No dependency declared.")
    independent.grounding shouldBe Absent(AbsenceReason.NotApplicable, "Grounding does not apply.")
    independent.traceability shouldBe Present(Vector.empty)
    val foreign = Vector(Source("Other:能力/B", "model/capability-model-secondary.cml", "foreign-edge-r9", Some(8)),
      Source("Model:能力/A", "model/capability-model.cml", "evidence-r2", Some(91)))
    definitions.tail.foreach { definition =>
      val demand = _present(definition.demandReferences)
      val discovery = _present(definition.discoveryReferences)
      val mappings = _present(definition.componentMappings)
      val dependencies = _present(definition.dependencies)
      val grounding = _present(definition.grounding)
      val traceability = _present(definition.traceability)
      Vector(demand.size, discovery.size, mappings.size, dependencies.size, grounding.size, traceability.size) shouldBe Vector(2, 2, 2, 1, 2, 2)
      demand.map(_.role) shouldBe Vector(ModelRole.UseCase, ModelRole.Goal)
      demand.map(_.target) shouldBe Vector(ModelReference(ModelElementId("Model:能力/A", "usecase:one"), ReferenceBoundary.Local),
        ModelReference(ModelElementId("Model:能力/A", "goal:two"), ReferenceBoundary.Local))
      demand.map(_.source.sourceRevision) shouldBe Vector("demand-r1", "demand-r2")
      demand.map(_.origin) shouldBe Vector(Declared, Derived("rule:discovery|α", foreign))
      discovery.map(_.role) shouldBe Vector(ModelRole.Scenario, ModelRole.Component)
      discovery.map(_.target.identity) shouldBe Vector(ModelElementId("Model:能力/A", "scenario:one"), ModelElementId("Model:能力/A", "component:model"))
      discovery.map(_.target.boundary) shouldBe Vector.fill(2)(ReferenceBoundary.External)
      discovery.map(_.source.sourceRevision) shouldBe Vector("discover-r7", "discover-r8")
      discovery.map(_.origin) shouldBe Vector(Derived("rule:discovery|α", foreign), Declared)
      mappings.map(_.target) shouldBe Vector(CapabilityReference(ModelElementId("Model:能力/A", "Component:Store|α"), "contract:二"),
        CapabilityReference(ModelElementId("Other:能力/B", "Component:Remote"), "contract:二"))
      mappings.map(_.boundary) shouldBe Vector(ReferenceBoundary.Local, ReferenceBoundary.External)
      mappings.map(_.source.sourceRevision) shouldBe Vector("mapping-r10", "mapping-r11")
      mappings.map(_.origin) shouldBe Vector(Declared, Derived("rule:discovery|α", foreign))
      dependencies.head.target shouldBe ApplicationCapabilityReference(ModelElementId("Other:能力/B", "Application:Remote"), "app:v1")
      dependencies.head.boundary shouldBe ReferenceBoundary.External
      dependencies.head.source.sourceRevision shouldBe "dependency-r12"
      dependencies.head.origin shouldBe Declared
      grounding.map(_.target) shouldBe Vector(ModelTarget(ModelElementId("Model:能力/A", "concept:Order")), TermTarget(TermId("BoK:語彙", "term:注文|α")))
      grounding.map(_.boundary) shouldBe Vector(ReferenceBoundary.Local, ReferenceBoundary.External)
      grounding.head.relation shouldBe Present(RelationId("vocab:关系", "rel:uses|α"))
      grounding.head.profile shouldBe Present(ProfileId("profile:scope", "profile:one"))
      grounding.head.context shouldBe Present("Domain context")
      grounding.head.preferredLabel shouldBe Present(LocalizedLabel("注文", None))
      grounding.head.source shouldBe Source("Model:能力/A", "model/capability-model.cml", "ground-model-r5", Some(37))
      grounding.head.origin shouldBe Declared
      grounding(1).relation shouldBe Absent(AbsenceReason.NotDeclared, "No declared relation.")
      grounding(1).profile shouldBe Absent(AbsenceReason.Unsupported, "No profile representation.")
      grounding(1).context shouldBe Absent(AbsenceReason.NotApplicable, "No context applies.")
      grounding(1).preferredLabel shouldBe Present(LocalizedLabel("Order", Some("en-X-literal")))
      grounding(1).source.sourceRevision shouldBe "ground-term-r6"
      grounding(1).origin shouldBe Derived("rule:discovery|α", foreign)
      traceability.map(_.role) shouldBe Vector(TraceabilityRole.Specification, TraceabilityRole.Evidence)
      traceability.map(_.target.identity) shouldBe Vector(ModelElementId("Model:能力/A", "spec:1"), ModelElementId("Model:能力/A", "evidence:1"))
      traceability.map(_.target.boundary) shouldBe Vector.fill(2)(ReferenceBoundary.External)
      traceability.map(_.source.sourceRevision) shouldBe Vector("spec-r3", "evidence-r4")
      traceability.map(_.origin) shouldBe Vector(Declared, Derived("rule:discovery|α", foreign))
      val edgesources = demand.map(_.source) ++ discovery.map(_.source) ++ mappings.map(_.source) ++
        dependencies.map(_.source) ++ grounding.map(_.source) ++ traceability.map(_.source)
      edgesources.map(_.modelAuthority).distinct shouldBe Vector("Model:能力/A")
      edgesources.map(_.path).distinct shouldBe Vector("model/capability-model.cml")
      (demand ++ discovery).map(_.source.line) shouldBe Vector.fill(4)(None)
      (mappings.map(_.source) ++ dependencies.map(_.source) ++ Vector(grounding(1).source) ++ traceability.map(_.source))
        .map(_.line) shouldBe Vector.fill(6)(None)
    }
  }
  private def _assert_secondary(projection: ApplicationProjection, source: Source): Unit = {
    projection.documents.map(_.source) shouldBe Vector(source)
    projection.documents.head.applicationCapabilities.size shouldBe 1
    val definition = projection.documents.head.applicationCapabilities.head
    definition.identity shouldBe ModelElementId("Other:能力/B", "Application:Remote")
    definition.contractVersion shouldBe "app:v1"
    definition.name shouldBe "Remote"
    definition.summary shouldBe Absent(AbsenceReason.NotDeclared, "No summary declared.")
    definition.description shouldBe Present("Remote description")
    definition.source shouldBe Source("Other:能力/B", "model/capability-model-secondary.cml", "secondary-definition-r20", None)
    definition.demandReferences shouldBe Present(Vector.empty)
    definition.discoveryReferences shouldBe Present(Vector.empty)
    definition.componentMappings shouldBe Present(Vector.empty)
    definition.dependencies shouldBe Present(Vector.empty)
    definition.grounding shouldBe Present(Vector.empty)
    definition.traceability shouldBe Present(Vector.empty)
  }
}
