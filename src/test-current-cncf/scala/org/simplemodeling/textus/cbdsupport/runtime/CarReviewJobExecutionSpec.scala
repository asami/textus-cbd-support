package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.job.JobStatus
import org.goldenport.cncf.unitofwork.UnitOfWorkTermination
import org.goldenport.protocol.operation.OperationResponse
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct.  6, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewJobExecutionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import CarReviewJobExecutionFixtures.*

  "Native CAR Review Job execution" should {
    "stage and complete one actual canonical Entity report" which {
      "keeps the exact Job unpublished until outer commit and reads its Report through a fresh context (CJE1)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a ready configured native owner and an actual Entity-issued Review owner")
          val plan = fixture.plan()
          val work = fixture.work(plan, fixture.claim(plan))

          When("the gateway stages the selected provider under the original outer UOW")
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(work))

          Then("the exact native Job ID is reserved without a visible Job or provider invocation")
          fixture.query(id) shouldBe None
          fixture.runner.executions shouldBe 0
          fixture.outer.unitOfWork.lastCommitTermination shouldBe None

          When("the same outer UOW commits and the native scheduler drains")
          CarReviewJobExecutionFixtures.value(fixture.commit())
          val submitted = fixture.query(id).get
          fixture.dispose()
          fixture.drain()
          val record = withClue(s"Native Job after drain: ${fixture.query(id)}; ") {
            _result_record(fixture.signedResult(id))
          }
          val report = CarReviewJobExecutionFixtures.value(fixture.readReport(ReviewReportId(record.getString("reportId").get)))

          Then("one real ProviderCall completes the exact canonical Report and attestation before native success")
          submitted.jobId shouldBe fixture.jobId(id)
          submitted.status shouldBe JobStatus.Submitted
          fixture.query(id).get.status shouldBe JobStatus.Succeeded
          fixture.native.signedStatus(fixture.jobId(id)) shouldBe "succeeded"
          fixture.runner.executions shouldBe 1
          record.getString("reviewId") shouldBe Some(work.owner.reviewId.value)
          record.getString("reportId") shouldBe Some(report.reportId.value)
          record.getString("reportDigest") shouldBe Some(report.reportDigest.value)
          report.reviewId shouldBe work.owner.reviewId
          report.target shouldBe work.plan.request.target
          report.execution.startedAt shouldBe work.plan.request.startedAt
          report.execution.completedAt shouldBe ReviewInstant(fixture.outer.clock.instant().toString)
          report.createdAt shouldBe report.execution.completedAt
          fixture.reportCount shouldBe 1
          fixture.attestationCount shouldBe 1
          fixture.trace() should include("execute-car-review-provider")
        }
      }

      "retains native terminal results without another provider or snapshot (CJE9)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("one real committed and successfully drained Review Job")
          val plan = fixture.plan()
          val work = fixture.work(plan, fixture.claim(plan))
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(work))
          CarReviewJobExecutionFixtures.value(fixture.commit())
          fixture.drain()
          val result = fixture.signedResult(id)
          val paid = CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage)

          When("the native scheduler and exact signed result reader are used repeatedly")
          val drains = Vector(fixture.drain(), fixture.drain())
          val reads = Vector(fixture.signedResult(id), fixture.signedResult(id))

          Then("the exact Report identity and paid terminal evidence remain with one provider and one composition")
          drains shouldBe Vector(0, 0)
          reads shouldBe Vector(result, result)
          _result_record(result).getString("reviewId") shouldBe Some(plan.request.reviewId.value)
          fixture.native.signedStatus(fixture.jobId(id)) shouldBe "succeeded"
          paid.get(2) should be > 0L
          CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage) shouldBe paid
          fixture.runner.executions shouldBe 1
          fixture.reportCount shouldBe 1
          fixture.attestationCount shouldBe 1
        }
      }
    }

    "preserve outer admission and explicit work preconditions" which {
      Vector("abort", "dispose").foreach { ending =>
        s"leaves $ending reservations unpublished and cannot resurrect them (CJE2)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"a ready native owner with one staged Review reservation to $ending")
            val plan = fixture.plan()
            val id = CarReviewJobExecutionFixtures.value(fixture.stage(fixture.work(plan, fixture.claim(plan))))

            When(s"the submitting UOW is ended by $ending and later asked to commit again")
            if ending == "abort" then CarReviewJobExecutionFixtures.value(fixture.abort()) else fixture.dispose()
            val _ = fixture.commit()
            fixture.drain()

            Then("the reserved ID remains unpublished and there is no provider or successful native result")
            fixture.query(id) shouldBe None
            fixture.runner.executions shouldBe 0
            fixture.reportCount shouldBe 0
          }
        }
      }

      "requires an execution plan on the compatible legacy submit API (CJE2)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a native gateway and an old transport-only Review start request")
          val request = fixture.plan().request
          val issued = fixture.issuedIds

          When("the legacy caller submits without server work")
          val result = fixture.gateway.submit(request)(using fixture.outer)

          Then("the structured missing-plan refusal issues no ID and runs no provider")
          failure(result) should include("review-execution-plan-required")
          fixture.issuedIds shouldBe issued
          fixture.runner.executions shouldBe 0
        }
      }

      val invalidcases: Vector[(String, CarReviewJobExecutionWork => CarReviewJobExecutionWork)] = Vector(
        "null work" -> (_ => null),
        "null plan" -> (_.copy(plan = null)),
        "null owner" -> (_.copy(owner = null)),
        "null template" -> (_.copy(template = null)),
        "null providers" -> (_.copy(providers = null)),
        "missing providers" -> (_.copy(providers = Vector.empty)),
        "too many providers" -> (work => work.copy(providers = Vector.fill(9)(work.providers.head))),
        "duplicate provider" -> (work => work.copy(providers = work.providers ++ work.providers)),
        "plan revision" -> (work => work.copy(plan = work.plan.copy(reference = work.plan.reference.copy(revision = 2L)))),
        "plan reference" -> (work => work.copy(plan = work.plan.copy(reference = null))),
        "declared native reference" -> (work => work.copy(plan = work.plan.copy(reference = work.plan.reference.copy(diagnosisId = CarReviewJobExecutionFixtures.value(
          org.simplemodeling.model.datatype.EntityId.parse("major-minor-entity-ec1_5_major_5_minor_16_review_diagnosis-0-otherPlan")))))),
        "Review" -> (work => work.copy(template = work.template.copy(reviewId = ReviewId("foreign-review")))),
        "profile" -> (work => work.copy(template = work.template.copy(profile = ReviewProfile("foreign-profile")))),
        "semantic target" -> (work => work.copy(template = work.template.copy(target = work.template.target.copy(name = "foreign-target")))),
        "declared provider selection" -> (work => work.copy(plan = work.plan.copy(reuseInput = work.plan.reuseInput.copy(providerSelections = Vector.empty)))),
        "unbound provider rule" -> (work => work.copy(plan = work.plan.copy(reuseInput = work.plan.reuseInput.copy(ruleSets = Vector.empty)))),
        "provider binding" -> (work => work.copy(providers = Vector(work.providers.head.copy(request = work.providers.head.request.copy(provider = ReviewProviderIdentity(ReviewProviderId("foreign"), ReviewVersion("1.0"))))))),
        "descriptor" -> (work => work.copy(providers = Vector(work.providers.head.copy(request = work.providers.head.request.copy(descriptor = "{}"))))),
        "typed descriptor rule" -> (work => {
          val document = io.circe.parser.parse(work.providers.head.request.descriptor).toOption.get
          val changed = document.hcursor.downField("ruleSet").withFocus(_.mapObject(_.add("version", io.circe.Json.fromString("different")))).top.get.noSpaces
          work.copy(providers = Vector(work.providers.head.copy(request = work.providers.head.request.copy(descriptor = changed))))
        }),
        "baseline" -> (work => work.copy(plan = work.plan.copy(reuseInput = work.plan.reuseInput.copy(baselineReference = Some(CarReviewDeclaredReference("baseline:declared", 1L)))))),
        "assessment" -> (work => work.copy(template = work.template.copy(assessments = Vector.empty))),
        "canonical schema" -> (work => work.copy(template = work.template.copy(schemaVersion = ReviewSchemaVersion("unsupported")))),
        "canonical document" -> (work => work.copy(template = work.template.copy(documentType = ReviewDocumentType("unsupported")))),
        "gate policy" -> (work => work.copy(template = work.template.copy(gate = work.template.gate.copy(policyId = "foreign-gate"))))
      )
      invalidcases.foreach { case (name, change) =>
        s"refuses $name before identity issuance or provider work (CJE3)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"an actual issued Entity owner and conflicting $name server work")
            val plan = fixture.plan()
            val work = change(fixture.work(plan, fixture.claim(plan)))
            val issued = fixture.issuedIds
            val budget = CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage)

            When("the actual staged gateway validates the complete work")
            val result = fixture.stage(work)
            fixture.drain()

            Then("a structured refusal leaves native issuance and provider counts untouched")
            result shouldBe a[Consequence.Failure[_]]
            fixture.issuedIds shouldBe issued
            CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage) shouldBe budget
            fixture.runner.executions shouldBe 0
            fixture.reportCount shouldBe 0
          }
        }
      }
    }

    "execute inside the native Job lifecycle" which {
      "uses a distinct rebound live UOW after submitting disposal (CJE4)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a staged Review with an outer callback and real submitting security and datastore scopes")
          val plan = fixture.plan()
          val work = fixture.work(plan, fixture.claim(plan))
          var callbacks = 0
          fixture.outer.unitOfWork.stagePostCommitC { callbacks += 1; Consequence.unit }
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(work))
          CarReviewJobExecutionFixtures.value(fixture.commit())
          fixture.dispose()
          val original = fixture.outer.unitOfWork

          When("native Job execution completes its provider and generated Entity work")
          fixture.drain()
          val observed = fixture.native.observedContexts.filter(_.jobContext.jobId.contains(fixture.jobId(id)))

          Then("the actual Entity interpretation carries the Job and Task through a fresh committed UOW")
          observed should not be empty
          val context = observed.last
          (context.unitOfWork eq original) shouldBe false
          (context.unitOfWork.executionContext eq context) shouldBe true
          context.jobContext.taskId should not be empty
          context.jobContext.cancellationScope should not be empty
          context.security shouldBe fixture.outer.security
          (context.dataStoreSpace eq fixture.outer.dataStoreSpace) shouldBe true
          (context.clock eq fixture.outer.clock) shouldBe true
          context.executionControl shouldBe fixture.outer.executionControl
          context.unitOfWork.lastCommitTermination shouldBe Some(UnitOfWorkTermination.Committed)
          callbacks shouldBe 1
          fixture.runner.executions shouldBe 1
          fixture.query(id).get.status shouldBe JobStatus.Succeeded
        }
      }

      Vector("missing", "descriptor", "failed", "exception").foreach { mode =>
        s"keeps $mode provider execution as native failure without completed Report (CJE5)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"a real Review owner and a selected $mode provider boundary")
            val plan = fixture.plan()
            val work = fixture.work(plan, fixture.claim(plan))
            val application = if mode == "missing" || mode == "descriptor" then {
              val registry = new CarReviewProviderRegistry
              if mode == "descriptor" then {
                val document = io.circe.parser.parse(fixture.descriptor).toOption.get
                val changed = document.hcursor.downField("ruleSet").withFocus(_.mapObject(_.add("version", io.circe.Json.fromString("different")))).top.get.noSpaces
                registry.register(changed, fixture.runner).isRight shouldBe true
              }
              new CarReviewProviderExecutionApplication(registry, new CarReviewProviderExecutionCoordinator)
            } else fixture.application
            if mode == "failed" then fixture.runner.execution = request => ProviderBundleRunnerResult.Failed("controlled-provider-failed", "PRIVATE-RUNNER-DETAIL", request.startedAtMillis)
            if mode == "exception" then fixture.runner.execution = _ => throw new IllegalStateException("PRIVATE-RUNNER-DETAIL")
            val id = CarReviewJobExecutionFixtures.value(fixture.stage(work, application))
            CarReviewJobExecutionFixtures.value(fixture.commit())

            When("the actual native task executes the registry boundary")
            fixture.native.expectTerminalFailure()
            fixture.drain()

            Then("the Job fails with bounded attribution and no canonical completion or fallback")
            fixture.query(id).get.status shouldBe JobStatus.Failed
            fixture.native.signedStatus(fixture.jobId(id)) shouldBe "failed"
            fixture.reportCount shouldBe 0
            fixture.attestationCount shouldBe 0
            fixture.runner.executions shouldBe (if mode == "missing" || mode == "descriptor" then 0 else 1)
            val code = mode match {
              case "missing" => "provider-not-registered"
              case "descriptor" => "provider-registration-mismatch"
              case "failed" => "controlled-provider-failed"
              case _ => "provider-runner-exception"
            }
            fixture.query(id).get.resultSummary.message.get should include(code)
            fixture.query(id).get.toString should not include "PRIVATE-RUNNER-DETAIL"
            fixture.trace() should not include "PRIVATE-RUNNER-DETAIL"
          }
        }
      }

      "refuses a stale issued owner against the current persisted terminal diagnosis (CJE6)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a committed staged Job whose owner is terminated by another actual Entity UOW")
          val plan = fixture.plan()
          val work = fixture.work(plan, fixture.claim(plan))
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(work))
          CarReviewJobExecutionFixtures.value(fixture.commit())
          fixture.terminal(work)

          When("the original native task executes its provider and attempts completion")
          fixture.native.expectTerminalFailure()
          fixture.drain()

          Then("the current-root guard preserves terminal history and refuses canonical success")
          fixture.runner.executions shouldBe 1
          fixture.query(id).get.status shouldBe JobStatus.Failed
          fixture.diagnosis(plan).state.value shouldBe "failed"
          fixture.reportCount shouldBe 0
          fixture.attestationCount shouldBe 0
          fixture.query(id).get.resultSummary.message.get should include("review-diagnosis-completion-not-owner")
        }
      }

      Vector("prepare", "commit").foreach { fault =>
        s"requires successful fresh UOW $fault after provider execution (CJE7)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"a successfully activated Review Job and a controlled task $fault fault")
            val plan = fixture.plan()
            val id = CarReviewJobExecutionFixtures.value(fixture.stage(fixture.work(plan, fixture.claim(plan))))
            CarReviewJobExecutionFixtures.value(fixture.commit())
            fixture.fault = fault

            When("the fresh task UOW interprets provider and Entity work then finalizes")
            fixture.native.expectTerminalFailure()
            fixture.drain()

            Then("native success is impossible and actual termination evidence records the failure")
            fixture.runner.executions shouldBe 1
            fixture.query(id).get.status shouldBe JobStatus.Failed
            fixture.native.signedStatus(fixture.jobId(id)) shouldBe "failed"
            fixture.recorder.entries should contain(if fault == "prepare" then "EventEngine.prepare.rejected" else "EventEngine.commit.failed")
            fixture.native.observedContexts.filter(_.jobContext.jobId.contains(fixture.jobId(id))).last.unitOfWork.lastCommitTermination shouldBe Some(UnitOfWorkTermination.Aborted)
            fixture.signedResult(id) shouldBe None
          }
        }
      }
    }

    "retain original native cancellation ownership" which {
      "runs no provider after cancellation before dispatch (CJE8)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("one committed queued native Review Job")
          val plan = fixture.plan()
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(fixture.work(plan, fixture.claim(plan))))
          CarReviewJobExecutionFixtures.value(fixture.commit())
          val scope = fixture.native.cancellationScope(fixture.jobId(id)).get

          When("the original Job control cancels before the scheduler drains")
          CarReviewJobExecutionFixtures.value(fixture.cancel(id))
          fixture.drain()

          Then("there is no provider execution or canonical completion")
          fixture.runner.executions shouldBe 0
          fixture.runner.cancellations shouldBe 0
          scope.isCancelled shouldBe true
          fixture.query(id).get.status shouldBe JobStatus.Cancelled
          fixture.reportCount shouldBe 0
          fixture.attestationCount shouldBe 0
        }
      }

      Vector(false, true).foreach { throwing =>
        s"delivers active cancellation once with delegate failure=$throwing under native best effort (CJE8)" in {
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given("an active real ProviderCall with one original Job scope callback")
            val plan = fixture.plan()
            val id = CarReviewJobExecutionFixtures.value(fixture.stage(fixture.work(plan, fixture.claim(plan))))
            CarReviewJobExecutionFixtures.value(fixture.commit())
            val scope = fixture.native.cancellationScope(fixture.jobId(id)).get
            if throwing then fixture.runner.cancellation = () => throw new IllegalStateException("PRIVATE-CANCEL-DETAIL")
            fixture.runner.execution = request => {
              CarReviewJobExecutionFixtures.value(fixture.cancel(id))
              fixture.cancel(id)
              ProviderBundleRunnerResult.Completed(load("car-review-evidence-bundle-v1.json"), request.startedAtMillis + 1000L)
            }

            When("the delegate signals the actual native scope during execution")
            fixture.drain()
            fixture.cancel(id)

            Then("only one actual cancellation call is delivered and observed cancellation prevents canonical success")
            fixture.runner.executions shouldBe 1
            fixture.runner.cancellations shouldBe 1
            scope.isCancelled shouldBe true
            fixture.query(id).get.status should not be JobStatus.Succeeded
            fixture.reportCount shouldBe 0
            fixture.attestationCount shouldBe 0
            fixture.trace() should include("cancel-car-review-provider")
            if throwing then fixture.trace() should include("provider-cancellation-failed")
            fixture.trace() should not include "PRIVATE-CANCEL-DETAIL"
          }
        }
      }

      "closes the active provider callback before later Job control (CJE8)" in {
        CarReviewJobExecutionFixtures.withFixture { fixture =>
          Given("a real provider whose Job has completed its provider scope and canonical transaction")
          val plan = fixture.plan()
          val id = CarReviewJobExecutionFixtures.value(fixture.stage(fixture.work(plan, fixture.claim(plan))))
          CarReviewJobExecutionFixtures.value(fixture.commit())
          val scope = fixture.native.cancellationScope(fixture.jobId(id)).get
          fixture.drain()

          When("late control addresses the completed native Job")
          val result = fixture.cancel(id)
          scope.cancel()

          Then("native control retains its final-outcome policy and the closed provider callback cannot run")
          fixture.runner.executions shouldBe 1
          fixture.runner.cancellations shouldBe 0
          scope.isCancelled shouldBe true
          result shouldBe a[Consequence.Failure[_]]
          fixture.query(id).get.status shouldBe JobStatus.Succeeded
        }
      }
    }

    "interpret declared selections as shrinking semantic properties" which {
      "executes valid revisions and policy orders while refusing Review/provider conflicts before issuance (CJE10)" in {
        val generator = for {
          revision <- Gen.chooseNum(1L, 12L)
          reverse <- Gen.oneOf(false, true)
          kind <- Gen.oneOf("valid", "review", "provider")
        } yield (revision, reverse, kind)
        val property = Prop.forAll(generator) { case (revision, reverse, kind) =>
          CarReviewJobExecutionFixtures.withFixture { fixture =>
            Given(s"a fresh actual native fixture with declared revision $revision, reversed policy order=$reverse, selection=$kind")
            val plan = fixture.plan(revision, reverse)
            val base = fixture.work(plan, fixture.claim(plan))
            val work = kind match {
              case "review" => base.copy(template = base.template.copy(reviewId = ReviewId("foreign-review")))
              case "provider" => base.copy(providers = Vector(base.providers.head.copy(selection = base.providers.head.selection.copy(provider = ReviewProviderIdentity(ReviewProviderId("foreign"), ReviewVersion("1.0"))))))
              case _ => base
            }
            val issued = fixture.issuedIds

            val budget = CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage)

            When("the complete selected work enters the actual staged gateway")
            val staged = fixture.stage(work)
            if kind == "valid" then {
              val id = CarReviewJobExecutionFixtures.value(staged)
              CarReviewJobExecutionFixtures.value(fixture.commit())
              fixture.drain()

              Then("the valid selection executes one actual provider and persists exact signed native success")
              fixture.query(id).get.status shouldBe JobStatus.Succeeded
              fixture.native.signedStatus(fixture.jobId(id)) shouldBe "succeeded"
              _result_record(fixture.signedResult(id)).getString("reviewId") shouldBe Some(plan.request.reviewId.value)
              fixture.runner.executions shouldBe 1
              fixture.reportCount shouldBe 1
              fixture.attestationCount shouldBe 1
            } else {
              Then("the mismatch refuses before identity issuance, paid native admission, or provider execution")
              staged shouldBe a[Consequence.Failure[_]]
              fixture.issuedIds shouldBe issued
              CarReviewJobExecutionFixtures.value(fixture.native.budgetUsage) shouldBe budget
              fixture.runner.executions shouldBe 0
              fixture.reportCount shouldBe 0
            }
            true
          }
        }
        val result = Test.check(Test.Parameters.default.withInitialSeed(69710301L).withMinSuccessfulTests(24), property)
        result.passed shouldBe true
      }
    }
  }

  private def _result_record(response: Option[OperationResponse]): org.goldenport.record.Record = response match {
    case Some(OperationResponse.RecordResponse(record)) => record
    case _ => fail("Actual native success must retain the canonical RecordResponse")
  }
}
