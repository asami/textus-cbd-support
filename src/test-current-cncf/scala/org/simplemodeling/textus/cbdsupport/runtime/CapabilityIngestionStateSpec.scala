package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalacheck.{Gen, Prop, Test}
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.Producer
import CapabilityIngestionModel.*
import CapabilityIngestionFixtures.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CapabilityIngestionStateSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _ingestion = CapabilityIngestion
  "Explicit immutable ingestion history" should {
    "C11S01 append accepted, original rejected and accepted attempts with explicit management references" in {
      Given("C11I-D08/09: acquired package, independently allocated local references and original unauthorized optional acquisition")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val firstref = snapshot(" É | first logical snapshot ", 11)
        val thirdref = snapshot("third:logical:snapshot", 23)
        val diagnostic = rejected(DiagnosticKind.Unauthorized)
        val first = attempt("attempt:first", selectedinput, firstref)
        val second = attempt("attempt:second", selectedinput, snapshot("rejected:snapshot", 17))
        val third = attempt("attempt:third", selectedinput, thirdref)
        When("the three attempts are ingested as successive pure state values")
        val accepted = right(_ingestion.ingest(State.empty, first))
        val rejectedtransition = right(_ingestion.transition(accepted.state, second, Left(diagnostic)))
        val later = right(_ingestion.ingest(rejectedtransition.state, third))
        val missingreference = _ingestion.ingest(later.state, third.copy(reference = null))
        Then("only complete acceptance changes current, rejection retains original selection/Diagnostic, and invalid management cannot record an attempt")
        accepted.outcome shouldBe Outcome.Accepted(firstref)
        rejectedtransition.outcome shouldBe Outcome.Rejected(diagnostic)
        rejectedtransition.state.current shouldBe Some(firstref)
        rejectedtransition.state.snapshots shouldBe accepted.state.snapshots
        rejectedtransition.state.snapshots.head should be theSameInstanceAs accepted.state.snapshots.head
        rejectedtransition.state.attempts.last.attempt should be theSameInstanceAs second
        rejectedtransition.state.attempts.last.outcome match {
          case Outcome.Rejected(value) => value should be theSameInstanceAs diagnostic
          case _ => fail("Expected recorded original rejection.")
        }
        later.state.current shouldBe Some(thirdref)
        later.state.snapshots.map(_.reference) shouldBe Vector(firstref, thirdref)
        later.state.attempts.map(_.attempt) shouldBe Vector(first, second, third)
        firstref.value.recordRevision.value shouldBe 11
        left(missingreference).kind shouldBe DiagnosticKind.Malformed
        later.state.attempts.size shouldBe 3
      }
    }
    "C11S02 read only an exact selected accepted historical reference" in {
      Given("C11I-D09: two accepted snapshots with an intentionally older explicit historical selection")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val candidate = right(_ingestion.acquire(selectedinput))
        val oldref = snapshot("old", 5)
        val newref = snapshot("new", 1)
        val first = right(_ingestion.transition(State.empty, attempt("a:old", selectedinput, oldref), Right(candidate)))
        val latest = right(_ingestion.transition(first.state, attempt("a:new", selectedinput, newref), Right(candidate)))
        When("the old reference and an unknown reference are selected for historical reads")
        val old = right(_ingestion.readHistorical(latest.state, oldref))
        val unavailable = left(_ingestion.readHistorical(latest.state, snapshot("unknown", 99)))
        Then("the exact old object remains readable without currentness or newest-reference inference")
        old should be theSameInstanceAs first.state.snapshots.head
        latest.state.current shouldBe Some(newref)
        unavailable.kind shouldBe DiagnosticKind.Unavailable
        old.reference shouldBe oldref
      }
    }
    "C11S05 record duplicate accepted snapshot identity as an ambiguous rejected attempt" in {
      Given("C11I-D08/09: an already accepted exact snapshot reference and another explicitly allocated attempt")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val candidate = right(_ingestion.acquire(selectedinput))
        val ref = snapshot("one:accepted", 7)
        val first = right(_ingestion.transition(State.empty, attempt("a:first", selectedinput, ref), Right(candidate)))
        val again = attempt("a:another", selectedinput, ref)
        When("a complete candidate tries to identify another snapshot with the same accepted reference")
        val result = right(_ingestion.transition(first.state, again, Right(candidate)))
        Then("the attempt is appended as Ambiguous while original snapshots and current are retained")
        result.outcome match {
          case Outcome.Rejected(diagnostic) => diagnostic.kind shouldBe DiagnosticKind.Ambiguous
          case _ => fail("Expected duplicate snapshot rejection.")
        }
        result.state.snapshots shouldBe first.state.snapshots
        result.state.current shouldBe Some(ref)
        result.state.attempts.map(_.attempt.reference) shouldBe Vector(first.state.attempts.head.attempt.reference, again.reference)
        result.state.snapshots.head should be theSameInstanceAs first.state.snapshots.head
      }
    }
    "C11S07 actively preserve complete transition sequences over at least one hundred samples" in {
      Given("C11I-D08/09: one actual publicly acquired reusable candidate and finite acceptance/rejection sequences with explicit test references")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val candidate = right(_ingestion.acquire(selectedinput))
        val samples = Gen.choose(1, 24).flatMap(n => Gen.listOfN(n, Gen.oneOf(true, false)))
        When("ScalaCheck applies explicitly allocated attempts to each finite sequence")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { (accepted: List[Boolean]) =>
          val events = accepted.toVector.zipWithIndex.map { (success, index) =>
            val ref = snapshot("property:snapshot:" + index, index.toLong + 31)
            val a = attempt("property:attempt:" + index, selectedinput, ref)
            val error = rejected(DiagnosticKind.Stale).copy(detail = "Selected original rejection " + index)
            (success, a, error)
          }
          val finalstate = events.foldLeft[Either[Diagnostic, State]](Right(State.empty)) { case (prior, (success, a, error)) =>
            prior.flatMap(s => _ingestion.transition(s, a, if (success) Right(candidate) else Left(error)).map(_.state))
          }
          finalstate.exists { state =>
            val successful = events.filter(_._1).map(_._2.snapshotReference)
            state.current == successful.lastOption && state.snapshots.map(_.reference) == successful &&
              state.snapshots.forall(s => (s.candidate eq candidate) && s.provenance == candidate.provenance) &&
              state.attempts.map(_.attempt) == events.map(_._2) &&
              state.attempts.zip(events).forall { case (record, (success, a, error)) =>
                (record.attempt.selection eq selectedinput) && (record.attempt eq a) && (record.outcome match {
                  case Outcome.Accepted(ref) => success && ref == a.snapshotReference
                  case Outcome.Rejected(d) => !success && (d eq error)
                })
              }
          }
        })
        Then("only successful candidates select current and original rejection/selection history survives with at least 100 successes and zero discards")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
      }
    }
  }
  "Explicit current provenance and availability" should {
    "C11S03 require declared current reference and complete matching structured provenance" in {
      Given("C11I-D04/09: an accepted current snapshot and independently changed reference/scope/producer/coordinate/Source/header/availability selections")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val candidate = right(_ingestion.acquire(selectedinput))
        val ref = snapshot("current", 3)
        val state = right(_ingestion.transition(State.empty, attempt("a:current", selectedinput, ref), Right(candidate))).state
        val p = candidate.provenance
        val pkg = p.packages.head
        val resource = pkg.resources.head
        def _package_change_(x: PackageExpectation): Provenance = p.copy(packages = Vector(x))
        val variants = Vector(
          p.copy(scope = p.scope.copy(scopeId = "scope:other")),
          _package_change_(pkg.copy(resources = Vector(resource.copy(producer = Producer("different", "producer-v8"))))),
          _package_change_(pkg.copy(input = pkg.input.copy(packageCoordinate = pkg.input.packageCoordinate.copy(version = "another-package")))),
          _package_change_(pkg.copy(resources = Vector(resource.copy(source = resource.source.copy(sourceRevision = "stale"))))),
          _package_change_(pkg.copy(resources = Vector(resource.copy(schemaVersion = 2)))),
          p.copy(packages = Vector.empty),
          _package_change_(pkg.copy(usage = Some(CapabilityComponentUsageProjectionFixtures.expectation))))
        When("the exact selection and every changed structured expectation are used for current reads")
        val current = right(_ingestion.readCurrent(state, CurrentSelection(ref, p, Right(()))))
        val referenceerror = left(_ingestion.readCurrent(state, CurrentSelection(snapshot("other", 3), p, Right(()))))
        val errors = variants.map(v => left(_ingestion.readCurrent(state, CurrentSelection(ref, v, Right(())))))
        Then("only exact current provenance succeeds; typed mismatches never fall back to retained history")
        current should be theSameInstanceAs state.snapshots.head
        referenceerror.kind shouldBe DiagnosticKind.Stale
        errors.map(_.kind) shouldBe Vector(DiagnosticKind.Incompatible, DiagnosticKind.Incompatible, DiagnosticKind.Incompatible, DiagnosticKind.Stale,
          DiagnosticKind.Unsupported, DiagnosticKind.Malformed, DiagnosticKind.Incompatible)
        right(_ingestion.readHistorical(state, ref)) should be theSameInstanceAs current
      }
    }
    "C11S04 preserve original current observation failures before freshness selection" in {
      Given("C11I-D09: prior accepted history and original Stale, Unavailable and Unauthorized query observations")
      withDirectory { root =>
        val selectedinput = selection(Vector(selected(root, Vector("first.json" -> projection()))))
        val ref = snapshot("accepted:history", 4)
        val state = right(_ingestion.ingest(State.empty, attempt("a:history", selectedinput, ref))).state
        val originals = Vector(DiagnosticKind.Stale, DiagnosticKind.Unavailable, DiagnosticKind.Unauthorized).map(rejected)
        When("current reads supply those original observations with a different declared snapshot reference")
        val errors = originals.map(d => left(_ingestion.readCurrent(state, CurrentSelection(snapshot("other", 99), state.snapshots.head.provenance, Left(d)))))
        val historical = right(_ingestion.readHistorical(state, ref))
        Then("each complete original Diagnostic wins unchanged while explicit historical reading stays available")
        errors.zip(originals).foreach((actual, original) => actual should be theSameInstanceAs original)
        historical should be theSameInstanceAs state.snapshots.head
        state.current shouldBe Some(ref)
      }
    }
    "C11S06 retain honestly unavailable forward UseCase demand alongside authored ComponentUsage links" in {
      Given("C11I-D07: publicly acquired Component plus independently available full ComponentUsage with authored demand and mapping edges")
      withDirectory { root =>
        val s = CapabilityComponentUsageProjectionFixtures.scope
        val input = native(selectedScope = s)
        val selectedinput = selection(Vector(withUsage(selected(root, Vector("first.json" -> projection(input)), s, input), CapabilityComponentUsageProjectionFixtures.projection)), s)
        val ref = snapshot("with:usage", 8)
        When("the complete all-family candidate is ingested and explicitly selected")
        val state = right(_ingestion.ingest(State.empty, attempt("a:usage", selectedinput, ref))).state
        val stored = right(_ingestion.readHistorical(state, ref))
        Then("forward UseCase remains not-yet-admitted with its positive path unexercised and ComponentUsage authored directions intact")
        stored.useCase shouldBe useCase
        stored.useCase.family shouldBe ProjectionFamily.UseCase
        stored.useCase.reason shouldBe "not-yet-admitted"
        stored.useCase.detail shouldBe "Positive forward UseCase demand path remains unexercised."
        stored.usage.head.family match {
          case ComponentUsageFamily.Available(p, _) =>
            CapabilityComponentUsageJsonPart._items(CapabilityComponentUsageProjectionFixtures.usageDefinition(p).demandReferences).size shouldBe 4
            CapabilityComponentUsageJsonPart._items(CapabilityComponentUsageProjectionFixtures.usageDefinition(p).componentMappings).head.target shouldBe
              org.goldenport.cncf.component.capability.ComponentCapabilityProjection.CapabilityReference(capability.identity, capability.contractVersion)
          case _ => fail("Expected original available ComponentUsage alongside unavailable UseCase.")
        }
      }
    }
  }
}
