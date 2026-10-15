package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.jar.{JarEntry, JarOutputStream}
import scala.jdk.CollectionConverters.*
import io.circe.Json
import org.goldenport.cncf.component.{ComponentId, ComponentFactory}
import org.goldenport.cncf.component.identity.ComponentReleaseCoordinate
import org.goldenport.cncf.component.capability.*
import ComponentCapabilityAdmissionScope.*
import ComponentCapabilityReference.*
import ComponentCapabilityProjection.*
import ComponentCapabilityMetadataProvider.*
import ComponentCapabilityPackageSource.*
import CapabilityIngestionModel.*
import CapabilityProducerNativeFixtures.CountingProvider
import org.simplemodeling.textus.cbdsupport.generated.capabilityproducer.*

/*
 * Actual generated product locations, source-free intake and normal CAR inputs.
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[runtime] object CapabilityProducerIntegrationFixtures {
  final case class Products(productionRoot: Path, inputRoot: Path, classDirectory: Path,
    cncfJar: Path, cozyVersion: String, cncfVersion: String, runtimeClasspath: String,
    primaryFirst: Path, secondaryFirst: Path, primarySecond: Path, secondarySecond: Path, primaryContracts: Path) {
    def output(family: String, suffix: String): Path = (family, suffix) match {
      case ("primary", "First") => primaryFirst
      case ("secondary", "First") => secondaryFirst
      case ("primary", "Second") => primarySecond
      case ("secondary", "Second") => secondarySecond
      case ("primary", "Contracts") => primaryContracts
      case _ => throw new IllegalArgumentException("An explicit primary/secondary First/Second product is required.")
    }
  }

  def products(): Products = {
    val properties = new Properties
    val stream = Option(getClass.getResourceAsStream("/capability-producer-integration.properties"))
      .getOrElse(throw new IllegalArgumentException("Explicit Capability producer Test products are required."))
    try properties.load(stream) finally stream.close()
    require(properties.getProperty("schema") == "cbd.capability-producer-fixture.v1", "Unsupported Test product locator schema")
    def _text_(key: String): String = Option(properties.getProperty(key))
      .getOrElse(throw new IllegalArgumentException("Missing Test product location/version: " + key))
    def _path_(key: String): Path = Paths.get(_text_(key))
    Products(_path_("productionRoot"), _path_("inputRoot"), _path_("classDirectory"), _path_("cncfJar"),
      _text_("cozyVersion"), _text_("cncfVersion"), _text_("runtimeClasspath"),
      _path_("primaryFirst"), _path_("secondaryFirst"), _path_("primarySecond"), _path_("secondarySecond"), _path_("primaryContracts"))
  }

  def accessor(id: String, suffix: String): Projection = {
    val json = (id, suffix) match {
      case ("Store", "First") => StoreFirstMetadata.projectionJson
      case ("Archive", "First") => ArchiveFirstMetadata.projectionJson
      case ("Remote", "First") => RemoteFirstMetadata.projectionJson
      case ("Store", "Second") => StoreSecondMetadata.projectionJson
      case ("Archive", "Second") => ArchiveSecondMetadata.projectionJson
      case ("Remote", "Second") => RemoteSecondMetadata.projectionJson
      case ("Store", "Contracts") => StoreContractsMetadata.projectionJson
      case ("Archive", "Contracts") => ArchiveContractsMetadata.projectionJson
      case _ => throw new IllegalArgumentException("An explicit generated accessor is required.")
    }
    right(ComponentCapabilityProjectionCodec.decode(json))
  }

  def header(id: String, suffix: String): Source = (id, suffix) match {
    case ("Store" | "Archive" | "UsageOnly", "First") => Source("Model:能力/A", "model/capability-model.cml", "header:primary-r42", None)
    case ("Store" | "Archive" | "UsageOnly", "Second") => Source("Model:能力/A", "model/capability-model.cml", "header:primary-r44", None)
    case ("Store" | "Archive" | "UsageOnly", "Contracts") => Source("Model:能力/A", "model/capability-model.cml", "header:primary-r46", None)
    case ("Remote", "First") => Source("Other:能力/B", "model/capability-model-secondary.cml", "header:secondary-r43", Some(2))
    case ("Remote", "Second") => Source("Other:能力/B", "model/capability-model-secondary.cml", "header:secondary-r45", Some(2))
    case _ => throw new IllegalArgumentException("An explicit independently selected header is required.")
  }
  def coordinate(id: String): ComponentCoordinate =
    ComponentCoordinate(ComponentId("org.simplemodeling.textus." + id), "1.2.3-SNAPSHOT")
  def packageCoordinate(id: String): PackageCoordinate = PackageCoordinate("pkg:" + id + "|日本語", "package:r9")
  def provider(id: String): CountingProvider = new CountingProvider(id match {
    case "Store" => CapabilityProducerNativeFixtures.storeInput
    case "Archive" | "Remote" => CapabilityProducerNativeFixtures.emptyInput
    case _ => throw new IllegalArgumentException("An explicit fixture Component is required.")
  })
  val useCase: UnavailableUpstreamProjection = UnavailableUpstreamProjection(ProjectionFamily.UseCase,
    "not-yet-admitted", "Positive forward UseCase demand path remains unexercised.")
  final class Intake(val root: Path, val products: Products) {
    def bundle(id: String, suffix: String): Path = root.resolve(suffix + "/" + id)
    def usageProjection(id: String, suffix: String): ComponentUsageProjection = {
      val input = PackageInput.Selected(CapabilityProducerNativeFixtures.scope.scopeId, Location.Directory(bundle(id, suffix)), coordinate(id), packageCoordinate(id))
      val acquired = right(ComponentFactory.acquireUsageCapabilities(input)).getOrElse(
        throw new IllegalArgumentException("Expected actual normally produced Usage metadata."))
      right(CapabilityComponentUsageProjectionCodec.decode(acquired.content))
    }
    def selected(id: String, suffix: String, nativeProvider: Option[ComponentCapabilityMetadataProvider],
      location: Location): PackageSelection = {
      val resources = if (id == "UsageOnly") Vector.empty else Vector(ResourceExpectation("meta/component-capability/" + id + ".json",
        Producer("cozy", products.cozyVersion), 1, "cncf.component-capability.v1", header(id, suffix)))
      val usage = if (id == "Archive" || suffix == "Contracts") None else Some(ComponentUsageExpectation(
        Producer("cozy", products.cozyVersion), 1, "cozy.component-usage-capability.v1", Vector(header(id, suffix))))
      PackageSelection(PackageExpectation(PackageInput.Selected(CapabilityProducerNativeFixtures.scope.scopeId, location, coordinate(id), packageCoordinate(id)),
        resources, usage), nativeProvider)
    }
    def directory(id: String, suffix: String, nativeProvider: CountingProvider): PackageSelection =
      selected(id, suffix, Some(nativeProvider), Location.Directory(bundle(id, suffix)))
    def selection(packages: Vector[PackageSelection]): Selection =
      Selection(CapabilityProducerNativeFixtures.scope, packages, useCase)
    def directories(ids: Vector[String], suffix: String): Selection = selection(ids.map { id =>
      if (id == "UsageOnly") selected(id, suffix, None, Location.Directory(bundle(id, suffix)))
      else directory(id, suffix, provider(id))
    })
    def cmlInputs: Vector[Path] = {
      val stream = Files.walk(root)
      try stream.iterator().asScala.filter(path => Files.isRegularFile(path) && path.toString.endsWith(".cml")).toVector
      finally stream.close()
    }

    def packageCars(): Vector[Path] = {
      val mainjar = _carrier_jar(products.classDirectory, root.resolve("artifacts/capability-carriers.jar"))
      val logs = products.productionRoot.resolve("package-logs")
      Files.createDirectories(logs)
      Vector("Store", "Remote").map { id =>
        val family = if (id == "Store") "primary" else "secondary"
        val project = root.resolve("package/" + id)
        _copy_tree(products.output(family, "First"), project)
        val source = header(id, "First").path
        val original = project.resolve(source)
        Files.createDirectories(original.getParent)
        Files.copy(products.inputRoot.resolve(source), original, StandardCopyOption.REPLACE_EXISTING)
        _write(project.resolve("project.yaml"), s"""project:
          |  namespace: org.simplemodeling.textus
          |  id: $id
          |  name: capability-product
          |  kind: car
          |  component:
          |    version: 1.2.3-SNAPSHOT
          |build:
          |  cozyVersion: ${products.cozyVersion}
          |  dependencies:
          |    compile:
          |      - org.goldenport::goldenport-cncf:${products.cncfVersion}
          |packaging:
          |  kind: car
          |  car:
          |    include_dependencies: true
          |    runtime:
          |      cncf:
          |        minimum: ${products.cncfVersion}
          |        tested:
          |          - ${products.cncfVersion}
          |""".stripMargin)
        val component = Json.obj("namespace" -> Json.fromString("org.simplemodeling.textus"), "id" -> Json.fromString(id),
          "version" -> Json.fromString("1.2.3-SNAPSHOT"))
        val abi = Json.obj("format" -> Json.fromString("cozy.car.abi-manifest.v2"), "component" -> component,
          "abi" -> Json.obj("version" -> Json.fromInt(1), "exports" -> Json.obj(
            "components" -> Json.arr(Json.obj("namespace" -> Json.fromString("org.simplemodeling.textus"), "id" -> Json.fromString(id))),
            "operations" -> Json.arr(), "entities" -> Json.arr()), "dependencies" -> Json.arr()))
        _write(project.resolve("src/main/car/abi-manifest.json"), abi.noSpaces)
        val release = ComponentReleaseCoordinate.create(coordinate(id).componentId.sharedIdentity, "1.2.3-SNAPSHOT").value().get()
        val archive = root.resolve(id + ".car")
        val argv = Vector("package-car", "--save", archive.toString, "--project-dir", project.toString,
          "--main-jar", mainjar.toString, "--lib-jars", products.cncfJar.toString,
          "--name", release.mavenArtifactId(), "--version", "1.2.3-SNAPSHOT", "--component", "org.simplemodeling.textus." + id,
          "--capability-dir", project.resolve("target/bundles/" + id).toString,
          "--capability-package-coordinate", "pkg:" + id + "|日本語", "--capability-package-version", "package:r9")
        _run_command(argv, project, products.runtimeClasspath, logs.resolve(id + "-package.log"))
        Files.delete(original)
        archive
      }
    }
  }

  def withIntake[A](body: Intake => A): A = {
    val selected = products()
    val parent = selected.productionRoot.resolve("intake")
    Files.createDirectories(parent)
    val root = Files.createTempDirectory(parent, "selected-")
    try {
      Vector("First", "Second", "Contracts").foreach { suffix =>
        val ids = if (suffix == "Contracts") Vector("Store", "Archive", "UsageOnly") else Vector("Store", "Archive", "UsageOnly", "Remote")
        ids.foreach { id =>
          val family = if (id == "Remote") "secondary" else "primary"
          _copy_tree(selected.output(family, suffix).resolve("target/bundles/" + id), root.resolve(suffix + "/" + id))
        }
      }
      body(new Intake(root, selected))
    } finally {
      val stream = Files.walk(root)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
      finally stream.close()
    }
  }

  private def _copy_tree(source: Path, destination: Path): Unit = {
    val stream = Files.walk(source)
    try stream.iterator().asScala.foreach { path =>
      val target = destination.resolve(source.relativize(path))
      if (Files.isDirectory(path)) Files.createDirectories(target)
      else Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
    } finally stream.close()
  }
  private def _write(path: Path, text: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.writeString(path, text, StandardCharsets.UTF_8)
    ()
  }
  private def _carrier_jar(classdirectory: Path, output: Path): Path = {
    val prefix = "org/simplemodeling/textus/cbdsupport/generated/capabilityproducer"
    val directory = classdirectory.resolve(prefix)
    val symbols = Vector("StoreFirstMetadata", "ArchiveFirstMetadata", "RemoteFirstMetadata",
      "StoreSecondMetadata", "ArchiveSecondMetadata", "RemoteSecondMetadata", "StoreContractsMetadata", "ArchiveContractsMetadata")
    symbols.foreach { name =>
      require(Files.isRegularFile(directory.resolve(name + "$.class")) && Files.isRegularFile(directory.resolve(name + ".tasty")),
        "Actual configured Scala 3 compiled accessor is required: " + name)
    }
    Files.createDirectories(output.getParent)
    val stream = Files.walk(directory)
    val jar = new JarOutputStream(Files.newOutputStream(output))
    try stream.iterator().asScala.filter(path => Files.isRegularFile(path) &&
      (path.toString.endsWith(".class") || path.toString.endsWith(".tasty"))).toVector.sortBy(_.toString).foreach { path =>
      val name = classdirectory.relativize(path).iterator().asScala.map(_.toString).mkString("/")
      jar.putNextEntry(new JarEntry(name))
      Files.copy(path, jar)
      jar.closeEntry()
    } finally { jar.close(); stream.close() }
    output
  }
  private def _run_command(argv: Vector[String], cwd: Path, classpath: String, log: Path): Unit = {
    val java = Paths.get(sys.props("java.home"), "bin", "java")
    val command = Vector(java.toString, "-Xmx2048m", "-cp", classpath, "cozy.Cozy") ++ argv
    val process = new ProcessBuilder(command.asJava).directory(cwd.toFile).redirectErrorStream(true).redirectOutput(log.toFile).start()
    try {
      require(process.waitFor(180, TimeUnit.SECONDS), "Normal package-car timed out; log: " + log)
      require(process.exitValue() == 0, "Normal package-car failed; log: " + log)
    } finally {
      if (process.isAlive) {
        process.destroyForcibly()
        process.waitFor(10, TimeUnit.SECONDS)
      }
    }
  }

  def record(id: String, revision: Long): InternalModelRecordReference = InternalModelRecordReference(
    InternalModelRecordId.from(id).fold(error => throw new IllegalArgumentException(error), identity),
    InternalModelRecordRevision.from(revision).fold(error => throw new IllegalArgumentException(error), identity))
  def snapshot(id: String, revision: Long): SnapshotReference = right(SnapshotReference.from(record(id, revision)))
  def attempt(id: String, revision: Long, selection: Selection, snapshotReference: SnapshotReference): Attempt =
    Attempt(right(AttemptReference.from(record(id, revision))), snapshotReference, selection)
  def right[A](value: Either[Diagnostic, A]): A = value.fold(error => throw new IllegalArgumentException(error.toString), identity)
  def left[A](value: Either[Diagnostic, A]): Diagnostic = value.fold(identity, _ => throw new IllegalArgumentException("Expected a typed rejection."))
  def available(value: Vector[ComponentUsagePackage], id: String): ComponentUsageFamily.Available =
    value.find(_.input.component == coordinate(id)).get.family match {
    case selected: ComponentUsageFamily.Available => selected
    case _ => throw new IllegalArgumentException("A complete acquired ComponentUsage family is required.")
  }
}
