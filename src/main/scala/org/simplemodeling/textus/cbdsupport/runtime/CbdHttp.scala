package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

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
final class CbdHttp(actioncore: ActionCall.Core) extends CatalogFetcher {
  def get(uri: URI): Consequence[String] = {
    ProviderEngine.execute(Call(
      ProviderCall.Core(
        ProviderRequest("cbd-catalog-http", "get", Map("url" -> uri.toString)),
        actioncore.executionContext,
        actioncore.component,
        actioncore.correlationId
      ),
      uri
    )).flatMap { response =>
      if (response.statuscode >= 200 && response.statuscode < 300)
        Consequence.success(response.body)
      else
        Consequence.serviceUnavailable(s"HTTP ${response.statuscode} while fetching $uri")
    }
  }

  private final case class Response(statuscode: Int, body: String)

  private final case class Call(
    core: ProviderCall.Core,
    uri: URI
  ) extends ProviderCall[Response] {
    protected def build_Program: ExecUowM[Response] =
      for {
        response <- http_get(uri.toString, Map.empty, Vector(Property("http.timeout-seconds", "10", None)))
      } yield Response(response.status.code, response.getString.getOrElse(""))
  }
}
