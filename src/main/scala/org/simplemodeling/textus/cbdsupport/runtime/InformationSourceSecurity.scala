package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.util.Locale
import scala.util.control.NonFatal
import scala.util.matching.Regex

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object InformationSourceDiagnosticPolicy {
  private val _max_diagnostic_length = 2048
  private val _http_uri_pattern: Regex = "(?i)https?://[^\\s\\[\\]<>\"']+".r
  private val _authorization_pattern: Regex =
    "(?i)\\b(authorization)\\s*[:=]\\s*(?:bearer|basic)?\\s*[^\\s,;]+".r
  private val _bearer_pattern: Regex = "(?i)\\bbearer\\s+[-._~+/A-Za-z0-9]+=*".r
  private val _secret_assignment_pattern: Regex =
    "(?i)\\b((?:[A-Za-z0-9]+[_-])*(?:password|passwd|token|secret|api[_-]?key))\\s*[:=]\\s*[^\\s,;]+".r

  def sanitize(value: String): String = {
    val normalized = value.map(character => if (Character.isISOControl(character)) ' ' else character)
      .mkString.replaceAll("\\s+", " ").trim
    val urisafe = _http_uri_pattern.replaceAllIn(normalized, matched =>
      try renderUri(URI.create(matched.matched))
      catch {
        case NonFatal(_) => "[redacted-uri]"
      }
    )
    val bearerredacted = _bearer_pattern.replaceAllIn(urisafe, "Bearer [redacted]")
    val authorizationredacted = _authorization_pattern.replaceAllIn(
      bearerredacted,
      matched => s"${matched.group(1).toLowerCase(Locale.ROOT)}=[redacted]"
    )
    val redacted = _secret_assignment_pattern.replaceAllIn(
      authorizationredacted,
      matched => s"${matched.group(1).toLowerCase(Locale.ROOT)}=[redacted]"
    )
    if (redacted.length <= _max_diagnostic_length) redacted
    else redacted.take(_max_diagnostic_length) + " [truncated]"
  }

  def renderUri(uri: URI): String = {
    val scheme = Option(uri.getScheme).map(_.toLowerCase(Locale.ROOT))
    if (scheme.exists(Set("http", "https")) && uri.getHost != null)
      new URI(
        scheme.get,
        null,
        uri.getHost.toLowerCase(Locale.ROOT),
        uri.getPort,
        Option(uri.getPath).getOrElse(""),
        null,
        null
      ).toASCIIString
    else if (uri.getUserInfo == null && uri.getQuery == null && uri.getFragment == null)
      uri.toASCIIString
    else
      scheme.fold("[redacted-uri]")(x => s"$x:[redacted]")
  }
}
