package org.simplemodeling.textus.cbdsupport.runtime

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path}
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*
import io.circe.Json
import io.circe.parser.parse

/**
 * Test-private local Git transfer and bounded owned JVM/process lifecycle.
 * Helpers execute only inside the parent's selected forked SBT specification.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  2, 2026
 */
private[runtime] object InternalModelDurableHandoffSupport {
  final case class ChildEvidence(report: Json, pid: Long, exitcode: Int, terminated: Boolean,
    stderr: String, cwd: Path, home: Path, temporary: Path, environment: Map[String, String])
  final case class Transfer(work: Path, producer: Path, consumer: Path, input: Path,
    commit: String, consumercommit: String, paths: Vector[String], consumerpaths: Vector[String],
    alternatesabsent: Boolean, initial: ChildEvidence)
  private final case class ProcessEvidence(pid: Long, exitcode: Int, terminated: Boolean, stdout: String, stderr: String)

  def withTransfer(sourceRoot: Path, request: InternalModelContinuationRequest)(body: Transfer => Unit): Unit = {
    val base = Files.createDirectories(Path.of("target/internal-model-durable-handoff/work").toAbsolutePath.normalize())
    require(!Files.isSymbolicLink(base), "durable work base must not be a symbolic link")
    val work = Files.createTempDirectory(base, "run-")
    try {
      val producer = work.resolve("producer")
      val consumer = work.resolve("consumer")
      val input = work.resolve("independent-input.json")
      writeOwned(work, input, InternalModelDurableHandoffInput.encode(request))
      val paths = InternalModelDurableHandoffFixture.copyPackage(sourceRoot, producer, work)
      val initial = runProbe(work, producer, input)
      require(initial.terminated && initial.pid != ProcessHandle.current().pid(), "producer must reach terminal distinct JVM evidence")
      _git(work, producer, Vector("init", "--quiet"))
      _git(work, producer, Vector("add", "--") ++ paths)
      _git(work, producer, Vector("commit", "--quiet", "-m", "private durable package proof"))
      val commit = _git(work, producer, Vector("rev-parse", "HEAD")).trim
      require(commit.nonEmpty, "Git did not return its native commit reference")
      val tracked = _tracked(work, producer)
      require(tracked == paths, "producer tracked paths differ from admitted present inventory")
      _git(work, work, Vector("clone", "--quiet", "--no-local", "--no-hardlinks", "--no-checkout", "--", producer.toString, consumer.toString))
      _git(work, consumer, Vector("checkout", "--quiet", "--detach", commit))
      val consumercommit = _git(work, consumer, Vector("rev-parse", "HEAD")).trim
      val consumerpaths = _tracked(work, consumer)
      val alternatesabsent = !Files.exists(consumer.resolve(".git/objects/info/alternates"), LinkOption.NOFOLLOW_LINKS)
      require(consumercommit == commit && consumerpaths == paths && alternatesabsent, "independent checkout does not retain exact native revision and paths")
      removeOwned(work, producer)
      require(!Files.exists(producer, LinkOption.NOFOLLOW_LINKS), "producer must be absent before consumer reevaluation")
      body(Transfer(work, producer, consumer, input, commit, consumercommit, paths, consumerpaths, alternatesabsent, initial))
    } finally {
      val children = Files.list(work)
      try children.iterator.asScala.toVector.foreach(removeOwned(work, _))
      finally children.close()
    }
  }

  def runProbe(workRoot: Path, projectRoot: Path, inputPath: Path, arguments: Option[Vector[String]] = None): ChildEvidence = {
    requireOwned(workRoot, projectRoot)
    requireOwned(workRoot, inputPath)
    require(!inputPath.startsWith(projectRoot), "independent input cannot be a consuming package artifact")
    val childroot = Files.createTempDirectory(workRoot, "jvm-")
    val cwd = Files.createDirectory(childroot.resolve("cwd"))
    val home = Files.createDirectory(childroot.resolve("home"))
    val temporary = Files.createDirectory(childroot.resolve("tmp"))
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath.normalize()
    require(Files.isExecutable(java), "java.home does not supply an executable JVM")
    val entries = System.getProperty("java.class.path", "").split(Pattern.quote(File.pathSeparator), -1).toVector
    require(entries.nonEmpty && entries.forall(_.nonEmpty), "forked Test runtime classpath is required")
    val paths = entries.map(Path.of(_))
    require(paths.forall(path => path.isAbsolute && Files.exists(path)), "forked Test classpath must have available absolute entries")
    val command = Vector(java.toString, s"-Duser.home=$home", s"-Djava.io.tmpdir=$temporary", "-cp", paths.mkString(File.pathSeparator),
      "org.simplemodeling.textus.cbdsupport.runtime.InternalModelDurableHandoffProbe") ++ arguments.getOrElse(Vector(projectRoot.toString, inputPath.toString))
    val evidence = _process(workRoot, childroot, cwd, command, Map.empty)
    val report = parse(evidence.stdout).fold(error => throw new IllegalStateException(s"child report is invalid: ${error.getMessage}; ${evidence.stderr}"), identity)
    ChildEvidence(report, evidence.pid, evidence.exitcode, evidence.terminated, evidence.stderr, cwd, home, temporary, Map.empty)
  }

  def writeOwned(workRoot: Path, path: Path, bytes: Vector[Byte]): Unit = {
    requireOwned(workRoot, path)
    Files.createDirectories(path.getParent)
    Files.write(path, bytes.toArray)
  }

  /** Captured private root and no-follow ancestry bound every mutation and removal. */
  def requireOwned(workRoot: Path, path: Path): Unit = {
    val root = workRoot.toAbsolutePath.normalize()
    val selected = path.toAbsolutePath.normalize()
    require(selected.startsWith(root) && selected != root, "target must be a strict descendant of the captured private work root")
    var current = selected
    while (current != root) {
      require(!Files.isSymbolicLink(current), "owned target ancestry must not follow symbolic links")
      current = current.getParent
    }
    require(!Files.isSymbolicLink(root), "captured work root must not be a symbolic link")
  }

  def removeOwned(workRoot: Path, path: Path): Unit = {
    requireOwned(workRoot, path)
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      val stream = Files.walk(path)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach { item =>
        requireOwned(workRoot, item)
        Files.delete(item)
      } finally stream.close()
    }
  }

  private def _tracked(work: Path, root: Path): Vector[String] =
    _git(work, root, Vector("ls-files", "-z")).split("\u0000", -1).toVector.filter(_.nonEmpty).sorted

  private def _git(work: Path, cwd: Path, arguments: Vector[String]): String = {
    val git = Path.of("/usr/bin/git")
    require(Files.isExecutable(git), "required absolute Git executable is unavailable")
    val operation = Files.createTempDirectory(work, "git-")
    val home = Files.createDirectory(operation.resolve("home"))
    val temporary = Files.createDirectory(operation.resolve("tmp"))
    val hooks = Files.createDirectory(operation.resolve("empty-hooks"))
    val environment = Map("HOME" -> home.toString, "TMPDIR" -> temporary.toString, "PATH" -> "/usr/bin:/bin",
      "GIT_CONFIG_NOSYSTEM" -> "1", "GIT_CONFIG_GLOBAL" -> "/dev/null", "GIT_TERMINAL_PROMPT" -> "0")
    val command = Vector(git.toString, "-c", s"core.hooksPath=$hooks", "-c", "user.name=Durable Test",
      "-c", "user.email=durable-test@example.invalid", "-c", "commit.gpgSign=false", "-c", "tag.gpgSign=false") ++ arguments
    val evidence = _process(work, operation, cwd, command, environment)
    require(evidence.exitcode == 0, s"private Git operation failed: ${arguments.head}; ${evidence.stderr}")
    evidence.stdout
  }

  private def _process(work: Path, operation: Path, cwd: Path, command: Vector[String], environment: Map[String, String]): ProcessEvidence = {
    requireOwned(work, operation)
    require(cwd == work || cwd.startsWith(work), "process cwd must remain private")
    val output = operation.resolve("stdout")
    val errors = operation.resolve("stderr")
    requireOwned(work, output)
    requireOwned(work, errors)
    val builder = new ProcessBuilder(command*)
    builder.environment().clear()
    environment.foreach { case (key, value) => builder.environment().put(key, value) }
    builder.directory(cwd.toFile).redirectOutput(output.toFile).redirectError(errors.toFile)
    val child = builder.start()
    try {
      require(child.waitFor(30, TimeUnit.SECONDS), s"owned process exceeded 30 seconds: ${command.head}")
      ProcessEvidence(child.pid(), child.exitValue(), !child.isAlive, Files.readString(output, StandardCharsets.UTF_8), Files.readString(errors, StandardCharsets.UTF_8))
    } finally {
      if (child.isAlive) {
        child.destroy()
        if (!child.waitFor(2, TimeUnit.SECONDS)) {
          child.destroyForcibly()
          require(child.waitFor(5, TimeUnit.SECONDS), "owned process could not be terminated")
        }
      }
    }
  }
}
