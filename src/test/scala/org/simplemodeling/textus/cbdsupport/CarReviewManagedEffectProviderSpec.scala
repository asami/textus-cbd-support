package org.simplemodeling.textus.cbdsupport

import cats.~>
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.context.{ExecutionContext, RuntimeContext}
import org.goldenport.cncf.effect.ExternalEffectUsage
import org.goldenport.cncf.processexecution.{GitOperationKind, ProcessExternalEffectPolicy, SbtOperation}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.observation.calltree.CallTreeNode
import org.goldenport.protocol.Request
import org.goldenport.tree.{TreeDir, TreeLeaf, TreeNode}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.{value => cbdvalue}
import org.simplemodeling.textus.cbdsupport.runtime.CarReviewManagedEffectProvider

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
final class CarReviewManagedEffectProviderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review managed effect provider" should {
    "E1 attribute recognized process laundering" should
      afterWord("in spec:car-review-managed-effect-contract, example:E1, rules:R2,R3, phase:16") {
      "report the original file misuse and HTTP and persistence escapes" in {
        Given("docs/spec/car-review-managed-effect-contract.md R2,R3 E1 and supplied file, HTTP and persistence process observations")
        val subjects = Vector(
          _subject("observe-selected", "process", command = Some("cat")),
          _subject("apply-selected", "process", command = Some("cp")),
          _subject("fetch-http", "process", command = Some("curl")),
          _subject("database", "process", command = Some("psql")))
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        When("the actual fixed provider reviews the supplied misuse through CNCF")
        val result = _value(CarReviewManagedEffectProvider.reviewC(fixture.core, _request(subjects)))
        Then("each finding preserves its exact source identity and CNCF effect kinds and remediation")
        result.reviewId shouldBe "review-001"
        result.revision shouldBe 3L
        result.observations.map(_.identity) shouldBe subjects.map(_.identity)
        result.observations.map(_.source) shouldBe subjects.map(_.source)
        result.observations.map(_.effectKinds) shouldBe Vector(Vector("FileRead"), Vector("FileRead", "FileWrite"), Vector("Http"), Vector("Persistence"))
        result.observations.foreach { observation =>
          observation.outcome shouldBe "finding"
          observation.ruleId shouldBe "cbd.car-review.managed-effects.capability-laundering"
          observation.route shouldBe "external.effect.capability-laundering"
          observation.remediationCategory shouldBe "use-managed-effect"
        }
      }
    }

    "E2 distinguish managed typed use" should
      afterWord("in spec:car-review-managed-effect-contract, example:E2, rules:R3,R4, phase:16") {
      "consume the unchanged CNCF classification for typed Git, SBT and resources" in {
        Given("docs/spec/car-review-managed-effect-contract.md R3,R4 E2 with read/write Git, all SBT operations and a resource observation")
        val cases = Vector(
          _subject("git-read", "git", Some("head")) -> ExternalEffectUsage.TypedGit(GitOperationKind.Head),
          _subject("git-write", "git", Some("fetch")) -> ExternalEffectUsage.TypedGit(GitOperationKind.Fetch),
          _subject("compile", "sbt", Some("compile")) -> ExternalEffectUsage.TypedSbt(SbtOperation.Compile),
          _subject("test", "sbt", Some("test")) -> ExternalEffectUsage.TypedSbt(SbtOperation.Test),
          _subject("test-only", "sbt", Some("test-only")) -> ExternalEffectUsage.TypedSbt(SbtOperation.TestOnly),
          _subject("resource", "resource-read") -> ExternalEffectUsage.ResourceRead)
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val expected = cases.map { case (_, usage) => _value(ProcessExternalEffectPolicy.classifyUsageC(usage)) }
        When("the provider reviews the supplied managed-route observations")
        val result = _value(CarReviewManagedEffectProvider.reviewC(fixture.core, _request(cases.map(_._1))))
        Then("each managed-use result carries exactly CNCF's route and sorted kinds without a security PASS")
        result.observations.zip(expected).foreach { case (observation, classification) =>
          observation.effectKinds shouldBe classification.effectKinds.toVector.map(_.toString).sorted
          observation.route shouldBe classification.route.diagnosticCode
          observation.outcome shouldBe "managed-use"
          observation.ruleId shouldBe "cbd.car-review.managed-effects.managed-route"
          observation.remediationCategory shouldBe "retain-managed-route"
        }
      }
    }

    "E3 preserve inference limits and direct-process findings" should
      afterWord("in spec:car-review-managed-effect-contract, example:E3, rules:R3,R4, phase:16") {
      "keep unknown use unknown and attribute interpreter use" in {
        Given("docs/spec/car-review-managed-effect-contract.md R3,R4 E3 with an unknown binary and a Python interpreter observation")
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val subjects = Vector(_subject("unknown", "process", command = Some("selected-tool")),
          _subject("interpreter", "process", command = Some("python3.12")))
        When("the provider classifies those explicit process observations")
        val observations = _value(CarReviewManagedEffectProvider.reviewC(fixture.core, _request(subjects))).observations
        Then("unknown remains attributable unknown and direct use is a finding with managed-route remediation")
        observations.map(_.outcome) shouldBe Vector("unknown", "finding")
        observations.map(_.ruleId) shouldBe Vector("cbd.car-review.managed-effects.unclassified", "cbd.car-review.managed-effects.direct-process")
        observations.map(_.route) shouldBe Vector("external.effect.unclassified", "external.effect.direct")
        observations.map(_.remediationCategory) shouldBe Vector("supply-effect-evidence", "use-managed-effect")
        observations.map(_.effectKinds) shouldBe Vector(Vector("Process"), Vector("Process"))
      }
    }

    "E4 refuse invalid or contradictory evidence" should
      afterWord("in spec:car-review-managed-effect-contract, example:E4, rules:R2, phase:16") {
      "fail missing, unknown, contradictory and out-of-bound facts before observations" in {
        Given("docs/spec/car-review-managed-effect-contract.md R2 E4 with invalid identities, revisions, subject sets and usage facts")
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val subject = _subject("subject", "process", command = Some("cat"))
        val base = _request(Vector(subject))
        val badsubjects = Vector(
          null, subject.copy(identity = "invalid id"), subject.copy(identity = null),
          subject.copy(source = ""), subject.copy(source = null), subject.copy(source = "x\u0000"),
          subject.copy(source = "あ" * 683), subject.copy(usage = null), subject.copy(usage = "Process"),
          subject.copy(commandName = None), subject.copy(commandName = Some(null)),
          subject.copy(commandName = Some(" ")), subject.copy(commandName = Some("private-command\u0000")),
          subject.copy(commandName = Some("あ" * 342)), subject.copy(operation = Some("head")),
          _subject("subject", "git"), _subject("subject", "git", Some("HEAD")),
          _subject("subject", "git", Some("head"), Some("git")),
          _subject("subject", "sbt"), _subject("subject", "sbt", Some("publish")),
          _subject("subject", "sbt", Some("test"), Some("sbt")),
          _subject("subject", "resource-read", Some("read")),
          _subject("subject", "resource-read", command = Some("cat")))
        val requests = Vector(null, base.copy(reviewId = "bad id"), base.copy(reviewId = null),
          base.copy(revision = 0L), base.copy(subjects = Vector.empty), base.copy(subjects = null),
          base.copy(subjects = Vector(subject, subject)), base.copy(subjects = Vector.tabulate(65)(i => subject.copy(identity = s"s$i")))) ++
          badsubjects.map(subject => base.copy(subjects = Vector(subject)))
        When("the fixed provider receives each invalid request")
        val results = requests.map(request => CarReviewManagedEffectProvider.reviewC(fixture.core, request))
        Then("all requests fail without observations, provider evaluation or command disclosure")
        results.foreach { result =>
          result.isFaillure shouldBe true
          result.toOption shouldBe None
          result.toString should not include "private-command"
        }
        fixture.interpretedOperationCount shouldBe 0
        fixture.providerStepCount shouldBe 0
      }
    }

    "E5 evaluate the ordinary ProviderCall program" should
      afterWord("in spec:car-review-managed-effect-contract, example:E5, rules:R1, phase:16") {
      "evaluate the pure provider program without external effects and expose only safe review metadata" in {
        Given("docs/spec/car-review-managed-effect-contract.md R1 E5 with a real UnitOfWork and private source and command sentinels")
        val fixture = ManagedEffectReviewSpecSupport.contextFixture()
        val input = _request(Vector(_subject("subject", "process", command = Some("/private-command/cat"))
          .copy(source = "private-source-sentinel")))
        When("the provider evaluates its pure fixed diagnostic program through the bound UnitOfWork")
        val result = CarReviewManagedEffectProvider.reviewC(fixture.core, input)
        Then("one provider step is evaluated without external effects and its CallTree retains only safe input metadata")
        result.isSuccess shouldBe true
        _value(result).observations.head.detail should not include "private-command"
        fixture.interpretedOperationCount shouldBe 0
        fixture.providerStepCount shouldBe 1
        fixture.context.unitOfWork.executionContext should be theSameInstanceAs fixture.context
        val tree = fixture.context.observability.callTreeContext.build().getOrElse(fail("CallTree missing"))
        val rendered = tree.toRecord.print
        rendered should include("provider:cbd-car-review-managed-effects.review")
        rendered should include("review-managed-effects")
        rendered should include("review_id=review-001")
        rendered should include("revision=3")
        rendered should include("subject_count=1")
        rendered should not include "private-source-sentinel"
        rendered should not include "private-command"
      }
    }
  }

  private def _subject(identity: String, usage: String, operation: Option[String] = None,
    command: Option[String] = None): cbdvalue.ManagedEffectReviewSubject =
    cbdvalue.ManagedEffectReviewSubject(identity, s"src/main/scala/$identity.scala", usage, operation, command)

  private def _request(subjects: Vector[cbdvalue.ManagedEffectReviewSubject]): cbdvalue.ManagedEffectReviewRequest =
    cbdvalue.ManagedEffectReviewRequest("review-001", 3L, subjects)

  private def _value[A](result: Consequence[A]): A = result match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }
}

private[cbdsupport] object ManagedEffectReviewSpecSupport {
  final case class RuntimeFixture(context: ExecutionContext, interpreted: () => Int) {
    def interpretedOperationCount: Int = interpreted()
    def providerStepCount: Int = {
      def _count_provider_steps_(node: TreeNode[CallTreeNode]): Int = node match {
        case TreeLeaf(CallTreeNode.Enter(label, attributes), _)
            if label == "review-managed-effects" && attributes.get("calltree_kind").contains("provider-step") => 1
        case TreeLeaf(_, _) => 0
        case TreeDir(children) => children.map(entry => _count_provider_steps_(entry.node)).sum
      }
      context.observability.callTreeContext.build().map(tree => _count_provider_steps_(tree.tree.root)).getOrElse(0)
    }
    def core: ActionCall.Core = ActionCall.Core(TestAction(Request.ofOperation("managed-effect-review-spec")), context, None, None)
  }

  def contextFixture(): RuntimeFixture = {
    val base = ExecutionContext.withFrameworkCallTreeEnabled(ExecutionContext.create(), enabled = true)
    var count = 0
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = RuntimeContext.core("managed-effect-review-spec", None, base.observability),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = {
          count += 1
          interpreter.interpret(operation)
        }
      },
      commitAction = _ => (), abortAction = _ => (), disposeAction = _ => (),
      token = "managed-effect-review-spec")
    RuntimeFixture(context, () => count)
  }

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall =
      throw new UnsupportedOperationException("Fixture supplies only ActionCall.Core")
  }
}
