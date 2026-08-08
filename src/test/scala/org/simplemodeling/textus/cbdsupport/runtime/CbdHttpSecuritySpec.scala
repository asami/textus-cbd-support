package org.simplemodeling.textus.cbdsupport.runtime

import cats.~>
import java.net.URI
import java.nio.charset.StandardCharsets

import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.context.{ExecutionContext, RuntimeContext}
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.{Property, Request}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 15, 2026
 *  version Jul. 15, 2026
 * @version Aug.  8, 2026
 * @author  ASAMI, Tomoharu
 */
final class CbdHttpSecuritySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "CbdHttp authenticated information-source boundary" should {
    "isolate catalog, BoK, and SIE credentials while recording only safe CallTree metadata" in {
      Given("three remote source kinds with separate schemes, configuration keys, and credential values")
      val catalogsecret = "catalog-header-secret"
      val boksecret = "bok-header-secret"
      val siesecret = "sie-header-secret"
      val harness = _harness(Map(
        "credential.catalog" -> catalogsecret,
        "credential.bok" -> boksecret,
        "credential.sie" -> siesecret
      ))
      val cataloguri = URI.create("https://catalog.example/metadata/index.json?access_token=query-secret")
      val bokuri = URI.create("https://bok.example/knowledge/terms.json?api_key=query-secret")
      val sieuri = URI.create("https://sie.example/mcp?token=query-secret")
      val siebody = "{\"query\":\"body-secret\"}"

      When("each source executes through ProviderCall, UnitOfWork, and the configured HTTP driver")
      val catalogresult = harness.http.get(_catalog_source, cataloguri, 4096)
      val bokresult = harness.http.get(_bok_source, bokuri, 4096)
      val sieresult = harness.http.postJson(_sie_source, sieuri, siebody, 4096)

      Then("each driver call receives only its owning source's exact authentication header")
      catalogresult.toOption shouldBe Some("ok")
      bokresult.toOption shouldBe Some("ok")
      sieresult.toOption shouldBe Some("ok")
      harness.driver.calls shouldBe Vector(
        HttpCall("GET", cataloguri.toString, None, Map("Authorization" -> s"Bearer $catalogsecret")),
        HttpCall("GET", bokuri.toString, None, Map("Authorization" -> s"Basic $boksecret")),
        HttpCall(
          "POST",
          sieuri.toString,
          Some(siebody),
          Map("Content-Type" -> "application/json", "X-Api-Key" -> siesecret)
        )
      )

      And("CallTree retains source posture and sanitized locations without credentials or request payload")
      val calltree = harness.context.observability.callTreeContext.build().getOrElse(fail("calltree missing"))
      val calltreetext = calltree.toRecord.print
      calltreetext should include("source_id=catalog-auth")
      calltreetext should include("source_id=bok-auth")
      calltreetext should include("source_id=sie-auth")
      calltreetext should include("authentication_scheme=bearer")
      calltreetext should include("authentication_scheme=basic")
      calltreetext should include("authentication_scheme=api-key")
      calltreetext should include("credential_configured=***")
      calltreetext should include("url=https://catalog.example/metadata/index.json")
      calltreetext should include("url=https://bok.example/knowledge/terms.json")
      calltreetext should include("url=https://sie.example/mcp")
      Vector(
        "credential.catalog",
        "credential.bok",
        "credential.sie",
        catalogsecret,
        boksecret,
        siesecret,
        "query-secret",
        "body-secret",
        "Authorization",
        "X-Api-Key"
      ).foreach(calltreetext should not include _)
    }

    "refuse cross-origin work for every authenticated source before the HTTP driver" in {
      Given("catalog, BoK, and SIE sources whose credentials belong to three authorized origins")
      val secrets = Map(
        "credential.catalog" -> "catalog-cross-origin-secret",
        "credential.bok" -> "bok-cross-origin-secret",
        "credential.sie" -> "sie-cross-origin-secret"
      )
      val harness = _harness(secrets)
      val foreignuri = URI.create("https://foreign.example/data?access_token=foreign-query-secret")

      When("each source is asked to send its credential to the foreign origin")
      val results = Vector(
        harness.http.get(_catalog_source, foreignuri, 4096),
        harness.http.get(_bok_source, foreignuri, 4096),
        harness.http.postJson(_sie_source, foreignuri, "{\"query\":\"foreign-body-secret\"}", 4096)
      )

      Then("every request fails without driver work, fallback, or credential-bearing diagnostics")
      all(results.map(_.toOption)) shouldBe None
      harness.driver.calls shouldBe empty
      val diagnostics = results.map(_.display).mkString(" ")
      diagnostics should include("outside its authorized origin")
      diagnostics should not include "foreign-query-secret"
      diagnostics should not include "foreign-body-secret"
      secrets.foreach { case (key, value) =>
        diagnostics should not include key
        diagnostics should not include value
      }

      And("failed-provider CallTree metadata remains bounded to sanitized source identity and location")
      val calltree = harness.context.observability.callTreeContext.build().getOrElse(fail("calltree missing"))
      val calltreetext = calltree.toRecord.print
      calltreetext should include("url=https://foreign.example/data")
      calltreetext should include("outcome=failure")
      calltreetext should not include "foreign-query-secret"
      calltreetext should not include "foreign-body-secret"
      secrets.foreach { case (key, value) =>
        calltreetext should not include key
        calltreetext should not include value
      }
    }
  }

  private def _catalog_source: CatalogSource =
    CatalogSource(
      "catalog-auth",
      URI.create("https://catalog.example/"),
      100,
      true,
      authentication = Some(SourceAuthentication("bearer", "config-key/credential.catalog"))
    )

  private def _bok_source: BokSource =
    BokSource(
      "bok-auth",
      URI.create("https://bok.example/knowledge/"),
      200,
      true,
      authentication = Some(SourceAuthentication("basic", "config-key/credential.bok"))
    )

  private def _sie_source: SieBokSource =
    SieBokSource(
      "sie-auth",
      URI.create("https://sie.example/mcp"),
      300,
      true,
      authentication = Some(SourceAuthentication("api-key", "config-key/credential.sie"))
    )

  private def _harness(parameters: Map[String, String]): TestHarness = {
    val driver = new RecordingHttpDriver
    val base = ExecutionContext.withFrameworkCallTreeEnabled(ExecutionContext.create(), enabled = true)
    lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(base, runtime)
    lazy val unitofwork: UnitOfWork = new UnitOfWork(context)
    lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(unitofwork)
    lazy val runtime: RuntimeContext = new RuntimeContext(
      core = RuntimeContext.core(
        name = "cbd-http-security-spec",
        parent = None,
        observabilityContext = base.observability,
        httpDriverOption = Some(driver)
      ),
      unitOfWorkSupplier = () => unitofwork,
      unitOfWorkInterpreterFn = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] =
          interpreter.interpret(operation)
      },
      commitAction = _ => (),
      abortAction = _ => (),
      disposeAction = _ => (),
      token = "cbd-http-security-spec"
    )
    runtime.setResolvedParameters(ResolvedParameters.fromFrameworkProperties(
      parameters.toList.map { case (key, value) => Property(key, value, None) },
      "provider",
      None
    ))
    val action = TestAction(Request.ofOperation("cbd-http-security"))
    val core = ActionCall.Core(action, context, None, None)
    TestHarness(new CbdHttp(core), context, driver)
  }

  private final case class TestHarness(
    http: CbdHttp,
    context: ExecutionContext,
    driver: RecordingHttpDriver
  )

  private final case class TestAction(request: Request) extends Action {
    override def createCall(core: ActionCall.Core): ActionCall = {
      val _ = core
      throw new UnsupportedOperationException("TestAction is an ActionCall.Core fixture only.")
    }
  }

  private final case class HttpCall(
    method: String,
    path: String,
    body: Option[String],
    headers: Map[String, String]
  )

  private final class RecordingHttpDriver extends HttpDriver {
    private var _calls = Vector.empty[HttpCall]
    private val _response = HttpResponse.Text(
      HttpStatus.Ok,
      ContentType(MimeType("text/plain"), Some(StandardCharsets.UTF_8)),
      Bag.text("ok", StandardCharsets.UTF_8)
    )

    def calls: Vector[HttpCall] = _calls

    override def get(
      path: String,
      headers: Map[String, String],
      properties: Vector[Property]
    ): HttpResponse = {
      val _ = properties
      _calls = _calls :+ HttpCall("GET", path, None, headers)
      _response
    }

    override def post(
      path: String,
      body: Option[String],
      headers: Map[String, String],
      properties: Vector[Property]
    ): HttpResponse = {
      val _ = properties
      _calls = _calls :+ HttpCall("POST", path, body, headers)
      _response
    }

    override def put(
      path: String,
      body: Option[String],
      headers: Map[String, String],
      properties: Vector[Property]
    ): HttpResponse = {
      val _ = properties
      _calls = _calls :+ HttpCall("PUT", path, body, headers)
      _response
    }
  }
}
