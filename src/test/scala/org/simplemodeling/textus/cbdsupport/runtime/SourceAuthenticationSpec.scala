package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class SourceAuthenticationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "SourceAuthenticationConfig" should {
    "admit bounded credential references for configured remote sources" in {
      Given("catalog, BoK, and SIE source IDs with secret-free authentication bindings")
      val configured = SourceAuthenticationConfig.parse(
        Some(
          "catalog-team=bearer:config-key/textus.cbd.credentials.catalog," +
          "bok-team=api-key:config-key/textus.cbd.credentials.bok," +
          "sie-team=basic:config-key/textus.cbd.credentials.sie"
        ),
        Set("catalog-team", "bok-team", "sie-team")
      )

      When("the shared source authentication configuration is parsed")
      val catalog = configured.authenticationFor("catalog-team").get
      val bok = configured.authenticationFor("bok-team").get
      val sie = configured.authenticationFor("sie-team").get

      Then("each source retains only its explicit scheme and CNCF configuration-key reference")
      catalog shouldBe SourceAuthentication("bearer", "config-key/textus.cbd.credentials.catalog")
      bok shouldBe SourceAuthentication("api-key", "config-key/textus.cbd.credentials.bok")
      sie shouldBe SourceAuthentication("basic", "config-key/textus.cbd.credentials.sie")
      configured.warnings shouldBe empty
    }

    "reject unknown sources, unsupported schemes, raw values, and duplicate bindings without echoing them" in {
      Given("invalid bindings containing a secret-like raw value and an unconfigured source")
      val rawsecret = "do-not-echo-this-secret"
      val configured = SourceAuthenticationConfig.parse(
        Some(
          s"missing=bearer:config-key/missing,catalog=oauth:config-key/oauth," +
          s"bok=api-key:$rawsecret,catalog=bearer:config-key/catalog," +
          "catalog=basic:config-key/catalog-duplicate"
        ),
        Set("catalog", "bok")
      )

      When("the bounded parser validates source ownership, scheme, reference shape, and uniqueness")
      val warningtext = configured.warnings.mkString(" ")

      Then("only the first valid binding survives and diagnostics contain no rejected credential material")
      configured.authentications shouldBe Map(
        "catalog" -> SourceAuthentication("bearer", "config-key/catalog")
      )
      warningtext should include("source ID is not configured")
      warningtext should include("scheme is unsupported")
      warningtext should include("credential reference is invalid")
      warningtext should include("source ID is duplicated")
      warningtext should not include rawsecret
      warningtext should not include "config-key/catalog-duplicate"
    }

    "stop authentication discovery at the configured binding bound" in {
      Given("two valid source authentication bindings and a policy that admits one")
      val configured = SourceAuthenticationConfig.parse(
        Some("catalog=bearer:config-key/catalog,bok=api-key:config-key/bok"),
        Set("catalog", "bok"),
        SourceAuthenticationPolicy(maxBindings = 1)
      )

      When("the bounded authentication configuration is parsed")
      val sourceids = configured.authentications.keySet

      Then("only admitted work becomes configuration and truncation remains observable")
      sourceids shouldBe Set("catalog")
      configured.warnings should contain("Source authentication configuration exceeds the limit of 1.")
    }
  }

  "Information source descriptors" should {
    "expose authentication posture without exposing credential references" in {
      Given("catalog, BoK, and SIE sources bound to separate credential references")
      val sources = Vector(
        CatalogSource(
          "catalog",
          URI.create("https://catalog.example/"),
          100,
          true,
          authentication = Some(SourceAuthentication("bearer", "config-key/catalog-secret"))
        ).descriptor,
        BokSource(
          "bok",
          URI.create("https://bok.example/"),
          200,
          true,
          Some(SourceAuthentication("api-key", "config-key/bok-secret"))
        ).descriptor,
        SieBokSource(
          "sie",
          URI.create("https://sie.example/mcp"),
          300,
          true,
          Some(SourceAuthentication("basic", "config-key/sie-secret"))
        ).descriptor
      )

      When("the runtime projects source descriptors")
      val rendered = sources.mkString(" ")

      Then("scheme and configured state are visible while every credential reference stays internal")
      sources.map(_.authenticationScheme) shouldBe Vector("bearer", "api-key", "basic")
      sources.map(_.credentialConfigured) shouldBe Vector(true, true, true)
      sources.flatMap(_.productElementNames).toSet should not contain "credentialRef"
      rendered should not include "config-key/"
    }
  }
}
