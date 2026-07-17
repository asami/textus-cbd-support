package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext as ScalaExecutionContext, Future}

import org.goldenport.Consequence
import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeLimits, ResourceTreeReference}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 18, 2026
 * @version Jul. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final class CbdRuntimeInvocationIsolationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CBD runtime invocation views" should {
    "retain independent local evidence when calls are sequentially interleaved" in {
      Given("one shared runtime and two inventories admitted from distinct resource-tree snapshots")
      val runtime = _runtime
      val alpha = runtime.invocation(_inventory("alpha-tree", "alpha-component"))
      val beta = runtime.invocation(_inventory("beta-tree", "beta-component").copy(
        warnings = Vector("beta inventory warning"),
        sourceDiagnostics = Map("beta-tree" -> Vector("beta inventory warning"))
      ))

      When("both invocation views initialize and the first is read again after the second")
      alpha.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val alphabefore = _local_names(alpha)
      beta.ensureInputsReady(EmptyFederatedFetcher).isSuccess shouldBe true
      val betanames = _local_names(beta)
      val alphaafter = _local_names(alpha)

      Then("each view retains only its own admitted source and search observation")
      alphabefore shouldBe Vector("alpha-component")
      alphaafter shouldBe alphabefore
      betanames shouldBe Vector("beta-component")
      _search_names(alpha, "alpha-component") shouldBe Vector("alpha-component")
      _search_names(alpha, "beta-component") shouldBe empty
      _search_names(beta, "beta-component") shouldBe Vector("beta-component")
      alpha.overallStatus shouldBe "ready"
      beta.overallStatus shouldBe "degraded"
      alpha.overallStatus shouldBe "ready"
    }

    "retain independent local evidence when calls initialize concurrently" in {
      Given("two invocation views over one shared runtime and independent admitted snapshots")
      given ScalaExecutionContext = ScalaExecutionContext.global
      val runtime = _runtime
      val alpha = runtime.invocation(_inventory("concurrent-alpha", "alpha-component"))
      val beta = runtime.invocation(_inventory("concurrent-beta", "beta-component"))

      When("the views initialize concurrently")
      val initialized = Await.result(Future.sequence(Vector(
        Future(alpha.ensureInputsReady(EmptyFederatedFetcher)),
        Future(beta.ensureInputsReady(EmptyFederatedFetcher))
      )), 10.seconds)

      Then("both complete from local evidence without cross-invocation replacement")
      initialized.forall(_.isSuccess) shouldBe true
      _local_names(alpha) shouldBe Vector("alpha-component")
      _local_names(beta) shouldBe Vector("beta-component")
      alpha.informationSourceStates(includeDisabled = false).map(_.descriptor.id) should contain("concurrent-alpha")
      alpha.informationSourceStates(includeDisabled = false).map(_.descriptor.id) should not contain "concurrent-beta"
      beta.informationSourceStates(includeDisabled = false).map(_.descriptor.id) should contain("concurrent-beta")
      beta.informationSourceStates(includeDisabled = false).map(_.descriptor.id) should not contain "concurrent-alpha"
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC)

  private def _runtime: CbdRuntime =
    CbdRuntime.create(
      Vector.empty,
      new InMemoryComponentCatalogProvider(Vector.empty, clock = _clock),
      _clock
    )

  private def _inventory(treename: String, componentname: String): LocalInformationInventory = {
    val reference = _value(ResourceTreeReference.parseC(treename))
    val entry = _value(ResourceTreeEntry.createC(
      "project.yaml",
      s"""project:
         |  name: $componentname
         |  component:
         |    name: $componentname
         |    version: 1.0.0-SNAPSHOT
         |  organization: org.example
         |  kind: car
         |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector
    ))
    val snapshot = _value(ResourceTreeAccess.inMemory(Map(reference -> Vector(entry)))
      .snapshot(reference, ResourceTreeLimits.default))
    val source = InformationSourceDescriptor(
      treename,
      InformationSourceKind.DEVELOPMENT_DIRECTORY,
      s"resource-tree:$treename",
      300,
      true,
      InformationSourceAuthorization.EXPLICIT
    )
    LocalInformationSourceInventory.inspectDevelopmentSnapshot(
      source,
      snapshot,
      VersionAvailabilityState.WORKING,
      LocalInspectionPolicy.DEFAULT,
      _clock
    )
  }

  private def _local_names(invocation: CbdRuntimeInvocation): Vector[String] =
    invocation._local_inventory_snapshot.toVector.flatMap(_.observations).flatMap(_.componentName).distinct.sorted

  private def _search_names(invocation: CbdRuntimeInvocation, name: String): Vector[String] =
    invocation.searchSourceAware(_query(name)).report.observations.flatMap(_.componentName).distinct.sorted

  private def _query(name: String): SourceAwareComponentSearchQuery =
    SourceAwareComponentSearchQuery(
      requirement = name,
      organization = Some("org.example"),
      componentKind = Some("car"),
      version = None,
      runtimeVersion = None,
      sourceId = None,
      sourceKind = Some(InformationSourceKind.DEVELOPMENT_DIRECTORY),
      freshness = Some("observed"),
      versionState = Some(VersionAvailabilityState.WORKING),
      conflictCode = None,
      purpose = Some(ReconciliationPurpose.DEVELOPMENT_WORK),
      limit = 20
    )

  private def _value[A](consequence: Consequence[A]): A = consequence match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }

  private object EmptyFederatedFetcher extends CatalogFetcher with BokFetcher {
    def get(uri: URI): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected catalog fetch: $uri")

    override def get(uri: URI, maxbytes: Int): Consequence[String] =
      Consequence.serviceUnavailable(s"Unexpected bounded fetch: $uri")
  }
}
