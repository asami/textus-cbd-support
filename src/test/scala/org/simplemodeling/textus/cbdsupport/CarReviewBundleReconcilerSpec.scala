package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 16, 2026
 * @version Jul. 24, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewBundleReconcilerSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CAR Review bundle reconciliation" should {
    "preserve provider, bundle, Evidence, and Observation identities without selecting a winner" in {
      Given("one admitted canonical Cozy evidence bundle")
      val input = AdmittedProviderBundleInput(_admitted, _bundle)

      When("CBD reconciles the already-admitted bundle without invoking a provider")
      val result = CarReviewBundleReconciler.reconcile(Vector(input))

      Then("all canonical records retain provider attribution and provider-local identities")
      result.toOption.map(_.evidence.map(_.id.value)) shouldBe Some(Vector("cozy:evidence-cml-model", "cozy:evidence-project-yaml"))
      result.toOption.map(_.observations.map(_.id.value)) shouldBe Some(Vector("cozy:observation-component-identity", "cozy:observation-runtime-unknown"))
      result.toOption.flatMap(_.observations.find(_.`type`.value == "assurance")).map(_.evidenceIds) shouldBe Some(Vector(ReviewEvidenceId("cozy:evidence-project-yaml"), ReviewEvidenceId("cozy:evidence-cml-model")))
      result.toOption.map(_.conflicts) shouldBe Some(Vector.empty)
    }

    "ignore a duplicate admitted bundle instead of reconciling it twice" in {
      Given("the same admitted bundle repeated by an orchestration retry")
      val input = AdmittedProviderBundleInput(_admitted, _bundle)

      When("CBD reconciles both submitted references")
      val result = CarReviewBundleReconciler.reconcile(Vector(input, input))

      Then("the report-local Evidence and Observations appear exactly once")
      result.toOption.map(_.evidence.size) shouldBe Some(2)
      result.toOption.map(_.observations.size) shouldBe Some(2)
    }

    "refuse mismatched bundle identity rather than merge another provider's evidence" in {
      Given("a typed admission paired with bundle text for a different provider version")
      val mismatched = _bundle.replace("\"version\": \"0.1.14\"", "\"version\": \"0.1.15\"")

      When("CBD reconciles the mismatched pair")
      val result = CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(_admitted, mismatched)))

      Then("the result is refused before any report record can be created")
      result.left.toOption.map(_.code) shouldBe Some("provider-identity-mismatch")
    }

    "keep competing provider observations visible without selecting an implicit winner" in {
      Given("one bundle with two observations for the same rule and subject")
      val conflicting = _bundle.replace("cozy.car.runtime-evidence", "cozy.car.identity-consistency")

      When("CBD reconciles the provider observations")
      val result = CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(_admitted, conflicting)))

      Then("both observations survive and the conflict is explicit instead of resolved by precedence")
      result.toOption.map(_.observations.size) shouldBe Some(2)
      result.toOption.map(_.conflicts.map(_.observationIds.map(_.value))) shouldBe Some(Vector(Vector("cozy:observation-component-identity", "cozy:observation-runtime-unknown")))
    }

    "turn an unsupported provider assurance without Evidence into Unknown" in {
      Given("an assurance whose provider-local evidence references are empty")
      val unsupported = _bundle.replace("\"evidenceIds\": [\n        \"evidence-project-yaml\",\n        \"evidence-cml-model\"\n      ]", "\"evidenceIds\": []")

      When("CBD reconciles the provider candidate")
      val result = CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(_admitted, unsupported)))

      Then("no canonical Assurance is fabricated and the limitation remains attributable")
      result.toOption.flatMap(_.observations.find(_.id.value == "cozy:observation-component-identity")).map(_.`type`.value) shouldBe Some("unknown")
      result.toOption.map(_.limitations.map(_.code).contains("provider-assurance-without-evidence")) shouldBe Some(true)
    }

    "project provider-only limitation scopes into the canonical report vocabulary" in {
      Given("an admitted provider bundle with a capability-scoped limitation")
      val input = AdmittedProviderBundleInput(_admitted, _bundle)

      When("CBD reconciles the provider limitation into its report")
      val result = CarReviewBundleReconciler.reconcile(Vector(input))

      Then("the report retains the limitation content with provider attribution")
      result.toOption.flatMap(_.limitations.find(_.code == "runtime-evidence-not-supported")).map(_.scope.value) shouldBe Some("provider")
    }

    "retain an admitted Observation quality mapping for named AI views" in {
      Given("an admitted provider bundle whose assurance maps to MCP operability")
      val mapped = _bundle.replace(
        "\"type\": \"assurance\"",
        "\"mappings\": {\"cncfFeatures\": [], \"implementationSubjects\": [\"component:textus-user-account\"], \"qualityCapabilities\": [\"quality.ai.operability.mcp\"]}, \"type\": \"assurance\""
      )

      When("CBD reconciles the already-admitted immutable bundle")
      val result = CarReviewBundleReconciler.reconcile(Vector(AdmittedProviderBundleInput(_admitted, mapped)))

      Then("the canonical Observation remains addressable from the AI-operability MCP view")
      result.toOption.flatMap(_.observations.find(_.id.value == "cozy:observation-component-identity")).map(_.mappings) shouldBe Some(
        ReviewMappings(Vector.empty, Vector("component:textus-user-account"), Vector(ReviewCapabilityId("quality.ai.operability.mcp")))
      )
    }
  }

  private val _bundle = Files.readString(Path.of("docs", "spec", "examples", "car-review-evidence-bundle-v1.json"))
  private val _admitted = AdmittedProviderBundle(
    ReviewProviderIdentity(ReviewProviderId("cozy"), ReviewVersion("0.1.14")),
    ReviewRuleIdentity(ReviewRuleId("cozy.car-review"), ReviewVersion("1.0.0")),
    ReviewDigest("sha256:d88fe085924cc9d234d233963bb624e584826bb8f02f19fc47253888a7c21d97"),
    ReviewDigest("sha256:6feabc3d18f6b5dd9c62d9dcbd478b22aacd46fa50a0c8415950ce26b39ba875"),
    Vector("evidence-project-yaml", "evidence-cml-model"),
    Vector("observation-component-identity", "observation-runtime-unknown"),
    Vector.empty
  )
}
