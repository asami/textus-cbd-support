package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.util.Locale

import org.goldenport.Consequence

/*
 * @since   Jul. 15, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class SourceAuthentication(
  scheme: String,
  credentialRef: String
) {
  require(SourceAuthentication.SUPPORTED_SCHEMES.contains(scheme), s"Unsupported source authentication scheme: $scheme")
  require(credentialRef.startsWith(SourceAuthentication.CREDENTIAL_REFERENCE_PREFIX), "Credential reference must use config-key/.")

  def configKey: String =
    credentialRef.stripPrefix(SourceAuthentication.CREDENTIAL_REFERENCE_PREFIX)
}

final case class SourceAuthenticationRequest(
  sourceId: String,
  authorizedUri: URI,
  authentication: Option[SourceAuthentication]
)

object SourceAuthenticationRequest {
  def from(source: CatalogSource): SourceAuthenticationRequest =
    SourceAuthenticationRequest(source.id, source.baseUri, source.authentication)

  def from(source: BokSource): SourceAuthenticationRequest =
    SourceAuthenticationRequest(source.id, source.baseUri, source.authentication)

  def from(source: SieBokSource): SourceAuthenticationRequest =
    SourceAuthenticationRequest(source.id, source.endpoint, source.authentication)
}

object SourceAuthenticationHeaders {
  val AUTHORIZATION = "Authorization"
  val API_KEY_HEADER = "X-Api-Key"

  def headersFor(
    source: SourceAuthenticationRequest,
    requesturi: URI,
    resolver: String => Option[String]
  ): Consequence[Map[String, String]] =
    source.authentication match {
      case None => Consequence.success(Map.empty)
      case Some(authentication) if !CatalogUriPolicy.isAuthorizedFetch(source.authorizedUri, requesturi) =>
        Consequence.securityPermissionDenied(
          s"Authentication for source ${source.sourceId} cannot be used outside its authorized origin."
        )
      case Some(authentication) =>
        resolver(authentication.configKey).filter(_is_header_value) match {
          case None =>
            Consequence.securityAuthenticationRequired(
              s"Authentication credential for source ${source.sourceId} is unavailable."
            )
          case Some(credential) =>
            Consequence.success(_headers(authentication.scheme, credential))
        }
    }

  private def _is_header_value(value: String): Boolean =
    value.nonEmpty && !value.exists(character => Character.isISOControl(character))

  private def _headers(scheme: String, credential: String): Map[String, String] =
    scheme match {
      case SourceAuthentication.BEARER => Map(AUTHORIZATION -> s"Bearer $credential")
      case SourceAuthentication.BASIC => Map(AUTHORIZATION -> s"Basic $credential")
      case SourceAuthentication.API_KEY => Map(API_KEY_HEADER -> credential)
    }
}

object SourceAuthentication {
  val NONE = "none"
  val BEARER = "bearer"
  val BASIC = "basic"
  val API_KEY = "api-key"
  val SUPPORTED_SCHEMES: Set[String] = Set(BEARER, BASIC, API_KEY)
  val CREDENTIAL_REFERENCE_PREFIX = "config-key/"
}

final case class SourceAuthenticationPolicy(
  maxBindings: Int = 32,
  maxCredentialReferenceCharacters: Int = 160
) {
  require(maxBindings > 0, "Source authentication binding limit must be positive.")
  require(
    maxCredentialReferenceCharacters > SourceAuthentication.CREDENTIAL_REFERENCE_PREFIX.length,
    "Credential reference character limit must admit a config key."
  )
}

object SourceAuthenticationPolicy {
  val DEFAULT: SourceAuthenticationPolicy = SourceAuthenticationPolicy()
}

final case class SourceAuthenticationConfiguration(
  authentications: Map[String, SourceAuthentication],
  warnings: Vector[String]
) {
  def authenticationFor(sourceid: String): Option[SourceAuthentication] =
    authentications.get(sourceid)
}

object SourceAuthenticationConfig {
  val ENVIRONMENT_KEY = "TEXTUS_CBD_SOURCE_AUTHENTICATION"

  def loadConfiguration(
    knownsourceids: Set[String],
    policy: SourceAuthenticationPolicy = SourceAuthenticationPolicy.DEFAULT
  ): SourceAuthenticationConfiguration =
    parse(sys.env.get(ENVIRONMENT_KEY), knownsourceids, policy)

  def parse(
    value: Option[String],
    knownsourceids: Set[String],
    policy: SourceAuthenticationPolicy = SourceAuthenticationPolicy.DEFAULT
  ): SourceAuthenticationConfiguration = {
    val entries = value.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val overflow = Option.when(entries.size > policy.maxBindings) {
      s"Source authentication configuration exceeds the limit of ${policy.maxBindings}."
    }.toVector
    val parsed = entries.take(policy.maxBindings).zipWithIndex.map { case (entry, index) =>
      _parse_entry(entry, index, knownsourceids, policy)
    }
    val initial = (Map.empty[String, SourceAuthentication], Vector.empty[String])
    val (authentications, duplicatewarnings) = parsed.collect { case Right(binding) => binding }
      .foldLeft(initial) { case ((accepted, warnings), (sourceid, authentication)) =>
        if (accepted.contains(sourceid))
          (accepted, warnings :+ s"Source authentication for $sourceid was rejected because its source ID is duplicated.")
        else
          (accepted.updated(sourceid, authentication), warnings)
      }
    SourceAuthenticationConfiguration(
      authentications,
      overflow ++ parsed.collect { case Left(warning) => warning } ++ duplicatewarnings
    )
  }

  private def _parse_entry(
    entry: String,
    index: Int,
    knownsourceids: Set[String],
    policy: SourceAuthenticationPolicy
  ): Either[String, (String, SourceAuthentication)] = {
    val pair = entry.split("=", 2)
    if (pair.length != 2 || !pair(0).trim.matches("[A-Za-z0-9._-]+"))
      Left(s"Source authentication entry ${index + 1} was rejected because its source ID is invalid.")
    else {
      val sourceid = pair(0).trim
      val authentication = pair(1).trim.split(":", 2)
      if (!knownsourceids.contains(sourceid))
        Left(s"Source authentication for $sourceid was rejected because the source ID is not configured.")
      else if (authentication.length != 2)
        Left(s"Source authentication for $sourceid was rejected because its binding is malformed.")
      else {
        val scheme = authentication(0).trim.toLowerCase(Locale.ROOT)
        val credentialref = authentication(1).trim
        if (!SourceAuthentication.SUPPORTED_SCHEMES.contains(scheme))
          Left(s"Source authentication for $sourceid was rejected because its scheme is unsupported.")
        else if (!_is_credential_reference(credentialref, policy))
          Left(s"Source authentication for $sourceid was rejected because its credential reference is invalid.")
        else
          Right(sourceid -> SourceAuthentication(scheme, credentialref))
      }
    }
  }

  private def _is_credential_reference(
    value: String,
    policy: SourceAuthenticationPolicy
  ): Boolean = {
    val prefix = SourceAuthentication.CREDENTIAL_REFERENCE_PREFIX
    val key = value.stripPrefix(prefix)
    value.startsWith(prefix) &&
      value.length <= policy.maxCredentialReferenceCharacters &&
      key.matches("[A-Za-z0-9][A-Za-z0-9._/-]*")
  }
}
