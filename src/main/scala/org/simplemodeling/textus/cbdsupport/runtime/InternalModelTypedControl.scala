package org.simplemodeling.textus.cbdsupport.runtime

/**
 * Validated producer-declared identities and revisions for distinct control references.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] opaque type InternalModelPackageId = String

private[runtime] object InternalModelPackageId {
  private val _pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  def from(value: String): Either[String, InternalModelPackageId] =
    Either.cond(Option(value).exists(_pattern.matches), value, "packageId must be a lowercase hyphenated UUID")

  extension (identity: InternalModelPackageId) def value: String = identity
}

private[runtime] opaque type InternalModelProjectToken = String

private[runtime] object InternalModelProjectToken {
  private val _pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r

  def from(value: String): Either[String, InternalModelProjectToken] =
    Either.cond(Option(value).exists(_pattern.matches), value, "project identity must be an ASCII token")

  extension (identity: InternalModelProjectToken) def value: String = identity
}

opaque type InternalModelArtifactId = String

object InternalModelArtifactId {
  private val _pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r

  def from(value: String): Either[String, InternalModelArtifactId] =
    Either.cond(Option(value).exists(_pattern.matches), value, "artifactId must be an ASCII token")

  extension (identity: InternalModelArtifactId) def value: String = identity
}

opaque type InternalModelArtifactRevision = Long

object InternalModelArtifactRevision {
  def from(value: Long): Either[String, InternalModelArtifactRevision] =
    Either.cond(value > 0, value, "artifactRevision must be positive")

  extension (revision: InternalModelArtifactRevision) def value: Long = revision
}

opaque type InternalModelRecordId = String

object InternalModelRecordId {
  def from(value: String): Either[String, InternalModelRecordId] =
    Either.cond(Option(value).exists(!_.isBlank), value, "recordId must be nonblank")

  extension (identity: InternalModelRecordId) def value: String = identity
}

opaque type InternalModelRecordRevision = Long

object InternalModelRecordRevision {
  def from(value: Long): Either[String, InternalModelRecordRevision] =
    Either.cond(value > 0, value, "recordRevision must be positive")

  extension (revision: InternalModelRecordRevision) def value: Long = revision
}

enum InternalModelArtifactRole {
  case Resume, SourceSnapshot, Decision, OpenIssue, Realization, Projection, Approval, Validation

  def wireValue: String = this match {
    case Resume => "resume"
    case SourceSnapshot => "source-snapshot"
    case Decision => "decision"
    case OpenIssue => "open-issue"
    case Realization => "realization"
    case Projection => "projection"
    case Approval => "approval"
    case Validation => "validation"
  }
}

object InternalModelArtifactRole {
  def fromWire(value: String): Either[String, InternalModelArtifactRole] =
    values.find(_.wireValue == value).toRight("artifact role is unsupported")
}

private[runtime] final case class InternalModelPackageReference(
  packageId: InternalModelPackageId,
  projectNamespace: InternalModelProjectToken,
  projectId: InternalModelProjectToken
)

final case class InternalModelArtifactReference(
  artifactId: InternalModelArtifactId,
  artifactRevision: InternalModelArtifactRevision,
  role: InternalModelArtifactRole
)

final case class InternalModelRecordReference(
  recordId: InternalModelRecordId,
  recordRevision: InternalModelRecordRevision
)
