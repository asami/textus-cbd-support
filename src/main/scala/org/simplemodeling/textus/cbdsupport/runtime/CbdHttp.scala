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
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class CbdHttp(actioncore: ActionCall.Core) extends CatalogFetcher with BokFetcher with SieBokTransport {
  def get(uri: URI): Consequence[String] =
    _get(None, uri, None)

  override def get(uri: URI, maxbytes: Int): Consequence[String] =
    _get(None, uri, Some(maxbytes))

  override def get(source: CatalogSource, uri: URI, maxbytes: Int): Consequence[String] =
    _get(Some(SourceAuthenticationRequest.from(source)), uri, Some(maxbytes))

  override def get(source: BokSource, uri: URI, maxbytes: Int): Consequence[String] =
    _get(Some(SourceAuthenticationRequest.from(source)), uri, Some(maxbytes))

  def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String] =
    _post_json(None, endpoint, body, maxbytes)

  override def postJson(
    source: SieBokSource,
    endpoint: URI,
    body: String,
    maxbytes: Int
  ): Consequence[String] =
    _post_json(Some(SourceAuthenticationRequest.from(source)), endpoint, body, maxbytes)

  private def _post_json(
    sourceauthentication: Option[SourceAuthenticationRequest],
    endpoint: URI,
    body: String,
    maxbytes: Int
  ): Consequence[String] =
    ProviderEngine.execute(Call(
      ProviderCall.Core(
        ProviderRequest("cbd-information-http", "post", _request_attributes(sourceauthentication, endpoint)),
        actioncore.executionContext,
        actioncore.component,
        actioncore.correlationId
      ),
      endpoint,
      Some(body),
      Map("Content-Type" -> "application/json"),
      sourceauthentication
    )).flatMap(response => _bounded_response(sourceauthentication, endpoint, response, Some(maxbytes)))

  private def _get(
    sourceauthentication: Option[SourceAuthenticationRequest],
    uri: URI,
    maxbytes: Option[Int]
  ): Consequence[String] = {
    ProviderEngine.execute(Call(
      ProviderCall.Core(
        ProviderRequest("cbd-information-http", "get", _request_attributes(sourceauthentication, uri)),
        actioncore.executionContext,
        actioncore.component,
        actioncore.correlationId
      ),
      uri,
      None,
      Map.empty,
      sourceauthentication
    )).flatMap(response => _bounded_response(sourceauthentication, uri, response, maxbytes))
  }

  private def _request_attributes(
    sourceauthentication: Option[SourceAuthenticationRequest],
    uri: URI
  ): Map[String, String] =
    Map("url" -> InformationSourceDiagnosticPolicy.renderUri(uri)) ++ sourceauthentication.map { source =>
      Map(
        "source_id" -> source.sourceId,
        "authentication_scheme" -> source.authentication.map(_.scheme).getOrElse(SourceAuthentication.NONE),
        "credential_configured" -> source.authentication.nonEmpty.toString
      )
    }.getOrElse(Map.empty)

  private def _bounded_response(
    sourceauthentication: Option[SourceAuthenticationRequest],
    uri: URI,
    response: Response,
    maxbytes: Option[Int]
  ): Consequence[String] = {
    val bodybytes = response.body.getBytes(StandardCharsets.UTF_8).length
    SourceAuthenticationFailure.fromHttp(
      sourceauthentication,
      response.statuscode,
      response.expiredchallenge
    ) match {
      case Some(failure) => failure.consequence
      case None if response.statuscode < 200 || response.statuscode >= 300 =>
        Consequence.serviceUnavailable(
          s"HTTP ${response.statuscode} while fetching ${InformationSourceDiagnosticPolicy.renderUri(uri)}"
        )
      case None if maxbytes.exists(bodybytes > _) =>
        Consequence.serviceUnavailable(
          s"HTTP response exceeds ${maxbytes.get} bytes while fetching ${InformationSourceDiagnosticPolicy.renderUri(uri)}"
        )
      case None =>
        Consequence.success(response.body)
    }
  }

  private final case class Response(
    statuscode: Int,
    body: String,
    expiredchallenge: Boolean
  )

  private final case class Call(
    core: ProviderCall.Core,
    uri: URI,
    body: Option[String],
    headers: Map[String, String],
    sourceauthentication: Option[SourceAuthenticationRequest]
  ) extends ProviderCall[Response] {
    protected def build_Program: ExecUowM[Response] =
      for {
        authenticationheaders <- provider_step(
          "resolve-source-authentication",
          sourceauthentication.map(source => Map(
            "source_id" -> source.sourceId,
            "authentication_scheme" -> source.authentication.map(_.scheme).getOrElse(SourceAuthentication.NONE)
          )).getOrElse(Map.empty)
        ) {
          sourceauthentication.fold(Consequence.success(Map.empty[String, String])) { source =>
            SourceAuthenticationHeaders.headersFor(
              source,
              uri,
              key => Option(provider_config_string(key, "")).filter(_.nonEmpty)
            )
          }
        }
        requestheaders = headers ++ authenticationheaders
        response <- body match {
          case Some(value) => http_post(uri.toString, Some(value), requestheaders, Vector(Property("http.timeout-seconds", "10", None)))
          case None => http_get(uri.toString, requestheaders, Vector(Property("http.timeout-seconds", "10", None)))
        }
      } yield Response(
        response.status.code,
        response.getString.getOrElse(""),
        SourceAuthenticationFailure.isExpiredChallenge(response.headerValue("WWW-Authenticate"))
      )
  }
}
