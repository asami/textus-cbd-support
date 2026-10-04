package org.simplemodeling.textus.cbdsupport.runtime

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*
import io.circe.Json
import io.circe.jawn.JawnParser

/**
 * Test-only independent caller transport and owned, sequential JVM lifecycles.
 *
 * @since   Oct.  4, 2026
 * @version Oct.  4, 2026
 */
private[runtime] object InternalModelEndToEndSupport {
  import InternalModelCandidateHumanApprovalValidatorSpec.recordReference
  private val _core = InternalModelContinuationFixture
  private val _parser = JawnParser(allowDuplicateKeys = false)
  final case class CallerInput(original: InternalModelContinuationRequest, current: InternalModelContinuationRequest,
    live: Map[InternalModelArtifactReference, InternalModelPackageFreshnessInput],
    authority: Option[InternalModelCmlMutationAuthority], application: InternalModelRecordReference,
    postvalidation: InternalModelRecordReference, label: String)
  final case class Child(report: Json, pid: Long, exitcode: Int, terminated: Boolean, cwd: Path, home: Path,
    temporary: Path, environment: Map[String, String], stdout: String, stderr: String)
  final case class Produced(work: Path, root: Path, input: Path, producer: Child)

  def produce(label: String = "end-to-end 日本語", reverseInputs: Boolean = false, carrierRevision: Long = 43L,
    cmlMode: String = "success"): Produced = {
    val base = Files.createDirectories(Path.of("target/internal-model-end-to-end/work").toAbsolutePath.normalize)
    val work = Files.createTempDirectory(base, "run-")
    val producer = runChild(work, Vector("producer", work.toString, label, reverseInputs.toString, carrierRevision.toString, cmlMode))
    require(producer.exitcode == 0 && producer.terminated, "producer must exit successfully before any consumer starts: " + producer.stderr)
    Produced(work, Path.of(producer.report.hcursor.get[String]("projectRoot").toOption.get),
      Path.of(producer.report.hcursor.get[String]("inputPath").toOption.get), producer)
  }

  def consume(produced: Produced, mode: String = "synthetic-success"): Child = {
    require(produced.producer.terminated, "consumer requires terminal producer evidence")
    runChild(produced.work, Vector("consumer", produced.work.toString, produced.root.toString, produced.input.toString, mode))
  }

  /** Only a test-owned JVM is started; environment has no inherited provider or DB state. */
  def runChild(workRoot: Path, arguments: Vector[String]): Child = {
    val operation = Files.createTempDirectory(workRoot, "jvm-")
    val cwd = Files.createDirectory(operation.resolve("cwd"))
    val home = Files.createDirectory(operation.resolve("home"))
    val temporary = Files.createDirectory(operation.resolve("tmp"))
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath.normalize
    val classpath = System.getProperty("java.class.path", "").split(Pattern.quote(File.pathSeparator), -1).toVector
    require(classpath.nonEmpty && classpath.forall(item => item.nonEmpty && Path.of(item).isAbsolute && Files.exists(Path.of(item))),
      "forked Test must supply available absolute runtime classpath entries")
    val command = Vector(java.toString, s"-Duser.home=$home", s"-Djava.io.tmpdir=$temporary", "-cp", classpath.mkString(File.pathSeparator),
      "org.simplemodeling.textus.cbdsupport.runtime.InternalModelEndToEndProbe") ++ arguments
    val environment = Map("HOME" -> home.toString, "TMPDIR" -> temporary.toString)
    val output = operation.resolve("stdout.json")
    val errors = operation.resolve("stderr.txt")
    val builder = new ProcessBuilder(command*)
    builder.environment().clear()
    environment.foreach { case (name, value) => builder.environment().put(name, value) }
    val process = builder.directory(cwd.toFile).redirectOutput(output.toFile).redirectError(errors.toFile).start()
    try {
      require(process.waitFor(60L, TimeUnit.SECONDS), "owned synthetic JVM exceeded the explicit one-minute ceiling")
      val stdout = Files.readString(output, StandardCharsets.UTF_8)
      val stderr = Files.readString(errors, StandardCharsets.UTF_8)
      val report = _parser.parse(stdout).fold(error => throw new IllegalStateException(error.getMessage + "; " + stderr), identity)
      Child(report, process.pid(), process.exitValue(), !process.isAlive, cwd, home, temporary, environment, stdout, stderr)
    } finally {
      if (process.isAlive) {
        process.destroy()
        if (!process.waitFor(2L, TimeUnit.SECONDS)) {
          process.destroyForcibly()
          require(process.waitFor(5L, TimeUnit.SECONDS), "owned child must terminate")
        }
      }
    }
  }

  def writeInput(path: Path, current: InternalModelContinuationRequest,
    liveSources: Map[InternalModelArtifactReference, InternalModelPackageFreshnessInput],
    authority: InternalModelCmlMutationAuthority, label: String): Unit = {
    val request = _core.json(InternalModelDurableHandoffInput.encode(current))
    val live = liveSources.toVector.sortBy(_._1.artifactId.value).map { case (reference, input) =>
      val (kind, source, bytes, relative) = input match {
        case InternalModelPackageFreshnessInput.CmlObserved(owner, id, revision, relativepath) =>
          ("cml", InternalModelSemanticSource(owner, id, None, revision), Vector.empty[Byte], Some(relativepath))
        case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(owner, id, revision, raw, relativepath)) =>
          ("source", InternalModelSemanticSource(owner, id, None, revision), raw, relativepath)
        case _ => throw new IllegalArgumentException("fixture must explicitly declare observed owners")
      }
      Json.obj("reference" -> _core.referenceJson(reference), "kind" -> Json.fromString(kind), "source" -> _source_json(source),
        "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(bytes.toArray)),
        "projectRelativePath" -> relative.map(Json.fromString).getOrElse(Json.Null))
    }
    writeJson(path, Json.obj("schema" -> Json.fromString("textus.internal-model-e2e-caller.v1"),
      "original" -> request, "current" -> request, "liveSources" -> Json.fromValues(live), "authority" -> _authority_json(authority),
      "applicationReference" -> recordJson(recordReference("e2e-application", 701L)),
      "postValidationReference" -> recordJson(recordReference("e2e-post-validation", 709L)), "label" -> Json.fromString(label)))
  }

  /** No cached gate/plan is transported; all permission is reevaluated by the existing owners. */
  def readInput(path: Path): CallerInput = {
    val value = readJson(path)
    require(value.asObject.get.keys.toSet == Set("schema", "original", "current", "liveSources", "authority",
      "applicationReference", "postValidationReference", "label"), "test caller envelope must be closed")
    require(value.hcursor.get[String]("schema").toOption.contains("textus.internal-model-e2e-caller.v1"), "test caller schema must match")
    val original = InternalModelDurableHandoffInput.decode(_core.canonical(value.hcursor.get[Json]("original").toOption.get)).toOption.get
    val current = InternalModelDurableHandoffInput.decode(_core.canonical(value.hcursor.get[Json]("current").toOption.get)).toOption.get
    val entries = value.hcursor.get[Vector[Json]]("liveSources").toOption.get.map { item =>
      val reference = InternalModelTypedControlCodec.decodeArtifactReference(_core.canonical(item.hcursor.get[Json]("reference").toOption.get)).toOption.get
      val source = _source(item.hcursor.get[Json]("source").toOption.get)
      val relative = item.hcursor.get[Option[String]]("projectRelativePath").toOption.get
      val input: InternalModelPackageFreshnessInput = item.hcursor.get[String]("kind").toOption.get match {
        case "cml" => InternalModelPackageFreshnessInput.CmlObserved(source.authority, source.identity, source.revision, relative.get)
        case "source" => InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(
          source.authority, source.identity, source.revision,
          Base64.getDecoder.decode(item.hcursor.get[String]("rawBytesBase64").toOption.get).toVector, relative))
        case _ => throw new IllegalArgumentException("unsupported declared source input")
      }
      reference -> input
    }
    require(entries.map(_._1).distinct.size == entries.size, "caller source references must be unique")
    val authorityvalue = value.hcursor.get[Json]("authority").toOption.get
    val authority = if (authorityvalue.isNull) None else {
      val selectedpackage = InternalModelTypedControlCodec.decodePackageReference(_core.canonical(authorityvalue.hcursor.get[Json]("packageReference").toOption.get)).toOption.get
      val scopevalue = authorityvalue.hcursor.get[Json]("scope").toOption.get
      val scope = InternalModelSemanticScope(_text(scopevalue, "componentIdentity"), _text(scopevalue, "projectionContextIdentity"),
        _text(scopevalue, "selectedUseCaseElementIdentity"))
      val targets = authorityvalue.hcursor.get[Vector[Json]]("targets").toOption.get.map(item =>
        InternalModelCmlMutationTarget(_text(item, "targetId"), _text(item, "projectRelativePath"),
          _text(item, "sourceAuthority"), _text(item, "sourceIdentity"), _text(item, "nextSourceRevision")))
      Some(InternalModelCmlMutationAuthority(_record(authorityvalue.hcursor.get[Json]("requestReference").toOption.get),
        _text(authorityvalue, "projectRoot"), selectedpackage, scope, _source(authorityvalue.hcursor.get[Json]("provenance").toOption.get), targets))
    }
    CallerInput(original, current, entries.toMap, authority, _record(value.hcursor.get[Json]("applicationReference").toOption.get),
      _record(value.hcursor.get[Json]("postValidationReference").toOption.get), _text(value, "label"))
  }

  def readJson(path: Path): Json = _parser.parse(Files.readString(path, StandardCharsets.UTF_8)).toOption.get
  def writeJson(path: Path, value: Json): Unit = {
    Files.write(path, _core.canonical(value).toArray, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    ()
  }
  def replaceCaller(produced: Produced, change: Json => Json): Unit = {
    Files.write(produced.input, _core.canonical(change(readJson(produced.input))).toArray, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    ()
  }
  def recordJson(reference: InternalModelRecordReference): Json = _core.json(InternalModelTypedControlCodec.encodeRecordReference(reference))

  /** Complete ordinary result transport for direct semantic expectations, never control identity. */
  def evidence(value: Any): Json = value match {
    case null => throw new IllegalArgumentException("null is not admitted result evidence")
    case text: String => Json.obj("type" -> Json.fromString("String"), "value" -> Json.fromString(text))
    case flag: Boolean => Json.fromBoolean(flag)
    case number: Byte => Json.fromInt(number.toInt)
    case number: Int => Json.fromInt(number)
    case number: Long => Json.fromLong(number)
    case None => Json.obj("type" -> Json.fromString("None"))
    case Some(item) => Json.obj("type" -> Json.fromString("Some"), "value" -> evidence(item))
    case items: Vector[?] => Json.fromValues(items.map(evidence))
    case items: Map[?, ?] => Json.fromValues(items.toVector.map { case (key, item) => Json.arr(evidence(key), evidence(item)) })
    case product: Product => Json.obj("type" -> Json.fromString(product.getClass.getName),
      "fields" -> Json.fromFields(product.productElementNames.zip(product.productIterator).map { case (name, item) => name -> evidence(item) }.toVector))
    case other => throw new IllegalArgumentException("unsupported evidence: " + other.getClass.getName)
  }

  private def _authority_json(value: InternalModelCmlMutationAuthority): Json = Json.obj(
    "requestReference" -> recordJson(value.requestreference), "projectRoot" -> Json.fromString(value.projectroot),
    "packageReference" -> _core.json(InternalModelTypedControlCodec.encodePackageReference(value.packagereference)),
    "scope" -> Json.obj("componentIdentity" -> Json.fromString(value.scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(value.scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(value.scope.selectedUseCaseElementIdentity)),
    "provenance" -> _source_json(value.provenance), "targets" -> Json.fromValues(value.targets.map(target => Json.obj(
      "targetId" -> Json.fromString(target.targetid), "projectRelativePath" -> Json.fromString(target.projectrelativepath),
      "sourceAuthority" -> Json.fromString(target.sourceauthority), "sourceIdentity" -> Json.fromString(target.sourceidentity),
      "nextSourceRevision" -> Json.fromString(target.nextsourcerevision)))))
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority),
    "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null),
    "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null))
  private def _source(value: Json): InternalModelSemanticSource = InternalModelSemanticSource(_text(value, "authority"),
    _text(value, "identity"), value.hcursor.get[Option[String]]("locator").toOption.get, value.hcursor.get[Option[String]]("revision").toOption.get)
  private def _record(value: Json): InternalModelRecordReference = InternalModelTypedControlCodec.decodeRecordReference(_core.canonical(value)).toOption.get
  private def _text(value: Json, key: String): String = value.hcursor.get[String](key).toOption.get
}
