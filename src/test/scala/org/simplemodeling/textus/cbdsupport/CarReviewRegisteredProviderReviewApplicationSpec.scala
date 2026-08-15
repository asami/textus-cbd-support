package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewRegisteredProviderReviewApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The registered CAR Review provider application" should {
    "preflight frozen registered provider selections" which {
    "produce one canonical report from a policy-bound registered static-quality provider" in {
      Given("one registered deterministic static-quality provider and its fully frozen diagnosis plan")
      val profile = _profile()
      val policy = _policy
      val registry = new CarReviewProviderRegistry()
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      registry.register(descriptor, new CarReviewInitialStaticQualityProviderRunner(profile), policy).isRight shouldBe true
      val execution = _execution(profile, policy)

      When("CBD preflights, executes, admits, and canonicalizes the registered provider selection")
      val response = new CarReviewRegisteredProviderReviewApplication(registry).execute(execution, Set("reviewer")).fold(_fail, identity)
      val delivery = CarReviewDeliveryProjection.project(response.report)

      Then("the canonical report retains provider, rule, and admitted bundle attribution with total quality coverage")
      response.report.execution.providers.map(_.provider) shouldBe Vector(profile.provider)
      response.report.execution.providers.map(_.ruleSet) shouldBe Vector(profile.ruleSet)
      response.report.execution.providers.head.bundleDigest should not be empty
      response.report.observations.exists(observation =>
        observation.provider.provider == profile.provider &&
          observation.provider.ruleSet == profile.ruleSet &&
          observation.id.value.startsWith(s"${profile.provider.id.value}:initial-static-")
      ) shouldBe true
      delivery.qualityCoverage.size shouldBe CarReviewCapabilityCatalog.definitions.size
      delivery.qualityCoverage.exists(_.state == CarReviewQualityCoverageState.Observed) shouldBe true
      delivery.qualityCoverage.exists(_.state == CarReviewQualityCoverageState.Unknown) shouldBe true
    }

    "reject a policy digest mismatch before the registered runner executes" in {
      Given("one registered provider whose frozen selection has a different availability-policy digest")
      val profile = _profile()
      val runner = new CountingRunner(new CarReviewInitialStaticQualityProviderRunner(profile))
      val registry = new CarReviewProviderRegistry()
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      registry.register(descriptor, runner, _policy).isRight shouldBe true
      val original = _execution(profile, _policy)
      val selection = original.plan.reuseInput.providerSelections.head.copy(
        availabilityPolicyDigest = ReviewDigest("sha256:" + ("f" * 64))
      )
      val plan = _plan(original.plan.request, original.plan.reuseInput.copy(providerSelections = Vector(selection)))
      val execution = original.copy(plan = plan)

      When("CBD verifies the actual registration policy against the frozen selection")
      val response = new CarReviewRegisteredProviderReviewApplication(registry).execute(execution, Set("reviewer"))

      Then("the mismatch is explicit and no provider work can occur")
      response.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-policy-digest-mismatch"), _ => fail("Unexpected canonical response."))
      runner.executions shouldBe 0
    }

    "reject missing registry and plan-mismatched selections without producing a canonical response" in {
      Given("one frozen registered-provider execution and a counting implementation")
      val profile = _profile()
      val runner = new CountingRunner(new CarReviewInitialStaticQualityProviderRunner(profile))
      val execution = _execution(profile, _policy)
      val registered = new CarReviewProviderRegistry()
      registered.register(CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile), runner, _policy).isRight shouldBe true
      val mismatched = execution.copy(providers = execution.providers.map(selection =>
        selection.copy(request = selection.request.copy(target = selection.request.target.copy(name = "another-target")))
      ))
      val expandedinput = execution.plan.reuseInput.copy(
        ruleSets = execution.plan.reuseInput.ruleSets :+ ReviewRuleIdentity(ReviewRuleId("unselected-rule"), ReviewVersion("1.0.0"))
      )
      val expanded = execution.copy(plan = _plan(execution.plan.request, expandedinput))

      When("CBD receives an unregistered selection, a selection that diverges from its frozen plan, and an extra frozen rule")
      val missing = new CarReviewRegisteredProviderReviewApplication(new CarReviewProviderRegistry()).execute(execution, Set("reviewer"))
      val divergent = new CarReviewRegisteredProviderReviewApplication(registered).execute(mismatched, Set("reviewer"))
      val extrarule = new CarReviewRegisteredProviderReviewApplication(registered).execute(expanded, Set("reviewer"))

      Then("every mismatch fails before a runner can fabricate a Report")
      missing.fold(error => error.toString should include ("textus.cbd.review.failure.v1:provider-not-registered"), _ => fail("Unexpected canonical response."))
      divergent.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-request-target-mismatch"), _ => fail("Unexpected canonical response."))
      extrarule.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-selection-plan-mismatch"), _ => fail("Unexpected canonical response."))
      runner.executions shouldBe 0
    }

    "reject an altered admitted descriptor or provider request before the registered runner executes" in {
      Given("one original registered descriptor and a frozen provider request identity")
      val profile = _profile()
      val runner = new CountingRunner(new CarReviewInitialStaticQualityProviderRunner(profile))
      val registry = new CarReviewProviderRegistry()
      val original = _execution(profile, _policy)
      registry.register(original.providers.head.descriptorDocument, runner, _policy).isRight shouldBe true
      val changeddescriptor = original.providers.head.descriptorDocument.replace(
        "Missing static analyzer facts remain Unknown and do not imply a passing quality check.",
        "Changed limitation text remains a distinct registered descriptor document."
      )
      val altereddescriptor = original.copy(providers = original.providers.map(selection =>
        selection.copy(descriptorDocument = changeddescriptor, request = selection.request.copy(descriptor = changeddescriptor))
      ))
      val alteredrequest = original.copy(providers = original.providers.map(selection =>
        selection.copy(request = selection.request.copy(providerRequest = selection.request.providerRequest.replace("\"timeoutMillis\":1000", "\"timeoutMillis\":999")))
      ))
      val innerreviewidmismatch = original.copy(providers = original.providers.map(selection =>
        selection.copy(request = selection.request.copy(providerRequest = selection.request.providerRequest.replace(
          s"\"reviewId\":\"${selection.request.reviewId.value}\"",
          "\"reviewId\":\"review-provider-inner-mismatch\""
        )))
      ))
      val innertargetmismatch = original.copy(providers = original.providers.map(selection =>
        selection.copy(request = selection.request.copy(providerRequest = selection.request.providerRequest.replace(
          s"\"name\":\"${selection.request.target.name}\"",
          "\"name\":\"another-target\""
        )))
      ))

      When("CBD preflights complete descriptor and provider-request documents against their outer execution identity")
      val descriptorresponse = new CarReviewRegisteredProviderReviewApplication(registry).execute(altereddescriptor, Set("reviewer"))
      val requestresponse = new CarReviewRegisteredProviderReviewApplication(registry).execute(alteredrequest, Set("reviewer"))
      val innerreviewidresponse = new CarReviewRegisteredProviderReviewApplication(registry).execute(innerreviewidmismatch, Set("reviewer"))
      val innertargetresponse = new CarReviewRegisteredProviderReviewApplication(registry).execute(innertargetmismatch, Set("reviewer"))

      Then("projection-preserving descriptor text and every altered request identity cannot use the frozen plan")
      descriptorresponse.fold(error => error.toString should include ("textus.cbd.review.failure.v1:provider-registration-mismatch"), _ => fail("Unexpected canonical response."))
      requestresponse.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-policy-digest-mismatch"), _ => fail("Unexpected canonical response."))
      innerreviewidresponse.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-request-review-id-mismatch"), _ => fail("Unexpected canonical response."))
      innertargetresponse.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-request-target-mismatch"), _ => fail("Unexpected canonical response."))
      runner.executions shouldBe 0
    }

    "separate full execution policy binding from Review-ID-free reusable policy binding" in {
      Given("two otherwise equal provider request documents whose only difference is their per-Run Review ID")
      val profile = _profile()
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      val descriptordigest = CarReviewProviderBundleAdmission.descriptorDigest(descriptor).fold(_fail_admission, identity)
      val target = _template.target
      val firstdocument = CarReviewInitialStaticQualityProviderRunner.requestDocument(ReviewId("review-provider-request-a"), target)
      val seconddocument = CarReviewInitialStaticQualityProviderRunner.requestDocument(ReviewId("review-provider-request-b"), target)
      val changeddocument = firstdocument.replace("\"timeoutMillis\":1000", "\"timeoutMillis\":999")
      val firstrequestdigest = CarReviewProviderBundleAdmission.requestDigest(firstdocument).fold(_fail_admission, identity)
      val secondrequestdigest = CarReviewProviderBundleAdmission.requestDigest(seconddocument).fold(_fail_admission, identity)
      val firstfull = CarReviewProviderSelectionPolicy.digest(descriptordigest, firstrequestdigest, ProviderBundleAvailability.Enabled, _policy)
      val secondfull = CarReviewProviderSelectionPolicy.digest(descriptordigest, secondrequestdigest, ProviderBundleAvailability.Enabled, _policy)
      val firstreuse = CarReviewProviderSelectionPolicy.reuseDigest(descriptordigest, firstdocument, ProviderBundleAvailability.Enabled, _policy).fold(_fail_admission, identity)
      val secondreuse = CarReviewProviderSelectionPolicy.reuseDigest(descriptordigest, seconddocument, ProviderBundleAvailability.Enabled, _policy).fold(_fail_admission, identity)
      val changedreuse = CarReviewProviderSelectionPolicy.reuseDigest(descriptordigest, changeddocument, ProviderBundleAvailability.Enabled, _policy).fold(_fail_admission, identity)
      val execution = _execution(profile, _policy)

      When("CBD calculates full execution and reusable provider-selection policy digests")
      val actualrequest = execution.providers.head.request.providerRequest

      Then("execution keeps the actual ID while reusable selection excludes only that Run identity")
      _request_review_id(actualrequest) shouldBe execution.plan.request.reviewId.value
      firstrequestdigest should not be secondrequestdigest
      firstfull should not be secondfull
      firstreuse shouldBe secondreuse
      changedreuse should not be firstreuse
    }

    "preflight every selection before any runner executes when a later selected request is malformed" in {
      Given("two distinct registered providers and a second malformed provider request")
      val firstprofile = _profile()
      val secondprofile = _profile("cozy-static-quality-second", "cozy-static-quality-second.car-review")
      val firstrunner = new CountingRunner(new CarReviewInitialStaticQualityProviderRunner(firstprofile))
      val secondrunner = new CountingRunner(new CarReviewInitialStaticQualityProviderRunner(secondprofile))
      val registry = new CarReviewProviderRegistry()
      val execution = _two_provider_execution(firstprofile, secondprofile, _policy)
      registry.register(execution.providers(0).descriptorDocument, firstrunner, _policy).isRight shouldBe true
      registry.register(execution.providers(1).descriptorDocument, secondrunner, _policy).isRight shouldBe true
      val malformed = execution.copy(providers = execution.providers.updated(1, execution.providers(1).copy(request = execution.providers(1).request.copy(providerRequest = "{"))))

      When("the second frozen selection has no valid provider-request document")
      val response = new CarReviewRegisteredProviderReviewApplication(registry).execute(malformed, Set("reviewer"))

      Then("all preflight fails before the first registered runner starts")
      response.fold(error => error.toString should include ("textus.cbd.review.failure.v1:registered-provider-request-invalid"), _ => fail("Unexpected canonical response."))
      firstrunner.executions shouldBe 0
      secondrunner.executions shouldBe 0
    }
    }
  }

  private val _policy = CarReviewQualityProviderPolicy(
    CarReviewQualityProviderAuthority.Deterministic,
    declaredCostUnits = 0L,
    maximumCostUnits = 0L
  )

  private def _execution(
    profile: CarReviewInitialStaticQualityProviderProfile,
    policy: CarReviewQualityProviderPolicy
  ): CarReviewRegisteredReviewExecution = {
    val template = _template
    val request = ReviewStartRequest(template.reviewId, template.target, template.profile, template.execution.startedAt)
    val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
    val providerrequest = CarReviewInitialStaticQualityProviderRunner.requestDocument(request.reviewId, request.target)
    val selectionrequest = ProviderBundleExecutionRequest(
      request.reviewId,
      request.target,
      profile.provider,
      ProviderBundleAvailability.Enabled,
      descriptor,
      providerrequest,
      startedAtMillis = 0L
    )
    val selectionpolicy = _selection_policy(descriptor, providerrequest, selectionrequest.availability, policy)
    val reuseinput = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      template.schemaVersion,
      request.target,
      request.profile,
      None,
      Vector(profile.ruleSet),
      Vector(CarReviewReuseProviderSelection(profile.provider, profile.ruleSet, selectionpolicy)),
      Vector.empty,
      _policy_bindings
    )
    CarReviewRegisteredReviewExecution(
      _plan(request, reuseinput),
      template,
      Vector(CarReviewRegisteredProviderSelection(descriptor, selectionrequest))
    )
  }

  private def _plan(request: ReviewStartRequest, input: CarReviewReuseKeyInput): CarReviewExecutionPlan =
    CarReviewExecutionPlan.create(request, input).fold(_fail_plan, identity)

  private def _two_provider_execution(
    first: CarReviewInitialStaticQualityProviderProfile,
    second: CarReviewInitialStaticQualityProviderProfile,
    policy: CarReviewQualityProviderPolicy
  ): CarReviewRegisteredReviewExecution = {
    val template = _template
    val request = ReviewStartRequest(template.reviewId, template.target, template.profile, template.execution.startedAt)
    val selections = Vector(first, second).map { profile =>
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
      val providerrequest = CarReviewInitialStaticQualityProviderRunner.requestDocument(request.reviewId, request.target)
      val executionrequest = ProviderBundleExecutionRequest(request.reviewId, request.target, profile.provider, ProviderBundleAvailability.Enabled, descriptor, providerrequest, 0L)
      CarReviewRegisteredProviderSelection(descriptor, executionrequest)
    }
    val reuseinput = CarReviewReuseKeyInput(
      CarReviewReuseKey.DEFINITION_ID,
      template.schemaVersion,
      request.target,
      request.profile,
      None,
      Vector(first.ruleSet, second.ruleSet),
      selections.map(selection => CarReviewReuseProviderSelection(
        selection.request.provider,
        CarReviewProviderBundleAdmission.describeDescriptor(selection.descriptorDocument).fold(_fail_admission, _.ruleSet),
        _selection_policy(selection.descriptorDocument, selection.request.providerRequest, selection.request.availability, policy)
      )),
      Vector.empty,
      _policy_bindings
    )
    CarReviewRegisteredReviewExecution(_plan(request, reuseinput), template, selections)
  }

  private def _selection_policy(
    descriptor: String,
    providerrequest: String,
    availability: ProviderBundleAvailability,
    policy: CarReviewQualityProviderPolicy
  ): ReviewDigest =
    CarReviewProviderSelectionPolicy.reuseDigest(
      CarReviewProviderBundleAdmission.descriptorDigest(descriptor).fold(_fail_admission, identity),
      providerrequest,
      availability,
      policy
    ).fold(_fail_admission, identity)

  private def _request_review_id(document: String): String =
    io.circe.parser.parse(document).toOption
      .flatMap(_.hcursor.get[String]("reviewId").toOption)
      .getOrElse(fail("Provider request reviewId is missing."))

  private def _profile(
    providerid: String = "cozy-static-quality",
    rulesetid: String = "cozy-static-quality.car-review"
  ): CarReviewInitialStaticQualityProviderProfile =
    CarReviewInitialStaticQualityProviderProfile(
      ReviewProviderIdentity(ReviewProviderId(providerid), ReviewVersion("0.1.0")),
      ReviewRuleIdentity(ReviewRuleId(rulesetid), ReviewVersion("1.0.0")),
      CarReviewInitialStaticQualityEvidence(
        "sha256:" + ("a" * 64),
        Some(true), None, None, None, None, None, None, None, None, None, None, None
      )
    )

  private val _policy_bindings = Vector(
    CarReviewReusePolicyBinding("profile", "review-profile", ReviewVersion("1.0.0"), ReviewDigest("sha256:" + ("1" * 64))),
    CarReviewReusePolicyBinding("gate", "review-gate", ReviewVersion("1.0.0"), ReviewDigest("sha256:" + ("2" * 64))),
    CarReviewReusePolicyBinding("reconciliation", "review-reconciliation", ReviewVersion("1.0.0"), ReviewDigest("sha256:" + ("3" * 64))),
    CarReviewReusePolicyBinding("suppression", "review-suppression", ReviewVersion("1.0.0"), ReviewDigest("sha256:" + ("4" * 64)))
  )

  private lazy val _template = {
    val source = CarReviewReportCodec.decode(
      Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))
    ).fold(_fail_codec, identity)
    source.copy(baseline = None, execution = source.execution.copy(providers = Vector.empty))
  }

  private final class CountingRunner(delegate: CarReviewProviderRunner) extends CarReviewProviderRunner {
    private var _executions = 0

    def executions: Int = _executions

    def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult = {
      _executions += 1
      delegate.execute(request)
    }

    def cancel(request: ProviderBundleExecutionRequest): Unit = delegate.cancel(request)
  }

  private def _fail(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
  private def _fail_admission(error: String): Nothing = fail(error)
  private def _fail_plan(error: CarReviewExecutionPlanFailure): Nothing = fail(s"${error.code}: ${error.message}")
  private def _fail_codec(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
