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

  "SourceAuthenticationHeaders" should {
    "resolve each supported scheme only when a same-origin request reaches the outbound boundary" in {
      Given("three authenticated sources and one resolver containing their runtime credentials")
      val credentials = Map(
        "bearer" -> "bearer-token",
        "basic" -> "dXNlcjpwYXNz",
        "api-key" -> "api-key-value"
      )
      var resolvedkeys = Vector.empty[String]
      val resolver = (key: String) => {
        resolvedkeys = resolvedkeys :+ key
        credentials.get(key)
      }
      val sources = Vector(
        SourceAuthenticationRequest(
          "catalog",
          URI.create("https://catalog.example/base/"),
          Some(SourceAuthentication("bearer", "config-key/bearer"))
        ) -> URI.create("https://catalog.example/metadata/index.json"),
        SourceAuthenticationRequest(
          "bok",
          URI.create("https://bok.example/knowledge/"),
          Some(SourceAuthentication("basic", "config-key/basic"))
        ) -> URI.create("https://bok.example/knowledge/terms.json"),
        SourceAuthenticationRequest(
          "sie",
          URI.create("https://sie.example/mcp"),
          Some(SourceAuthentication("api-key", "config-key/api-key"))
        ) -> URI.create("https://sie.example/mcp")
      )

      When("header construction runs immediately before each authorized outbound request")
      val headers = sources.map { case (source, requesturi) =>
        SourceAuthenticationHeaders.headersFor(source, requesturi, resolver).toOption.get
      }

      Then("the resolver is invoked once per request and each scheme produces only its defined header")
      resolvedkeys shouldBe Vector("bearer", "basic", "api-key")
      headers shouldBe Vector(
        Map("Authorization" -> "Bearer bearer-token"),
        Map("Authorization" -> "Basic dXNlcjpwYXNz"),
        Map("X-Api-Key" -> "api-key-value")
      )
    }

    "refuse cross-origin credential use before resolving a secret" in {
      Given("one source-owned bearer reference and a request for another origin")
      val source = SourceAuthenticationRequest(
        "catalog",
        URI.create("https://catalog.example/base/"),
        Some(SourceAuthentication("bearer", "config-key/catalog"))
      )
      var resolutioncount = 0

      When("the outbound authentication boundary checks the request origin")
      val result = SourceAuthenticationHeaders.headersFor(
        source,
        URI.create("https://other.example/metadata/index.json"),
        _ => {
          resolutioncount += 1
          Some("must-not-be-read")
        }
      )

      Then("the request fails without consulting or exposing the source credential")
      result.toOption shouldBe None
      resolutioncount shouldBe 0
      result.display should not include "config-key/catalog"
      result.display should not include "must-not-be-read"
    }

    "avoid credential resolution for a source without an authentication binding" in {
      Given("one authorized source with no authentication reference")
      val source = SourceAuthenticationRequest(
        "public-catalog",
        URI.create("https://catalog.example/"),
        None
      )
      var resolutioncount = 0

      When("the request reaches outbound header construction")
      val result = SourceAuthenticationHeaders.headersFor(
        source,
        URI.create("https://catalog.example/index.json"),
        _ => {
          resolutioncount += 1
          Some("unused")
        }
      )

      Then("the request remains unauthenticated and no resolver work occurs")
      result.toOption shouldBe Some(Map.empty)
      resolutioncount shouldBe 0
    }

    "classify missing, unavailable, and locally rejected credentials without fallback or secret disclosure" in {
      Given("one source-owned reference and resolvers for each pre-request lifecycle failure")
      val source = SourceAuthenticationRequest(
        "catalog",
        URI.create("https://catalog.example/"),
        Some(SourceAuthentication("bearer", "config-key/catalog-primary"))
      )
      val requesturi = URI.create("https://catalog.example/index.json")
      val rawsecret = "secret-with-control\n"
      var resolvedkeys = Vector.empty[String]

      When("the outbound boundary resolves the source key once for each attempt")
      val missing = SourceAuthenticationHeaders.headersFor(source, requesturi, key => {
        resolvedkeys = resolvedkeys :+ key
        None
      })
      val unavailable = SourceAuthenticationHeaders.headersFor(source, requesturi, key => {
        resolvedkeys = resolvedkeys :+ key
        throw new IllegalStateException("secret provider detail")
      })
      val rejected = SourceAuthenticationHeaders.headersFor(source, requesturi, key => {
        resolvedkeys = resolvedkeys :+ key
        Some(rawsecret)
      })

      Then("each failure has a distinct stable code and no other source key, value, or resolver detail is exposed")
      resolvedkeys shouldBe Vector.fill(3)("catalog-primary")
      missing.display should include(SourceAuthenticationFailure.CREDENTIAL_MISSING)
      unavailable.display should include(SourceAuthenticationFailure.CREDENTIAL_UNAVAILABLE)
      rejected.display should include(SourceAuthenticationFailure.CREDENTIAL_REJECTED)
      val diagnostics = Vector(missing.display, unavailable.display, rejected.display).mkString(" ")
      diagnostics should not include "config-key/"
      diagnostics should not include rawsecret.trim
      diagnostics should not include "secret provider detail"
    }
  }

  "SourceAuthenticationFailure" should {
    "distinguish an explicitly expired challenge from other remote credential rejection" in {
      Given("one authenticated source and bounded HTTP authentication outcomes")
      val source = Some(SourceAuthenticationRequest(
        "catalog",
        URI.create("https://catalog.example/"),
        Some(SourceAuthentication("bearer", "config-key/catalog"))
      ))
      val expiredchallenge = SourceAuthenticationFailure.isExpiredChallenge(
        Some("Bearer error=\"invalid_token\", error_description=\"access token expired\"")
      )
      val outofboundexpiry = SourceAuthenticationFailure.isExpiredChallenge(
        Some("x" * SourceAuthenticationFailure.MAXIMUM_CHALLENGE_CHARACTERS + "expired")
      )

      When("the authenticated response status and sanitized challenge signal are classified")
      val expired = SourceAuthenticationFailure.fromHttp(source, 401, expiredchallenge).get
      val unauthorized = SourceAuthenticationFailure.fromHttp(source, 401, expiredchallenge = false).get
      val forbidden = SourceAuthenticationFailure.fromHttp(source, 403, expiredchallenge = false).get
      val publicresponse = SourceAuthenticationFailure.fromHttp(
        source.map(_.copy(authentication = None)),
        401,
        expiredchallenge = true
      )

      Then("only explicit expiry is expired, other authenticated denials are rejected, and public failures stay transport-owned")
      expired.code shouldBe SourceAuthenticationFailure.CREDENTIAL_EXPIRED
      outofboundexpiry shouldBe false
      unauthorized.code shouldBe SourceAuthenticationFailure.CREDENTIAL_REJECTED
      forbidden.code shouldBe SourceAuthenticationFailure.CREDENTIAL_REJECTED
      publicresponse shouldBe None
      expired.consequence[String].display should include(SourceAuthenticationFailure.CREDENTIAL_EXPIRED)
      expired.consequence[String].display should not include "invalid_token"
      expired.consequence[String].display should not include "access token expired"
    }
  }
}
