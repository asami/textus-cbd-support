package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.Instant

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class IntentAwareUsageGuidanceSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "IntentAwareUsageGuidance" should {
    "attribute observed and inferred statements" which {
      "separates selected-source facts from deterministic intent inference" in {
        Given("catalog-owned usage with one operation observed from model metadata")
        val usage = _usage

        When("a caller supplies an intent that matches the observed operation")
        val result = IntentAwareUsageGuidance.enrich(usage, Some("retrieve an order"))

        Then("the selected source and version are explicit and no statement claims model inference")
        result.intent shouldBe Some("retrieve an order")
        result.selectedSourceId shouldBe Some("catalog")
        result.selectedSourceKind shouldBe Some(InformationSourceKind.PUBLISHED_CATALOG)
        result.selectedVersion shouldBe Some("1.2.0")
        result.guidance.map(_.statementKind) shouldBe Vector(
          IntentAwareUsageGuidance.OBSERVED_FACT,
          IntentAwareUsageGuidance.DETERMINISTIC_INFERENCE
        )
        result.guidance.map(_.statementKind) should not contain IntentAwareUsageGuidance.MODEL_INFERENCE
        val inferred = result.guidance.last
        inferred.service shouldBe Some("OrderQuery")
        inferred.operation shouldBe Some("getOrder")
        inferred.score shouldBe Some(0.5)
        inferred.evidenceUris shouldBe Vector(
          URI.create("https://catalog.example/textus-order.model-metadata.json"),
          URI.create("https://catalog.example/index.json")
        )
        inferred.rationale should include("order")
      }

      "matches Unicode intent terms without changing their script" in {
        Given("catalog-owned usage whose observed operation metadata contains a Japanese term")
        val japaneseoperation = ComponentOperation(Some("注文照会"), "注文検索", Some("query"), Some("注文検索"))
        val usage = _usage.copy(operations = _usage.operations :+ japaneseoperation)

        When("the caller supplies the same Japanese intent term")
        val result = IntentAwareUsageGuidance.enrich(usage, Some("注文検索"))

        Then("the Japanese operation is returned as deterministic inference")
        val inferred = result.guidance.filter(_.statementKind == IntentAwareUsageGuidance.DETERMINISTIC_INFERENCE)
        inferred.map(_.service) shouldBe Vector(Some("注文照会"))
        inferred.map(_.operation) shouldBe Vector(Some("注文検索"))
        inferred.map(_.score) shouldBe Vector(Some(1.0))
      }

      "does not fabricate an operation recommendation for unrelated intent" in {
        Given("the same catalog-owned usage evidence")
        val usage = _usage

        When("the requested intent has no terms in the observed operation metadata")
        val result = IntentAwareUsageGuidance.enrich(usage, Some("calculate payroll"))

        Then("only the selected-source observed fact remains")
        result.guidance.map(_.statementKind) shouldBe Vector(IntentAwareUsageGuidance.OBSERVED_FACT)
        result.guidance.flatMap(_.operation) shouldBe empty
      }

      "withholds attributable guidance when source context is absent" in {
        Given("provider usage that has not been attached to a runtime source observation")
        val usage = _usage.copy(profile = _usage.profile.copy(observationContext = None))

        When("intent guidance is requested")
        val result = IntentAwareUsageGuidance.enrich(usage, Some("retrieve an order"))

        Then("the runtime reports the missing attribution instead of inferring a source")
        result.selectedSourceId shouldBe None
        result.selectedSourceKind shouldBe None
        result.guidance shouldBe empty
        result.warnings.exists(_.contains("source identity is absent")) shouldBe true
        result.warnings.exists(_.contains("source kind is absent")) shouldBe true
      }
    }

    "bound intent interpretation" which {
      "rejects intent text beyond the character limit" in {
        Given("an intent one character beyond the documented limit")
        val oversizedintent = "x" * (IntentAwareUsageGuidance.MAXIMUM_INTENT_LENGTH + 1)

        When("usage guidance interprets the oversized intent")
        val result = IntentAwareUsageGuidance.enrich(_usage, Some(oversizedintent))

        Then("the intent and its inference are withheld while the observed fact remains")
        result.intent shouldBe None
        result.guidance.map(_.statementKind) shouldBe Vector(IntentAwareUsageGuidance.OBSERVED_FACT)
        result.warnings.exists(_.contains("exceeds 512 characters")) shouldBe true
      }

      "does not inspect intent tokens beyond the token limit" in {
        Given("an intent whose only operation match occurs after the admitted token range")
        val admittedtokens = (1 to IntentAwareUsageGuidance.MAXIMUM_INTENT_TOKENS).map(x => s"token$x")
        val intent = (admittedtokens :+ "order").mkString(" ")

        When("usage guidance evaluates the bounded intent")
        val result = IntentAwareUsageGuidance.enrich(_usage, Some(intent))

        Then("the out-of-bound token cannot create an inferred recommendation")
        result.guidance.map(_.statementKind) shouldBe Vector(IntentAwareUsageGuidance.OBSERVED_FACT)
      }

      "caps the total guidance record count" in {
        Given("more matching operations than the response guidance bound")
        val operations = (1 to 40).map { index =>
          ComponentOperation(Some("OrderQuery"), s"getOrder$index", Some("query"), Some("order"))
        }.toVector
        val usage = _usage.copy(operations = operations)

        When("usage guidance ranks every matching operation")
        val result = IntentAwareUsageGuidance.enrich(usage, Some("order"))

        Then("one observed fact and only the bounded number of inferences are returned")
        result.guidance should have size IntentAwareUsageGuidance.MAXIMUM_GUIDANCE_RECORDS
        result.guidance.head.statementKind shouldBe IntentAwareUsageGuidance.OBSERVED_FACT
        result.guidance.tail.map(_.statementKind).distinct shouldBe Vector(IntentAwareUsageGuidance.DETERMINISTIC_INFERENCE)
      }
    }
  }

  private def _usage: ComponentUsage = {
    val profile = ComponentProfile(
      "catalog",
      Some("org.textus"),
      "textus-order",
      "Textus Order",
      Some("Order retrieval component."),
      "car",
      Vector("1.2.0"),
      Some("1.2.0"),
      Some("1.2.0"),
      Some("1.2.0"),
      None,
      Some("0.5.1"),
      Vector("business.order"),
      Vector.empty,
      Vector.empty,
      Some(URI.create("https://catalog.example/textus-order.car")),
      URI.create("https://catalog.example/index.json"),
      Some(URI.create("https://catalog.example/textus-order.model-metadata.json")),
      None,
      Vector.empty,
      Vector.empty,
      observationContext = Some(ComponentObservationContext(
        "catalog",
        InformationSourceKind.PUBLISHED_CATALOG,
        Instant.parse("2026-07-14T08:00:00Z"),
        Instant.parse("2026-07-14T08:15:00Z"),
        Vector.empty
      ))
    )
    ComponentUsage(
      profile,
      Vector(ComponentOperation(Some("OrderQuery"), "getOrder", Some("query"), Some("Return one order."))),
      Vector(
        ("catalog", profile.evidenceUri, true),
        ("model-metadata", profile.modelMetadataUri.get, true)
      ),
      Vector.empty
    )
  }
}
