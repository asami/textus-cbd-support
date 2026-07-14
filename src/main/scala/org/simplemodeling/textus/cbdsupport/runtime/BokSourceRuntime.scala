package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant}
import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final case class BokInspectionPolicy(
  maxSources: Int = 16,
  maxAllowedOrigins: Int = 32,
  maxResources: Int = 64,
  maxTerms: Int = 10000,
  maxManifestBytes: Int = 1024 * 1024,
  maxResourceBytes: Int = 8 * 1024 * 1024
) {
  require(maxSources > 0, "BoK source limit must be positive.")
  require(maxAllowedOrigins > 0, "BoK allowed-origin limit must be positive.")
  require(maxResources > 0, "BoK resource limit must be positive.")
  require(maxTerms > 0, "BoK term limit must be positive.")
  require(maxManifestBytes > 0, "BoK manifest byte limit must be positive.")
  require(maxResourceBytes > 0, "BoK resource byte limit must be positive.")
}

object BokInspectionPolicy {
  val DEFAULT: BokInspectionPolicy = BokInspectionPolicy()
}

final case class BokSource(
  id: String,
  baseUri: URI,
  priority: Int,
  enabled: Boolean
) {
  def descriptor: InformationSourceDescriptor =
    InformationSourceDescriptor(
      id,
      InformationSourceKind.BOK_SITE,
      baseUri.toString,
      priority,
      enabled,
      InformationSourceAuthorization.EXACT_ORIGIN_ALLOWLIST
    )
}

final case class BokSourceConfiguration(
  sources: Vector[BokSource],
  warnings: Vector[String]
)

final case class BokKnowledgeResource(
  kind: String,
  uri: URI,
  mediaType: String
)

final case class BokTermObservation(
  sourceId: String,
  manifestId: String,
  termId: String,
  slug: Option[String],
  title: Option[String],
  reading: Option[String],
  category: Option[String],
  summary: Option[String],
  aliases: Vector[String],
  termType: Option[String],
  articleRefs: Vector[String],
  termRefs: Vector[String],
  rdfRefs: Vector[String],
  videoRefs: Vector[String],
  evidenceLocation: String,
  diagnostics: Vector[String]
)

final case class BokSourceSnapshot(
  source: InformationSourceDescriptor,
  manifestId: String,
  label: Option[String],
  sourceRefValue: Option[String],
  sourceRefUri: Option[URI],
  manifestUri: URI,
  resources: Vector[BokKnowledgeResource],
  terms: Vector[BokTermObservation],
  observedAt: Instant,
  warnings: Vector[String]
)

final case class BokSourceState(
  source: BokSource,
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

trait BokFetcher {
  def get(uri: URI, maxbytes: Int): Consequence[String]
}

object BokSourceConfig {
  private val _fixed_reserved_source_ids = Set("simplemodeling", "local-car", "cache-car")

  def loadConfiguration(
    policy: BokInspectionPolicy = BokInspectionPolicy.DEFAULT
  ): BokSourceConfiguration = {
    val catalogids = CatalogSourceConfig.loadConfiguration().sources.map(_.id).toSet
    loadConfiguration(catalogids ++ _fixed_reserved_source_ids, policy)
  }

  def loadConfiguration(
    reservedsourceids: Set[String]
  ): BokSourceConfiguration =
    loadConfiguration(reservedsourceids, BokInspectionPolicy.DEFAULT)

  def loadConfiguration(
    reservedsourceids: Set[String],
    policy: BokInspectionPolicy
  ): BokSourceConfiguration =
    parse(
      sys.env.get("TEXTUS_CBD_BOK_SITES"),
      sys.env.get("TEXTUS_CBD_BOK_ALLOWED_ORIGINS"),
      reservedsourceids,
      policy
    )

  def parse(
    sites: Option[String],
    allowedorigins: Option[String],
    reservedsourceids: Set[String] = _fixed_reserved_source_ids,
    policy: BokInspectionPolicy = BokInspectionPolicy.DEFAULT
  ): BokSourceConfiguration = {
    val allowedentries = allowedorigins.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val allowedoverflow = Option.when(allowedentries.size > policy.maxAllowedOrigins) {
      s"BoK allowed-origin configuration exceeds the limit of ${policy.maxAllowedOrigins}."
    }.toVector
    val allowedresults = allowedentries.take(policy.maxAllowedOrigins).zipWithIndex.map { case (value, index) =>
      try {
        val uri = _base_uri(value)
        if (!Set("", "/").contains(Option(uri.getPath).getOrElse("")))
          Left(s"BoK allowlist entry ${index + 1} is not an origin without a path.")
        else Right(CatalogUriPolicy.origin(uri))
      } catch {
        case NonFatal(_) => Left(s"BoK allowlist entry ${index + 1} is not a valid HTTP(S) origin.")
      }
    }
    val allowed = allowedresults.collect { case Right(origin) => origin }.toSet
    val allowwarnings = allowedresults.collect { case Left(warning) => warning }
    val siteentries = sites.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val siteoverflow = Option.when(siteentries.size > policy.maxSources) {
      s"BoK source configuration exceeds the limit of ${policy.maxSources}."
    }.toVector
    val siteresults = siteentries.take(policy.maxSources).zipWithIndex.map { case (value, index) =>
      val pair = value.split("=", 2)
      val id = if (pair.length == 2) pair(0).trim else s"bok-${index + 1}"
      val urivalue = if (pair.length == 2) pair(1).trim else pair(0).trim
      if (!id.matches("[A-Za-z0-9._-]+"))
        Left(s"Configured BoK entry ${index + 1} was rejected because its source ID is invalid.")
      else {
        try {
          val uri = _base_uri(urivalue)
          val origin = CatalogUriPolicy.origin(uri)
          if (allowed.contains(origin)) Right(BokSource(id, uri, 600 + index, true))
          else Left(s"Configured BoK entry ${index + 1} was rejected because origin $origin is not allowlisted.")
        } catch {
          case NonFatal(_) => Left(s"Configured BoK entry ${index + 1} was rejected because its base URI is invalid.")
        }
      }
    }
    val candidates = siteresults.collect { case Right(source) => source }
    val rejectionwarnings = siteresults.collect { case Left(warning) => warning }
    val initial = (Vector.empty[BokSource], reservedsourceids, Vector.empty[String])
    val (sources, _, duplicatewarnings) = candidates.foldLeft(initial) { case ((accepted, ids, warnings), source) =>
      if (ids.contains(source.id))
        (accepted, ids, warnings :+ s"Configured BoK source ${source.id} was rejected because its source ID is reserved or duplicated.")
      else
        (accepted :+ source, ids + source.id, warnings)
    }
    BokSourceConfiguration(
      sources.sortBy(x => (x.priority, x.id)),
      (allowedoverflow ++ allowwarnings ++ siteoverflow ++ rejectionwarnings ++ duplicatewarnings).distinct
    )
  }

  private def _base_uri(value: String): URI = {
    val normalized = if (value.endsWith("/")) value else s"$value/"
    val uri = URI.create(normalized)
    if (
      !Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase(java.util.Locale.ROOT)) ||
      uri.getHost == null ||
      uri.getUserInfo != null ||
      uri.getQuery != null ||
      uri.getFragment != null
    ) throw new IllegalArgumentException("BoK source URI must be an absolute HTTP(S) base URI.")
    uri
  }
}

final class BokKnowledgeSourceProvider(clock: Clock = Clock.systemUTC()) {
  private final case class ParsedManifest(
    manifestid: String,
    label: Option[String],
    sourcerefvalue: Option[String],
    sourcerefuri: Option[URI],
    resources: Vector[BokKnowledgeResource],
    warnings: Vector[String]
  )

  def read(
    source: BokSource,
    fetcher: BokFetcher,
    policy: BokInspectionPolicy = BokInspectionPolicy.DEFAULT
  ): Consequence[BokSourceSnapshot] = {
    val manifesturi = source.baseUri.resolve(BokKnowledgeSourceProvider.MANIFEST_PATH)
    fetcher.get(manifesturi, policy.maxManifestBytes).flatMap { body =>
      _bounded_json(body, policy.maxManifestBytes, "BoK KnowledgeSource manifest").flatMap(_parse_manifest(source, _, policy)) match {
        case Left(warning) => Consequence.serviceUnavailable(s"BoK source ${source.id}: $warning")
        case Right(manifest) =>
          val glossaryresources = manifest.resources.filter(_.kind == "glossary-terms")
          val glossarywarning = Option.when(glossaryresources.isEmpty) {
            "KnowledgeSource manifest declares no glossary-terms resource."
          }.toVector
          val results = glossaryresources.map(_read_terms(source, manifest.manifestid, _, fetcher, policy))
          val terms = results.flatMap(_._1)
          val warnings = (
            manifest.warnings ++ glossarywarning ++ results.flatMap(_._2) ++ _duplicate_term_warnings(terms)
          ).distinct
          Consequence.success(BokSourceSnapshot(
            source.descriptor,
            manifest.manifestid,
            manifest.label,
            manifest.sourcerefvalue,
            manifest.sourcerefuri,
            manifesturi,
            manifest.resources,
            _mark_duplicate_terms(terms),
            clock.instant(),
            warnings
          ))
      }
    }
  }

  private def _parse_manifest(
    source: BokSource,
    json: Json,
    policy: BokInspectionPolicy
  ): Either[String, ParsedManifest] = {
    val cursor = json.hcursor
    val schemaversion = _string(json, "schemaVersion")
    val kind = _string(json, "kind")
    val manifestid = _string(json, "id")
    if (schemaversion != Some("cncf.knowledge-source.v1"))
      Left("KnowledgeSource manifest has an unsupported schemaVersion.")
    else if (kind != Some("bok-site"))
      Left("KnowledgeSource manifest kind must be bok-site.")
    else if (manifestid.isEmpty)
      Left("KnowledgeSource manifest has no stable id.")
    else if (cursor.downField("sourceRef").get[String]("kind").toOption.map(_.trim) != Some("bok-site"))
      Left("KnowledgeSource manifest sourceRef.kind must be bok-site.")
    else if (cursor.downField("sourceRef").get[String]("value").toOption.map(_.trim).filter(_.nonEmpty).isEmpty)
      Left("KnowledgeSource manifest sourceRef.value is required.")
    else if (cursor.downField("sourceRef").get[String]("value").toOption.map(_.trim) != manifestid)
      Left("KnowledgeSource manifest id and sourceRef.value must identify the same BoK site.")
    else cursor.get[Vector[Json]]("resources").toOption match {
      case None => Left("KnowledgeSource manifest resources must be an array.")
      case Some(resourcejsons) =>
        val overflow = Option.when(resourcejsons.size > policy.maxResources) {
          s"KnowledgeSource resources were truncated at ${policy.maxResources} entries."
        }.toVector
        val parsedresources = resourcejsons.take(policy.maxResources).zipWithIndex.map { case (resourcejson, index) =>
          _parse_resource(source, resourcejson, index)
        }
        val resources = parsedresources.collect { case Right(resource) => resource }
        val resourcewarnings = parsedresources.collect { case Left(warning) => warning }
        val sourceref = cursor.downField("sourceRef")
        val sourcerefvalue = sourceref.get[String]("value").toOption.map(_.trim).filter(_.nonEmpty)
        val (sourcerefuri, sourcerefwarnings) = sourceref.get[String]("uri").toOption match {
          case None => None -> Vector.empty[String]
          case Some(value) => _source_ref_uri(source, value)
        }
        val identitywarning = Option.when(manifestid.get != source.id) {
          s"Configured source ID ${source.id} differs from manifest ID ${manifestid.get}; both identities remain separate."
        }.toVector
        Right(ParsedManifest(
          manifestid.get,
          _string(json, "label"),
          sourcerefvalue,
          sourcerefuri,
          resources,
          overflow ++ resourcewarnings ++ sourcerefwarnings ++ identitywarning
        ))
    }
  }

  private def _parse_resource(
    source: BokSource,
    json: Json,
    index: Int
  ): Either[String, BokKnowledgeResource] = {
    val kind = _string(json, "kind")
    val href = _string(json, "href")
    val mediatype = _string(json, "mediaType")
    if (kind.isEmpty || href.isEmpty || mediatype.isEmpty)
      Left(s"KnowledgeSource resource ${index + 1} was rejected because kind, href, or mediaType is missing.")
    else _resource_uri(source.baseUri, href.get, index).flatMap { uri =>
      if (kind.contains("glossary-terms") && !_is_json(mediatype.get))
        Left(s"KnowledgeSource resource ${index + 1} was rejected because glossary-terms must use application/json.")
      else Right(BokKnowledgeResource(kind.get, uri, mediatype.get))
    }
  }

  private def _resource_uri(baseuri: URI, value: String, index: Int): Either[String, URI] = {
    try {
      val reference = URI.create(value)
      val path = Option(reference.getPath).getOrElse("")
      val segments = path.split("/").toVector
      if (
        reference.isAbsolute || reference.getRawAuthority != null || path.isEmpty || path.startsWith("/") ||
        reference.getQuery != null || reference.getFragment != null || segments.contains("..")
      ) Left(s"KnowledgeSource resource ${index + 1} was rejected because href must be a safe relative path.")
      else {
        val resolved = baseuri.resolve(reference).normalize()
        val basepath = Option(baseuri.normalize().getPath).getOrElse("/")
        if (!CatalogUriPolicy.sameOrigin(baseuri, resolved) || !resolved.getPath.startsWith(basepath))
          Left(s"KnowledgeSource resource ${index + 1} was rejected because href escapes the configured source base.")
        else Right(resolved)
      }
    } catch {
      case NonFatal(_) => Left(s"KnowledgeSource resource ${index + 1} was rejected because href is invalid.")
    }
  }

  private def _source_ref_uri(source: BokSource, value: String): (Option[URI], Vector[String]) = {
    try {
      val uri = URI.create(value)
      if (
        uri.getUserInfo != null || uri.getQuery != null || uri.getFragment != null ||
        !CatalogUriPolicy.sameOrigin(source.baseUri, uri)
      ) None -> Vector("KnowledgeSource sourceRef.uri was ignored because it is not on the configured source origin.")
      else Some(uri) -> Vector.empty
    } catch {
      case NonFatal(_) => None -> Vector("KnowledgeSource sourceRef.uri was ignored because it is invalid.")
    }
  }

  private def _read_terms(
    source: BokSource,
    manifestid: String,
    resource: BokKnowledgeResource,
    fetcher: BokFetcher,
    policy: BokInspectionPolicy
  ): (Vector[BokTermObservation], Vector[String]) = {
    fetcher.get(resource.uri, policy.maxResourceBytes) match {
      case Consequence.Success(body) =>
        _bounded_json(body, policy.maxResourceBytes, "glossary-terms resource") match {
          case Left(warning) => Vector.empty -> Vector(s"BoK source ${source.id}: $warning")
          case Right(json) => _parse_terms(source, manifestid, resource, json, policy)
        }
      case Consequence.Failure(conclusion) =>
        Vector.empty -> Vector(s"BoK source ${source.id} could not read glossary-terms resource: ${conclusion.display}")
    }
  }

  private def _parse_terms(
    source: BokSource,
    manifestid: String,
    resource: BokKnowledgeResource,
    json: Json,
    policy: BokInspectionPolicy
  ): (Vector[BokTermObservation], Vector[String]) = {
    json.hcursor.get[Vector[Json]]("terms").toOption match {
      case None => Vector.empty -> Vector(s"BoK source ${source.id} glossary-terms document has no terms array.")
      case Some(termjsons) =>
        val overflow = Option.when(termjsons.size > policy.maxTerms) {
          s"BoK source ${source.id} glossary terms were truncated at ${policy.maxTerms} entries."
        }.toVector
        val parsedterms = termjsons.take(policy.maxTerms).zipWithIndex.map { case (termjson, index) =>
          _parse_term(source, manifestid, resource, termjson, index)
        }
        parsedterms.collect { case Right(term) => term } ->
          (overflow ++ parsedterms.collect { case Left(warning) => warning })
    }
  }

  private def _parse_term(
    source: BokSource,
    manifestid: String,
    resource: BokKnowledgeResource,
    json: Json,
    index: Int
  ): Either[String, BokTermObservation] = {
    val termid = _string(json, "id").orElse(_string(json, "slug"))
    if (termid.isEmpty)
      Left(s"BoK source ${source.id} glossary term ${index + 1} was rejected because id and slug are absent.")
    else {
      val title = _string(json, "title")
      val diagnostics = Option.when(title.isEmpty)("Glossary term has no title.").toVector
      Right(BokTermObservation(
        source.id,
        manifestid,
        termid.get,
        _string(json, "slug"),
        title,
        _string(json, "reading"),
        _string(json, "category"),
        _string(json, "summary"),
        _strings(json, "aliases"),
        _string(json, "term_type"),
        _strings(json, "article_refs"),
        _strings(json, "term_refs"),
        _strings(json, "rdf_refs"),
        _strings(json, "video_refs"),
        s"${resource.uri}#/terms/$index",
        diagnostics
      ))
    }
  }

  private def _duplicate_term_ids(terms: Vector[BokTermObservation]): Set[String] =
    terms.groupBy(_.termId.toLowerCase(java.util.Locale.ROOT)).collect {
      case (termid, entries) if entries.size > 1 => termid
    }.toSet

  private def _duplicate_term_warnings(terms: Vector[BokTermObservation]): Vector[String] =
    _duplicate_term_ids(terms).toVector.sorted.map { termid =>
      s"Duplicate glossary term identity remains unresolved: $termid."
    }

  private def _mark_duplicate_terms(terms: Vector[BokTermObservation]): Vector[BokTermObservation] = {
    val duplicates = _duplicate_term_ids(terms)
    terms.map { term =>
      if (duplicates.contains(term.termId.toLowerCase(java.util.Locale.ROOT)))
        term.copy(diagnostics = term.diagnostics :+ "Duplicate term identity is preserved without selecting a winner.")
      else term
    }
  }

  private def _bounded_json(body: String, maxbytes: Int, label: String): Either[String, Json] = {
    if (body.getBytes(StandardCharsets.UTF_8).length > maxbytes)
      Left(s"$label exceeds $maxbytes bytes.")
    else parse(body).left.map(_ => s"$label is not valid JSON.")
  }

  private def _string(json: Json, field: String): Option[String] =
    json.hcursor.get[String](field).toOption.map(_.trim).filter(_.nonEmpty)

  private def _strings(json: Json, field: String): Vector[String] =
    json.hcursor.get[Vector[String]](field).toOption.getOrElse(Vector.empty)
      .map(_.trim).filter(_.nonEmpty).distinct

  private def _is_json(value: String): Boolean =
    value.split(";", 2).head.trim.equalsIgnoreCase("application/json")
}

object BokKnowledgeSourceProvider {
  val MANIFEST_PATH = "metadata/cncf/knowledge-source.json"
}
