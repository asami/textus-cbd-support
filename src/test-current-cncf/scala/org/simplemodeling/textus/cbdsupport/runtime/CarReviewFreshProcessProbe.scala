package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.{Files, Path}
import io.circe.Json
import io.circe.parser.parse
import org.goldenport.cncf.job.{JobId, JobSubmitter, NativeCbdReviewJobFixtures}
import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.simplemodeling.textus.cbdsupport.impl.ComponentFactory
import org.simplemodeling.textus.cbdsupport.entity.{ReviewJobBinding, ReviewReportSnapshot, ReviewAttestationSnapshot}

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CarReviewFreshProcessProbe {
  import CarReviewJobExecutionFixtures.{value, failure}
  def main(args: Array[String]): Unit = {
    require(args.length == 2)
    val mode = args(0)
    val root = Path.of(args(1)).toAbsolutePath
    val manifest = root.resolve("writer.json")
    val previous = if (mode == "reader") Some(parse(Files.readString(manifest)).toOption.get) else None
    val jobs = previous.toVector.map(json => value(JobId.parse(json.hcursor.get[String]("jobId").toOption.get)))
    if (previous.exists(_.hcursor.get[String]("mode").contains("writer-running"))) {
      val refused = value(NativeCbdReviewJobFixtures.refusedStartup(root, jobs, JobSubmitter("review-owner", "User"), selected => {
        val component = value(new ComponentFactory().createPrimaryC(ComponentCreate(selected, ComponentOrigin.Builtin)))
        Vector(value(new org.goldenport.cncf.component.ComponentFactory().bootstrapC(component)))
      }))
      require(refused.message == "durable runtime startup refused")
      require(!refused.executionAvailable && refused.drained == 0)
      Files.writeString(root.resolve("reader.json"), Json.obj("pid" -> Json.fromLong(ProcessHandle.current().pid()),
        "executions" -> Json.fromInt(refused.drained),
        "startupRefusal" -> Json.fromString(refused.message)).noSpaces)
      return
    }
    val fixture = new CarReviewProductionFixture(root, jobs)
    try {
      val evidence = previous match {
        case None =>
          val first = value(fixture.start())
          val reviewid = first.getString("reviewId").get
          val jobid = first.getString("jobId").get
          if (mode == "writer-running") {
            fixture.beforeProvider = () => {
              require(fixture.native.signedStatus(value(JobId.parse(jobid))) == "running")
              Files.writeString(manifest, Json.obj("mode" -> Json.fromString(mode), "pid" -> Json.fromLong(ProcessHandle.current().pid()),
                "reviewId" -> Json.fromString(reviewid), "jobId" -> Json.fromString(jobid)).noSpaces)
              System.exit(0)
              throw new IllegalStateException("writer exit returned")
            }
          }
          value(fixture.native.drain())
          require(fixture.executions == 1)
          val completed = value(fixture.get(reviewid))
          require(completed.getString("state").contains("completed"))
          val reportid = completed.getString("reportId").get
          value(fixture.report(reportid))
          _state(fixture, reviewid, jobid, reportid).deepMerge(Json.obj("mode" -> Json.fromString(mode)))
        case Some(expected) =>
          val reviewid = expected.hcursor.get[String]("reviewId").toOption.get
          val jobid = expected.hcursor.get[String]("jobId").toOption.get
          val completed = value(fixture.get(reviewid))
          val reportid = expected.hcursor.get[String]("reportId").toOption.get
          require(completed.getString("state").contains("completed"))
          require(completed.getString("reportId").contains(reportid))
          value(fixture.report(reportid))
          val foreign = fixture.context("another-operator")
          require(failure(fixture.get(reviewid, foreign)).contains("review-job-not-owned"))
          require(failure(fixture.cancel(reviewid, fixture.context("another-operator"))).contains("review-job-not-owned"))
          require(failure(fixture.report(reportid, fixture.context("another-operator"))).contains("review-job-not-owned"))
          value(fixture.get(reviewid))
          value(fixture.native.drain())
          require(fixture.executions == 0)
          val current = _state(fixture, reviewid, jobid, reportid)
          Vector("reviewId", "jobId", "reportId", "report", "attestation", "bindingCount", "reportCount", "attestationCount", "jobStatus").foreach { field =>
            require(current.hcursor.downField(field).focus == expected.hcursor.downField(field).focus, field)
          }
          current
      }
      Files.writeString(root.resolve(if (mode == "reader") "reader.json" else "writer.json"), evidence.deepMerge(
        Json.obj("pid" -> Json.fromLong(ProcessHandle.current().pid()), "executions" -> Json.fromInt(fixture.executions))).noSpaces)
    } finally fixture.close()
  }

  private def _state(fixture: CarReviewProductionFixture, reviewid: String, jobid: String, reportid: String): Json = {
    val reports = fixture.rows[ReviewReportSnapshot](ReviewReportSnapshot.collectionId)
    val attestations = fixture.rows[ReviewAttestationSnapshot](ReviewAttestationSnapshot.collectionId)
    require(reports.size == 1 && attestations.size == 1)
    val report = reports.head
    val attestation = attestations.head
    require(report.review_id.value == reviewid && report.report_id.value == reportid && report.job_id.exists(_.value == jobid))
    val signed = parse(attestation.attestation_document.value.value).toOption.get
    require(attestation.review_id.value == reviewid && attestation.report_id.value == reportid &&
      signed.hcursor.get[String]("attestationId").contains(report.attestation_id.value))
    Json.obj("reviewId" -> Json.fromString(reviewid), "jobId" -> Json.fromString(jobid), "reportId" -> Json.fromString(reportid),
      "report" -> parse(report.report_document.value.value).toOption.get,
      "attestation" -> parse(attestation.attestation_document.value.value).toOption.get,
      "bindingCount" -> Json.fromInt(fixture.count[ReviewJobBinding](ReviewJobBinding.collectionId)),
      "reportCount" -> Json.fromInt(reports.size), "attestationCount" -> Json.fromInt(attestations.size),
      "jobStatus" -> Json.fromString(fixture.native.signedStatus(value(JobId.parse(jobid)))))
  }
}
