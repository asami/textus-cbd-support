package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant}
import scala.util.control.NonFatal

import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import org.goldenport.Consequence

/*
 * @since   Jul. 14, 2026
 * @version Jul. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class SieBokPolicy(
  maxSources: Int = 8,
  maxAllowedOrigins: Int = 16,
  maxTermsPerResponse: Int = 100,
  maxResponseBytes: Int = 2 * 1024 * 1024,
  maxQueryCharacters: Int = 1024,
  maxCategoryCharacters: Int = 256
) {
  require(maxSources > 0, "SIE source limit must be positive.")
  require(maxAllowedOrigins > 0, "SIE allowed-origin limit must be positive.")
  require(maxTermsPerResponse > 0, "SIE term limit must be positive.")
  require(maxResponseBytes > 0, "SIE response byte limit must be positive.")
  require(maxQueryCharacters > 0, "SIE query character limit must be positive.")
  require(maxCategoryCharacters > 0, "SIE category character limit must be positive.")
}

object SieBokPolicy {
  val DEFAULT: SieBokPolicy = SieBokPolicy()
}

final case class SieBokSource(
  id: String,
  endpoint: URI,
  priority: Int,
  enabled: Boolean,
  authentication: Option[SourceAuthentication] = None
) {
  def descriptor: InformationSourceDescriptor =
    InformationSourceDescriptor(
      id,
      InformationSourceKind.SIE_BOK,
      endpoint.toString,
      priority,
      enabled,
      InformationSourceAuthorization.COMPONENT_ROUTE_ALLOWLIST,
      authentication.map(_.scheme).getOrElse(SourceAuthentication.NONE),
      authentication.nonEmpty
    )
}

final case class SieBokConfiguration(
  sources: Vector[SieBokSource],
  warnings: Vector[String]
)

final case class SieBokTermEvidence(
  sourceId: String,
  id: String,
  title: String,
  definition: String,
  category: Option[String],
  termType: String,
  datasetId: String,
  matchKind: String,
  score: Double,
  rationale: String,
  evidenceUri: URI
)

final case class SieBokSnapshot(
  source: InformationSourceDescriptor,
  status: String,
  query: String,
  terms: Vector[SieBokTermEvidence],
  observedAt: Instant,
  warnings: Vector[String]
)

final case class SieBokSourceState(
  source: SieBokSource,
  status: String,
  termCount: Int,
  observedAt: Option[Instant],
  lastRefreshAttemptAt: Option[Instant],
  diagnostics: Vector[String]
) {
  def informationSourceState: InformationSourceState =
    InformationSourceState(
      source.descriptor,
      status,
      termCount,
      InformationSourceFreshness(
        if (!source.enabled) "disabled" else if (observedAt.nonEmpty) "observed" else "empty",
        observedAt,
        None,
        lastRefreshAttemptAt
      ),
      diagnostics
    )
}

trait SieBokTransport {
  def postJson(endpoint: URI, body: String, maxbytes: Int): Consequence[String]

  def postJson(
    source: SieBokSource,
    endpoint: URI,
    body: String,
    maxbytes: Int
  ): Consequence[String] =
    postJson(endpoint, body, maxbytes)
}

object SieBokConfig {
  private val _fixed_reserved_source_ids = Set("simplemodeling", "local-car", "cache-car")

  def loadConfiguration(
    reservedsourceids: Set[String],
    policy: SieBokPolicy = SieBokPolicy.DEFAULT
  ): SieBokConfiguration =
    parse(
      sys.env.get("TEXTUS_CBD_SIE_BOK_ROUTES"),
      sys.env.get("TEXTUS_CBD_SIE_ALLOWED_ORIGINS"),
      reservedsourceids,
      policy
    )

  def parse(
    routes: Option[String],
    allowedorigins: Option[String],
    reservedsourceids: Set[String] = _fixed_reserved_source_ids,
    policy: SieBokPolicy = SieBokPolicy.DEFAULT
  ): SieBokConfiguration = {
    val allowedentries = allowedorigins.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val allowedoverflow = Option.when(allowedentries.size > policy.maxAllowedOrigins) {
      s"SIE allowed-origin configuration exceeds the limit of ${policy.maxAllowedOrigins}."
    }.toVector
    val allowedresults = allowedentries.take(policy.maxAllowedOrigins).zipWithIndex.map { case (value, index) =>
      try {
        val uri = _http_uri(value)
        if (!Set("", "/").contains(Option(uri.getPath).getOrElse("")))
          Left(s"SIE allowlist entry ${index + 1} is not an origin without a path.")
        else Right(CatalogUriPolicy.origin(uri))
      } catch {
        case NonFatal(_) => Left(s"SIE allowlist entry ${index + 1} is not a valid HTTP(S) origin.")
      }
    }
    val allowed = allowedresults.collect { case Right(origin) => origin }.toSet
    val routeentries = routes.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val routeoverflow = Option.when(routeentries.size > policy.maxSources) {
      s"SIE source configuration exceeds the limit of ${policy.maxSources}."
    }.toVector
    val routeresults = routeentries.take(policy.maxSources).zipWithIndex.map { case (value, index) =>
      val pair = value.split("=", 2)
      val id = if (pair.length == 2) pair(0).trim else s"sie-${index + 1}"
      val routevalue = if (pair.length == 2) pair(1).trim else pair(0).trim
      if (!id.matches("[A-Za-z0-9._-]+"))
        Left(s"Configured SIE entry ${index + 1} was rejected because its source ID is invalid.")
      else {
        try {
          val endpoint = _http_uri(routevalue)
          val origin = CatalogUriPolicy.origin(endpoint)
          if (Option(endpoint.getPath).getOrElse("").stripSuffix("/") != "/mcp")
            Left(s"Configured SIE entry ${index + 1} was rejected because its public route must be /mcp.")
          else if (!allowed.contains(origin))
            Left(s"Configured SIE entry ${index + 1} was rejected because origin $origin is not allowlisted.")
          else Right(SieBokSource(id, endpoint, 700 + index, true))
        } catch {
          case NonFatal(_) => Left(s"Configured SIE entry ${index + 1} was rejected because its endpoint is invalid.")
        }
      }
    }
    val candidates = routeresults.collect { case Right(source) => source }
    val initial = (Vector.empty[SieBokSource], reservedsourceids, Vector.empty[String])
    val (sources, _, duplicatewarnings) = candidates.foldLeft(initial) { case ((accepted, ids, warnings), source) =>
      if (ids.contains(source.id))
        (accepted, ids, warnings :+ s"Configured SIE source ${source.id} was rejected because its source ID is reserved or duplicated.")
      else
        (accepted :+ source, ids + source.id, warnings)
    }
    SieBokConfiguration(
      sources.sortBy(x => (x.priority, x.id)),
      (allowedoverflow ++
        allowedresults.collect { case Left(warning) => warning } ++
        routeoverflow ++
        routeresults.collect { case Left(warning) => warning } ++
        duplicatewarnings).distinct
    )
  }

  private def _http_uri(value: String): URI = {
    val uri = URI.create(value)
    if (
      !Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase(java.util.Locale.ROOT)) ||
      uri.getHost == null || uri.getUserInfo != null || uri.getQuery != null || uri.getFragment != null
    ) throw new IllegalArgumentException("SIE route must be an absolute HTTP(S) URI.")
    uri
  }
}

final class SieBokProvider(clock: Clock = Clock.systemUTC()) {
  def searchTerms(
    source: SieBokSource,
    query: String,
    category: Option[String],
    limit: Int,
    transport: SieBokTransport,
    policy: SieBokPolicy = SieBokPolicy.DEFAULT
  ): Consequence[SieBokSnapshot] = {
    val boundedlimit = limit.max(1).min(policy.maxTermsPerResponse)
    _bounded_request(query, category, policy).flatMap { case (boundedquery, boundedcategory) =>
      val arguments = JsonObject.fromIterable(Vector(
        "query" -> Json.fromString(boundedquery),
        "limit" -> Json.fromInt(boundedlimit)
      ) ++ boundedcategory.map(x => "category" -> Json.fromString(x)))
      val request = Json.obj(
        "jsonrpc" -> Json.fromString("2.0"),
        "id" -> Json.fromString(s"cbd-${source.id}"),
        "method" -> Json.fromString("tools/call"),
        "params" -> Json.obj(
          "name" -> Json.fromString(SieBokProvider.SEARCH_TERMS_TOOL),
          "arguments" -> Json.fromJsonObject(arguments)
        )
      )
      transport.postJson(source, source.endpoint, request.noSpaces, policy.maxResponseBytes).flatMap { body =>
        _parse_response(source, boundedquery, body, boundedlimit, policy)
      }
    }
  }

  private def _bounded_request(
    query: String,
    category: Option[String],
    policy: SieBokPolicy
  ): Consequence[(String, Option[String])] = {
    val normalizedquery = query.trim
    val normalizedcategory = category.map(_.trim).filter(_.nonEmpty)
    if (normalizedquery.length > policy.maxQueryCharacters)
      Consequence.failure(s"SIE query exceeds ${policy.maxQueryCharacters} characters.")
    else if (normalizedcategory.exists(_.length > policy.maxCategoryCharacters))
      Consequence.failure(s"SIE category exceeds ${policy.maxCategoryCharacters} characters.")
    else
      Consequence.success(normalizedquery -> normalizedcategory)
  }

  private def _parse_response(
    source: SieBokSource,
    query: String,
    body: String,
    limit: Int,
    policy: SieBokPolicy
  ): Consequence[SieBokSnapshot] = {
    if (body.getBytes(StandardCharsets.UTF_8).length > policy.maxResponseBytes)
      Consequence.serviceUnavailable(s"SIE source ${source.id} response exceeds ${policy.maxResponseBytes} bytes.")
    else parse(body) match {
      case Left(_) => Consequence.serviceUnavailable(s"SIE source ${source.id} returned invalid JSON.")
      case Right(envelope) if envelope.hcursor.downField("error").focus.nonEmpty =>
        Consequence.serviceUnavailable(s"SIE source ${source.id} returned an MCP error.")
      case Right(envelope) =>
        val content = envelope.hcursor.downField("result").get[Vector[Json]]("content").toOption.getOrElse(Vector.empty)
        val text = content.headOption.flatMap(_.hcursor.get[String]("text").toOption)
        text.flatMap(value => parse(value).toOption) match {
          case None => Consequence.serviceUnavailable(s"SIE source ${source.id} returned no JSON text payload.")
          case Some(payload) => _snapshot(source, query, payload, limit)
        }
    }
  }

  private def _snapshot(
    source: SieBokSource,
    query: String,
    payload: Json,
    limit: Int
  ): Consequence[SieBokSnapshot] = {
    val cursor = payload.hcursor
    val status = cursor.get[String]("status").toOption.map(_.trim).filter(_.nonEmpty)
    val resultjsons = cursor.get[Vector[Json]]("results").toOption
    if (status.isEmpty || resultjsons.isEmpty)
      Consequence.serviceUnavailable(s"SIE source ${source.id} returned an invalid term response contract.")
    else {
      val parsed = resultjsons.get.take(limit).zipWithIndex.map { case (json, index) =>
        _term(source, json, index)
      }
      val failures = parsed.collect { case Left(warning) => warning }
      if (failures.nonEmpty)
        Consequence.serviceUnavailable(failures.mkString("; "))
      else Consequence.success(SieBokSnapshot(
        source.descriptor,
        status.get,
        cursor.get[String]("query").toOption.map(_.trim).filter(_.nonEmpty).getOrElse(query),
        parsed.collect { case Right(term) => term },
        clock.instant(),
        cursor.get[Vector[String]]("warnings").toOption.getOrElse(Vector.empty)
          .map(InformationSourceDiagnosticPolicy.sanitize).filter(_.nonEmpty)
      ))
    }
  }

  private def _term(source: SieBokSource, json: Json, index: Int): Either[String, SieBokTermEvidence] = {
    val cursor = json.hcursor
    val values = Vector("id", "title", "definition", "term_type", "dataset_id", "match_kind", "rationale", "evidence_uri")
      .map(name => name -> cursor.get[String](name).toOption.map(_.trim).filter(_.nonEmpty)).toMap
    val missing = values.collect { case (name, None) => name }.toVector.sorted
    val evidenceuri = values("evidence_uri").flatMap(_evidence_uri)
    if (missing.nonEmpty)
      Left(s"SIE source ${source.id} term ${index + 1} is missing required fields: ${missing.mkString(", ")}.")
    else if (evidenceuri.isEmpty)
      Left(s"SIE source ${source.id} term ${index + 1} has an invalid evidence URI.")
    else cursor.get[Double]("score").toOption match {
      case None => Left(s"SIE source ${source.id} term ${index + 1} has no numeric score.")
      case Some(score) => Right(SieBokTermEvidence(
        source.id,
        values("id").get,
        values("title").get,
        values("definition").get,
        cursor.get[String]("category").toOption.map(_.trim).filter(_.nonEmpty),
        values("term_type").get,
        values("dataset_id").get,
        values("match_kind").get,
        score,
        values("rationale").get,
        evidenceuri.get
      ))
    }
  }

  private def _evidence_uri(value: String): Option[URI] =
    try {
      val uri = URI.create(value)
      Option.when(uri.isAbsolute && uri.getUserInfo == null)(uri)
    } catch {
      case NonFatal(_) => None
    }
}

object SieBokProvider {
  val SEARCH_TERMS_TOOL = "SemanticIntegrationEngine.SemanticRetrieval.searchTerms"
}
