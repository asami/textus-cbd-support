package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.component.capability.ComponentCapabilityAdmissionScope.*
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.*
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.CapabilityDefinition
import CapabilityIngestionModel.*
import CapabilityComponentUsageJsonPart.*

/*
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityComponentUsageBindingPart {
  private final case class VersionCandidate(identity: ModelElementId, kind: ModelKind, version: String)
  def _bind(projection: ComponentUsageProjection, scope: Scope, components: Vector[CapabilityDefinition], selectedUsage: Vector[ComponentUsageProjection]): Either[Diagnostic, Vector[ExternalReference]] = {
    val usages = selectedUsage.flatMap(_.documents.flatMap(_.componentUsageCapabilities))
    val componentdefinitions = scope.capabilities ++ components
    val versioned = componentdefinitions.map(x => VersionCandidate(x.identity, ModelKind.ComponentCapability, x.contractVersion)) ++
      usages.map(x => VersionCandidate(x.identity, ModelKind.ComponentUsageCapability, x.contractVersion))
    // Kind presence collapses versions; explicit scope facts remain independent candidates.
    val inferred = versioned.map(x => x.identity -> x.kind).distinct
    val models = scope.modelFacts.map(x => x.identity -> x.kind) ++ inferred
    def _resolve_[A](identity: A, candidates: Vector[A], path: String): Either[Diagnostic, Vector[ExternalReference]] = {
      val count = candidates.count(_ == identity)
      if (count == 0) Left(_diagnostic(DiagnosticKind.Malformed, "Local target is absent from the selected finite scope.", path))
      else if (count > 1) Left(_diagnostic(DiagnosticKind.Ambiguous, "Multiple exact Local targets.", path)) else Right(Vector.empty)
    }
    def _model_(identity: ModelElementId, kinds: Set[ModelKind], path: String): Either[Diagnostic, Vector[ExternalReference]] = {
      val known = models.filter(_._1 == identity)
      val exact = known.filter(x => kinds.isEmpty || kinds.contains(x._2))
      if (known.isEmpty) Left(_diagnostic(DiagnosticKind.Malformed, "Local model identity is absent.", path))
      else if (exact.isEmpty) Left(_diagnostic(DiagnosticKind.Incompatible, "Known model identity has another kind.", path))
      else if (exact.size > 1) Left(_diagnostic(DiagnosticKind.Ambiguous, "Multiple exact Local model candidates.", path)) else Right(Vector.empty)
    }
    def _version_(identity: ModelElementId, kind: ModelKind, contract: String, path: String): Either[Diagnostic, Vector[ExternalReference]] = {
      val known = versioned.filter(_.identity == identity)
      val exact = known.filter(x => x.kind == kind && x.version == contract)
      if (known.isEmpty && !models.exists(_._1 == identity)) Left(_diagnostic(DiagnosticKind.Malformed, "Local Capability identity is absent.", path))
      else if (exact.isEmpty) Left(_diagnostic(DiagnosticKind.Incompatible, "Known Capability has another kind or version.", path))
      else if (exact.size > 1) Left(_diagnostic(DiagnosticKind.Ambiguous, "Multiple exact versioned Local candidates.", path)) else Right(Vector.empty)
    }
    def _boundary_(value: ReferenceBoundary, target: ExternalTarget, source: Source, origin: Origin, path: String)(local: => Either[Diagnostic, Vector[ExternalReference]]): Either[Diagnostic, Vector[ExternalReference]] =
      _context(if (value == ReferenceBoundary.External) Right(Vector(ExternalReference(path, target, source, origin))) else local, Some(source), Some(scope.scopeId))
    def _role_(value: ModelRole): ModelKind = value match {
      case ModelRole.Goal => ModelKind.Goal
      case ModelRole.UseCase => ModelKind.UseCase
      case ModelRole.Scenario => ModelKind.Scenario
      case ModelRole.UseCaseSlice => ModelKind.UseCaseSlice
      case ModelRole.Component => ModelKind.Component
      case ModelRole.Specification => ModelKind.Specification
      case ModelRole.Evidence => ModelKind.Evidence
    }
    def _links_(xs: Vector[ModelLink], path: String): Vector[Either[Diagnostic, Vector[ExternalReference]]] = xs.zipWithIndex.map { (x, i) =>
      val p = s"$path/$i/target"
      _boundary_(x.target.boundary, ExternalTarget.Model(x.target.identity), x.source, x.origin, p)(_model_(x.target.identity, Set(_role_(x.role)), p))
    }
    val checks = projection.documents.zipWithIndex.flatMap { (doc, i) => doc.componentUsageCapabilities.zipWithIndex.flatMap { (app, k) =>
      val path = s"/documents/$i/componentUsageCapabilities/$k"
      val mapping = _items(app.componentMappings).zipWithIndex.map { (x, n) =>
        val p = s"$path/componentMappings/value/$n/target"
        _boundary_(x.boundary, ExternalTarget.ComponentCapability(x.target), x.source, x.origin, p)(_version_(x.target.identity, ModelKind.ComponentCapability, x.target.contractVersion, p))
      }
      val dependency = _items(app.dependencies).zipWithIndex.map { (x, n) =>
        val p = s"$path/dependencies/value/$n/target"
        _boundary_(x.boundary, ExternalTarget.ComponentUsageCapability(x.target), x.source, x.origin, p)(_version_(x.target.identity, ModelKind.ComponentUsageCapability, x.target.contractVersion, p))
      }
      val trace = _items(app.traceability).zipWithIndex.map { (x, n) =>
        val p = s"$path/traceability/value/$n/target"
        val kind = if (x.role == TraceabilityRole.Specification) ModelKind.Specification else ModelKind.Evidence
        _boundary_(x.target.boundary, ExternalTarget.Model(x.target.identity), x.source, x.origin, p)(_model_(x.target.identity, Set(kind), p))
      }
      val grounding = _items(app.grounding).zipWithIndex.flatMap { (x, n) =>
        val p = s"$path/grounding/value/$n"
        val target = x.target match {
          case ModelTarget(id) => _boundary_(x.boundary, ExternalTarget.Model(id), x.source, x.origin, p + "/target")(_model_(id, Set.empty, p + "/target"))
          case TermTarget(id) => _boundary_(x.boundary, ExternalTarget.Term(id), x.source, x.origin, p + "/target")(_resolve_(id, scope.termFacts.map(_.identity), p + "/target"))
        }
        val relation = x.relation match {
          case Present(id) => Vector(_boundary_(x.boundary, ExternalTarget.Relation(id), x.source, x.origin, p + "/relation/value")(_resolve_(id, scope.relationFacts.map(_.identity), p + "/relation/value")))
          case _: Absent => Vector.empty
        }
        val profile = x.profile match {
          case Present(id) => Vector(_boundary_(x.boundary, ExternalTarget.Profile(id), x.source, x.origin, p + "/profile/value")(_resolve_(id, scope.profileFacts.map(_.identity), p + "/profile/value")))
          case _: Absent => Vector.empty
        }
        Vector(target) ++ relation ++ profile
      }
      _links_(_items(app.demandReferences), path + "/demandReferences/value") ++ _links_(_items(app.discoveryReferences), path + "/discoveryReferences/value") ++ mapping ++ dependency ++ grounding ++ trace
    } }
    _sequence(checks).map(_.flatten)
  }
}
