package org.simplemodeling.textus.cbdsupport.runtime

/*
 * @since   Jul. 16, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
/** A read-only cross-view projection. It never changes canonical review data. */
final case class CarReviewViewProjection(
  cncf: Vector[CarReviewViewItem],
  implementation: Vector[CarReviewViewItem],
  quality: Vector[CarReviewViewItem],
  namedViews: Vector[CarReviewNamedView]
) {
  def namedView(name: String): Option[CarReviewNamedView] = namedViews.find(_.name == name)
}

final case class CarReviewNamedView(
  name: String,
  items: Vector[CarReviewViewItem]
)

final case class CarReviewViewItem(
  key: String,
  observationIds: Vector[ReviewObservationId],
  evidenceIds: Vector[ReviewEvidenceId],
  providerLinks: Vector[CarReviewProviderLink],
  locations: Vector[ReviewLocation]
)

final case class CarReviewProviderLink(
  provider: ReviewProviderIdentity,
  ruleSet: ReviewRuleIdentity,
  bundleDigest: ReviewDigest
)

object CarReviewViewProjection {
  def project(report: CarReviewReport): CarReviewViewProjection = {
    val evidence = report.evidence.map(value => value.id -> value).toMap
    val namedviews = CarReviewCapabilityCatalog.viewNames.map { name =>
      CarReviewNamedView(name, _view(report, evidence, _capability_keys(name)))
    }
    CarReviewViewProjection(
      _view(report, evidence, _.mappings.cncfFeatures),
      _view(report, evidence, _.mappings.implementationSubjects),
      _view(report, evidence, _.mappings.qualityCapabilities.map(_.value)),
      namedviews
    )
  }

  private def _view(
    report: CarReviewReport,
    evidence: Map[ReviewEvidenceId, ReviewEvidence],
    keys: ReviewObservation => Vector[String]
  ): Vector[CarReviewViewItem] =
    report.observations.flatMap { observation =>
      keys(observation).map(_ -> observation)
    }.groupMap(_._1)(_._2).toVector.sortBy(_._1).map { case (key, observations) =>
      val observationids = observations.map(_.id).distinct.sortBy(_.value)
      val evidenceids = observations.flatMap(_.evidenceIds).distinct.sortBy(_.value)
      val providers = observations.map(_.provider).distinct.sortBy(value => (
        value.provider.id.value,
        value.provider.version.value,
        value.ruleSet.id.value,
        value.ruleSet.version.value,
        value.bundleDigest.value
      )).map(value => CarReviewProviderLink(value.provider, value.ruleSet, value.bundleDigest))
      val locations = (observations.flatMap(_.locations) ++ evidenceids.flatMap(evidence.get).flatMap(_.location)).distinct.sortBy(_location_key)
      CarReviewViewItem(key, observationids, evidenceids, providers, locations)
    }

  private def _capability_keys(view: String)(observation: ReviewObservation): Vector[String] = {
    val capabilities = CarReviewCapabilityCatalog.capabilityIdsForView(view)
    observation.mappings.qualityCapabilities.filter(capabilities.contains).map(_.value)
  }

  private def _location_key(value: ReviewLocation): (String, String, Int, Int, Int, Int) =
    (
      value.uri.getOrElse(""), value.path.getOrElse(""), value.line.getOrElse(-1),
      value.column.getOrElse(-1), value.endLine.getOrElse(-1), value.endColumn.getOrElse(-1)
    )
}
