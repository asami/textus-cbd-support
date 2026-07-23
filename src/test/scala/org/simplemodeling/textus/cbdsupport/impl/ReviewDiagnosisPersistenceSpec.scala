package org.simplemodeling.textus.cbdsupport.impl

import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import java.util.UUID

import cats.~>
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.context.{DataStoreContext, EntityStoreContext, ExecutionContext, RuntimeContext, ScopeContext, ScopeKind}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.{EntityStore, EntityStoreSpace}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.Request
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class ReviewDiagnosisPersistenceSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review diagnosis persistence" should {
    "claim one stable Entity diagnosis and join a duplicate reusable execution" in {
      Given("two executions with the same server-derived reuse identity")
      val factory = new ComponentFactory()
      val context = _context()
      val firstplan = _plan("review-owner")
      val duplicateplan = _plan("review-joiner")

      When("both executions are admitted through separate UnitOfWork boundaries")
      val first = _run(factory, context, firstplan)
      val duplicate = _run(factory, context, duplicateplan)
      val owner = _owner(first)

      Then("the first execution owns the Aggregate and the duplicate joins it")
      first shouldBe a[CarReviewDiagnosisAdmission.Owner]
      owner.reviewId shouldBe ReviewId("review-owner")
      duplicate shouldBe CarReviewDiagnosisAdmission.Joined(first.diagnosisId, ReviewId("review-owner"))
    }

    "retain immutable completion snapshots and reuse the completed canonical Report" in {
      Given("an owned diagnosis and its canonical completed response")
      val factory = new ComponentFactory()
      val context = _context()
      val report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(error => fail(error.message), identity)
      val attestation = CarReviewAttestationCodec.fromReport(report).fold(error => fail(error.message), identity)
      val ownerplan = _plan_for(report, report.reviewId.value)
      val repeatedplan = _plan_for(report, "review-reused-request")

      When("completion and a later same-key request cross fresh UnitOfWork reads")
      val owner = _owner(_run(factory, context, ownerplan))
      _value(org.simplemodeling.model.datatype.EntityId.parse(owner.diagnosisId)) shouldBe _diagnosis_id(ownerplan)
      val completion = _run_completion(factory, context, owner, ownerplan, CarReviewCanonicalResponse(report, report.gate, attestation))
      val repeated = _run(factory, context, repeatedplan)

      Then("both responses reuse the persisted canonical Report identity")
      completion match {
        case CarReviewDiagnosisAdmission.Reused(diagnosisid, reviewid, reportid, digest) =>
          diagnosisid shouldBe owner.diagnosisId
          reviewid shouldBe report.reviewId
          reportid shouldBe report.reportId
          digest shouldBe report.reportDigest
        case other => fail(s"Expected completed reuse, got $other")
      }
      repeated match {
        case CarReviewDiagnosisAdmission.Reused(diagnosisid, reviewid, reportid, digest) =>
          diagnosisid shouldBe owner.diagnosisId
          reviewid shouldBe report.reviewId
          reportid shouldBe report.reportId
          digest shouldBe report.reportDigest
        case other => fail(s"Expected persisted reuse, got $other")
      }
    }

    "retain every non-success terminal Run without presenting it as a reusable Report" in {
      Given("independent owned diagnoses for every non-success terminal state")
      val factory = new ComponentFactory()
      val context = _context()
      val states = Vector(
        CarReviewDiagnosisTerminalState.Failed,
        CarReviewDiagnosisTerminalState.Cancelled,
        CarReviewDiagnosisTerminalState.Expired,
        CarReviewDiagnosisTerminalState.Incompatible
      ).zip(Vector('e', 'f', 'b', 'c'))

      When("each diagnosis records its terminal Run through the Aggregate boundary")
      val results = states.map { case (state, targetdigest) =>
        val plan = _plan(s"review-${state.value}", targetdigest)
        val owner = _owner(_run(factory, context, plan))
        _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._record_terminal_review_execution(_core(context), owner, plan, state, s"{\"state\":\"${state.value}\"}", ReviewInstant("2026-07-23T01:00:00Z"))
        )) shouldBe ()
        state -> new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
          factory._admit_review_execution(_core(context), _plan(s"review-after-${state.value}", targetdigest))
        )
      }

      Then("no terminal diagnosis is exposed as a reusable successful Report")
      results.foreach { case (state, result) =>
        withClue(s"${state.value} must never be reused as a successful Report") {
          result shouldBe a[Consequence.Failure[_]]
        }
      }
    }

    "require the issued Owner lease before a claimed Aggregate can transition" in {
      Given("an Aggregate claimed by one issued Owner lease")
      val factory = new ComponentFactory()
      val context = _context()
      val ownerplan = _plan("review-lease-owner", 'd')
      val otherplan = _plan("review-lease-other", 'd')
      val owner = _owner(_run(factory, context, ownerplan))

      When("another execution plan attempts the terminal transition")
      val result = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          otherplan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:00Z")
        )
      )

      Then("the Aggregate boundary rejects the non-owner transition")
      result shouldBe a[Consequence.Failure[_]]
    }

    "read the persisted terminal Aggregate state before a repeat transition" in {
      Given("an owned diagnosis that will be retained as cancelled")
      val factory = new ComponentFactory()
      val context = _context()
      val plan = _plan("review-terminal-replay", '0')
      val owner = _owner(_run(factory, context, plan))

      When("the same terminal transition is attempted through two UnitOfWork boundaries")
      val first = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          plan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:00Z")
        )
      )
      val repeated = new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
        factory._record_terminal_review_execution(
          _core(context),
          owner,
          plan,
          CarReviewDiagnosisTerminalState.Cancelled,
          "{\"state\":\"cancelled\"}",
          ReviewInstant("2026-07-23T01:00:01Z")
        )
      )

      Then("the persisted first transition succeeds and the replay is rejected")
      _value(first) shouldBe ()
      repeated shouldBe a[Consequence.Failure[_]]
    }
  }

  private def _run(
    factory: ComponentFactory,
    context: ExecutionContext,
    plan: CarReviewExecutionPlan
  ): CarReviewDiagnosisAdmission =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      factory._admit_review_execution(_core(context), plan)
    ))

  private def _run_completion(
    factory: ComponentFactory,
    context: ExecutionContext,
    owner: CarReviewDiagnosisAdmission.Owner,
    plan: CarReviewExecutionPlan,
    response: CarReviewCanonicalResponse
  ): CarReviewDiagnosisAdmission =
    _value(new UnitOfWorkInterpreter(new UnitOfWork(context)).run(
      factory._complete_review_execution(_core(context), owner, plan, response)
    ))

  private def _owner(admission: CarReviewDiagnosisAdmission): CarReviewDiagnosisAdmission.Owner =
    admission match {
      case owner: CarReviewDiagnosisAdmission.Owner => owner
      case other => fail(s"Expected owner admission, got $other")
    }

  private def _plan_for(report: CarReviewReport, reviewid: String): CarReviewExecutionPlan = {
    val provider = CarReviewReuseProviderSelection(
      ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
      _digest('b')
    )
    val input = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      report.schemaVersion,
      report.target,
      report.profile,
      None,
      Vector(provider.ruleSet),
      Vector(provider),
      Vector(CarReviewReuseEvidenceSnapshot("runtime", "runtime-snapshot-main", ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")), _digest('c'))),
      Vector(
        CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
        CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
        CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
        CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
      )
    )
    CarReviewExecutionPlan.create(ReviewStartRequest(ReviewId(reviewid), report.target, report.profile, report.execution.startedAt), input).fold(error => fail(error.message), identity)
  }

  private def _context(): ExecutionContext = {
    val base = ExecutionContext.create()
    val datastorespace = new DataStoreSpace().useDataStore(DataStore.inMemorySearchable())
    val entitystorespace = new EntityStoreSpace().addEntityStore(EntityStore.standard())
    lazy val context: ExecutionContext = ExecutionContext.create(runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = ScopeContext.Core(
        kind = ScopeKind.Runtime,
        name = "cbd-review-diagnosis-persistence-spec",
        parent = None,
        observabilityContext = base.cncfCore.observability,
        httpDriverOption = None,
        datastore = Some(DataStoreContext(datastorespace)),
        entitystore = Some(EntityStoreContext(entitystorespace)),
        entityspace = None,
        aggregateInternalRead = false,
        processExecutionDriverOption = None,
        processExecutionAdmissionOption = None,
        scopedConcurrencyAdmissionOption = None
      ),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "cbd-review-diagnosis-persistence-spec"
    )
    runtime.setResolvedParameters(ResolvedParameters.empty())
    require(context.dataStoreSpace eq datastorespace, "review persistence test must use its configured DataStoreSpace")
    context
  }

  private def _core(context: ExecutionContext): ActionCall.Core =
    ActionCall.Core(TestAction(Request.ofOperation("cbd-review-diagnosis-persistence")), context, None, None)

  private def _plan(reviewid: String, targetdigest: Char = 'a'): CarReviewExecutionPlan = {
    val target = ReviewTarget(
      ReviewTargetKind("car"),
      Some("org.textus"),
      "textus-cbd-support",
      Some(ReviewVersion("0.1.0-SNAPSHOT")),
      _digest(targetdigest)
    )
    val provider = CarReviewReuseProviderSelection(
      ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("1.0")),
      ReviewRuleIdentity(ReviewRuleId("rule-catalog"), ReviewVersion("1.0")),
      _digest('b')
    )
    val input = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      target,
      ReviewProfile("development"),
      None,
      Vector(provider.ruleSet),
      Vector(provider),
      Vector(CarReviewReuseEvidenceSnapshot(
        "runtime",
        "runtime-snapshot-main",
        ReviewProviderIdentity(ReviewProviderId("runtime"), ReviewVersion("1.0")),
        _digest('c')
      )),
      Vector(
        CarReviewReusePolicyBinding("profile", "profile-development", ReviewVersion("1.0"), _digest('1')),
        CarReviewReusePolicyBinding("gate", "gate-default", ReviewVersion("1.0"), _digest('2')),
        CarReviewReusePolicyBinding("reconciliation", "reconciliation-default", ReviewVersion("1.0"), _digest('3')),
        CarReviewReusePolicyBinding("suppression", "suppression-default", ReviewVersion("1.0"), _digest('4'))
      )
    )
    CarReviewExecutionPlan.create(
      ReviewStartRequest(ReviewId(reviewid), target, ReviewProfile("development"), ReviewInstant("2026-07-23T00:00:00Z")),
      input
    ).fold(error => fail(s"${error.code}: ${error.message}"), identity)
  }

  private def _digest(character: Char): ReviewDigest =
    ReviewDigest("sha256:" + character.toString * 64)

  private def _diagnosis_id(plan: CarReviewExecutionPlan): org.simplemodeling.model.datatype.EntityId = {
    val seed = s"${plan.reuseKey.definitionId}:${plan.reuseKey.digest.value}"
    val key = "d" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString.replace("-", "")
    org.simplemodeling.model.datatype.EntityId(
      "CbdReviewDiagnosis",
      key,
      org.simplemodeling.textus.cbdsupport.entity.ReviewDiagnosis.collectionId,
      timestamp = Some(org.goldenport.id.UniversalId.StableTimestamp),
      entropy = Some(org.goldenport.id.UniversalId.StableEntropy)
    )
  }

  private def _value[A](consequence: Consequence[A]): A = consequence match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(s"${conclusion.display} / ${conclusion.show} / $conclusion")
  }

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("Test fixture exposes ActionCall.Core only.")
    }
  }
}
