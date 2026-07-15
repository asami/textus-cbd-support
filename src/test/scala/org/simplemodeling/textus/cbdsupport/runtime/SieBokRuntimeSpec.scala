package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.time.{Clock, Instant, ZoneOffset}

import io.circe.parser.parse
import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 14, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class SieBokRuntimeSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "SieBokConfig" should {
    "admit only explicitly allowlisted public MCP component routes" in {
      Given("one authorized public SIE MCP route and several unsafe alternatives")
      val configuration = SieBokConfig.parse(
        Some("semantic=https://sie.example/mcp,bad-path=https://sie.example/rest,bad-origin=https://other.example/mcp,simplemodeling=https://sie.example/mcp"),
        Some("https://sie.example"),
        Set("simplemodeling")
      )

      Then("only the fixed public route is exposed as a SIE BoK information source")
      configuration.sources.map(_.id) shouldBe Vector("semantic")
      configuration.sources.head.descriptor shouldBe InformationSourceDescriptor(
        "semantic",
        InformationSourceKind.SIE_BOK,
        "https://sie.example/mcp",
        700,
        true,
        InformationSourceAuthorization.COMPONENT_ROUTE_ALLOWLIST
      )
      configuration.warnings should contain("Configured SIE entry 2 was rejected because its public route must be /mcp.")
      configuration.warnings should contain("Configured SIE entry 3 was rejected because origin https://other.example is not allowlisted.")
      configuration.warnings should contain("Configured SIE source simplemodeling was rejected because its source ID is reserved or duplicated.")
    }
  }

  "SieBokProvider" should {
    "retrieve the typed public SIE term contract with mandatory evidence" in {
      Given("an evidence-bearing response from the typed public searchTerms tool")
      val transport = new MemorySieBokTransport(_response(
        "matched",
        "Execution Runtime",
        """[{"id":"architecture:runtime","title":"Execution Runtime","definition":"Runtime definition.","category":"architecture","term_type":"term","dataset_id":"bok-main","match_kind":"exact","score":1.0,"rationale":"Exact label match.","evidence_uri":"https://bok.example/terms/runtime"}]"""
      ))
      val provider = new SieBokProvider(_clock)

      When("CBD calls the public SIE MCP route")
      val snapshot = provider.searchTerms(
        _source,
        "Execution Runtime",
        Some("architecture"),
        10,
        transport
      ).toOption.get

      Then("the SIE-owned terminology remains a separate evidence-bearing observation")
      snapshot.source.sourceKind shouldBe InformationSourceKind.SIE_BOK
      snapshot.status shouldBe "matched"
      snapshot.observedAt shouldBe Instant.parse("2026-07-14T06:00:00Z")
      snapshot.terms shouldBe Vector(SieBokTermEvidence(
        "semantic",
        "architecture:runtime",
        "Execution Runtime",
        "Runtime definition.",
        Some("architecture"),
        "term",
        "bok-main",
        "exact",
        1.0,
        "Exact label match.",
        URI.create("https://bok.example/terms/runtime")
      ))

      And("the request uses only the typed public operation and a bounded response")
      transport.sourceId shouldBe Some(_source.id)
      transport.maxBytes shouldBe SieBokPolicy.DEFAULT.maxResponseBytes
      val request = parse(transport.body).toOption.get.hcursor
      request.get[String]("method").toOption shouldBe Some("tools/call")
      request.downField("params").get[String]("name").toOption shouldBe Some(SieBokProvider.SEARCH_TERMS_TOOL)
      request.downField("params").downField("arguments").get[String]("query").toOption shouldBe Some("Execution Runtime")
      request.downField("params").downField("arguments").get[String]("category").toOption shouldBe Some("architecture")
      request.downField("params").downField("arguments").get[Int]("limit").toOption shouldBe Some(10)
    }

    "reject a term response that has no valid evidence URI" in {
      Given("a nominal SIE match whose evidence field is absent")
      val transport = new MemorySieBokTransport(_response(
        "matched",
        "Runtime",
        """[{"id":"architecture:runtime","title":"Runtime","definition":"Definition.","term_type":"term","dataset_id":"bok-main","match_kind":"exact","score":1.0,"rationale":"Exact."}]"""
      ))

      When("CBD validates the response contract")
      val result = new SieBokProvider(_clock).searchTerms(_source, "Runtime", None, 10, transport)

      Then("the response is rejected instead of manufacturing evidence")
      result match {
        case Consequence.Failure(conclusion) => conclusion.display should include("evidence_uri")
        case Consequence.Success(_) => fail("SIE term response without evidence was accepted.")
      }
    }
  }

  "CbdRuntime" should {
    "report SIE retrieval readiness without merging terminology into component profiles" in {
      Given("a federated runtime with an authorized SIE source and no catalog component")
      val transport = new MemorySieBokTransport(_response(
        "matched",
        "Runtime",
        """[{"id":"architecture:runtime","title":"Runtime","definition":"Definition.","term_type":"term","dataset_id":"bok-main","match_kind":"exact","score":1.0,"rationale":"Exact.","evidence_uri":"urn:bok:runtime"}]"""
      ))
      val runtime = CbdRuntime.createFederated(
        Vector.empty,
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        _clock,
        Vector.empty,
        new BokKnowledgeSourceProvider(_clock),
        sieboksources = Vector(_source),
        siebokprovider = new SieBokProvider(_clock)
      )

      When("the runtime retrieves SIE terminology")
      val snapshots = runtime.searchSieTerms("Runtime", None, 10, transport).toOption.get

      Then("the source is ready with evidence but CBD component ownership remains empty")
      snapshots.flatMap(_.terms).map(_.evidenceUri) shouldBe Vector(URI.create("urn:bok:runtime"))
      runtime.componentCount shouldBe 0
      runtime.informationSourceStates(includeDisabled = false).map { state =>
        (state.descriptor.id, state.descriptor.sourceKind, state.status, state.observationCount)
      } shouldBe Vector(("semantic", InformationSourceKind.SIE_BOK, "ready", 1))
    }

    "keep last-known evidence in source state without returning it for a failed different query" in {
      Given("one successful SIE observation followed by failure for another query")
      val successfultransport = new MemorySieBokTransport(_response(
        "matched",
        "Runtime",
        """[{"id":"architecture:runtime","title":"Runtime","definition":"Definition.","term_type":"term","dataset_id":"bok-main","match_kind":"exact","score":1.0,"rationale":"Exact.","evidence_uri":"urn:bok:runtime"}]"""
      ))
      val runtime = CbdRuntime.createFederated(
        Vector.empty,
        new InMemoryComponentCatalogProvider(Vector.empty),
        CatalogCachePolicy.DEFAULT,
        _clock,
        Vector.empty,
        new BokKnowledgeSourceProvider(_clock),
        sieboksources = Vector(_source),
        siebokprovider = new SieBokProvider(_clock)
      )
      runtime.searchSieTerms("Runtime", None, 10, successfultransport).toOption.get

      When("a different query fails at the transport boundary")
      val current = runtime.searchSieTerms("Security", None, 10, FailingSieBokTransport).toOption.get

      Then("the current result is empty while retained evidence is visible only as degraded source state")
      current shouldBe Vector.empty
      runtime.sieBokSnapshots.map(_.query) shouldBe Vector("Runtime")
      val state = runtime.sieBokSourceStates(includedisabled = false).head
      state.status shouldBe "degraded"
      state.termCount shouldBe 1
      state.diagnostics should contain("SIE test transport is unavailable.")
    }
  }

  private val _clock = Clock.fixed(Instant.parse("2026-07-14T06:00:00Z"), ZoneOffset.UTC)
  private val _source = SieBokSource("semantic", URI.create("https://sie.example/mcp"), 700, true)

  private def _response(status: String, query: String, results: String): String =
    s"""{"jsonrpc":"2.0","id":"cbd-semantic","result":{"content":[{"type":"text","text":"{\\"status\\":\\"$status\\",\\"query\\":\\"$query\\",\\"results\\":${results.replace("\"", "\\\"")},\\"warnings\\":[]}"}]}}"""
}

private final class MemorySieBokTransport(response: String) extends SieBokTransport {
  var sourceId: Option[String] = None
  var endpoint: URI = URI.create("https://invalid.example/")
  var body: String = ""
  var maxBytes: Int = 0

  def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String] = {
    this.endpoint = endpoint
    this.body = body
    maxBytes = maxbytes
    Consequence.success(response)
  }

  override def postJson(
    source: SieBokSource,
    endpoint: URI,
    body: String,
    maxbytes: Int
  ): Consequence[String] = {
    sourceId = Some(source.id)
    postJson(endpoint, body, maxbytes)
  }
}

private object FailingSieBokTransport extends SieBokTransport {
  def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String] =
    Consequence.serviceUnavailable("SIE test transport is unavailable.")
}
