package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ComponentEvidenceAbsence(
  code: String,
  subject: String,
  message: String,
  sourceIds: Vector[String],
  versions: Vector[String],
  evidenceUris: Vector[URI]
)

final case class ExactComponentSelection(
  status: String,
  selectedProfile: Option[ComponentProfile],
  alternatives: Vector[ComponentProfile],
  candidateCount: Int,
  absences: Vector[ComponentEvidenceAbsence],
  warnings: Vector[String]
)

object ExactComponentSelection {
  val MAXIMUM_ALTERNATIVES = 20

  val AMBIGUOUS_SELECTION = "ambiguous-selection"
  val COMPONENT_NOT_FOUND = "component-not-found"
  val INTENT_MATCH_ABSENT = "intent-match-absent"
  val INTENT_REJECTED = "intent-rejected"
  val OPERATION_EVIDENCE_ABSENT = "operation-evidence-absent"
  val SOURCE_ATTRIBUTION_ABSENT = "source-attribution-absent"
  val SELECTED_VERSION_ABSENT = "selected-version-absent"
  val DEPENDENCY_METADATA_ABSENT = "dependency-metadata-absent"

  def fromCandidates(candidates: Vector[ComponentProfile]): ExactComponentSelection =
    candidates match {
      case Vector() =>
        ExactComponentSelection(
          "no-match",
          None,
          Vector.empty,
          0,
          Vector(ComponentEvidenceAbsence(
            COMPONENT_NOT_FOUND,
            "component-selection",
            "No catalog component satisfies the requested exact identity and version constraints.",
            Vector.empty,
            Vector.empty,
            Vector.empty
          )),
          Vector.empty
        )
      case Vector(profile) =>
        ExactComponentSelection("matched", Some(profile), Vector.empty, 1, Vector.empty, Vector.empty)
      case values =>
        val alternatives = values.take(MAXIMUM_ALTERNATIVES)
        ExactComponentSelection(
          "ambiguous",
          None,
          alternatives,
          values.size,
          Vector(ComponentEvidenceAbsence(
            AMBIGUOUS_SELECTION,
            "component-selection",
            "Multiple catalog components satisfy the requested exact constraints; select a catalog ID or refine the identity.",
            alternatives.map(_.catalogId).distinct,
            alternatives.flatMap(_.selectedVersion).distinct,
            alternatives.map(_.evidenceUri).distinct
          )),
          Option.when(values.size > MAXIMUM_ALTERNATIVES) {
            s"Exact component alternatives were truncated at $MAXIMUM_ALTERNATIVES of ${values.size} candidates."
          }.toVector
        )
    }
}
