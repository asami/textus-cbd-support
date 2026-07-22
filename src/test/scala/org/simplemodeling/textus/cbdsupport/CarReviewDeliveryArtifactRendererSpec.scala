package org.simplemodeling.textus.cbdsupport

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.cbdsupport.runtime.*

/*
 * @since   Jul. 23, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewDeliveryArtifactRendererSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The CAR Review delivery artifact renderer" should {
    "render byte-identical Markdown and tagged PDF from one canonical delivery document" in {
      Given("one delivery-safe canonical Report document with all dashboard sections")
      val report = _report
      val document = CarReviewDeliveryProjection.project(report)

      When("Markdown and PDF are rendered twice without any provider or repository work")
      val first = CarReviewDeliveryArtifactRenderer.render(document)
      val second = CarReviewDeliveryArtifactRenderer.render(document)
      val pdf = new String(first.pdf, StandardCharsets.ISO_8859_1)
      val artifact = Path.of("target", "test-artifacts", "car-review-delivery.pdf")
      Files.createDirectories(artifact.getParent)
      Files.write(artifact, first.pdf)

      Then("the artifacts retain the common identity, order, readable PDF structure, and deterministic bytes")
      first.markdown shouldBe second.markdown
      first.pdf.sameElements(second.pdf) shouldBe true
      first.markdown should include(s"${report.reportId.value}")
      first.markdown should include(s"${report.reportDigest.value}")
      first.markdown should include("| Field | Value |")
      first.markdown should include("report-unknown-runtime")
      first.markdown.indexOf("## Report identity") should be < first.markdown.indexOf("## Gate")
      first.markdown.indexOf("## Gate") should be < first.markdown.indexOf("## Dashboard")
      first.markdown.indexOf("## Dashboard") should be < first.markdown.indexOf("## Baseline")
      first.markdown.indexOf("## Baseline") should be < first.markdown.indexOf("## Capabilities")
      first.markdown.indexOf("## Capabilities") should be < first.markdown.indexOf("## Observations")
      first.markdown.indexOf("## Observations") should be < first.markdown.indexOf("## Limitations")
      first.markdown.indexOf("## Limitations") should be < first.markdown.indexOf("## Redaction and omissions")
      pdf should startWith("%PDF-1.7")
      pdf should include("/Marked true")
      pdf should include("/StructTreeRoot")
      pdf should include("/Pg ")
      pdf should include("/S /Table")
      pdf should include("/S /TR")
      pdf should include("/S /TH /P ")
      pdf should include("/S /TD /P ")
      pdf should include("/Title (CBD CAR Review)")
      pdf should include("/Lang (en-US)")
      pdf should include(report.reportId.value)
      pdf should include(report.gate.result.value)
      Files.readAllBytes(artifact).sameElements(first.pdf) shouldBe true
    }

    "make unsupported PDF characters and their limitation explicit without changing Markdown or a conclusion" in {
      Given("one delivery document with a printable character outside the embedded PDF font boundary")
      val document = CarReviewDeliveryProjection.project(_report).copy(
        limitations = Vector(CarReviewDeliveryLimitation("unicode", ReviewLimitationScope("report"), None, "東京都", false))
      )

      When("the self-contained PDF renderer cannot encode that character")
      val artifacts = CarReviewDeliveryArtifactRenderer.render(document)
      val pdf = new String(artifacts.pdf, StandardCharsets.ISO_8859_1)

      Then("Markdown preserves the delivery-safe text while PDF records a visible omission and stable limitation")
      artifacts.markdown should include("東京都")
      artifacts.limitations should contain("pdf.unsupported-character")
      pdf should include("[omitted: U+")
      pdf should include("PDF renderer limitation: pdf.unsupported-character")
      artifacts.markdown should include(s"${_report.gate.result.value}")
    }
  }

  private val _report = CarReviewReportCodec.decode(Files.readString(Path.of("docs", "spec", "examples", "car-review-report-v1.json"))).fold(_fail, identity)
  private def _fail(error: CarReviewCodecFailure): Nothing = fail(s"${error.code}: ${error.message}")
}
