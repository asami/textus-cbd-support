package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.{CbdCatalogAdminService, CbdRetrievalService}
import org.simplemodeling.textus.cbdsupport.runtime.{CbdHttp, CbdRuntime, ComponentDependency, ComponentDependencyConflict, ComponentMatch, ComponentObservation, ComponentProfile, ComponentUsage, InformationSourceState, ResolvedComponentDependency}
import org.simplemodeling.textus.cbdsupport.runtime.{ReconciliationIssue, ReconciliationObservation, ReconciliationPrecedenceTier, SourceAwareComponentSearchQuery}

/*
 * @since   Jul. 14, 2026
 * @version Jul. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactory extends CbdSupportComponent.Factory {
  private val _runtime = CbdRuntime.create()

  override val CbdRetrieval: CbdSupportComponent.CbdRetrievalServiceFactory =
    new CbdRetrievalServiceFactoryImpl()

  override val CbdCatalogAdmin: CbdSupportComponent.CbdCatalogAdminServiceFactory =
    new CbdCatalogAdminServiceFactoryImpl()

  override protected def create_Component(params: ComponentCreate): Component =
    createUninitializedComponent()

  private[cbdsupport] def createUninitializedComponent(): Component =
    new CbdSupportComponent {
      override def mcpReadyServices: Set[String] = Set("CbdRetrieval")
    }

  private final class CbdRetrievalServiceFactoryImpl
    extends CbdSupportComponent.CbdRetrievalServiceFactory {
    import CbdRetrievalService.*

    override def createSearchComponentsActionCall(
      core: ActionCall.Core,
      action: ComponentSearchRequest
    ): SearchComponentsActionCall = SearchComponentsActionCallImpl(core, action)

    override def createGetComponentActionCall(
      core: ActionCall.Core,
      action: ComponentLookupRequest
    ): GetComponentActionCall = GetComponentActionCallImpl(core, action)

    override def createGetUsageActionCall(
      core: ActionCall.Core,
      action: ComponentUsageRequest
    ): GetUsageActionCall = GetUsageActionCallImpl(core, action)

    override def createResolveDependenciesActionCall(
      core: ActionCall.Core,
      action: DependencyResolutionRequest
    ): ResolveDependenciesActionCall = ResolveDependenciesActionCallImpl(core, action)

    override def createListCatalogsActionCall(
      core: ActionCall.Core,
      action: CatalogListRequest
    ): ListCatalogsActionCall = ListCatalogsActionCallImpl(core, action)

    override def createStatusActionCall(
      core: ActionCall.Core,
      action: CbdStatusRequest
    ): StatusActionCall = StatusActionCallImpl(core, action)
  }

  private final class CbdCatalogAdminServiceFactoryImpl
    extends CbdSupportComponent.CbdCatalogAdminServiceFactory {
    import CbdCatalogAdminService.*

    override def createRefreshCatalogActionCall(
      core: ActionCall.Core,
      action: CatalogRefreshRequest
    ): RefreshCatalogActionCall = RefreshCatalogActionCallImpl(core, action)
  }

  private final case class SearchComponentsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentSearchRequest
  ) extends CbdRetrievalService.SearchComponentsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val fetcher = new CbdHttp(core)
      _runtime.ensureInputsReady(fetcher).flatMap { _ =>
        val requirement = _required_string(action.record, "requirement")
        val limit = _optional_int(action.record, "limit").getOrElse(10)
        _runtime.searchSieTerms(requirement, None, limit, fetcher).map { _ =>
          val result = _runtime.searchSourceAware(SourceAwareComponentSearchQuery(
            requirement,
            _optional_string(action.record, "organization"),
            _optional_string(action.record, "kind"),
            _optional_string(action.record, "version"),
            _optional_string(action.record, "runtimeVersion"),
            _optional_string(action.record, "sourceId"),
            _optional_string(action.record, "sourceKind"),
            _optional_string(action.record, "freshness"),
            _optional_string(action.record, "versionState"),
            _optional_string(action.record, "conflictCode"),
            _optional_string(action.record, "purpose"),
            limit
          ))
          OperationResponse(Record.dataAuto(
            "status" -> (if (result.report.observations.nonEmpty) "matched" else "no-match"),
            "results" -> result.matches.map(_match_record),
            "observations" -> result.report.observations.map(_source_aware_observation_record),
            "issues" -> result.report.issues.map(_source_aware_issue_record),
            "precedence" -> result.report.precedence.map(_source_aware_precedence_record),
            "selectedObservation" -> result.report.selectedObservation.map(_source_aware_observation_record),
            "warnings" -> (result.warnings ++ _source_warnings).distinct
          ))
        }
      }
    }
  }

  private final case class GetComponentActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentLookupRequest
  ) extends CbdRetrievalService.GetComponentActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val fetcher = new CbdHttp(core)
      _runtime.ensureInputsReady(fetcher).map { _ =>
        val profile = _lookup(action.record, _optional_string(action.record, "kind"))
        OperationResponse(Record.dataAuto(
          "status" -> (if (profile.nonEmpty) "matched" else "no-match"),
          "reference" -> profile.map(_reference_record),
          "component" -> profile.map(_profile_record),
          "warnings" -> _source_warnings
        ))
      }
    }
  }

  private final case class GetUsageActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentUsageRequest
  ) extends CbdRetrievalService.GetUsageActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val fetcher = new CbdHttp(core)
      _runtime.ensureInputsReady(fetcher).flatMap { _ =>
        _lookup(action.record, _optional_string(action.record, "kind")) match {
          case Some(profile) =>
            _runtime.usage(profile, fetcher).map { usage =>
              OperationResponse(_usage_record(usage))
            }
          case None =>
            org.goldenport.Consequence.success(OperationResponse(Record.dataAuto(
              "status" -> "no-match",
              "operations" -> Vector.empty[Record],
              "references" -> Vector.empty[Record],
              "warnings" -> Vector("Component was not found in the selected catalogs.")
            )))
        }
      }
    }
  }

  private final case class ResolveDependenciesActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.DependencyResolutionRequest
  ) extends CbdRetrievalService.ResolveDependenciesActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val fetcher = new CbdHttp(core)
      _runtime.ensureInputsReady(fetcher).map { _ =>
        val profile = _lookup(action.record, _optional_string(action.record, "kind"))
        val resolution = profile.map(_runtime.resolveDependencies(
          _,
          _optional_string(action.record, "version"),
          _optional_int(action.record, "maxDepth").getOrElse(CbdRuntime.DEFAULT_DEPENDENCY_DEPTH)
        ))
        OperationResponse(Record.dataAuto(
          "status" -> (if (profile.nonEmpty) "matched" else "no-match"),
          "reference" -> profile.map(_reference_record),
          "component" -> profile.map(_profile_record),
          "dependencies" -> resolution.toVector.flatMap(_.directDependencies).map(_dependency_record),
          "resolutions" -> resolution.toVector.flatMap(_.resolutions).map(_resolved_dependency_record),
          "conflicts" -> resolution.toVector.flatMap(_.conflicts).map(_dependency_conflict_record),
          "warnings" -> (profile.toVector.flatMap(_.warnings) ++ resolution.toVector.flatMap(_.warnings) ++ _source_warnings)
        ))
      }
    }
  }

  private final case class ListCatalogsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.CatalogListRequest
  ) extends CbdRetrievalService.ListCatalogsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      org.goldenport.Consequence.success(OperationResponse(Record.dataAuto(
        "sources" -> _runtime.informationSourceStates(_optional_boolean(action.record, "includeDisabled").getOrElse(false)).map(_source_record),
        "warnings" -> _runtime.configurationWarnings
      )))
    }
  }

  private final case class StatusActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.CbdStatusRequest
  ) extends CbdRetrievalService.StatusActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val states = _runtime.informationSourceStates(includeDisabled = false)
      org.goldenport.Consequence.success(OperationResponse(Record.dataAuto(
        "overall" -> _runtime.overallStatus,
        "sourceCount" -> states.size,
        "readySourceCount" -> states.count(_.status == "ready"),
        "componentCount" -> _runtime.componentCount,
        "detail" -> _optional_string(action.record, "detail").orElse(Some(states.map(x => s"${x.descriptor.id}=${x.status}").mkString(", ")))
      )))
    }
  }

  private final case class RefreshCatalogActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdCatalogAdminService.CatalogRefreshRequest
  ) extends CbdCatalogAdminService.RefreshCatalogActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      val fetcher = new CbdHttp(core)
      _runtime.refresh(_optional_string(action.record, "sourceId"), fetcher).map { states =>
        OperationResponse(Record.dataAuto(
          "status" -> _runtime.overallStatus,
          "sourceCount" -> states.count(_.source.enabled),
          "componentCount" -> _runtime.componentCount,
          "warnings" -> states.flatMap(_.warning)
        ))
      }
    }
  }

  private def _lookup(record: Record, kind: Option[String]): Option[ComponentProfile] =
    _runtime.get(
      _required_string(record, "name"),
      _optional_string(record, "organization"),
      kind,
      _optional_string(record, "version"),
      _optional_string(record, "catalogId")
    )

  private def _usage_record(usage: ComponentUsage): Record =
    Record.dataAuto(
      "status" -> "matched",
      "reference" -> _reference_record(usage.profile),
      "component" -> _profile_record(usage.profile),
      "operations" -> usage.operations.map { operation =>
        Record.dataAuto(
          "service" -> operation.service,
          "operation" -> operation.operation,
          "kind" -> operation.kind,
          "description" -> operation.description
        )
      },
      "references" -> usage.references.map { case (kind, uri, authoritative) =>
        Record.dataAuto("kind" -> kind, "uri" -> uri.toString, "authoritative" -> authoritative)
      },
      "warnings" -> (usage.warnings ++ _source_warnings)
    )

  private def _match_record(result: ComponentMatch): Record =
    Record.dataAuto(
      "component" -> _profile_record(result.profile),
      "reference" -> _reference_record(result.profile),
      "matchKind" -> result.matchKind,
      "score" -> result.score,
      "rationale" -> result.rationale
    )

  private def _profile_record(profile: ComponentProfile): Record =
    Record.dataAuto(
      "catalogId" -> profile.catalogId,
      "organization" -> profile.organization,
      "name" -> profile.name,
      "title" -> profile.title,
      "summary" -> profile.summary,
      "kind" -> profile.kind,
      "versions" -> profile.versions,
      "selectedVersion" -> profile.selectedVersion,
      "dependencyMetadataVersion" -> profile.dependencyMetadataVersion,
      "latestStable" -> profile.latestStable,
      "latestSnapshot" -> profile.latestSnapshot,
      "selectedChannel" -> profile.selectedChannel,
      "selectedStatus" -> profile.selectedStatus,
      "selectedComponent" -> profile.selectedComponent,
      "selectedPublishedAt" -> profile.selectedPublishedAt,
      "runtimeMinimum" -> profile.runtimeMinimum,
      "runtimeMaximum" -> profile.runtimeMaximum,
      "runtimeTested" -> profile.runtimeTested,
      "tags" -> profile.tags,
      "terms" -> profile.terms,
      "artifactUri" -> profile.artifactUri.map(_.toString),
      "artifactChecksumSha256" -> profile.artifactChecksumSha256,
      "evidenceUri" -> profile.evidenceUri.toString,
      "observation" -> _runtime.observation(profile).map(_observation_record),
      "warnings" -> profile.warnings
    )

  private def _observation_record(observation: ComponentObservation): Record =
    Record.dataAuto(
      "sourceId" -> observation.sourceId,
      "sourceKind" -> observation.sourceKind,
      "evidenceLocation" -> observation.evidenceLocation,
      "version" -> observation.version,
      "freshness" -> observation.freshness,
      "observedAt" -> observation.observedAt.map(_.toString),
      "expiresAt" -> observation.expiresAt.map(_.toString),
      "artifactChecksumSha256" -> observation.artifactChecksumSha256,
      "diagnostics" -> observation.diagnostics
    )

  private def _source_aware_observation_record(observation: ReconciliationObservation): Record =
    Record.dataAuto(
      "sourceId" -> observation.sourceId,
      "sourceKind" -> observation.sourceKind,
      "organization" -> observation.organization,
      "componentName" -> observation.componentName,
      "componentKind" -> observation.componentKind,
      "version" -> observation.version,
      "versionState" -> observation.versionState,
      "freshness" -> observation.freshness,
      "runtimeMinimum" -> observation.runtimeMinimum,
      "runtimeMaximum" -> observation.runtimeMaximum,
      "artifactChecksumSha256" -> observation.artifactChecksumSha256,
      "evidenceLocation" -> observation.evidenceLocation,
      "diagnostics" -> observation.diagnostics
    )

  private def _source_aware_issue_record(issue: ReconciliationIssue): Record =
    Record.dataAuto(
      "code" -> issue.code,
      "message" -> issue.message,
      "sourceIds" -> issue.sourceIds,
      "evidenceLocations" -> issue.evidenceLocations
    )

  private def _source_aware_precedence_record(tier: ReconciliationPrecedenceTier): Record =
    Record.dataAuto(
      "rank" -> tier.rank,
      "sourceKinds" -> tier.sourceKinds,
      "versionStates" -> tier.versionStates,
      "authority" -> tier.authority
    )

  private def _reference_record(profile: ComponentProfile): Record =
    Record.dataAuto(
      "catalogId" -> profile.catalogId,
      "organization" -> profile.organization,
      "name" -> profile.name,
      "title" -> profile.title,
      "kind" -> profile.kind,
      "version" -> profile.selectedVersion.orElse(profile.latestStable).orElse(profile.latestSnapshot).orElse(profile.versions.headOption),
      "evidenceUri" -> profile.evidenceUri.toString
    )

  private def _dependency_record(dependency: ComponentDependency): Record =
    Record.dataAuto(
      "name" -> dependency.name,
      "version" -> dependency.version,
      "kind" -> dependency.kind
    )

  private def _resolved_dependency_record(resolution: ResolvedComponentDependency): Record =
    Record.dataAuto(
      "dependency" -> _dependency_record(resolution.dependency),
      "status" -> resolution.status,
      "depth" -> resolution.depth,
      "path" -> resolution.path,
      "catalogId" -> resolution.resolvedProfile.map(_.catalogId),
      "organization" -> resolution.resolvedProfile.flatMap(_.organization),
      "resolvedVersion" -> resolution.resolvedProfile.flatMap { profile =>
        resolution.dependency.version.orElse(profile.latestStable).orElse(profile.latestSnapshot).orElse(profile.versions.headOption)
      },
      "metadataVersion" -> resolution.resolvedProfile.flatMap(_.dependencyMetadataVersion),
      "evidenceUri" -> resolution.resolvedProfile.map(_.evidenceUri.toString)
    )

  private def _dependency_conflict_record(conflict: ComponentDependencyConflict): Record =
    Record.dataAuto(
      "name" -> conflict.name,
      "kind" -> conflict.kind,
      "versions" -> conflict.versions,
      "paths" -> conflict.paths,
      "message" -> conflict.message
    )

  private def _source_record(state: InformationSourceState): Record =
    {
      val descriptor = state.descriptor
      Record.dataAuto(
        "id" -> descriptor.id,
        "baseUri" -> descriptor.location,
        "sourceKind" -> descriptor.sourceKind,
        "location" -> descriptor.location,
        "authorization" -> descriptor.authorization,
        "enabled" -> descriptor.enabled,
        "priority" -> descriptor.priority,
        "status" -> state.status,
        "componentCount" -> state.observationCount,
        "cacheStatus" -> state.freshness.status,
        "freshness" -> state.freshness.status,
        "refreshedAt" -> state.freshness.observedAt.map(_.toString),
        "expiresAt" -> state.freshness.expiresAt.map(_.toString),
        "lastRefreshAttemptAt" -> state.freshness.lastRefreshAttemptAt.map(_.toString),
        "diagnostics" -> state.diagnostics,
        "warning" -> state.diagnostics.headOption
      )
    }

  private def _source_warnings: Vector[String] =
    _runtime.configurationWarnings ++ _runtime.informationSourceStates(includeDisabled = false).flatMap(_.diagnostics)

  private def _required_string(record: Record, key: String): String =
    _optional_string(record, key)
      .getOrElse(throw new IllegalArgumentException(s"Missing required field: $key"))

  private[cbdsupport] def _optional_string(record: Record, key: String): Option[String] =
    record.getAny(key).flatMap(_scalar_string).map(_.trim).filter(_.nonEmpty)

  private def _scalar_string(value: Any): Option[String] = value match {
    case null => None
    case Some(x) => _scalar_string(x)
    case None => None
    case x: Record => x.getAny("value").flatMap(_scalar_string)
    case x: org.goldenport.record.RecordPresentable => x.toRecord().getAny("value").flatMap(_scalar_string)
    case x: Map[?, ?] => x.iterator.collectFirst {
      case (key, nested) if key.toString == "value" => nested
    }.flatMap(_scalar_string)
    case x => Some(x.toString)
  }

  private def _optional_int(record: Record, key: String): Option[Int] =
    record.getInt(key)

  private def _optional_boolean(record: Record, key: String): Option[Boolean] =
    record.getBoolean(key)
}
