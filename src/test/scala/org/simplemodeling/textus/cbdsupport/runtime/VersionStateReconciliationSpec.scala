package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class VersionStateReconciliationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentVersionStateObservation" should {
    "preserve all availability states and every catalog release and snapshot identity" in {
      Given("a catalog with separate release and snapshot versions plus three local availability observations")
      val profile = _profile.copy(
        versions = Vector("1.0.0", "1.1.0-SNAPSHOT"),
        selectedVersion = Some("1.0.0"),
        latestStable = Some("1.0.0"),
        latestSnapshot = Some("1.1.0-SNAPSHOT"),
        versionEvidence = Vector(
          _version_evidence("1.0.0", "stable", "release-sha"),
          _version_evidence("1.1.0-SNAPSHOT", "snapshot", "snapshot-sha")
        )
      )
      val catalog = _catalog_observation(Some("1.0.0"))
      val local = Vector(
        _local("work", VersionAvailabilityState.WORKING, Some("1.1.0-SNAPSHOT")),
        _local("local-car", VersionAvailabilityState.LOCAL_PUBLISHED, Some("1.0.0")),
        _local("cache-car", VersionAvailabilityState.CACHED, Some("0.9.0"))
      )

      When("catalog and local evidence are normalized")
      val observations = ComponentVersionStateObservation.fromCatalog(profile, catalog) ++
        local.map(ComponentVersionStateObservation.fromLocal)

      Then("availability and maturity remain independent and source attributable")
      observations.map(_.availabilityState).toSet shouldBe VersionAvailabilityState.ALL.toSet
      observations.filter(_.sourceId == "public").map(x => x.version -> x.maturity) shouldBe Vector(
        Some("1.0.0") -> VersionMaturity.RELEASE,
        Some("1.1.0-SNAPSHOT") -> VersionMaturity.SNAPSHOT
      )
      observations.find(x => x.sourceId == "public" && x.version.contains("1.1.0-SNAPSHOT")).get.channel shouldBe Some("snapshot")
      observations.find(x => x.sourceId == "public" && x.version.contains("1.1.0-SNAPSHOT")).get.artifactChecksumSha256 shouldBe Some("snapshot-sha")
      observations.find(_.sourceId == "work").get.maturity shouldBe VersionMaturity.SNAPSHOT
      observations.find(_.sourceId == "local-car").get.maturity shouldBe VersionMaturity.RELEASE
    }

    "report conflicting and unknown maturity evidence without rewriting either observation" in {
      Given("one snapshot-named stable catalog version and one non-version working identity")
      val conflictingprofile = _profile.copy(
        versions = Vector("2.0.0-SNAPSHOT"),
        selectedVersion = Some("2.0.0-SNAPSHOT"),
        latestStable = Some("2.0.0-SNAPSHOT"),
        versionEvidence = Vector(_version_evidence("2.0.0-SNAPSHOT", "stable", "conflict-sha"))
      )
      val unknownlocal = _local("work", VersionAvailabilityState.WORKING, Some("main"))

      When("maturity signals are classified")
      val conflicting = ComponentVersionStateObservation.fromCatalog(
        conflictingprofile,
        _catalog_observation(Some("2.0.0-SNAPSHOT"))
      ).head
      val unknown = ComponentVersionStateObservation.fromLocal(unknownlocal)

      Then("the contradiction and absence are explicit diagnostics attached to their original sources")
      conflicting.maturity shouldBe VersionMaturity.CONFLICTING
      conflicting.version shouldBe Some("2.0.0-SNAPSHOT")
      conflicting.channel shouldBe Some("stable")
      conflicting.diagnostics.exists(_.contains("conflicts")) shouldBe true
      unknown.maturity shouldBe VersionMaturity.UNKNOWN
      unknown.version shouldBe Some("main")
      unknown.diagnostics.exists(_.contains("unknown")) shouldBe true
    }
  }

  "VersionStateReconciler" should {
    "partition maturity evidence while retaining alternatives and selecting no latest version" in {
      Given("release, snapshot, unknown, and conflicting observations with duplicate version identities")
      val observations = Vector(
        ComponentVersionStateObservation.fromLocal(_local("work", VersionAvailabilityState.WORKING, Some("1.1.0-SNAPSHOT"))),
        ComponentVersionStateObservation.fromLocal(_local("local-car", VersionAvailabilityState.LOCAL_PUBLISHED, Some("1.0.0"))),
        ComponentVersionStateObservation.fromLocal(_local("cache-car", VersionAvailabilityState.CACHED, Some("1.0.0"))),
        ComponentVersionStateObservation.fromLocal(_local("branch", VersionAvailabilityState.WORKING, Some("main")))
      )
      val conflictingprofile = _profile.copy(
        versions = Vector("2.0.0-SNAPSHOT"),
        selectedVersion = Some("2.0.0-SNAPSHOT"),
        latestStable = Some("2.0.0-SNAPSHOT"),
        versionEvidence = Vector(_version_evidence("2.0.0-SNAPSHOT", "stable", "conflict-sha"))
      )
      val allobservations = observations ++ ComponentVersionStateObservation.fromCatalog(
        conflictingprofile,
        _catalog_observation(Some("2.0.0-SNAPSHOT"))
      )

      When("version states are reconciled")
      val report = VersionStateReconciler.reconcile(allobservations)

      Then("every source survives in exactly one maturity partition and no implicit winner is exposed")
      report.observations shouldBe allobservations
      report.releaseObservations.map(_.sourceId) shouldBe Vector("local-car", "cache-car")
      report.snapshotObservations.map(_.sourceId) shouldBe Vector("work")
      report.unknownObservations.map(_.sourceId) shouldBe Vector("branch")
      report.conflictingObservations.map(_.sourceId) shouldBe Vector("public")
      report.selectedObservation shouldBe None
    }
  }

  private def _version_evidence(version: String, channel: String, checksum: String): ComponentVersionEvidence =
    ComponentVersionEvidence(
      version,
      None,
      Vector.empty,
      Some(URI.create(s"https://catalog.example/$version/order.car")),
      None,
      hasDependencyMetadata = false,
      channel = Some(channel),
      status = Some("active"),
      artifactChecksumSha256 = Some(checksum)
    )

  private def _catalog_observation(version: Option[String]): ComponentObservation =
    ComponentObservation(
      "public",
      InformationSourceKind.PUBLISHED_CATALOG,
      "https://catalog.example/order",
      version,
      "fresh",
      None,
      None,
      Some("selected-sha"),
      Vector("catalog-diagnostic")
    )

  private def _local(
    sourceid: String,
    availabilitystate: String,
    version: Option[String]
  ): LocalComponentObservation =
    LocalComponentObservation(
      sourceid,
      if (availabilitystate == VersionAvailabilityState.WORKING)
        InformationSourceKind.DEVELOPMENT_DIRECTORY
      else
        InformationSourceKind.CAR_STORAGE,
      Some("textus-order"),
      Some("org.textus"),
      Some("car"),
      version,
      "observed",
      availabilitystate,
      s"file:///$sourceid/order",
      version,
      version,
      Some(s"$sourceid-sha"),
      Vector(s"$sourceid-diagnostic")
    )

  private val _profile = ComponentProfile(
    "public",
    Some("org.textus"),
    "textus-order",
    "Textus Order",
    Some("Order component."),
    "car",
    Vector.empty,
    None,
    None,
    None,
    None,
    None,
    Vector.empty,
    Vector.empty,
    Vector.empty,
    None,
    URI.create("https://catalog.example/order"),
    None,
    None,
    Vector.empty,
    Vector.empty
  )
}
