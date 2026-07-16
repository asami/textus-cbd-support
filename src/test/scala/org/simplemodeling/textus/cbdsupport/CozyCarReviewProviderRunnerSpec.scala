package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CozyCarReviewProviderRunnerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD Cozy CAR Review provider runner" should {
    "invoke only the registered Cozy transport for its admitted CAR target" in {
      Given("one registered Cozy descriptor, admitted target root, and neutral provider transport")
      val coordinator = new CarReviewProviderExecutionCoordinator()
      val registry = new CarReviewProviderRegistry()
      val transport = new RecordingTransport(_provider, Right(CozyCarReviewProviderTransportResult(_bundle, 1000L)))
      val runner = new CozyCarReviewProviderRunner(CozyCarReviewProviderTarget(_target, Path.of("/admitted/car")), transport)
      registry.register(_descriptor, runner).isRight shouldBe true

      When("CBD selects the descriptor-bound runner through the provider protocol")
      val outcome = coordinator.execute(_request(), registry)

      Then("the exact provider request reaches Cozy once and CBD admits its bundle")
      outcome should matchPattern {
        case ProviderBundleExecutionOutcome.Admitted(AdmittedProviderBundle(ReviewProviderIdentity(ReviewProviderId("cozy"), _), _, _, _, _, _, _), false) =>
      }
      transport.requests shouldBe Vector(_providerrequest)
      transport.timeouts shouldBe Vector(120000L)
      transport.cancellations shouldBe 0
    }

    "refuse a non-admitted target before invoking Cozy" in {
      Given("one Cozy runner bound to a different admitted CAR target")
      val transport = new RecordingTransport(_provider, Right(CozyCarReviewProviderTransportResult(_bundle, 1000L)))
      val runner = new CozyCarReviewProviderRunner(
        CozyCarReviewProviderTarget(_target.copy(name = "other-car"), Path.of("/admitted/other-car")),
        transport
      )

      When("CBD tries to invoke it for the original target")
      val result = runner.execute(_request())

      Then("the transport is never called and target authority remains explicit")
      result shouldBe ProviderBundleRunnerResult.Failed(
        "provider-target-not-admitted",
        "Configured Cozy target does not match the admitted Review target.",
        0L
      )
      transport.requests shouldBe Vector.empty
    }

    "refuse a configured Cozy transport whose version differs from the registered provider" in {
      Given("one target-bound transport for a different Cozy implementation version")
      val transport = new RecordingTransport(
        ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.15")),
        Right(CozyCarReviewProviderTransportResult(_bundle, 1000L))
      )
      val runner = new CozyCarReviewProviderRunner(CozyCarReviewProviderTarget(_target, Path.of("/admitted/car")), transport)

      When("the registered provider identity requests execution")
      val result = runner.execute(_request())

      Then("CBD refuses the mismatch before command transport work")
      result shouldBe ProviderBundleRunnerResult.Failed(
        "provider-identity-mismatch",
        "Configured Cozy transport does not match the registered provider identity.",
        0L
      )
      transport.requests shouldBe Vector.empty
    }

    "run a fixed local provider command with request stdin and an empty child environment" in {
      Given("one admitted target and a command that rejects inherited HOME authority")
      val root = Files.createTempDirectory("cbd-cozy-provider-transport")
      val script = root.resolve("provider-command.sh")
      Files.writeString(script, "#!/bin/sh\nif [ -n \"$HOME\" ]; then exit 21; fi\ncat\n")
      script.toFile.setExecutable(true)
      try {
        val transport = new CozyCarReviewProviderProcessTransport(
          CozyCarReviewProviderCommand(
            Vector("/bin/sh", script.toString),
            CozyCarReviewProviderTarget(_target, root),
            ReviewVersion("0.1.14"),
            root.resolve("output"),
            maxRequestBytes = 1024,
            maxResponseBytes = 1024
          ),
          () => 1000L
        )

        When("CBD invokes the fixed provider command with the neutral request on stdin")
        val result = transport.execute("{\"request\":\"bounded\"}", 1000L)

        Then("the private response is returned without ambient environment inheritance")
        result shouldBe Right(CozyCarReviewProviderTransportResult("{\"request\":\"bounded\"}", 1000L))
      } finally {
        _delete(root)
      }
    }
  }

  private val _descriptor = _load("car-review-provider-descriptor-v1.json")
  private val _providerrequest = _load("car-review-provider-request-v1.json")
  private val _bundle = _load("car-review-evidence-bundle-v1.json")

  private val _target = ReviewTarget(
    ReviewTargetKind("project"),
    Some("org.textus"),
    "textus-user-account",
    Some(ReviewVersion("0.2.0-SNAPSHOT")),
    ReviewDigest("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
  )

  private val _provider = ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14"))

  private def _request(): ProviderBundleExecutionRequest =
    ProviderBundleExecutionRequest(
      ReviewId("review-example-001"),
      _target,
      _provider,
      ProviderBundleAvailability.Enabled,
      _descriptor,
      _providerrequest,
      startedAtMillis = 0L
    )

  private def _load(name: String): String = Files.readString(Path.of("docs", "spec", "examples", name))

  private def _delete(root: Path): Unit = {
    val stream = Files.walk(root)
    try stream.sorted(java.util.Comparator.reverseOrder()).forEach(path => Files.deleteIfExists(path))
    finally stream.close()
  }

  private final class RecordingTransport(provideridentity: ReviewProviderIdentity, result: Either[String, CozyCarReviewProviderTransportResult]) extends CozyCarReviewProviderTransport {
    private var _requests = Vector.empty[String]
    private var _timeouts = Vector.empty[Long]
    private var _cancellations = 0

    def requests: Vector[String] = _requests
    def timeouts: Vector[Long] = _timeouts
    def cancellations: Int = _cancellations
    def provider: ReviewProviderIdentity = provideridentity

    def execute(providerRequest: String, timeoutMillis: Long): Either[String, CozyCarReviewProviderTransportResult] = {
      _requests = _requests :+ providerRequest
      _timeouts = _timeouts :+ timeoutMillis
      result
    }

    def cancel(): Unit = {
      _cancellations += 1
    }
  }
}
