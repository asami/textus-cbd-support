package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets

import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.provider.{ProviderCall, ProviderEngine, ProviderRequest}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.Property

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class CbdHttp(actioncore: ActionCall.Core) extends CatalogFetcher with BokFetcher with SieBokTransport {
  def get(uri: URI): Consequence[String] =
    _get(uri, None)

  override def get(uri: URI, maxbytes: Int): Consequence[String] =
    _get(uri, Some(maxbytes))

  def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String] =
    ProviderEngine.execute(Call(
      ProviderCall.Core(
        ProviderRequest("cbd-information-http", "post", Map("url" -> endpoint.toString)),
        actioncore.executionContext,
        actioncore.component,
        actioncore.correlationId
      ),
      endpoint,
      Some(body),
      Map("Content-Type" -> "application/json")
    )).flatMap(response => _bounded_response(endpoint, response, Some(maxbytes)))

  private def _get(uri: URI, maxbytes: Option[Int]): Consequence[String] = {
    ProviderEngine.execute(Call(
      ProviderCall.Core(
        ProviderRequest("cbd-information-http", "get", Map("url" -> uri.toString)),
        actioncore.executionContext,
        actioncore.component,
        actioncore.correlationId
      ),
      uri,
      None,
      Map.empty
    )).flatMap(response => _bounded_response(uri, response, maxbytes))
  }

  private def _bounded_response(
    uri: URI,
    response: Response,
    maxbytes: Option[Int]
  ): Consequence[String] = {
    val bodybytes = response.body.getBytes(StandardCharsets.UTF_8).length
    if (response.statuscode < 200 || response.statuscode >= 300)
      Consequence.serviceUnavailable(
        s"HTTP ${response.statuscode} while fetching ${InformationSourceDiagnosticPolicy.renderUri(uri)}"
      )
    else if (maxbytes.exists(bodybytes > _))
      Consequence.serviceUnavailable(
        s"HTTP response exceeds ${maxbytes.get} bytes while fetching ${InformationSourceDiagnosticPolicy.renderUri(uri)}"
      )
    else
      Consequence.success(response.body)
  }

  private final case class Response(statuscode: Int, body: String)

  private final case class Call(
    core: ProviderCall.Core,
    uri: URI,
    body: Option[String],
    headers: Map[String, String]
  ) extends ProviderCall[Response] {
    protected def build_Program: ExecUowM[Response] =
      for {
        response <- body match {
          case Some(value) => http_post(uri.toString, Some(value), headers, Vector(Property("http.timeout-seconds", "10", None)))
          case None => http_get(uri.toString, headers, Vector(Property("http.timeout-seconds", "10", None)))
        }
      } yield Response(response.status.code, response.getString.getOrElse(""))
  }
}
