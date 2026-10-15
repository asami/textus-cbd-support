package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.ComponentFactory
import org.goldenport.cncf.component.capability.{ComponentCapabilityDiscovery, ComponentCapabilityPackageLoader}
import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import CapabilityIngestionModel.*
import CapabilityComponentUsageJsonPart.*
import CapabilityIngestionSelectionPart.*

/*
 * Public discovery is the only acquisition authority. State is an immutable,
 * explicitly selected logical history; it has no storage or execution adapter.
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityIngestion extends Kernel {
  def acquire(selection: Selection): Either[Diagnostic, Candidate] = for {
    provenance <- _selection(selection)
    packages <- selection.packages.zipWithIndex.foldLeft[Either[Diagnostic, Vector[ComponentPackage]]](Right(Vector.empty)) { case (previous, (pkg, i)) =>
      for {
        values <- previous
        acquired <- if (pkg.expected.resources.isEmpty) for {
          metadata <- ComponentCapabilityPackageLoader.load(pkg.expected.input)
          _ <- _metadata_observed(pkg.expected, metadata)
        } yield ComponentPackage.MetadataOnly(metadata) else for {
          provider <- pkg.provider.toRight(_diagnostic(DiagnosticKind.Unavailable, "Selected native metadata provider is unavailable.", s"/packages/$i/provider")
            .copy(scopeId = Some(selection.scope.scopeId), component = Some(pkg.expected.input.component.componentId)))
          discovered <- ComponentFactory.discoverCapabilities(pkg.expected.input, provider)
          _ <- _observed(pkg.expected, discovered, selection.scope)
        } yield ComponentPackage.Native(discovered)
      } yield values :+ acquired
    }
    components = packages.collect { case ComponentPackage.Native(value) => value }
    _ <- _snapshot_uniqueness(components)
    acquired <- _sequence(selection.packages.map(pkg => CapabilityComponentUsageAcquisitionPart.acquire(pkg.expected)))
    projections = acquired.flatMap(_.family match {
      case ComponentUsageFamily.Available(projection, _) => Vector(projection)
      case _: ComponentUsageFamily.Unavailable => Vector.empty
    })
    usage <- _sequence(acquired.map { pkg => pkg.family match {
      case ComponentUsageFamily.Available(projection, _) => CapabilityComponentUsageProjectionCodec.admit(projection, selection.scope,
        components.flatMap(_.entries.flatMap(_.loaded.projection.capabilities)), projections).map(family => pkg.copy(family = family))
      case _: ComponentUsageFamily.Unavailable => Right(pkg)
    } })
  } yield _candidate(provenance, packages, usage, selection.useCase)

  private def _management(prior: State, attempt: Attempt): Either[Diagnostic, Unit] = for {
    _ <- _nonnull(prior, "/state")
    a <- _nonnull(attempt, "/attempt")
    reference <- _nonnull(a.reference, "/attempt/reference")
    _ <- AttemptReference.from(reference.value)
    snapshot <- _nonnull(a.snapshotReference, "/attempt/snapshotReference")
    _ <- SnapshotReference.from(snapshot.value)
  } yield ()

  def ingest(prior: State, attempt: Attempt): Either[Diagnostic, Transition] = for {
    _ <- _management(prior, attempt)
    result <- transition(prior, attempt, acquire(attempt.selection))
  } yield result

  def transition(prior: State, attempt: Attempt, result: Either[Diagnostic, Candidate]): Either[Diagnostic, Transition] = for {
    _ <- _management(prior, attempt)
    supplied <- _nonnull(result, "/candidate")
    outcome <- supplied match {
      case Left(diagnostic) => _nonnull(diagnostic, "/candidate/diagnostic").map(d => Outcome.Rejected(d))
      case Right(candidate) =>
        val checked = for {
          c <- _nonnull(candidate, "/candidate")
          declared <- _selection(attempt.selection)
          _ <- _compare(declared, c.provenance)
          _ <- Either.cond(!prior.snapshots.exists(_.reference == attempt.snapshotReference), (),
            _diagnostic(DiagnosticKind.Ambiguous, "An accepted snapshot reference already identifies a snapshot.", "/attempt/snapshotReference"))
        } yield ()
        Right(checked.fold(Outcome.Rejected(_), _ => Outcome.Accepted(attempt.snapshotReference)))
    }
  } yield {
    val record = AttemptRecord(attempt, outcome)
    val next = outcome match {
      case Outcome.Accepted(ref) => _state(Some(ref), prior.snapshots :+ _snapshot(ref, supplied.toOption.get), prior.attempts :+ record)
      case Outcome.Rejected(_) => _state(prior.current, prior.snapshots, prior.attempts :+ record)
    }
    Transition(next, outcome)
  }

  def readHistorical(state: State, reference: SnapshotReference): Either[Diagnostic, Snapshot] = for {
    s <- _nonnull(state, "/state")
    r <- _nonnull(reference, "/snapshotReference")
    _ <- SnapshotReference.from(r.value)
    snapshot <- s.snapshots.find(_.reference == r).toRight(_diagnostic(DiagnosticKind.Unavailable, "Selected accepted snapshot is unavailable.", "/snapshotReference"))
  } yield snapshot

  def readCurrent(state: State, selection: CurrentSelection): Either[Diagnostic, Snapshot] = for {
    selected <- _nonnull(selection, "/current")
    observation <- _nonnull(selected.observation, "/current/observation")
    _ <- observation match {
      case Left(d) => _nonnull(d, "/current/diagnostic").flatMap(x => Left(x))
      case Right(_) => Right(())
    }
    s <- _nonnull(state, "/state")
    ref <- _nonnull(selected.snapshotReference, "/current/snapshotReference")
    _ <- SnapshotReference.from(ref.value)
    current <- s.current.toRight(_diagnostic(DiagnosticKind.Unavailable, "No accepted current snapshot is selected.", "/current"))
    _ <- Either.cond(ref == current, (), _diagnostic(DiagnosticKind.Stale, "Declared snapshot differs from the current reference.", "/current/snapshotReference"))
    snapshot <- readHistorical(s, ref)
    _ <- _compare(selected.expected, snapshot.provenance)
  } yield snapshot
}
