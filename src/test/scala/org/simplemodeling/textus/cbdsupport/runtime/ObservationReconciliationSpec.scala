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
final class ObservationReconciliationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ReconciliationObservation" should {
    "normalize catalog and local evidence without copying fields between sources" in {
      Given("one catalog profile observation and one independent cached CAR observation")
      val profile = _profile.copy(runtimeMinimum = Some("0.5.0"), runtimeMaximum = Some("0.7.0"))
      val catalog = ComponentObservation(
        "public",
        InformationSourceKind.PUBLISHED_CATALOG,
        "https://catalog.example/order",
        Some("1.0.0"),
        "fresh",
        Some(Instant.parse("2026-07-14T06:00:00Z")),
        Some(Instant.parse("2026-07-14T06:15:00Z")),
        Some("catalog-sha"),
        Vector("catalog-only")
      )
      val local = LocalComponentObservation(
        "cache-car",
        InformationSourceKind.CAR_STORAGE,
        Some("textus-order"),
        None,
        Some("car"),
        Some("1.0.0"),
        "component-descriptor",
        "cached",
        "file:///cache/textus-order/1.0.0/order.car",
        Some("1.0.0"),
        Some("1.0.0"),
        Some("cache-sha"),
        Vector("cache-only")
      )

      When("both source-specific shapes enter reconciliation")
      val observations = Vector(
        ReconciliationObservation.fromCatalog(profile, catalog),
        ReconciliationObservation.fromLocal(local)
      )

      Then("each observation retains only its own authority, state, checksum, and diagnostics")
      observations.head.versionState shouldBe Some("remotely-published")
      observations.head.runtimeMinimum shouldBe Some("0.5.0")
      observations.head.artifactChecksumSha256 shouldBe Some("catalog-sha")
      observations.head.diagnostics shouldBe Vector("catalog-only")
      observations.last.organization shouldBe None
      observations.last.versionState shouldBe Some("cached")
      observations.last.runtimeMinimum shouldBe None
      observations.last.artifactChecksumSha256 shouldBe Some("cache-sha")
      observations.last.diagnostics shouldBe Vector("cache-only")
    }
  }

  "ObservationReconciler" should {
    "report duplicate, missing, stale, incompatible, version, and checksum evidence without a winner" in {
      Given("overlapping remote and local observations containing every conflict class")
      val observations = Vector(
        _observation(
          "public",
          InformationSourceKind.PUBLISHED_CATALOG,
          Some("1.0.0"),
          Some("aaa"),
          "stale",
          Some("0.6.0"),
          "https://catalog.example/order"
        ),
        _observation(
          "local-car",
          InformationSourceKind.CAR_STORAGE,
          Some("1.0.0"),
          Some("bbb"),
          "observed",
          None,
          "file:///local/order-1.0.0.car"
        ),
        _observation(
          "cache-car",
          InformationSourceKind.CAR_STORAGE,
          Some("2.0.0"),
          Some("ccc"),
          "observed",
          None,
          "file:///cache/order-2.0.0.car"
        ),
        _observation(
          "incomplete",
          InformationSourceKind.CAR_STORAGE,
          None,
          None,
          "observed",
          None,
          "file:///cache/unknown.car"
        ).copy(componentName = None)
      )

      When("artifact evidence is reconciled for a concrete version and runtime")
      val report = ObservationReconciler.reconcile(
        observations,
        ReconciliationPurpose.ARTIFACT_VERIFICATION,
        requestedversion = Some("1.0.0"),
        runtimeversion = Some("0.5.1")
      )

      Then("all six issue classes and every original observation remain visible")
      report.issues.map(_.code).toSet shouldBe Set(
        ReconciliationIssueCode.DUPLICATE,
        ReconciliationIssueCode.MISSING,
        ReconciliationIssueCode.STALE,
        ReconciliationIssueCode.INCOMPATIBLE,
        ReconciliationIssueCode.VERSION_CONFLICT,
        ReconciliationIssueCode.CHECKSUM_CONFLICT
      )
      report.observations shouldBe observations
      report.selectedObservation shouldBe None
      report.issues.find(_.code == ReconciliationIssueCode.CHECKSUM_CONFLICT).get.sourceIds shouldBe Vector("local-car", "public")
      report.issues.find(_.code == ReconciliationIssueCode.VERSION_CONFLICT).get.message should include("1.0.0, 2.0.0")
    }

    "expose purpose-specific precedence as authority guidance without selecting an observation" in {
      Given("one working, locally published, cached, and remotely published observation")
      val observations = Vector(
        _observation("work", InformationSourceKind.DEVELOPMENT_DIRECTORY, Some("1.1.0-SNAPSHOT"), None, "observed", None, "file:///work/project.yaml").copy(versionState = Some("working")),
        _observation("local-car", InformationSourceKind.CAR_STORAGE, Some("1.0.0"), Some("aaa"), "observed", None, "file:///local/order.car").copy(versionState = Some("local-published")),
        _observation("cache-car", InformationSourceKind.CAR_STORAGE, Some("0.9.0"), Some("bbb"), "observed", None, "file:///cache/order.car").copy(versionState = Some("cached")),
        _observation("public", InformationSourceKind.PUBLISHED_CATALOG, Some("1.0.0"), Some("aaa"), "fresh", Some("0.5.0"), "https://catalog.example/order")
      )

      When("the same observations are reconciled for development and published reuse")
      val development = ObservationReconciler.reconcile(observations, ReconciliationPurpose.DEVELOPMENT_WORK)
      val published = ObservationReconciler.reconcile(observations, ReconciliationPurpose.PUBLISHED_REUSE)

      Then("the leading authority changes by purpose while both reports preserve all alternatives")
      development.precedence.head.sourceKinds shouldBe Vector(InformationSourceKind.DEVELOPMENT_DIRECTORY)
      published.precedence.head.sourceKinds shouldBe Vector(InformationSourceKind.PUBLISHED_CATALOG)
      development.observations shouldBe observations
      published.observations shouldBe observations
      development.selectedObservation shouldBe None
      published.selectedObservation shouldBe None
    }
  }

  private def _observation(
    sourceid: String,
    sourcekind: String,
    version: Option[String],
    checksum: Option[String],
    freshness: String,
    runtimeminimum: Option[String],
    evidence: String
  ): ReconciliationObservation =
    ReconciliationObservation(
      sourceid,
      sourcekind,
      Some("org.textus"),
      Some("textus-order"),
      Some("car"),
      version,
      Some(if (sourcekind == InformationSourceKind.PUBLISHED_CATALOG) "remotely-published" else "cached"),
      freshness,
      runtimeminimum,
      None,
      checksum,
      evidence,
      Vector.empty
    )

  private val _profile = ComponentProfile(
    "public",
    Some("org.textus"),
    "textus-order",
    "Textus Order",
    Some("Order component."),
    "car",
    Vector("1.0.0"),
    Some("1.0.0"),
    None,
    Some("1.0.0"),
    None,
    None,
    Vector.empty,
    Vector.empty,
    Vector.empty,
    Some(URI.create("https://catalog.example/order.car")),
    URI.create("https://catalog.example/order"),
    None,
    None,
    Vector.empty,
    Vector.empty
  )
}
