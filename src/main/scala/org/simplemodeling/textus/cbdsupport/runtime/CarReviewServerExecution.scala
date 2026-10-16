package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final case class CarReviewServerExecution(
  plan: CarReviewExecutionPlan,
  template: CarReviewReport,
  providers: Vector[CarReviewServerProviderCall],
  application: CarReviewProviderExecutionApplication
)

/** The transport cannot supply plan identity, provider descriptors or executable paths. */
object CarReviewServerExecution {
  val configurationKey: String = "textus.cbd.review.execution"
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private final case class Provider(descriptor: String, request: Json)
  private final case class Configuration(
    reference: CarReviewExecutionPlanReference,
    selection: CarReviewReuseKeyInput,
    template: CarReviewReport,
    providers: Vector[Provider]
  )

  def selectC(
    document: String,
    request: ReviewStartRequest
  )(using ctx: ExecutionContext): Consequence[CarReviewServerExecution] =
    for {
      configuration <- _result(_decode(document))
      plan <- _result(CarReviewExecutionPlan.create(request, configuration.reference, configuration.selection).left.map(_.code))
      calls <- _result(configuration.providers.foldLeft(Right(Vector.empty): Either[String, Vector[CarReviewServerProviderCall]]) {
        case (result, provider) => for {
          previous <- result
          descriptor <- CarReviewProviderBundleAdmission.describeDescriptor(provider.descriptor)
          selection <- configuration.selection.providerSelections.find(_.provider == descriptor.provider)
            .toRight("review-server-provider-selection-missing")
          _ <- Either.cond(descriptor.provider.id == ReviewProviderId("cozy"), (), "review-server-provider-unsupported")
        } yield previous :+ CarReviewServerProviderCall(selection, ProviderBundleExecutionRequest(
          request.reviewId, request.target, descriptor.provider, ProviderBundleAvailability.Enabled,
          provider.descriptor, _request(provider.request, request).noSpaces, ctx.clock.instant().toEpochMilli))
      })
      registry <- _registry(calls)
    } yield CarReviewServerExecution(plan, configuration.template.copy(
      reviewId = request.reviewId,
      reportId = ReviewReportId(s"report-${ctx.idGeneration.opaqueId("cbd.review-report")}"),
      target = request.target, profile = request.profile, createdAt = request.startedAt,
      execution = configuration.template.execution.copy(startedAt = request.startedAt, completedAt = request.startedAt)
    ), calls, new CarReviewProviderExecutionApplication(registry, new CarReviewProviderExecutionCoordinator))

  private def _registry(calls: Vector[CarReviewServerProviderCall])(using ctx: ExecutionContext): Consequence[CarReviewProviderRegistry] =
    calls.foldLeft(Consequence.success(new CarReviewProviderRegistry)) { (result, call) =>
      for {
        registry <- result
        runner <- CozyCarReviewProviderRunner.fromScopeC(call.request.target, call.selection.provider, ctx.scope)
        _ <- _result(registry.register(call.request.descriptor, runner).left.map(_.code))
      } yield registry
    }

  private def _decode(document: String): Either[String, Configuration] =
    for {
      text <- Option(document).filter(_.trim.nonEmpty).toRight("review-execution-plan-required")
      json <- _parser.parse(text).left.map(_ => "review-server-plan-invalid")
      root <- _object(json, Set("schemaVersion", "diagnosisId", "revision", "selection", "template", "providers"))
      schema <- root("schemaVersion").flatMap(_.asString).toRight("review-server-plan-invalid")
      _ <- Either.cond(schema == "textus.cbd.review-server.v1", (), "review-server-plan-schema-unsupported")
      identity <- root("diagnosisId").flatMap(_.asString).toRight("review-plan-reference-invalid")
      id <- EntityId.parse(identity) match {
        case Consequence.Success(value) => Right(value)
        case _ => Left("review-plan-reference-invalid")
      }
      revision <- root("revision").flatMap(_.asNumber).flatMap(_.toLong).filter(_ > 0L).toRight("review-plan-reference-invalid")
      selection <- CarReviewExecutionSelectionCodec.decode(root("selection").get.noSpaces).left.map(_.code)
      template <- CarReviewReportCodec.decode(root("template").get.noSpaces).left.map(_.code)
      providers <- root("providers").flatMap(_.asArray).filter(values => values.nonEmpty && values.size <= 8)
        .toRight("review-server-providers-invalid")
      decoded <- providers.foldLeft(Right(Vector.empty): Either[String, Vector[Provider]]) { (result, json) =>
        for {
          previous <- result
          provider <- _object(json, Set("descriptor", "request"))
          request <- provider("request").filter(_.isObject).toRight("review-server-provider-request-invalid")
          descriptor <- provider("descriptor").filter(_.isObject).toRight("review-server-provider-descriptor-invalid")
        } yield previous :+ Provider(descriptor.noSpaces, request)
      }
    } yield Configuration(CarReviewExecutionPlanReference(id, revision), selection, template, decoded)

  private def _object(json: Json, fields: Set[String]): Either[String, JsonObject] =
    json.asObject.filter(_.keys.toSet == fields).toRight("review-server-plan-invalid")

  private def _request(json: Json, request: ReviewStartRequest): Json =
    json.mapObject(_.add("reviewId", Json.fromString(request.reviewId.value)).add("target", Json.obj(
      "kind" -> Json.fromString(request.target.kind.value),
      "organization" -> request.target.organization.fold(Json.Null)(Json.fromString),
      "name" -> Json.fromString(request.target.name),
      "version" -> request.target.version.fold(Json.Null)(value => Json.fromString(value.value)),
      "digest" -> Json.fromString(request.target.digest.value)
    ).dropNullValues))

  private def _result[A](result: Either[String, A]): Consequence[A] =
    result.fold(Consequence.operationInvalid(_), Consequence.success)
}
