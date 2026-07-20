package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant, ZoneOffset}
import org.goldenport.Consequence
import org.goldenport.cncf.resource.{ResourceTreeAccess, ResourceTreeEntry, ResourceTreeQuery, ResourceTreeReference}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Executable specification for bounded development descriptor discovery.
 *
 * @since   Jul. 20, 2026
 * @version Jul. 20, 2026
 * @author  ASAMI, Tomoharu
 */
final class LocalSourceRuntimeQuerySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "LocalInformationSourceInventory development query" should {
    "create one working observation per bounded project descriptor" in {
      Given("an admitted development tree with two nested project descriptors and unrelated content")
      val reference = _value(ResourceTreeReference.parseC("development"))
      val access = ResourceTreeAccess.inMemory(Map(reference -> Vector(
        _entry("alpha/project.yaml", "alpha-component"),
        _entry("beta/project.yaml", "beta-component"),
        _entry("beta/notes.txt", "not-a-descriptor")
      )))
      val query = _value(ResourceTreeQuery.exactLeafNameC(reference, "project.yaml"))

      When("CBD inspects the bounded query result as development evidence")
      val result = _value(access.query(query))
      val inventory = LocalInformationSourceInventory.inspectDevelopmentQuery(
        _source("working"),
        result,
        VersionAvailabilityState.WORKING,
        LocalInspectionPolicy.DEFAULT,
        _clock
      )

      Then("each matching descriptor becomes one working observation with a logical evidence location")
      inventory.observations.flatMap(_.componentName) shouldBe Vector("alpha-component", "beta-component")
      inventory.observations.map(_.evidenceLocation) shouldBe Vector(
        "resource-tree:development/alpha/project.yaml",
        "resource-tree:development/beta/project.yaml"
      )
      inventory.observations.map(_.versionState).distinct shouldBe Vector(VersionAvailabilityState.WORKING)
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC)

  private def _entry(path: String, name: String): org.goldenport.cncf.resource.ResourceTreeEntry =
    _value(ResourceTreeEntry.createC(
      path,
      s"""project:
         |  component:
         |    name: $name
         |    version: 1.0.0-SNAPSHOT
         |""".stripMargin.getBytes(StandardCharsets.UTF_8).toVector
    ))

  private def _source(id: String): InformationSourceDescriptor =
    InformationSourceDescriptor(id, InformationSourceKind.DEVELOPMENT_DIRECTORY, s"resource-tree:$id", 1, true, InformationSourceAuthorization.EXPLICIT)

  private def _value[A](consequence: Consequence[A]): A = consequence match {
    case Consequence.Success(value) => value
    case Consequence.Failure(conclusion) => fail(conclusion.display)
  }
}
