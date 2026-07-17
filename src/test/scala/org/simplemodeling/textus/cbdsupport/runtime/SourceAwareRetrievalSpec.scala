package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{Clock, Instant, ZoneOffset}
import scala.jdk.CollectionConverters.*

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final class SourceAwareRetrievalSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Source-aware read-only retrieval" should {
    "preserve catalog and working observations while reporting an unresolved version conflict" in {
      Given("one published profile and one working-directory observation for another version of the same component")
      val runtime = _runtime("reconciliation")

      When("the federated inputs are initialized and the identity is searched without a source winner filter")
      runtime.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val result = runtime.searchSourceAware(_query())

      Then("both sources and their diagnostics remain visible with no selected observation")
      result.matches.map(_.profile.catalogId) shouldBe Vector("catalog")
      result.report.observations.map(observation => observation.sourceId -> observation.version) shouldBe Vector(
        "catalog" -> Some("1.0.0"),
        "working" -> Some("1.1.0-SNAPSHOT")
      )
      result.report.issues.map(_.code) should contain(ReconciliationIssueCode.VERSION_CONFLICT)
      result.report.precedence.head.sourceKinds should contain(InformationSourceKind.PUBLISHED_CATALOG)
      result.report.selectedObservation shouldBe None

      And("only explicitly admitted catalog and development-tree state is projected read-only")
      runtime.informationSourceStates(includeDisabled = false).map(_.descriptor.id) shouldBe Vector(
        "catalog",
        "working"
      )
    }

    "apply source, freshness, availability, version, and conflict filters without fabricating a profile" in {
      Given("the same published and working evidence initialized through the federated runtime")
      val runtime = _runtime("filters")

      When("the inputs are initialized and search is constrained to the working snapshot observation")
      runtime.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val working = runtime.searchSourceAware(_query(
        version = Some("1.1.0-SNAPSHOT"),
        sourceid = Some("working"),
        sourcekind = Some(InformationSourceKind.DEVELOPMENT_DIRECTORY),
        freshness = Some("observed"),
        versionstate = Some(VersionAvailabilityState.WORKING)
      ))

      Then("the local observation is returned separately and no catalog component profile is synthesized")
      working.matches shouldBe empty
      working.report.observations.map(_.sourceId) shouldBe Vector("working")
      working.report.observations.flatMap(_.versionState) shouldBe Vector(VersionAvailabilityState.WORKING)

      When("search is instead constrained to the version-conflict diagnostic")
      val conflicting = runtime.searchSourceAware(_query(
        conflictcode = Some(ReconciliationIssueCode.VERSION_CONFLICT)
      ))

      Then("only conflict-participating alternatives and the requested issue class remain")
      conflicting.report.observations.map(_.sourceId) shouldBe Vector("catalog", "working")
      conflicting.report.issues.map(_.code) shouldBe Vector(ReconciliationIssueCode.VERSION_CONFLICT)
      conflicting.report.selectedObservation shouldBe None

      When("the same conflict query requests only one returned observation")
      val limited = runtime.searchSourceAware(_query(
        conflictcode = Some(ReconciliationIssueCode.VERSION_CONFLICT),
        limit = 1
      ))

      Then("the complete conflict diagnostic remains visible outside the response observation limit")
      limited.report.observations should have size 1
      limited.report.issues.map(_.code) shouldBe Vector(ReconciliationIssueCode.VERSION_CONFLICT)
      limited.report.issues.flatMap(_.sourceIds).distinct.sorted shouldBe Vector("catalog", "working")
    }

    "exclude a same-source same-location component that does not participate in the requested conflict" in {
      Given("two catalog components sharing one index location and working evidence conflicting with only one component")
      val runtime = _runtime("exact-conflict-participants", Vector(_profile, _unrelated_profile))

      When("the inputs are initialized and version-conflict participants are requested")
      runtime.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val conflicting = runtime.searchSourceAware(_query(
        conflictcode = Some(ReconciliationIssueCode.VERSION_CONFLICT)
      ))

      Then("only observations belonging to the conflicting component remain")
      conflicting.report.observations.flatMap(_.componentName).distinct shouldBe Vector("textus-order")
      conflicting.matches.map(_.profile.name) shouldBe Vector("textus-order")
    }

    "diagnose unsupported read-only filters without expanding authority" in {
      Given("an initialized source-aware runtime")
      val runtime = _runtime("invalid-filter")

      When("the inputs are initialized and an unknown conflict filter is requested")
      runtime.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val result = runtime.searchSourceAware(_query(conflictcode = Some("winner")))

      Then("the result is empty and the unsupported filter is explicit")
      result.matches shouldBe empty
      result.report.observations shouldBe empty
      result.report.issues shouldBe empty
      result.warnings should contain("Unsupported conflictCode filter: winner.")
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)

  private def _runtime(name: String, profiles: Vector[ComponentProfile] = Vector(_profile)): CbdRuntime = {
    val working = InformationSourceDescriptor(
      "working",
      InformationSourceKind.DEVELOPMENT_DIRECTORY,
      "resource-tree:working",
      300,
      true,
      InformationSourceAuthorization.EXPLICIT
    )
    val inventory = LocalInformationInventory(
      Vector(working),
      Vector(LocalComponentObservation(
        "working", InformationSourceKind.DEVELOPMENT_DIRECTORY, Some("textus-order"),
        Some("org.textus"), Some("car"), Some("1.1.0-SNAPSHOT"), "project-yaml",
        VersionAvailabilityState.WORKING, "resource-tree:working/project.yaml",
        None, None, None, Vector.empty
      )),
      Vector.empty,
      _clock.instant(),
      Map("working" -> Vector.empty)
    )
    val catalog = CatalogSource("catalog", URI.create("https://catalog.example/"), 100, true)
    CbdRuntime.createFederated(
      Vector(catalog),
      new InMemoryComponentCatalogProvider(profiles, clock = _clock),
      CatalogCachePolicy.DEFAULT,
      _clock,
      Vector.empty,
      new BokKnowledgeSourceProvider(_clock),
      siebokprovider = new SieBokProvider(_clock),
      admittedlocalinventory = Some(inventory)
    )
  }

  private def _query(
    version: Option[String] = None,
    sourceid: Option[String] = None,
    sourcekind: Option[String] = None,
    freshness: Option[String] = None,
    versionstate: Option[String] = None,
    conflictcode: Option[String] = None,
    limit: Int = 20
  ): SourceAwareComponentSearchQuery =
    SourceAwareComponentSearchQuery(
      requirement = "textus-order",
      organization = Some("org.textus"),
      componentKind = Some("car"),
      version = version,
      runtimeVersion = None,
      sourceId = sourceid,
      sourceKind = sourcekind,
      freshness = freshness,
      versionState = versionstate,
      conflictCode = conflictcode,
      purpose = Some(ReconciliationPurpose.PUBLISHED_REUSE),
      limit = limit
    )

  private def _profile: ComponentProfile =
    ComponentProfile(
      catalogId = "catalog",
      organization = Some("org.textus"),
      name = "textus-order",
      title = "Textus Order",
      summary = Some("Order component."),
      kind = "car",
      versions = Vector("1.0.0"),
      selectedVersion = Some("1.0.0"),
      dependencyMetadataVersion = None,
      latestStable = Some("1.0.0"),
      latestSnapshot = None,
      runtimeMinimum = Some("0.5.1"),
      tags = Vector("order"),
      terms = Vector.empty,
      dependencies = Vector.empty,
      artifactUri = None,
      evidenceUri = URI.create("https://catalog.example/metadata/repository/car/index.json"),
      modelMetadataUri = None,
      documentationUri = None,
      versionEvidence = Vector(ComponentVersionEvidence("1.0.0", Some("0.5.1"), Vector.empty, None, None, false)),
      warnings = Vector.empty
    )

  private def _unrelated_profile: ComponentProfile =
    _profile.copy(
      name = "textus-order-helper",
      title = "Textus Order Helper",
      tags = Vector("textus-order")
    )

  private def _reset_work_area(name: String): Path = {
    val root = Path.of("target", "test-work", "source-aware-retrieval", name).toAbsolutePath.normalize()
    if (Files.exists(root)) {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
      finally stream.close()
    }
    Files.createDirectories(root)
  }

  private object EmptyFederatedFetcher extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected catalog fetch: $uri")

    override def get(uri: URI, maxbytes: Int): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected bounded fetch: $uri")
  }
}
