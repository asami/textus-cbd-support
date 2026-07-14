package org.simplemodeling.textus.cbdsupport.runtime

import java.net.URI

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
object IntentAwareUsageGuidance {
  val OBSERVED_FACT = "observed-fact"
  val DETERMINISTIC_INFERENCE = "deterministic-inference"
  val MODEL_INFERENCE = "model-inference"

  val MAXIMUM_INTENT_LENGTH = 512
  val MAXIMUM_INTENT_TOKENS = 32
  val MAXIMUM_GUIDANCE_RECORDS = 32

  private val _uninformative_tokens = Set(
    "a", "an", "and", "component", "for", "from", "in", "of", "operation",
    "service", "the", "to", "use", "using", "want", "with"
  )

  def enrich(
    usage: ComponentUsage,
    requestedintent: Option[String]
  ): ComponentUsage = {
    val intent = requestedintent.map(_.trim).filter(_.nonEmpty)
    val intentwarning = intent.filter(_.length > MAXIMUM_INTENT_LENGTH).map { _ =>
      s"Usage intent exceeds $MAXIMUM_INTENT_LENGTH characters; inference was not produced."
    }.toVector
    val boundedintent = intent.filter(_.length <= MAXIMUM_INTENT_LENGTH)
    val context = usage.profile.observationContext
    val sourceid = context.map(_.sourceId)
    val sourcekind = context.map(_.sourceKind)
    val version = usage.profile.selectedVersion
    val selectionwarnings = Vector(
      Option.when(sourceid.isEmpty)("Selected usage source identity is absent; attributable guidance was not produced."),
      Option.when(sourcekind.isEmpty)("Selected usage source kind is absent; attributable guidance was not produced."),
      Option.when(version.isEmpty)("Selected usage version is absent; guidance does not claim a component version.")
    ).flatten
    val observed = for {
      id <- sourceid.toVector
      kind <- sourcekind.toVector
    } yield ComponentUsageGuidance(
      OBSERVED_FACT,
      boundedintent,
      s"Usage evidence was read from source $id${version.fold("")(x => s" for version $x")}.",
      id,
      kind,
      version,
      None,
      None,
      None,
      Vector(usage.profile.evidenceUri),
      "The exact catalog lookup and selected profile provide this source and version evidence."
    )
    val inferred = for {
      selectedintent <- boundedintent.toVector
      id <- sourceid.toVector
      kind <- sourcekind.toVector
      result <- _matching_operations(usage, selectedintent)
    } yield {
      val operationlabel = result.operation.service
        .map(x => s"$x.${result.operation.operation}")
        .getOrElse(result.operation.operation)
      ComponentUsageGuidance(
        DETERMINISTIC_INFERENCE,
        Some(selectedintent),
        s"The observed operation $operationlabel is a candidate for the requested intent.",
        id,
        kind,
        version,
        result.operation.service,
        Some(result.operation.operation),
        Some(result.score),
        _operation_evidence_uris(usage),
        s"Intent terms matched observed operation metadata: ${result.matchedtokens.mkString(", ")}."
      )
    }
    usage.copy(
      warnings = (usage.warnings ++ intentwarning ++ selectionwarnings).distinct,
      intent = boundedintent,
      selectedSourceId = sourceid,
      selectedSourceKind = sourcekind,
      selectedVersion = version,
      guidance = (observed ++ inferred).take(MAXIMUM_GUIDANCE_RECORDS)
    )
  }

  private final case class OperationMatch(
    operation: ComponentOperation,
    matchedtokens: Vector[String],
    score: Double
  )

  private def _matching_operations(
    usage: ComponentUsage,
    intent: String
  ): Vector[OperationMatch] = {
    val intenttokens = _tokens(intent).take(MAXIMUM_INTENT_TOKENS).toSet
    usage.operations.flatMap { operation =>
      val operationtokens = _tokens(Vector(
        operation.service,
        Some(operation.operation),
        operation.kind,
        operation.description
      ).flatten.mkString(" ")).toSet
      val matched = intenttokens.intersect(operationtokens).toVector.sorted
      Option.when(intenttokens.nonEmpty && matched.nonEmpty) {
        OperationMatch(operation, matched, matched.size.toDouble / intenttokens.size.toDouble)
      }
    }.sortBy(x => (-x.score, x.operation.service.getOrElse(""), x.operation.operation))
  }

  private def _operation_evidence_uris(usage: ComponentUsage): Vector[URI] = {
    val metadata = usage.references.collect {
      case ("model-metadata", uri, _) => uri
    }
    (metadata ++ Vector(usage.profile.evidenceUri)).distinct
  }

  private def _tokens(value: String): Vector[String] =
    value.replaceAll("([\\p{Ll}\\p{N}])([\\p{Lu}])", "$1 $2")
      .toLowerCase(java.util.Locale.ROOT)
      .split("[^\\p{L}\\p{N}]+")
      .toVector
      .map(_.trim)
      .filter(x => x.length >= 2 && !_uninformative_tokens.contains(x))
      .distinct
}
