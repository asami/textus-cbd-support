package org.simplemodeling.textus.cbdsupport.runtime

import java.io.{ByteArrayOutputStream, InputStream}
import java.net.{HttpURLConnection, URI}
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.util.control.NonFatal

import io.circe.{Json, Printer}
import io.circe.parser.parse

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Stable CLI result. `runId` is the CBD Review ID carried by the canonical Report. */
final case class CarReviewCliResult(runId: String, response: String, gate: String) {
  def exitCode: Int = gate match {
    case "pass" => 0
    case "fail" => 2
    case "unknown" => 3
    case _ => 1
  }

  def render: String =
    Printer.noSpaces.copy(sortKeys = true).print(Json.obj(
      "documentType" -> Json.fromString("review-cli-result"),
      "gateResult" -> Json.fromString(gate),
      "response" -> parse(response).getOrElse(Json.Null),
      "runId" -> Json.fromString(runId),
      "schemaVersion" -> Json.fromString("textus.cbd.review-submission.v1")
    ))
}

/**
 * CLI orchestration only. It delegates local input to the same bounded wire
 * application as the component and delegates server authorization to the
 * configured HTTP endpoint; it does not create a second Review policy.
 */
final class CarReviewCli(
  local: CarReviewSubmissionCliAdapter,
  server: CarReviewCliServerTransport
) {
  def submitLocal(document: String, processRoles: Set[String]): Either[String, CarReviewCliResult] =
    local.submitStdin(document, processRoles).map(_result).fold(
      error => Left(error.toString): Either[String, CarReviewCliResult],
      identity
    )

  def submitServer(document: String): Either[String, CarReviewCliResult] =
    server.submit(document).flatMap(_result)

  private def _result(response: String): Either[String, CarReviewCliResult] =
    for {
      json <- parse(response).left.map(_ => "cbd-review-cli-response-invalid")
      gate <- json.hcursor.get[String]("gateResult").toOption.filter(Set("pass", "fail", "unknown")).toRight("cbd-review-cli-gate-invalid")
      report <- json.hcursor.downField("report").focus.toRight("cbd-review-cli-report-missing")
      run <- report.hcursor.get[String]("reviewId").toOption.filter(_.nonEmpty).toRight("cbd-review-cli-run-id-missing")
    } yield CarReviewCliResult(run, Printer.noSpaces.copy(sortKeys = true).print(json), gate)
}

trait CarReviewCliServerTransport {
  def submit(document: String): Either[String, String]
}

/** Fixed, credential-free JSON gateway. Authentication belongs to the server boundary. */
final class CarReviewCliHttpTransport(endpoint: String, timeoutMillis: Int = 30000) extends CarReviewCliServerTransport {
  def submit(document: String): Either[String, String] =
    for {
      uri <- _uri(endpoint)
      _ <- Either.cond(document.getBytes(StandardCharsets.UTF_8).length <= CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES, (), "cbd-review-cli-request-too-large")
      response <- _post(uri, Printer.noSpaces.print(Json.obj("submissionDocument" -> Json.fromString(document))))
      canonical <- parse(response).toOption.flatMap(_.hcursor.get[String]("canonical_response").toOption).filter(_.nonEmpty).toRight("cbd-review-cli-response-envelope-invalid")
    } yield canonical

  private def _uri(value: String): Either[String, URI] =
    scala.util.Try(new URI(value)).toOption.filter { uri =>
      Set("http", "https").contains(Option(uri.getScheme).getOrElse("").toLowerCase(java.util.Locale.ROOT)) &&
        uri.getHost != null && uri.getRawUserInfo == null && uri.getRawQuery == null && uri.getRawFragment == null
    }.toRight("cbd-review-cli-endpoint-invalid")

  private def _post(uri: URI, body: String): Either[String, String] =
    try {
      val connection = uri.toURL.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("POST")
      connection.setInstanceFollowRedirects(false)
      connection.setConnectTimeout(timeoutMillis)
      connection.setReadTimeout(timeoutMillis)
      connection.setDoOutput(true)
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")
      val output = connection.getOutputStream
      try output.write(body.getBytes(StandardCharsets.UTF_8)) finally output.close()
      val status = connection.getResponseCode
      val contenttype = Option(connection.getContentType).getOrElse("").toLowerCase(java.util.Locale.ROOT)
      val input = if (status >= 200 && status < 300) connection.getInputStream else connection.getErrorStream
      val response = _read(input)
      connection.disconnect()
      Either.cond(status >= 200 && status < 300 && contenttype.startsWith("application/json"), response, "cbd-review-cli-http-response-invalid")
    } catch {
      case NonFatal(_) => Left("cbd-review-cli-http-request-failed")
    }

  private def _read(input: InputStream): String =
    if (input == null) ""
    else {
      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](8192)
      try Iterator.continually(input.read(buffer)).takeWhile(_ >= 0).foreach { size =>
        if (output.size + size > CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES) throw new IllegalArgumentException("cbd-review-cli-response-too-large")
        output.write(buffer, 0, size)
      } finally input.close()
      new String(output.toByteArray, StandardCharsets.UTF_8)
    }
}

/** `sbt runMain org.simplemodeling.textus.cbdsupport.runtime.CarReviewCliMain review submit ... < document.json` */
object CarReviewCliMain {
  def main(args: Array[String]): Unit = {
    val code = _parse(args.toList).flatMap { command =>
      _stdin().flatMap { document =>
        val local = new CarReviewSubmissionCliAdapter(_wire(document))
        val cli = new CarReviewCli(local, command.endpoint.map(new CarReviewCliHttpTransport(_)).getOrElse(CarReviewCliMain._no_server))
        val result = command.endpoint match {
          case Some(_) => cli.submitServer(document)
          case None => cli.submitLocal(document, _roles())
        }
        result.map { value => println(value.render); value.exitCode }
      }
    }.fold(error => { Console.err.println(error); 1 }, identity)
    if (code != 0) sys.exit(code)
  }

  private final case class Command(endpoint: Option[String])
  private val _no_server = new CarReviewCliServerTransport { def submit(document: String) = Left("cbd-review-cli-server-not-configured") }

  private def _parse(args: List[String]): Either[String, Command] = args match {
    case "review" :: "submit" :: Nil => Right(Command(None))
    case "review" :: "submit" :: "--endpoint" :: endpoint :: Nil => Right(Command(Some(endpoint)))
    case _ => Left("usage: review submit [--endpoint <private-cbd-post-url>] < provider-document-submission.json")
  }

  private def _stdin(): Either[String, String] = {
    val bytes = new ByteArrayOutputStream()
    val buffer = new Array[Byte](8192)
    var size = System.in.read(buffer)
    var oversized = false
    while (size >= 0 && !oversized) {
      if (bytes.size + size > CarReviewSubmissionTransportAdapters.MAX_REQUEST_BYTES) oversized = true
      else bytes.write(buffer, 0, size)
      if (!oversized) size = System.in.read(buffer)
    }
    if (oversized) Left("cbd-review-cli-request-too-large")
    else Right(new String(bytes.toByteArray, StandardCharsets.UTF_8))
  }

  private def _roles(): Set[String] =
    sys.env.get("TEXTUS_CBD_REVIEW_PROCESS_ROLES").toVector.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty).toSet

  private def _wire(document: String): CarReviewSubmissionWireApplication = {
    val digest = MessageDigest.getInstance("SHA-256").digest(document.getBytes(StandardCharsets.UTF_8)).map(byte => f"${byte & 0xff}%02x").mkString.take(16)
    new CarReviewSubmissionWireApplication(new CarReviewProviderDocumentSubmissionApplication(
      new CarReviewDevelopmentTemplateProvider(ReviewInstant("1970-01-01T00:00:00Z"), () => ReviewReportId(s"report-cli-$digest"))
    ))
  }
}
