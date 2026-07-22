package org.simplemodeling.textus.cbdsupport

import java.nio.file.{Files, Path}

import org.goldenport.cncf.http.WebDescriptor
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewWebDeliveryApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The private CAR Review Web delivery application" should {
    "return an authorized exact-Report dashboard and Observation/capability diagnoses without provider work" in {
      Given("one retained canonical Report and the private Web delivery boundary")
      val repository = new CarReviewRepository()
      repository.retain(_report) shouldBe Right(_report)
      val application = new CarReviewWebDeliveryApplication(repository)

      When("an authorized viewer requests the dashboard and exact Finding, Assurance, Unknown, and capability diagnoses")
      val dashboard = application.dashboard(_report.reportId, Set("viewer")).fold(_fail_conclusion, identity)
      val finding = application.diagnosis(_report.reportId, CarReviewWebDiagnosisKind.OBSERVATION, "report-finding-missing-rationale", Set("viewer")).fold(_fail_conclusion, identity)
      val assurance = application.diagnosis(_report.reportId, CarReviewWebDiagnosisKind.OBSERVATION, "report-assurance-component-identity", Set("viewer")).fold(_fail_conclusion, identity)
      val unknown = application.diagnosis(_report.reportId, CarReviewWebDiagnosisKind.OBSERVATION, "report-unknown-runtime", Set("viewer")).fold(_fail_conclusion, identity)
      val capability = application.diagnosis(_report.reportId, CarReviewWebDiagnosisKind.CAPABILITY, "quality.domain.identity-consistency", Set("viewer")).fold(_fail_conclusion, identity)

      Then("the Web model keeps canonical identity, gate, baseline, limitations, all Observation kinds, and bounded navigation guidance")
      dashboard.dashboard.reportId shouldBe _report.reportId
      dashboard.dashboard.reportDigest shouldBe _report.reportDigest
      dashboard.dashboard.gate.result shouldBe _report.gate.result
      dashboard.dashboard.unknownCount shouldBe _report.observations.count(_.`type`.value == "unknown")
      dashboard.dashboard.baseline.map(_.reportId) shouldBe _report.baseline.map(_.reportId)
      dashboard.limitations should not be empty
      finding.diagnosis.locations should contain("project.yaml")
      finding.nextActions should not be empty
      assurance.diagnosis.rule.map(_.id.value) shouldBe Some("cozy.car.identity-consistency")
      assurance.diagnosis.disposition.map(_.state.value) shouldBe Some("active")
      unknown.diagnosis.rule.map(_.id.value) shouldBe Some("cozy.car.runtime-evidence")
      unknown.diagnosis.disposition.map(_.state.value) shouldBe Some("deferred")
      unknown.nextActions.mkString(" ") should include("missing admitted evidence")
      capability.diagnosis.locations should contain("project.yaml")
      capability.nextActions should not be empty
    }

    "deny unauthorized, missing, unsupported, and cross-Report diagnosis requests without a fallback" in {
      Given("one retained canonical Report and the private Web delivery boundary")
      val repository = new CarReviewRepository()
      repository.retain(_report) shouldBe Right(_report)
      val application = new CarReviewWebDeliveryApplication(repository)

      When("a caller requests a dashboard or diagnosis outside exact authorized report scope")
      val denied = application.dashboard(_report.reportId, Set.empty)
      val missing = application.dashboard(ReviewReportId("report-missing"), Set("viewer"))
      val unsupported = application.diagnosis(_report.reportId, "history", "all", Set("viewer"))
      val absent = application.diagnosis(_report.reportId, CarReviewWebDiagnosisKind.OBSERVATION, "observation-missing", Set("viewer"))

      Then("the Web boundary does not enumerate history, select another Report, or fabricate a diagnosis")
      denied.isFaillure shouldBe true
      missing.isFaillure shouldBe true
      unsupported.isFaillure shouldBe true
      absent.isFaillure shouldBe true
    }

    "publish authenticated static forms with bounded diagnosis choices through the private Review service instead of the MCP-ready retrieval service" in {
      Given("the generated CML contract, static form configuration, and component MCP boundary")
      val cml = Files.readString(Path.of("src", "main", "cozy", "textus-cbd-support.cml"))
      val form = Files.readString(Path.of("src", "main", "web-inf", "form.yaml"))
      val descriptor = WebDescriptor.load(Path.of("src", "main", "web-inf", "form.yaml")).fold(_fail_conclusion, identity)
      val component = new impl.ComponentFactory()._create_uninitialized_component()

      Then("dashboard and diagnosis are Web-visible with supported choices but remain private to MCP")
      cml should include("#### getReviewDashboard")
      cml should include("#### getReviewDiagnosis")
      form should include("textus-cbd-support.cbd-review-admin.get-review-dashboard")
      form should include("textus-cbd-support.cbd-review-admin.get-review-diagnosis")
      form.substring(form.indexOf("get-review-dashboard"), form.indexOf("admin:")) should include("access: authenticated")
      descriptor.form("textus-cbd-support.cbd-review-admin.get-review-diagnosis").controls("itemKind").controlType shouldBe Some("select")
      descriptor.form("textus-cbd-support.cbd-review-admin.get-review-diagnosis").controls("itemKind").values shouldBe Vector("observation", "capability")
      component.isMcpReady("CbdReviewAdmin", "getReviewDashboard") shouldBe false
      component.isMcpReady("CbdReviewAdmin", "getReviewDiagnosis") shouldBe false
    }
  }

  private val _report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
  private def _fail_conclusion(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
}
