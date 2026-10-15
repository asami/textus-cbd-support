package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path
import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, SecurityContext}
import org.goldenport.cncf.http.{StaticFormAppRenderer, WebDescriptor}
import org.goldenport.cncf.http.StaticFormAppRendererSupport.{FormPageProperties, FormResultProperties}
import org.goldenport.cncf.mcp.{McpJsonRpcAdapter, McpProtocolRevision}
import org.goldenport.cncf.naming.NamingConventions
import org.goldenport.cncf.subsystem.*
import org.goldenport.configuration.{Configuration, ConfigurationValue, ConfigurationTrace, ResolvedConfiguration}
import org.goldenport.protocol.{Request, Property}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import CapabilityIngestionModel.*
import CapabilityCatalogModel.*
import CapabilityProducerIntegrationFixtures.right

/*
 * Test-only connections to production server configuration and native transports.
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityReadSurfaceFixtures {
  final case class NativeRead(api: Json, mcp: Json, form: Json, formhtml: String, formresulthtml: String)

  def admitted(selected: Selection, id: String, revision: Long): LiveCatalog = {
    val reference = CapabilityProducerIntegrationFixtures.snapshot(id, revision)
    val transition = right(CapabilityIngestion.ingest(State.empty,
      CapabilityProducerIntegrationFixtures.attempt(id + "-attempt", revision, selected, reference)))
    val snapshot = right(CapabilityIngestion.readCurrent(transition.state,
      CurrentSelection(reference, transition.state.snapshots.head.provenance, Right(()))))
    right(CapabilityCatalog.fromSnapshot(snapshot, 1))
  }

  final class Native(val catalog: Catalog, authenticated: Boolean = true) {
    private val _selection = CatalogSelection(catalog.projection.snapshotReference, catalog.projection.provenance)
    private val _configuration = Configuration(Map(
      "textus.subsystem.user-mode" -> ConfigurationValue.StringValue("standalone"),
      "textus.cbd.capability.projection" -> ConfigurationValue.StringValue(right(CapabilityCatalogProjectionCodec.encode(catalog.projection, _selection))),
      "textus.cbd.capability.selection" -> ConfigurationValue.StringValue(CapabilityCatalogJsonEncodingPart.encodeSelection(_selection).noSpaces)))
    val subsystem: Subsystem = org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture.default(Some("server"), ResolvedConfiguration(_configuration, ConfigurationTrace.empty))
    private val _component = new org.simplemodeling.textus.cbdsupport.impl.ComponentFactory().create(
      ComponentCreate(subsystem, ComponentOrigin.Repository("capability-read-real-producer"))).primary
      .withArtifactMetadata(Component.ArtifactMetadata(sourceType = "spec", name = "textus-cbd-support", version = "1.2.3-SNAPSHOT", component = Some("textus-cbd-support")))
    subsystem.add(Vector(_component))
    if (authenticated) subsystem.withDescriptor(GenericSubsystemDescriptor(
      path = Path.of("capability-read-native"), subsystemName = subsystem.name,
      security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
        convention = Some("disabled"), fallbackPrivilege = Some("disabled"),
        localSubject = Some(GenericSubsystemLocalSubjectBinding("capability-viewer", roles = Vector("viewer")))))))))
    private val _adapter = new McpJsonRpcAdapter(subsystem)
    private val _renderer = new StaticFormAppRenderer()
    val descriptor: WebDescriptor = WebDescriptor.load(Path.of("src/main/web-inf/form.yaml")).fold(c => throw new IllegalArgumentException(c.display), identity)

    def toolNames: Vector[String] = _rpc("tools/list", Json.obj()).hcursor.downField("result").get[Vector[Json]]("tools").toOption.get.map(_.hcursor.get[String]("name").toOption.get)

    def request(operation: String, selector: Json): Request = requestRaw(operation, selector.noSpaces)

    def requestRaw(operation: String, selector: String): Request = Request.of(
      component = CbdSupportComponent.name, service = "CbdRetrieval", operation = operation,
      properties = List(Property("selector", selector, None), Property("textus.format", "json", None)))

    def api(operation: String, selector: Json): Consequence[Json] = apiRaw(operation, selector.noSpaces)

    def apiRaw(operation: String, selector: String): Consequence[Json] = subsystem.execute(requestRaw(operation, selector)).map(r => _json(r.print))

    def mcpOutcome(operation: String, selector: Json): Json = mcpOutcomeRaw(operation, selector.noSpaces)

    def mcpOutcomeRaw(operation: String, selector: String): Json = _rpc("tools/call", Json.obj(
      "name" -> Json.fromString(CbdSupportComponent.name + ".CbdRetrieval." + operation),
      "arguments" -> Json.obj("selector" -> Json.fromString(selector))))

    def read(operation: String, selector: Json): NativeRead = {
      val apiresult = api(operation, selector).fold(c => throw new IllegalArgumentException(c.display), identity)
      val mcpresult = mcpOutcome(operation, selector)
      val mcpbody = _json(mcpresult.hcursor.downField("result").downField("content").downArray.get[String]("text").toOption.get)
      val kebab = NamingConventions.toNormalizedSegment(operation)
      val formvalues = Map("selector" -> selector.noSpaces)
      val formhtml = _renderer.renderOperationForm(subsystem, "textus-cbd-support", "cbd-retrieval", kebab, descriptor, formvalues).get.body
      val validation = _renderer.validateOperationForm(subsystem, "textus-cbd-support", "cbd-retrieval", kebab, formvalues, descriptor).get
      require(validation.valid, "Native operation form must accept the declared selector field")
      // Form ingress uses the generated operation with its validated form properties.
      val formrequest = Request.of(component = CbdSupportComponent.name, service = "CbdRetrieval", operation = operation,
        properties = validation.values.toList.map((key, value) => Property(key, value, None)) :+ Property("textus.format", "json", None))
      val formbody = subsystem.execute(formrequest).map(_.print).fold(c => throw new IllegalArgumentException(c.display), identity)
      val template = descriptor.form("textus-cbd-support.cbd-retrieval." + kebab).resultTemplate.get
      val rendered = _renderer.renderFormResult(FormResultProperties(
        FormPageProperties("textus-cbd-support", "cbd-retrieval", kebab, formvalues), 200, "application/json", formbody), template).body
      NativeRead(apiresult, mcpbody, _json(formbody), formhtml, rendered)
    }

    def anonymous(operation: String, selector: Json): Consequence[OperationResponse] =
      subsystem.executeOperationResponseInContext(request(operation, selector), ExecutionContext.create(SecurityContext.Privilege.Anonymous))

    def errorHtml(operation: String, detail: String): String = {
      val kebab = NamingConventions.toNormalizedSegment(operation)
      val template = descriptor.form("textus-cbd-support.cbd-retrieval." + kebab).resultTemplate.get
      _renderer.renderFormResult(FormResultProperties(FormPageProperties("textus-cbd-support", "cbd-retrieval", kebab),
        403, "application/json", Json.obj("message" -> Json.fromString(detail)).noSpaces), template).body
    }

    private def _rpc(method: String, params: Json): Json = _adapter.handle(Json.obj("jsonrpc" -> Json.fromString("2.0"),
      "id" -> Json.fromInt(1), "method" -> Json.fromString(method), "params" -> params).noSpaces,
      Some(McpProtocolRevision.PREFERRED.print)).responseBody.get
    private def _json(text: String): Json = parse(text).fold(e => throw new IllegalArgumentException(e.message), identity)
  }
}
