package org.simplemodeling.textus.cbdsupport.runtime

import java.time.{Clock, Duration, Instant}
import java.util.Locale

/*
 * @since   Jul. 14, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class SemanticRequirementEvidence(
  id: String,
  sourceId: String,
  sourceKind: String,
  termId: String,
  title: Option[String],
  definition: Option[String],
  category: Option[String],
  aliases: Vector[String],
  datasetId: Option[String],
  matchKind: String,
  score: Double,
  rationale: String,
  freshness: String,
  observedAt: Instant,
  evidenceLocation: String,
  diagnostics: Vector[String]
) {
  def labels: Vector[String] =
    (Vector(Some(termId), title) ++ aliases.map(Some(_))).flatten.map(_.trim).filter(_.nonEmpty).distinct
}

object SemanticRequirementMatcher {
  val MAXIMUM_EVIDENCE = 100

  def matchEvidence(
    requirement: String,
    boksnapshots: Vector[BokSourceSnapshot],
    siesnapshots: Vector[SieBokSnapshot],
    limit: Int,
    clock: Clock,
    bokttl: Duration = BokInspectionPolicy.DEFAULT.refreshTtl,
    sourceid: Option[String] = None,
    sourcekind: Option[String] = None
  ): Vector[SemanticRequirementEvidence] = {
    val bokevidence = boksnapshots.flatMap { snapshot =>
      val freshness = if (clock.instant().isBefore(snapshot.observedAt.plus(bokttl))) "fresh" else "stale"
      snapshot.terms.flatMap(_match_bok_term(requirement, _, freshness, snapshot.observedAt))
    }
    val sieevidence = siesnapshots.filter(_.query.trim.equalsIgnoreCase(requirement.trim)).flatMap { snapshot =>
      snapshot.terms.map(_from_sie_term(_, snapshot.observedAt))
    }
    (bokevidence ++ sieevidence).filter { evidence =>
      sourceid.forall(_ == evidence.sourceId) && sourcekind.forall(_ == evidence.sourceKind)
    }
      .sortBy(evidence => (-evidence.score, evidence.sourceId, evidence.termId, evidence.evidenceLocation))
      .take(limit.max(1).min(MAXIMUM_EVIDENCE))
  }

  def matchingEvidenceIds(
    profile: ComponentProfile,
    evidence: Vector[SemanticRequirementEvidence]
  ): Vector[String] = {
    val declaredlabels = (profile.terms ++ profile.tags).map(_normalize).filter(_.nonEmpty).toSet
    evidence.filter { citation =>
      citation.labels.exists(label => declaredlabels.contains(_normalize(label)))
    }.map(_.id).distinct
  }

  private def _match_bok_term(
    requirement: String,
    term: BokTermObservation,
    freshness: String,
    observedat: Instant
  ): Option[SemanticRequirementEvidence] = {
    val labels = (Vector(Some(term.termId), term.slug, term.title, term.reading) ++ term.aliases.map(Some(_))).flatten
    val exact = labels.exists(_normalize(_) == _normalize(requirement))
    val querytokens = _tokens(requirement)
    val evidencewords = (labels ++ term.summary).mkString(" ")
    val matchedtokens = querytokens.intersect(_tokens(evidencewords))
    val score = if (exact) 1.0 else if (querytokens.isEmpty) 0.0 else matchedtokens.size.toDouble / querytokens.size.toDouble
    Option.when(score > 0.0)(SemanticRequirementEvidence(
      _evidence_id(term.sourceId, term.termId, term.evidenceLocation),
      term.sourceId,
      InformationSourceKind.BOK_SITE,
      term.termId,
      term.title,
      term.summary,
      term.category,
      term.aliases,
      None,
      if (exact) "exact" else "candidate",
      score,
      if (exact) s"Exact published BoK label match for ${term.termId}."
      else s"BoK term metadata matched ${matchedtokens.toVector.sorted.mkString(", ")}.",
      freshness,
      observedat,
      term.evidenceLocation,
      term.diagnostics
    ))
  }

  private def _from_sie_term(term: SieBokTermEvidence, observedat: Instant): SemanticRequirementEvidence =
    SemanticRequirementEvidence(
      _evidence_id(term.sourceId, term.id, term.evidenceUri.toString),
      term.sourceId,
      InformationSourceKind.SIE_BOK,
      term.id,
      Some(term.title),
      Some(term.definition),
      term.category,
      Vector.empty,
      Some(term.datasetId),
      term.matchKind,
      term.score,
      term.rationale,
      "observed",
      observedat,
      term.evidenceUri.toString,
      Vector.empty
    )

  private def _evidence_id(sourceid: String, termid: String, location: String): String =
    s"$sourceid::$termid::$location"

  private def _tokens(value: String): Set[String] =
    _normalize(value).split("[^\\p{L}\\p{N}._:-]+").toSet.map(_.trim).filter(_.nonEmpty)

  private def _normalize(value: String): String =
    value.trim.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
}
