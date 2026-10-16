package org.simplemodeling.textus.cbdsupport.runtime

import java.io.File
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CarReviewFreshProcessSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "A newly bootstrapped CBD process" should {
    Vector("writer-completed", "writer-running").foreach { mode =>
      s"retain persisted state and refuse redispatch after $mode exits (CFR1/CFR2)" in {
        Given("a separate writer JVM with full CBD and native durable Job runtime")
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("target/cbd-fresh-process")), "runtime-").toAbsolutePath
        val writer = _child(mode, root)
        val original = parse(Files.readString(root.resolve("writer.json"))).toOption.get
        original.hcursor.get[Long]("pid").toOption shouldBe Some(writer)

        When("the writer has exited and a different JVM starts a full runtime over the same persistent storage")
        val reader = _child("reader", root)
        val recovered = parse(Files.readString(root.resolve("reader.json"))).toOption.get

        Then("native recovery and production reads preserve exact terminal or unsupported-recovery outcomes")
        reader should not be writer
        reader should not be ProcessHandle.current().pid()
        recovered.hcursor.get[Long]("pid").toOption shouldBe Some(reader)
        recovered.hcursor.get[Int]("executions").toOption shouldBe Some(0)
        if (mode == "writer-completed") {
          recovered.hcursor.get[String]("jobStatus").toOption shouldBe Some("succeeded")
          Vector("reviewId", "jobId", "reportId", "report", "attestation", "bindingCount", "reportCount", "attestationCount").foreach { field =>
            recovered.hcursor.downField(field).focus shouldBe original.hcursor.downField(field).focus
          }
        } else recovered.hcursor.get[String]("startupRefusal").toOption shouldBe Some("durable runtime startup refused")
      }
    }
  }

  private def _child(mode: String, root: Path): Long = {
    val classpath = System.getProperty("java.class.path", "").split(Pattern.quote(File.pathSeparator), -1)
      .map(entry => Path.of(entry).toAbsolutePath.normalize().toString).mkString(File.pathSeparator)
    require(classpath.nonEmpty, "forked Test runtime classpath is required")
    val output = root.resolve(mode + ".stdout")
    val errors = root.resolve(mode + ".stderr")
    val command = Vector(Path.of(System.getProperty("java.home"), "bin", "java").toString,
      "-cp", classpath, "org.simplemodeling.textus.cbdsupport.runtime.CarReviewFreshProcessProbe", mode, root.toString)
    val builder = new ProcessBuilder(command*)
    builder.environment().clear()
    builder.redirectOutput(output.toFile)
    builder.redirectError(errors.toFile)
    val child = builder.start()
    try {
      val terminated = child.waitFor(45, TimeUnit.SECONDS)
      withClue(s"$mode: ${Files.readString(errors)} ${Files.readString(output)}") {
        terminated shouldBe true
        child.exitValue() shouldBe 0
      }
      child.pid()
    } finally {
      if (child.isAlive) {
        child.destroy()
        if (!child.waitFor(2, TimeUnit.SECONDS)) {
          child.destroyForcibly()
          require(child.waitFor(5, TimeUnit.SECONDS), "owned probe JVM could not terminate")
        }
      }
    }
  }
}
