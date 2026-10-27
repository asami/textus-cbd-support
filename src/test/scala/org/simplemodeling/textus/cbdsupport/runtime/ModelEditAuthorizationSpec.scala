package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.context.{ExecutionContext, SessionContext, SubjectKind}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditAuthorizationSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditApplicationSpecFixture.*

  "Server-owned Model Edit authorization" should {
    "admit only the exact configured principal and scoped grant regardless of client name" in {
      forAll(Gen.identifier.suchThat(_.nonEmpty)) { principal =>
        Given("an explicitly selected principal and a separately selected read-only principal")
        When("that principal, an unrelated client and a read-only binding request editing")
        val fixture = create()
        val grant = ModelEditSpecFixture.authorization(fixture.current)
        val bindings = Vector(ModelEditAuthorization.Binding(principal, true, Some(grant)))
        val allowed = ModelEditAuthorization.authorize(bindings, fixture.current, true)(using principalContext(principal))
        val unknown = ModelEditAuthorization.authorize(bindings, fixture.current, true)(using principalContext(principal + "-other"))
        val readonly = ModelEditAuthorization.authorize(Vector(ModelEditAuthorization.Binding(principal, true, None)), fixture.current, true)(using principalContext(principal))
        Then("authority comes from the host binding rather than any product/client label")
        allowed.isSuccess shouldBe true
        unknown.isSuccess shouldBe false
        readonly.isSuccess shouldBe false
      }
    }

    "deny anonymous, expired, ambiguous and wrongly scoped authority before decoding a request" in {
      Given("a normal grant and an exact authenticated principal")
      When("authentication, session lifetime or host grant selection is invalid")
      val fixture = create()
      val base = principalContext("chatgpt")
      val grant = ModelEditSpecFixture.authorization(fixture.current)
      val binding = ModelEditAuthorization.Binding("chatgpt", true, Some(grant))
      val anonymous = ExecutionContext.withSecurityContext(base, base.security.copy(subjectKind = SubjectKind.Anonymous))
      val expired = ExecutionContext.withSecurityContext(base, base.security.copy(session = Some(SessionContext(expiresAt = Some(base.clock.instant().minusSeconds(1))))))
      val wrong = binding.copy(authorization = Some(grant.copy(session = ModelEditSpecFixture.record("another-session"))))
      val failures = Vector(
        ModelEditAuthorization.authorize(Vector(binding), fixture.current, true)(using anonymous),
        ModelEditAuthorization.authorize(Vector(binding), fixture.current, true)(using expired),
        ModelEditAuthorization.authorize(Vector(binding, binding), fixture.current, true)(using base),
        ModelEditAuthorization.authorize(Vector(wrong), fixture.current, true)(using base))
      val untrusted = fixture.application.execute("applyModelEdit", "not JSON")(using principalContext("unknown"))
      Then("no malformed caller payload can acquire editing authority")
      failures.map(_.isSuccess) shouldBe Vector.fill(4)(false)
      untrusted.isSuccess shouldBe false
    }
  }
}
