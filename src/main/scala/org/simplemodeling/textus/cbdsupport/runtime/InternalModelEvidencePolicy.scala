package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/**
 * Pure evidence admission with structurally separate input and retained payloads.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] enum InternalModelEvidenceKind {
  case Prompt, Response, ProviderIdentity, ModelIdentity, ToolIdentity, CallTree, ExternalEvidence, Narrative
}

private[runtime] enum InternalModelEvidenceOmission {
  case PolicyExcluded, Unavailable
}

private[runtime] enum InternalModelEvidenceInputPayload {
  case Raw(value: String)
  case SafeIdentity(value: String)
  case RedactedText(value: String, redactionReference: InternalModelRecordReference)
  case Unavailable
}

private[runtime] final case class InternalModelEvidenceInput(
  reference: InternalModelRecordReference,
  kind: InternalModelEvidenceKind,
  payload: InternalModelEvidenceInputPayload
)

private[runtime] enum InternalModelRetainedEvidencePayload {
  case Identity(value: String)
  case RedactedText(value: String, redactionReference: InternalModelRecordReference)
  case Omitted(reason: InternalModelEvidenceOmission)
}

private[runtime] final case class InternalModelRetainedEvidence(
  reference: InternalModelRecordReference,
  kind: InternalModelEvidenceKind,
  payload: InternalModelRetainedEvidencePayload
)

private[runtime] object InternalModelEvidencePolicy {
  private val _identity_pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r

  def retain(input: InternalModelEvidenceInput): Consequence[InternalModelRetainedEvidence] = {
    if (input == null) {
      _invalid("input")
    } else {
      Consequence.zip3(
        _record_reference(input.reference),
        _check(input.kind != null, "kind"),
        _input_payload(input.payload)
      ).flatMap { _ =>
        _retain_payload(input.kind, input.payload).map { payload =>
          InternalModelRetainedEvidence(input.reference, input.kind, payload)
        }
      }
    }
  }

  /** Construction alone does not admit a retained value or its upstream redaction claim. */
  def validate(retained: InternalModelRetainedEvidence): Consequence[InternalModelRetainedEvidence] = {
    if (retained == null) {
      _invalid("retained")
    } else {
      Consequence.zip3(
        _record_reference(retained.reference),
        _check(retained.kind != null, "kind"),
        _retained_payload(retained.kind, retained.payload)
      ).map(_ => retained)
    }
  }

  private def _input_payload(payload: InternalModelEvidenceInputPayload): Consequence[Unit] = {
    if (payload == null) {
      _invalid("payload")
    } else {
      payload match {
        case InternalModelEvidenceInputPayload.Raw(value) =>
          _check(value != null && value.length <= 1048576, "raw")
        case InternalModelEvidenceInputPayload.SafeIdentity(value) => _identity(value)
        case InternalModelEvidenceInputPayload.RedactedText(value, reference) =>
          _narrative(value).zip(_record_reference(reference)).map(_ => ())
        case InternalModelEvidenceInputPayload.Unavailable => Consequence.unit
      }
    }
  }

  private def _retain_payload(
    kind: InternalModelEvidenceKind,
    payload: InternalModelEvidenceInputPayload
  ): Consequence[InternalModelRetainedEvidencePayload] = {
    import InternalModelEvidenceInputPayload.*
    import InternalModelRetainedEvidencePayload.{Identity, Omitted}
    payload match {
      case Unavailable => Consequence.success(Omitted(InternalModelEvidenceOmission.Unavailable))
      case Raw(_) => Consequence.success(Omitted(InternalModelEvidenceOmission.PolicyExcluded))
      case _ if _excluded_kind(kind) => Consequence.success(Omitted(InternalModelEvidenceOmission.PolicyExcluded))
      case SafeIdentity(value) if _identity_kind(kind) => Consequence.success(Identity(value))
      case RedactedText(value, reference) if kind == InternalModelEvidenceKind.Narrative =>
        Consequence.success(InternalModelRetainedEvidencePayload.RedactedText(value, reference))
      case _ => _invalid("pairing")
    }
  }

  private def _retained_payload(
    kind: InternalModelEvidenceKind,
    payload: InternalModelRetainedEvidencePayload
  ): Consequence[Unit] = {
    if (payload == null) {
      _invalid("payload")
    } else {
      payload match {
        case InternalModelRetainedEvidencePayload.Identity(value) =>
          _identity(value).zip(_check(_identity_kind(kind), "pairing")).map(_ => ())
        case InternalModelRetainedEvidencePayload.RedactedText(value, reference) =>
          Consequence.zip3(_narrative(value), _record_reference(reference),
            _check(kind == InternalModelEvidenceKind.Narrative, "pairing")).map(_ => ())
        case InternalModelRetainedEvidencePayload.Omitted(reason) => _check(reason != null, "omission")
      }
    }
  }

  private def _identity_kind(kind: InternalModelEvidenceKind): Boolean = {
    kind == InternalModelEvidenceKind.ProviderIdentity || kind == InternalModelEvidenceKind.ModelIdentity ||
      kind == InternalModelEvidenceKind.ToolIdentity
  }

  private def _excluded_kind(kind: InternalModelEvidenceKind): Boolean = {
    kind == InternalModelEvidenceKind.Prompt || kind == InternalModelEvidenceKind.Response ||
      kind == InternalModelEvidenceKind.CallTree || kind == InternalModelEvidenceKind.ExternalEvidence
  }

  private def _identity(value: String): Consequence[Unit] = {
    _check(value != null && value.length <= 128 && _identity_pattern.matches(value), "identity")
  }

  private def _narrative(value: String): Consequence[Unit] = {
    _check(value != null && !value.isBlank && value.length <= 4096 && !value.contains('\u0000'), "narrative")
  }

  private def _record_reference(reference: InternalModelRecordReference): Consequence[Unit] = {
    if (reference == null) {
      _invalid("reference")
    } else {
      _check(InternalModelRecordId.from(reference.recordId.value).isRight, "record-id")
        .zip(_check(InternalModelRecordRevision.from(reference.recordRevision.value).isRight, "record-revision"))
        .map(_ => ())
    }
  }

  private def _check(valid: Boolean, dimension: String): Consequence[Unit] = {
    if (valid) Consequence.unit else _invalid(dimension)
  }

  private def _invalid[A](dimension: String): Consequence[A] = {
    Consequence.operationInvalid(s"internal-model evidence invalid: $dimension")
  }
}
