package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import io.circe.{Json, Printer}
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 24, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewQualityProviderAdmissionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review quality-provider admission boundary" should {
    "admit a bounded deterministic provider only within its finite preflight cost" in {
      Given("one admitted fixed static-quality provider bundle")
      val (context, provider) = _context()
      val policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, declaredCostUnits = 0L, maximumCostUnits = 0L)

      When("CBD preflights and admits the deterministic bundle")
      val preflight = CarReviewQualityProviderAdmission.preflight(provider, policy)
      val admitted = CarReviewQualityProviderAdmission.admit(context, policy)

      Then("the provider has explicit deterministic authority and no unbounded cost")
      preflight shouldBe Right(())
      admitted shouldBe a[ProviderBundleAdmissionOutcome.Admitted]
    }

    "refuse budget excess, advisory Assurance, runtime Assurance without runtime Evidence, and unredacted facts" in {
      Given("a standard static-quality bundle plus constrained authority policies")
      val (context, provider) = _context()
      val budget = CarReviewQualityProviderAdmission.preflight(provider, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, 2L, 1L))
      val advisory = CarReviewQualityProviderAdmission.admit(context, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Advisory, 0L, 0L))
      val runtime = CarReviewQualityProviderAdmission.admit(context, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Runtime, 0L, 0L))
      val raw = context.copy(bundle = _with_bundle_digest(context.bundle.replace("\"passed\":true", "\"passed\":true,\"password\":\"must-not-retain\"")))
      val redaction = CarReviewQualityProviderAdmission.admit(raw, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, 0L, 0L))

      When("each policy boundary is evaluated after strict v1 bundle admission")

      Then("CBD preserves the provider identity while refusing every unsafe authority or Evidence path")
      budget.left.toOption.map(_.limitation.code) shouldBe Some("provider-cost-limit-exceeded")
      advisory should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("advisory-provider-authority-violation", _, _, _, _), false)) =>
      }
      runtime should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("runtime-assurance-evidence-required", _, _, _, _), false)) =>
      }
      redaction should matchPattern {
        case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("quality-provider-evidence-redaction-required", _, _, _, _), false)) =>
      }
    }

    "apply the same policy at provider execution rather than only at standalone admission" in {
      Given("a runner returning a static Assurance bundle")
      val (context, provider) = _context()
      val request = ProviderBundleExecutionRequest(context.reviewId, context.target, provider, ProviderBundleAvailability.Enabled, context.descriptor, context.request, 0L)
      val runner = new CarReviewProviderRunner {
        def execute(value: ProviderBundleExecutionRequest): ProviderBundleRunnerResult = ProviderBundleRunnerResult.Completed(context.bundle, value.startedAtMillis)
        def cancel(value: ProviderBundleExecutionRequest): Unit = ()
      }

      When("the coordinator executes under Runtime authority")
      val outcome = new CarReviewProviderExecutionCoordinator().execute(
        request,
        runner,
        CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Runtime, 0L, 0L)
      )

      Then("the static Assurance is refused because no runtime-observation Evidence backs it")
      outcome should matchPattern {
        case ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("runtime-assurance-evidence-required", _, _, _, _), false)) =>
      }
    }

    "admit Runtime Assurance only when descriptor-declared runtime-observation Evidence backs it" in {
      Given("one bundle whose descriptor, request, and Evidence all declare runtime-observation")
      val (context, _) = _context()
      val runtimerequest = context.request.replace("static-quality-check", CarReviewRuntimeEvidencePolicy.RuntimeObservationKind)
      val runtimebundle = _with_bundle_digest(_replace_request_digest(
        context.bundle.replace("static-quality-check", CarReviewRuntimeEvidencePolicy.RuntimeObservationKind),
        runtimerequest
      ))
      val runtimecontext = context.copy(
        descriptor = context.descriptor.replace("static-quality-check", CarReviewRuntimeEvidencePolicy.RuntimeObservationKind),
        request = runtimerequest,
        bundle = runtimebundle
      )

      When("CBD applies Runtime authority")
      val outcome = CarReviewQualityProviderAdmission.admit(runtimecontext, CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Runtime, 0L, 0L))

      Then("the provider is admitted because every Assurance references declared bounded runtime Evidence")
      outcome shouldBe a[ProviderBundleAdmissionOutcome.Admitted]
    }

    "refuse separator and case variants recursively while admitting an unrelated URL-like key" in {
      Given("separately re-digested bundles with unsafe fact-key variants in nested objects and arrays")
      val (context, provider) = _context()
      val policy = CarReviewQualityProviderPolicy(CarReviewQualityProviderAuthority.Deterministic, 0L, 0L)
      val unsafevariants = Vector(
        _with_nested_fact(context.bundle, Vector("password"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("apiKey"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("api_key"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("api-key"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("rawRequest"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("raw_request"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("nestedObject", "raw-request"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("authorizationHeader"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("authorization_header"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("authorization.header"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("endpoint"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("URL"), Json.fromString("must-not-retain")),
        _with_nested_fact(context.bundle, Vector("url"), Json.fromString("must-not-retain")),
        _with_nested_fact(
          context.bundle,
          Vector("nestedArray"),
          Json.arr(Json.obj("RawResponse" -> Json.fromString("must-not-retain")))
        )
      )
      val safevariant = _with_nested_fact(context.bundle, Vector("metadata", "curlVersion"), Json.fromString("8.0"))

      When("CBD admits each re-digested bundle under deterministic policy")
      val unsafeoutcomes = unsafevariants.map(bundle =>
        CarReviewQualityProviderAdmission.admit(context.copy(bundle = bundle), policy)
      )
      val safeoutcome = CarReviewQualityProviderAdmission.admit(context.copy(bundle = safevariant), policy)

      Then("every unsafe variant is refused for redaction while the unrelated key remains admissible")
      unsafeoutcomes.foreach { outcome =>
        outcome should matchPattern {
          case ProviderBundleAdmissionOutcome.Refused(ProviderBundleUnknown(_, _, ReviewLimitation("quality-provider-evidence-redaction-required", _, _, _, _), false)) =>
        }
      }
      safeoutcome shouldBe a[ProviderBundleAdmissionOutcome.Admitted]
    }
  }

  private def _context(): (ProviderBundleAdmissionContext, ReviewProviderIdentity) = {
    val provider = ReviewProviderIdentity(ReviewProviderId("cozy-static-quality"), ReviewVersion("0.1.0"))
    val profile = CarReviewInitialStaticQualityProviderProfile(
      provider,
      ReviewRuleIdentity(ReviewRuleId("cozy-static-quality.car-review"), ReviewVersion("1.0.0")),
      CarReviewInitialStaticQualityEvidence(
        "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true)
      )
    )
    val target = ReviewTarget(ReviewTargetKind("project"), Some("org.textus"), "textus-user-account", Some(ReviewVersion("0.2.0-SNAPSHOT")), ReviewDigest("sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"))
    val reviewid = ReviewId("review-quality-policy-001")
    val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(profile)
    val request = CarReviewInitialStaticQualityProviderRunner.requestDocument(reviewid, target)
    val execution = ProviderBundleExecutionRequest(reviewid, target, provider, ProviderBundleAvailability.Enabled, descriptor, request, 0L)
    val bundle = new CarReviewInitialStaticQualityProviderRunner(profile).execute(execution) match {
      case ProviderBundleRunnerResult.Completed(value, _) => value
      case value => fail(s"Expected completed static-quality bundle but got $value")
    }
    ProviderBundleAdmissionContext(reviewid, target, ProviderBundleAvailability.Enabled, descriptor, request, bundle) -> provider
  }

  private def _with_nested_fact(bundle: String, path: Vector[String], value: Json): String = {
    val printer = Printer.noSpaces.copy(sortKeys = true)
    val json = parse(bundle).toOption.get
    val evidence = json.hcursor.downField("evidence").focus.flatMap(_.asArray).get
    val nested = path.reverse.foldLeft(value) { case (current, segment) => Json.obj(segment -> current) }
    val updatedevidence = evidence.updated(0, evidence.head.mapObject { fields =>
      val facts = fields("facts").getOrElse(Json.obj())
      fields.add("facts", facts.deepMerge(nested))
    })
    val body = json.mapObject(_.add("evidence", Json.fromValues(updatedevidence)))
    _with_bundle_digest(printer.print(body))
  }

  private def _with_bundle_digest(value: String): String = {
    val printer = Printer.noSpaces.copy(sortKeys = true)
    val json = parse(value).toOption.get
    val body = json.mapObject(_.remove("bundleDigest"))
    val digest = MessageDigest.getInstance("SHA-256").digest(printer.print(body).getBytes(StandardCharsets.UTF_8))
    val rendered = "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
    printer.print(json.mapObject(_.add("bundleDigest", Json.fromString(rendered))))
  }

  private def _replace_request_digest(bundle: String, request: String): String = {
    val printer = Printer.noSpaces.copy(sortKeys = true)
    val requestdigest = _digest(parse(request).toOption.get, printer)
    val json = parse(bundle).toOption.get
    printer.print(json.mapObject(_.add("requestDigest", Json.fromString(requestdigest))))
  }

  private def _digest(value: Json, printer: Printer): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(printer.print(value).getBytes(StandardCharsets.UTF_8))
    "sha256:" + digest.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
