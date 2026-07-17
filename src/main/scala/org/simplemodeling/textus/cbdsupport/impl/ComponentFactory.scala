package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate}
import org.goldenport.cncf.config.{ComponentConfigurationAccess, ComponentConfigurationKey, ComponentConfigurationSources}
import org.goldenport.cncf.context.{GlobalRuntimeContext, ScopeContext}
import org.goldenport.cncf.resource.{ResourceTreeLimits, ResourceTreeReference}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.configuration.Configuration
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.{CbdCatalogAdminService, CbdRetrievalService, CbdReviewAdminService}
import org.simplemodeling.textus.cbdsupport.runtime.{CbdHttp, CbdRuntime, CbdRuntimeInvocation, ComponentDependency, ComponentDependencyConflict, ComponentEvidenceAbsence, ComponentMatch, ComponentObservation, ComponentProfile, ComponentUsage, ComponentUsageGuidance, ExactComponentSelection, InformationSourceState, ResolvedComponentDependency}
import org.simplemodeling.textus.cbdsupport.runtime.{ReconciliationIssue, ReconciliationObservation, ReconciliationPrecedenceTier, SemanticRequirementEvidence, SourceAwareComponentSearchQuery}
import org.simplemodeling.textus.cbdsupport.runtime.{CarReviewAuthorization, CarReviewDevelopmentTemplateProvider, CarReviewMcpReadApplication, CarReviewMcpObservation, CarReviewMcpReport, CarReviewMcpSummary, CarReviewProviderDocumentSubmissionApplication, CarReviewRepository, CarReviewRunApplication, CarReviewSubmissionBoundedAdapter, CarReviewSubmissionWireApplication, CarReviewViewItem, CarReviewViewProjection, CncfCarReviewJobGateway, ReviewDigest, ReviewId, ReviewInstant, ReviewLimitation, ReviewLocation, ReviewProfile, ReviewReportId, ReviewRunAdmission, ReviewStartRequest as RuntimeReviewStartRequest, ReviewTarget, ReviewTargetKind, ReviewVersion}
import org.simplemodeling.textus.cbdsupport.runtime.{InformationSourceAuthorization, InformationSourceDescriptor, InformationSourceKind, LocalInformationInventory, LocalInformationSourceInventory, LocalInspectionPolicy, VersionAvailabilityState}

/*
 * @since   Jul. 14, 2026
 * @version Jul. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentFactory extends CbdSupportComponent.Factory {
  private final case class RuntimeCache(
    configuration: CbdRuntime.Configuration,
    clock: java.time.Clock,
    runtime: CbdRuntime
  )

  private var _runtime_cache: Option[RuntimeCache] = None
  private val _review_application = new CarReviewRunApplication()
  private val _review_repository = new CarReviewRepository()
  private val _review_reads = new CarReviewMcpReadApplication(_review_repository)

  private[cbdsupport] def _review_read_application: CarReviewMcpReadApplication = _review_reads
  private[cbdsupport] def _retain_review_report(report: org.simplemodeling.textus.cbdsupport.runtime.CarReviewReport): org.goldenport.Consequence[Unit] =
    _review_repository.retain(report).fold(error => org.goldenport.Consequence.operationInvalid(error.code), _ => org.goldenport.Consequence.unit)

  override val CbdRetrieval: CbdSupportComponent.CbdRetrievalServiceFactory =
    new CbdRetrievalServiceFactoryImpl()

  override val CbdCatalogAdmin: CbdSupportComponent.CbdCatalogAdminServiceFactory =
    new CbdCatalogAdminServiceFactoryImpl()

  override val CbdReviewAdmin: CbdSupportComponent.CbdReviewAdminServiceFactory =
    new CbdReviewAdminServiceFactoryImpl()

  override protected def create_Component(params: ComponentCreate): Component =
    _create_uninitialized_component()

  private[cbdsupport] def _create_uninitialized_component(): Component =
    new CbdSupportComponent {
      override def mcpReadyServices: Set[String] = Set("CbdRetrieval")
    }

  private final case class RuntimeConfigurationInput(
    configuration: CbdRuntime.Configuration,
    developmenttrees: Option[String],
    localcartree: Option[String],
    cachecartree: Option[String]
  )

  private[impl] def _runtime_for(core: ActionCall.Core): Consequence[CbdRuntimeInvocation] = {
    val access = ComponentConfigurationAccess(ComponentConfigurationSources(
      core.component.flatMap(_.applicationConfig.config).getOrElse(Configuration.empty),
      core.component.flatMap(_.subsystem).map(_.configuration.configuration).getOrElse(Configuration.empty),
      _runtime_configuration(core.executionContext.runtime)
    ))
    _runtime_configuration_input(access).map { input =>
      val admittedinventory = _admitted_local_inventory(
        input.developmenttrees,
        input.localcartree,
        input.cachecartree,
        core
      )
      val sharedruntime = synchronized {
        _runtime_cache match {
          case Some(cache)
            if cache.configuration == input.configuration &&
              _same_runtime_boundary(cache, core.executionContext.clock) =>
            cache.runtime
          case _ =>
            val created = CbdRuntime.create(input.configuration, core.executionContext.clock)
            _runtime_cache = Some(RuntimeCache(input.configuration, core.executionContext.clock, created))
            created
        }
      }
      sharedruntime.invocation(admittedinventory)
    }
  }

  private def _same_runtime_boundary(
    cache: RuntimeCache,
    clock: java.time.Clock
  ): Boolean =
    cache.clock eq clock

  private def _runtime_configuration_input(
    access: ComponentConfigurationAccess
  ): Consequence[RuntimeConfigurationInput] =
    for {
      catalogs <- _configuration(access, "textus.cbd.catalogs")
      catalogorigins <- _configuration(access, "textus.cbd.catalog.allowed-origins")
      boksites <- _configuration(access, "textus.cbd.bok.sites")
      bokorigins <- _configuration(access, "textus.cbd.bok.allowed-origins")
      sieroutes <- _configuration(access, "textus.cbd.sie-bok.routes")
      sieorigins <- _configuration(access, "textus.cbd.sie-bok.allowed-origins")
      sourceauthentication <- _configuration(access, "textus.cbd.source-authentication")
      developmenttrees <- _configuration(access, "textus.cbd.development.trees")
      localcartree <- _configuration(access, "textus.cbd.local-car.tree")
      cachecartree <- _configuration(access, "textus.cbd.cache-car.tree")
    } yield RuntimeConfigurationInput(
      CbdRuntime.Configuration(
        catalogs = catalogs,
        catalogAllowedOrigins = catalogorigins,
        bokSites = boksites,
        bokAllowedOrigins = bokorigins,
        sieBokRoutes = sieroutes,
        sieAllowedOrigins = sieorigins,
        sourceAuthentication = sourceauthentication
      ),
      developmenttrees,
      localcartree,
      cachecartree
    )

  private def _configuration(
    access: ComponentConfigurationAccess,
    name: String
  ): Consequence[Option[String]] =
    access.resolve(ComponentConfigurationKey.optionalString(name)).map(_.value)

  private def _runtime_configuration(scope: ScopeContext): Configuration =
    scope match {
      case runtime: GlobalRuntimeContext => runtime.resolvedConfiguration.configuration
      case _ => scope.parent.map(_runtime_configuration).getOrElse(Configuration.empty)
    }

  private final case class LocalTreeSource(
    descriptor: InformationSourceDescriptor,
    reference: ResourceTreeReference,
    versionstate: String,
    carstorage: Boolean
  )

  private def _admitted_local_inventory(
    developmenttrees: Option[String],
    localcartree: Option[String],
    cachecartree: Option[String],
    core: ActionCall.Core
  ): LocalInformationInventory = {
    val (development, developmentwarnings) = _development_tree_sources(
      developmenttrees
    )
    val (storage, storagewarnings) = _storage_tree_sources(
      localcartree,
      cachecartree
    )
    val sources = development ++ storage
    val inventories = sources.map { source =>
      core.executionContext.resourceTrees.snapshot(source.reference, ResourceTreeLimits.default) match {
        case Consequence.Success(snapshot) if source.carstorage =>
          LocalInformationSourceInventory.inspectCarStorageSnapshot(
            source.descriptor,
            snapshot,
            source.versionstate,
            LocalInspectionPolicy.DEFAULT,
            core.executionContext.clock
          )
        case Consequence.Success(snapshot) =>
          LocalInformationSourceInventory.inspectDevelopmentSnapshot(
            source.descriptor,
            snapshot,
            source.versionstate,
            LocalInspectionPolicy.DEFAULT,
            core.executionContext.clock
          )
        case Consequence.Failure(_) =>
          LocalInformationInventory(
            Vector(source.descriptor),
            Vector.empty,
            Vector(s"Resource tree ${source.reference.name} is unavailable."),
            core.executionContext.clock.instant(),
            Map(source.descriptor.id -> Vector(s"Resource tree ${source.reference.name} is unavailable."))
          )
      }
    }
    LocalInformationInventory(
      sources.map(_.descriptor),
      inventories.flatMap(_.observations),
      (developmentwarnings ++ storagewarnings ++ inventories.flatMap(_.warnings)).distinct,
      core.executionContext.clock.instant(),
      inventories.flatMap(_.sourceDiagnostics).toMap
    )
  }

  private def _development_tree_sources(
    value: Option[String]
  ): (Vector[LocalTreeSource], Vector[String]) = {
    val entries = value.toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)
    val parsed = entries.zipWithIndex.map { case (entry, index) =>
      val pair = entry.split("=", 2)
      val rawid = if (pair.length == 2) pair(0).trim else s"development-${index + 1}"
      val rawtree = if (pair.length == 2) pair(1).trim else pair(0).trim
      if (!rawid.matches("[A-Za-z0-9._-]+")) Left(s"Development resource-tree entry ${index + 1} has an invalid source ID.")
      else ResourceTreeReference.parseC(rawtree) match {
        case Consequence.Success(reference) =>
          Right(LocalTreeSource(
            InformationSourceDescriptor(rawid, InformationSourceKind.DEVELOPMENT_DIRECTORY, s"resource-tree:${reference.name}", 300 + index, true, InformationSourceAuthorization.EXPLICIT),
            reference,
            VersionAvailabilityState.WORKING,
            carstorage = false
          ))
        case Consequence.Failure(_) => Left(s"Development resource-tree entry ${index + 1} has an invalid logical tree name.")
      }
    }
    parsed.collect { case Right(source) => source } -> parsed.collect { case Left(warning) => warning }
  }

  private def _storage_tree_sources(
    local: Option[String],
    cache: Option[String]
  ): (Vector[LocalTreeSource], Vector[String]) = {
    val entries = Vector(
      ("local-car", local, VersionAvailabilityState.LOCAL_PUBLISHED, 400),
      ("cache-car", cache, VersionAvailabilityState.CACHED, 500)
    ).flatMap { case (id, value, versionstate, priority) =>
      value.map { rawtree =>
        ResourceTreeReference.parseC(rawtree) match {
          case Consequence.Success(reference) =>
            Right(LocalTreeSource(
              InformationSourceDescriptor(id, InformationSourceKind.CAR_STORAGE, s"resource-tree:${reference.name}", priority, true, InformationSourceAuthorization.EXPLICIT),
              reference,
              versionstate,
              carstorage = true
            ))
          case Consequence.Failure(_) => Left(s"CAR storage resource-tree ${id} has an invalid logical tree name.")
        }
      }
    }
    entries.collect { case Right(source) => source } -> entries.collect { case Left(warning) => warning }
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

    override def createGetReviewRunActionCall(
      core: ActionCall.Core,
      action: ReviewRunRequest
    ): GetReviewRunActionCall = GetReviewRunActionCallImpl(core, action)

    override def createGetReviewSummaryActionCall(
      core: ActionCall.Core,
      action: GetReviewSummaryRequest
    ): GetReviewSummaryActionCall = GetReviewSummaryActionCallImpl(core, action)

    override def createGetReviewReportActionCall(
      core: ActionCall.Core,
      action: GetReviewReportRequest
    ): GetReviewReportActionCall = GetReviewReportActionCallImpl(core, action)

    override def createListReviewFindingsActionCall(
      core: ActionCall.Core,
      action: ListReviewFindingsRequest
    ): ListReviewFindingsActionCall = ListReviewFindingsActionCallImpl(core, action)

    override def createListReviewAssurancesActionCall(
      core: ActionCall.Core,
      action: ListReviewAssurancesRequest
    ): ListReviewAssurancesActionCall = ListReviewAssurancesActionCallImpl(core, action)

    override def createGetReviewViewsActionCall(
      core: ActionCall.Core,
      action: GetReviewViewsRequest
    ): GetReviewViewsActionCall = GetReviewViewsActionCallImpl(core, action)
  }

  private final class CbdCatalogAdminServiceFactoryImpl
    extends CbdSupportComponent.CbdCatalogAdminServiceFactory {
    import CbdCatalogAdminService.*

    override def createRefreshCatalogActionCall(
      core: ActionCall.Core,
      action: CatalogRefreshRequest
    ): RefreshCatalogActionCall = RefreshCatalogActionCallImpl(core, action)
  }

  private final class CbdReviewAdminServiceFactoryImpl
    extends CbdSupportComponent.CbdReviewAdminServiceFactory {
    import CbdReviewAdminService.*

    override def createStartReviewActionCall(
      core: ActionCall.Core,
      action: ReviewStartRequest
    ): StartReviewActionCall = StartReviewActionCallImpl(core, action)

    override def createCancelReviewActionCall(
      core: ActionCall.Core,
      action: ReviewCancelRequest
    ): CancelReviewActionCall = CancelReviewActionCallImpl(core, action)

    override def createSubmitReviewDocumentsActionCall(
      core: ActionCall.Core,
      action: ReviewSubmissionRequest
    ): SubmitReviewDocumentsActionCall = SubmitReviewDocumentsActionCallImpl(core, action)

    override def createPostActionCall(
      core: ActionCall.Core,
      action: ReviewHttpSubmissionRequest
    ): PostActionCall = PostActionCallImpl(core, action)
  }

  private final case class SearchComponentsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentSearchRequest
  ) extends CbdRetrievalService.SearchComponentsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).flatMap { runtime =>
        val fetcher = new CbdHttp(core)
        runtime.ensureInputsReady(fetcher).flatMap { _ =>
          val requirement = _required_string(action.record, "requirement")
          val limit = _optional_int(action.record, "limit").getOrElse(10)
          runtime.searchSieTerms(requirement, None, limit, fetcher).map { siesnapshots =>
            val result = runtime.searchSourceAware(SourceAwareComponentSearchQuery(
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
            ), siesnapshots)
            OperationResponse(Record.dataAuto(
              "status" -> (if (result.report.observations.nonEmpty || result.semanticEvidence.nonEmpty) "matched" else "no-match"),
              "results" -> result.matches.map(_match_record(runtime, _)),
              "observations" -> result.report.observations.map(_source_aware_observation_record),
              "semanticEvidence" -> result.semanticEvidence.map(_semantic_evidence_record),
              "issues" -> result.report.issues.map(_source_aware_issue_record),
              "precedence" -> result.report.precedence.map(_source_aware_precedence_record),
              "selectedObservation" -> result.report.selectedObservation.map(_source_aware_observation_record),
              "warnings" -> (result.warnings ++ _source_warnings(runtime)).distinct
            ))
          }
        }
      }
    }
  }

  private final case class GetComponentActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentLookupRequest
  ) extends CbdRetrievalService.GetComponentActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).flatMap { runtime =>
        val fetcher = new CbdHttp(core)
        runtime.ensureInputsReady(fetcher).map { _ =>
          val selection = _selection(runtime, action.record, _optional_string(action.record, "kind"))
          val profile = selection.selectedProfile
          OperationResponse(Record.dataAuto(
            "status" -> selection.status,
            "reference" -> profile.map(_reference_record),
            "component" -> profile.map(_profile_record(runtime, _)),
            "alternatives" -> selection.alternatives.map(_reference_record),
            "candidateCount" -> selection.candidateCount,
            "absences" -> selection.absences.map(_absence_record),
            "warnings" -> (selection.warnings ++ _source_warnings(runtime)).distinct
          ))
        }
      }
    }
  }

  private final case class GetUsageActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ComponentUsageRequest
  ) extends CbdRetrievalService.GetUsageActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).flatMap { runtime =>
        val fetcher = new CbdHttp(core)
        runtime.ensureInputsReady(fetcher).flatMap { _ =>
          val selection = _selection(runtime, action.record, _optional_string(action.record, "kind"))
          selection.selectedProfile match {
            case Some(profile) =>
              runtime.usage(profile, _optional_string(action.record, "intent"), fetcher).map { usage =>
                OperationResponse(_usage_record(runtime, usage, selection))
              }
            case None =>
              org.goldenport.Consequence.success(OperationResponse(_unselected_usage_record(runtime, selection)))
          }
        }
      }
    }
  }

  private final case class ResolveDependenciesActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.DependencyResolutionRequest
  ) extends CbdRetrievalService.ResolveDependenciesActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).flatMap { runtime =>
        val fetcher = new CbdHttp(core)
        runtime.ensureInputsReady(fetcher).map { _ =>
          val selection = _selection(runtime, action.record, _optional_string(action.record, "kind"))
          val profile = selection.selectedProfile
          val resolution = profile.map(runtime.resolveDependencies(
            _,
            _optional_string(action.record, "version"),
            _optional_int(action.record, "maxDepth").getOrElse(CbdRuntime.DEFAULT_DEPENDENCY_DEPTH)
          ))
          OperationResponse(Record.dataAuto(
            "status" -> selection.status,
            "reference" -> profile.map(_reference_record),
            "component" -> profile.map(_profile_record(runtime, _)),
            "dependencies" -> resolution.toVector.flatMap(_.directDependencies).map(_dependency_record),
            "resolutions" -> resolution.toVector.flatMap(_.resolutions).map(_resolved_dependency_record),
            "conflicts" -> resolution.toVector.flatMap(_.conflicts).map(_dependency_conflict_record),
            "alternatives" -> selection.alternatives.map(_reference_record),
            "candidateCount" -> selection.candidateCount,
            "absences" -> (selection.absences ++ resolution.toVector.flatMap(_.absences)).map(_absence_record),
            "warnings" -> (profile.toVector.flatMap(_.warnings) ++ resolution.toVector.flatMap(_.warnings) ++ selection.warnings ++ _source_warnings(runtime)).distinct
          ))
        }
      }
    }
  }

  private final case class ListCatalogsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.CatalogListRequest
  ) extends CbdRetrievalService.ListCatalogsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).map { runtime =>
        OperationResponse(Record.dataAuto(
          "sources" -> runtime.informationSourceStates(_optional_boolean(action.record, "includeDisabled").getOrElse(false)).map(_source_record),
          "warnings" -> runtime.configurationWarnings
        ))
      }
    }
  }

  private final case class StatusActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.CbdStatusRequest
  ) extends CbdRetrievalService.StatusActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).map { runtime =>
        val states = runtime.informationSourceStates(includeDisabled = false)
        OperationResponse(Record.dataAuto(
          "overall" -> runtime.overallStatus,
          "sourceCount" -> states.size,
          "readySourceCount" -> states.count(_.status == "ready"),
          "componentCount" -> runtime.componentCount,
          "detail" -> _optional_string(action.record, "detail").orElse(Some(states.map(x => s"${x.descriptor.id}=${x.status}").mkString(", ")))
        ))
      }
    }
  }

  private final case class GetReviewRunActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ReviewRunRequest
  ) extends CbdRetrievalService.GetReviewRunActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      _review_gateway(core).flatMap { gateway =>
        _review_application.get(
          ReviewId(_required_string(action.record, "reviewId")),
          CarReviewAuthorization.roles(core.executionContext),
          gateway
        ).map(admission => OperationResponse(_review_run_record(admission)))
      }
    }
  }

  private final case class GetReviewSummaryActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.GetReviewSummaryRequest
  ) extends CbdRetrievalService.GetReviewSummaryActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _review_reads.summary(
        ReviewReportId(_required_string(action.record, "reportId")),
        CarReviewAuthorization.roles(core.executionContext)
      ).map(summary => OperationResponse(_review_summary_record(summary)))
    }
  }

  private final case class GetReviewReportActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.GetReviewReportRequest
  ) extends CbdRetrievalService.GetReviewReportActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _review_reads.report(
        ReviewReportId(_required_string(action.record, "reportId")),
        CarReviewAuthorization.roles(core.executionContext)
      ).map(report => OperationResponse(_review_report_record(report)))
    }
  }

  private final case class ListReviewFindingsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ListReviewFindingsRequest
  ) extends CbdRetrievalService.ListReviewFindingsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _review_reads.findings(
        ReviewReportId(_required_string(action.record, "reportId")),
        CarReviewAuthorization.roles(core.executionContext),
        _optional_int(action.record, "limit").getOrElse(CarReviewMcpReadApplication.MAX_OBSERVATIONS)
      ).map(observations => OperationResponse(Record.dataAuto("observations" -> observations.map(_review_observation_record))))
    }
  }

  private final case class ListReviewAssurancesActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.ListReviewAssurancesRequest
  ) extends CbdRetrievalService.ListReviewAssurancesActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _review_reads.assurances(
        ReviewReportId(_required_string(action.record, "reportId")),
        CarReviewAuthorization.roles(core.executionContext),
        _optional_int(action.record, "limit").getOrElse(CarReviewMcpReadApplication.MAX_OBSERVATIONS)
      ).map(observations => OperationResponse(Record.dataAuto("observations" -> observations.map(_review_observation_record))))
    }
  }

  private final case class GetReviewViewsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdRetrievalService.GetReviewViewsRequest
  ) extends CbdRetrievalService.GetReviewViewsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _review_reads.views(
        ReviewReportId(_required_string(action.record, "reportId")),
        CarReviewAuthorization.roles(core.executionContext)
      ).map(views => OperationResponse(_review_views_record(views)))
    }
  }

  private final case class RefreshCatalogActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdCatalogAdminService.CatalogRefreshRequest
  ) extends CbdCatalogAdminService.RefreshCatalogActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      _runtime_for(core).flatMap { runtime =>
        val fetcher = new CbdHttp(core)
        runtime.refresh(_optional_string(action.record, "sourceId"), fetcher).map { states =>
          OperationResponse(Record.dataAuto(
            "status" -> runtime.overallStatus,
            "sourceCount" -> states.count(_.source.enabled),
            "componentCount" -> runtime.componentCount,
            "warnings" -> states.flatMap(_.warning)
          ))
        }
      }
    }
  }

  private final case class StartReviewActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdReviewAdminService.ReviewStartRequest
  ) extends CbdReviewAdminService.StartReviewActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      val ctx = core.executionContext
      val now = ReviewInstant(ctx.clock.instant().toString)
      val reviewid = ReviewId(s"review-${ctx.idGeneration.opaqueId("cbd.review")}")
      val request = RuntimeReviewStartRequest(
        reviewid,
        ReviewTarget(
          ReviewTargetKind(_required_string(action.record, "targetKind")),
          _optional_string(action.record, "organization"),
          _required_string(action.record, "name"),
          _optional_string(action.record, "version").map(ReviewVersion.apply),
          ReviewDigest(_required_string(action.record, "targetDigest"))
        ),
        ReviewProfile(_required_string(action.record, "profile")),
        now
      )
      _review_gateway(core).flatMap { gateway =>
        _review_application.start(
          request,
          CarReviewAuthorization.roles(ctx),
          gateway
        ).map(admission => OperationResponse(_review_run_record(admission)))
      }
    }
  }

  private final case class CancelReviewActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdReviewAdminService.ReviewCancelRequest
  ) extends CbdReviewAdminService.CancelReviewActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      val ctx = core.executionContext
      _review_gateway(core).flatMap { gateway =>
        _review_application.cancel(
          ReviewId(_required_string(action.record, "reviewId")),
          CarReviewAuthorization.roles(ctx),
          ReviewInstant(ctx.clock.instant().toString),
          gateway
        ).map(admission => OperationResponse(_review_run_record(admission)))
      }
    }
  }

  private final case class SubmitReviewDocumentsActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdReviewAdminService.ReviewSubmissionRequest
  ) extends CbdReviewAdminService.SubmitReviewDocumentsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      val ctx = core.executionContext
      val application = _review_submission_application(ctx)
      application.submit(
        _required_string(action.record, "submissionDocument"),
        CarReviewAuthorization.roles(ctx)
      ).map { response =>
        OperationResponse(Record.dataAuto("canonicalResponse" -> response))
      }
    }
  }

  private final case class PostActionCallImpl(
    core: ActionCall.Core,
    override val action: CbdReviewAdminService.ReviewHttpSubmissionRequest
  ) extends CbdReviewAdminService.PostActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      val ctx = core.executionContext
      val application = _review_submission_application(ctx)
      application.submit(
        _required_string(action.record, "submissionDocument"),
        CarReviewAuthorization.roles(ctx)
      ).map { response =>
        OperationResponse(Record.dataAuto("canonicalResponse" -> response))
      }
    }
  }

  private def _review_submission_application(
    ctx: org.goldenport.cncf.context.ExecutionContext
  ): CarReviewSubmissionBoundedAdapter = {
    val now = ReviewInstant(ctx.clock.instant().toString)
    new CarReviewSubmissionBoundedAdapter(new CarReviewSubmissionWireApplication(
      new CarReviewProviderDocumentSubmissionApplication(
        new CarReviewDevelopmentTemplateProvider(
          now,
          () => ReviewReportId(s"report-${ctx.idGeneration.opaqueId("cbd.review.report")}")
        )
      ),
      response => _retain_review_report(response.report)
    ))
  }

  private def _selection(runtime: CbdRuntimeInvocation, record: Record, kind: Option[String]): ExactComponentSelection =
    runtime.selectComponent(
      _required_string(record, "name"),
      _optional_string(record, "organization"),
      kind,
      _optional_string(record, "version"),
      _optional_string(record, "catalogId")
    )

  private def _usage_record(runtime: CbdRuntimeInvocation, usage: ComponentUsage, selection: ExactComponentSelection): Record =
    Record.dataAuto(
      "status" -> "matched",
      "reference" -> _reference_record(usage.profile),
      "component" -> _profile_record(runtime, usage.profile),
      "intent" -> usage.intent,
      "selectedSourceId" -> usage.selectedSourceId,
      "selectedSourceKind" -> usage.selectedSourceKind,
      "selectedVersion" -> usage.selectedVersion,
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
      "guidance" -> usage.guidance.map(_usage_guidance_record),
      "alternatives" -> selection.alternatives.map(_reference_record),
      "candidateCount" -> selection.candidateCount,
      "absences" -> usage.absences.map(_absence_record),
      "warnings" -> (usage.warnings ++ selection.warnings ++ _source_warnings(runtime)).distinct
    )

  private[cbdsupport] def _unselected_usage_record(runtime: CbdRuntimeInvocation, selection: ExactComponentSelection): Record =
    Record.dataAuto(
      "status" -> selection.status,
      "operations" -> Vector.empty[Record],
      "references" -> Vector.empty[Record],
      "guidance" -> Vector.empty[Record],
      "alternatives" -> selection.alternatives.map(_reference_record),
      "candidateCount" -> selection.candidateCount,
      "absences" -> selection.absences.map(_absence_record),
      "warnings" -> (selection.warnings ++ _source_warnings(runtime)).distinct
    )

  private[cbdsupport] def _unselected_usage_record(selection: ExactComponentSelection): Record =
    Record.dataAuto(
      "status" -> selection.status,
      "operations" -> Vector.empty[Record],
      "references" -> Vector.empty[Record],
      "guidance" -> Vector.empty[Record],
      "alternatives" -> selection.alternatives.map(_reference_record),
      "candidateCount" -> selection.candidateCount,
      "absences" -> selection.absences.map(_absence_record),
      "warnings" -> selection.warnings
    )

  private[cbdsupport] def _absence_record(absence: ComponentEvidenceAbsence): Record =
    Record.dataAuto(
      "code" -> absence.code,
      "subject" -> absence.subject,
      "message" -> absence.message,
      "sourceIds" -> absence.sourceIds,
      "versions" -> absence.versions,
      "evidenceUris" -> absence.evidenceUris.map(_.toString)
    )

  private[cbdsupport] def _usage_guidance_record(guidance: ComponentUsageGuidance): Record =
    Record.dataAuto(
      "statementKind" -> guidance.statementKind,
      "intent" -> guidance.intent,
      "statement" -> guidance.statement,
      "sourceId" -> guidance.sourceId,
      "sourceKind" -> guidance.sourceKind,
      "version" -> guidance.version,
      "service" -> guidance.service,
      "operation" -> guidance.operation,
      "score" -> guidance.score,
      "evidenceUris" -> guidance.evidenceUris.map(_.toString),
      "rationale" -> guidance.rationale
    )

  private def _match_record(runtime: CbdRuntimeInvocation, result: ComponentMatch): Record =
    Record.dataAuto(
      "component" -> _profile_record(runtime, result.profile),
      "reference" -> _reference_record(result.profile),
      "matchKind" -> result.matchKind,
      "score" -> result.score,
      "rationale" -> result.rationale,
      "semanticEvidenceIds" -> result.semanticEvidenceIds
    )

  private[cbdsupport] def _semantic_evidence_record(evidence: SemanticRequirementEvidence): Record =
    Record.dataAuto(
      "id" -> evidence.id,
      "sourceId" -> evidence.sourceId,
      "sourceKind" -> evidence.sourceKind,
      "termId" -> evidence.termId,
      "title" -> evidence.title,
      "definition" -> evidence.definition,
      "category" -> evidence.category,
      "aliases" -> evidence.aliases,
      "datasetId" -> evidence.datasetId,
      "matchKind" -> evidence.matchKind,
      "score" -> evidence.score,
      "rationale" -> evidence.rationale,
      "freshness" -> evidence.freshness,
      "observedAt" -> evidence.observedAt.toString,
      "evidenceUri" -> evidence.evidenceLocation,
      "diagnostics" -> evidence.diagnostics
    )

  private def _profile_record(runtime: CbdRuntimeInvocation, profile: ComponentProfile): Record =
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
      "observation" -> runtime.observation(profile).map(_observation_record),
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

  private[cbdsupport] def _source_record(state: InformationSourceState): Record =
    {
      val descriptor = state.descriptor
      Record.dataAuto(
        "id" -> descriptor.id,
        "baseUri" -> descriptor.location,
        "sourceKind" -> descriptor.sourceKind,
        "location" -> descriptor.location,
        "authorization" -> descriptor.authorization,
        "authenticationScheme" -> descriptor.authenticationScheme,
        "credentialConfigured" -> descriptor.credentialConfigured,
        "enabled" -> descriptor.enabled,
        "priority" -> descriptor.priority,
        "status" -> state.status,
        "componentCount" -> state.observationCount,
        "cacheStatus" -> state.freshness.status,
        "freshness" -> state.freshness.status,
        "refreshedAt" -> state.freshness.observedAt.map(_.toString),
        "expiresAt" -> state.freshness.expiresAt.map(_.toString),
        "lastRefreshAttemptAt" -> state.freshness.lastRefreshAttemptAt.map(_.toString),
        "nextRefreshAttemptAt" -> state.freshness.nextRefreshAttemptAt.map(_.toString),
        "diagnostics" -> state.diagnostics,
        "warning" -> state.diagnostics.headOption
      )
    }

  private def _source_warnings(runtime: CbdRuntimeInvocation): Vector[String] =
    runtime.configurationWarnings ++ runtime.informationSourceStates(includeDisabled = false).flatMap(_.diagnostics)

  private def _review_gateway(
    core: ActionCall.Core
  ): org.goldenport.Consequence[CncfCarReviewJobGateway] =
    core.component
      .map(component => org.goldenport.Consequence.success(new CncfCarReviewJobGateway(component.jobEngine)))
      .getOrElse(org.goldenport.Consequence.operationInvalid("CNCF component context is required for Review Job execution."))

  private[cbdsupport] def _review_run_record(admission: ReviewRunAdmission): Record = {
    val run = admission.run
    Record.dataAuto(
      "schemaVersion" -> run.schemaVersion.value,
      "documentType" -> run.documentType.value,
      "reviewId" -> run.reviewId.value,
      "jobId" -> admission.binding.jobId.value,
      "targetKind" -> run.target.kind.value,
      "organization" -> run.target.organization,
      "name" -> run.target.name,
      "version" -> run.target.version.map(_.value),
      "targetDigest" -> run.target.digest.value,
      "profile" -> run.profile.value,
      "state" -> run.state.value,
      "limitations" -> run.limitations.map { limitation =>
        Record.dataAuto(
          "code" -> limitation.code,
          "scope" -> limitation.scope.value,
          "subjectId" -> limitation.subjectId,
          "message" -> limitation.message,
          "retryable" -> limitation.retryable
        )
      },
      "startedAt" -> run.startedAt.value,
      "updatedAt" -> run.updatedAt.value,
      "completedAt" -> run.completedAt.map(_.value),
      "reportId" -> run.reportId.map(_.value),
      "reportDigest" -> run.reportDigest.map(_.value),
      "failureCode" -> run.failureCode.map(_.value)
    )
  }

  private[cbdsupport] def _review_summary_record(summary: CarReviewMcpSummary): Record =
    Record.dataAuto(
      "reviewId" -> summary.reviewId.value,
      "reportId" -> summary.reportId.value,
      "reportDigest" -> summary.reportDigest.value,
      "target" -> summary.target,
      "profile" -> summary.profile.value,
      "gate" -> summary.gate.value,
      "findingCount" -> summary.findingCount,
      "assuranceCount" -> summary.assuranceCount,
      "unknownCount" -> summary.unknownCount
    )

  private[cbdsupport] def _review_report_record(report: CarReviewMcpReport): Record =
    Record.dataAuto(
      "summary" -> _review_summary_record(report.summary),
      "providers" -> report.providers.map { provider =>
        Record.dataAuto(
          "providerId" -> provider.provider.id.value,
          "version" -> provider.provider.version.value,
          "state" -> provider.state.value,
          "limitations" -> provider.limitations.map(_review_limitation_record)
        )
      },
      "observations" -> report.observations.map(_review_observation_record),
      "limitations" -> report.limitations.map(_review_limitation_record)
    )

  private[cbdsupport] def _review_observation_record(observation: CarReviewMcpObservation): Record =
    Record.dataAuto(
      "id" -> observation.id.value,
      "observationType" -> observation.`type`.value,
      "ruleId" -> observation.ruleId.value,
      "message" -> observation.message,
      "severity" -> observation.severity.map(_.value),
      "providerId" -> observation.providerId.value,
      "locations" -> observation.locations
    )

  private[cbdsupport] def _review_views_record(views: CarReviewViewProjection): Record =
    Record.dataAuto(
      "cncf" -> views.cncf.map(_review_view_record),
      "implementation" -> views.implementation.map(_review_view_record),
      "quality" -> views.quality.map(_review_view_record)
    )

  private[cbdsupport] def _review_view_record(view: CarReviewViewItem): Record =
    Record.dataAuto(
      "key" -> view.key,
      "observationIds" -> view.observationIds.map(_.value),
      "evidenceIds" -> view.evidenceIds.map(_.value),
      "providerLinks" -> view.providerLinks.map { link =>
        Record.dataAuto(
          "providerId" -> link.provider.id.value,
          "providerVersion" -> link.provider.version.value,
          "ruleId" -> link.ruleSet.id.value,
          "ruleVersion" -> link.ruleSet.version.value,
          "bundleDigest" -> link.bundleDigest.value
        )
      },
      "locations" -> view.locations.flatMap(_safe_review_location).distinct.sorted
    )

  private def _safe_review_location(location: ReviewLocation): Option[String] =
    CarReviewMcpReadApplication.renderLocation(location)

  private[cbdsupport] def _review_limitation_record(limitation: ReviewLimitation): Record =
    Record.dataAuto(
      "code" -> limitation.code,
      "scope" -> limitation.scope.value,
      "subjectId" -> limitation.subjectId,
      "message" -> limitation.message,
      "retryable" -> limitation.retryable
    )

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
