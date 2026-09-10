package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class CompositionDashboardCrossSurfaceNavigationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "CompositionDashboardCrossSurfaceNavigation" should {
    "P9-63B pure caller-admitted Composition-to-Dashboard navigation" which {
      "retain distinct candidate and association evidence with only Dashboard-admitted usable targets" in {
        Given("one caller-admitted candidate, distinct association, and Dashboard with one gated and one usable target")
        val component = _component("textus-order")
        val candidate = _candidate(component)
        val association = _association(candidate, component)
        val gatedrecord = _record(
          "gated-record",
          component,
          "source-gated",
          "entity-gated",
          navigation = Some(_navigation("entity-gated", component, implementedandusable = false))
        )
        val usablerecord = _record(
          "usable-record",
          component,
          "source-usable",
          "entity-usable",
          navigation = Some(_navigation("entity-usable", component, implementedandusable = true))
        )
        val dashboardresult = ComponentDashboard.create(component, Vector(gatedrecord, usablerecord))

        When("the exact supplied values are linked")
        dashboardresult.isRight shouldBe true
        val result = CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)

        Then("the successful link preserves the separate evidence and suppresses only the Dashboard-gated target")
        result shouldBe CompositionDashboardNavigationLinkProjected(
          CompositionDashboardNavigationLink(candidate, association, dashboardresult.toOption.get)
        )
        result.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.usableNavigationRecords.map(_.sourceRecord.id) shouldBe
          Vector("usable-record")
        result.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.dashboard.concernGroups.flatMap(_.records).map(_.sourceRecord.id) shouldBe
          Vector("gated-record", "usable-record")
      }

      "admit zero Dashboard targets only when the association supplies an explicit zero-target condition" in {
        Given("a valid association and an existing Dashboard whose records expose no navigation target")
        val component = _component("textus-order")
        val candidate = _candidate(component)
        val record = _record("evidence-only", component, "source-evidence", "entity-evidence")
        val dashboardresult = ComponentDashboard.create(component, Vector(record))
        val noexplicitcondition = _association(candidate, component)
        val explicitcondition = _association(
          candidate,
          component,
          condition = _condition(explicitabsence = Some("no-usable-dashboard-target"))
        )

        When("the zero-target Dashboard is linked without and with an explicit association condition")
        dashboardresult.isRight shouldBe true
        val rejected = CompositionDashboardCrossSurfaceNavigation.create(candidate, noexplicitcondition, dashboardresult.toOption.get)
        val projected = CompositionDashboardCrossSurfaceNavigation.create(candidate, explicitcondition, dashboardresult.toOption.get)

        Then("the factory returns an all-or-nothing typed rejection or one link without proxy targets")
        rejected.isInstanceOf[CompositionDashboardNavigationLinkRejected] shouldBe true
        rejected.asInstanceOf[CompositionDashboardNavigationLinkRejected].failure.violations should contain(
          "Composition Dashboard association with zero usable Dashboard targets requires an explicit zero-target condition."
        )
        projected.isInstanceOf[CompositionDashboardNavigationLinkProjected] shouldBe true
        projected.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.usableNavigationRecords shouldBe Vector.empty
      }

      "return typed all-or-nothing failures for malformed mismatched duplicate foreign unavailable and unauthorized association input" in {
        Given("one valid candidate Dashboard and associations at each rejected input boundary")
        val component = _component("textus-order")
        val candidate = _candidate(component)
        val record = _record(
          "usable-record",
          component,
          "source-usable",
          "entity-usable",
          navigation = Some(_navigation("entity-usable", component, implementedandusable = true))
        )
        val dashboardresult = ComponentDashboard.create(component, Vector(record))
        val blank = _association(candidate, component).copy(locator = CompositionDashboardAssociationLocator(" "))
        val mismatched = _association(candidate, _component("textus-payment"))
        val duplicate = _association(candidate, component).copy(associationIdentity = CompositionDashboardAssociationIdentity(candidate.candidateIdentity.value))
        val foreign = _association(candidate, component).copy(candidateIdentity = CompositionDashboardCandidateIdentity("candidate-foreign"))
        val unavailable = _association(candidate, component, condition = _condition(availability = "unavailable"))
        val unauthorized = _association(candidate, component, condition = _condition(authorization = "denied"))

        When("each association is admitted against the same exact Dashboard")
        dashboardresult.isRight shouldBe true
        val results = Vector(blank, mismatched, duplicate, foreign, unavailable, unauthorized).map { association =>
          CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)
        }

        Then("each input produces only the typed rejection and no partial link")
        results.forall(_.isInstanceOf[CompositionDashboardNavigationLinkRejected]) shouldBe true
        results.flatMap {
          case CompositionDashboardNavigationLinkRejected(failure) => failure.violations
          case _ => Vector.empty
        } should contain allOf (
          "Composition Dashboard association locator must not be blank.",
          "Composition Dashboard association Component identity must equal the exact candidate Component identity.",
          "Composition Dashboard candidate and association identities must not be duplicated.",
          "Composition Dashboard association must retain the exact caller-supplied candidate identity.",
          "Composition Dashboard association availability must be available.",
          "Composition Dashboard association authorization must be admitted."
        )
      }

      "never reconstruct an identity or navigation target from matching-looking caller text" in {
        Given("a candidate identity and locator that look like another Component while exact supplied identities differ")
        val component = _component("textus-order")
        val candidate = _candidate(component).copy(
          candidateIdentity = CompositionDashboardCandidateIdentity("candidate-for-textus-payment"),
          attribution = _attribution("candidate-source", "catalog:textus-payment")
        )
        val association = _association(candidate, _component("textus-payment"))
        val dashboardresult = ComponentDashboard.create(
          component,
          Vector(
            _record(
              "usable-record",
              component,
              "source-usable",
              "entity-usable",
              navigation = Some(_navigation("entity-usable", component, implementedandusable = true))
            )
          )
        )

        When("the factory receives only those exact caller-supplied values")
        dashboardresult.isRight shouldBe true
        val result = CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)

        Then("it rejects the foreign exact Component instead of inferring a replacement identity or target")
        result shouldBe a[CompositionDashboardNavigationLinkRejected]
        result.asInstanceOf[CompositionDashboardNavigationLinkRejected].failure.violations should contain(
          "Composition Dashboard association Component identity must equal the exact candidate Component identity."
        )
      }

      "preserve existing Dashboard navigation order under generated inventory permutations without ranking or selection" in {
        Given("three exact usable Dashboard records whose semantic target identities have a stable order")
        val component = _component("textus-order")
        val candidate = _candidate(component)
        val association = _association(candidate, component)
        val records = Vector(
          _record("record-zulu", component, "source-zulu", "target-zulu", navigation = Some(_navigation("target-zulu", component, implementedandusable = true))),
          _record("record-alpha", component, "source-alpha", "target-alpha", navigation = Some(_navigation("target-alpha", component, implementedandusable = true))),
          _record("record-middle", component, "source-middle", "target-middle", navigation = Some(_navigation("target-middle", component, implementedandusable = true)))
        )

        When("ScalaCheck supplies generated permutations to the owning Dashboard projector and the link factory")
        forAll(Gen.pick(records.size, records)) { permutation =>
          val dashboardresult = ComponentDashboard.create(component, permutation.toVector)
          dashboardresult.isRight shouldBe true
          val result = CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)

          Then("the link retains Dashboard semantic-target-first order and remains a caller-admitted navigation record")
          result shouldBe a[CompositionDashboardNavigationLinkProjected]
          result.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.usableNavigationRecords.map(_.navigationTarget.map(_.targetIdentity.value)) shouldBe
            Vector(Some("target-alpha"), Some("target-middle"), Some("target-zulu"))
          result.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.candidate shouldBe candidate
          result.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.association shouldBe association
        }
      }

      "perform no selection fact persistence lifecycle or external action beyond returning immutable values" in {
        Given("one valid caller-admitted candidate association and existing Dashboard")
        val component = _component("textus-order")
        val candidate = _candidate(component)
        val association = _association(candidate, component)
        val dashboardresult = ComponentDashboard.create(
          component,
          Vector(
            _record(
              "usable-record",
              component,
              "source-usable",
              "entity-usable",
              navigation = Some(_navigation("entity-usable", component, implementedandusable = true))
            )
          )
        )

        When("the pure factory is called repeatedly")
        dashboardresult.isRight shouldBe true
        val first = CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)
        val second = CompositionDashboardCrossSurfaceNavigation.create(candidate, association, dashboardresult.toOption.get)

        Then("it returns the same navigation-only admitted value without selection or mutation")
        first shouldBe second
        first shouldBe a[CompositionDashboardNavigationLinkProjected]
        first.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.candidate shouldBe candidate
        first.asInstanceOf[CompositionDashboardNavigationLinkProjected].link.dashboard shouldBe dashboardresult.toOption.get
      }
    }
  }

  private def _component(componentid: String): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _provenance(sourceid: String, locator: String): CompositionProvenance =
    CompositionProvenance(sourceid, s"$sourceid-authority", locator)

  private def _attribution(sourceid: String, locator: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, s"$sourceid-authority", locator)

  private def _condition(
    availability: String = "available",
    authorization: String = "admitted",
    explicitabsence: Option[String] = None
  ): ComponentDashboardCondition =
    ComponentDashboardCondition(
      availability,
      authorization,
      None,
      explicitabsence,
      None,
      None,
      None,
      None,
      Vector.empty
    )

  private def _candidate(component: ComponentDashboardComponentIdentity): CompositionDashboardNavigationCandidate = {
    val intent = ApplicationIntent("intent-order", "application-order", _provenance("intent-source", "intent:order"))
    val capability = RequiredCapability("capability-order", intent.id, _provenance("capability-source", "capability:order"))
    CompositionDashboardNavigationCandidate(
      CompositionDashboardCandidateIdentity("candidate-order"),
      intent,
      capability,
      component,
      _provenance("candidate-source", "candidate:order"),
      _attribution("candidate-source", "candidate:order"),
      _condition(),
      Vector("candidate-evidence-boundary"),
      Some("candidate-order")
    )
  }

  private def _association(
    candidate: CompositionDashboardNavigationCandidate,
    component: ComponentDashboardComponentIdentity,
    condition: ComponentDashboardCondition = _condition()
  ): CompositionDashboardNavigationAssociation =
    CompositionDashboardNavigationAssociation(
      CompositionDashboardAssociationIdentity("association-order"),
      candidate.candidateIdentity,
      component,
      _attribution("association-source", "association:order"),
      CompositionDashboardAssociationLocator("association:order"),
      condition,
      Vector("association-evidence-boundary")
    )

  private def _navigation(
    targetid: String,
    component: ComponentDashboardComponentIdentity,
    implementedandusable: Boolean
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      _attribution(s"target-source-$targetid", s"target:$targetid"),
      _condition(),
      implementedandusable
    )

  private def _record(
    recordid: String,
    component: ComponentDashboardComponentIdentity,
    sourceid: String,
    targetid: String,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): ComponentDashboardInventoryRecord =
    ComponentDashboardInventoryRecord(
      recordid,
      component,
      sourceid,
      _attribution(sourceid, s"source:$sourceid"),
      s"admitted statement for $recordid",
      _condition(),
      Some(Content),
      Some(ComponentDashboardSemanticTargetIdentity(targetid)),
      None,
      navigation
    )
}
