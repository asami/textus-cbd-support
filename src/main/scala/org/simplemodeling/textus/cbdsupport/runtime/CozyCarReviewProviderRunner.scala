package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * The CBD-side, transport-neutral Cozy command seam. Cozy owns the command's
 * input/output implementation; CBD owns admission, target authority, and
 * evidence-bundle admission. No Cozy implementation class is linked here.
 */
final case class CozyCarReviewProviderTransportResult(
  bundle: String,
  completedAtMillis: Long
)

trait CozyCarReviewProviderTransport {
  def provider: ReviewProviderIdentity
  def execute(providerRequest: String, timeoutMillis: Long): Either[String, CozyCarReviewProviderTransportResult]
  def cancel(): Unit
}

final case class CozyCarReviewProviderTarget(
  target: ReviewTarget,
  projectRoot: Path
)

final case class CozyCarReviewProviderCommand(
  command: Vector[String],
  target: CozyCarReviewProviderTarget,
  providerVersion: ReviewVersion,
  outputRoot: Path,
  maxRequestBytes: Int,
  maxResponseBytes: Int
)

/**
 * Fixed local-command transport for Cozy's public `review car-evidence`
 * surface. The child receives only a request on stdin, exact command-template
 * arguments, an explicit project root, and an empty environment. Its response
 * is held under a CBD-owned output root rather than entering logs or CallTree.
 */
final class CozyCarReviewProviderProcessTransport(
  command: CozyCarReviewProviderCommand,
  clock: () => Long
) extends CozyCarReviewProviderTransport {
  private var _active: Option[Process] = None

  def provider: ReviewProviderIdentity =
    ReviewProviderIdentity(ReviewProviderId("cozy"), command.providerVersion)

  def execute(providerRequest: String, timeoutMillis: Long): Either[String, CozyCarReviewProviderTransportResult] = synchronized {
    if (command.command.isEmpty || command.maxRequestBytes <= 0 || command.maxResponseBytes <= 0 || timeoutMillis <= 0)
      Left("provider-command-policy-invalid")
    else if (providerRequest.getBytes(StandardCharsets.UTF_8).length > command.maxRequestBytes)
      Left("provider-request-byte-limit")
    else {
      val outputroot = command.outputRoot.toAbsolutePath.normalize()
      try {
        Files.createDirectories(outputroot)
        val output = Files.createTempFile(outputroot, "cozy-car-review-", ".json")
        try {
          val processbuilder = new ProcessBuilder((command.command ++ Vector(
            "review",
            "car-evidence",
            "--project-root",
            command.target.projectRoot.toAbsolutePath.normalize().toString,
            "--provider-version",
            command.providerVersion.value,
            "--request-stdin"
          )).asJava)
          processbuilder.directory(outputroot.toFile)
          processbuilder.redirectErrorStream(true)
          processbuilder.redirectOutput(output.toFile)
          processbuilder.environment().clear()
          val process = processbuilder.start()
          _active = Some(process)
          try {
            val stdin = process.getOutputStream
            try stdin.write(providerRequest.getBytes(StandardCharsets.UTF_8))
            finally stdin.close()
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
              process.destroyForcibly()
              Left("provider-timeout")
            } else if (process.exitValue() != 0)
              Left("provider-command-failed")
            else if (Files.size(output) > command.maxResponseBytes)
              Left("provider-response-byte-limit")
            else
              Right(CozyCarReviewProviderTransportResult(Files.readString(output, StandardCharsets.UTF_8), clock()))
          } finally {
            _active = None
          }
        } finally {
          Files.deleteIfExists(output)
        }
      } catch {
        case NonFatal(_) => Left("provider-transport-failed")
      }
    }
  }

  def cancel(): Unit = synchronized {
    _active.foreach(_.destroyForcibly())
    _active = None
  }
}

/**
 * Binds an explicitly admitted local CAR root to the Cozy provider identity.
 * Target mismatch is refused before any Cozy command can access the root.
 */
final class CozyCarReviewProviderRunner(
  admittedTarget: CozyCarReviewProviderTarget,
  transport: CozyCarReviewProviderTransport
) extends CarReviewProviderRunner {
  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    if (request.provider.id != ReviewProviderId("cozy"))
      ProviderBundleRunnerResult.Failed("provider-identity-mismatch", "Configured runner is restricted to the Cozy provider.", request.startedAtMillis)
    else if (request.provider != transport.provider)
      ProviderBundleRunnerResult.Failed("provider-identity-mismatch", "Configured Cozy transport does not match the registered provider identity.", request.startedAtMillis)
    else if (request.target != admittedTarget.target)
      ProviderBundleRunnerResult.Failed("provider-target-not-admitted", "Configured Cozy target does not match the admitted Review target.", request.startedAtMillis)
    else CarReviewProviderBundleAdmission.timeoutMillis(request.providerRequest) match {
      case Left(code) => ProviderBundleRunnerResult.Failed(code, "Provider request is not admissible for Cozy execution.", request.startedAtMillis)
      case Right(timeout) =>
        transport.execute(request.providerRequest, timeout) match {
          case Right(value) => ProviderBundleRunnerResult.Completed(value.bundle, value.completedAtMillis)
          case Left(code) => ProviderBundleRunnerResult.Failed(code, "Cozy provider transport did not return an evidence bundle.", request.startedAtMillis)
        }
    }

  def cancel(request: ProviderBundleExecutionRequest): Unit = {
    val _ = request
    transport.cancel()
  }
}
