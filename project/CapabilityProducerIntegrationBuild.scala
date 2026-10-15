import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import org.goldenport.cozy.CozyPlugin.autoImport._
import sbt._
import sbt.Keys._

/*
 * Explicit Test-only normal producer inputs and generated Scala source products.
 * @since   Oct. 14, 2026
 * @version Oct. 15, 2026
 * @author  ASAMI, Tomoharu
 */
object CapabilityProducerIntegrationBuild {
  val selectedProducerRoot = settingKey[Option[File]]("Explicit dedicated Capability producer source root")
  val prepareCapabilityProducer = taskKey[Option[Products]]("Five actual single-source normal Capability productions")
  final case class Products(sources: Seq[File], resource: File)

  val settings: Seq[Def.Setting[_]] = Seq(
    selectedProducerRoot := None,
    Test / unmanagedSourceDirectories ++= selectedProducerRoot.value.toSeq.map(_ =>
      baseDirectory.value / "src/test-capability-producer/scala"),
    Test / unmanagedResourceDirectories ++= selectedProducerRoot.value.toSeq.map(_ =>
      baseDirectory.value / "src/test-capability-producer/resources"),
    prepareCapabilityProducer := Def.taskDyn {
      selectedProducerRoot.value match {
        case None => Def.task { None: Option[Products] }
        case Some(directory) => Def.task {
          val jars = InternalModelExclusionBuild.selectedCncfSourceJars.value
          require(jars.size == 1, "Capability producer requires the actual selected CNCF source JAR")
          val cozyversion = CbdSupportProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.cozyVersion")
          val cncfversion = CbdSupportProjectYamlBuild.dependencyVersion(cozyProjectMetadata.value, "org.goldenport", "goldenport-cncf")
          val products = _produce(baseDirectory.value, target.value, (Test / classDirectory).value,
            (Test / resourceManaged).value, directory, cozyversion, cncfversion, jars.head.data)
          Some(products): Option[Products]
        }
      }
    }.value,
    Test / sourceGenerators += Def.task {
      prepareCapabilityProducer.value.toSeq.flatMap(_.sources)
    }.taskValue,
    Test / resourceGenerators += Def.task {
      prepareCapabilityProducer.value.toSeq.map(_.resource)
    }.taskValue
  )

  private def _produce(base: File, targetroot: File, classdirectory: File, resourceroot: File,
    producerroot: File, cozyversion: String, cncfversion: String, cncfjar: File): Products = {
    val runtimeversion = IO.read(producerroot / "target/cozy.d/runtime-version.txt").trim
    require(runtimeversion == cozyversion, "Actual dedicated Cozy runtime version mismatch")
    val runtimeclasspath = IO.read(producerroot / "target/cozy.d/runtime-classpath.txt").trim
    val entries = runtimeclasspath.split(java.util.regex.Pattern.quote(File.pathSeparator), -1).toVector
    require(entries.nonEmpty && entries.forall(path => path.nonEmpty && file(path).exists),
      "Every exported dedicated Cozy runtime classpath entry must exist")
    require(entries.map(path => file(path).getCanonicalFile).contains(
      (producerroot / "target/scala-2.12/classes").getCanonicalFile),
      "The actual dedicated Scala 2.12 Cozy classes must be on its exported runtime classpath")
    val parent = targetroot / "capability-producer-integration"
    IO.createDirectory(parent)
    val runroot = Files.createTempDirectory(parent.toPath, "production-").toFile
    val inputroot = runroot / "inputs"
    val resources = base / "src/test-capability-producer/resources"
    Seq("capability-model.cml", "capability-model-secondary.cml").foreach { name =>
      IO.copyFile(resources / "model" / name, inputroot / "model" / name)
    }
    val descriptor = runroot / "runtime.yaml"
    val zip = new ZipFile(cncfjar)
    try {
      val entry = Option(zip.getEntry("META-INF/cncf/runtime.yaml"))
        .getOrElse(sys.error("Actual CNCF source JAR lacks META-INF/cncf/runtime.yaml"))
      val stream = zip.getInputStream(entry)
      try Files.copy(stream, descriptor.toPath) finally stream.close()
    } finally zip.close()
    val properties = new Properties
    properties.setProperty("schema", "cbd.capability-producer-fixture.v1")
    Seq("productionRoot" -> runroot, "inputRoot" -> inputroot, "classDirectory" -> classdirectory,
      "cncfJar" -> cncfjar, "producerRoot" -> producerroot).foreach { case (key, path) =>
      properties.setProperty(key, path.getAbsolutePath)
    }
    properties.setProperty("cozyVersion", cozyversion)
    properties.setProperty("cncfVersion", cncfversion)
    properties.setProperty("runtimeClasspath", runtimeclasspath)
    val variants = Vector(
      ("primary", "First", "header:primary-r42", "capability-model.cml", Vector("Store", "Archive")),
      ("secondary", "First", "header:secondary-r43", "capability-model-secondary.cml", Vector("Remote")),
      ("primary", "Second", "header:primary-r44", "capability-model.cml", Vector("Store", "Archive")),
      ("secondary", "Second", "header:secondary-r45", "capability-model-secondary.cml", Vector("Remote")),
      ("primary", "Contracts", "header:primary-r46", "capability-model.cml", Vector("Store", "Archive")))
    val sources = variants.flatMap { case (family, suffix, header, name, components) =>
      val output = runroot / (family + suffix)
      val request = runroot / (family + suffix + "-request.json")
      val requestname = if (suffix == "Contracts") "primary-contract-only" else family
      val text = IO.read(resources / "requests" / (requestname + ".json"))
        .replace("${PRODUCER_VERSION}", cozyversion).replace("${HEADER_REVISION}", header)
        .replace("${SUFFIX}", suffix)
      IO.write(request, text, StandardCharsets.UTF_8)
      val sourceidentity = "model/" + name
      val argv = Vector("modeler-scala", (inputroot / sourceidentity).getAbsolutePath,
        "--capability-request", request.getAbsolutePath, "--capability-source-root", inputroot.getAbsolutePath,
        "--cncf-version", cncfversion, "--cozy-generator-version", cozyversion,
        "--component-version", "1.2.3-SNAPSHOT", "--cncf-runtime-descriptor", descriptor.getAbsolutePath,
        "--generation-source-identity", sourceidentity, "--save", output.getAbsolutePath)
      _run_command(argv, runroot, runtimeclasspath, runroot / (family + suffix + ".log"))
      properties.setProperty(family + suffix, output.getAbsolutePath)
      components.map { id =>
        val generated = output / "target/generated" / (id + suffix + ".scala")
        require(generated.isFile, "Missing actual generated Capability source: " + generated)
        generated
      }
    }
    val resource = resourceroot / "capability-producer-integration.properties"
    IO.createDirectory(resource.getParentFile)
    val stream = Files.newOutputStream(resource.toPath)
    try properties.store(stream, "Ordinary Test product paths and selected versions") finally stream.close()
    Products(sources, resource)
  }

  private def _run_command(argv: Vector[String], cwd: File, classpath: String, log: File): Unit = {
    val java = file(sys.props("java.home")) / "bin/java"
    val command = Vector(java.getAbsolutePath, "-Xmx2048m", "-cp", classpath, "cozy.Cozy") ++ argv
    val process = new ProcessBuilder(command: _*).directory(cwd).redirectErrorStream(true).redirectOutput(log).start()
    try {
      require(process.waitFor(180, TimeUnit.SECONDS), "Capability producer timed out; log: " + log)
      require(process.exitValue() == 0, "Capability producer failed; log: " + log)
    } finally {
      if (process.isAlive) {
        process.destroyForcibly()
        process.waitFor(10, TimeUnit.SECONDS)
      }
    }
  }
}
