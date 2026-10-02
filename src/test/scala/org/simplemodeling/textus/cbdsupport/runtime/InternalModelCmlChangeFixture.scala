package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import io.circe.Json
import org.goldenport.Consequence

/**
 * Narrow adapter over the rich actual-human and durable action fixture families.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object InternalModelCmlChangeFixture {
  import InternalModelCandidateHumanApprovalValidatorSpec.*

  final case class Fixture(
    root: Path,
    data: FixtureData,
    handoff: InternalModelVerifiedContinuationPackage,
    request: InternalModelCmlChangeRequest
  )

  def withFixture(options: FixtureOptions = InternalModelDurableHandoffFixture.approvedOptions,
    blocking: Boolean = false)(body: Fixture => Unit): Unit =
    InternalModelDurableHandoffFixture.withRich(options, blocking) { (root, data, currentrequest) =>
      // These are exactly the inherited candidate-declared existing target paths.
      writeCml(root, "cml/alpha.cml", "alpha cml baseline\n".getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector)
      writeCml(root, "cml/beta.cml", "beta cml baseline\n".getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector)
      val original = admit(root, data)
      val handoff = InternalModelPackageValidator.verifiedContinuation(root).toOption.get
      val authority = InternalModelCmlMutationAuthority(recordReference("cml-mutation-request", 307L),
        root.toAbsolutePath.normalize.toString, currentrequest.packagereference, currentrequest.scope,
        InternalModelSemanticSource("project-source-owner", "canonical-cml-owner", Some("owner/request"), Some("owner-authority-1")),
        Vector(InternalModelCmlMutationTarget("target-alpha", "cml/alpha.cml", "cml-authority", "cml-alpha", "approved-next-alpha"),
          InternalModelCmlMutationTarget("target-beta", "cml/beta.cml", "cml-authority", "cml-beta", "approved-next-beta")))
      val live = unchangedSources.map { case (id, observation) =>
        val input = observation match {
          case InternalModelLiveSourceObservation.Observed(authority, identity, revision, _, Some(path)) =>
            InternalModelPackageFreshnessInput.CmlObserved(authority, identity, revision, path)
          case other => InternalModelPackageFreshnessInput.SourceObservation(other)
        }
        artifactReference(id, options) -> input
      }
      val request = InternalModelCmlChangeRequest(currentrequest, original.approval, original.snapshots,
        original.review, live, None, Some(authority))
      body(Fixture(root, data, handoff, request))
    }

  def withPair(options: FixtureOptions)(body: (Fixture, Fixture) => Unit): Unit =
    withFixture() { original => withFixture(options) { current => body(original, current) } }

  def fromOriginal(original: Fixture, current: Fixture): InternalModelCmlChangeRequest =
    current.request.copy(originalapproval = original.request.originalapproval,
      originalsnapshots = original.request.originalsnapshots)

  def evaluate(fixture: Fixture, request: InternalModelCmlChangeRequest): Consequence[InternalModelCmlChangeReport] =
    InternalModelCmlChangeGate.evaluateVerified(fixture.root, fixture.handoff, request)

  def replacePayload(handoff: InternalModelVerifiedContinuationPackage, reference: InternalModelArtifactReference,
    bytes: Vector[Byte]): InternalModelVerifiedContinuationPackage =
    handoff.copy(artifacts = handoff.artifacts.map(entry =>
      if (entry.context.reference == reference) entry.copy(bytes = Some(bytes)) else entry))

  def withCursor(fixture: Fixture, change: InternalModelResumeCursor => InternalModelResumeCursor): InternalModelVerifiedContinuationPackage = {
    val entry = fixture.handoff.artifacts.find(_.context.reference.role == InternalModelArtifactRole.Resume).get
    val cursor = InternalModelResumeCursorCodec.decode(entry.bytes.get).toOption.get
    replacePayload(fixture.handoff, entry.context.reference, InternalModelResumeCursorCodec.encode(change(cursor)))
  }

  def writeCml(root: Path, relativePath: String, bytes: Vector[Byte]): Unit = {
    val target = root.resolve(relativePath).normalize
    require(target.startsWith(root.normalize) && target != root.normalize, "fixture target must remain in its exact owned subtree")
    Files.createDirectories(target.getParent)
    Files.write(target, bytes.toArray)
    ()
  }

  /** All new versions are explicit test inputs, independently applied to producer and caller evidence. */
  def reviseBasis(fixture: Fixture, artifactRevisions: Map[String, Long],
    recordRevisions: Map[String, Long], subjectRevision: Long, carrierRevision: Long): Fixture = {
    def _rewrite_(value: Json): Json = value.arrayOrObject(value,
      values => Json.fromValues(values.map(_rewrite_)), objectvalue => {
        val nested = objectvalue.mapValues(_rewrite_)
        val artifact = nested("artifactId").flatMap(_.asString).flatMap(artifactRevisions.get)
          .fold(nested)(revision => nested.add("artifactRevision", Json.fromLong(revision)))
        val record = artifact("recordId").flatMap(_.asString).flatMap(recordRevisions.get)
          .fold(artifact)(revision => artifact.add("recordRevision", Json.fromLong(revision)))
        val subject = if (record.contains("subjectId")) record.add("subjectRevision", Json.fromLong(subjectRevision)) else record
        val manifest = if (subject.contains("artifacts") && subject.contains("lifecycleState"))
          subject.add("revision", Json.fromLong(carrierRevision)) else subject
        Json.fromJsonObject(if (manifest.contains("packageRevision")) manifest.add("packageRevision", Json.fromLong(carrierRevision)) else manifest)
      })
    val packagepath = fixture.root.resolve("src/main/internal-model")
    val paths = Vector("manifest.yaml") ++ fixture.handoff.artifacts.filter(_.context.present).map(_.context.path)
    paths.foreach { relative =>
      val path = packagepath.resolve(relative)
      io.circe.parser.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).toOption.foreach { value =>
        Files.write(path, jsonBytes(_rewrite_(value)))
      }
    }
    val human = InternalModelCandidateHumanApprovalCodec.decode(jsonBytes(_rewrite_(recordJson(fixture.data.expected))).toVector).toOption.get.approval
    def _artifact_(reference: InternalModelArtifactReference): InternalModelArtifactReference =
      artifactRevisions.get(reference.artifactId.value).fold(reference)(revision =>
        reference.copy(artifactRevision = InternalModelArtifactRevision.from(revision).toOption.get))
    val currentrequest = fixture.request.currentrequest.copy(humandecision = Some(human), carrierrevision = carrierRevision,
      candidateartifact = fixture.request.currentrequest.candidateartifact.map(_artifact_),
      semanticdiffartifact = fixture.request.currentrequest.semanticdiffartifact.map(_artifact_),
      reviewartifact = fixture.request.currentrequest.reviewartifact.map(_artifact_),
      approvalartifact = fixture.request.currentrequest.approvalartifact.map(_artifact_),
      realizationreference = recordRevisions.get(fixture.request.currentrequest.realizationreference.recordId.value)
        .fold(fixture.request.currentrequest.realizationreference)(revision =>
          recordReference(fixture.request.currentrequest.realizationreference.recordId.value, revision)))
    val handoff = InternalModelPackageValidator.verifiedContinuation(fixture.root).toOption.get
    val review = InternalModelCandidateReviewBindingValidator.validate(fixture.root,
      currentrequest.reviewartifact.get, fixture.data.executionbasis).toOption.get
    fixture.copy(handoff = handoff, request = fixture.request.copy(currentrequest = currentrequest, currentreview = review))
  }
}
